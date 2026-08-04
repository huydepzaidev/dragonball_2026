package nro.models.server;

import java.util.List;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.boss.Boss_Manager.BrolyManager;
import nro.models.player.Player;

public final class CommandWhitelistTest {

    private CommandWhitelistTest() {
    }

    public static void main(String[] args) {
        for (String command : List.of(
                "boss", "brl", "a", "item", "getitem", "hs", "d",
                "toado", "1", "2", "b", "m 5", "n 4", "dm 100",
                "hp 100", "ki 100", "up 100", "upp 100", "i 14",
                "i 14 2", "i 14 2 50:10", " BOSS ", "DM 100")) {
            if (!Command.isSupportedAdminCommand(command)) {
                throw new AssertionError("supported command rejected: " + command);
            }
        }

        for (String text : List.of(
                "boss 1", "brl 1", "item 14", "m abc", "nhiệm vụ 4",
                "dame 100", "hp của tôi", "ki cao", "up đồ", "upp đồ",
                "i love you", "xin chào", "mình đi map 5", "boss mạnh")) {
            if (Command.isSupportedAdminCommand(text)) {
                throw new AssertionError("normal chat treated as command: " + text);
            }
        }

        if (Command.gI().check(new Player(), "boss")) {
            throw new AssertionError("non-admin player executed admin command");
        }
        if (BossManager.ADMIN_BOSS_LIST_MENU_TYPE
                == BrolyManager.ADMIN_BROLY_LIST_MENU_TYPE) {
            throw new AssertionError("boss and Broly lists must use different menu types");
        }

        System.out.println("COMMAND_PARSER_OK oldCommands=true normalChatFallback=true menuTypes=3,4");
    }
}
