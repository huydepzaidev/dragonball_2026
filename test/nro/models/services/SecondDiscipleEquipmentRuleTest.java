package nro.models.services;

import nro.models.item.Item;
import nro.models.player.Pet;
import nro.models.player.Player;
import nro.models.player_system.Template;

public final class SecondDiscipleEquipmentRuleTest {

    private SecondDiscipleEquipmentRuleTest() {
    }

    public static void main(String[] args) {
        Pet pet = new Pet(new Player());
        for (int i = 0; i < 9; i++) {
            pet.inventory.itemsBody.add(new Item());
        }
        if (PetService.hasEquippedItems(pet)) {
            throw new AssertionError("An unequipped disciple must be replaceable");
        }

        Item costume = new Item();
        costume.template = new Template.ItemTemplate();
        pet.inventory.itemsBody.set(8, costume);
        if (!PetService.hasEquippedItems(pet)) {
            throw new AssertionError("Every occupied body slot must block replacement");
        }

        System.out.println("SECOND_DISCIPLE_EQUIPMENT_RULE_TEST_OK");
    }
}
