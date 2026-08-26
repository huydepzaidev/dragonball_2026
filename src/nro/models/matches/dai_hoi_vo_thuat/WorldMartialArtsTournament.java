package nro.models.matches.dai_hoi_vo_thuat;

import nro.models.matches.giai_dau.WorldMartialArtsTournamentManager;
import nro.models.utils.Functions;
import nro.models.consts.ConstPlayer;
import nro.models.consts.ConstTournament;
import nro.models.map.Zone;
import nro.models.matches.DHVT;
import nro.models.player.Player;
import nro.models.server.Maintenance;
import nro.models.services.PlayerService;
import nro.models.services.Service;
import nro.models.map.service.ChangeMapService;
import nro.models.utils.Util;

public final class WorldMartialArtsTournament implements Runnable {

    public Player player_1;
    public Player player_2;
    public Zone zone;
    public Player npc;
    public int timeUp;
    public int timeDown;
    public volatile boolean competing;
    public volatile boolean isFinished;
    public boolean isFinal;
    public Player plWin;
    public Player plLose;
    public int typeEnd;

    public WorldMartialArtsTournament(Player player_1, Player player_2, Zone zone) {
        this(player_1, player_2, zone, false);
    }

    public WorldMartialArtsTournament(Player player_1, Player player_2, Zone zone, boolean isFinal) {
        this.player_1 = player_1;
        this.player_2 = player_2;
        this.zone = zone;
        this.isFinal = isFinal;
        this.timeUp = 0;
        this.timeDown = 180;
        this.competing = true;
        this.isFinished = false;
        this.init();
    }

    private void init() {
        if (player_1 != null) {
            player_1.totalDamageTaken = 0;
            ChangeMapService.gI().changeMap(player_1, zone, 328, 262);
        }
        if (player_2 != null) {
            player_2.totalDamageTaken = 0;
            ChangeMapService.gI().changeMap(player_2, zone, 443, 262);
        }
        if (zone != null) {
            npc = zone.getNpc();
            if (npc != null) {
                Service.gI().setPos(npc, npc.location.x, 312);
            }
        }
        new Thread(this, "WMAT Match").start();
    }

    @Override
    public void run() {
        while (!Maintenance.isRunning && competing && !isFinished) {
            try {
                long startTime = System.currentTimeMillis();
                update();
                Functions.sleep(Math.max(150 - (System.currentTimeMillis() - startTime), 10));
            } catch (Exception e) {
                e.printStackTrace();
                dispose();
                break;
            }
        }
    }

    private void update() {
        if (!competing || isFinished) {
            return;
        }
        if (timeUp < 23) {
            switch (timeUp) {
                case 6 -> {
                    if (npc != null && player_1 != null && player_2 != null) {
                        Service.gI().chat(npc, "Trận đấu giữa " + player_1.name + " và " + player_2.name + " sắp diễn ra");
                    }
                    if (player_1 != null) {
                        Service.gI().setPos0(player_1, 328, 312);
                        player_1.nPoint.setFullHpMp();
                        PlayerService.gI().sendInfoHpMp(player_1);
                    }
                    if (player_2 != null) {
                        Service.gI().setPos0(player_2, 443, 312);
                        player_2.nPoint.setFullHpMp();
                        PlayerService.gI().sendInfoHpMp(player_2);
                    }
                }
                case 9 -> {
                    if (npc != null) {
                        Service.gI().chat(npc, "Xin quý vị khán giả cho 1 tràng pháo tay cổ vũ cho 2 đấu thủ nào");
                    }
                }
                case 11 -> {
                    if (npc != null) {
                        Service.gI().chat(npc, "Mọi người hãy ổn định chỗ ngồi, trận đấu sẽ bắt đầu sau 3 giây nữa");
                    }
                }
                case 13 -> {
                    if (npc != null) {
                        Service.gI().chat(npc, "3");
                    }
                }
                case 15 -> {
                    if (npc != null) {
                        Service.gI().chat(npc, "2");
                    }
                }
                case 17 -> {
                    if (npc != null) {
                        Service.gI().chat(npc, "1");
                    }
                }
                case 19 -> {
                    if (npc != null) {
                        Service.gI().chat(npc, "Trận đấu bắt đầu");
                    }
                }
                case 22 -> {
                    try {
                        if (npc != null) {
                            Service.gI().setPos(npc, npc.location.x, 10000);
                        }
                        if (player_1 != null && player_2 != null) {
                            Service.gI().setPos0(player_1, 328, 312);
                            Service.gI().setPos0(player_2, 443, 312);
                            PlayerService.gI().changeAndSendTypePK(player_1, ConstPlayer.PK_PVP);
                            PlayerService.gI().changeAndSendTypePK(player_2, ConstPlayer.PK_PVP);
                            Service.gI().sendPVP(player_1, player_2);
                            Service.gI().sendPVP(player_2, player_1);
                            new DHVT(player_1, player_2);
                        } else {
                            leaveMap();
                        }
                    } catch (Exception e) {
                        leaveMap();
                    }
                    timeDown = 181;
                }
            }
            timeUp++;
            leaveMap();
        } else if (timeDown > 0) {
            timeDown--;
            fallOut();
            die();
            leaveMap();
        } else {
            timeOut();
        }
    }

