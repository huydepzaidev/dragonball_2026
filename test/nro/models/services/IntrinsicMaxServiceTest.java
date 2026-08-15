package nro.models.services;

import java.util.ArrayList;
import java.util.List;
import nro.models.intrinsic.Intrinsic;
import nro.models.intrinsic.IntrinsicMaxPolicy;
import nro.models.player.Player;
import nro.models.server.Manager;

public final class IntrinsicMaxServiceTest {

    private IntrinsicMaxServiceTest() {
    }

    public static void main(String[] args) {
        List<Intrinsic> original = new ArrayList<>(Manager.INTRINSIC_TD);
        try {
            Manager.INTRINSIC_TD.clear();
            Manager.INTRINSIC_TD.add(intrinsic(0, 0, 0, 0, 0));
            Manager.INTRINSIC_TD.add(intrinsic(3, 10, 35, 10, 35));
            Manager.INTRINSIC_TD.add(intrinsic(3, 10, 35, 10, 35));

            Player insufficient = player(499, IntrinsicMaxPolicy.MIN_POWER);
            Intrinsic previous = intrinsic(1, 5, 25, 0, 0);
            insufficient.playerIntrinsic.intrinsic = previous;
            IntrinsicService.gI().openMax(insufficient);
            require(insufficient.inventory.ruby == 499,
                    "Thiếu hồng ngọc không được trừ tiền.");
            require(insufficient.playerIntrinsic.intrinsic == previous,
                    "Thiếu hồng ngọc không được đổi nội tại.");

            Player success = player(500, IntrinsicMaxPolicy.MIN_POWER);
            success.playerIntrinsic.countOpen = 6;
            IntrinsicService.gI().openMax(success);
            require(success.inventory.ruby == 0,
                    "Mở MAX phải trừ đúng 500 hồng ngọc.");
            require(success.playerIntrinsic.intrinsic.id == 3,
                    "Phải nhận nội tại hợp lệ thay vì ID 0.");
            require(success.playerIntrinsic.intrinsic.param1 == 35
                    && success.playerIntrinsic.intrinsic.param2 == 35,
                    "Mọi tham số phải đạt mức tối đa.");
            require(success.playerIntrinsic.countOpen == 6,
                    "Mở MAX không được thay đổi giá quay bằng vàng.");
        } finally {
            Manager.INTRINSIC_TD.clear();
            Manager.INTRINSIC_TD.addAll(original);
        }
        System.out.println("IntrinsicMaxServiceTest: OK");
    }

    private static Player player(int ruby, long power) {
        Player player = new Player();
        player.gender = 0;
        player.inventory.ruby = ruby;
        player.nPoint.power = power;
        return player;
    }

    private static Intrinsic intrinsic(int id, int from1, int to1, int from2, int to2) {
        Intrinsic intrinsic = new Intrinsic();
        intrinsic.id = id;
        intrinsic.name = "Test +p0% đến p1% -p2% đến p3%";
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
