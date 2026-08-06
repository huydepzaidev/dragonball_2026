package nro.models.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import nro.models.Bot.BotAttackplayer;
import nro.models.Bot.BotManager;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.boss.Boss_Manager.BrolyManager;
import nro.models.consts.ConstNpc;
import nro.models.consts.ConstPlayer;
import nro.models.item.Item;
import nro.models.map.service.ChangeMapService;
import nro.models.map.service.NpcService;
import nro.models.network.SessionManager;
import nro.models.player.Pet;
import nro.models.player.Player;
import nro.models.services.EffectSkillService;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.PetService;
import nro.models.services.PlayerService;
import nro.models.services.Service;
import nro.models.services.TaskService;
import nro.models.services_func.Input;
import nro.models.utils.SystemMetrics;

public final class Command {

    private static final Set<String> EXACT_ADMIN_COMMANDS = Set.of(
            "boss", "brl", "a", "item", "getitem", "hs", "d",
            "toado", "1", "2", "b");

    private static Command instance;

    private final Map<String, Consumer<Player>> adminCommands = new HashMap<>();
    private final Map<String, BiConsumer<Player, String>> parameterizedCommands = new HashMap<>();

    public static Command gI() {
        if (instance == null) {
            instance = new Command();
        }
        return instance;
    }

    private Command() {
        initAdminCommands();
        initParameterizedCommands();
    }

    private void initAdminCommands() {
        adminCommands.put("boss", player -> BossManager.gI().showListBoss(player));
        adminCommands.put("brl", player -> BrolyManager.gI().showListBoss(player));
        adminCommands.put("item", player -> Input.gI().createFormGiveItem(player));
        adminCommands.put("getitem", player -> Input.gI().createFormGetItem(player));
        adminCommands.put("hs", Command::restoreAdminPlayer);
        adminCommands.put("d", player -> Service.gI().setPos(
                player, player.location.x, player.location.y + 10));
        adminCommands.put("toado", player -> Service.gI().sendThongBaoOK(player,
                "x: " + player.location.x + " - y: " + player.location.y));
        adminCommands.put("a", this::openAdminMenu);
        adminCommands.put("1", this::openBotMenu);
        adminCommands.put("2", this::createAttackBot);
        adminCommands.put("b", player -> Input.gI().createFormSenditem1(player));
    }

    static void restoreAdminPlayer(Player player) {
        Service.gI().releaseCooldownSkill(player);
        EffectSkillService.gI().removeControlEffects(player);
        player.nPoint.setHp(player.nPoint.hpMax);
        player.nPoint.setMp(player.nPoint.mpMax);
        PlayerService.gI().sendInfoHpMpMoney(player);
        Service.gI().Send_Info_NV(player);
    }

    private void initParameterizedCommands() {
        parameterizedCommands.put("m", (player, argument) -> {
            try {
                int mapId = Integer.parseInt(argument);
                ChangeMapService.gI().changeMapInYard(player, mapId, -1, -1);
            } catch (NumberFormatException e) {
                Service.gI().sendThongBao(player, "Sai định dạng map ID!");
            }
        });

        parameterizedCommands.put("n", (player, argument) -> {
            try {
                int taskId = Integer.parseInt(argument);
                player.playerTask.taskMain.id = taskId - 1;
                player.playerTask.taskMain.index = 0;
                TaskService.gI().sendNextTaskMain(player);
            } catch (Exception e) {
                Service.gI().sendThongBao(player, "Sai định dạng task ID!");
            }
        });

        parameterizedCommands.put("dm", (player, argument) -> {
            try {
                int damage = Integer.parseInt(argument);
                player.nPoint.dameg = damage;
                Service.gI().point(player);
                Service.gI().sendThongBao(player, "SET DAMAGE = " + damage);
            } catch (Exception e) {
                Service.gI().sendThongBao(player, "Sai cú pháp: dm <số>");
            }
        });

        parameterizedCommands.put("hp", (player, argument) -> {
            try {
                int hp = Integer.parseInt(argument);
                player.nPoint.hpg = hp;
                Service.gI().point(player);
                Service.gI().sendThongBao(player, "SET HP GỐC = " + hp);
            } catch (Exception e) {
                Service.gI().sendThongBao(player, "Sai cú pháp: hp <số>");
            }
        });

        parameterizedCommands.put("ki", (player, argument) -> {
            try {
                int ki = Integer.parseInt(argument);
                player.nPoint.mpg = ki;
                Service.gI().point(player);
                Service.gI().sendThongBao(player, "SET KI GỐC = " + ki);
            } catch (Exception e) {
                Service.gI().sendThongBao(player, "Sai cú pháp: ki <số>");
            }
        });

        parameterizedCommands.put("up", (player, argument) -> {
            try {
                long power = Long.parseLong(argument);
                Service.gI().addSMTN(player, (byte) 2, power, false);
                Service.gI().sendThongBao(player, "UP SMTN = " + power);
            } catch (Exception e) {
                Service.gI().sendThongBao(player, "Sai cú pháp: up <số>");
            }
        });

        parameterizedCommands.put("upp", (player, argument) -> {
            if (player.pet == null) {
                Service.gI().sendThongBao(player, "Bạn chưa có đệ tử");
                return;
            }
            try {
                long power = Long.parseLong(argument);
                Service.gI().addSMTN(player.pet, (byte) 2, power, false);
                Service.gI().sendThongBao(player, "UP TNSM cho đệ tử = " + power);
            } catch (Exception e) {
                Service.gI().sendThongBao(player, "Sai cú pháp: upp <số>");
            }
        });

        parameterizedCommands.put("i", this::giveItem);
    }

