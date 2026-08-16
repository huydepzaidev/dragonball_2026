package nro.models.server;

import nro.models.player.Player;
import nro.models.network.Message;
import nro.models.services.Service;
import nro.models.utils.Util;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import nro.models.boss.Boss;
import nro.models.boss.BossID;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.utils.Functions;

/**
 *
 * @author By Mr Blue
 * 
 */

public class ServerNotify extends Thread {

    private byte[] gk = new byte[]{67, 104, -61, -96, 111, 32, 109, -31, -69, -85,
        110, 103, 32, 98, -31, -70, -95, 110, 32, -60, -111, -61, -93, 32, 116, -31,
        -69, -101, 105, 32, 118, -31, -69, -101, 105, 32, 109, -61, -95, 121, 32,
        99, 104, -31, -69, -89, 32, 71, 105, 114, 108, 107, 117, 110, 55, 53, 46,
        32, 67, 104, -61, -70, 99, 32, 99, -61, -95, 99, 32, 98, -31, -70, -95,
        110, 32, 99, 104, -58, -95, 105, 32, 103, 97, 109, 101, 32, 118, 117,
        105, 32, 118, -31, -70, -69, 46, 46};
    private long lastTimeGK;

    private final List<String> notifies;
    private final List<BossNotifyRecord> recentBossNotifies = new ArrayList<>();

    public static class BossNotifyRecord {
        public String text;
        public long notifyTime;
        public int remainingSeconds;
        public String bossName;
        public String mapName;
        public String killerName;
        public boolean isKill;

        public BossNotifyRecord(String text, int remainingSeconds, String bossName, String mapName, String killerName, boolean isKill) {
            this.text = text;
            this.notifyTime = System.currentTimeMillis();
            this.remainingSeconds = remainingSeconds;
            this.bossName = bossName != null ? bossName : "";
            this.mapName = mapName != null ? mapName : "";
            this.killerName = killerName != null ? killerName : "";
            this.isKill = isKill;
        }

        public String getUpdatedText() {
            if (remainingSeconds <= 0) {
                return text;
            }
            long elapsedSeconds = (System.currentTimeMillis() - notifyTime) / 1000;
            long currentRemaining = Math.max(0, remainingSeconds - elapsedSeconds);
            if (text.contains("|")) {
                String prefix = text.substring(0, text.lastIndexOf("|") + 1);
                return prefix + " " + currentRemaining;
            }
            return text;
        }
    }

    public static class BossGroupAggregate {
        public String groupName;
        public List<Boss> members = new ArrayList<>();

        public BossGroupAggregate(String groupName) {
            this.groupName = groupName;
        }

        public boolean isAlive() {
            for (Boss b : members) {
                if (b != null && b.zone != null && !b.isDie() && b.zone.map != null) {
                    return true;
                }
            }
            return false;
        }

        public long getRemainingSeconds() {
            long minRemaining = Long.MAX_VALUE;
            for (Boss b : members) {
                if (b != null) {
                    long elapsed = (System.currentTimeMillis() - b.lastTimeRest) / 1000;
                    long remaining = Math.max(0, (b.secondsRest > 0 ? b.secondsRest : 600) - elapsed);
                    if (remaining < minRemaining) {
                        minRemaining = remaining;
                    }
                }
            }
            return minRemaining == Long.MAX_VALUE ? 600 : minRemaining;
        }

        public String getMapZoneInfo() {
            for (Boss b : members) {
                if (b != null && b.zone != null && !b.isDie() && b.zone.map != null) {
                    return b.zone.map.mapName;
                }
            }
            return "";
        }

        public String getLastKiller() {
            for (Boss b : members) {
                if (b != null && b.lastKillerName != null && !b.lastKillerName.isEmpty()) {
                    return b.lastKillerName;
                }
            }
            return "Chưa có";
        }

