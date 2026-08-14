package nro.models.boss.Baby;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import nro.models.consts.BossStatus;
import nro.models.consts.ConstPlayer;
import nro.models.boss.Boss;
import nro.models.boss.BossesData;
import nro.models.boss.BossID;
import nro.models.item.Item;
import nro.models.map.ItemMap;
import nro.models.map.Map;
import nro.models.map.Zone;
import nro.models.map.service.ChangeMapService;
import nro.models.map.service.MapService;
import nro.models.player.Player;
import nro.models.services.EffectSkillService;
import nro.models.services.ItemService;
import nro.models.services.Service;
import nro.models.services.SkillService;
import nro.models.utils.Util;

public class Baby extends Boss {

    private static final long SPAWN_RETRY_MS = 1_000L;
    private static final int[] SPAWN_MAP_IDS = BabyEncounterState.spawnMapIds();

    private final BabyEncounterState encounterState;
    private boolean spawnRequested;
    private long lastSpawnRetryAt;

    public Baby() throws Exception {
        super(BossID.BABY, BossesData.BABY, BossesData.BABY_2, BossesData.BABY_3);
        encounterState = new BabyEncounterState(System.currentTimeMillis());
    }

    @Override
    public void update() {
        BabyEncounterState.Action action = encounterState.poll(System.currentTimeMillis());
        if (action != BabyEncounterState.Action.NONE) {
            prepareNextRound();
        }
        super.update();
    }

    @Override
    public void rest() {
        if (spawnRequested
                && (lastSpawnRetryAt == 0L
                || Util.canDoWithTime(lastSpawnRetryAt, SPAWN_RETRY_MS))) {
            changeStatus(BossStatus.JOIN_MAP);
        }
    }

    @Override
    public void joinMap() {
        if (!spawnRequested || currentLevel != 0) {
            super.joinMap();
            return;
        }

        Zone emptyZone = findEmptyBossZone();
        if (emptyZone == null) {
            zone = null;
            lastZone = null;
            lastSpawnRetryAt = System.currentTimeMillis();
            changeStatus(BossStatus.REST);
            return;
        }

        joinMapByZone(emptyZone);
        Service.gI().sendFlagBag(this);
        encounterState.startRound();
        spawnRequested = false;
        lastSpawnRetryAt = 0L;
        notifyJoinMap();
        changeStatus(BossStatus.CHAT_S);
        wakeupAnotherBossWhenAppear();
    }

    @Override
    public void die(Player killer) {
        super.die(killer);
        encounterState.defeat(currentLevel, System.currentTimeMillis());
    }

    @Override
    public synchronized void recoverFromRuntimeError() {
        super.recoverFromRuntimeError();
        currentLevel = -1;
        spawnRequested = true;
        lastSpawnRetryAt = 0L;
    }

