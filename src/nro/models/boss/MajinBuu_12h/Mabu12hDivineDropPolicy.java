package nro.models.boss.MajinBuu_12h;

import java.util.HashMap;
import java.util.Map;
import nro.models.boss.BossID;

final class Mabu12hDivineDropPolicy {

    private static final int[] TURN_BOSS_IDS = {
        BossID.DRABURA,
        BossID.BUI_BUI,
        BossID.BUI_BUI_2,
        BossID.YA_CON,
        BossID.DRABURA_2,
        BossID.GOKU,
        BossID.CADIC,
        BossID.MABU_12H,
        BossID.DRABURA_3
    };

    private final Map<Integer, TurnState> statesByZone = new HashMap<>();
    private long activeTurnId = Long.MIN_VALUE;

    synchronized boolean reserveDrop(long turnId, int zoneId, int bossId, int selectedBossIndex) {
        if (!isTurnBoss(bossId)) {
            return false;
        }
        if (activeTurnId != turnId) {
            activeTurnId = turnId;
            statesByZone.clear();
        }
        TurnState state = statesByZone.computeIfAbsent(zoneId,
                ignored -> new TurnState(TURN_BOSS_IDS[Math.floorMod(selectedBossIndex, TURN_BOSS_IDS.length)]));
        if (state.dropped) {
            return false;
        }
        // Mabư là mốc kết thúc phiên và rơi bù nếu boss được chọn chưa bị hạ.
        if (bossId == state.selectedBossId || bossId == BossID.MABU_12H) {
            state.dropped = true;
            return true;
        }
        return false;
    }

    static int bossCount() {
        return TURN_BOSS_IDS.length;
    }

    static int bossIdAt(int index) {
        return TURN_BOSS_IDS[index];
    }

    private static boolean isTurnBoss(int bossId) {
        for (int id : TURN_BOSS_IDS) {
            if (id == bossId) {
                return true;
            }
        }
        return false;
    }

    private static final class TurnState {

        private final int selectedBossId;
        private boolean dropped;

        private TurnState(int selectedBossId) {
            this.selectedBossId = selectedBossId;
        }
    }
}
