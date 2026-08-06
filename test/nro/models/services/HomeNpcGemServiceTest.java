package nro.models.services;

import nro.models.player.Player;

public final class HomeNpcGemServiceTest {

    private HomeNpcGemServiceTest() {
    }

    public static void main(String[] args) {
        Player player = new Player();

        player.inventory.gem = 0;
        HomeNpcGemService.claimFreeGems(player);
        assertGemBalance(player, HomeNpcGemService.FREE_GEM_BALANCE,
                "first claim");

        player.inventory.gem = HomeNpcGemService.CLAIM_THRESHOLD + 1;
        HomeNpcGemService.claimFreeGems(player);
        assertGemBalance(player, HomeNpcGemService.CLAIM_THRESHOLD + 1,
                "claim above threshold");

        player.inventory.gem = HomeNpcGemService.CLAIM_THRESHOLD;
        HomeNpcGemService.claimFreeGems(player);
        assertGemBalance(player, HomeNpcGemService.FREE_GEM_BALANCE,
                "repeat claim at threshold");

        System.out.println("HOME_NPC_FREE_GEMS_OK");
    }

    private static void assertGemBalance(Player player, int expected, String scenario) {
        if (player.inventory.gem != expected) {
            throw new AssertionError(scenario + ": expected " + expected
                    + " gems but got " + player.inventory.gem);
        }
    }
}
