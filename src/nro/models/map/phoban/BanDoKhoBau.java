package nro.models.map.phoban;

import nro.models.utils.Functions;
import nro.models.boss.Boss;
import nro.models.boss.ban_do_kho_bau.TrungUyXanhLo;
import nro.models.clan.Clan;
import nro.models.map.TrapMap;
import nro.models.map.Zone;
import nro.models.mob.Mob;
import nro.models.player.Player;
import nro.models.services.ItemTimeService;
import nro.models.map.service.MapService;
import nro.models.services.Service;
import nro.models.map.service.ChangeMapService;
import nro.models.utils.Util;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import lombok.Data;
import nro.models.server.Maintenance;
import nro.models.map.service.ItemMapService;
import nro.models.utils.TimeUtil;

@Data
public class BanDoKhoBau implements Runnable {

    public static final long POWER_CAN_GO_TO_DBKB = 2000000000;
    public static final int AVAILABLE = 50;
    public static final int TIME_BAN_DO_KHO_BAU = 1800000;
    public int id;
    public byte level;
    public final List<Zone> zones;
    public Clan clan;
    public boolean isOpened;
    private long lastTimeOpen;
    private boolean kickoutbdkb;
    private long timeKickOutBDKB;
    private Boss boss;
    private long lastTimeSendNotify;
    private boolean allCharactersDead;

    public void addZone(Zone zone) {
        this.zones.add(zone);
    }

    public BanDoKhoBau(int id) {
        this.id = id;
        this.zones = new ArrayList<>();
    }