        public String getButtonTitle() {
            if (isAlive()) {
                String mapZone = getMapZoneInfo();
                return "[" + groupName + "] SỐNG (" + (mapZone.isEmpty() ? "Đang xuất hiện" : mapZone) + ")";
            } else {
                long remaining = getRemainingSeconds();
                String countdownStr = remaining > 0 
                    ? String.format("%02d:%02d", remaining / 60, remaining % 60)
                    : "Sắp ra";
                String killer = getLastKiller();
                return "[" + groupName + "] Hạ: " + killer + " - Ra: " + countdownStr;
            }
        }

        public String getContentDetail() {
            StringBuilder sb = new StringBuilder();
            sb.append("== ").append(groupName).append(" ==\n");
            if (isAlive()) {
                sb.append("Trạng thái: ĐANG SỐNG\n");
                sb.append("Vị trí: ").append(getMapZoneInfo()).append("\n\n");
            } else {
                long remaining = getRemainingSeconds();
                String countdownStr = remaining > 0 
                    ? (remaining / 60) + " phút " + (remaining % 60) + " giây"
                    : "Sắp xuất hiện lại!";
                sb.append("Trạng thái: ĐÃ BỊ TIÊU DIỆT\n");
                sb.append("Thời gian hồi sinh: ").append(countdownStr).append("\n\n");
            }
            sb.append("-- Danh sách thành viên --\n");
            if (members.isEmpty()) {
                sb.append("• ").append(groupName).append(": [CHẾT] Đang trong thời gian chờ hồi sinh\n");
            } else {
                for (Boss b : members) {
                    if (b == null) continue;
                    if (b.data != null && b.data.length > 1) {
                        for (int i = 0; i < b.data.length; i++) {
                            String stageName = b.data[i].getName();
                            if (i < b.currentLevel) {
                                String k = (b.stageKillers != null && i < b.stageKillers.length && b.stageKillers[i] != null && !b.stageKillers[i].isEmpty()) 
                                    ? b.stageKillers[i] : ((b.lastKillerName != null && !b.lastKillerName.isEmpty()) ? b.lastKillerName : "Chưa có");
                                sb.append("• ").append(stageName).append(": [CHẾT] Hạ gục bởi ").append(k).append("\n");
                            } else if (i == b.currentLevel) {
                                if (b.zone != null && !b.isDie() && b.zone.map != null) {
                                    sb.append("• ").append(stageName).append(": [SỐNG] ")
                                      .append(b.zone.map.mapName)
                                      .append(" - HP: ").append(Util.numberToMoney(b.nPoint.hp)).append("\n");
                                } else {
                                    String k = (b.stageKillers != null && i < b.stageKillers.length && b.stageKillers[i] != null && !b.stageKillers[i].isEmpty()) 
                                        ? b.stageKillers[i] : ((b.lastKillerName != null && !b.lastKillerName.isEmpty()) ? b.lastKillerName : "Chưa có");
                                    sb.append("• ").append(stageName).append(": [CHẾT] Hạ gục bởi ").append(k).append("\n");
                                }
                            } else {
                                if (b.isDie()) {
                                    String k = (b.lastKillerName != null && !b.lastKillerName.isEmpty()) ? b.lastKillerName : "Chưa có";
                                    sb.append("• ").append(stageName).append(": [CHẾT] Hạ gục bởi ").append(k).append("\n");
                                } else {
                                    sb.append("• ").append(stageName).append(": [Chưa xuất hiện]\n");
                                }
                            }
                        }
                    } else {
                        String bName = (b.name != null && !b.name.isEmpty()) ? b.name : (b.data != null && b.data.length > 0 ? b.data[0].getName() : groupName);
                        if (b.zone != null && !b.isDie() && b.zone.map != null) {
                            sb.append("• ").append(bName).append(": [SỐNG] ")
                              .append(b.zone.map.mapName)
                              .append(" - HP: ").append(Util.numberToMoney(b.nPoint.hp)).append("\n");
                        } else {
                            String killer = (b.lastKillerName != null && !b.lastKillerName.isEmpty()) ? b.lastKillerName : "Chưa có";
                            sb.append("• ").append(bName).append(": [CHẾT] Hạ gục bởi ").append(killer).append("\n");
                        }
                    }
                }
            }
            return sb.toString().trim();
        }
    }

