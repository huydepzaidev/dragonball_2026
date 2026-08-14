package nro.models.data;

public final class ItemDataVersionTest {

    private ItemDataVersionTest() {
    }

    public static void main(String[] args) {
        if (DataGame.vsData != 12 || DataGame.vsRes != 2) {
            throw new AssertionError();
        }
        if (ItemData.ITEM_TEMPLATE_SPLIT_INDEX != 900) {
            throw new AssertionError();
        }
        if (DataGame.vsItem != 12) {
            throw new AssertionError("Expected item data version 12, got " + DataGame.vsItem);
        }
        System.out.println("ITEM_DATA_VERSION_OK version=" + DataGame.vsItem);
    }
}
