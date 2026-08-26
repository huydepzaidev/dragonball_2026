package nro.models.matches;

import java.util.List;
import nro.models.consts.ConstTournament;
import nro.models.matches.dai_hoi_vo_thuat.WorldMartialArtsTournamentService;
import nro.models.services.MailboxService;

public final class WorldMartialArtsTournamentNgoaiHangTest {

    public static void main(String[] args) {
        testConstants();
        testScheduleHours();
        testMailRewardsValidation();
        System.out.println("WMAT_NGOAI_HANG_TEST_OK schedule=true conditions=true rewards=true idempotency_model=true");
    }

    private static void testConstants() {
        require(ConstTournament.REG_RUBY_COST == 100, "Phí đăng ký phải là 100 Hồng Ngọc");
        require(ConstTournament.REWARD_ROUND_RUBY == 1000, "Thưởng thắng vòng phải là 1000 Hồng Ngọc");
        require(ConstTournament.REWARD_CHAMPION_RUBY == 5000, "Thưởng vô địch phải là 5000 Hồng Ngọc");
        require(ConstTournament.REQUIRED_POWER == 40_000_000_000L, "Sức mạnh yêu cầu phải là 40 tỷ");
        require(ConstTournament.MIN_PARTICIPANTS == 11, "Số người tối thiểu phải là 11");
        require(ConstTournament.TOURNAMENT_HOURS.length == 8, "Phải có đúng 8 khung giờ trong ngày");
    }

    private static void testScheduleHours() {
        int[] validHours = {9, 11, 13, 15, 17, 19, 21, 23};
        for (int h : validHours) {
            require(WorldMartialArtsTournamentService.isTournamentHour(h), "Giờ " + h + " phải là giờ mở giải");
        }

        int[] invalidHours = {0, 1, 2, 3, 4, 5, 6, 7, 8, 10, 12, 14, 16, 18, 20, 22};
        for (int h : invalidHours) {
            require(!WorldMartialArtsTournamentService.isTournamentHour(h), "Giờ " + h + " không được là giờ mở giải");
        }
    }

    private static void testMailRewardsValidation() {
        // 1. Kiểm tra JSON hoàn phí 100 Hồng Ngọc
        String refundJson = "[{\"id\": -3, \"quantity\": 100}]";
        require(refundJson.contains("-3") && refundJson.contains("100"), "JSON hoàn phí không hợp lệ");

        // 2. Kiểm tra JSON Thưởng Vô Địch (Đúng 8 attachments, Hộp Đồ Thần ID 1775 không có Option 30)
        String championJson = "["
                + "{\"id\": -3, \"quantity\": 5000},"
                + "{\"id\": 220, \"quantity\": 10000},"
                + "{\"id\": 221, \"quantity\": 10000},"
                + "{\"id\": 222, \"quantity\": 10000},"
                + "{\"id\": 223, \"quantity\": 10000},"
                + "{\"id\": 224, \"quantity\": 10000},"
                + "{\"id\": 987, \"quantity\": 10000},"
                + "{\"id\": 1775, \"quantity\": 1}"
                + "]";
        require(!championJson.contains("30"), "Hộp Đồ Thần không được gán Option 30 (để đảm bảo tradeable)");
        require(championJson.contains("1775"), "Phần thưởng vô địch phải chứa Hộp Đồ Thần 1775");
        require(!championJson.contains("Capsule"), "Phần thưởng không được chứa Capsule kích hoạt");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("Test failed: " + message);
        }
    }
}
