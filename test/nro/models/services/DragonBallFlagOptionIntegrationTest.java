package nro.models.services;

import nro.models.item.Item;
import nro.models.player_system.Template;
import nro.models.server.Manager;

public final class DragonBallFlagOptionIntegrationTest {

    private DragonBallFlagOptionIntegrationTest() {
    }

    public static void main(String[] args) {
        Manager.ITEM_OPTION_TEMPLATES.clear();
        for (int id = 0; id <= 231; id++) {
            Manager.ITEM_OPTION_TEMPLATES.add(new Template.ItemOptionTemplate(id, null, 0));
        }

        Item flag = item(2008);
        ItemService.gI().initDragonBallFlagOptionsIfEmpty(flag);
        if (flag.itemOptions.size() != 7) {
            throw new AssertionError();
        }

        ItemService.gI().initDragonBallFlagOptionsIfEmpty(flag);
        if (flag.itemOptions.size() != 7) {
            throw new AssertionError();
        }

        Item customFlag = item(2015);
        customFlag.itemOptions.add(new Item.ItemOption(73, 0));
        ItemService.gI().initDragonBallFlagOptionsIfEmpty(customFlag);
        if (customFlag.itemOptions.size() != 1
                || customFlag.itemOptions.get(0).optionTemplate.id != 73) {
            throw new AssertionError();
        }
    }

    private static Item item(int templateId) {
        Item item = new Item();
        item.template = new Template.ItemTemplate();
        item.template.id = (short) templateId;
        return item;
    }
}
