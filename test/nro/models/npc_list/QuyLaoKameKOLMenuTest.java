package nro.models.npc_list;

import nro.models.player.Player;
import nro.models.player.PlayerEvent;
import nro.models.services.KOLQuestService;

public final class QuyLaoKameKOLMenuTest {

    private QuyLaoKameKOLMenuTest() {
    }

    public static void main(String[] args) {
        require(QuyLaoKame.kolBaseMenuLabel(false).equals(\u0022Nhiệm vụ\nKOL\u0022));
        require(QuyLaoKame.kolBaseMenuLabel(true).equals(\u0022Nhận quà\u0022));
        require(!QuyLaoKame.isKOLQuestComplete(9, 10));
        require(QuyLaoKame.isKOLQuestComplete(10, 10));
        require(QuyLaoKame.isKOLQuestComplete(11, 10));
        PlayerEvent event = new PlayerEvent(null);
        for (int stage = 1; stage <= 6; stage++) {
            int reward = QuyLaoKame.kolEventPointReward(stage);
            require(reward == stage * 1_000);
            event.addEventPoint(reward);
        }
        require(event.getEventPoint() == 21_000);
        require(event.trySpendEventPoint(6_000));
        require(event.getEventPoint() == 15_000);
        require(!event.trySpendEventPoint(16_000));
        require(QuyLaoKame.kolEventPointReward(0) == 0);
        require(QuyLaoKame.kolEventPointReward(7) == 0);

        Player npcPlayer = new Player();
        require(KOLQuestService.gI().claimCurrentQuestReward(npcPlayer) == 0);
        npcPlayer.kolWoodDummyAutoTrainKills = KOLQuestService.REQUIRED_WOOD_DUMMY;
        require(KOLQuestService.gI().claimCurrentQuestReward(npcPlayer) == 1_000);
        require(npcPlayer.kolQuestStage == KOLQuestService.STAGE_BIRD_DEMON);
        require(npcPlayer.event.getEventPoint() == 1_000);
        require(KOLQuestService.gI().claimCurrentQuestReward(npcPlayer) == 0);
    }

    private static void require(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }
}
