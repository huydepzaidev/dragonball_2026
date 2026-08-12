package nro.models.mob;

/**
 * Per-zone kill progress. Pending super mobs count as occupied slots so a
 * burst of kills cannot reserve more super mobs than the zone allows.
 */
public final class NaturalSuperMobSpawnState {

    private int eligibleNormalKills;

    public synchronized boolean recordEligibleNormalKill(int playerCount, int occupiedSlots) {
        if (playerCount <= 0) {
            return false;
        }

        int requiredKills = NaturalSuperMobPolicy.requiredNormalKills(playerCount);
        eligibleNormalKills = Math.min(requiredKills, eligibleNormalKills + 1);

        if (eligibleNormalKills < requiredKills
                || occupiedSlots >= NaturalSuperMobPolicy.maxConcurrentSupers(playerCount)) {
            return false;
        }

        eligibleNormalKills = 0;
        return true;
    }

    public synchronized int getEligibleNormalKills() {
        return eligibleNormalKills;
    }
}
