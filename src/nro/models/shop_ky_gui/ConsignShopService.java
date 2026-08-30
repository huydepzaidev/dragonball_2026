package nro.models.shop_ky_gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nro.models.consts.ConstNpc;
import nro.models.item.Item;
import nro.models.item.Item.ItemOption;
import nro.models.map.service.NpcService;
import nro.models.network.Message;
import nro.models.player.Player;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.Service;

/**
 *
 * @author By Mr Blue
 * 
 */

public class ConsignShopService {

    private static ConsignShopService instance;

    public static ConsignShopService gI() {
        if (instance == null) {
            instance = new ConsignShopService();
        }
        return instance;
    }

    private List<ConsignItem> getItemKyGui2(Player pl, byte tab, byte to, byte max) {
        List<ConsignItem> filtered = ConsignShopManager.gI().listItem.stream()
                .filter(it -> it != null && it.tab == tab && !it.isBuy)
                .sorted(Comparator.comparingInt((ConsignItem it) -> it.isUpTop).reversed())
                .collect(Collectors.toList());

        List<ConsignItem> result = new ArrayList<>();
        for (int i = to; i <= max && i < filtered.size(); i++) {
            result.add(filtered.get(i));
        }
        return result;
    }

    private List<ConsignItem> getItemKyGui(Player pl, byte tab, byte... max) {
        List<ConsignItem> filtered = ConsignShopManager.gI().listItem.stream()
                .filter(it -> it != null && it.tab == tab && !it.isBuy && it.player_sell != pl.id)
                .sorted(Comparator.comparingInt((ConsignItem it) -> it.isUpTop).reversed())
                .collect(Collectors.toList());

        List<ConsignItem> result = new ArrayList<>();

        if (max.length == 2) {
            int from = max[0], to = max[1];
            for (int i = from; i < to && i < filtered.size(); i++) {
                result.add(filtered.get(i));
            }
        } else if (max.length == 1) {
            int limit = max[0];
            for (int i = 0; i < limit && i < filtered.size(); i++) {
                result.add(filtered.get(i));
            }
        } else {
            return filtered;
        }

        return result;
    }

    private List<ConsignItem> getItemKyGui() {
        return ConsignShopManager.gI().listItem.stream()
                .filter(it -> it != null && !it.isBuy)
                .sorted(Comparator.comparingInt((ConsignItem it) -> it.isUpTop).reversed())
                .collect(Collectors.toList());
    }

    private boolean isKyGui(Item item) {
        switch (item.template.type) {
            case 27:
                switch (item.template.id) {
                    case 921:
                    case 1155:
                    case 1156:
                    case 568:
                        return true;
                }
                return false;
            case 21:
            case 72:
                return true;
        }
        for (int i = 0; i < item.itemOptions.size(); i++) {
            if (item.itemOptions.get(i).optionTemplate.id == 86) {
                return true;
            }
        }
        return false;
    }

    private boolean SubThoiVang(Player pl, int quatity) {
        for (Item item : pl.inventory.itemsBag) {
            if (item.isNotNullItem() && item.template.id == 457 && item.quantity >= quatity) {
                nro.models.services.InventoryService.gI().subQuantityItemsBag(pl, item, quatity);
                return true;
            }
        }
        return false;
    }

    public void buyItem(Player pl, int id) {
        Service.gI().sendThongBao(pl, "Chức năng ký gửi hiện đang tạm đóng");
    }

    public ConsignItem getItemBuy(int id) {
        for (ConsignItem it : getItemKyGui()) {
            if (it != null && it.id == id) {
                return it;
            }
        }
        return null;
    }

    public ConsignItem getItemBuy(Player pl, int id) {
        for (ConsignItem it : ConsignShopManager.gI().listItem) {
            if (it != null && it.id == id && it.player_sell == pl.id) {
                return it;
            }
        }
        return null;
    }

    public void openShopKyGui(Player pl, byte index, int page) {
        Service.gI().sendThongBao(pl, "Chức năng ký gửi hiện đang tạm đóng");
    }

    public void upItemToTop(Player pl, int id) {
        Service.gI().sendThongBao(pl, "Chức năng ký gửi hiện đang tạm đóng");
    }

    public void StartupItemToTop(Player pl) {
        Service.gI().sendThongBao(pl, "Chức năng ký gửi hiện đang tạm đóng");
    }

    public void claimOrDel(Player pl, byte action, int id) {
        Service.gI().sendThongBao(pl, "Chức năng ký gửi hiện đang tạm đóng");
    }

    public List<ConsignItem> getItemCanKiGui(Player pl) {
        return new ArrayList<>();
    }

    public boolean itemCanConsign(Item it) {
        return false;
    }

    public int getMaxId() {
        return 0;
    }

    public byte getTabKiGui(Item it) {
        return 0;
    }

    public void KiGui(Player pl, int id, int money, byte moneyType, int quantity) {
        Service.gI().sendThongBao(pl, "Chức năng ký gửi hiện đang tạm đóng");
    }

    public void openShopKyGui(Player pl) {
        Service.gI().sendThongBao(pl, "Chức năng ký gửi hiện đang tạm đóng");
    }
}
