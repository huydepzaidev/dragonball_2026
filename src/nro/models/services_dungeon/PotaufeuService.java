package nro.models.services_dungeon;

import nro.models.consts.ConstPlayer;
import nro.models.map.phoban.PotaufeuPolicy;
import nro.models.map.service.ChangeMapService;
import nro.models.player.Player;
import nro.models.services.PlayerService;
import nro.models.services.Service;
import nro.models.utils.Util;

public final class PotaufeuService {

    private static PotaufeuService instance;

    private PotaufeuService() {
    }

    public static PotaufeuService gI() {
        if (instance == null) {
            instance = new PotaufeuService();
        }
        return instance;
    }

    public boolean hasUsedDailyChallenge(Player player) {
        return player != null
                && player.lastPkCommesonTime > 0
                && !Util.isAfterMidnight(player.lastPkCommesonTime);
    }

    public void startChallenge(Player player) {
        player.lastPkCommesonTime = System.currentTimeMillis();
        player.potaufeuReturnHomeAt = 0;
        player.potaufeuReturnStarted = false;
    }

    public void finishChallenge(Player player) {
        if (player == null || player.zone == null || player.zone.map == null
                || player.zone.map.mapId != PotaufeuPolicy.MAP_ID) {
            return;
        }
        if (player.potaufeuReturnHomeAt == 0) {
            player.potaufeuReturnHomeAt
                    = System.currentTimeMillis() + PotaufeuPolicy.HOME_CAPSULE_DELAY_MS;
            player.potaufeuReturnStarted = false;
            Service.gI().sendThongBao(player, "Trận đấu đã kết thúc. Sau 1 phút capsule sẽ tự động đưa bạn về nhà.");
        }
        PlayerService.gI().changeAndSendTypePK(player, ConstPlayer.NON_PK);
    }

    public boolean isAutoReturnReady(Player player) {
        if (player == null || player.effectSkill == null || player.effectSkill.isPKCommeson
                || !hasUsedDailyChallenge(player)) {
            return false;
        }
        // A zero value occurs after reconnecting. The daily attempt is persisted,
        // so the player must still be able to leave without another fight.
        return player.potaufeuReturnHomeAt == 0
                || PotaufeuPolicy.isAutoReturnReady(
                        player.potaufeuReturnHomeAt, System.currentTimeMillis());
    }

    public int secondsUntilHomeCapsule(Player player) {
        if (player == null || player.potaufeuReturnHomeAt == 0) {
            return 0;
        }
        long remaining = player.potaufeuReturnHomeAt - System.currentTimeMillis();
        return (int) Math.max(0, (remaining + 999) / 1000);
    }

    public void update(Player player) {
        if (player == null || player.zone == null || player.zone.map == null
                || player.zone.map.mapId != PotaufeuPolicy.MAP_ID
                || player.potaufeuReturnStarted || !isAutoReturnReady(player)) {
            return;
        }
        returnHome(player);
    }

    public void returnHome(Player player) {
        if (!isAutoReturnReady(player)) {
            Service.gI().sendThongBao(player,
                    "Capsule sẽ tự động đưa bạn về nhà sau " + secondsUntilHomeCapsule(player) + " giây.");
            return;
        }
        player.potaufeuReturnStarted = true;
        player.mapBeforeCapsule = null;
        player.mapCapsule = null;
        Service.gI().sendThongBao(player, "Capsule đang đưa bạn về nhà.");
        ChangeMapService.gI().changeMapBySpaceShip(player, 21 + player.gender, -1, -1);
    }
}
