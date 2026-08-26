package nro.models.matches.giai_dau;

import nro.models.matches.dai_hoi_vo_thuat.WorldMartialArtsTournamentService;
import nro.models.matches.dai_hoi_vo_thuat.WorldMartialArtsTournament;
import nro.models.utils.Functions;
import nro.models.utils.Logger;
import nro.models.consts.ConstTask;
import nro.models.consts.ConstTournament;
import nro.models.data.LocalManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import nro.models.map.Map;
import nro.models.map.Zone;
import nro.models.player.Player;
import nro.models.server.Client;
import nro.models.server.Maintenance;
import nro.models.map.service.MapService;
import nro.models.services.MailboxService;
import nro.models.services.Service;
import nro.models.services.TaskService;
import nro.models.utils.TimeUtil;
import nro.models.utils.Util;

public class WorldMartialArtsTournamentManager implements Runnable {

    public static class RegisteredParticipant {
        public final long playerId;
        public final int accountId;
        public final String playerName;
        public final long registeredTime;
        public volatile boolean refunded;

        public RegisteredParticipant(long playerId, int accountId, String playerName, long registeredTime) {
            this.playerId = playerId;
            this.accountId = accountId;
            this.playerName = playerName;
            this.registeredTime = registeredTime;
            this.refunded = false;
        }
    }

    public final ConcurrentHashMap<Long, RegisteredParticipant> registeredParticipants = new ConcurrentHashMap<>();
    public final List<Long> listReg = Collections.synchronizedList(new ArrayList<>());
    public final List<Long> listWait = Collections.synchronizedList(new ArrayList<>());
    public final List<String> listChamp = Collections.synchronizedList(new ArrayList<>());
    public final List<WorldMartialArtsTournament> listTournaments = Collections.synchronizedList(new ArrayList<>());

    public String cupName = "Ngoại hạng";
    public int gem = 0;
    public int gold = 0;
    public int round = 0;

    public long lastUpdateTime;
    public boolean canReg;
    public int nextTime;

    public int lastMins;
    public long lastWaitTime;
    public int waitTime;
    public long lastTime;

    public List<String> chatText;

    public String lastHandledSessionKey = "";
    public volatile boolean isSessionClosing = false;
    public volatile boolean sessionCancelled = false;

    private static WorldMartialArtsTournamentManager instance;

    public static WorldMartialArtsTournamentManager gI() {
        if (instance == null) {
            instance = new WorldMartialArtsTournamentManager();
        }
        return instance;
    }

    public WorldMartialArtsTournamentManager() {
        chatText = new ArrayList<>();
        lastTime = System.currentTimeMillis();
    }

    public int getRegisteredCount() {
        return listReg.size();
    }

    public boolean isRegistered(long playerId) {
        return listReg.contains(playerId);
    }

    public void registerPlayer(Player player) {
        if (player == null) {
            return;
        }
        int accountId = player.getSession() != null ? player.getSession().userId : getAccountId(player.id);
        registeredParticipants.put(player.id, new RegisteredParticipant(player.id, accountId, player.name, System.currentTimeMillis()));
        if (!listReg.contains(player.id)) {
            listReg.add(player.id);
        }
    }

    public void unregisterPlayer(Player player) {
        if (player == null) {
            return;
        }
        listReg.remove(player.id);
        registeredParticipants.remove(player.id);
    }

    public void addWinnerToWait(long playerId) {
        if (!listWait.contains(playerId)) {
            listWait.add(playerId);
        }
    }

    @Override
    public void run() {
        while (!Maintenance.isRunning) {
            try {
                long start = System.currentTimeMillis();
                if (Util.isAfterMidnight(lastTime)) {
                    listChamp.clear();
                    lastTime = System.currentTimeMillis();
                }
                if (Util.canDoWithTime(lastUpdateTime, 1000)) {
                    lastUpdateTime = System.currentTimeMillis();
                    int tour = WorldMartialArtsTournamentService.getTournament();
                    int currMin = TimeUtil.getCurrMin();
                    canReg = (tour != -1) && (currMin < ConstTournament.MINS_MAX_CAN_REG);
                    if (tour != -1) {
                        gem = 0;
                        gold = 0;
                        cupName = "Ngoại hạng";
                        update();
                    }
                    nextTime = WorldMartialArtsTournamentService.getNextTournamentTime();
                }
                updateChatText();
                Functions.sleep(Math.max(1000 - (System.currentTimeMillis() - start), 10));
            } catch (Exception e) {
                Logger.logException(WorldMartialArtsTournamentManager.class, e);
            }
        }
    }

