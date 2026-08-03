package nro.models.npc_list;

import nro.models.consts.ConstNpc;
import nro.models.item.Item;
import nro.models.npc.Npc;
import nro.models.player.NPoint;
import nro.models.player.Player;
import nro.models.services.InventoryService;
import nro.models.services.OpenPowerService;
import nro.models.services.Service;
import nro.models.utils.Util;

public class QuocVuong extends Npc {

    private static final byte MAX_LIMIT_CUSTOM = NPoint.MAX_LIMIT;
    private static final byte EARLY_LIMIT_MAX = 3;
    private static final int EARLY_LIMIT_OPEN_COST = 500_000_000;
    private static final byte DIVINE_ITEM_LIMIT_MIN = 4;
    private static final byte DIVINE_ITEM_LIMIT_MAX = 5;
    private static final int DIVINE_ITEM_OPEN_COST = 500_000_000;
    private static final byte DESTROY_ITEM_LIMIT_MIN = 6;
    private static final byte DESTROY_ITEM_LIMIT_MAX = 7;
    private static final int DESTROY_ITEM_OPEN_COST = 500_000_000;
    private static final byte ANGEL_ITEM_LIMIT = 8;
    private static final int ANGEL_ITEM_OPEN_COST = 500_000_000;

    public QuocVuong(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        this.createOtherMenu(player, ConstNpc.BASE_MENU,
                "Con muốn nâng giới hạn sức mạnh cho bản thân hay đệ tử?",
                "Bản thân", "Đệ tử", "Từ chối");
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }

