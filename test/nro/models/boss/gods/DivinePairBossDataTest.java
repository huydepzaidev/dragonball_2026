package nro.models.boss.gods;

import java.util.Arrays;
import nro.models.boss.BossData;
import nro.models.boss.BossID;
import nro.models.boss.BossesData;
import nro.models.consts.AppearType;
import nro.models.skill.Skill;

public final class DivinePairBossDataTest {

    private static final int[] EXPECTED_SKILLS = {
        Skill.GALICK, Skill.ANTOMIC, Skill.TAI_TAO_NANG_LUONG,
        Skill.KAMEJOKO, Skill.THAI_DUONG_HA_SAN, Skill.THOI_MIEN,
        Skill.DICH_CHUYEN_TUC_THOI, Skill.LIEN_HOAN, Skill.MASENKO
    };
    private static final int[] EXPECTED_LEVELS = {7, 7, 4, 7, 3, 7, 4, 7, 7};

    private DivinePairBossDataTest() { }

    public static void main(String[] args) {
        assertLeader(BossesData.GOD_BILL, new short[]{508, 509, 510}, BossID.ANGEL_WHIS);
        assertPartner(BossesData.ANGEL_WHIS, new short[]{505, 506, 507});
        assertLeader(BossesData.GOD_CHAMPA, new short[]{511, 512, 513}, BossID.ANGEL_VADOS);
        assertPartner(BossesData.ANGEL_VADOS, new short[]{530, 531, 532});
        System.out.println("DIVINE_PAIR_BOSS_DATA_OK hp=2000000000 dame=400000 maps=154,155 skills=9 outfits=4");
    }

    private static void assertLeader(BossData data, short[] outfit, int partnerId) {
        assertCommon(data, outfit);
        if (data.getSecondsRest() != 600
                || !Arrays.equals(data.getBossesAppearTogether(), new int[]{partnerId})) {
            throw new AssertionError("invalid leader lifecycle data");
        }
    }

    private static void assertPartner(BossData data, short[] outfit) {
        assertCommon(data, outfit);
        if (data.getTypeAppear() != AppearType.APPEAR_WITH_ANOTHER) {
            throw new AssertionError("partner must appear with leader");
        }
    }

    private static void assertCommon(BossData data, short[] outfit) {
        if (data.getDame() != 400_000 || !Arrays.equals(data.getHp(), new int[]{2_000_000_000})
                || !Arrays.equals(data.getMapJoin(), new int[]{154, 155})) {
            throw new AssertionError("invalid damage, hp or map data");
        }
        if (!Arrays.equals(Arrays.copyOf(data.getOutfit(), 3), outfit)) {
            throw new AssertionError("invalid outfit " + Arrays.toString(data.getOutfit()));
        }
        int[][] skills = data.getSkillTemp();
        if (skills.length != EXPECTED_SKILLS.length) throw new AssertionError("invalid skill count");
        for (int i = 0; i < skills.length; i++) {
            if (skills[i][0] != EXPECTED_SKILLS[i] || skills[i][1] != EXPECTED_LEVELS[i]) {
                throw new AssertionError("invalid skill at index " + i);
            }
        }
    }
}
