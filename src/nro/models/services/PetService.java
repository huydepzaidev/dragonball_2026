package nro.models.services;

import nro.models.consts.ConstPlayer;
import nro.models.item.Item;
import nro.models.player.NewPet;
import nro.models.player.Pet;
import nro.models.player.Player;
import nro.models.map.service.ChangeMapService;
import nro.models.utils.SkillUtil;
import nro.models.utils.Util;

/**
 *
 * @author By Mr Blue
 *
 */
public class PetService {

    private static PetService instance;

    public static PetService gI() {
        if (instance == null) {
            instance = new PetService();
        }
        return instance;
    }

    public static boolean isSecondDisciple(Player player) {
        return player != null && player.pet != null && isSecondDiscipleType(player.pet.typePet);
    }

    public static boolean isSecondDiscipleType(byte typePet) {
        return typePet >= 2 && typePet <= 4;
    }

    public boolean canReplacePet(Player player) {
        if (player == null) {
            return false;
        }
        if (player.pet == null || player.pet.inventory == null) {
            Service.gI().sendThongBao(player, "Bạn chưa có đệ tử để thay đổi.");
            return false;
        }
        if (hasEquippedItems(player.pet)) {
            Service.gI().sendThongBao(player,
                    "Đệ tử vẫn đang mặc trang bị. Hãy tháo toàn bộ trang bị và cải trang trước khi đổi.");
            return false;
        }
        return true;
    }

    static boolean hasEquippedItems(Pet pet) {
        if (pet == null || pet.inventory == null) {
            return false;
        }
        for (Item item : pet.inventory.itemsBody) {
            if (item != null && item.isNotNullItem()) {
                return true;
            }
        }
        return false;
    }

    public boolean replaceWithSecondDisciple(Player player, byte typePet) {
        if (typePet < 2 || typePet > 4 || !canReplacePet(player)) {
            return false;
        }
        Pet newPet = createSecondDisciple(player, typePet);
        Pet oldPet = player.pet;
        if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {
            oldPet.unFusion();
        }
        ChangeMapService.gI().exitMap(oldPet);
        oldPet.dispose();
        player.pet = newPet;
        return true;
    }

    public void createNormalPet(Player player, int gender, byte... limitPower) {
        new Thread(() -> {
            try {
                createNewPet(player, false, false, false, false, (byte) gender);
                if (limitPower != null && limitPower.length == 1) {
                    player.pet.nPoint.limitPower = limitPower[0];
                }
                Thread.sleep(1000);
                Service.gI().chatJustForMe(player, player.pet, "Xin hãy thu nhận tao làm đệ tử");
            } catch (Exception e) {
            }
        }).start();
    }

    public void createNormalPet(Player player, byte... limitPower) {
        new Thread(() -> {
            try {
                createNewPet(player, false, false, false, false);
                if (limitPower != null && limitPower.length == 1) {
                    player.pet.nPoint.limitPower = limitPower[0];
                }
                Thread.sleep(1000);
                Service.gI().chatJustForMe(player, player.pet, "Xin hãy thu nhận tao làm đệ tử");
            } catch (Exception e) {
            }
        }).start();
    }

    public void createMabuPet(Player player, int gender, byte... limitPower) {
        new Thread(() -> {
            try {
                createNewPet(player, true, false, false, false, (byte) gender);
                if (limitPower != null && limitPower.length == 1) {
                    player.pet.nPoint.limitPower = limitPower[0];
                }
                Thread.sleep(1000);
                Service.gI().chatJustForMe(player, player.pet, "Oa oa oa...");
            } catch (Exception e) {
            }
        }).start();
    }

    public void createMabuPet(Player player, byte... limitPower) {
        new Thread(() -> {
            try {
                createNewPet(player, true, false, false, false);
                if (limitPower != null && limitPower.length == 1) {
                    player.pet.nPoint.limitPower = limitPower[0];
                }
                Thread.sleep(1000);
                Service.gI().chatJustForMe(player, player.pet, "Oa oa oa...");
            } catch (Exception e) {
            }
        }).start();
    }

