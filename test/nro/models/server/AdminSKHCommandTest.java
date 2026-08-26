package nro.models.server;

import java.lang.reflect.Field;
import java.util.List;
import nro.models.item.Item;
import nro.models.item.Item.ItemOption;
import nro.models.network.MySession;
import nro.models.player.Player;
import nro.models.player_system.Template;
import nro.models.services.ActivationRewardService;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import sun.misc.Unsafe;

public final class AdminSKHCommandTest {

    private AdminSKHCommandTest() {
    }

    public static void main(String[] args) {
        initTemplates();
        testOptionMappings();
        testRewardCreationOptionsAndTiers();
        testPermissionAndSecurity();
        testInventoryAtomicity();
        testValidationAndSafeHandling();
        testAllSetResolutions();
        testQuickCommandsAllGenders();

        System.out.println("ADMIN_SKH_COMMAND_TEST_OK vip_sets=true parameterized=true atomicity=true options=true permission=true");
    }

    private static void initTemplates() {
        Manager.ITEM_TEMPLATES.clear();
        for (int i = 0; i <= 2000; i++) {
            Template.ItemTemplate temp = new Template.ItemTemplate();
            temp.id = (short) i;
            temp.name = "Item " + i;
            temp.type = (byte) (i % 5);
            Manager.ITEM_TEMPLATES.add(temp);
        }

        Manager.ITEM_OPTION_TEMPLATES.clear();
        for (int i = 0; i <= 500; i++) {
            Manager.ITEM_OPTION_TEMPLATES.add(new Template.ItemOptionTemplate(i, "Option " + i, 0));
        }
    }

    private static void testOptionMappings() {
        require(ActivationRewardService.getVipOptionByPlanet(0) == 245, "Earth VIP must be 245 (Kaio)");
        require(ActivationRewardService.getVipOptionByPlanet(1) == 237, "Namek VIP must be 237 (Nail)");
        require(ActivationRewardService.getVipOptionByPlanet(2) == 241, "Saiyan VIP must be 241 (Cadic M)");

        require(ActivationRewardService.resolveActivationOption("kaio") == 245, "resolve kaio");
        require(ActivationRewardService.resolveActivationOption("kaiovip") == 245, "resolve kaiovip");
        require(ActivationRewardService.resolveActivationOption("nail") == 237, "resolve nail");
        require(ActivationRewardService.resolveActivationOption("nailvip") == 237, "resolve nailvip");
        require(ActivationRewardService.resolveActivationOption("cadicm") == 241, "resolve cadicm");
        require(ActivationRewardService.resolveActivationOption("cadicmvip") == 241, "resolve cadicmvip");
        require(ActivationRewardService.resolveActivationOption("gohan") == 233, "resolve gohan");
        require(ActivationRewardService.resolveActivationOption("songoku") == 129, "resolve songoku");
        require(ActivationRewardService.resolveActivationOption("kirin") == 128, "resolve kirin");
        require(ActivationRewardService.resolveActivationOption("thenxinhang") == 127, "resolve thenxinhang");
        require(ActivationRewardService.resolveActivationOption("kakarot") == 133, "resolve kakarot");
        require(ActivationRewardService.resolveActivationOption("nappa") == 135, "resolve nappa");
        require(ActivationRewardService.resolveActivationOption("cadic") == 134, "resolve cadic");
        require(ActivationRewardService.resolveActivationOption("pikkoro") == 132, "resolve pikkoro");
        require(ActivationRewardService.resolveActivationOption("octieu") == 131, "resolve octieu");
        require(ActivationRewardService.resolveActivationOption("piccolo") == 130, "resolve piccolo");
        require(ActivationRewardService.resolveActivationOption("245") == 245, "resolve 245");
        require(ActivationRewardService.resolveActivationOption("237") == 237, "resolve 237");
        require(ActivationRewardService.resolveActivationOption("241") == 241, "resolve 241");
        require(ActivationRewardService.resolveActivationOption("unknown") == -1, "resolve unknown must return -1");

        require(ActivationRewardService.getNativePlanetForOption(245) == 0, "Kaio native planet Earth");
        require(ActivationRewardService.getNativePlanetForOption(237) == 1, "Nail native planet Namek");
        require(ActivationRewardService.getNativePlanetForOption(241) == 2, "Cadic M native planet Saiyan");

        require(ActivationRewardService.isOptionAllowedForPlanet(245, 0), "Kaio allowed for Earth");
        require(!ActivationRewardService.isOptionAllowedForPlanet(245, 1), "Kaio not in Namek default pool");
        require(ActivationRewardService.isOptionAllowedForPlanet(233, 0), "Gohan allowed for Earth");
        require(ActivationRewardService.isOptionAllowedForPlanet(233, 1), "Gohan allowed for Namek");
        require(ActivationRewardService.isOptionAllowedForPlanet(233, 2), "Gohan allowed for Saiyan");
    }

