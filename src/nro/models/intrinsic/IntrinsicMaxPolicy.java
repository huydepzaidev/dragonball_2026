package nro.models.intrinsic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class IntrinsicMaxPolicy {

    public static final long MIN_POWER = 10_000_000_000L;
    public static final int RUBY_COST = 500;

    private IntrinsicMaxPolicy() {
    }

    public static boolean canOpen(long power, int ruby) {
        return power >= MIN_POWER && ruby >= RUBY_COST;
    }

    public static List<Intrinsic> getEligibleIntrinsics(List<Intrinsic> source) {
        Map<Integer, Intrinsic> uniqueById = new LinkedHashMap<>();
        if (source != null) {
            for (Intrinsic intrinsic : source) {
                if (intrinsic != null && intrinsic.id > 0) {
                    uniqueById.putIfAbsent(intrinsic.id, intrinsic);
                }
            }
        }
        return new ArrayList<>(uniqueById.values());
    }

    public static Intrinsic maximize(Intrinsic template) {
        if (template == null || template.id <= 0) {
            return null;
        }
        Intrinsic result = new Intrinsic(template);
        result.param1 = (short) Math.max(result.paramFrom1, result.paramTo1);
        result.param2 = (short) Math.max(result.paramFrom2, result.paramTo2);
        return result;
    }
}