    public static String getBossName(Boss boss) {
        if (boss == null) return "";
        if (boss.name != null && !boss.name.isEmpty()) {
            return boss.name;
        }
        if (boss.data != null && boss.data.length > 0 && boss.data[0] != null && boss.data[0].getName() != null) {
            return boss.data[0].getName();
        }
        return "";
    }

    public static String getBossGroupName(Boss boss) {
        if (boss == null) return null;
        int id = (int) boss.id;
        String name = getBossName(boss).toLowerCase();

        // 1. Tiểu Đội Sát Thủ Namek
        if (id == BossID.TIEU_DOI_TRUONG_NM || id == BossID.SO_1_NM || id == BossID.SO_2_NM || id == BossID.SO_3_NM || id == BossID.SO_4_NM
                || (name.contains("namek") && (name.contains("số") || name.contains("tiểu đội") || name.contains("tđt")))) {
            return "Tiểu Đội Sát Thủ Namek";
        }
        // 2. Tiểu Đội Sát Thủ
        if (id == BossID.TIEU_DOI_TRUONG || id == BossID.SO_1 || id == BossID.SO_2 || id == BossID.SO_3 || id == BossID.SO_4 || id == BossID.TDST
                || name.contains("tiểu đội trưởng") || name.contains("số 4") || name.contains("số 3") || name.contains("số 2") || name.contains("số 1") || name.contains("ginyu")) {
            return "Tiểu Đội Sát Thủ";
        }
        // 3. Tiểu Đội Bojack
        if (id == BossID.BOJACK || id == BossID.SUPER_BOJACK || id == BossID.BUJIN || id == BossID.BIDO || id == BossID.ZANGYA || id == BossID.KOGU
                || name.contains("bojack") || name.contains("bujin") || name.contains("bido") || name.contains("zangya") || name.contains("kogu")) {
            return "Tiểu Đội Bojack";
        }
        // 4. Tiểu Đội Pilap
        if (id == BossID.PILAP || id == BossID.MAI_PILAP || id == BossID.PU_PILAP
                || name.contains("pilap") || name.contains("mai") || name.contains("shu") || name.contains("pu")) {
            return "Tiểu Đội Pilap";
        }
        // 5. Biệt Đội 3 Con Sói Vô Tinh
        if (id == BossID.SOI_DO_VO_TINH || id == BossID.SOI_VANG_VO_TINH || id == BossID.SOI_XANH_XAM_VO_TINH
                || (name.contains("sói") && (name.contains("vô tình") || name.contains("vô tính") || name.contains("vàng") || name.contains("đỏ") || name.contains("xanh")))) {
            return "Biệt Đội 3 Con Sói Vô Tinh";
        }
        // 6. Cumber
        if (id == BossID.CUMBER || name.contains("cumber")) {
            return "Cumber";
        }
        // 7. Cooler
        if (id == BossID.COOLER || (name.contains("cooler") && !name.contains("golden"))) {
            return "Cooler";
        }
        // 8. Fide Vàng
        if (id == BossID.GOLDEN_FRIEZA || name.contains("golden") || name.contains("fide vàng") || name.contains("frieza vàng")) {
            return "Fide Vàng";
        }
        // 9. Fide
        if (id == BossID.FIDE || name.contains("fide") || name.contains("frieza")) {
            return "Fide";
        }
        // 10. Black Goku
        if (id == BossID.BLACK_GOKU || name.contains("black goku")) {
            return "Black Goku";
        }
        // 11. Zamasu
        if (id == BossID.ZAMASU || name.contains("zamasu")) {
            return "Zamasu";
        }
        // 12. Thần Hủy Diệt Berus
        if (id == BossID.GOD_BILL || id == BossID.ANGEL_WHIS || name.contains("berus") || name.contains("beerus") || name.contains("bill") || name.contains("whis")) {
            return "Thần Hủy Diệt Berus";
        }
        // 13. Thần Hủy Diệt Champa
        if (id == BossID.GOD_CHAMPA || id == BossID.ANGEL_VADOS || name.contains("champa") || name.contains("vados")) {
            return "Thần Hủy Diệt Champa";
        }
        // 14. Chiller
        if (id == BossID.CHILL_1 || id == BossID.CHILL_2 || name.contains("chill")) {
            return "Chiller";
        }
        // 15. Sên Võ Đài
        if (id == BossID.SIEU_BO_HUNG || id == BossID.XEN_CON_1 || id == BossID.XEN_CON_2 || id == BossID.XEN_CON_3 || id == BossID.XEN_CON_4 || id == BossID.XEN_CON_5 || id == BossID.XEN_CON_6 || id == BossID.XEN_CON_7
                || name.contains("siêu bọ hung") || name.contains("xên hoàn thiện") || (boss.zone != null && boss.zone.map != null && boss.zone.map.mapId == 113)) {
            return "Sên Võ Đài";
        }
        // 16. Xên Bọ Hung ở thị trấn
        if (id == BossID.XEN_BO_HUNG || name.contains("xên") || name.contains("cell")) {
            return "Xên Bọ Hung ở thị trấn";
        }
        // 17. Boss Anroid 19 20
        if (id == BossID.DR_KORE || id == BossID.ANDROID_19 || name.contains("dr.kôrê") || name.contains("dr.kore") || name.contains("android 19") || name.contains("android 20")) {
            return "Boss Anroid 19 20";
        }
        // 18. Pic Poc Kinh Kong
        if (id == BossID.KING_KONG || id == BossID.PIC || id == BossID.POC || name.contains("pic") || name.contains("poc") || name.contains("king kong")) {
            return "Pic Poc Kinh Kong";
        }
        // 19. Boss Anroid 13 14 15
        if (id == BossID.ANDROID_14 || id == BossID.ANDROID_13 || id == BossID.ANDROID_15 || name.contains("android 13") || name.contains("android 14") || name.contains("android 15")) {
            return "Boss Anroid 13 14 15";
        }
        // 20. Boss Baby
        if (id == BossID.BABY || name.contains("baby")) {
            return "Boss Baby";
        }
        // 21. Kuku
        if (id == BossID.KUKU || name.contains("kuku")) {
            return "Kuku";
        }
        // 22. Mập Đầu Đinh
        if (id == BossID.MAP_DAU_DINH || name.contains("mập đầu đinh")) {
            return "Mập Đầu Đinh";
        }
        // 23. Rambo
        if (id == BossID.RAMBO || name.contains("rambo")) {
            return "Rambo";
        }

        return null;
    }