    private static void testRewardCreationOptionsAndTiers() {
        // Earth Kaio Tier 12
        List<Item> earthRewards = ActivationRewardService.gI().createSpecificRewards(0, 245, List.of(), 12);
        require(earthRewards.size() == 5, "Must create 5 items");
        int[] expectedEarthTier12 = {233, 245, 257, 269, 281};
        for (int i = 0; i < 5; i++) {
            require(earthRewards.get(i).template.id == expectedEarthTier12[i],
                    "Earth Tier 12 item slot " + i + " must be " + expectedEarthTier12[i]);
            assertItemSKHOptions(earthRewards.get(i), 245, new int[]{246, 247, 248});
        }

        // Namek Nail Tier 12
        List<Item> namekRewards = ActivationRewardService.gI().createSpecificRewards(1, 237, List.of(), 12);
        require(namekRewards.size() == 5, "Must create 5 items");
        int[] expectedNamekTier12 = {237, 249, 261, 273, 281};
        for (int i = 0; i < 5; i++) {
            require(namekRewards.get(i).template.id == expectedNamekTier12[i],
                    "Namek Tier 12 item slot " + i + " must be " + expectedNamekTier12[i]);
            assertItemSKHOptions(namekRewards.get(i), 237, new int[]{238, 239, 240});
        }

        // Saiyan Cadic M Tier 12
        List<Item> saiyanRewards = ActivationRewardService.gI().createSpecificRewards(2, 241, List.of(), 12);
        require(saiyanRewards.size() == 5, "Must create 5 items");
        int[] expectedSaiyanTier12 = {241, 253, 265, 277, 281};
        for (int i = 0; i < 5; i++) {
            require(saiyanRewards.get(i).template.id == expectedSaiyanTier12[i],
                    "Saiyan Tier 12 item slot " + i + " must be " + expectedSaiyanTier12[i]);
            assertItemSKHOptions(saiyanRewards.get(i), 241, new int[]{242, 243, 244});
        }

        // Tier 1 (sơ sinh)
        List<Item> earthTier1 = ActivationRewardService.gI().createSpecificRewards(0, 245, List.of(), 1);
        int[] expectedEarthTier1 = {0, 6, 21, 27, 12};
        for (int i = 0; i < 5; i++) {
            require(earthTier1.get(i).template.id == expectedEarthTier1[i],
                    "Earth Tier 1 item slot " + i + " must be " + expectedEarthTier1[i]);
        }

        // Tier 8
        List<Item> earthTier8 = ActivationRewardService.gI().createSpecificRewards(0, 245, List.of(), 8);
        int[] expectedEarthTier8 = {139, 143, 147, 151, 187};
        for (int i = 0; i < 5; i++) {
            require(earthTier8.get(i).template.id == expectedEarthTier8[i],
                    "Earth Tier 8 item slot " + i + " must be " + expectedEarthTier8[i]);
        }
    }

    private static void assertItemSKHOptions(Item item, int skhOption, int[] subOptions) {
        int skhCount = 0;
        for (ItemOption opt : item.itemOptions) {
            if (opt != null && opt.optionTemplate != null) {
                if (opt.optionTemplate.id == 102 || opt.optionTemplate.id == 107) {
                    throw new AssertionError("Crystal star option " + opt.optionTemplate.id + " must not exist on SKH reward");
                }
                if (opt.optionTemplate.id == skhOption) {
                    skhCount++;
                }
            }
        }
        require(skhCount == 1, "Must have exactly 1 main SKH option #" + skhOption + ", found: " + skhCount);

        for (int sub : subOptions) {
            int subCount = 0;
            for (ItemOption opt : item.itemOptions) {
                if (opt != null && opt.optionTemplate != null && opt.optionTemplate.id == sub) {
                    subCount++;
                }
            }
            require(subCount == 1, "Must have exactly 1 sub-option #" + sub + ", found: " + subCount);
        }
    }

    private static void testPermissionAndSecurity() {
        Player normalPlayer = createTestPlayer(false, 0, 20);
        require(!Command.gI().check(normalPlayer, "skhvip"), "Normal player must not execute skhvip");
        require(!Command.gI().check(normalPlayer, "buffskh"), "Normal player must not execute buffskh");
        require(!Command.gI().check(normalPlayer, "skh kaio"), "Normal player must not execute skh");
        require(!ActivationRewardService.gI().buffActivationSet(normalPlayer, 0, 245, 12),
                "buffActivationSet must reject non-admin");
        require(countFilledItems(normalPlayer) == 0, "Normal player must not receive items");
    }

