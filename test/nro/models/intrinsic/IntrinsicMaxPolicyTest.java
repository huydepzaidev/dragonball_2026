package nro.models.intrinsic;

import java.util.Arrays;
import java.util.List;

public final class IntrinsicMaxPolicyTest {

    private IntrinsicMaxPolicyTest() {
    }

    public static void main(String[] args) {
        Intrinsic inactive = intrinsic(0, 0, 0, 0, 0);
        Intrinsic earthSkill = intrinsic(1, 5, 25, 0, 0);
        Intrinsic duplicatedEarthSkill = intrinsic(1, 5, 25, 0, 0);
        Intrinsic sharedSkill = intrinsic(23, 25, 300, 0, 0);
        Intrinsic twoParamSkill = intrinsic(3, 10, 35, 10, 35);

        List<Intrinsic> eligible = IntrinsicMaxPolicy.getEligibleIntrinsics(
                Arrays.asList(inactive, earthSkill, duplicatedEarthSkill, sharedSkill, twoParamSkill));
        require(eligible.size() == 3, "Phải loại ID 0 và khử nội tại trùng ID.");
        require(eligible.get(0).id == 1 && eligible.get(1).id == 23 && eligible.get(2).id == 3,
                "Danh sách hợp lệ phải giữ thứ tự để random công bằng.");

        Intrinsic maxEarth = IntrinsicMaxPolicy.maximize(earthSkill);
        require(maxEarth != earthSkill, "Không được sửa trực tiếp template nội tại.");
        require(maxEarth.param1 == 25 && maxEarth.param2 == 0,
                "Nội tại một tham số phải đạt mức tối đa.");

        Intrinsic maxTwoParam = IntrinsicMaxPolicy.maximize(twoParamSkill);
        require(maxTwoParam.param1 == 35 && maxTwoParam.param2 == 35,
                "Cả hai tham số nội tại phải đạt mức tối đa.");

        require(!IntrinsicMaxPolicy.canOpen(IntrinsicMaxPolicy.MIN_POWER - 1, 500),
                "Thiếu sức mạnh phải bị từ chối.");
        require(!IntrinsicMaxPolicy.canOpen(IntrinsicMaxPolicy.MIN_POWER, 499),
                "Thiếu một hồng ngọc phải bị từ chối.");
        require(IntrinsicMaxPolicy.canOpen(IntrinsicMaxPolicy.MIN_POWER, 500),
                "Đủ đúng 500 hồng ngọc phải được mở.");
        System.out.println("IntrinsicMaxPolicyTest: OK");
    }

    private static Intrinsic intrinsic(int id, int from1, int to1, int from2, int to2) {
        Intrinsic intrinsic = new Intrinsic();
        intrinsic.id = id;
        intrinsic.name = "Test p0 đến p1 p2 đến p3";
        intrinsic.paramFrom1 = (short) from1;
        intrinsic.paramTo1 = (short) to1;
        intrinsic.paramFrom2 = (short) from2;
        intrinsic.paramTo2 = (short) to2;
        return intrinsic;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
