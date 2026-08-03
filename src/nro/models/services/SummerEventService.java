package nro.models.services;

import nro.models.consts.ConstItem;
import nro.models.item.Item;
import nro.models.map.ItemMap;
import nro.models.map.Map;
import nro.models.map.service.MapService;
import nro.models.player.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Rules shared by the beach-item shop and the summer material drop event.
 */
public final class SummerEventService {

    public static final int MATERIAL_DROP_RATE_SCALE = 1_000;
    public static final int MATERIAL_BASE_DROP_RATE = 100; // 10%
    public static final int SUMMER_DROP_BONUS_PERCENT = 70;
    public static final int SUMMER_MATERIAL_OPTION_ID = 251;

    private static final int TILE_WATER_FLOW = 32;
    private static final int TILE_WATER = 64;
    private static final int TILE_UNDERWATER = 2048;
    private static final int[][] WATER_FLOW_TILES = MapService.gI().readTileIndexTileType(TILE_WATER_FLOW);
    private static final int[][] WATER_TILES = MapService.gI().readTileIndexTileType(TILE_WATER);
    private static final int[][] UNDERWATER_TILES = MapService.gI().readTileIndexTileType(TILE_UNDERWATER);
    private static final ConcurrentMap<Integer, Boolean> WATER_MAP_CACHE = new ConcurrentHashMap<>();

    private SummerEventService() {
    }

    public static boolean isBeachShorts(int itemId) {
        return itemId >= ConstItem.QUAN_DI_BIEN && itemId <= ConstItem.QUAN_DI_BIEN_693;
    }

    public static int getBeachShortsId(int gender) {
        return switch (gender) {
            case 0 -> ConstItem.QUAN_DI_BIEN;
            case 1 -> ConstItem.QUAN_DI_BIEN_692;
            case 2 -> ConstItem.QUAN_DI_BIEN_693;
            default -> -1;
        };
    }

    public static boolean isSummerMaterial(int itemId) {
        return itemId >= ConstItem.VO_OC && itemId <= ConstItem.SAO_BIEN;
    }

    public static int getBalancedMaterialDropId(Player player) {
        if (player == null || player.inventory == null) {
            return -1;
        }
        int selectedId = -1;
        int lowestQuantity = Integer.MAX_VALUE;
        for (int itemId = ConstItem.VO_OC; itemId <= ConstItem.SAO_BIEN; itemId++) {
            int quantity = countItems(player.inventory.itemsBag, itemId)
                    + countItems(player.inventory.itemsBox, itemId)
                    + countPendingMapItems(player, itemId);
            if (quantity < lowestQuantity) {
                lowestQuantity = quantity;
                selectedId = itemId;
            }
        }
        return lowestQuantity >= 99 ? -1 : selectedId;
    }

    public static boolean isWearingBeachShortsWithoutShirtOrCostume(Player player) {
        if (player == null || player.inventory == null || player.inventory.itemsBody == null
                || player.inventory.itemsBody.size() <= 1) {
            return false;
        }

        Item pants = player.inventory.itemsBody.get(1);
        if (pants == null || !pants.isNotNullItem() || pants.template.id != getBeachShortsId(player.gender)) {
            return false;
        }

        Item shirt = player.inventory.itemsBody.get(0);
        Item costume = player.inventory.itemsBody.size() > 5
                ? player.inventory.itemsBody.get(5) : null;
        return (shirt == null || !shirt.isNotNullItem())
                && (costume == null || !costume.isNotNullItem());
    }

    public static int getMaterialDropRate(Player player) {
        boolean summerDropBoost = player != null && player.itemTime != null
                && (player.itemTime.isUseTraiDua || player.itemTime.isUseCoBonLa);
        return getMaterialDropRate(summerDropBoost);
    }

    public static int getMaterialDropRate(boolean summerDropBoost) {
        if (!summerDropBoost) {
            return MATERIAL_BASE_DROP_RATE;
        }
        return MATERIAL_BASE_DROP_RATE * (100 + SUMMER_DROP_BONUS_PERCENT) / 100;
    }

    public static boolean isMapWithWater(Map map) {
        if (map == null) {
            return false;
        }
        return WATER_MAP_CACHE.computeIfAbsent(map.mapId,
                ignored -> containsWaterTile(Byte.toUnsignedInt(map.tileId), map.tileMap));
    }

    static boolean containsWaterTile(int tileId, int[][] tileMap) {
        if (tileMap == null || tileId <= 0) {
            return false;
        }
        int tileSetIndex = tileId - 1;
        int[] waterFlowTiles = getTileIndexes(WATER_FLOW_TILES, tileSetIndex);
        int[] waterTiles = getTileIndexes(WATER_TILES, tileSetIndex);
        int[] underwaterTiles = getTileIndexes(UNDERWATER_TILES, tileSetIndex);
        for (int[] row : tileMap) {
            if (row == null) {
                continue;
            }
            for (int tile : row) {
                if (contains(waterFlowTiles, tile)
                        || contains(waterTiles, tile)
                        || contains(underwaterTiles, tile)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int[] getTileIndexes(int[][] tileIndexes, int index) {
        return tileIndexes != null && index >= 0 && index < tileIndexes.length
                ? tileIndexes[index] : null;
    }

    private static boolean contains(int[] values, int value) {
        if (values == null) {
            return false;
        }
        for (int candidate : values) {
            if (candidate == value) {
                return true;
            }
        }
        return false;
    }

    private static int countItems(List<Item> items, int itemId) {
        if (items == null) {
            return 0;
        }
        int quantity = 0;
        for (Item item : items) {
            if (item != null && item.isNotNullItem() && item.template.id == itemId) {
                quantity += Math.max(0, item.quantity);
            }
        }
        return quantity;
    }

    private static int countPendingMapItems(Player player, int itemId) {
        if (player.zone == null || player.zone.items == null) {
            return 0;
        }
        int quantity = 0;
        for (ItemMap itemMap : new ArrayList<>(player.zone.items)) {
            if (itemMap != null && itemMap.itemTemplate != null
                    && itemMap.playerId == Math.abs(player.id)
                    && itemMap.itemTemplate.id == itemId) {
                quantity += Math.max(0, itemMap.quantity);
            }
        }
        return quantity;
    }
}
