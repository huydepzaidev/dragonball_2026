package nro.models.services_dungeon;

import java.io.FileInputStream;
import java.util.Properties;
import nro.models.map.phoban.SnakeWay;
import nro.models.server.Manager;

public final class SnakeWayAvailabilityTest {

    private SnakeWayAvailabilityTest() {
    }

    public static void main(String[] args) throws Exception {
        boolean original = Manager.SNAKE_WAY_ENABLED;
        try {
            Manager.SNAKE_WAY_ENABLED = false;
            require(!SnakeWayAvailability.isEnabled(), "Snake Way phải tắt theo feature flag.");
            require(SnakeWayAvailability.availableZoneCount() == 0,
                    "Không được tạo zone Snake Way khi feature flag tắt.");

            for (int mapId = 141; mapId <= 144; mapId++) {
                require(SnakeWayAvailability.isMap(mapId), "Thiếu map Snake Way: " + mapId);
            }
            require(!SnakeWayAvailability.isMap(140), "Map 140 không thuộc Snake Way.");
            require(!SnakeWayAvailability.isMap(145), "Map 145 không thuộc Snake Way.");

            Manager.SNAKE_WAY_ENABLED = true;
            require(SnakeWayAvailability.availableZoneCount() == SnakeWay.AVAILABLE,
                    "Bật lại phải khôi phục đủ zone Snake Way.");

            Properties properties = new Properties();
            try (FileInputStream input = new FileInputStream("Config.properties")) {
                properties.load(input);
            }
            require("false".equalsIgnoreCase(properties.getProperty("server.snakeway.enabled")),
                    "Config.properties phải tạm tắt Snake Way.");
        } finally {
            Manager.SNAKE_WAY_ENABLED = original;
        }

        System.out.println("SnakeWayAvailabilityTest: OK");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
