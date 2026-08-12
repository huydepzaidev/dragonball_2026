package nro.models.server;

import nro.models.player.Player;
import nro.models.services.KOLQuestService;

public final class AdminKOLCommandTest {

    private AdminKOLCommandTest() {
    }

    public static void main(String[] args) {
        Player player = new Player();
        int expectedEventPoints = 0;

        for (int stage = 1; stage <= 6; stage++) {
            Command.completeAdminKOLQuest(player);
            expectedEventPoints += stage * 1_000;

            require(player.kolQuestStage == stage + 1,
                    "kol command did not advance stage " + stage);
            require(player.event.getEventPoint() == expectedEventPoints,
                    "kol command granted wrong reward at stage " + stage);
            require(KOLQuestService.gI().getProgress(player, stage)
                    == requiredQuantity(stage),
                    "kol command did not complete progress for stage " + stage);
        }

        Command.completeAdminKOLQuest(player);
        require(player.kolQuestStage == 7,
                "kol command advanced past the final stage");
        require(player.event.getEventPoint() == 21_000,
                "kol command rewarded after all stages were complete");

        System.out.println("ADMIN_KOL_COMMAND_OK points=21000 stage=7");
    }

    private static long requiredQuantity(int stage) {
        return switch (stage) {
            case KOLQuestService.STAGE_WOOD_DUMMY ->
                KOLQuestService.REQUIRED_WOOD_DUMMY;
            case KOLQuestService.STAGE_BIRD_DEMON ->
                KOLQuestService.REQUIRED_BIRD_DEMON;
            case KOLQuestService.STAGE_FIDE_WAVE ->
                KOLQuestService.REQUIRED_FIDE_WAVE;
            case KOLQuestService.STAGE_CHALLENGE ->
                KOLQuestService.REQUIRED_CHALLENGE;
            case KOLQuestService.STAGE_HARD_DAILY_TASK ->
                KOLQuestService.REQUIRED_HARD_DAILY_TASK;
            case KOLQuestService.STAGE_SUPER_BROLY ->
                KOLQuestService.REQUIRED_SUPER_BROLY;
            default -> 0;
        };
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
