package nro.models.boss.Boss_mini;

import java.util.ArrayList;
import java.util.List;
import nro.models.boss.BossID;
import nro.models.map.Zone;
import nro.models.player.Player;

public final class MatTroiZoneSelectionTest {

    private MatTroiZoneSelectionTest() {
    }

    public static void main(String[] args) {
        List<Zone> zones = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            zones.add(new Zone(null, i, 10));
        }
        zones.get(0).getBosses().add(boss(-12345));
        assertSize(40, MatTroi.collectFreeZones(zones));
        for (int i = 0; i < 20; i++) {
            List<Zone> freeZones = MatTroi.collectFreeZones(zones);
            assertSize(40 - i, freeZones);
            freeZones.get(0).getBosses().add(boss(BossID.MAT_TROI));
        }
        assertSize(20, MatTroi.collectFreeZones(zones));
        for (Zone zone : zones) {
            if (!MatTroi.containsMatTroi(zone)) {
                zone.getBosses().add(boss(BossID.MAT_TROI));
            }
        }
        assertSize(0, MatTroi.collectFreeZones(zones));
    }

    private static Player boss(long id) {
        Player boss = new Player();
        boss.id = id;
        boss.isBoss = true;
        return boss;
    }

    private static void assertSize(int expected, List<Zone> zones) {
        if (zones.size() != expected) {
            throw new AssertionError();
        }
    }
}