    @Override
    public void reward(Player plKill) {
        int x = this.location.x; // đâyyyy
        int y = this.zone.map.yPhysicInTop(x, this.location.y - 24);
        int drop = 190; // 100% rơi item ID 190
        int quantity = Util.nextInt(20000, 30000);
        // Tạo itemMap cho item ID 190
        plKill.bossBabyDefeatParticipationCount++;
        if (Util.isTrue(1, 100)) {
            int[] costumes = {1785, 1786, 1788};
            int costumeId = costumes[Util.nextInt(costumes.length)];

            ItemMap itemMap = new ItemMap(this.zone, costumeId, 1, x, y, plKill.id);

            itemMap.options.add(new Item.ItemOption(50, Util.nextInt(30, 40)));
            itemMap.options.add(new Item.ItemOption(77, Util.nextInt(30, 40)));
            itemMap.options.add(new Item.ItemOption(103, Util.nextInt(30, 40)));
            itemMap.options.add(new Item.ItemOption(94, Util.nextInt(10, 20)));
            itemMap.options.add(new Item.ItemOption(5, Util.nextInt(10, 20)));
            itemMap.options.add(new Item.ItemOption(204, Util.nextInt(10, 20)));
            itemMap.options.add(new Item.ItemOption(30, 0));
            itemMap.options.add(new Item.ItemOption(93, Util.nextInt(2, 5)));

            Service.gI().dropItemMap(this.zone, itemMap);
        }
        ItemMap itemMap = new ItemMap(this.zone, drop, quantity, x, y, plKill.id);
        Item item = ItemService.gI().createNewItem((short) drop);
        Service.gI().dropItemMap(zone, itemMap);
        // 30% xác suất để rơi đồ
        if (Util.isTrue(5, 100)) {
            int group = Util.nextInt(1, 100) <= 70 ? 0 : 1;  // 70% chọn Áo Quần Giày (group = 0), 30% chọn Găng Rada (group = 1)

            // Các vật phẩm rơi từ nhóm Áo Quần Giày và Găng Rada
            int[][] drops = {
                {230, 231, 232, 234, 235, 236, 238, 239, 240, 242, 243, 244, 246, 247, 248, 250, 251, 252, 266, 267, 268, 270, 271, 272, 274, 275, 276}, // Áo Quần Giày
                {254, 255, 256, 258, 259, 260, 262, 263, 264, 278, 279, 280} // Găng Rada
            };
            // Chọn vật phẩm ngẫu nhiên từ nhóm đã chọn
            int dropOptional = drops[group][Util.nextInt(0, drops[group].length - 1)];
            // Tạo vật phẩm và thêm chỉ số shop
            ItemMap optionalItemMap = new ItemMap(this.zone, dropOptional, 1, x, y, plKill.id);
            Item optionalItem = ItemService.gI().createNewItem((short) dropOptional);
            List<Item.ItemOption> optionalOps = ItemService.gI().getListOptionItemShop((short) dropOptional);
            optionalOps.forEach(option -> option.param = (int) (option.param * Util.nextInt(100, 115) / 100.0));
            optionalItemMap.options.addAll(optionalOps);
            // Thêm chỉ số sao pha lê (80% từ 1-3 sao, 17% từ 4-5 sao, 3% sao 6)
            int rand = Util.nextInt(1, 100);
            int value = 0;
            if (rand <= 80) {
                value = Util.nextInt(1, 3); // 80% xác suất: sao từ 1 đến 3
            } else if (rand <= 97) {
                value = Util.nextInt(4, 5); // 17% xác suất: sao từ 4 đến 5
            } else {
                value = 6; // 3% xác suất: sao 6
            }
            optionalItemMap.options.add(new Item.ItemOption(107, value));
            // Drop vật phẩm tùy chọn xuống bản đồ
            Service.gI().dropItemMap(zone, optionalItemMap);
        }
        // 80% xác suất rơi ngọc rồng
        if (Util.isTrue(10, 100)) {
            int[] dropItems = {15, 16, 17, 18, 19, 20, 992};
            int dropOptional = dropItems[Util.nextInt(0, dropItems.length - 1)];
            // Tạo và rơi vật phẩm ngọc rồng hoặc item cấp 2
            ItemMap optionalItemMap = new ItemMap(this.zone, dropOptional, Util.nextInt(1, 3), x, y, plKill.id);
            Item optionalItem = ItemService.gI().createNewItem((short) dropOptional);
            Service.gI().dropItemMap(zone, optionalItemMap);
        }
    }

    @Override
    public void active() {
        if (this.typePk == ConstPlayer.NON_PK) {
            this.changeToTypePK();
        }
        this.attack();
    }

    @Override
    public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack
    ) {
        if (!this.isDie()) {
            if (!piercing && Util.isTrue(this.nPoint.tlNeDon, 1000)) {
                this.chat("Xí hụt");
                return 0;
            }

            damage = (long) (damage * 0.7);

            damage = this.nPoint.subDameInjureWithDeff(damage / 2);

            if (!piercing && effectSkill.isShielding) {
                if (damage > nPoint.hpMax) {
                    EffectSkillService.gI().breakShield(this);
                }
                damage = damage / 4;
            }

            this.nPoint.subHP(damage);

            if (isDie()) {
                this.setDie(plAtt);
                die(plAtt);
            }
            return (int) damage;
        } else {
            return 0;
        }
    }

    private void prepareNextRound() {
        if (zone != null) {
            ChangeMapService.gI().exitMap(this);
        }
        zone = null;
        lastZone = null;
        playerTarger = null;
        typePk = ConstPlayer.NON_PK;
        if (effectSkill != null) {
            effectSkill.removeSkillEffectWhenDie();
        }
        currentLevel = -1;
        spawnRequested = true;
        lastSpawnRetryAt = 0L;
        changeStatus(BossStatus.RESPAWN);
    }

    private Zone findEmptyBossZone() {
        int mapOffset = ThreadLocalRandom.current().nextInt(SPAWN_MAP_IDS.length);
        for (int i = 0; i < SPAWN_MAP_IDS.length; i++) {
            Map map = MapService.gI().getMapById(
                    SPAWN_MAP_IDS[(i + mapOffset) % SPAWN_MAP_IDS.length]);
            if (map == null || map.zones == null || map.zones.isEmpty()) {
                continue;
            }
            int zoneOffset = ThreadLocalRandom.current().nextInt(map.zones.size());
            for (int j = 0; j < map.zones.size(); j++) {
                Zone candidate = map.zones.get((j + zoneOffset) % map.zones.size());
                if (candidate != null && candidate.getBosses().isEmpty()) {
                    return candidate;
                }
            }
        }
        return null;
    }

}