    private void openAdminMenu(Player player) {
        NpcService.gI().createMenuConMeo(player, ConstNpc.MENU_ADMIN, -1,
                "|0|Time start: " + ServerManager.timeStart
                + "\nClients: " + Client.gI().getPlayers().size()
                + "\n Sessions: " + SessionManager.gI().getNumSession()
                + "\nThreads: " + Thread.activeCount()
                + " luồng\n" + SystemMetrics.ToString(),
                "Ngọc rồng", "Đệ tử", "Bảo trì", "Tìm kiếm\nngười chơi", "Boss", "Đóng");
    }

    private void openBotMenu(Player player) {
        NpcService.gI().createMenuConMeo(player, 206783, 206783, "|7| Menu bot\n"
                + "Player online : " + Client.gI().getPlayers().size() + "\n"
                + "\b|1|Thread: " + Thread.activeCount() + "\n"
                + "\n Sessions: " + SessionManager.gI().getNumSession() + "\n"
                + "Bot online : " + BotManager.gI().bot.size(),
                "Bot\nPem Quái", "Bot\nBán Item", "Bot\nSăn Boss", "Bot\nAttack Player");
    }

    private void createAttackBot(Player player) {
        player.originalName = player.name;
        PlayerService.gI().changeAndSendTypePK(player, ConstPlayer.PK_ALL);
        player.originalName = player.name;
        Service.gI().Send_Caitrang(player);

        BotAttackplayer bot = new BotAttackplayer(
                (short) 1624, (short) 1628, (short) 1629,
                1, "đánh nhau không?", (short) 0);
        bot.player = player;
        bot.zone = player.zone;
        bot.location.x = player.location.x;
        bot.location.y = player.location.y;

        player.zone.addPlayer(bot);
        BotManager.gI().bot.add(bot);
        for (Player zonePlayer : player.zone.getPlayers()) {
            if (zonePlayer.session != null) {
                Service.gI().sendAppear(bot, zonePlayer);
                Service.gI().sendInfoCharMoiToMe(zonePlayer, bot);
            }
        }
        if (player.session != null) {
            Service.gI().Send_Info_NV(player);
        }
        bot.update();
        ServerNotify.gI().notify("Đã gọi bot tấn công người chơi!");
    }

