package nro.models.boss.pilap;

import java.util.Arrays;
import nro.models.boss.BossData;
import nro.models.boss.BossID;
import nro.models.boss.BossesData;
import nro.models.skill.Skill;

public final class PilapSquadConfigurationTest {

    private PilapSquadConfigurationTest() {
    }

    public static void main(String[] args) {
        assertMember(BossesData.PILAP, 100_000, 1_000,
                new short[]{612, 613, 614, -1, -1, -1});
        assertMember(BossesData.MAI_PILAP, 100_000, 1_000,
                new short[]{615, 616, 617, -1, -1, -1});
        assertMember(BossesData.PU_PILAP, 100_000, 1_000,
                new short[]{618, 619, 620, -1, -1, -1});

        assertArrayEquals(new int[]{0, 7, 14, 42, 43, 44},
                BossesData.PILAP.getMapJoin());
        assertArrayEquals(new int[]{BossID.MAI_PILAP, BossID.PU_PILAP},
                BossesData.PILAP.getBossesAppearTogether());
        assertArrayEquals(new int[]{
            Skill.THOI_MIEN, Skill.DRAGON, Skill.GALICK,
            Skill.LIEN_HOAN, Skill.THAI_DUONG_HA_SAN, Skill.MASENKO
        }, skillIds(BossesData.PILAP));
        assertArrayEquals(new int[]{7, 7, 7, 7, 3, 7},
                skillLevels(BossesData.PILAP));
        if (PilapSquadBoss.DAMAGE_LIMIT != -1L) {
            throw new AssertionError("damage limit must use the legacy -1 rule");
        }
        if (PilapSquadBoss.limitedDamage(0L) != 0L
                || PilapSquadBoss.limitedDamage(1L) != 1L
                || PilapSquadBoss.limitedDamage(999_999_999L) != 1L) {
            throw new AssertionError("damageLimit=-1 must remove exactly 1 HP per valid hit");
        }
        System.out.println("PILAP_SQUAD_CONFIG_OK hp=1000 dame=100000 damageLimit=-1 means 1hp/hit maps=0/7/14/42/43/44");
    }

    private static void assertMember(
            BossData data, int damage, int hp, short[] outfit) {
        if (data.getDame() != damage || data.getHp().length != 1
                || data.getHp()[0] != hp) {
            throw new AssertionError("invalid damage/hp for " + data.getName());
        }
        if (!Arrays.equals(outfit, data.getOutfit())) {
            throw new AssertionError("invalid outfit for " + data.getName());
        }
    }

    private static int[] skillIds(BossData data) {
        return Arrays.stream(data.getSkillTemp()).mapToInt(skill -> skill[0]).toArray();
    }

    private static int[] skillLevels(BossData data) {
        return Arrays.stream(data.getSkillTemp()).mapToInt(skill -> skill[1]).toArray();
    }

    private static void assertArrayEquals(int[] expected, int[] actual) {
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError("expected=" + Arrays.toString(expected)
                    + " actual=" + Arrays.toString(actual));
        }
    }
}
