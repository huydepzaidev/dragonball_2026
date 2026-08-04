package nro.models.boss.Boss_Manager;

public class BrolyManager extends BossManager {

    public static final int ADMIN_BROLY_LIST_MENU_TYPE = 4;

    private static BrolyManager instance;

    public static BrolyManager gI() {
        if (instance == null) {
            instance = new BrolyManager();
        }
        return instance;
    }

    @Override
    protected int getAdminBossListMenuType() {
        return ADMIN_BROLY_LIST_MENU_TYPE;
    }

    @Override
    protected String getAdminBossListTitle() {
        return "Broly / Super Broly";
    }

}
