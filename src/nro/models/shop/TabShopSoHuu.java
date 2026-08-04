package nro.models.shop;

import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.player_badges.BagesTemplate;
import nro.models.shop.ItemShop;
import nro.models.shop.TabShop;
import nro.models.task.BadgesTaskService;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author By Mr Blue
 * 
 */

public class TabShopSoHuu extends TabShop {

    public TabShopSoHuu(TabShop tabShop, Player player) {
        this.itemShops = new ArrayList<>();
        this.shop = tabShop.shop;
        this.id = tabShop.id;
        this.name = tabShop.name + BagesTemplate.listEffect(player).size();

        for (ItemShop itemShop : tabShop.itemShops) {
            if (itemShop.temp.gender == player.gender || itemShop.temp.gender > 2) {
                boolean shouldAdd = false;
                for (Integer i : BagesTemplate.listEffect(player)) {
                    if (itemShop.temp.id == i) {
                        shouldAdd = true;
                        break;
                    }
                }
                if (shouldAdd) {
                    for (Item.ItemOption option : itemShop.options) {
                        if (option.optionTemplate.id == 93) {
                            option.param = BadgesTaskService.sendDay(player, BagesTemplate.fineIdEffectbyIdItem(itemShop.temp.id));
                            break;
                        }
                    }
                    ItemShop ownedTitle = new ItemShop(itemShop);
                    // A zero price marks an owned title as a single use action.
                    ownedTitle.cost = 0;
                    this.itemShops.add(ownedTitle);
                }
            }
        }
    }
}
