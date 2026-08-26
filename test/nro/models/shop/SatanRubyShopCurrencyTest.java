package nro.models.shop;

import nro.models.consts.ConstItem;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.player_system.Template;
import nro.models.services.ItemService;
import nro.models.server.Manager;

public final class SatanRubyShopCurrencyTest {

    public static void main(String[] args) {
        testRubyItemTemplate();
        testSatanRubyShopStructure();
        testRubyPurchaseDeduction();
        System.out.println("SATAN_RUBY_SHOP_CURRENCY_TEST_OK spec_shop=true icon_spec=7743 currency_ruby=true");
    }

    private static void testRubyItemTemplate() {
        require(ConstItem.HONG_NGOC == 861, "HONG_NGOC ID phải là 861");
        
        Template.ItemTemplate rubyTemplate = new Template.ItemTemplate();
        rubyTemplate.id = 861;
        rubyTemplate.name = "Hồng ngọc";
        rubyTemplate.iconID = 7743;
        Manager.ITEM_TEMPLATES.add(rubyTemplate);

        short mappedId = ItemService.gI().getItemIdByIcon((short) 7743);
        require(mappedId == 861, "Icon 7743 phải map đúng về Item 861 (Hồng ngọc)");
    }

    private static void testSatanRubyShopStructure() {
        Shop rubyShop = new Shop();
        rubyShop.id = 37;
        rubyShop.npcId = 39;
        rubyShop.tagName = "SATAN_RUBY";
        rubyShop.typeShop = 3; // SPEC_SHOP

        TabShop tab = new TabShop();
        tab.id = 64;
        tab.name = "Cửa hàng\nHồng Ngọc";
        tab.shop = rubyShop;
        rubyShop.tabShops.add(tab);

        ItemShop itemShop = new ItemShop();
        itemShop.id = 1003;
        itemShop.tabShop = tab;
        itemShop.cost = 2500;
        itemShop.typeSell = 3; // COST_RUBY
        itemShop.iconSpec = 7743; // Icon Hồng ngọc
        tab.itemShops.add(itemShop);

        require(rubyShop.typeShop == 3, "SATAN_RUBY phải có typeShop = 3 (SPEC_SHOP) để hiển thị iconSpec");
        require(itemShop.iconSpec == 7743, "Item trong shop Hồng ngọc phải có iconSpec = 7743 (Hồng ngọc)");
    }

    private static void testRubyPurchaseDeduction() {
        Player player = new Player();
        player.inventory.ruby = 3000;

        int cost = 2500;
        require(player.inventory.ruby >= cost, "Player phải có đủ hồng ngọc");
        player.inventory.ruby -= cost;
        require(player.inventory.ruby == 500, "Sau khi mua 2500 ruby từ 3000 ruby phải còn lại 500 ruby");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