    private void giveItem(Player player, String argument) {
        try {
            String[] parts = argument.split("\\s+");
            int itemId = Integer.parseInt(parts[0]);
            int quantity = parts.length >= 2 ? Integer.parseInt(parts[1]) : 1;

            List<Item.ItemOption> customOptions = new ArrayList<>();
            for (int i = 2; i < parts.length; i++) {
                String[] option = parts[i].split(":", 2);
                customOptions.add(new Item.ItemOption(
                        Integer.parseInt(option[0]), Integer.parseInt(option[1])));
            }

            for (int i = 0; i < quantity; i++) {
                Item item = ItemService.gI().createNewItem((short) itemId);
                if (!customOptions.isEmpty()) {
                    item.itemOptions = new ArrayList<>(customOptions);
                } else {
                    List<Item.ItemOption> options
                            = ItemService.gI().getListOptionItemShop((short) itemId);
                    if (!options.isEmpty()) {
                        item.itemOptions = options;
                    }
                }
                InventoryService.gI().addItemBag(player, item);
            }

            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player,
                    "GET " + quantity + " x " + ItemService.gI().getTemplate(itemId).name
                    + " [" + itemId + "] SUCCESS!");
        } catch (Exception e) {
            Service.gI().sendThongBao(player,
                    "Lỗi cú pháp! Dùng: i <itemId> <số lượng> [optionId:value]");
        }
    }

    public void chat(Player player, String text) {
        String cleanedText = text == null ? "" : text.trim();
        if (cleanedText.isEmpty()) {
            return;
        }
        if (!check(player, cleanedText)) {
            Service.gI().chat(player, cleanedText);
        }
    }

    public boolean check(Player player, String text) {
        if (player == null || text == null) {
            return false;
        }

        String cleanedText = text.trim();
        String normalizedText = normalize(cleanedText);
        if (player.isAdmin()) {
            Consumer<Player> exactCommand = adminCommands.get(normalizedText);
            if (exactCommand != null) {
                exactCommand.accept(player);
                return true;
            }

            ParsedCommand parsedCommand = parseParameterizedCommand(normalizedText);
            if (parsedCommand != null) {
                parameterizedCommands.get(parsedCommand.name)
                        .accept(player, parsedCommand.argument);
                return true;
            }
        }

        handlePetCommand(player, cleanedText, normalizedText);
        return false;
    }

    private void handlePetCommand(Player player, String originalText, String normalizedText) {
        if (normalizedText.startsWith("ten con la ") && originalText.length() > 11) {
            PetService.gI().changeNamePet(player, originalText.substring(11).trim());
        }

        if (player.pet == null) {
            return;
        }
        switch (normalizedText) {
            case "di theo", "follow" -> player.pet.changeStatus(Pet.FOLLOW);
            case "bao ve", "protect" -> player.pet.changeStatus(Pet.PROTECT);
            case "tan cong", "attack" -> player.pet.changeStatus(Pet.ATTACK);
            case "ve nha", "go home" -> player.pet.changeStatus(Pet.GOHOME);
            case "bien hinh" -> player.pet.transform();
            case "sach tuyet ky" -> equipPetSkillBook(player);
            default -> {
            }
        }
    }

    private void equipPetSkillBook(Player player) {
        int typePet = player.pet.typePet;
        if (typePet != 2 && typePet != 3 && typePet != 4) {
            Service.gI().sendThongBaoOK(player,
                    "Chỉ đệ tử (Goku vô cực, Kid Beerus, Jiren) mới có thể dùng sách tuyệt kỹ.");
            return;
        }

        for (int i = 0; i < player.inventory.itemsBag.size(); i++) {
            Item item = player.inventory.itemsBag.get(i);
            if (item == null || !item.isNotNullItem() || item.template.type != 25) {
                continue;
            }
            if (player.pet.nPoint == null || player.pet.nPoint.power < 1_500_000) {
                Service.gI().sendThongBaoOK(player,
                        "Đệ tử cần đạt 1tr5 sức mạnh để trang bị.");
                return;
            }

            Item oldItem = InventoryService.gI().putItemBody(player.pet, item);
            player.inventory.itemsBag.set(i, oldItem);
            InventoryService.gI().sendItemBags(player);
            InventoryService.gI().sendItemBody(player);
            Service.gI().Send_Caitrang(player.pet);
            Service.gI().Send_Caitrang(player);
            Service.gI().sendThongBao(player,
                    "Đã dùng " + item.template.name + " cho đệ tử");
            return;
        }
    }

    static boolean isSupportedAdminCommand(String text) {
        if (text == null) {
            return false;
        }
        String normalizedText = normalize(text);
        return EXACT_ADMIN_COMMANDS.contains(normalizedText)
                || parseParameterizedCommand(normalizedText) != null;
    }

    private static ParsedCommand parseParameterizedCommand(String text) {
        String[] parts = text.split("\\s+", 2);
        if (parts.length != 2 || parts[1].isBlank()) {
            return null;
        }

        String name = parts[0];
        String argument = parts[1].trim();
        if (Set.of("m", "n", "dm", "hp", "ki", "up", "upp").contains(name)) {
            return argument.matches("[+-]?\\d+")
                    ? new ParsedCommand(name, argument) : null;
        }
        if (name.equals("i")
                && argument.matches("\\d+(?:\\s+\\d+)?(?:\\s+\\d+:-?\\d+)*")) {
            return new ParsedCommand(name, argument);
        }
        return null;
    }

    private static String normalize(String text) {
        return text.trim().toLowerCase(Locale.ROOT);
    }

    private static final class ParsedCommand {

        private final String name;
        private final String argument;

        private ParsedCommand(String name, String argument) {
            this.name = name;
            this.argument = argument;
        }
    }
}