        int menuId = player.idMark.getIndexMenu();
        if (player.idMark.isBaseMenu()) {
            switch (select) {
                case 0 -> showMyLimitMenu(player);
                case 1 -> showPetLimitMenu(player);
            }
        } else if (isEarlyLimitMenu(menuId)) {
            handleEarlyLimitMenu(player, select,
                    (byte) (menuId - ConstNpc.OPEN_POWER_MYSEFT_GOLD));
        } else if (menuId == ConstNpc.OPEN_POWER_MYSEFT_DIVINE) {
            handleDivineItemLimitMenu(player, select, DIVINE_ITEM_LIMIT_MIN);
        } else if (menuId == ConstNpc.OPEN_POWER_MYSEFT_DIVINE_2) {
            handleDivineItemLimitMenu(player, select, DIVINE_ITEM_LIMIT_MAX);
        } else if (isDestroyItemLimitMenu(menuId)) {
            handleDestroyItemLimitMenu(player, select,
                    (byte) (menuId - ConstNpc.OPEN_POWER_MYSEFT_DESTROY + DESTROY_ITEM_LIMIT_MIN));
        } else if (menuId == ConstNpc.OPEN_POWER_MYSEFT_ANGEL) {
            handleAngelItemLimitMenu(player, select);
        } else if (isPetEarlyLimitMenu(menuId)) {
            handlePetEarlyLimitMenu(player, select,
                    (byte) (menuId - ConstNpc.OPEN_POWER_PET_GOLD));
        } else if (menuId == ConstNpc.OPEN_POWER_PET_DIVINE) {
            handlePetDivineLimitMenu(player, select, DIVINE_ITEM_LIMIT_MIN);
        } else if (menuId == ConstNpc.OPEN_POWER_PET_DIVINE_2) {
            handlePetDivineLimitMenu(player, select, DIVINE_ITEM_LIMIT_MAX);
        } else if (isPetDestroyLimitMenu(menuId)) {
            handlePetDestroyLimitMenu(player, select,
                    (byte) (menuId - ConstNpc.OPEN_POWER_PET_DESTROY + DESTROY_ITEM_LIMIT_MIN));
        } else if (menuId == ConstNpc.OPEN_POWER_PET_ANGEL) {
            handlePetAngelLimitMenu(player, select);
        }
    }

    private void showMyLimitMenu(Player player) {
        if (player.nPoint.limitPower >= MAX_LIMIT_CUSTOM) {
            this.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Sức mạnh của con đã đạt tới giới hạn hiện tại", "Đóng");
            return;
        }

        if (player.nPoint.limitPower >= 0 && player.nPoint.limitPower <= EARLY_LIMIT_MAX) {
            int menuId = ConstNpc.OPEN_POWER_MYSEFT_GOLD + player.nPoint.limitPower;
            this.createOtherMenu(player, menuId,
                    "Ta sẽ mở giới hạn sức mạnh của con lên "
                    + Util.numberToMoney(player.nPoint.getPowerNextLimit())
                    + " với chi phí 500 triệu vàng.",
                    "Mở giới hạn\n500 triệu vàng",
                    "Đóng");
            return;
        }

        if (player.nPoint.limitPower >= DIVINE_ITEM_LIMIT_MIN
                && player.nPoint.limitPower <= DIVINE_ITEM_LIMIT_MAX) {
            int divineMenuId = player.nPoint.limitPower == DIVINE_ITEM_LIMIT_MIN
                    ? ConstNpc.OPEN_POWER_MYSEFT_DIVINE
                    : ConstNpc.OPEN_POWER_MYSEFT_DIVINE_2;
            this.createOtherMenu(player, divineMenuId,
                    "Để mở giới hạn sức mạnh lên "
                    + Util.numberToMoney(player.nPoint.getPowerNextLimit())
                    + ", con phải mặc 1 món đồ Thần và có 500 triệu vàng. Đồ Thần đang mặc sẽ bị mất.",
                    "Mở giới hạn\n500 triệu vàng\n+ 1 đồ Thần",
                    "Đóng");
            return;
        }

        if (player.nPoint.limitPower >= DESTROY_ITEM_LIMIT_MIN
                && player.nPoint.limitPower <= DESTROY_ITEM_LIMIT_MAX) {
            int destroyMenuId = ConstNpc.OPEN_POWER_MYSEFT_DESTROY
                    + player.nPoint.limitPower - DESTROY_ITEM_LIMIT_MIN;
            this.createOtherMenu(player, destroyMenuId,
                    "Để mở giới hạn sức mạnh lên "
                    + Util.numberToMoney(player.nPoint.getPowerNextLimit())
                    + ", con phải mặc 1 món đồ Hủy Diệt và có 500 triệu vàng. Đồ Hủy Diệt đang mặc sẽ bị mất.",
                    "Mở giới hạn\n500 triệu vàng\n+ 1 đồ Hủy Diệt",
                    "Đóng");
            return;
        }

        if (player.nPoint.limitPower == ANGEL_ITEM_LIMIT) {
            this.createOtherMenu(player, ConstNpc.OPEN_POWER_MYSEFT_ANGEL,
                    "Để mở giới hạn sức mạnh lên "
                    + Util.numberToMoney(player.nPoint.getPowerNextLimit())
                    + ", con phải mặc 1 món đồ Thiên Sứ và có 500 triệu vàng. Đồ Thiên Sứ đang mặc sẽ bị mất.",
                    "Mở giới hạn\n500 triệu vàng\n+ 1 đồ Thiên Sứ",
                    "Đóng");
            return;
        }

        Service.gI().sendThongBao(player, "Không thể thực hiện");
    }

    private void showPetLimitMenu(Player player) {
        if (player.pet == null) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
            return;
        }
        if (player.pet.nPoint.limitPower >= MAX_LIMIT_CUSTOM) {
            this.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Sức mạnh của đệ tử đã đạt tới giới hạn hiện tại", "Đóng");
            return;
        }

        if (player.pet.nPoint.limitPower >= 0 && player.pet.nPoint.limitPower <= EARLY_LIMIT_MAX) {
            int menuId = ConstNpc.OPEN_POWER_PET_GOLD + player.pet.nPoint.limitPower;
            this.createOtherMenu(player, menuId,
                    "Ta sẽ mở giới hạn sức mạnh của đệ tử lên "
                    + Util.numberToMoney(player.pet.nPoint.getPowerNextLimit())
                    + " với chi phí 500 triệu vàng.",
                    "Mở giới hạn\n500 triệu vàng", "Đóng");
            return;
        }
        if (player.pet.nPoint.limitPower >= DIVINE_ITEM_LIMIT_MIN
                && player.pet.nPoint.limitPower <= DIVINE_ITEM_LIMIT_MAX) {
            int menuId = player.pet.nPoint.limitPower == DIVINE_ITEM_LIMIT_MIN
                    ? ConstNpc.OPEN_POWER_PET_DIVINE : ConstNpc.OPEN_POWER_PET_DIVINE_2;
            this.createOtherMenu(player, menuId,
                    "Để mở giới hạn sức mạnh của đệ tử lên "
                    + Util.numberToMoney(player.pet.nPoint.getPowerNextLimit())
                    + ", sư phụ phải mặc 1 món đồ Thần và có 500 triệu vàng. Đồ đang mặc sẽ bị mất.",
                    "Mở giới hạn\n500 triệu vàng\n+ 1 đồ Thần", "Đóng");
            return;
        }
        if (player.pet.nPoint.limitPower >= DESTROY_ITEM_LIMIT_MIN
                && player.pet.nPoint.limitPower <= DESTROY_ITEM_LIMIT_MAX) {
            int menuId = ConstNpc.OPEN_POWER_PET_DESTROY
                    + player.pet.nPoint.limitPower - DESTROY_ITEM_LIMIT_MIN;
            this.createOtherMenu(player, menuId,
                    "Để mở giới hạn sức mạnh của đệ tử lên "
                    + Util.numberToMoney(player.pet.nPoint.getPowerNextLimit())
                    + ", sư phụ phải mặc 1 món đồ Hủy Diệt và có 500 triệu vàng. Đồ đang mặc sẽ bị mất.",
                    "Mở giới hạn\n500 triệu vàng\n+ 1 đồ Hủy Diệt", "Đóng");
            return;
        }
        if (player.pet.nPoint.limitPower == ANGEL_ITEM_LIMIT) {
            this.createOtherMenu(player, ConstNpc.OPEN_POWER_PET_ANGEL,
                    "Để mở giới hạn sức mạnh của đệ tử lên "
                    + Util.numberToMoney(player.pet.nPoint.getPowerNextLimit())
                    + ", sư phụ phải mặc 1 món đồ Thiên Sứ và có 500 triệu vàng. Đồ đang mặc sẽ bị mất.",
                    "Mở giới hạn\n500 triệu vàng\n+ 1 đồ Thiên Sứ", "Đóng");
            return;
        }
        Service.gI().sendThongBao(player, "Không thể thực hiện");
    }

    private boolean isEarlyLimitMenu(int menuId) {
        return menuId >= ConstNpc.OPEN_POWER_MYSEFT_GOLD
                && menuId <= ConstNpc.OPEN_POWER_MYSEFT_GOLD + EARLY_LIMIT_MAX;
    }

    private boolean isDestroyItemLimitMenu(int menuId) {
        return menuId >= ConstNpc.OPEN_POWER_MYSEFT_DESTROY
                && menuId <= ConstNpc.OPEN_POWER_MYSEFT_DESTROY_2;
    }

    private boolean isPetEarlyLimitMenu(int menuId) {
        return menuId >= ConstNpc.OPEN_POWER_PET_GOLD
                && menuId <= ConstNpc.OPEN_POWER_PET_GOLD + EARLY_LIMIT_MAX;
    }

    private boolean isPetDestroyLimitMenu(int menuId) {
        return menuId >= ConstNpc.OPEN_POWER_PET_DESTROY
                && menuId <= ConstNpc.OPEN_POWER_PET_DESTROY_2;
    }

    private void handleEarlyLimitMenu(Player player, int select, byte expectedLimit) {
        if (select != 0) {
            return;
        }
        synchronized (player) {
            if (expectedLimit < 0 || expectedLimit > EARLY_LIMIT_MAX
                    || player.nPoint.limitPower != expectedLimit) {
                sendExpiredLimitMenuMessage(player);
                return;
            }
            if (player.inventory.gold < EARLY_LIMIT_OPEN_COST) {
                Service.gI().sendThongBao(player, "Bạn không đủ vàng để mở, còn thiếu "
                        + Util.numberToMoney(EARLY_LIMIT_OPEN_COST - player.inventory.gold) + " vàng");
                return;
            }
            if (OpenPowerService.gI().openPowerSpeed(player)) {
                player.inventory.gold -= EARLY_LIMIT_OPEN_COST;
                Service.gI().sendMoney(player);
            }
        }
    }

    private void handleDivineItemLimitMenu(Player player, int select, byte expectedLimit) {
        if (select != 0) {
            return;
        }
        synchronized (player) {
            if (expectedLimit < DIVINE_ITEM_LIMIT_MIN || expectedLimit > DIVINE_ITEM_LIMIT_MAX
                    || player.nPoint.limitPower != expectedLimit) {
                sendExpiredLimitMenuMessage(player);
                return;
            }
            if (player.inventory.gold < DIVINE_ITEM_OPEN_COST) {
                Service.gI().sendThongBao(player, "Bạn không đủ vàng để mở, còn thiếu "
                        + Util.numberToMoney(DIVINE_ITEM_OPEN_COST - player.inventory.gold) + " vàng");
                return;
            }
            Item divineItem = findEquippedDivineItem(player);
            if (divineItem == null) {
                Service.gI().sendThongBao(player,
                        "Bạn phải mặc ít nhất 1 món đồ Thần để mở giới hạn sức mạnh");
                return;
            }
            if (OpenPowerService.gI().openPowerSpeed(player)) {
                player.inventory.gold -= DIVINE_ITEM_OPEN_COST;
                InventoryService.gI().subQuantityItemsBody(player, divineItem, 1);
                sendSelfUpgradeUpdates(player);
            }
        }
    }

    private Item findEquippedDivineItem(Player player) {
        for (Item item : player.inventory.itemsBody) {
            if (item != null && item.isNotNullItem() && item.isThanLinh()) {
                return item;
            }
        }
        return null;
    }

    private void handleDestroyItemLimitMenu(Player player, int select, byte expectedLimit) {
        if (select != 0) {
            return;
        }
        synchronized (player) {
            if (expectedLimit < DESTROY_ITEM_LIMIT_MIN || expectedLimit > DESTROY_ITEM_LIMIT_MAX
                    || player.nPoint.limitPower != expectedLimit) {
                sendExpiredLimitMenuMessage(player);
                return;
            }
            if (player.inventory.gold < DESTROY_ITEM_OPEN_COST) {
                Service.gI().sendThongBao(player, "Bạn không đủ vàng để mở, còn thiếu "
                        + Util.numberToMoney(DESTROY_ITEM_OPEN_COST - player.inventory.gold) + " vàng");
                return;
            }
            Item destroyItem = findEquippedDestroyItem(player);
            if (destroyItem == null) {
                Service.gI().sendThongBao(player,
                        "Bạn phải mặc ít nhất 1 món đồ Hủy Diệt để mở giới hạn sức mạnh");
                return;
            }
            if (OpenPowerService.gI().openPowerSpeed(player)) {
                player.inventory.gold -= DESTROY_ITEM_OPEN_COST;
                InventoryService.gI().subQuantityItemsBody(player, destroyItem, 1);
                sendSelfUpgradeUpdates(player);
            }
        }
    }

    private Item findEquippedDestroyItem(Player player) {
        for (Item item : player.inventory.itemsBody) {
            if (item != null && item.isNotNullItem() && item.isDHD()) {
                return item;
            }
        }
        return null;
    }

    private void handleAngelItemLimitMenu(Player player, int select) {
        if (select != 0) {
            return;
        }
        synchronized (player) {
            if (player.nPoint.limitPower != ANGEL_ITEM_LIMIT) {
                sendExpiredLimitMenuMessage(player);
                return;
            }
            if (player.inventory.gold < ANGEL_ITEM_OPEN_COST) {
                Service.gI().sendThongBao(player, "Bạn không đủ vàng để mở, còn thiếu "
                        + Util.numberToMoney(ANGEL_ITEM_OPEN_COST - player.inventory.gold) + " vàng");
                return;
            }
            Item angelItem = findEquippedAngelItem(player);
            if (angelItem == null) {
                Service.gI().sendThongBao(player,
                        "Bạn phải mặc ít nhất 1 món đồ Thiên Sứ để mở giới hạn sức mạnh");
                return;
            }
            if (OpenPowerService.gI().openPowerSpeed(player)) {
                player.inventory.gold -= ANGEL_ITEM_OPEN_COST;
                InventoryService.gI().subQuantityItemsBody(player, angelItem, 1);
                sendSelfUpgradeUpdates(player);
            }
        }
    }

    private Item findEquippedAngelItem(Player player) {
        for (Item item : player.inventory.itemsBody) {
            if (item != null && item.isNotNullItem() && item.isDTS()) {
                return item;
            }
        }
        return null;
    }

    private void handlePetEarlyLimitMenu(Player player, int select, byte expectedLimit) {
        if (select != 0) {
            return;
        }
        synchronized (player) {
            if (player.pet == null || expectedLimit < 0 || expectedLimit >= MAX_LIMIT_CUSTOM
                    || player.pet.nPoint.limitPower != expectedLimit) {
                sendExpiredLimitMenuMessage(player);
                return;
            }
            if (player.inventory.gold < EARLY_LIMIT_OPEN_COST) {
                Service.gI().sendThongBao(player, "Bạn không đủ vàng để mở, còn thiếu "
                        + Util.numberToMoney(EARLY_LIMIT_OPEN_COST - player.inventory.gold)
                        + " vàng");
                return;
            }
            if (openPetLimitAtMarker(player, expectedLimit)) {
                player.inventory.gold -= EARLY_LIMIT_OPEN_COST;
                sendPetUpgradeUpdates(player);
            }
        }
    }

    private void handlePetDivineLimitMenu(Player player, int select, byte expectedLimit) {
        handlePetItemLimitMenu(player, select, expectedLimit, DIVINE_ITEM_OPEN_COST,
                "Thần", this::findEquippedDivineItem);
    }

    private void handlePetDestroyLimitMenu(Player player, int select, byte expectedLimit) {
        handlePetItemLimitMenu(player, select, expectedLimit, DESTROY_ITEM_OPEN_COST,
                "Hủy Diệt", this::findEquippedDestroyItem);
    }

    private void handlePetAngelLimitMenu(Player player, int select) {
        handlePetItemLimitMenu(player, select, ANGEL_ITEM_LIMIT, ANGEL_ITEM_OPEN_COST,
                "Thiên Sứ", this::findEquippedAngelItem);
    }

    private void handlePetItemLimitMenu(Player player, int select, byte expectedLimit,
            int cost, String itemType, java.util.function.Function<Player, Item> itemFinder) {
        if (select != 0) {
            return;
        }
        synchronized (player) {
            if (player.pet == null || player.pet.nPoint.limitPower != expectedLimit) {
                sendExpiredLimitMenuMessage(player);
                return;
            }
            if (player.inventory.gold < cost) {
                Service.gI().sendThongBao(player, "Bạn không đủ vàng để mở, còn thiếu "
                        + Util.numberToMoney(cost - player.inventory.gold) + " vàng");
                return;
            }
            Item item = itemFinder.apply(player);
            if (item == null) {
                Service.gI().sendThongBao(player,
                        "Sư phụ phải mặc ít nhất 1 món đồ " + itemType + " để mở giới hạn cho đệ tử");
                return;
            }
            if (openPetLimitAtMarker(player, expectedLimit)) {
                player.inventory.gold -= cost;
                InventoryService.gI().subQuantityItemsBody(player, item, 1);
                sendPetUpgradeUpdates(player);
            }
        }
    }

    /**
     * Opens exactly the next disciple limit marker and refreshes the capped
     * disciple stats immediately, preventing a stale limit/stat combination.
     */
    private boolean openPetLimitAtMarker(Player master, byte expectedCurrentLimit) {
        if (master.pet == null || master.pet.nPoint.limitPower != expectedCurrentLimit
                || expectedCurrentLimit < 0 || expectedCurrentLimit >= MAX_LIMIT_CUSTOM) {
            sendExpiredLimitMenuMessage(master);
            return false;
        }
        byte targetLimit = (byte) (expectedCurrentLimit + 1);
        if (!OpenPowerService.gI().openPowerSpeed(master.pet)) {
            return false;
        }
        // Keep the persisted/runtime value tied to the marker that was opened.
        master.pet.nPoint.limitPower = targetLimit;
        syncPetStatsToLimit(master);
        return true;
    }

    /**
     * Re-applies the disciple's HP/KI/damage caps after an unlock. Defense and
     * critical growth remain disabled by NPoint.increasePoint for disciples.
     */
    private void syncPetStatsToLimit(Player master) {
        if (master.pet == null) {
            return;
        }
        NPoint point = master.pet.nPoint;
        int hpKiLimit = point.getHpMpLimit();
        point.hpg = Math.min(point.hpg, hpKiLimit);
        point.mpg = Math.min(point.mpg, hpKiLimit);
        point.dameg = Math.min(point.dameg, point.getDameLimit());
        point.calPoint();
    }

    private void sendSelfUpgradeUpdates(Player player) {
        Service.gI().sendMoney(player);
        InventoryService.gI().sendItemBody(player);
        Service.gI().point(player);
        Service.gI().Send_Caitrang(player);
    }

    private void sendPetUpgradeUpdates(Player master) {
        Service.gI().sendMoney(master);
        InventoryService.gI().sendItemBody(master);
        if (master.pet != null) {
            Service.gI().point(master.pet);
        }
        Service.gI().point(master);
        Service.gI().Send_Caitrang(master);
    }

    private void sendExpiredLimitMenuMessage(Player player) {
        Service.gI().sendThongBao(player,
                "Thông tin mở giới hạn đã thay đổi, vui lòng nói chuyện lại với Quốc Vương");
    }
}
