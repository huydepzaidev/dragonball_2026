package nro.models.data;

public final class DataGameVersionTest {
    public static void main(String[] args) {
        assertVersion(8094, 1);
        assertVersion(8617, 1);
        assertVersion(8586, 0);
        assertVersion(11656, 0);
        System.out.println("DATA_GAME_VERSION_TEST_OK");
    }

    private static void assertVersion(int iconId, int expected) {
        int actual = DataGame.getSmallImageVersion(iconId);
        if (actual != expected) {
            throw new AssertionError("Unexpected version for " + iconId + ": " + actual);
        }
    }
}
