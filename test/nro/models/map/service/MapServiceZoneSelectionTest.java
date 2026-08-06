package nro.models.map.service;

import java.util.Arrays;
import java.util.List;
import nro.models.map.Zone;

public final class MapServiceZoneSelectionTest {

    private MapServiceZoneSelectionTest() {
    }

    public static void main(String[] args) {
        Zone zone0 = new Zone(null, 0, 10);
        Zone zone1 = new Zone(null, 1, 10);
        Zone zone2 = new Zone(null, 2, 10);
        List<Zone> zones = Arrays.asList(zone0, zone1, zone2);

        assertSame(zone0, MapService.selectAutomaticZone(zones));

        addPlayers(zone0, Zone.PLAYERS_TIEU_CHUAN_TRONG_MAP);
        assertSame(zone1, MapService.selectAutomaticZone(zones));

        addPlayers(zone1, Zone.PLAYERS_TIEU_CHUAN_TRONG_MAP);
        assertSame(zone2, MapService.selectAutomaticZone(zones));

        addPlayers(zone2, Zone.PLAYERS_TIEU_CHUAN_TRONG_MAP);
        assertSame(zone0, MapService.selectAutomaticZone(zones));

        addPlayers(zone0, zone0.maxPlayer - zone0.getNumOfPlayers());
        assertSame(zone1, MapService.selectAutomaticZone(zones));

        addPlayers(zone1, zone1.maxPlayer - zone1.getNumOfPlayers());
        addPlayers(zone2, zone2.maxPlayer - zone2.getNumOfPlayers());
        assertSame(null, MapService.selectAutomaticZone(zones));

        System.out.println("MAP_ZONE_SELECTION_OK default=0 preferredCapacity=7 sequential=true");
    }

    private static void addPlayers(Zone zone, int count) {
        for (int i = 0; i < count; i++) {
            zone.getPlayers().add(null);
        }
    }

    private static void assertSame(Object expected, Object actual) {
        if (expected != actual) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }
}
