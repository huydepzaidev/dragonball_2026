package nro.models.item;

public final class VipRecipeStackPolicyTest {
    public static void main(String[] args) {
        require(VipRecipeStackPolicy.isRecipe(1071));
        require(VipRecipeStackPolicy.isRecipe(1072));
        require(VipRecipeStackPolicy.isRecipe(1073));
        require(VipRecipeStackPolicy.isRecipe(1084));
        require(VipRecipeStackPolicy.isRecipe(1085));
        require(VipRecipeStackPolicy.isRecipe(1086));
        require(!VipRecipeStackPolicy.isRecipe(1070));
        require(!VipRecipeStackPolicy.isRecipe(1074));
        require(!VipRecipeStackPolicy.isRecipe(1083));
        require(!VipRecipeStackPolicy.isRecipe(1087));
    }

    private static void require(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }
}
