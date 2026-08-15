package nro.models.clan;

/**
 * Keeps clan membership stable while a clan dungeon is being opened, played,
 * or disposed. Adding members is intentionally not restricted by this policy.
 */
public final class ClanDungeonMembershipPolicy {

    private ClanDungeonMembershipPolicy() {
    }

    public static boolean isRemovalLocked(Clan clan) {
        return clan != null && isRemovalLocked(clan.doanhTrai != null, clan.BanDoKhoBau != null);
    }

    public static boolean isRemovalLocked(boolean redRibbonAssigned, boolean treasureMapAssigned) {
        return redRibbonAssigned || treasureMapAssigned;
    }

    public static String getActiveDungeonName(Clan clan) {
        if (clan == null) {
            return "phó bản bang hội";
        }
        return getActiveDungeonName(clan.doanhTrai != null, clan.BanDoKhoBau != null);
    }

    public static String getActiveDungeonName(boolean redRibbonAssigned, boolean treasureMapAssigned) {
        if (redRibbonAssigned && treasureMapAssigned) {
            return "Doanh trại và Bản đồ kho báu";
        }
        if (redRibbonAssigned) {
            return "Doanh trại";
        }
        if (treasureMapAssigned) {
            return "Bản đồ kho báu";
        }
        return "phó bản bang hội";
    }
}