    public void createUubPet(Player player, byte... limitPower) {
        new Thread(() -> {
            try {
                createNewPet(player, false, true, false, false, (byte) player.gender);
                if (limitPower != null && limitPower.length == 1) {
                    player.pet.nPoint.limitPower = limitPower[0];
                }
                Thread.sleep(1000);
                Service.gI().chatJustForMe(player, player.pet, "Xin hãy thu nhận tao làm đệ tử");
            } catch (Exception e) {
            }
        }).start();
    }
    
    public void createJirenPet(Player player, byte... limitPower) {
        new Thread(() -> {
            try {
                createNewPet(player, false, false, false,true, (byte) player.gender);
                if (limitPower != null && limitPower.length == 1) {
                    player.pet.nPoint.limitPower = limitPower[0];
                }
                Thread.sleep(1000);
                Service.gI().chatJustForMe(player, player.pet, "Xin hãy thu nhận tao làm đệ tử");
            } catch (Exception e) {
            }
        }).start();
    }

    public void createKidBeerPet(Player player, byte... limitPower) {
        new Thread(() -> {
            try {
                createNewPet(player, false, false, true, false, (byte) player.gender);
                if (limitPower != null && limitPower.length == 1) {
                    player.pet.nPoint.limitPower = limitPower[0];
                }
                Thread.sleep(1000);
                Service.gI().chatJustForMe(player, player.pet, "Hãy hợp tác với ta, Kakarot!");
            } catch (Exception e) {
            }
        }).start();
    }

    public void changeNormalPet(Player player, int gender) {
        if (!canReplacePet(player)) {
            return;
        }
        if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {
            player.pet.unFusion();
        }
        ChangeMapService.gI().exitMap(player.pet);
        player.pet.dispose();
        player.pet = null;
        // A changed disciple is a new disciple: its power limit starts over.
        createNormalPet(player, gender);
    }

    public void changeNormalPet(Player player) {
        if (!canReplacePet(player)) {
            return;
        }
        if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {
            player.pet.unFusion();
        }
        ChangeMapService.gI().exitMap(player.pet);
        player.pet.dispose();
        player.pet = null;
        createNormalPet(player);
    }

    public void changeMabuPet(Player player) {
        if (!canReplacePet(player)) {
            return;
        }
        if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {
            player.pet.unFusion();
        }
        ChangeMapService.gI().exitMap(player.pet);
        player.pet.dispose();
        player.pet = null;
        createMabuPet(player);
    }

    public void changeUubPet(Player player) {
        if (!canReplacePet(player)) {
            return;
        }
        byte gender = player.pet.gender;
        if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {
            player.pet.unFusion();
        }
        ChangeMapService.gI().exitMap(player.pet);
        player.pet.dispose();
        player.pet = null;
        createUubPet(player, gender);
    }

    public void changeKidBeerPet(Player player) {
        if (!canReplacePet(player)) {
            return;
        }
        byte gender = player.pet.gender;
        if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {
            player.pet.unFusion();
        }
        ChangeMapService.gI().exitMap(player.pet);
        player.pet.dispose();
        player.pet = null;
        createKidBeerPet(player, gender);
    }
    
    public void changeJirenPet(Player player) {
        if (!canReplacePet(player)) {
            return;
        }
        byte gender = player.pet.gender;
        if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {
            player.pet.unFusion();
        }
        ChangeMapService.gI().exitMap(player.pet);
        player.pet.dispose();
        player.pet = null;
        createJirenPet(player, gender);
    }

    public void changeMabuPet(Player player, int gender) {
        if (!canReplacePet(player)) {
            return;
        }
        if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {
            player.pet.unFusion();
        }
        ChangeMapService.gI().exitMap(player.pet);
        player.pet.dispose();
        player.pet = null;
        createMabuPet(player, gender);
    }

