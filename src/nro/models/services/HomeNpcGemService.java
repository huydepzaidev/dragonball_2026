package nro.models.services;

import nro.models.player.Player;

public final class HomeNpcGemService {

    public static final int CLAIM_THRESHOLD = 1_000_000;
    public static final int FREE_GEM_BALANCE = 10_000_000;

    private HomeNpcGemService() {
    }

    public static void claimFreeGems(Player player) {
        if (player == null || player.inventory == null) {
            return;
        }
        if (player.inventory.gem > CLAIM_THRESHOLD) {
            Service.gI().sendThongBao(player,
                    "Hãy sử dụng ngọc xanh xuống còn 1 triệu hoặc ít hơn để nhận tiếp.");
            return;
        }

        player.inventory.gem = FREE_GEM_BALANCE;
        if (player.getSession() != null) {
            Service.gI().sendMoney(player);
        }
        Service.gI().sendThongBao(player, "Đã nhận 10 triệu ngọc xanh miễn phí!");
    }
}
