package nro.models.combine;

public final class PhaLeHoaSingleStarStepTest {

    private PhaLeHoaSingleStarStepTest() {
    }

    public static void main(String[] args) {
        for (int star = 0; star < CombineService.MAX_STAR_ITEM; star++) {
            int nextStar = PhaLeHoaTrangBi.getNextStarOnSuccess(star);
            require(nextStar == star + 1,
                    "Successful upgrade jumped from " + star + " to " + nextStar);
        }
        require(PhaLeHoaTrangBi.getNextStarOnSuccess(CombineService.MAX_STAR_ITEM)
                == CombineService.MAX_STAR_ITEM,
                "Max-star equipment must not gain another star");
        System.out.println("PHA_LE_HOA_SINGLE_STAR_STEP_TEST_OK");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
