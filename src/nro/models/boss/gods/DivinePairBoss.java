package nro.models.boss.gods;

import java.util.concurrent.ThreadLocalRandom;
import nro.models.boss.Boss;
import nro.models.boss.BossData;
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
import nro.models.server.ServerNotify;
import nro.models.services.Service;
import nro.models.services.TaskService;
import nro.models.utils.Util;

public abstract class DivinePairBoss extends Boss {

    private static final int RANDOM_CRITICAL_PERCENT = 10;
    private static final long STANDBY_CHAT_INTERVAL_MS = 7_000L;

    private final boolean encounterLeader;
    private final DivinePairEncounterState encounterState;
    private boolean spawnRequested;
    private boolean partnerActivated;
    private long lastStandbyChat;

    protected DivinePairBoss(int id, BossData data, boolean encounterLeader) throws Exception {
        super(id, !encounterLeader, false, data);
        this.encounterLeader = encounterLeader;
        this.encounterState = encounterLeader
                ? new DivinePairEncounterState(System.currentTimeMillis()) : null;
    }

    protected abstract String encounterName();
    protected abstract String partnerActivationText();
    protected abstract String standbyText();

    @Override
    protected void resetBase() {
        super.resetBase();
        nPoint.critg = RANDOM_CRITICAL_PERCENT;
        nPoint.crit = RANDOM_CRITICAL_PERCENT;
        partnerActivated = false;
        lastStandbyChat = 0L;
    }

    @Override
    public void update() {
        if (encounterLeader) {
            DivinePairEncounterState.Action action = encounterState.poll(System.currentTimeMillis());
            if (action != DivinePairEncounterState.Action.NONE) prepareNextRound();
        }
        super.update();
    }

    @Override
    public void rest() {
        if (encounterLeader && spawnRequested) changeStatus(BossStatus.RESPAWN);
    }

    @Override
    public void joinMap() {
        if (!encounterLeader) {
            super.joinMap();
            return;
        }
        Zone emptyZone = findEmptyBossZone();
        if (emptyZone == null) {
            zone = null;
            lastZone = null;
            changeStatus(BossStatus.REST);
            return;
        }
        joinMapByZone(emptyZone);
        Service.gI().sendFlagBag(this);
        resetPartnerForRound();
        encounterState.startRound(System.currentTimeMillis(),
                ThreadLocalRandom.current().nextBoolean()
                        ? DivinePairEncounterState.Member.LEADER
                        : DivinePairEncounterState.Member.PARTNER);
        spawnRequested = false;
        notifyJoinMap();
        changeStatus(BossStatus.CHAT_S);
        wakeupAnotherBossWhenAppear();
    }

    @Override
    protected void notifyJoinMap() {
        if (encounterLeader && zone != null) {
            ServerNotify.gI().notifyBoss("BOSS " + encounterName()
                    + " vừa xuất hiện tại " + zone.map.mapName, 0, encounterName(), zone.map.mapName, "", false);
        }
    }

    @Override
    public void doneChatS() {
        if (!encounterLeader) {
            if (partnerActivated) {
                changeToTypePK();
                changeStatus(BossStatus.ACTIVE);
            } else {
                changeToTypeNonPK();
                changeStatus(BossStatus.AFK);
            }
        }
    }

    @Override
    public void afk() {
        if (partnerActivated) {
            changeToTypePK();
            changeStatus(BossStatus.ACTIVE);
            return;
        }
        if (zone != null && Util.canDoWithTime(lastStandbyChat, STANDBY_CHAT_INTERVAL_MS)) {
            Service.gI().chat(this, standbyText());
            lastStandbyChat = System.currentTimeMillis();
        }
    }

    @Override
    public synchronized int injured(Player attacker, long damage, boolean piercing, boolean isMobAttack) {
        if (!encounterLeader && !partnerActivated) return 0;
        return super.injured(attacker, damage, piercing, isMobAttack);
    }

