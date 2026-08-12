package nro.models.player;

import nro.models.item.Item;
import nro.models.player_system.Template;

public final class NPointPicoloSetKiBonusTest {

    private NPointPicoloSetKiBonusTest() {
    }

    public static void main(String[] args) {
        require(NPoint.applyPicoloSetKiBonus(1_000L, 5) == 1_500L);
        require(NPoint.applyPicoloSetKiBonus(1_000L, 4) == 1_000L);
        require(NPoint.applyPicoloSetKiBonus(1_000L, 0) == 1_000L);

        Player player = new Player();
        for (int i = 0; i < 5; i++) {
            Item item = new Item();
            item.template = new Template.ItemTemplate();
            item.template.id = (short) (10 + i);
            item.itemOptions.add(new Item.ItemOption(
                    new Template.ItemOptionTemplate(130, null, 0), 1));
            item.itemOptions.add(new Item.ItemOption(
                    new Template.ItemOptionTemplate(142, null, 0), 1));
            player.inventory.itemsBody.add(item);
        }
        player.inventory.itemsBody.add(new Item());
        player.setClothes.setup();

        require(player.setClothes.picolo == 5);
        require(NPoint.applyPicoloSetKiBonus(1_600L, player.setClothes.picolo) == 2_400L);
    }

    private static void require(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }
}
