package nro.models.services;

import nro.models.item.Item;
import nro.models.player.NewSkill;
import nro.models.player.Pet;
import nro.models.player.Player;
import nro.models.player_system.Template;

public final class SecondDiscipleEquipmentSlotTest {

    private SecondDiscipleEquipmentSlotTest() {
    }

    public static void main(String[] args) {
        testBodySlotMapping();
        testSecondDiscipleEligibilityUsesTypePet();
        testLegacyAccessoryMigration();
        testLegacyMigrationDoesNotOverwriteOccupiedSlot();
        testSpecialSkillBookStaysInSlotEight();
        System.out.println("SECOND_DISCIPLE_EQUIPMENT_SLOT_TEST_OK");
    }

    private static void testBodySlotMapping() {
        Player master = new Player();
        check(InventoryService.resolveBodySlot(master, (byte) 11) == 8);
        check(InventoryService.resolveBodySlot(master, (byte) 25) == 10);
        Pet pet = createPet((byte) 4);
        check(InventoryService.resolveBodySlot(pet, (byte) 11) == 7);
        check(InventoryService.resolveBodySlot(pet, (byte) 25) == 8);
    }

    private static void testSecondDiscipleEligibilityUsesTypePet() {
        Pet secondDisciple = createPet((byte) 4);
        check(secondDisciple.type == 0);
        check(InventoryService.canPetEquipType(secondDisciple, (byte) 11));
        check(InventoryService.canPetEquipType(secondDisciple, (byte) 25));
        check(!InventoryService.canPetEquipType(secondDisciple, (byte) 27));

        Pet normalDisciple = createPet((byte) 0);
        check(!InventoryService.canPetEquipType(normalDisciple, (byte) 11));
        check(!InventoryService.canPetEquipType(normalDisciple, (byte) 25));
    }

    private static void testLegacyAccessoryMigration() {
        Pet pet = createPet((byte) 4);
        Item accessory = createItem(766, (byte) 11, 42);
        pet.inventory.itemsBody.set(8, accessory);

        check(InventoryService.normalizeLegacySecondDiscipleEquipmentSlots(pet));
        check(pet.inventory.itemsBody.get(7) == accessory);
        check(!pet.inventory.itemsBody.get(8).isNotNullItem());
        check(pet.getFlagBag() == 42);
    }

    private static void testLegacyMigrationDoesNotOverwriteOccupiedSlot() {
        Pet pet = createPet((byte) 4);
        Item currentAccessory = createItem(767, (byte) 11, 43);
        Item legacyAccessory = createItem(768, (byte) 11, 44);
        pet.inventory.itemsBody.set(7, currentAccessory);
        pet.inventory.itemsBody.set(8, legacyAccessory);

        check(!InventoryService.normalizeLegacySecondDiscipleEquipmentSlots(pet));
        check(pet.inventory.itemsBody.get(7) == currentAccessory);
        check(pet.inventory.itemsBody.get(8) == legacyAccessory);
    }

    private static void testSpecialSkillBookStaysInSlotEight() {
        Pet pet = createPet((byte) 4);
        Item book = createItem(1212, (byte) 25, -1);
        pet.inventory.itemsBody.set(8, book);

        check(!InventoryService.normalizeLegacySecondDiscipleEquipmentSlots(pet));
        check(pet.inventory.itemsBody.get(8) == book);
        check(new NewSkill(pet).getTypePaint() == 2);

        pet.inventory.itemsBody.set(8, createItem(1280, (byte) 25, -1));
        check(new NewSkill(pet).getTypePaint() == 3);
    }

    private static Pet createPet(byte typePet) {
        Pet pet = new Pet(new Player());
        pet.typePet = typePet;
        for (int i = 0; i < 9; i++) {
            pet.inventory.itemsBody.add(new Item());
        }
        return pet;
    }

    private static Item createItem(int id, byte type, int part) {
        Item item = new Item();
        item.template = new Template.ItemTemplate();
        item.template.id = (short) id;
        item.template.type = type;
        item.template.part = (short) part;
        item.quantity = 1;
        return item;
    }

    private static void check(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }
}