    @Override
    public void die(Player killer) {
        super.die(killer);
        DivinePairBoss leader = encounterLeader ? this : encounterLeader();
        if (leader == null || leader.encounterState == null) return;
        DivinePairEncounterState.Member member = encounterLeader
                ? DivinePairEncounterState.Member.LEADER
                : DivinePairEncounterState.Member.PARTNER;
        DivinePairEncounterState.DefeatResult result = leader.encounterState.defeat(
                member, System.currentTimeMillis());
        if (!result.accepted()) return;
        dropDragonBall(killer);
        if (encounterLeader && !result.bothDefeated()) activatePartner();
    }

    @Override
    public void reward(Player killer) {
        if (killer != null) TaskService.gI().checkDoneTaskKillBoss(killer, this);
    }

    private void prepareNextRound() {
        forceRest(this);
        DivinePairBoss partner = partner();
        if (partner != null) forceRest(partner);
        spawnRequested = true;
        changeStatus(BossStatus.RESPAWN);
    }

    private void forceRest(DivinePairBoss boss) {
        if (boss.zone != null) ChangeMapService.gI().exitMap(boss);
        boss.zone = null;
        boss.lastZone = null;
        boss.playerTarger = null;
        boss.typePk = ConstPlayer.NON_PK;
        boss.partnerActivated = false;
        boss.effectSkill.removeSkillEffectWhenDie();
        boss.changeStatus(BossStatus.REST);
    }

    private void resetPartnerForRound() {
        DivinePairBoss partner = partner();
        if (partner != null) {
            partner.partnerActivated = false;
            partner.lastStandbyChat = 0L;
        }
    }

    private void activatePartner() {
        DivinePairBoss partner = partner();
        if (partner == null || partner.isDie()) return;
        partner.partnerActivated = true;
        if (partner.zone != null) {
            partner.changeToTypePK();
            Service.gI().chat(partner, partnerActivationText());
            partner.changeStatus(BossStatus.ACTIVE);
        }
    }

    private DivinePairBoss encounterLeader() {
        return parentBoss instanceof DivinePairBoss leader ? leader : null;
    }

    private DivinePairBoss partner() {
        if (!encounterLeader || bossAppearTogether == null || bossAppearTogether.length == 0
                || bossAppearTogether[0] == null || bossAppearTogether[0].length == 0) return null;
        return bossAppearTogether[0][0] instanceof DivinePairBoss pairBoss ? pairBoss : null;
    }

    private Zone findEmptyBossZone() {
        int[] mapIds = {ConstMap.HANH_TINH_BILL, ConstMap.HANH_TINH_NGUC_TU};
        int mapOffset = ThreadLocalRandom.current().nextInt(mapIds.length);
        for (int i = 0; i < mapIds.length; i++) {
            Map map = MapService.gI().getMapById(mapIds[(i + mapOffset) % mapIds.length]);
            if (map == null || map.zones == null || map.zones.isEmpty()) continue;
            int zoneOffset = ThreadLocalRandom.current().nextInt(map.zones.size());
            for (int j = 0; j < map.zones.size(); j++) {
                Zone candidate = map.zones.get((j + zoneOffset) % map.zones.size());
                if (candidate != null && candidate.getBosses().isEmpty()) return candidate;
            }
        }
        return null;
    }

    private void dropDragonBall(Player killer) {
        Player owner = resolveRewardOwner(killer);
        if (owner == null || zone == null) return;
        int itemId = DivinePairEncounterState.dragonBallItemForRoll(
                ThreadLocalRandom.current().nextInt(100));
        int x = location == null ? 0 : location.x;
        int rawY = location == null ? 0 : location.y - 24;
        int y = zone.map == null ? rawY : zone.map.yPhysicInTop(x, rawY);
        ItemMap item = new ItemMap(zone, itemId, 1, x, y, owner.id);
        Service.gI().dropItemMap(zone, item);
    }

    private static Player resolveRewardOwner(Player attacker) {
        if (attacker == null || attacker.isBot) return null;
        if (attacker instanceof Pet pet) {
            return pet.master != null && !pet.master.isBot ? pet.master : null;
        }
        if (attacker instanceof NewPet pet) {
            return pet.master != null && !pet.master.isBot ? pet.master : null;
        }
        return attacker.isPl() ? attacker : null;
    }
}