    public void changeNamePet(Player player, String name) {
        try {
            if (!InventoryService.gI().isExistItemBag(player, 400)) {
                Service.gI().sendThongBao(player, "Bạn cần thẻ đặt tên đệ tử, mua tại Santa");
                return;
            } else if (Util.haveSpecialCharacter(name)) {
                Service.gI().sendThongBao(player, "Tên không được chứa ký tự đặc biệt");
                return;
            } else if (name.length() > 10) {
                Service.gI().sendThongBao(player, "Tên quá dài");
                return;
            }
            ChangeMapService.gI().exitMap(player.pet);
            player.pet.name = "$" + name.toLowerCase().trim();
            InventoryService.gI().subQuantityItemsBag(player, InventoryService.gI().findItemBag(player, 400), 1);
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    Service.gI().chatJustForMe(player, player.pet, "Cảm ơn sư phụ đã đặt cho con tên " + name);
                } catch (Exception e) {
                }
            }).start();
        } catch (Exception ex) {

        }
    }

    private int[] getDataPetNormal() {
        int[] petData = new int[5];
        petData[0] = Util.nextInt(40, 105) * 20; //hp
        petData[1] = Util.nextInt(40, 105) * 20; //mp
        petData[2] = Util.nextInt(20, 45); //dame
        petData[3] = Util.nextInt(9, 50); //def
        petData[4] = Util.nextInt(0, 2); //crit
        return petData;
    }

    private int[] getDataPetMabu() {
        int[] petData = new int[5];
        petData[0] = Util.nextInt(40, 105) * 20; //hp
        petData[1] = Util.nextInt(40, 105) * 20; //mp
        petData[2] = Util.nextInt(50, 120); //dame
        petData[3] = Util.nextInt(9, 50); //def
        petData[4] = Util.nextInt(0, 2); //crit
        return petData;
    }

    private int[] getDataPetUub() {
        int[] petData = new int[5];
        petData[0] = 400_000; // hp
        petData[1] = 400_000; // mp
        petData[2] = 20_000;  // dame
        petData[3] = Util.nextInt(9, 50); //def
        petData[4] = Util.nextInt(0, 2); //crit
        return petData;
    }

    private int[] getDataPetKidBeer() {
        int[] petData = new int[5];
        petData[0] = 400_000; // hp
        petData[1] = 400_000; // mp
        petData[2] = 20_000;  // dame
        petData[3] = Util.nextInt(9, 50); //def
        petData[4] = Util.nextInt(0, 2); //crit
        return petData;
    }
    
    private int[] getDataPetJiren() {
        int[] petData = new int[5];
        petData[0] = 400_000; // hp
        petData[1] = 400_000; // mp
        petData[2] = 20_000;  // dame
        petData[3] = Util.nextInt(9, 50); //def
        petData[4] = Util.nextInt(0, 2); //crit
        return petData;
    }

    private Pet createSecondDisciple(Player player, byte typePet) {
        int[] data = switch (typePet) {
            case 2 -> getDataPetUub();
            case 3 -> getDataPetKidBeer();
            case 4 -> getDataPetJiren();
            default -> throw new IllegalArgumentException("Invalid second disciple type: " + typePet);
        };

        Pet pet = new Pet(player);
        pet.typePet = typePet;
        pet.name = switch (typePet) {
            case 2 -> "$Uub";
            case 3 -> "$Kid Beerus";
            default -> "$Kid Jiren";
        };
        pet.gender = switch (typePet) {
            case 2 -> ConstPlayer.TRAI_DAT;
            case 3 -> ConstPlayer.XAYDA;
            default -> ConstPlayer.NAMEC;
        };
        pet.id = player.isPl() ? -player.id : -Math.abs(player.id) - 100000;
        pet.nPoint.power = 40_000_000_000L;
        pet.nPoint.limitPower = 5;
        pet.nPoint.stamina = 1000;
        pet.nPoint.maxStamina = 1000;
        pet.nPoint.hpg = data[0];
        pet.nPoint.mpg = data[1];
        pet.nPoint.dameg = data[2];
        pet.nPoint.defg = data[3];
        pet.nPoint.critg = data[4];

        for (int i = 0; i < 9; i++) {
            pet.inventory.itemsBody.add(ItemService.gI().createItemNull());
        }
        pet.playerSkill.skills.add(SkillUtil.createSkill(Util.nextInt(0, 2) * 2, 1));
        for (int i = 1; i < 5; i++) {
            pet.playerSkill.skills.add(SkillUtil.createEmptySkill());
        }
        pet.openSkill2();
        pet.openSkill3();
        pet.openSkill4();
        pet.nPoint.setFullHpMp();
        return pet;
    }

    private void createNewPet(Player player, boolean isMabu, boolean isUub, boolean isKidBeer, boolean isJiren, byte... gender) {
        int[] data;

        if (isMabu) {
            data = getDataPetMabu();
        } else if (isUub) {
            data = getDataPetUub();
        } else if (isKidBeer) {
            data = getDataPetKidBeer();
        } else if (isJiren) {
            data = getDataPetJiren();
        } else {
            data = getDataPetNormal();
        }

        Pet pet = new Pet(player);

        pet.name = "$" + (isMabu ? "Mabư" : isUub ? "Uub" : isKidBeer ? "Kid Beerus" : isJiren ? "Kid Jiren" : "Đệ tử");

        pet.gender = isUub ? ConstPlayer.TRAI_DAT
                : isKidBeer ? ConstPlayer.XAYDA
                : isJiren ? ConstPlayer.NAMEC
                : (gender != null && gender.length != 0) ? gender[0] : (byte) Util.nextInt(0, 2);

        pet.id = player.isPl() ? -player.id : -Math.abs(player.id) - 100000;

        pet.nPoint.power = isUub ? 40000000000L : isMabu ? 1500000L : isKidBeer ? 40000000000L : isJiren ? 40000000000L : 2000L;

        pet.typePet = (byte) (isMabu ? 1 : isUub ? 2 : isKidBeer ? 3 : isJiren ? 4 : 0);
        if (pet.typePet >= 2 && pet.typePet <= 4) {
            pet.nPoint.limitPower = 5;
        }

        pet.nPoint.stamina = 1000;
        pet.nPoint.maxStamina = 1000;
        pet.nPoint.hpg = data[0];
        pet.nPoint.mpg = data[1];
        pet.nPoint.dameg = data[2];
        pet.nPoint.defg = data[3];
        pet.nPoint.critg = data[4];

        int itemBodySize = 7;
        if (pet.typePet == 2 || pet.typePet == 3 || pet.typePet == 4) {
            itemBodySize = 9;
        }
        for (int i = 0; i < itemBodySize; i++) {
            pet.inventory.itemsBody.add(ItemService.gI().createItemNull());
        }

        pet.playerSkill.skills.add(SkillUtil.createSkill(Util.nextInt(0, 2) * 2, 1));
        int emptySkillCount = pet.typePet >= 2 && pet.typePet <= 4 ? 4 : 6;
        for (int i = 0; i < emptySkillCount; i++) {
            pet.playerSkill.skills.add(SkillUtil.createEmptySkill());
        }

        pet.nPoint.setFullHpMp();
        player.pet = pet;
    }

    public static void Pet2(Player pl, int h, int b, int l) {
        if (pl.newPet != null) {
            pl.newPet.dispose();
        }
        pl.newPet = new NewPet(pl, (short) h, (short) b, (short) l);
        pl.newPet.name = "$";
        pl.newPet.gender = pl.gender;
        pl.newPet.nPoint.tiemNang = 1;
        pl.newPet.nPoint.power = 1;
        pl.newPet.nPoint.limitPower = 1;
        pl.newPet.nPoint.hpg = 500000000;
        pl.newPet.nPoint.mpg = 500000000;
        pl.newPet.nPoint.hp = 500000000;
        pl.newPet.nPoint.mp = 500000000;
        pl.newPet.nPoint.dameg = 1;
        pl.newPet.nPoint.defg = 1;
        pl.newPet.nPoint.critg = 1;
        pl.newPet.nPoint.stamina = 1;
        pl.newPet.nPoint.setBasePoint();
        pl.newPet.nPoint.setFullHpMp();
    }
}
