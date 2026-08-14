package nro.models.boss.Boss_mini;

import java.util.Arrays;
import java.util.List;
import nro.models.boss.BossID;
import nro.models.map.Zone;
import nro.models.player.Player;

public final class AnTromSpawnZoneTest {

    private AnTromSpawnZoneTest() {
    }

    public static void main(String[] args) {
        Zone freeOne = new Zone(null, 0, 15);
        Zone occupied = new Zone(null, 1, 15);
        Zone freeTwo = new Zone(null, 2, 15);

        Player anTrom = new Player();
        anTrom.id = BossID.AN_TROM;
        anTrom.isBoss = true;
        occupied.addPlayer(anTrom);

        require(AnTrom.containsAnTrom(occupied));
        require(!AnTrom.containsAnTrom(freeOne));
        require(!AnTrom.containsAnTrom(null));

        List<Zone> freeZones = AnTrom.collectFreeZones(
                Arrays.asList(freeOne, occupied, null, freeTwo));
        require(freeZones.size() == 2);
        require(freeZones.contains(freeOne));
        require(freeZones.contains(freeTwo));
        require(!freeZones.contains(occupied));
        require(AnTrom.collectFreeZones(null).isEmpty());
    }

    private static void require(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }
}