    public synchronized void timeOut() {
        if (isFinished) {
            return;
        }
        if (player_1 != null && player_2 != null && player_1.zone != null && player_2.zone != null) {
            if (player_1.totalDamageTaken < player_2.totalDamageTaken) {
                plWin = player_1;
                plLose = player_2;
            } else {
                plWin = player_2;
                plLose = player_1;
            }
        } else if (player_1 != null && player_1.zone == null) {
            plWin = player_2;
            plLose = player_1;
        } else if (player_2 != null && player_2.zone == null) {
            plWin = player_1;
            plLose = player_2;
        }
        typeEnd = 0;
        finish();
    }

    private void die() {
        try {
            if (player_2 != null && player_2.isDie()) {
                plWin = player_1;
                plLose = player_2;
                typeEnd = 2;
                finish();
            } else if (player_1 != null && player_1.isDie()) {
                plWin = player_2;
                plLose = player_1;
                typeEnd = 2;
                finish();
            }
        } catch (Exception e) {
        }
    }

    private void leaveMap() {
        try {
            if (player_2 == null || player_2.zone == null || !player_2.zone.equals(zone) || player_2.zone.map.mapId != 51) {
                plWin = player_1;
                plLose = player_2;
                typeEnd = 3;
                finish();
            } else if (player_1 == null || player_1.zone == null || !player_1.zone.equals(zone) || player_1.zone.map.mapId != 51) {
                plWin = player_2;
                plLose = player_1;
                typeEnd = 3;
                finish();
            }
        } catch (Exception e) {
        }
    }

    private boolean fallOutCheck(Player pl) {
        if (pl == null || pl.location == null) {
            return true;
        }
        return pl.location.x < 158 || pl.location.x > 610 || pl.location.y > 320;
    }

    private void fallOut() {
        if (player_1 != null && player_2 != null && player_1.zone != null && player_2.zone != null) {
            if (fallOutCheck(player_1)) {
                plWin = player_2;
                plLose = player_1;
                typeEnd = 1;
                finish();
            } else if (fallOutCheck(player_2)) {
                plWin = player_1;
                plLose = player_2;
                typeEnd = 1;
                finish();
            }
        }
    }

