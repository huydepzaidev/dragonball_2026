package nro.models.server;

import java.util.Set;
import nro.models.boss.Android.Android19;
import nro.models.boss.Android.DrKore;
import nro.models.boss.Android.KingKong;
import nro.models.boss.Android.Pic;
import nro.models.boss.Android.Poc;
import nro.models.boss.Baby.Baby;
import nro.models.boss.Boss;
import nro.models.boss.BossID;
import nro.models.boss.Cold.Cooler;
import nro.models.boss.cumber.Cumber;
import nro.models.map.ItemMap;
import nro.models.map.Zone;
import nro.models.player.Player;

/**
 * Integration test for the complete configured divine-drop path. It creates an
 * isolated Zone with no connected clients, so no test item reaches the live
 * game world or a real player.
 */
public final class ConfiguredBossDropIntegrationTest {

    private static final Set<Integer> DIVINE_ITEM_IDS = Set.of(
            555, 556, 557, 558, 559, 560, 561,
            562, 563, 564, 565, 566, 567);

    private ConfiguredBossDropIntegrationTest() {
    }

    public static void main(String[] args) throws Exception {
        Manager.gI();
        GameConfigService service = GameConfigService.gI();
        if (!service.loadNow()) {
            throw new AssertionError("Cannot load configured boss drops");
        }

        Zone testZone = new Zone(Manager.MAPS.get(0), 9999, 1);
        Player owner = new Player();
        owner.id = 987654321L;
        owner.name = "configured-drop-test";
        owner.isPlayer = true;
        owner.zone = testZone;
        owner.location.x = 100;
        owner.location.y = 100;

        if (!GameConfigService.isEncounterManagedDivineBoss(BossID.XEN_BO_HUNG)
                || !GameConfigService.isEncounterManagedDivineBoss(BossID.SIEU_BO_HUNG)
                || !GameConfigService.isEncounterManagedDivineBoss(BossID.GOD_BILL)
                || !GameConfigService.isEncounterManagedDivineBoss(BossID.ANGEL_WHIS)
                || !GameConfigService.isEncounterManagedDivineBoss(BossID.GOD_CHAMPA)
                || !GameConfigService.isEncounterManagedDivineBoss(BossID.ANGEL_VADOS)
                || !GameConfigService.isEncounterManagedDivineBoss(BossID.PILAP)
                || !GameConfigService.isEncounterManagedDivineBoss(BossID.MAI_PILAP)
                || !GameConfigService.isEncounterManagedDivineBoss(BossID.PU_PILAP)
                || !GameConfigService.isEncounterManagedDivineBoss(BossID.SOI_DO_VO_TINH)
                || !GameConfigService.isEncounterManagedDivineBoss(BossID.SOI_VANG_VO_TINH)
                || !GameConfigService.isEncounterManagedDivineBoss(BossID.SOI_XANH_XAM_VO_TINH)
                || !GameConfigService.isEncounterManagedDivineBoss(BossID.ZAMASU)) {
            throw new AssertionError("Pair encounters must use their guaranteed divine-drop state");
        }
        assertEventuallyDrops(service, new Cumber(), owner, testZone);
        assertEventuallyDrops(service, new Cooler(), owner, testZone);
        assertEventuallyDrops(service, new Baby(), owner, testZone);
        assertEventuallyDrops(service, new Pic(), owner, testZone);
        assertEventuallyDrops(service, new Poc(), owner, testZone);
        assertEventuallyDrops(service, new KingKong(), owner, testZone);
        assertEventuallyDrops(service, new Android19(), owner, testZone);
        assertEventuallyDrops(service, new DrKore(), owner, testZone);

        System.out.println("CONFIGURED_BOSS_DROP_INTEGRATION_OK configured=8 encounterManaged=13 chance=3%");
        System.exit(0);
    }

    private static void assertEventuallyDrops(GameConfigService service,
            Boss boss, Player owner, Zone zone) {
        if (service.getConfiguredDropRuleCount((int) boss.id) != 1) {
            throw new AssertionError("Boss " + boss.id + " must have exactly one configured rule");
        }
        boss.zone = zone;
        boss.location.x = 100;
        boss.location.y = 100;

        int before = zone.items.size();
        for (int attempt = 0; attempt < 2000 && zone.items.size() == before; attempt++) {
            service.dropConfiguredRewards(boss, owner);
        }
        if (zone.items.size() != before + 1) {
            throw new AssertionError("Boss " + boss.id + " did not create one divine item at 3%");
        }

        ItemMap dropped = zone.items.get(zone.items.size() - 1);
        if (dropped.itemTemplate == null || !DIVINE_ITEM_IDS.contains((int) dropped.itemTemplate.id)) {
            throw new AssertionError("Boss " + boss.id + " created a non-divine item");
        }
        if (dropped.quantity != 1 || dropped.playerId != owner.id) {
            throw new AssertionError("Boss " + boss.id + " created an invalid quantity/owner");
        }
        boolean hasBossRarity = dropped.options.stream()
                .anyMatch(option -> option.optionTemplate.id == 207);
        boolean isTradeLocked = dropped.options.stream()
                .anyMatch(option -> option.optionTemplate.id == 30);
        if (!hasBossRarity || isTradeLocked) {
            throw new AssertionError("Divine boss item must show option 207 and remain tradeable");
        }
    }
}
