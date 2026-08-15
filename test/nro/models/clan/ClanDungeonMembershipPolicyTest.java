package nro.models.clan;

import nro.models.map.phoban.BanDoKhoBau;
import nro.models.map.phoban.RedRibbonHQ;

public final class ClanDungeonMembershipPolicyTest {

    private ClanDungeonMembershipPolicyTest() {
    }

    public static void main(String[] args) {
        require(!ClanDungeonMembershipPolicy.isRemovalLocked(false, false),
                "Bang không đi phó bản phải được phép thay đổi thành viên.");
        require(ClanDungeonMembershipPolicy.isRemovalLocked(true, false),
                "Doanh trại phải khóa rời, kích và giải tán bang.");
        require(ClanDungeonMembershipPolicy.isRemovalLocked(false, true),
                "Bản đồ kho báu phải khóa rời, kích và giải tán bang.");
        require(ClanDungeonMembershipPolicy.isRemovalLocked(true, true),
                "Trạng thái bất thường có hai phó bản vẫn phải khóa an toàn.");
        require("Doanh trại".equals(ClanDungeonMembershipPolicy.getActiveDungeonName(true, false)),
                "Phải báo đúng tên Doanh trại.");
        require("Bản đồ kho báu".equals(ClanDungeonMembershipPolicy.getActiveDungeonName(false, true)),
                "Phải báo đúng tên Bản đồ kho báu.");

        Clan clan = new Clan();
        require(!ClanDungeonMembershipPolicy.isRemovalLocked(clan),
                "Bang mới không được bị khóa thành viên.");
        clan.doanhTrai = new RedRibbonHQ(0);
        require(ClanDungeonMembershipPolicy.isRemovalLocked(clan),
                "Tham chiếu Doanh trại thực tế phải khóa thành viên.");
        clan.doanhTrai = null;
        clan.BanDoKhoBau = new BanDoKhoBau(0);
        require(ClanDungeonMembershipPolicy.isRemovalLocked(clan),
                "Tham chiếu Bản đồ kho báu thực tế phải khóa thành viên.");
        clan.BanDoKhoBau = null;
        require(!ClanDungeonMembershipPolicy.isRemovalLocked(clan),
                "Kết thúc phó bản phải mở lại thao tác thành viên.");
        System.out.println("ClanDungeonMembershipPolicyTest: OK");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
