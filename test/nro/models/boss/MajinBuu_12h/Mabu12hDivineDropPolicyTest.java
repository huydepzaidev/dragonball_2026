package nro.models.boss.MajinBuu_12h;

import nro.models.boss.BossID;

public final class Mabu12hDivineDropPolicyTest {

    private Mabu12hDivineDropPolicyTest() {
    }

    public static void main(String[] args) {
        selectedBossDropsOncePerZone();
        mabuDropsAsFallback();
        newDayStartsNewTurn();
        everyBossCanBeSelected();
        System.out.println("MABU_12H_DIVINE_DROP_POLICY_OK bosses=9 max=1/zone/turn");
    }

    private static void selectedBossDropsOncePerZone() {
        Mabu12hDivineDropPolicy policy = new Mabu12hDivineDropPolicy();
        int selectedIndex = 2;
        int selectedBossId = Mabu12hDivineDropPolicy.bossIdAt(selectedIndex);
        no(policy.reserveDrop(1, 0, BossID.DRABURA, selectedIndex));
        ok(policy.reserveDrop(1, 0, selectedBossId, selectedIndex));
        no(policy.reserveDrop(1, 0, BossID.MABU_12H, selectedIndex));
        ok(policy.reserveDrop(1, 1, selectedBossId, selectedIndex));
    }

    private static void mabuDropsAsFallback() {
        Mabu12hDivineDropPolicy policy = new Mabu12hDivineDropPolicy();
        int selectedIndex = 0;
        ok(policy.reserveDrop(1, 0, BossID.MABU_12H, selectedIndex));
        no(policy.reserveDrop(1, 0, BossID.DRABURA, selectedIndex));
    }

    private static void newDayStartsNewTurn() {
        Mabu12hDivineDropPolicy policy = new Mabu12hDivineDropPolicy();
        int selectedIndex = 0;
        ok(policy.reserveDrop(1, 0, BossID.DRABURA, selectedIndex));
        no(policy.reserveDrop(1, 0, BossID.DRABURA, selectedIndex));
        ok(policy.reserveDrop(2, 0, BossID.DRABURA, selectedIndex));
    }

    private static void everyBossCanBeSelected() {
        eq(9, Mabu12hDivineDropPolicy.bossCount());
        for (int index = 0; index < Mabu12hDivineDropPolicy.bossCount(); index++) {
            Mabu12hDivineDropPolicy policy = new Mabu12hDivineDropPolicy();
            int bossId = Mabu12hDivineDropPolicy.bossIdAt(index);
            ok(policy.reserveDrop(1, index, bossId, index));
            no(policy.reserveDrop(1, index, bossId, index));
        }
    }

    private static void ok(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }

    private static void no(boolean condition) {
        ok(!condition);
    }

    private static void eq(int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }
}
