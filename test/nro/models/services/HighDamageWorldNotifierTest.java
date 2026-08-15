package nro.models.services;

import nro.models.skill.Skill;

public final class HighDamageWorldNotifierTest {

    private HighDamageWorldNotifierTest() {
    }

    public static void main(String[] args) {
        testThreshold();
        testMonitoredSkills();
        testExcludedSkills();
        testMessage();
        System.out.println("HIGH_DAMAGE_WORLD_NOTIFIER_TEST_OK");
    }

    private static void testThreshold() {
        check(!HighDamageWorldNotifier.shouldNotify(Skill.DRAGON, 99_999_999L),
                "Damage below 100 million must not notify");
        check(HighDamageWorldNotifier.shouldNotify(Skill.DRAGON, 100_000_000L),
                "Damage at 100 million must notify");
    }

    private static void testMonitoredSkills() {
        int[] skillIds = {
            Skill.DRAGON, Skill.DEMON, Skill.GALICK,
            Skill.KAMEJOKO, Skill.MASENKO, Skill.ANTOMIC,
            Skill.KAIOKEN, Skill.LIEN_HOAN,
            Skill.MAKANKOSAPPO, Skill.QUA_CAU_KENH_KHI, Skill.TU_SAT,
            Skill.SUPER_KAME, Skill.LIEN_HOAN_CHUONG
        };
        for (int skillId : skillIds) {
            check(HighDamageWorldNotifier.shouldNotify(skillId, 100_000_000L),
                    "Expected monitored skill: " + skillId);
        }
    }

    private static void testExcludedSkills() {
        int[] skillIds = {
            Skill.THAI_DUONG_HA_SAN, Skill.TRI_THUONG,
            Skill.DICH_CHUYEN_TUC_THOI, Skill.MA_PHONG_BA
        };
        for (int skillId : skillIds) {
            check(!HighDamageWorldNotifier.shouldNotify(skillId, 2_000_000_000L),
                    "Expected excluded skill: " + skillId);
        }
    }

    private static void testMessage() {
        String message = HighDamageWorldNotifier.buildMessage(
                "BoMong", "Makankosappo", 123_456_789L);
        check(message.equals("Người chơi BoMong dùng chiêu Makankosappo với mức sát thương là 123.456.789"),
                "Unexpected world message: " + message);
        check(message.length() <= 100, "World message must fit the existing channel limit");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
