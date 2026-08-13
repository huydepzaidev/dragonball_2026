package nro.models.services_func;

import nro.models.player.Pet;
import nro.models.player.Player;
import nro.models.services.PetService;

public final class SecondDiscipleChangeItemRuleTest {

    private SecondDiscipleChangeItemRuleTest() {
    }

    public static void main(String[] args) {
        assertSecondDiscipleOwnershipRule();
        assertRerollAlwaysChangesType();
        System.out.println("SECOND_DISCIPLE_CHANGE_ITEM_RULE_TEST_OK");
    }

    private static void assertSecondDiscipleOwnershipRule() {
        Player player = new Player();
        if (PetService.isSecondDisciple(player)) {
            throw new AssertionError("A player without a disciple must be rejected");
        }

        player.pet = new Pet(player);
        for (byte type = 0; type <= 4; type++) {
            player.pet.typePet = type;
            boolean expected = type >= 2;
            if (PetService.isSecondDisciple(player) != expected) {
                throw new AssertionError("Unexpected ownership result for disciple type " + type);
            }
        }
    }

    private static void assertRerollAlwaysChangesType() {
        for (byte currentType = 2; currentType <= 4; currentType++) {
            for (int roll = 0; roll <= 1; roll++) {
                byte nextType = UseItem.chooseDifferentSecondDiscipleType(currentType, roll);
                if (!PetService.isSecondDiscipleType(nextType)) {
                    throw new AssertionError("Reroll returned an invalid second disciple type");
                }
                if (nextType == currentType) {
                    throw new AssertionError("Reroll must not return the current second disciple type");
                }
            }
        }
    }
}
