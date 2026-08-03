package nro.models.services;

import nro.models.item.Item;
import nro.models.player_system.Template;

public final class PermanentExpirationSmokeTest {

    private PermanentExpirationSmokeTest() {
    }

    public static void main(String[] args) {
        Template.ItemOptionTemplate expiration =
                new Template.ItemOptionTemplate(93, "Hạn sử dụng # ngày", 0);

        Item permanent = new Item();
        permanent.itemOptions.add(new Item.ItemOption(expiration, 0));
        ItemService.gI().normalizePermanentExpiration(permanent);
        if (!permanent.itemOptions.isEmpty()) {
            throw new AssertionError("Permanent item must not display option 93=0");
        }

        Item limited = new Item();
        limited.itemOptions.add(new Item.ItemOption(expiration, 1));
        ItemService.gI().normalizePermanentExpiration(limited);
        if (limited.itemOptions.size() != 1 || limited.itemOptions.get(0).param != 1) {
            throw new AssertionError("Limited item expiration must be preserved");
        }

        System.out.println("PERMANENT_EXPIRATION_SMOKE_TEST_OK");
    }
}
