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
        player.effectSkill.isShielding = true;
        player.effectSkill.isMonkey = true;
        player.effectSkill.isCharging = true;
        player.effectSkill.tiLeHPHuytSao = 20;
        player.effectSkill.isTanHinh = true;
        player.effectSkill.isDameBuff = true;
        player.effectSkill.isPKCommeson = true;
        player.effectSkill.isPKSTT = true;
        player.effectSkill.isChibi = true;
        player.effectSkill.isHalloween = true;
        player.effectSkill.isUseMafuba = true;
        player.effectSkill.isIntrinsic = true;
        player.effectSkill.playerUseMafuba = new Player();
        player.newSkill.playersTaget.add(new Player());

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
        if (player.effectSkill.isStun
                || player.effectSkill.isShielding
                || player.effectSkill.isMonkey
                || player.effectSkill.isCharging
                || player.effectSkill.tiLeHPHuytSao != 0
                || player.effectSkill.isTanHinh
                || player.effectSkill.isDameBuff
                || player.effectSkill.isPKCommeson
                || player.effectSkill.isPKSTT
                || player.effectSkill.isChibi
                || player.effectSkill.isHalloween
                || player.effectSkill.isUseMafuba
                || player.effectSkill.isIntrinsic
                || player.effectSkill.playerUseMafuba != null
                || !player.newSkill.playersTaget.isEmpty()) {
            throw new AssertionError("hs did not remove every skill effect");
        }
        if (skill.lastTimeUseThisSkill + skill.coolDown > System.currentTimeMillis()) {
            throw new AssertionError("hs did not reset the skill cooldown");
        }

        System.out.println("ADMIN_HEAL_COMMAND_OK");
    }
}
