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
                "boss", "brl", "a", "item", "getitem", "hs", "kol", "d",
                "toado", "1", "2", "b", "skhvip", "buffskh",
                "skh kaio", "skh kaio 12", "skh kaio 8", "skh kaio 1",
                "skh nail", "skh nail 12 1", "skh cadicm", "skh cadicm 12",
                "skh 245", "skh 245 12", "skh 237 12", "skh 241 12",
                "skh gohan", "skh songoku", "skh kirin", "skh thenxinhang",
                "skh kakarot", "skh nappa", "skh cadic", "skh pikkoro",
                "skh octieu", "skh piccolo", "skh unknown",
                "m 5", "n 4", "dm 100", "hp 100", "ki 100", "up 100", "upp 100",
                "sm 100", "upsm 100", "tn 100", "uptn 100", "i 14",
                "i 14 2", "i 14 2 50:10", "i 2041", "i 2041 1",
                " BOSS ", "DM 100", "SKHVIP", "BUFFSKH", "SKH KAIO 12")) {
            if (!Command.isSupportedAdminCommand(command)) {
                throw new AssertionError("supported command rejected: " + command);
            }
        }

        for (String text : List.of(
                "boss 1", "brl 1", "item 14", "kol 1", "m abc", "nhiệm vụ 4",
                "dame 100", "hp của tôi", "ki cao", "up đồ", "upp đồ",
                "i love you", "xin chào", "mình đi map 5", "boss mạnh",
                "skh a b c d", "skh kaio vip max pro")) {
            if (Command.isSupportedAdminCommand(text)) {
                throw new AssertionError("normal chat treated as command: " + text);
            }
        }

        if (Command.gI().check(new Player(), "boss")) {
            throw new AssertionError("non-admin player executed admin command");
        }
        if (Command.gI().check(new Player(), "kol")) {
            throw new AssertionError("non-admin player executed kol command");
        }
        if (Command.gI().check(new Player(), "skhvip")) {
            throw new AssertionError("non-admin player executed skhvip command");
        }
        if (Command.gI().check(new Player(), "buffskh")) {
            throw new AssertionError("non-admin player executed buffskh command");
        }
        if (Command.gI().check(new Player(), "skh kaio")) {
            throw new AssertionError("non-admin player executed skh kaio command");
        }
        if (BossManager.ADMIN_BOSS_LIST_MENU_TYPE
                == BrolyManager.ADMIN_BROLY_LIST_MENU_TYPE) {
            throw new AssertionError("boss and Broly lists must use different menu types");
        }

        System.out.println("COMMAND_PARSER_OK oldCommands=true skhCommands=true normalChatFallback=true menuTypes=3,4");
    }
}
