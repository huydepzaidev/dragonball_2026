package nro.models.shop;

import java.lang.reflect.Method;
import nro.models.player.Player;
import nro.models.player_badges.BadgesData;
import nro.models.player_badges.BagesTemplate;
import nro.models.player_system.Template;
import nro.models.server.Manager;

public final class OwnedTitleUseSmokeTest {

    private static final Method CHANGE_TITLE;

    static {
        try {
            CHANGE_TITLE = ShopService.class.getDeclaredMethod(
                    "changeDanhHieu", Player.class, ItemShop.class);
            CHANGE_TITLE.setAccessible(true);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static ItemShop title(TabShop tab, short itemId, short effectId, int cost) {
        ItemShop item = new ItemShop();
        item.tabShop = tab;
        item.temp = new Template.ItemTemplate(itemId, (byte) 36, (byte) 3,
                "Test title " + itemId, "", (short) 0, effectId, false, 0);
        item.typeSell = 1;
        item.cost = cost;
        return item;
    }

    private static void addTemplate(int id, int itemId, int effectId) {
        BagesTemplate template = new BagesTemplate();
        template.id = id;
        template.idItem = itemId;
        template.idEffect = effectId;
        template.NAME = "Test title " + itemId;
        Manager.BAGES_TEMPLATES.add(template);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) throws Exception {
        Manager.BAGES_TEMPLATES.clear();
        addTemplate(1, 1297, 226);
        addTemplate(2, 1286, 215);

        TabShop sourceTab = new TabShop();
        sourceTab.id = 45;
        sourceTab.name = "Owned\n";
        sourceTab.itemShops.add(title(sourceTab, (short) 1297, (short) 226, 2));
        sourceTab.itemShops.add(title(sourceTab, (short) 1286, (short) 215, 2));

        Player player = new Player();
        player.dataBadges.add(new BadgesData(226, Long.MAX_VALUE, true));
        player.dataBadges.add(new BadgesData(215, Long.MAX_VALUE, false));
        player.badges.idBadges = 226;

        TabShopSoHuu ownedTab = new TabShopSoHuu(sourceTab, player);
        check(ownedTab.itemShops.size() == 2, "owned tab must contain both owned titles");
        check(ownedTab.itemShops.get(0).cost == 0 && ownedTab.itemShops.get(1).cost == 0,
                "owned titles must use zero cost so the client shows only Use");
        check(sourceTab.itemShops.get(0).cost == 2 && sourceTab.itemShops.get(1).cost == 2,
                "owned-tab rendering must not change the purchase price");

        ItemShop selected = ownedTab.itemShops.stream()
                .filter(item -> item.temp.id == 1286)
                .findFirst()
                .orElseThrow();
        CHANGE_TITLE.invoke(ShopService.gI(), player, selected);

        check(player.badges.idBadges == 215,
                "using an owned title must update the active effect immediately");
        check(!player.dataBadges.get(0).isUse && player.dataBadges.get(1).isUse,
                "using an owned title must deactivate the old title and activate the selected title");

        System.out.println("OWNED_TITLE_USE_SMOKE_TEST_OK");
    }
}
