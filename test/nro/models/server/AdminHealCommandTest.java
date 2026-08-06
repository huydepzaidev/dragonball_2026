package nro.models.server;

import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.skill.Skill;

public final class AdminHealCommandTest {

    private AdminHealCommandTest() {
    }

    public static void main(String[] args) {
        Player player = new Player();
        for (int i = 0; i < 11; i++) {
            player.inventory.itemsBody.add(new Item());
        }

        player.nPoint.hpMax = 100;
        player.nPoint.mpMax = 200;
        player.nPoint.setHp(1);
        player.nPoint.setMp(2);
        player.effectSkill.isStun = true;

        Skill skill = new Skill();
        skill.skillId = 1;
        skill.coolDown = 10_000;
        skill.lastTimeUseThisSkill = System.currentTimeMillis();
        player.playerSkill.skills.add(skill);

        Command.restoreAdminPlayer(player);

        if (player.nPoint.hp != player.nPoint.hpMax
                || player.nPoint.mp != player.nPoint.mpMax) {
            throw new AssertionError("hs did not restore full HP/KI");
        }
        if (player.effectSkill.isStun) {
            throw new AssertionError("hs did not remove control effects");
        }
        if (skill.lastTimeUseThisSkill + skill.coolDown > System.currentTimeMillis()) {
            throw new AssertionError("hs did not reset the skill cooldown");
        }

        System.out.println("ADMIN_HEAL_COMMAND_OK");
    }
}
