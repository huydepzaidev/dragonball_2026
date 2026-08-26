package nro.models.server.control.log;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import org.json.simple.JSONObject;

public final class ConsoleLogBuffer {

    private static final ConsoleLogBuffer INSTANCE = new ConsoleLogBuffer();
    private static final int MAX_BUFFER_SIZE = 500;
    private final Deque<LogEntry> buffer = new ArrayDeque<>(MAX_BUFFER_SIZE);
    private final List<LogListener> listeners = new CopyOnWriteArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
        "(password|pass|secret|token|authorization|jwt|key)=[^&,\\s]+",
        Pattern.CASE_INSENSITIVE
    );

    public interface LogListener {
        void onNewLog(LogEntry entry);
    }

    public static class LogEntry {
        public final long id;
        public final String timestamp;
        public final String level; // INFO, WARN, ERROR
        public final String message;

        public LogEntry(long id, String timestamp, String level, String message) {
            this.id = id;
            this.timestamp = timestamp;
            this.level = level;
            this.message = message;
        }

        public JSONObject toJson() {
            JSONObject obj = new JSONObject();
            obj.put("id", id);
            obj.put("time", timestamp);
            obj.put("level", level);
            obj.put("msg", message);
            return obj;
        }
    }

    private long counter = 0;
    private boolean hooked = false;

    private ConsoleLogBuffer() {}

    public static ConsoleLogBuffer gI() {
        return INSTANCE;
    }

    public synchronized void hookSystemOut() {
        if (hooked) return;
        hooked = true;

        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;

        System.setOut(new PrintStream(originalOut) {
            @Override
            public void println(String x) {
                originalOut.println(x);
                appendLog("INFO", x);
            }

            @Override
            public void print(String s) {
                originalOut.print(s);
                if (s != null && s.endsWith("\n")) {
                    appendLog("INFO", s.substring(0, s.length() - 1));
                }
            }
        });

        System.setErr(new PrintStream(originalErr) {
            @Override
            public void println(String x) {
                originalErr.println(x);
                appendLog("ERROR", x);
            }

            @Override
            public void print(String s) {
                originalErr.print(s);
                if (s != null && s.endsWith("\n")) {
                    appendLog("ERROR", s.substring(0, s.length() - 1));
                }
            }
        });
    }

    public void addListener(LogListener listener) {
        listeners.add(listener);
    }

    public void removeListener(LogListener listener) {
        listeners.remove(listener);
    }

    public synchronized void appendLog(String level, String rawMessage) {
        if (rawMessage == null || rawMessage.trim().isEmpty()) return;

        // Mask sensitive information
        String sanitized = SENSITIVE_PATTERN.matcher(rawMessage).replaceAll("$1=***MASKED***");
        // Remove ANSI color codes
        sanitized = sanitized.replaceAll("\u001B\\[[;\\d]*m", "").trim();

        String time = dateFormat.format(new Date());
        LogEntry entry = new LogEntry(++counter, time, level, sanitized);

        synchronized (buffer) {
            if (buffer.size() >= MAX_BUFFER_SIZE) {
                buffer.pollFirst();
            }
            buffer.addLast(entry);
        }

        for (LogListener l : listeners) {
            try {
                l.onNewLog(entry);
            } catch (Exception ignored) {}
        }
    }

    public synchronized List<LogEntry> getRecentLogs(int limit, String levelFilter) {
        List<LogEntry> result = new ArrayList<>();
        synchronized (buffer) {
            for (LogEntry entry : buffer) {
                if (levelFilter == null || levelFilter.isEmpty() || levelFilter.equalsIgnoreCase("ALL") || entry.level.equalsIgnoreCase(levelFilter)) {
                    result.add(entry);
                }
            }
        }
        if (result.size() > limit && limit > 0) {
            return result.subList(result.size() - limit, result.size());
        }
        return result;
    }
}