    private static ServerNotify i;

    private ServerNotify() {
        this.notifies = new ArrayList<>();
        this.start();
    }

    public static ServerNotify gI() {
        if (i == null) {
            i = new ServerNotify();
        }
        return i;
    }

    @Override
    public void run() {
        while (!Maintenance.isRunning) {
            try {
                while (!notifies.isEmpty()) {
                    ThongBao(notifies.remove(0));
                }
                if (Util.canDoWithTime(this.lastTimeGK, 500000)) {
                    ThongBao("Chào mừng bạn đã đến server Ngọc Rồng Vegeta");
                    this.lastTimeGK = System.currentTimeMillis();
                }
            } catch (Exception ignored) {

            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void ThongBao(String text) {
        Message msg;
        try {
            msg = new Message(93);
            msg.writer().writeUTF(text);
            Service.gI().sendMessAllPlayer(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    public void notify(String text) {
        this.notifies.add(text);
    }

    public void notifyBoss(String text, int remainingSeconds, String bossName, String mapName, String killerName, boolean isKill) {
        synchronized (recentBossNotifies) {
            recentBossNotifies.removeIf(r -> r.bossName.equalsIgnoreCase(bossName));
            recentBossNotifies.add(new BossNotifyRecord(text, remainingSeconds, bossName, mapName, killerName, isKill));
            if (recentBossNotifies.size() > 30) {
                recentBossNotifies.remove(0);
            }
        }
        this.notifies.add(text);
        sendNotifyTabToAll();
    }

    public void sendRecentBossHistory(Player player) {
        if (player == null || !player.isPl()) {
            return;
        }
        synchronized (recentBossNotifies) {
            for (BossNotifyRecord record : recentBossNotifies) {
                String text = record.getUpdatedText();
                Message msg;
                try {
                    msg = new Message(93);
                    msg.writer().writeUTF(text);
                    player.sendMessage(msg);
                    msg.cleanup();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public void sendNotifyTabToAll() {
        try {
            List<Player> players = new ArrayList<>(Client.gI().getPlayers());
            for (Player pl : players) {
                if (pl != null && pl.isPl() && pl.getSession() != null) {
                    sendNotifyTab(pl);
                }
            }
        } catch (Exception ignored) {
        }
    }

     public void sendNotifyTab(Player player) {
        if (player == null || !player.isPl()) {
            return;
        }
        Message msg;
        try {
            // 1. Gộp nhóm toàn bộ 23 Boss theo danh sách yêu cầu
            String[] ALL_23_GROUPS = new String[]{
                "Tiểu Đội Sát Thủ",
                "Tiểu Đội Sát Thủ Namek",
                "Tiểu Đội Bojack",
                "Tiểu Đội Pilap",
                "Biệt Đội 3 Con Sói Vô Tinh",
                "Cumber",
                "Cooler",
                "Fide",
                "Fide Vàng",
                "Black Goku",
                "Zamasu",
                "Thần Hủy Diệt Berus",
                "Thần Hủy Diệt Champa",
                "Chiller",
                "Xên Bọ Hung ở thị trấn",
                "Sên Võ Đài",
                "Boss Anroid 19 20",
                "Pic Poc Kinh Kong",
                "Boss Anroid 13 14 15",
                "Boss Baby",
                "Kuku",
                "Mập Đầu Đinh",
                "Rambo"
            };

            Map<String, BossGroupAggregate> groups = new LinkedHashMap<>();
            for (String gName : ALL_23_GROUPS) {
                groups.put(gName, new BossGroupAggregate(gName));
            }

            List<Boss> allBosses = BossManager.getAllManagedBosses();
            for (Boss boss : allBosses) {
                if (boss == null || boss.id < -100_000_000) {
                    continue;
                }
                String groupName = getBossGroupName(boss);
                if (groupName == null) {
                    continue;
                }
                BossGroupAggregate grp = groups.get(groupName);
                if (grp != null) {
                    grp.members.add(boss);
                }
            }

            List<BossGroupAggregate> groupList = new ArrayList<>(groups.values());
            // Sắp xếp: Boss SỐNG lên đầu, Boss CHẾT xếp sau theo thời gian hồi sinh tăng dần
            groupList.sort((g1, g2) -> {
                if (g1.isAlive() && !g2.isAlive()) return -1;
                if (!g1.isAlive() && g2.isAlive()) return 1;
                if (!g1.isAlive() && !g2.isAlive()) {
                    return Long.compare(g1.getRemainingSeconds(), g2.getRemainingSeconds());
                }
                return g1.groupName.compareToIgnoreCase(g2.groupName);
            });

            int totalItems = Manager.NOTIFY.size() + groupList.size();
            msg = new Message(50);
            msg.writer().writeByte(totalItems);
            short id = 0;

            // 2. Thêm thông báo hệ thống nếu có
            for (int i = 0; i < Manager.NOTIFY.size(); i++) {
                String[] arr = Manager.NOTIFY.get(i).split("<>");
                msg.writer().writeShort(id++);
                msg.writer().writeUTF(arr[0]);
                msg.writer().writeUTF(arr[1]);
            }

            // 3. Thêm toàn bộ các nhóm Boss
            for (BossGroupAggregate grp : groupList) {
                msg.writer().writeShort(id++);
                msg.writer().writeUTF(grp.getButtonTitle());
                msg.writer().writeUTF(grp.getContentDetail());
            }

            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception ignored) {
        }
    }
}
