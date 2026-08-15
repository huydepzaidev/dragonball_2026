package nro.models.services_dungeon;

import nro.models.map.phoban.SnakeWay;
import nro.models.server.Manager;

/** Central policy for temporarily enabling or disabling Snake Way. */
public final class SnakeWayAvailability {

    public static final String CLOSED_MESSAGE = "Con đường rắn độc hiện đang tạm đóng.";
    private static final int FIRST_MAP_ID = 141;
    private static final int LAST_MAP_ID = 144;

    private SnakeWayAvailability() {
    }

    public static boolean isEnabled() {
        return Manager.SNAKE_WAY_ENABLED;
    }

    public static boolean isMap(int mapId) {
        return mapId >= FIRST_MAP_ID && mapId <= LAST_MAP_ID;
    }

    public static int availableZoneCount() {
        return isEnabled() ? SnakeWay.AVAILABLE : 0;
    }
}
