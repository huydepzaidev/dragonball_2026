package nro.models.player;

import nro.models.consts.ConstPlayer;
import nro.models.item.Item;

public final class SecondDiscipleRulesTest {

    private SecondDiscipleRulesTest() {
    }

    public static void main(String[] args) {
        Pet uub = createPet((byte) 2, ConstPlayer.TRAI_DAT);
        Pet beerus = createPet((byte) 3, ConstPlayer.XAYDA);
        Pet jiren = createPet((byte) 4, ConstPlayer.NAMEC);

        assertTransformation(uub, 946, 947, 948);
        assertTransformation(beerus, 1422, 1423, 1424);
        assertTransformation(jiren, 876, 877, 878);

        uub.nPoint.power = 59_999_999_999L;
        if (Pet.canOpenSecondDiscipleSkill5(uub)) {
            throw new AssertionError("Skill 5 must stay locked below 60 billion");
        }
        uub.nPoint.power = 60_000_000_000L;
        if (!Pet.canOpenSecondDiscipleSkill5(uub)) {
            throw new AssertionError("Skill 5 must unlock at 60 billion");
        }

        if (NPoint.calculateFusionPetContribution(uub, 50_000L) != 60_000L) {
            throw new AssertionError("Second disciple contribution must be 120%");
        }
        Pet normalPet = createPet((byte) 0, ConstPlayer.TRAI_DAT);
        if (NPoint.calculateFusionPetContribution(normalPet, 50_000L) != 50_000L) {
            throw new AssertionError("Normal disciple contribution must stay at 100%");
        }

        System.out.println("SECOND_DISCIPLE_RULES_TEST_OK");
    }

    private static Pet createPet(byte typePet, byte gender) {
        Pet pet = new Pet(new Player());
        pet.typePet = typePet;
        pet.gender = gender;
        pet.nPoint.power = 40_000_000_000L;
        for (int i = 0; i < 9; i++) {
            pet.inventory.itemsBody.add(new Item());
        }
        return pet;
    }

    private static void assertTransformation(Pet pet, int normalHead, int normalBody, int normalLeg) {
        if (pet.getAvatar() != normalHead || pet.getHead() != normalHead
                || pet.getBody() != normalBody || pet.getLeg() != normalLeg) {
            throw new AssertionError("Unexpected normal appearance for pet type " + pet.typePet);
        }
        pet.isTransform = true;
        if (pet.getHead() == normalHead || pet.getBody() == normalBody) {
            throw new AssertionError("Transformation must visibly change pet type " + pet.typePet);
        }
    }
}
