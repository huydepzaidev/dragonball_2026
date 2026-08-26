package nro.models.services;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class InventoryUnequipItemTimeVisibilityTest {

    private InventoryUnequipItemTimeVisibilityTest() {
    }

    public static void main(String[] args) throws Exception {
        String source = Files.readString(
                Path.of("src/nro/models/services/InventoryService.java"),
                StandardCharsets.UTF_8);
        int methodStart = source.indexOf("public void itemBodyToBag(Player player, int index)");
        int methodEnd = source.indexOf("public void itemBagToPetBody", methodStart);
        check(methodStart >= 0 && methodEnd > methodStart,
                "Cannot locate itemBodyToBag in InventoryService");

        String method = source.substring(methodStart, methodEnd);
        check(!method.contains("Service.gI().player(player)"),
                "Unequipping must not send the full player packet because the client clears vItemTime");
        check(method.contains("sendItemBags(player)"), "Bag refresh must remain");
        check(method.contains("sendItemBody(player)"), "Body refresh must remain");
        check(method.contains("Service.gI().point(player)"), "Point refresh must remain");

        System.out.println("INVENTORY_UNEQUIP_ITEM_TIME_VISIBILITY_TEST_OK");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
