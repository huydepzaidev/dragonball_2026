package nro.models.server;

import nro.models.player.Player;

public final class AdminStatBuffCommandTest {

    private AdminStatBuffCommandTest() {
    }

    public static void main(String[] args) {
        Player player = new Player();
        player.nPoint.power = 1_000;
        player.nPoint.tiemNang = 2_000;

        long addedPower = Command.applyAdminStatBuff(
                player, Command.ADMIN_BUFF_POWER, 500);
        require(addedPower == 500, "power command reported the wrong amount");
        require(player.nPoint.power == 1_500, "power command did not add power");
        require(player.nPoint.tiemNang == 2_000,
                "power command also changed potential");

        player.nPoint.power = player.nPoint.getPowerLimit();
        long addedPotential = Command.applyAdminStatBuff(
                player, Command.ADMIN_BUFF_POTENTIAL, 700);
        require(addedPotential == 700,
                "potential command did not work at the power limit");
        require(player.nPoint.power == player.nPoint.getPowerLimit(),
                "potential command also changed power");
        require(player.nPoint.tiemNang == 2_700,
                "potential command did not add potential");

        require(Command.applyAdminStatBuff(
                player, Command.ADMIN_BUFF_POWER, 0) == 0,
                "zero power buff was accepted");
        require(Command.applyAdminStatBuff(
                player, Command.ADMIN_BUFF_POTENTIAL, -1) == 0,
                "negative potential buff was accepted");

        System.out.println("ADMIN_STAT_BUFF_COMMAND_OK separatePower=true separatePotential=true");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
