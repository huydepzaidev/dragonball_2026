package nro.models.combine;

/** Pure rules for Angel equipment quality and crafting success rates. */
final class AngelCraftingPolicy {

    static final int REQUIRED_FRAGMENTS = 999;
    static final int GOLD_COST = 10_000_000;

    private AngelCraftingPolicy() {
    }

    static boolean isVipRecipe(int itemId) {
        return itemId >= 1084 && itemId <= 1086;
    }

    static int qualityPercent(int destroyCount, int divineCount) {
        if (destroyCount == 0 && divineCount == 0) {
            return 0;
        }
        if (destroyCount == 1 && divineCount == 0) {
            return 5;
        }
        if (destroyCount == 1 && divineCount == 1) {
            return 10;
        }
        if (destroyCount == 2 && divineCount == 1) {
            return 15;
        }
        return -1;
    }

    static int successRate(int qualityPercent, int upgradeStoneId) {
        int baseRate = switch (qualityPercent) {
            case 0 -> 90;
            case 5 -> 70;
            case 10 -> 40;
            case 15 -> 15;
            default -> throw new IllegalArgumentException("Unsupported Angel quality: " + qualityPercent);
        };
        int stoneLevel = upgradeStoneId - 1073;
        if (stoneLevel < 1 || stoneLevel > 5) {
            throw new IllegalArgumentException("Unsupported Angel upgrade stone: " + upgradeStoneId);
        }
        return baseRate + stoneLevel;
    }

    static int statWithQuality(int baseValue, int qualityPercent) {
        if (qualityPercent <= 0) {
            return baseValue;
        }
        return baseValue + Math.round(baseValue * qualityPercent / 100F);
    }
}