    public void updateChatText() {
        chatText.clear();
        chatText.add("Đại hội võ thuật thế giới - Giải Ngoại Hạng uy tín bậc nhất");

        if (listChamp.isEmpty()) {
            chatText.add("Với những đấu thủ huyền thoại như Sôn Gô Ku, Ca Đích ... đã từng đạt chức vô địch");
        } else {
            chatText.add("Với những nhà vô địch giải trước là " + String.join(",", listChamp) + " ...");
        }

        if (canReg) {
            chatText.add("Bạn hãy nhanh chân đăng ký ngay bây giờ, giải đấu sẽ bắt đầu vào lúc " + TimeUtil.getCurrHour() + "h30");
        } else if (TimeUtil.getCurrMin() < ConstTournament.MINS_START) {
            chatText.add("Giải đấu tiếp theo sẽ diễn ra vào lúc " + WorldMartialArtsTournamentService.getNextTournamentTime() + "h");
        } else {
            chatText.add("Đang trong thời gian thi đấu, xin chờ đến " + WorldMartialArtsTournamentService.getNextTournamentTime() + "h để đăng ký");
        }
    }

    public void update() {
        int hour = TimeUtil.getCurrHour();
        int min = TimeUtil.getCurrMin();
        String currentSessionKey = LocalDate.now(TimeUtil.VIETNAM_ZONE).toString() + "_" + String.format("%02d", hour);

        // Khởi tạo phiên mới khi bước vào khung giờ mở giải
        if (!currentSessionKey.equals(lastHandledSessionKey)) {
            lastHandledSessionKey = currentSessionKey;
            isSessionClosing = false;
            sessionCancelled = false;
            round = 0;
            listReg.clear();
            listWait.clear();
            registeredParticipants.clear();
            listTournaments.clear();
        }

        // Hard Timeout tại mốc H:57
        if (min >= ConstTournament.MINS_END) {
            isSessionClosing = true;
            if (!listTournaments.isEmpty()) {
                List<WorldMartialArtsTournament> copy = new ArrayList<>(listTournaments);
                for (WorldMartialArtsTournament wmat : copy) {
                    if (wmat != null && wmat.competing) {
                        wmat.timeOut();
                    }
                }
            }
            if (listTournaments.isEmpty()) {
                round = 0;
                listReg.clear();
                listWait.clear();
            }
            return;
        }

        // Diễn ra giải đấu từ phút 30 đến 57
        if (min >= ConstTournament.MINS_START && !isSessionClosing) {
            if (sessionCancelled) {
                return;
            }

            // Giai đoạn bắt đầu giải tại phút 30 (Round == 0)
            if (round == 0) {
                // BƯỚC 1: Validate toàn bộ listReg
                List<Long> validIds = new ArrayList<>();
                for (int i = listReg.size() - 1; i >= 0; i--) {
                    long playerId = listReg.get(i);
                    Player pl = getPlayerById(playerId);
                    if (pl != null && pl.zone != null && pl.zone.map.mapId == 52
                            && pl.nPoint.power >= ConstTournament.REQUIRED_POWER
                            && TaskService.gI().getIdTask(pl) >= ConstTask.TASK_20_0) {
                        validIds.add(playerId);
                    } else {
                        if (pl != null) {
                            Service.gI().sendThongBao(pl, ConstTournament.TEXT_TRUAT_QUYEN);
                        }
                    }
                }

                // BƯỚC 2: Kiểm tra số lượng tối thiểu >= 11 người
                if (validIds.size() < ConstTournament.MIN_PARTICIPANTS) {
                    sessionCancelled = true;
                    // HỦY GIẢI & HOÀN PHÍ 100 HỒNG NGỌC CHO TẤT CẢ REGISTERED PARTICIPANTS
                    for (RegisteredParticipant p : registeredParticipants.values()) {
                        if (!p.refunded) {
                            String token = lastHandledSessionKey + ":" + p.playerId + ":REFUND";
                            MailboxService.sendSystemMailIdempotent(
                                    p.accountId, p.playerId,
                                    "Hoàn Phí Giải Ngoại Hạng",
                                    "Giải Ngoại Hạng phiên " + hour + "h đã bị hủy do không đủ 11 đấu thủ tham gia. Bạn nhận lại 100 Hồng Ngọc phí đăng ký.",
                                    "Trọng Tài",
                                    "[{\"id\": -3, \"quantity\": 100}]",
                                    token
                            );
                            p.refunded = true;
                            Player onlinePl = getPlayerById(p.playerId);
                            if (onlinePl != null) {
                                Service.gI().sendThongBao(onlinePl, "Giải đấu phiên " + hour + "h bị hủy do không đủ 11 người. 100 Hồng Ngọc đã được hoàn vào Hòm Thư.");
                            }
                        }
                    }
                    listReg.clear();
                    listWait.clear();
                    return;
                }

                // Đủ >= 11 người: Cập nhật lại listReg và bắt đầu Round 1
                listReg.clear();
                listReg.addAll(validIds);
            }

            // Tiến trình chuyển vòng
            if (round > 0) {
                if (listWait.size() > 1 && listTournaments.isEmpty() && waitTime - (System.currentTimeMillis() - lastWaitTime) > 30000) {
                    lastWaitTime = System.currentTimeMillis();
                    waitTime = 30000;
                    sendWaitNotify();
                }
                if (listWait.size() > 1 && listReg.isEmpty() && listTournaments.isEmpty() && Util.canDoWithTime(lastWaitTime, waitTime)) {
                    listReg.addAll(listWait);
                    listWait.clear();
                }
                if (lastMins != TimeUtil.getCurrMin()) {
                    lastMins = TimeUtil.getCurrMin();
                    sendWaitNotify();
                }
            }

            // Khởi tạo các trận đấu trong vòng
            if (!listReg.isEmpty() && listTournaments.isEmpty()) {
                round++;
                for (int i = listReg.size() - 1; i >= 0; i--) {
                    Player pl = getPlayerById(listReg.get(i));
                    if (pl != null && pl.zone != null && pl.zone.map.mapId == 52
                            && pl.nPoint.power >= ConstTournament.REQUIRED_POWER) {
                        // Hợp lệ
                    } else {
                        if (pl != null) {
                            Service.gI().sendThongBao(pl, ConstTournament.TEXT_TRUAT_QUYEN);
                        }
                        listReg.remove(i);
                    }
                }

                // Nếu số người lẻ, 1 người may mắn nhận vé bye vào thẳng vòng sau
                if (listReg.size() % 2 != 0) {
                    Player plHup = getPlayerById(listReg.remove(listReg.size() - 1));
                    if (plHup != null) {
                        listWait.add(plHup.id);
                        Service.gI().sendThongBao(plHup, ConstTournament.TEXT_DOI_THU_BO_CUOC);
                    }
                }

                // Kiểm tra xem đây có phải là trận CHUNG KẾT duy nhất hay không
                boolean isFinal = (listReg.size() == 2 && listWait.isEmpty());

                for (int i = 0; i < listReg.size() - 1; i += 2) {
                    Player p1 = getPlayerById(listReg.get(i));
                    Player p2 = getPlayerById(listReg.get(i + 1));
                    Zone z = getZoneTournament();
                    if (p1 != null && p2 != null && z != null) {
                        WorldMartialArtsTournament wmat = new WorldMartialArtsTournament(p1, p2, z, isFinal);
                        addWMAT(wmat);
                    }
                }
                lastWaitTime = System.currentTimeMillis();
                waitTime = 240000;
                listReg.clear();
            }
        } else {
            // Trước phút 30: Đếm ngược thông báo
            if (lastMins != TimeUtil.getCurrMin()) {
                lastMins = TimeUtil.getCurrMin();
                for (int i = listReg.size() - 1; i >= 0; i--) {
                    try {
                        Player pl = getPlayerById(listReg.get(i));
                        if (pl != null && pl.zone != null) {
                            Service.gI().sendThongBao(pl, "Trận đấu của bạn sẽ diễn ra trong vòng " + (ConstTournament.MINS_START - TimeUtil.getCurrMin()) + " phút nữa");
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    public void sendWaitNotify() {
        if (!listWait.isEmpty()) {
            for (int i = listWait.size() - 1; i >= 0; i--) {
                try {
                    Player pl = getPlayerById(listWait.get(i));
                    if (pl != null && pl.zone != null) {
                        Service.gI().sendThongBao(pl, "Trận đấu của bạn sẽ diễn ra trong vòng " + TimeUtil.getTimeLeft(lastWaitTime, waitTime / 1000) + " nữa");
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    public void rewardChampion(Player pl) {
        if (pl == null) {
            return;
        }
        listChamp.add(pl.name);
        listReg.clear();
        listWait.clear();

        int accountId = (pl.getSession() != null) ? pl.getSession().userId : getAccountId(pl.id);
        String token = lastHandledSessionKey + ":" + pl.id + ":CHAMPION";

        String rewardsJson = "["
                + "{\"id\": -3, \"quantity\": 5000},"
                + "{\"id\": 220, \"quantity\": 10000},"
                + "{\"id\": 221, \"quantity\": 10000},"
                + "{\"id\": 222, \"quantity\": 10000},"
                + "{\"id\": 223, \"quantity\": 10000},"
                + "{\"id\": 224, \"quantity\": 10000},"
                + "{\"id\": 987, \"quantity\": 10000},"
                + "{\"id\": 1775, \"quantity\": 1}"
                + "]";

        MailboxService.sendSystemMailIdempotent(
                accountId, pl.id,
                "Thưởng Vô Địch Giải Ngoại Hạng",
                "Chúc mừng bạn đã xuất sắc vô địch Giải Ngoại Hạng Đại Hội Võ Thuật!",
                "Trọng Tài",
                rewardsJson,
                token
        );

        Service.gI().sendThongBao(pl, ConstTournament.TEXT_VO_DICH);
        String msg = ConstTournament.TEXT_KHOE_VO_DICH.replaceAll("%1", pl.name);
        Service.gI().sendThongBaoToAnotherNotMe(pl, msg);
    }

    public void addWMAT(WorldMartialArtsTournament wmat) {
        listTournaments.add(wmat);
    }

    public void removeWMAT(WorldMartialArtsTournament wmat) {
        listTournaments.remove(wmat);
    }

    public boolean checkPlayer(long id) {
        return listWait.contains(id);
    }

    public Zone getZoneTournament() {
        Map map = MapService.gI().getMapById(51);
        Zone zone = null;
        try {
            if (map != null) {
                int zoneId = 0;
                while (zoneId < map.zones.size()) {
                    Zone zonez = map.zones.get(zoneId);
                    if (getWMAT(zonez) == null) {
                        zone = zonez;
                        break;
                    }
                    zoneId++;
                }
            }
        } catch (Exception ignored) {
        }
        return zone;
    }

    public WorldMartialArtsTournament getWMAT(@NonNull Zone zone) {
        synchronized (listTournaments) {
            for (WorldMartialArtsTournament wmat : listTournaments) {
                if (wmat != null && wmat.zone != null && wmat.zone.equals(zone)) {
                    return wmat;
                }
            }
        }
        return null;
    }

    public Player getPlayerById(long id) {
        try {
            return Client.gI().getPlayer(id);
        } catch (Exception ignored) {
        }
        return null;
    }

    private int getAccountId(long playerId) {
        String sql = "SELECT account_id FROM player WHERE id=? LIMIT 1";
        try (Connection con = LocalManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("account_id");
                }
            }
        } catch (Exception ignored) {
        }
        return 0;
    }
}
