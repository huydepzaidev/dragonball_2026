package nro.models.services;

import java.util.HashMap;
import java.util.Map;
import nro.models.player.Player;
import nro.models.skill.Skill;
import nro.models.utils.Util;

public final class HighDamageWorldNotifier {

    static final long DAMAGE_THRESHOLD = 100_000_000L;
    private static final long DUPLICATE_WINDOW_MS = 250L;
    private static final long CACHE_ENTRY_TTL_MS = 60_000L;
    private static final int MAX_CACHE_SIZE = 10_000;
    private static final Map<Long, Long> LAST_NOTIFICATIONS = new HashMap<>();

    private HighDamageWorldNotifier() {
    }

    public static void notifyIfNeeded(Player player, long damage) {
        if (player == null || !player.isPl() || player.playerSkill == null
                || player.playerSkill.skillSelect == null
                || player.playerSkill.skillSelect.template == null) {
            return;
        }

        int skillId = player.playerSkill.skillSelect.template.id;
        if (!shouldNotify(skillId, damage)
                || !claimNotification(player.id, skillId, System.currentTimeMillis())) {
            return;
        }

        String skillName = player.playerSkill.skillSelect.template.name;
        String message = buildMessage(player.name, skillName, damage);
        ChatGlobalService.gI().ThongBaoRoiDo(player, message);
    }

    static boolean shouldNotify(int skillId, long damage) {
        return damage >= DAMAGE_THRESHOLD && isMonitoredSkill(skillId);
    }

    static boolean isMonitoredSkill(int skillId) {
        return switch (skillId) {
            case Skill.DRAGON, Skill.DEMON, Skill.GALICK,
                    Skill.KAMEJOKO, Skill.MASENKO, Skill.ANTOMIC,
                    Skill.KAIOKEN, Skill.LIEN_HOAN,
                    Skill.MAKANKOSAPPO, Skill.QUA_CAU_KENH_KHI, Skill.TU_SAT,
                    Skill.SUPER_KAME, Skill.LIEN_HOAN_CHUONG -> true;
            default -> false;
        };
    }

    static String buildMessage(String playerName, String skillName, long damage) {
        String safePlayerName = playerName == null || playerName.isBlank() ? "Không rõ" : playerName;
        String safeSkillName = skillName == null || skillName.isBlank() ? "Không rõ" : skillName;
        return "Người chơi " + safePlayerName + " dùng chiêu " + safeSkillName
                + " với mức sát thương là " + Util.formatNumber(Math.max(0L, damage));
    }

    private static synchronized boolean claimNotification(long playerId, int skillId, long now) {
        long key = (playerId << 8) ^ (skillId & 0xFFL);
        Long lastTime = LAST_NOTIFICATIONS.get(key);
        if (lastTime != null && now - lastTime < DUPLICATE_WINDOW_MS) {
            return false;
        }

        LAST_NOTIFICATIONS.put(key, now);
        if (LAST_NOTIFICATIONS.size() > MAX_CACHE_SIZE) {
            LAST_NOTIFICATIONS.entrySet().removeIf(
                    entry -> now - entry.getValue() >= CACHE_ENTRY_TTL_MS);
        }
        return true;
    }
}
