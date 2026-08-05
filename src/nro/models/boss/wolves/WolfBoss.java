package nro.models.boss.wolves;

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
import nro.models.server.GameConfigService;
import nro.models.server.ServerNotify;
import nro.models.services.Service;
import nro.models.services.TaskService;
import nro.models.utils.Util;

public abstract class WolfBoss extends Boss {

    /** Legacy boss rule: -1 means every successful attack removes exactly 1 HP. */
    public static final long DAMAGE_LIMIT = -1L;
    static final int BLUE_GRAY_MAX_HP = 10_000;

    private static final long STANDBY_CHAT_INTERVAL_MS = 7_000L;
    private static final int[] SPAWN_MAP_IDS = {
        ConstMap.LANG_ARU, ConstMap.DOI_HOA_CUC,
        ConstMap.THUNG_LUNG_TRE, ConstMap.RUNG_NAM, ConstMap.RUNG_XUONG,
        ConstMap.LANG_MORI, ConstMap.DOI_NAM_TIM,
        ConstMap.THI_TRAN_MOORI, ConstMap.THUNG_LUNG_NAMEC, ConstMap.THUNG_LUNG_MAIMA,
        ConstMap.LANG_KAKAROT, ConstMap.DOI_HOANG,
        ConstMap.LANG_PLANT, ConstMap.RUNG_NGUYEN_SINH, ConstMap.RUNG_THONG_XAYDA
    };

    private final int memberIndex;
    private final WolfEncounterState encounterState;
    private boolean spawnRequested;
    private boolean combatActive;
    private boolean blueGrayCharging;
    private long lastStandbyChat;

    protected WolfBoss(int id, BossData data, int memberIndex) throws Exception {
        super(id, memberIndex != 0, false, data);
        this.memberIndex = memberIndex;
        this.encounterState = memberIndex == 0
                ? new WolfEncounterState(System.currentTimeMillis()) : null;
    }

    protected abstract String activationText();

    protected abstract String standbyText();

    @Override
    protected void resetBase() {
        super.resetBase();
        int criticalPercent = ThreadLocalRandom.current().nextInt(101);
        nPoint.critg = criticalPercent;
        nPoint.crit = criticalPercent;
        combatActive = false;
        blueGrayCharging = false;
        lastStandbyChat = 0L;
    }

    @Override
    public void update() {
        if (memberIndex == 0) {
            WolfEncounterState.Action action = encounterState.poll(System.currentTimeMillis());
            if (action != WolfEncounterState.Action.NONE) {
                prepareNextRound();
            }
        }
        super.update();
    }

    @Override
    public void rest() {
        if (memberIndex == 0 && spawnRequested) {
            changeStatus(BossStatus.RESPAWN);
        }
    }

