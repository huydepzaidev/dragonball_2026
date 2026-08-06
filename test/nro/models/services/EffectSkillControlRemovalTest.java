package nro.models.services;

import nro.models.item.Item;
import nro.models.player.Player;

public final class EffectSkillControlRemovalTest {

    private EffectSkillControlRemovalTest() {
    }

    public static void main(String[] args) {
        Player player = new Player();
        Player playerUsingHold = new Player();
        for (int i = 0; i < 11; i++) {
            player.inventory.itemsBody.add(new Item());
            playerUsingHold.inventory.itemsBody.add(new Item());
        }
        player.effectSkill.isStun = true;
        player.effectSkill.isThoiMien = true;
        player.effectSkill.isBlindDCTT = true;
        player.effectSkill.anTroi = true;
        player.effectSkill.isStone = true;
        player.effectSkill.isSocola = true;
        player.effectSkill.isBinh = true;
        player.effectSkill.isUseSkillMonkey = true;
        player.effectSkill.isBodyChangeTechnique = true;
        player.effectSkill.plTroi = playerUsingHold;
        playerUsingHold.effectSkill.useTroi = true;
        playerUsingHold.effectSkill.plAnTroi = player;

        EffectSkillService.gI().removeControlEffects(player);

        if (player.effectSkill.isStun
                || player.effectSkill.isThoiMien
                || player.effectSkill.isBlindDCTT
                || player.effectSkill.anTroi
                || player.effectSkill.isStone
                || player.effectSkill.isSocola
                || player.effectSkill.isBinh
                || player.effectSkill.isUseSkillMonkey
                || player.effectSkill.isBodyChangeTechnique) {
            throw new AssertionError("a control effect was not removed");
        }
        if (player.effectSkill.plTroi != null
                || playerUsingHold.effectSkill.useTroi
                || playerUsingHold.effectSkill.plAnTroi != null) {
            throw new AssertionError("the hold relationship was not removed");
        }

        System.out.println("EFFECT_SKILL_CONTROL_REMOVAL_OK");
    }
}
