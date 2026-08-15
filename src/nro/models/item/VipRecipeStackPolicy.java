package nro.models.item;

/** Stack rules for normal and VIP Angel-equipment recipes. */
public final class VipRecipeStackPolicy {

    public static final int FIRST_NORMAL_RECIPE_ID = 1071;
    public static final int LAST_NORMAL_RECIPE_ID = 1073;
    public static final int FIRST_VIP_RECIPE_ID = 1084;
    public static final int LAST_VIP_RECIPE_ID = 1086;

    private VipRecipeStackPolicy() {
    }

    public static boolean isRecipe(int itemTemplateId) {
        return itemTemplateId >= FIRST_NORMAL_RECIPE_ID && itemTemplateId <= LAST_NORMAL_RECIPE_ID
                || itemTemplateId >= FIRST_VIP_RECIPE_ID && itemTemplateId <= LAST_VIP_RECIPE_ID;
    }
}
