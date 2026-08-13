package nro.models.services_func;

import nro.models.item.Item;
import nro.models.player_system.Template;

public final class SecondDiscipleKilisRuleTest {

    private SecondDiscipleKilisRuleTest() {
    }

    public static void main(String[] args) {
        Item item = new Item();
        Template.ItemOptionTemplate kilis = new Template.ItemOptionTemplate(250, "Kilis #", 0);
        item.itemOptions.add(new Item.ItemOption(kilis, 2_999));

        if (UseItem.hasRequiredSecondDiscipleKilis(item)) {
            throw new AssertionError("2.999 Kilis must be rejected");
        }

        item.itemOptions.get(0).param = 3_000;
        if (!UseItem.hasRequiredSecondDiscipleKilis(item)) {
            throw new AssertionError("3.000 Kilis must be accepted");
        }

        System.out.println("SECOND_DISCIPLE_KILIS_RULE_TEST_OK");
    }
}