    public synchronized void finish() {
        if (isFinished) {
            return;
        }
        this.competing = false;
        this.isFinished = true;
        try {
            thongBao();
            if (plWin != null) {
                plWin.martialArtsTournamentWins++;
                if (this.isFinal) {
                    WorldMartialArtsTournamentManager.gI().rewardChampion(plWin);
                } else {
                    WorldMartialArtsTournamentManager.gI().addWinnerToWait(plWin.id);
                }
            }
            if (plWin != null && plWin.zone != null) {
                plWin.thongBaoChangeMap = true;
                plWin.textThongBaoChangeMap = ConstTournament.TEXT_THANG_VONG_NAY;
                PlayerService.gI().changeAndSendTypePK(plWin, ConstPlayer.NON_PK);
                if (plLose != null) {
                    Service.gI().sendPlayerVS(plWin, plLose, (byte) 0);
                }
            }
            if (plLose != null && plLose.zone != null) {
                plLose.thongBaoThua = true;
                plLose.textThongBaoThua = ConstTournament.TEXT_CHIA_BUON;
                PlayerService.gI().changeAndSendTypePK(plLose, ConstPlayer.NON_PK);
                if (plWin != null) {
                    Service.gI().sendPlayerVS(plLose, plWin, (byte) 0);
                }
            }

            Functions.sleep(250);

            // Thưởng thắng vòng: +1.000 Hồng Ngọc
            if (plWin != null) {
                long newRuby = (long) plWin.inventory.ruby + ConstTournament.REWARD_ROUND_RUBY;
                plWin.inventory.ruby = (int) Math.min(newRuby, 2_000_000_000L);
                Service.gI().sendMoney(plWin);
                Service.gI().sendThongBao(plWin, "Bạn vừa nhận thưởng 1.000 Hồng Ngọc vì thắng vòng đấu!");
            }

            if (npc != null) {
                Service.gI().setPos(npc, npc.location.x, 312);
                npcChat();
            }

            Functions.sleep(4750);

            if (plWin != null && plWin.zone != null) {
                plWin.nPoint.setFullHpMp();
                PlayerService.gI().sendInfoHpMp(plWin);
                Zone pl2z = ChangeMapService.gI().getMapCanJoin(plWin, 52);
                ChangeMapService.gI().changeMap(plWin, pl2z, Util.nextInt(200, 500), 336);
            }
            if (plLose != null && plLose.zone != null && plLose.zone.map.mapId != plLose.gender + 21 && plLose.thongBaoThua) {
                Zone pl1z = ChangeMapService.gI().getMapCanJoin(plLose, plLose.gender + 21);
                ChangeMapService.gI().changeMap(plLose, pl1z, Util.nextInt(200, 500), 1);
                if (plLose.isDie()) {
                    Service.gI().hsChar(plLose, plLose.nPoint.hpMax, plLose.nPoint.mpMax);
                }
            }
            if (npc != null) {
                Service.gI().setPos(npc, npc.location.x, 112);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.dispose();
        }
    }

    private void thongBao() {
        if (plWin == null) {
            return;
        }
        switch (typeEnd) {
            case 0 -> {
                Service.gI().sendThongBao(plWin, ConstTournament.TEXT_HET_GIO);
                if (plLose != null) {
                    Service.gI().sendThongBao(plLose, ConstTournament.TEXT_XU_THUA_HET_GIO);
                }
            }
            case 1 -> {
                Service.gI().sendThongBao(plWin, ConstTournament.TEXT_DOI_THU_BO_CUOC_ROI_MAP);
                if (plLose != null) {
                    Service.gI().sendThongBao(plLose, ConstTournament.TEXT_XU_THUA_BO_CHAY);
                }
            }
            case 2 -> {
                Service.gI().sendThongBao(plWin, ConstTournament.TEXT_DOI_THU_KIET_SUC);
                if (plLose != null) {
                    Service.gI().sendThongBao(plLose, ConstTournament.TEXT_XU_THUA_KIET_SUC);
                }
            }
            case 3 -> {
                Service.gI().sendThongBao(plWin, ConstTournament.TEXT_DOI_THU_BO_CUOC_ROI_MAP);
                if (plLose != null) {
                    Service.gI().sendThongBao(plLose, ConstTournament.TEXT_XU_THUA_BO_CHAY);
                }
            }
        }
    }

    private void npcChat() {
        if (npc == null || plWin == null) {
            return;
        }
        switch (typeEnd) {
            case 0 -> {
                Service.gI().chat(npc, ConstTournament.TEXT_NPC_CHAT_HET_GIO.replaceAll("%1", plWin.name));
            }
            case 1 -> {
                Service.gI().chat(npc, ConstTournament.TEXT_NPC_CHAT_ROI_DAI.replaceAll("%1", plWin.name));
            }
            case 2 -> {
                Service.gI().chat(npc, ConstTournament.TEXT_NPC_CHAT_DOI_THU_KIET_SUC.replaceAll("%1", plWin.name));
            }
            case 3 -> {
                Service.gI().chat(npc, ConstTournament.TEXT_NPC_CHAT_DOI_THU_BO_CUOC_ROI_MAP.replaceAll("%1", plWin.name));
            }
        }
    }

    public void dispose() {
        competing = false;
        isFinished = true;
        player_1 = null;
        player_2 = null;
        plWin = null;
        plLose = null;
        npc = null;
        zone = null;
        WorldMartialArtsTournamentManager.gI().removeWMAT(this);
    }
}