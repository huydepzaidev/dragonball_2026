package nro.models.player;

public final class InventoryCapacityTest {

    private InventoryCapacityTest() {
    }

    public static void main(String[] args) {
        require(Inventory.MAX_ITEMS_BAG == 120);
        require(Inventory.MAX_ITEMS_BOX == 120);
        require(Inventory.MAX_ITEMS_BAG <= Byte.MAX_VALUE);
        require(Inventory.MAX_ITEMS_BOX <= Byte.MAX_VALUE);
    }

    private static void require(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }
}