    private static void testInventoryAtomicity() {
        // Test with 4 slots (insufficient)
        Player admin4Slots = createTestPlayer(true, 0, 4);
        boolean result4 = ActivationRewardService.gI().buffActivationSet(admin4Slots, 0, 245, 12);
        require(!result4, "Buff with 4 slots must fail");
        require(countFilledItems(admin4Slots) == 0, "No items must be added when slots < 5");

        // Test with 0 slots
        Player admin0Slots = createTestPlayer(true, 0, 0);
        boolean result0 = ActivationRewardService.gI().buffActivationSet(admin0Slots, 0, 245, 12);
        require(!result0, "Buff with 0 slots must fail");
        require(countFilledItems(admin0Slots) == 0, "No items must be added when slots == 0");

        // Test with 5 slots (exact requirement)
        Player admin5Slots = createTestPlayer(true, 0, 5);
        boolean result5 = ActivationRewardService.gI().buffActivationSet(admin5Slots, 0, 245, 12);
        require(result5, "Buff with 5 slots must succeed");
        require(countFilledItems(admin5Slots) == 5, "5 items must be added");
    }

    private static void testValidationAndSafeHandling() {
        Player admin = createTestPlayer(true, 0, 20);

        // Unknown set
        Command.gI().check(admin, "skh unknown");
        require(countFilledItems(admin) == 0, "skh unknown must not add items");

        // Invalid tier
        Command.gI().check(admin, "skh kaio 0");
        require(countFilledItems(admin) == 0, "skh tier 0 must not add items");
        Command.gI().check(admin, "skh kaio 13");
        require(countFilledItems(admin) == 0, "skh tier 13 must not add items");

        // Invalid gender
        Command.gI().check(admin, "skh kaio 12 3");
        require(countFilledItems(admin) == 0, "skh gender 3 must not add items");
        Command.gI().check(admin, "skh kaio 12 -1");
        require(countFilledItems(admin) == 0, "skh gender -1 must not add items");
    }

    private static void testAllSetResolutions() {
        String[] sets = {
            "kaio", "nail", "cadicm", "gohan", "songoku", "kirin", "thenxinhang",
            "kakarot", "nappa", "cadic", "pikkoro", "octieu", "piccolo"
        };
        for (String set : sets) {
            Player admin = createTestPlayer(true, 0, 20);
            boolean executed = Command.gI().check(admin, "skh " + set);
            require(executed, "Command skh " + set + " must be executed by admin");
            require(countFilledItems(admin) == 5, "Must receive 5 items for set " + set);
        }
    }

    private static void testQuickCommandsAllGenders() {
        // Gender 0 -> Earth Kaio (245)
        Player earthAdmin = createTestPlayer(true, 0, 20);
        Command.gI().check(earthAdmin, "skhvip");
        require(countFilledItems(earthAdmin) == 5, "Earth admin receives 5 items via skhvip");
        require(earthAdmin.inventory.itemsBag.get(0).template.id == 233, "Earth slot 0 is 233");

        // Gender 1 -> Namek Nail (237)
        Player namekAdmin = createTestPlayer(true, 1, 20);
        Command.gI().check(namekAdmin, "buffskh");
        require(countFilledItems(namekAdmin) == 5, "Namek admin receives 5 items via buffskh");
        require(namekAdmin.inventory.itemsBag.get(0).template.id == 237, "Namek slot 0 is 237");

        // Gender 2 -> Saiyan Cadic M (241)
        Player saiyanAdmin = createTestPlayer(true, 2, 20);
        Command.gI().check(saiyanAdmin, "skhvip");
        require(countFilledItems(saiyanAdmin) == 5, "Saiyan admin receives 5 items via skhvip");
        require(saiyanAdmin.inventory.itemsBag.get(0).template.id == 241, "Saiyan slot 0 is 241");
    }

    private static Player createTestPlayer(boolean isAdmin, int gender, int emptySlots) {
        Player player = new Player();
        player.gender = (byte) gender;
        player.session = createTestSession(isAdmin);

        player.inventory.itemsBag.clear();
        for (int i = 0; i < emptySlots; i++) {
            player.inventory.itemsBag.add(new Item());
        }
        return player;
    }

    private static MySession createTestSession(boolean isAdmin) {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Unsafe unsafe = (Unsafe) f.get(null);
            MySession session = (MySession) unsafe.allocateInstance(MySession.class);
            session.isAdmin = isAdmin;
            return session;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static int countFilledItems(Player player) {
        int count = 0;
        for (Item item : player.inventory.itemsBag) {
            if (item != null && item.isNotNullItem()) {
                count++;
            }
        }
        return count;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
