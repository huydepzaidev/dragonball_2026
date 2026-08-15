package nro.models.services_dungeon;

import java.io.FileInputStream;
import java.util.Properties;
import nro.models.server.Manager;

public final class RedRibbonHQRewardPolicyTest {

    private RedRibbonHQRewardPolicyTest() {
    }

    public static void main(String[] args) throws Exception {
        boolean original = Manager.RED_RIBBON_CAU_VANG_ENABLED;
        try {
            Manager.RED_RIBBON_CAU_VANG_ENABLED = false;
            require(!RedRibbonHQRewardPolicy.isCauVangEnabled(),
                    "Cậu Vàng phải tắt khi resource chưa hoàn chỉnh.");
            RedRibbonHQRewardPolicy.dropCauVang(null, null, 0, 0);
            require(RedRibbonHQRewardPolicy.CAU_VANG_ITEM_ID == 1824,
                    "Policy phải chặn đúng item Cậu Vàng mà boss đang thả.");

            Manager.RED_RIBBON_CAU_VANG_ENABLED = true;
            require(RedRibbonHQRewardPolicy.isCauVangEnabled(),
                    "Policy phải cho phép bật lại sau khi cài đủ resource.");

            Properties properties = new Properties();
            try (FileInputStream input = new FileInputStream("Config.properties")) {
                properties.load(input);
            }
            require("false".equalsIgnoreCase(
                    properties.getProperty("server.redribbon.cauvang.enabled")),
                    "Config.properties phải tắt drop Cậu Vàng.");
        } finally {
            Manager.RED_RIBBON_CAU_VANG_ENABLED = original;
        }

        System.out.println("RedRibbonHQRewardPolicyTest: OK");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
