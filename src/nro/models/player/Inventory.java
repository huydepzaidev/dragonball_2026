package nro.models.player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import nro.models.consts.ConstTaskBadges;
import nro.models.item.Item;
import nro.models.item.Item.ItemOption;
import nro.models.task.BadgesTaskService;

/**
 *
 * @author By Mr Blue
 *
 */
public class Inventory {

    public static final long LIMIT_GOLD = 1_000_000_000_000L;
    public static final long GOLD_BAR_SELL_PRICE = 500_000_000L;
    public static final int MAX_ITEMS_BAG = 120;
    public static final int MAX_ITEMS_BOX = 120;
    public static final int BABY_DRAGON_MIN_ID = 1765;
    public static final int BABY_DRAGON_MAX_ID = 1771;
    public static final int BABY_DRAGON_SET_SIZE = 7;
    public Item trainArmor;
    public List<String> giftCode;
    public List<Item> itemsBody;
    public List<Item> itemsBag;
    public List<Item> itemsBox;

    public List<Item> itemsBoxCrackBall;
    public List<Item> itemsDaBan;

    public long gold;
    public int gem;
    public int ruby;
    public int coupon;
    public int event;
    public Iterable<Item> items;

    public Inventory() {
        itemsBody = new ArrayList<>();
        itemsBag = new ArrayList<>();
        itemsBox = new ArrayList<>();
        itemsBoxCrackBall = new ArrayList<>();
        itemsDaBan = new ArrayList<>();
        giftCode = new ArrayList<>();
    }

    public int getGem() {
        return this.gem;
    }

    public int getParam(Item it, int id) {
        for (ItemOption op : it.itemOptions) {
            if (op != null && op.optionTemplate.id == id) {
                return op.param;
            }
        }
        return 0;
    }

    public boolean haveOption(List<Item> l, int index, int id) {
        Item it = l.get(index);
        if (it != null && it.isNotNullItem()) {
            return it.itemOptions.stream().anyMatch(op -> op != null && op.optionTemplate.id == id);
        }
        return false;
    }

    public void subGem(int num) {
        this.gem -= num;
    }

    public void subGold(int num) {
        this.gold -= num;
    }

    public void addGold(int gold) {
        this.gold += gold;
        if (this.gold > LIMIT_GOLD) {
            this.gold = LIMIT_GOLD;
        }
    }

    public void dispose() {
        if (this.trainArmor != null) {
            this.trainArmor.dispose();
        }
        this.trainArmor = null;
        if (this.itemsBody != null) {
            for (Item it : this.itemsBody) {
                it.dispose();
            }
            this.itemsBody.clear();
        }
        if (this.itemsBag != null) {
            for (Item it : this.itemsBag) {
                it.dispose();
            }
            this.itemsBag.clear();
        }
        if (this.itemsBox != null) {
            for (Item it : this.itemsBox) {
                it.dispose();
            }
            this.itemsBox.clear();
        }
        if (this.itemsBoxCrackBall != null) {
            for (Item it : this.itemsBoxCrackBall) {
                it.dispose();
            }
            this.itemsBoxCrackBall.clear();
        }
        if (this.itemsDaBan != null) {
            for (Item it : this.itemsDaBan) {
                it.dispose();
            }
            this.itemsDaBan.clear();
        }
        this.itemsBody = null;
        this.itemsBag = null;
        this.itemsBox = null;
        this.itemsBoxCrackBall = null;
        this.itemsDaBan = null;
    }

    public void checkAndUpdateMeRongBadges(Player player) {
        Set<Integer> checkedItemIds = new HashSet<>();

        List<List<Item>> inventories = Arrays.asList(
                this.itemsBag,
                this.itemsBox,
                this.itemsBody
        );

        for (List<Item> inventory : inventories) {
            if (inventory == null) {
                continue;
            }
            for (Item item : inventory) {
                if (item != null && item.template != null && isPermanent(item)) {
                    int itemId = item.template.id;
                    if (isBabyDragon(itemId) && !checkedItemIds.contains(itemId)) {
                        BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.ME_RONG, 1);
                        checkedItemIds.add(itemId);
                    }
                }
            }
        }
    }

    public boolean hasFullPermanentBabyDragonSet() {
        Set<Integer> itemIds = new HashSet<>();
        collectPermanentBabyDragons(itemsBag, itemIds);
        collectPermanentBabyDragons(itemsBox, itemIds);
        collectPermanentBabyDragons(itemsBody, itemIds);
        return itemIds.size() == BABY_DRAGON_SET_SIZE;
    }

    public boolean hasPermanentBabyDragon(int itemId) {
        if (!isBabyDragon(itemId)) {
            return false;
        }
        return containsPermanentBabyDragon(itemsBag, itemId)
                || containsPermanentBabyDragon(itemsBox, itemId)
                || containsPermanentBabyDragon(itemsBody, itemId);
    }

    public static boolean isBabyDragon(int itemId) {
        return itemId >= BABY_DRAGON_MIN_ID && itemId <= BABY_DRAGON_MAX_ID;
    }

    private void collectPermanentBabyDragons(List<Item> items, Set<Integer> itemIds) {
        if (items == null) {
            return;
        }
        for (Item item : items) {
            if (item != null && item.template != null
                    && isBabyDragon(item.template.id) && isPermanent(item)) {
                itemIds.add((int) item.template.id);
            }
        }
    }

    private boolean containsPermanentBabyDragon(List<Item> items, int itemId) {
        if (items == null) {
            return false;
        }
        for (Item item : items) {
            if (item != null && item.template != null
                    && item.template.id == itemId && isPermanent(item)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPermanent(Item item) {
        ItemOption expiration = item == null ? null : item.getOptionById(93);
        return item != null && (expiration == null || expiration.param <= 0);
    }
}
