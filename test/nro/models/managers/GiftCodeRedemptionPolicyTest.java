package nro.models.managers;

import java.sql.Timestamp;
import nro.models.player.Player;
import nro.models.player_system.GiftCode;

public final class GiftCodeRedemptionPolicyTest {
    public static void main(String[] args) {
        testRequiredSlots();
        testInsufficientBagDoesNotConsume();
        testExpiration();
        System.out.println();
    }

    private static void testRequiredSlots() {
        GiftCode giftCode = new GiftCode();
        giftCode.detail.put(-1, 1000);
        giftCode.detail.put(-2, 10);
        if (GiftCodeManager.getRequiredBagSlots(giftCode) != 0) {
            throw new AssertionError();
        }
        giftCode.detail.put(457, 50);
        giftCode.detail.put(381, 10);
        if (GiftCodeManager.getRequiredBagSlots(giftCode) != 2) {
            throw new AssertionError();
        }
    }

    private static void testExpiration() {
        GiftCode giftCode = new GiftCode();
        giftCode.dateexpired = new Timestamp(System.currentTimeMillis() - 1000L);
        if (!giftCode.timeCode()) {
            throw new AssertionError();
        }
        giftCode.dateexpired = new Timestamp(System.currentTimeMillis() + 60000L);
        if (giftCode.timeCode()) {
            throw new AssertionError();
        }
    }

    private static void testInsufficientBagDoesNotConsume() {
        Player player = new Player();
        GiftCode giftCode = new GiftCode();
        giftCode.code = String.valueOf(381);
        giftCode.countLeft = 1;
        giftCode.detail.put(381, 1);
        GiftCodeManager manager = GiftCodeManager.gI();
        manager.listGiftCode.clear();
        manager.listGiftCode.add(giftCode);
        if (manager.checkUseGiftCode(player, giftCode.code) != null) {
            throw new AssertionError();
        }
        if (giftCode.countLeft != 1 || giftCode.isUsedGiftCode(player)) {
            throw new AssertionError();
        }
    }
}
