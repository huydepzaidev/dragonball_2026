package nro.models.services;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import nro.models.data.LocalManager;

/** Verifies that the deployed activation service reads the canonical DB weights. */
public final class ActivationRewardDatabaseSmokeTest {

    private static final int[][] EXPECTED_OPTIONS = {
        {127, 128, 129, 233, 245},
        {130, 131, 132, 233, 237},
        {133, 135, 134, 233, 241}
    };
    private static final int[][] EXPECTED_WEIGHTS = {
        {20, 120, 20, 120, 20},
        {120, 20, 20, 120, 20},
        {20, 20, 120, 120, 20}
    };

    private ActivationRewardDatabaseSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        try (Connection con = LocalManager.getConnection();
                PreparedStatement ps = con.prepareStatement(
                        "SELECT COUNT(*) FROM activation_reward_config WHERE enabled=1");
                ResultSet rs = ps.executeQuery()) {
            if (!rs.next() || rs.getInt(1) != 3) {
                throw new AssertionError("DB phải có đúng 3 cấu hình kích hoạt đang bật.");
            }
        }

        Method loader = ActivationRewardService.class.getDeclaredMethod("loadConfig", int.class);
        loader.setAccessible(true);
        for (int planet = 0; planet < 3; planet++) {
            ActivationRewardService.ActivationConfig config
                    = (ActivationRewardService.ActivationConfig) loader.invoke(
                            ActivationRewardService.gI(), planet);
            if (!Arrays.equals(config.optionIds, EXPECTED_OPTIONS[planet])) {
                throw new AssertionError("Sai pool DB hành tinh " + planet);
            }
            if (!Arrays.equals(config.weights, EXPECTED_WEIGHTS[planet])) {
                throw new AssertionError("Sai trọng số DB hành tinh " + planet);
            }
        }
        System.out.println("ACTIVATION_REWARD_DATABASE_SMOKE_OK rows=3 containers=1538,1559");
        LocalManager.close();
    }
}
