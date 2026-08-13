package nro.models.player;

import java.lang.reflect.Method;
import nro.models.consts.ConstPlayer;
import nro.models.map.Zone;

public final class PetRetaliationRuleTest {

    private static final Method FIND_PLAYER_ATTACK = findPlayerAttackMethod();

    private PetRetaliationRuleTest() {
    }

    public static void main(String[] args) throws Exception {
        for (byte status : new byte[]{Pet.PROTECT, Pet.ATTACK}) {
            assertBossWhoHitMasterIsSelected(status);
            assertBossWhoHitPetIsSelected(status);
            assertPkPlayerWhoHitMasterIsSelected(status);
            assertFlaggedPlayerWhoHitPetIsSelected(status);
        }
        assertBossHasPriorityOverCloserPkPlayer();
        assertNonPkPlayerIsRejected();
        assertPlayerWhoDidNotAttackIsRejected();
        assertTargetFromAnotherZoneIsRejected();
        assertFarTargetNeedsApproach();
        assertNearbyTargetDoesNotNeedApproach();
        System.out.println("PET_RETALIATION_RULE_TEST_OK");
    }

    private static void assertBossWhoHitMasterIsSelected(byte status) throws Exception {
        Scenario scenario = scenario(status, (byte) 0);
        Player boss = scenario.addTarget(true, ConstPlayer.PK_ALL, (byte) 0, 900);
        scenario.master.setTemporaryEnemies(boss);
        assertSame(boss, scenario.findTarget(), "boss that hit master");
    }

    private static void assertBossWhoHitPetIsSelected(byte status) throws Exception {
        Scenario scenario = scenario(status, (byte) 1);
        Player boss = scenario.addTarget(true, ConstPlayer.NON_PK, (byte) 0, 800);
        scenario.pet.setTemporaryEnemies(boss);
        assertSame(boss, scenario.findTarget(), "boss that hit pet");
    }

    private static void assertPkPlayerWhoHitMasterIsSelected(byte status) throws Exception {
        Scenario scenario = scenario(status, (byte) 0);
        Player attacker = scenario.addTarget(false, ConstPlayer.PK_ALL, (byte) 0, 700);
        scenario.master.setTemporaryEnemies(attacker);
        assertSame(attacker, scenario.findTarget(), "PK player that hit master");
    }

    private static void assertFlaggedPlayerWhoHitPetIsSelected(byte status) throws Exception {
        Scenario scenario = scenario(status, (byte) 0);
        scenario.master.cFlag = 1;
        scenario.pet.cFlag = 1;
        Player attacker = scenario.addTarget(false, ConstPlayer.NON_PK, (byte) 2, 650);
        scenario.pet.setTemporaryEnemies(attacker);
        assertSame(attacker, scenario.findTarget(), "opposing flag player that hit pet");
    }

    private static void assertBossHasPriorityOverCloserPkPlayer() throws Exception {
        Scenario scenario = scenario(Pet.PROTECT, (byte) 0);
        Player attacker = scenario.addTarget(false, ConstPlayer.PK_ALL, (byte) 0, 40);
        Player boss = scenario.addTarget(true, ConstPlayer.PK_ALL, (byte) 0, 950);
        scenario.master.setTemporaryEnemies(attacker);
        scenario.pet.setTemporaryEnemies(boss);
        assertSame(boss, scenario.findTarget(), "boss priority");
    }

    private static void assertNonPkPlayerIsRejected() throws Exception {
        Scenario scenario = scenario(Pet.ATTACK, (byte) 0);
        Player attacker = scenario.addTarget(false, ConstPlayer.NON_PK, (byte) 0, 40);
        scenario.master.setTemporaryEnemies(attacker);
        assertSame(null, scenario.findTarget(), "non-PK player");
    }

    private static void assertPlayerWhoDidNotAttackIsRejected() throws Exception {
        Scenario scenario = scenario(Pet.PROTECT, (byte) 0);
        scenario.addTarget(false, ConstPlayer.PK_ALL, (byte) 0, 40);
        assertSame(null, scenario.findTarget(), "PK player that did not attack");
    }

    private static void assertTargetFromAnotherZoneIsRejected() throws Exception {
        Scenario scenario = scenario(Pet.ATTACK, (byte) 0);
        Player boss = scenario.addTarget(true, ConstPlayer.PK_ALL, (byte) 0, 40);
        boss.zone = new Zone(null, 1, 20);
        scenario.master.setTemporaryEnemies(boss);
        assertSame(null, scenario.findTarget(), "boss from another zone");
    }

    private static void assertFarTargetNeedsApproach() {
        Scenario scenario = scenario(Pet.PROTECT, (byte) 0);
        Player boss = scenario.addTarget(true, ConstPlayer.PK_ALL, (byte) 0, 900);
        assertTrue(Pet.needsApproachPlayerAttack(scenario.pet, boss), "far target must be approached");
    }

    private static void assertNearbyTargetDoesNotNeedApproach() {
        Scenario scenario = scenario(Pet.ATTACK, (byte) 0);
        Player attacker = scenario.addTarget(false, ConstPlayer.PK_ALL, (byte) 0, 300);
        assertTrue(!Pet.needsApproachPlayerAttack(scenario.pet, attacker), "near target must not be approached");
    }

    private static Scenario scenario(byte status, byte typePet) {
        Zone zone = new Zone(null, 0, 20);
        Player master = alivePlayer(0);
        Pet pet = new Pet(master);
        pet.typePet = typePet;
        pet.status = status;
        pet.nPoint.hp = 100;
        pet.nPoint.hpMax = 100;
        pet.location.x = 100;
        master.pet = pet;
        master.zone = zone;
        pet.zone = zone;
        zone.addPlayer(master);
        zone.addPlayer(pet);
        return new Scenario(zone, master, pet);
    }

    private static Player alivePlayer(int x) {
        Player player = new Player();
        player.nPoint.hp = 100;
        player.nPoint.hpMax = 100;
        player.location.x = x;
        return player;
    }

    private static Method findPlayerAttackMethod() {
        try {
            Method method = Pet.class.getDeclaredMethod("findPlayerAttack");
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static void assertSame(Player expected, Player actual, String label) {
        if (expected != actual) {
            throw new AssertionError("Unexpected target for " + label);
        }
    }

    private static void assertTrue(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label);
        }
    }

    private record Scenario(Zone zone, Player master, Pet pet) {

        private Player addTarget(boolean boss, int typePk, byte flag, int x) {
            Player target = alivePlayer(x);
            target.isBoss = boss;
            target.typePk = (byte) typePk;
            target.cFlag = flag;
            target.zone = zone;
            zone.addPlayer(target);
            return target;
        }

        private Player findTarget() throws Exception {
            return (Player) FIND_PLAYER_ATTACK.invoke(pet);
        }
    }
}