    @Override
    public void run() {
        while (!Maintenance.isRunning && isOpened) {
            try {
                long startTime = System.currentTimeMillis();
                update();
                Functions.sleep(Math.max(150 - (System.currentTimeMillis() - startTime), 10));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void update() {
        if (isOpened) {
            if (Util.canDoWithTime(lastTimeOpen, TIME_BAN_DO_KHO_BAU) || (kickoutbdkb && Util.canDoWithTime(timeKickOutBDKB, 60000))) {
                finish();
                dispose();
                return;
            }

            allCharactersDead = true;
            for (Zone zone : zones) {

                if (zone.map.mapId == 135) {
                    for (Player pl : zone.getNotBosses()) {
                        if (pl != null) {
                            TrapMap trap = zone.isInTrap(pl);
                            if (trap != null) {
                                trap.doPlayer(pl);
                            }
                        }
                    }
                }

                for (Mob mob : zone.mobs) {
                    if (!mob.isDie()) {
                        allCharactersDead = false;
                        break;
                    }
                }

                if (allCharactersDead) {
                    for (Player cBoss : zone.getBosses()) {
                        if (!cBoss.isDie()) {
                            allCharactersDead = false;
                            break;
                        }
                    }
                }
            }

            if (!kickoutbdkb && (allCharactersDead || Util.canDoWithTime(lastTimeOpen, TIME_BAN_DO_KHO_BAU - 60000))) {
                kickoutbdkb = true;
                timeKickOutBDKB = System.currentTimeMillis();
            }

            if (kickoutbdkb && Util.canDoWithTime(lastTimeSendNotify, 10000)) {
                for (Zone zone : zones) {
                    List<Player> players = zone.getPlayers();
                    for (Player pl : players) {
                        Service.gI().sendThongBao(pl, "Cái hang này sắp sập rồi, chúng ta phải rời khỏi đây ngay " + TimeUtil.getTimeLeft(timeKickOutBDKB, 60) + " nữa");
                    }
                    lastTimeSendNotify = System.currentTimeMillis();
                }
            }

        }
    }

    public void openBanDoKhoBau(Player plOpen, Clan clan, byte level) {
        try {
            this.level = level;
            this.lastTimeOpen = System.currentTimeMillis();
            this.clan = clan;
            this.clan.lastTimeOpenBanDoKhoBau = this.lastTimeOpen;
            this.clan.playerOpenBanDoKhoBau = plOpen;
            this.clan.BanDoKhoBau = this;
            this.kickoutbdkb = false;
            this.isOpened = true;
            this.allCharactersDead = false;
            this.init();
            ChangeMapService.gI().goToDBKB(plOpen);
            sendTextBanDoKhoBau();
        } catch (Exception e) {
            e.printStackTrace();
            plOpen.clan.lastTimeOpenBanDoKhoBau = 0;
            this.dispose();
        }
    }

    public void sendThanhTichBanDoKhoBau(Player pl) {
        if (pl == null || pl.clan == null || pl.clan.BanDoKhoBau != this) {
            return;
        }

        long timeDone = System.currentTimeMillis() - pl.clan.timeOpenBanDoKhoBau;
        int levelDone = pl.clan.BanDoKhoBau.level;
        if (levelDone > pl.clan.levelDoneBanDoKhoBau) {
            pl.clan.levelDoneBanDoKhoBau = levelDone;
            pl.clan.thoiGianHoanThanhBDKB = timeDone;
        } else if (levelDone == pl.clan.levelDoneBanDoKhoBau
                && timeDone < pl.clan.thoiGianHoanThanhBDKB) {
            pl.clan.thoiGianHoanThanhBDKB = timeDone;
        }

        pl.clan.updatethanhTichBDKBForLeader();
        pl.clan.updateThongTinLeader(pl.clan.id);
    }

    private void init() {
        //Hồi sinh quái
        for (Zone zone : this.zones) {
            for (TrapMap trap : zone.trapMaps) {
                trap.dame = this.level * 100000;
            }

            if (zone.map.mapId == 135 || zone.map.mapId == 136 || zone.map.mapId == 137) {
                List<Mob> mobs = zone.mobs;
                for (int i = 0; i < mobs.size(); i++) {
                    Mob mob = mobs.get(i);
                    boolean superMob = ((i == 5 || i == 10) && zone.map.mapId == 135)
                            || (i == 5 && zone.map.mapId == 136)
                            || (i == 5 && zone.map.mapId == 137);
                    mob.level = level;
                    if (superMob) {
                        mob.lvMob = 1;
                        mob.point.dame = (int) Math.min((long) level * 600 * mob.tempId * 10, 2_147_483_647);
                        mob.point.maxHp = TreasureMapPolicy.mobMaxHp(level, true);
                        mob.hoiSinh();
                        mob.hoiSinhMobPhoBan();
                    } else {
                        mob.lvMob = 0;
                        mob.point.dame = (int) Math.min((long) level * 200 * mob.tempId, 2_147_483_647);
                        mob.point.maxHp = TreasureMapPolicy.mobMaxHp(level, false);
                        mob.hoiSinh();
                        mob.hoiSinhMobPhoBan();
                    }
                }
            } else {
                for (Mob mob : zone.mobs) {
                    mob.level = level;
                    mob.lvMob = 0;
                    mob.point.dame = (int) Math.min((long) level * 31 * 50 * mob.tempId, 2_147_483_647);
                    mob.point.maxHp = TreasureMapPolicy.mobMaxHp(level, false);
                    mob.hoiSinh();
                    mob.hoiSinhMobPhoBan();
                }
            }

            if (zone.map.mapId == 137) {
                try {
                    long bossDamage = (200000 * level);
                    long bossMaxHealth = (20000000 * level);
                    bossDamage = Math.min(bossDamage, 200000000L);
                    bossMaxHealth = Math.min(bossMaxHealth, 2000000000L);
                    boss = new TrungUyXanhLo(
                            zone,
                            level,
                            (int) bossDamage,
                            (int) bossMaxHealth
                    );
                } catch (Exception exception) {
                }
            }
        }
        Executors.newSingleThreadExecutor().submit(this, "Bản Đồ Kho Báu: " + this.clan.name);
    }

    public void finish() {
        for (Zone zone : zones) {
            List<Player> playersSnapshot = new ArrayList<>(zone.getPlayers());
            for (Player pl : playersSnapshot) {
                try {
                    if (pl != null && pl.clan != null && pl.clan.BanDoKhoBau == this) {
                        sendThanhTichBanDoKhoBau(pl);
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                try {
                    kickOutOfBDKB(pl);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        }
    }

    private void kickOutOfBDKB(Player player) {
        if (player == null || player.zone == null || player.zone.map == null) {
            return;
        }
        if (player.mapBeforeCapsule != null && player.mapBeforeCapsule.map != null
                && MapService.gI().isMapBanDoKhoBau(player.mapBeforeCapsule.map.mapId)) {
            player.mapBeforeCapsule = null;
        }
        if (MapService.gI().isMapBanDoKhoBau(player.zone.map.mapId)) {
            ChangeMapService.gI().changeMapBySpaceShip(player, 5, -1, 1038);
        }
    }

    public Zone getMapById(int mapId) {
        for (Zone zone : this.zones) {
            if (zone.map.mapId == mapId) {
                return zone;
            }
        }
        return null;
    }

    private void sendTextBanDoKhoBau() {
        for (Player pl : this.clan.membersInGame) {
            ItemTimeService.gI().sendTextBanDoKhoBau(pl);
        }
    }

    private void removeTextBanDoKhoBau() {
        if (this.clan == null) {
            return;
        }
        for (Player pl : this.clan.membersInGame) {
            ItemTimeService.gI().removeTextBanDoKhoBau(pl);
        }
    }

    public void dispose() {
        this.isOpened = false;
        Clan dungeonClan = this.clan;
        try {
            if (boss != null) {
                this.boss.leaveMap();
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        for (Zone zone : zones) {
            for (int i = zone.items.size() - 1; i >= 0; i--) {
                if (i < zone.items.size()) {
                    try {
                        ItemMapService.gI().removeItemMap(zone.items.get(i));
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
            }
        }
        try {
            this.removeTextBanDoKhoBau();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        this.allCharactersDead = false;
        this.boss = null;
        if (dungeonClan != null && dungeonClan.BanDoKhoBau == this) {
            dungeonClan.BanDoKhoBau = null;
        }
        this.clan = null;
        this.kickoutbdkb = false;
    }
}