    @Override
    public void joinMap() {
        if (memberIndex != 0) {
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
        resetMembersForRound();
        encounterState.startRound(
                System.currentTimeMillis(),
                ThreadLocalRandom.current().nextInt(WolfEncounterState.MEMBER_COUNT),
                ThreadLocalRandom.current().nextInt(2));
        spawnRequested = false;
        notifyJoinMap();
        changeStatus(BossStatus.CHAT_S);
        wakeupAnotherBossWhenAppear();
    }

    @Override
    protected void notifyJoinMap() {
        if (memberIndex == 0) {
            ServerNotify.gI().notify("BOSS Ba Con Sói Vô Tình vừa xuất hiện!");
        }
    }

    @Override
    public void doneChatS() {
        syncCombatState();
        if (combatActive) {
            changeToTypePK();
            changeStatus(BossStatus.ACTIVE);
        } else {
            changeToTypeNonPK();
            changeStatus(BossStatus.AFK);
        }
    }

    @Override
    public void afk() {
        syncCombatState();
        if (combatActive) {
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
    public synchronized int injured(
            Player attacker, long damage, boolean piercing, boolean isMobAttack) {
        if (!combatActive || damage <= 0) {
            return 0;
        }
        if (memberIndex == WolfEncounterState.BLUE_GRAY_MEMBER && blueGrayCharging) {
            nPoint.hp = Math.min(BLUE_GRAY_MAX_HP,
                    nPoint.hp + (int) limitedDamage(damage));
            if (nPoint.hp >= BLUE_GRAY_MAX_HP) {
                blueGrayCharging = false;
            }
            return 0;
        }
        return super.injured(attacker, limitedDamage(damage), piercing, isMobAttack);
    }

    static long limitedDamage(long damage) {
        if (damage <= 0) {
            return 0L;
        }
        return DAMAGE_LIMIT == -1L ? 1L : Math.min(damage, DAMAGE_LIMIT);
    }

    @Override
    public void die(Player killer) {
        super.die(killer);
        WolfBoss leader = leader();
        if (leader == null || leader.encounterState == null) {
            return;
        }

        WolfEncounterState.DefeatResult result
                = leader.encounterState.defeat(memberIndex, System.currentTimeMillis());
        if (!result.accepted()) {
            return;
        }

        dropDragonBall(killer);
        if (result.dropDivine()) {
            GameConfigService.gI().dropGuaranteedDivine(
                    this, killer, "Ba Con Sói Vô Tình");
        }
        if (!result.allDefeated()) {
            leader.activateMember(result.nextActiveMember());
        }
    }

    @Override
    public void reward(Player killer) {
        if (killer != null) {
            TaskService.gI().checkDoneTaskKillBoss(killer, this);
        }
    }

    private void prepareNextRound() {
        for (int index = 0; index < WolfEncounterState.MEMBER_COUNT; index++) {
            WolfBoss member = member(index);
            if (member != null) {
                forceRest(member);
            }
        }
        spawnRequested = true;
        changeStatus(BossStatus.RESPAWN);
    }

    private void forceRest(WolfBoss boss) {
        if (boss.zone != null) {
            ChangeMapService.gI().exitMap(boss);
        }
        boss.zone = null;
        boss.lastZone = null;
        boss.playerTarger = null;
        boss.typePk = ConstPlayer.NON_PK;
        boss.combatActive = false;
        boss.blueGrayCharging = false;
        if (boss.effectSkill != null) {
            boss.effectSkill.removeSkillEffectWhenDie();
        }
        boss.changeStatus(BossStatus.REST);
    }

    private void resetMembersForRound() {
        for (int index = 0; index < WolfEncounterState.MEMBER_COUNT; index++) {
            WolfBoss member = member(index);
            if (member != null) {
                member.combatActive = false;
                member.blueGrayCharging = false;
                member.lastStandbyChat = 0L;
            }
        }
    }

    private void syncCombatState() {
        WolfBoss leader = leader();
        combatActive = leader != null && leader.encounterState != null
                && leader.encounterState.getActiveMember() == memberIndex;
    }

    private void activateMember(int index) {
        WolfBoss member = member(index);
        if (member == null || member.isDie()) {
            return;
        }
        member.combatActive = true;
        if (index == WolfEncounterState.BLUE_GRAY_MEMBER) {
            member.prepareBlueGrayCharge();
        }
        if (member.zone != null
                && (member.bossStatus == BossStatus.AFK
                || member.bossStatus == BossStatus.CHAT_S
                || member.bossStatus == BossStatus.ACTIVE)) {
            member.changeToTypePK();
            Service.gI().chat(member, member.activationText());
            member.changeStatus(BossStatus.ACTIVE);
        }
    }

    private void prepareBlueGrayCharge() {
        nPoint.hpg = BLUE_GRAY_MAX_HP;
        nPoint.hpMax = BLUE_GRAY_MAX_HP;
        nPoint.hp = Math.min(nPoint.hp, 1_000);
        blueGrayCharging = true;
        Service.gI().reload_HP_NV(this);
    }

    private WolfBoss leader() {
        return memberIndex == 0
                ? this
                : parentBoss instanceof WolfBoss wolfLeader ? wolfLeader : null;
    }

    private WolfBoss member(int index) {
        WolfBoss leader = leader();
        if (leader == null || index < 0 || index >= WolfEncounterState.MEMBER_COUNT) {
            return null;
        }
        if (index == 0) {
            return leader;
        }
        if (leader.bossAppearTogether == null
                || leader.bossAppearTogether.length == 0
                || leader.bossAppearTogether[0] == null
                || leader.bossAppearTogether[0].length < index) {
            return null;
        }
        return leader.bossAppearTogether[0][index - 1] instanceof WolfBoss wolf
                ? wolf : null;
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
        int itemId = WolfEncounterState.dragonBallItemForRoll(
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
