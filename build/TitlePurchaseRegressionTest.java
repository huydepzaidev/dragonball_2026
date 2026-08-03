import java.lang.reflect.Method;
import nro.models.player.Player;
import nro.models.player_badges.BagesTemplate;
import nro.models.server.Manager;
import nro.models.shop.ItemShop;
import nro.models.shop.ShopService;
import nro.models.player_system.Template;

public final class TitlePurchaseRegressionTest {
    private static final Method BUY;

    static {
        try {
            BUY = ShopService.class.getDeclaredMethod("buyDanhHieu", Player.class, ItemShop.class);
            BUY.setAccessible(true);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static ItemShop title(short itemId, short effectId, int price) {
        ItemShop item = new ItemShop();
        item.temp = new Template.ItemTemplate(itemId, (byte) 36, (byte) 3,
                "Test title " + itemId, "", (short) 0, effectId, false, 0);
        item.typeSell = 1;
        item.cost = price;
        return item;
    }

    private static void buy(Player player, ItemShop item) throws Exception {
        BUY.invoke(ShopService.gI(), player, item);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) throws Exception {
        Manager.BAGES_TEMPLATES.clear();

        ItemShop configured = title((short) 1297, (short) 226, 2);
        BagesTemplate configuredTemplate = new BagesTemplate();
        configuredTemplate.id = 1;
        configuredTemplate.idItem = 1297;
        configuredTemplate.idEffect = 226;
        configuredTemplate.NAME = configured.temp.name;
        Manager.BAGES_TEMPLATES.add(configuredTemplate);

        Player buyer = new Player();
        buyer.inventory.gem = 10;
        buy(buyer, configured);
        check(buyer.inventory.gem == 8, "first purchase must deduct exactly 2 gems");
        check(buyer.dataBadges.size() == 1, "first purchase must add exactly one title");
        check(buyer.dataBadges.get(0).idBadGes == 226 && buyer.dataBadges.get(0).isUse,
                "purchased title must be active");

        buy(buyer, configured);
        check(buyer.inventory.gem == 8, "duplicate purchase must not deduct gems");
        check(buyer.dataBadges.size() == 1, "duplicate purchase must not add another title");

        Player poorBuyer = new Player();
        poorBuyer.inventory.gem = 1;
        buy(poorBuyer, configured);
        check(poorBuyer.inventory.gem == 1, "failed purchase must not deduct gems");
        check(poorBuyer.dataBadges.isEmpty(), "failed purchase must not grant a title");

        ItemShop missingDataLink = title((short) 1286, (short) 215, 2);
        Player fallbackBuyer = new Player();
        fallbackBuyer.inventory.gem = 5;
        buy(fallbackBuyer, missingDataLink);
        check(fallbackBuyer.inventory.gem == 3, "fallback purchase must deduct configured price");
        check(fallbackBuyer.dataBadges.size() == 1
                        && fallbackBuyer.dataBadges.get(0).idBadGes == 215,
                "fallback purchase must grant the shop title");
        check(BagesTemplate.fineBadgesbyIdItem(1286) != null,
                "missing data_badges link must be registered once");

        System.out.println("TITLE_PURCHASE_REGRESSION_OK");
    }
}
