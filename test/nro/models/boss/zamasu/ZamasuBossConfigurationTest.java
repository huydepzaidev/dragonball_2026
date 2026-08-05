package nro.models.boss.zamasu;

import java.util.HashSet;
import java.util.Set;
import nro.models.boss.BossData;
import nro.models.boss.BossID;
import nro.models.boss.BossesData;
import nro.models.skill.Skill;

public final class ZamasuBossConfigurationTest {

    private ZamasuBossConfigurationTest() {
    }

    public static void main(String[] args) {
        BossData data = BossesData.ZAMASU;
        assertEquals(-382, BossID.ZAMASU);
        assertEquals("Zamasu", data.getName());
        assertEquals(400_000, data.getDame());
        assertEquals(2_000_000_000, data.getHp()[0]);
        assertArrayEquals(new short[]{903, 904, 905, -1, -1, -1}, data.getOutfit());
        assertArrayEquals(new int[]{92, 93, 94, 96, 97, 98, 99, 100}, data.getMapJoin());

        Set<String> skills = new HashSet<>();
        for (int[] skill : data.getSkillTemp()) {
            skills.add(skill[0] + ":" + skill[1]);
        }
        assertEquals(Set.of(
                Skill.GALICK + ":7", Skill.ANTOMIC + ":7",
                Skill.TAI_TAO_NANG_LUONG + ":4", Skill.KAMEJOKO + ":7",
                Skill.THAI_DUONG_HA_SAN + ":3", Skill.THOI_MIEN + ":7",
                Skill.DICH_CHUYEN_TUC_THOI + ":4", Skill.LIEN_HOAN + ":7",
                Skill.MASENKO + ":7"), skills);
        System.out.println("ZAMASU_BOSS_CONFIGURATION_OK hp=2b dame=400k maps=8 skills=9");
    }

    private static void assertArrayEquals(short[] expected, short[] actual) {
        if (!java.util.Arrays.equals(expected, actual)) {
            throw new AssertionError("expected=" + java.util.Arrays.toString(expected)
                    + " actual=" + java.util.Arrays.toString(actual));
        }
    }

    private static void assertArrayEquals(int[] expected, int[] actual) {
        if (!java.util.Arrays.equals(expected, actual)) {
            throw new AssertionError("expected=" + java.util.Arrays.toString(expected)
                    + " actual=" + java.util.Arrays.toString(actual));
        }
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }
}