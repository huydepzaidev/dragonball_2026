package nro.models.item;

/**
 * Fixed default options for the Dragon Ball flags imported as item templates
 * 2008-2015.
 */
public final class DragonBallFlagOptionPolicy {

    private DragonBallFlagOptionPolicy() {
    }

    public static int[][] optionsFor(short itemTemplateId) {
        switch (itemTemplateId) {
            case 2008: // Cờ ngọc rồng 1 sao
                return options(19, new int[][]{{14, 10}, {5, 10}});
            case 2009: // Cờ ngọc rồng 2 sao
                return options(18, new int[][]{{108, 10}, {94, 10}});
            case 2010: // Cờ ngọc rồng 3 sao
                return options(17, new int[][]{{108, 5}, {94, 5}});
            case 2011: // Cờ ngọc rồng 4 sao
                return options(16, new int[][]{{14, 5}, {5, 5}});
            case 2012: // Cờ ngọc rồng 5 sao
                return options(15, new int[][]{{14, 5}, {108, 5}});
            case 2013: // Cờ ngọc rồng 6 sao
                return options(13, new int[][]{{108, 5}});
            case 2014: // Cờ ngọc rồng 7 sao
                return options(12, new int[][]{{14, 5}});
            case 2015: // Cờ ngọc rồng Super
                return options(22, new int[][]{{14, 10}, {108, 10}, {5, 15}});
            default:
                return new int[0][0];
        }
    }

    private static int[][] options(int basePercent, int[][] extraOptions) {
        int[][] result = new int[extraOptions.length + 5][2];
        result[0] = new int[]{50, basePercent};  // Sức đánh +#%
        result[1] = new int[]{77, basePercent};  // HP +#%
        result[2] = new int[]{103, basePercent}; // KI +#%
        for (int i = 0; i < extraOptions.length; i++) {
            result[i + 3] = extraOptions[i];
        }
        result[result.length - 2] = new int[]{30, 0};  // Không thể giao dịch
        result[result.length - 1] = new int[]{231, 0}; // Hạn sử dụng hoặc vĩnh viễn
        return result;
    }
}
