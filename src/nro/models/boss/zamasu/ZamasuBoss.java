package nro.models.boss.zamasu;

import java.util.concurrent.ThreadLocalRandom;
import nro.models.boss.Boss;
import nro.models.boss.BossID;
import nro.models.boss.BossesData;
import nro.models.consts.BossStatus;
import nro.models.consts.ConstMap;
import nro.models.consts.ConstPlayer;
import nro.models.map.ItemMap;
import nro.models.map.Map;
import nro.models.map.Zone;
import nro.models.map.service.ChangeMapService;
import nro.models.map.service.MapService;
import nro.models.player.NewPet;
import nro.models.player.Pet;
import nro.models.player.Player;
import nro.models.server.GameConfigService;
import nro.models.server.ServerNotify;
import nro.models.services.Service;
import nro.models.services.TaskService;

public final class ZamasuBoss extends Boss {

    private static final int[] SPAWN_MAP_IDS = {
        ConstMap.THANH_PHO_PHIA_DONG,
        ConstMap.THANH_PHO_PHIA_NAM,
        ConstMap.DAO_BALE,
        ConstMap.CAO_NGUYEN,
        ConstMap.THANH_PHO_PHIA_BAC,
        ConstMap.NGON_NUI_PHIA_BAC,
        ConstMap.THUNG_LUNG_PHIA_BAC,
        ConstMap.THI_TRAN_GINDER
    };

    private final ZamasuEncounterState encounterState;
    private boolean spawnRequested;

    public ZamasuBoss() throws Exception {
        super(BossID.ZAMASU, false, false, BossesData.ZAMASU);
        encounterState = new ZamasuEncounterState(
                System.currentTimeMillis(), ThreadLocalRandom.current().nextBoolean());
    }

    @Override
    protected void resetBase() {
        super.resetBase();
        int criticalPercent = ThreadLocalRandom.current().nextInt(101);
        nPoint.critg = criticalPercent;
        nPoint.crit = criticalPercent;
    }

    @Override
    public void update() {
        ZamasuEncounterState.Action action = encounterState.poll(System.currentTimeMillis());
        if (action != ZamasuEncounterState.Action.NONE) {
            prepareNextRound();
        }
        super.update();
    }

    @Override
    public void rest() {
        if (spawnRequested) {
            changeStatus(BossStatus.RESPAWN);
        }
    }

    @Override
    public void joinMap() {
        Zone emptyZone = findEmptyBossZone();
        if (emptyZone == null) {
            zone = null;
            lastZone = null;
            changeStatus(BossStatus.REST);
            return;
        }
        joinMapByZone(emptyZone);
        Service.gI().sendFlagBag(this);
        encounterState.startRound(System.currentTimeMillis());
        spawnRequested = false;
        notifyJoinMap();
        changeStatus(BossStatus.CHAT_S);
    }

    @Override
    protected void notifyJoinMap() {
        ServerNotify.gI().notifyBoss("BOSS Zamasu vừa xuất hiện tại " + this.zone.map.mapName, 0, "Zamasu", this.zone.map.mapName, "", false);
    }

    @Override
    public void die(Player killer) {
        super.die(killer);
        ZamasuEncounterState.DefeatResult result
                = encounterState.defeat(System.currentTimeMillis());
        if (!result.accepted()) {
            return;
        }
        dropDragonBall(killer);
        if (result.dropDivine()) {
            GameConfigService.gI().dropGuaranteedDivine(this, killer, "Zamasu");
        }
    }

    @Override
    public void reward(Player killer) {
        if (killer != null) {
            TaskService.gI().checkDoneTaskKillBoss(killer, this);
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
        spawnRequested = true;
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

    private void dropDragonBall(Player killer) {
        Player owner = resolveRewardOwner(killer);
        if (owner == null || zone == null) {
            return;
        }
        int itemId = ZamasuEncounterState.dragonBallItemForRoll(
                ThreadLocalRandom.current().nextInt(100));
        int x = location == null ? 0 : location.x;
        int rawY = location == null ? 0 : location.y - 24;
        int y = zone.map == null ? rawY : zone.map.yPhysicInTop(x, rawY);
        Service.gI().dropItemMap(zone, new ItemMap(zone, itemId, 1, x, y, owner.id));
    }

    private static Player resolveRewardOwner(Player attacker) {
        if (attacker == null || attacker.isBot) {
            return null;
        }
        if (attacker instanceof Pet pet) {
            return pet.master != null && !pet.master.isBot ? pet.master : null;
        }
        if (attacker instanceof NewPet pet) {
            return pet.master != null && !pet.master.isBot ? pet.master : null;
        }
        return attacker.isPl() ? attacker : null;
    }
}