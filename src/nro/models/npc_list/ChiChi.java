package nro.models.npc_list;

import nro.models.consts.ConstNpc;
import java.util.ArrayList;
import java.util.List;
import nro.models.item.Item;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.server.Manager;
import nro.models.server.EventControlService;
import nro.models.server.DailyRankingService;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.Service;
import nro.models.services_func.Input;
import nro.models.shop.ShopService;
import nro.models.utils.Util;

/**
 *
 * @author By Mr Blue
 *
 */
public class ChiChi extends Npc {

    private static final String CHILDRENS_DAY = "childrens_day";
    private static final String SUGARCANE = "sugarcane";
    private static final String FRUIT_ICE_CREAM = "fruit_ice_cream";
    private static final int MENU_DOI_QUA_MUA_HE = 20082026;
    private static final int MENU_XOA_VAT_PHAM_HSD = 20082027;
    private static final int SO_LUONG_NGUYEN_LIEU = 99;
    private static final int[] NGUYEN_LIEU_MUA_HE = {695, 696, 697, 698};
    private static final String[] TEN_NGUYEN_LIEU_MUA_HE = {"Vỏ ốc", "Vỏ sò", "Con cua", "Sao biển"};
    private static final int SO_LUONG_THE_NHAN_DUOC = 10;
    private static final int SO_LAN_DOI_TOI_DA_MOI_LAN = 1000;
    private static final int[] THE_SUU_TAP_MUA_HE = {1204, 1791, 1792, 1793};
    private static final String[] TEN_THE_SUU_TAP_MUA_HE = {"Rồng Thần Namek", "Oozaru", "Oozarun 1", "Oozarun 2"};
    private static final int[] TY_LE_THE_SUU_TAP = {35, 35, 15, 15};

    public ChiChi(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            EventControlService eventControl = EventControlService.gI();
            List<String> menu = new ArrayList<>();

            if (eventControl.isEnabled(EventControlService.SUMMER)) {
                menu.add("Sự kiện\nhè");
                menu.add("Cửa hàng\nhè");
                menu.add("Đổi quà\nsự kiện");
                menu.add("Xem điểm\nsự kiện");
            }
            if (eventControl.isEnabled(CHILDRENS_DAY)) {
                menu.add("Top\nHộp quà\nthiếu nhi\n2025");
            }
            if (eventControl.isEnabled(SUGARCANE)) {
                menu.add("Top\nNước mía");
            }
            if (eventControl.isEnabled(FRUIT_ICE_CREAM)) {
                menu.add("Top\nKem trái cây");
            }
            if (isOldChiChiEventEnabled(eventControl)) {
                menu.add("Cửa hàng\nsự kiện");
            }
            menu.add("Bỏ đồ\nhạn sử dụng");
            menu.add("Đóng");

            createOtherMenu(player, ConstNpc.BASE_MENU, "Bạn muốn hỏi chị?",
                    menu.toArray(new String[0]));
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (this.mapId == 5) {
                if (player.idMark.isBaseMenu()) {
                    handleBaseMenu(player, select);
                } else if (player.idMark.getIndexMenu() == ConstNpc.PHAO_BONG_VIP) {
                    switch (select) {
                        case 0:
                            Service.gI().showListTop(player, Manager.Topsukien);
                            break;
                        case 1:
                            Service.gI().sendThongBao(player, "Bạn có " + player.point_sukien + " điểm Hộp quà thiếu nhi.");
                            break;
                    }
                } else if (player.idMark.getIndexMenu() == ConstNpc.PHAO_BONG) {
                    switch (select) {
                        case 0:
                            Service.gI().showListTop(player, Manager.Topsukien1);
                            break;
                        case 1:
                            Service.gI().sendThongBao(player, "Bạn có " + player.point_sukien1 + " điểm Nước mía.");
                            break;
                    }
                } else if (player.idMark.getIndexMenu() == ConstNpc.GOKU_DAY) {
                    switch (select) {
                        case 0:
                            Service.gI().showListTop(player, Manager.Topsukien2);
                            break;
                        case 1:
                            Service.gI().sendThongBao(player, "Bạn có " + player.point_sukien2 + " điểm Kem trái cây.");
                            break;
                    }
                } else if (player.idMark.getIndexMenu() == MENU_DOI_QUA_MUA_HE
                        && select >= 0 && select < NGUYEN_LIEU_MUA_HE.length) {
                    openFormDoiQuaMuaHe(player, select);
                } else if (player.idMark.getIndexMenu() == MENU_XOA_VAT_PHAM_HSD
                        && select == 0) {
                    xoaVatPhamHanSuDungTrongHanhTrang(player);
                }
            }
        }
    }

    private void handleBaseMenu(Player player, int select) {
        EventControlService eventControl = EventControlService.gI();
        int menuIndex = 0;

        if (eventControl.isEnabled(EventControlService.SUMMER)) {
            if (select == menuIndex) {
                showSummerEventInfo(player);
                return;
            }
            menuIndex++;
            if (select == menuIndex) {
                ShopService.gI().opendShop(player, "SHOP_CHI_CHI", false);
                return;
            }
            menuIndex++;
            if (select == menuIndex) {
                openMenuDoiQuaMuaHe(player);
                return;
            }
            menuIndex++;
            if (select == menuIndex) {
                showSummerEventScore(player);
                return;
            }
            menuIndex++;
        }

        if (eventControl.isEnabled(CHILDRENS_DAY)) {
            if (select == menuIndex) {
                createOtherMenu(player, ConstNpc.PHAO_BONG_VIP,
                        "Sự kiện đua top Hộp quà thiếu nhi nhận quà khủng\n Kết thúc và trao giải sau (....)\nHạn chót nhận giải: (15 ngày nữa)\nĐến gặp ChiChi để nhận giải nhé\nChi tiết xem tại diễn đàn, Fanpage",
                        "Top 100\nHộp quà\nthiếu nhi\n2025", "Xem điểm", "Đóng");
                return;
            }
            menuIndex++;
        }

        if (eventControl.isEnabled(SUGARCANE)) {
            if (select == menuIndex) {
                createOtherMenu(player, ConstNpc.PHAO_BONG,
                        "Sự kiện đua top Nước mía nhận quà khủng\n Kết thúc và trao giải sau (....)\nHạn chót nhận giải: (15 ngày nữa)\nĐến gặp ChiChi để nhận giải nhé\nChi tiết xem tại diễn đàn, Fanpage",
                        "Top 100\nNước mía", "Xem điểm", "Đóng");
                return;
            }
            menuIndex++;
        }

        if (eventControl.isEnabled(FRUIT_ICE_CREAM)) {
            if (select == menuIndex) {
                createOtherMenu(player, ConstNpc.GOKU_DAY,
                        "Sự kiện đua top Kem trái cây nhận quà khủng\n Kết thúc và trao giải sau (....)\nHạn chót nhận giải: (15 ngày nữa)\nĐến gặp ChiChi để nhận giải nhé\nChi tiết xem tại diễn đàn, Fanpage",
                        "Top 100\nKem trái cây", "Xem điểm", "Đóng");
                return;
            }
            menuIndex++;
        }

        if (isOldChiChiEventEnabled(eventControl)) {
            if (select == menuIndex) {
                ShopService.gI().opendShop(player, "SHOP_CHI_CHI", false);
                return;
            }
            menuIndex++;
        }

        if (select == menuIndex) {
            openMenuXoaVatPhamHanSuDung(player);
        }
    }

    private void showSummerEventInfo(Player player) {
        createOtherMenu(player, ConstNpc.IGNORE_MENU,
                "SỰ KIỆN HÈ\n"
                + "Mua Quần đi biển đúng hành tinh và Trái dừa tại Cửa hàng hè của Chi Chi.\n"
                + "Mặc Quần đi biển, tháo áo và cải trang rồi đánh quái tại map có nước để tìm Vỏ ốc, Vỏ sò, Con cua và Sao biển.\n"
                + "Trái dừa hoặc Cỏ bốn lá tăng 70% tỷ lệ rơi nguyên liệu trong thời gian sử dụng.",
                "Đóng");
    }

    private void showSummerEventScore(Player player) {
        long weeklyScore = DailyRankingService.getCurrentSummerEventScore(player);
        createOtherMenu(player, ConstNpc.IGNORE_MENU,
                formatSummerEventScore(weeklyScore, player.point_summer_cards),
                "Đóng");
    }

    static String formatSummerEventScore(long weeklyScore, long totalScore) {
        return "ĐIỂM SỰ KIỆN HÈ\n"
                + "Điểm tuần này: " + Util.formatNumber(weeklyScore) + " điểm\n"
                + "Tổng điểm tích lũy: " + Util.formatNumber(totalScore) + " điểm";
    }

    private void openMenuDoiQuaMuaHe(Player player) {
        StringBuilder thongTin = new StringBuilder("Mỗi loại đủ x99 có thể đổi x10 mảnh thẻ ngẫu nhiên:\n"
                + "Rồng Thần Namek: 35% | Oozaru: 35%\n"
                + "Oozarun 1: 15% | Oozarun 2: 15%\n");
        for (int i = 0; i < NGUYEN_LIEU_MUA_HE.length; i++) {
            thongTin.append(TEN_NGUYEN_LIEU_MUA_HE[i])
                    .append(": ")
                    .append(getItemQuantity(player, NGUYEN_LIEU_MUA_HE[i]))
                    .append(" (đổi tối đa ")
                    .append(getSoLanDoiToiDa(player, NGUYEN_LIEU_MUA_HE[i]))
                    .append(" lần)")
                    .append("\n");
        }
        createOtherMenu(player, MENU_DOI_QUA_MUA_HE, thongTin.toString(),
                "Đổi x99\nVỏ ốc",
                "Đổi x99\nVỏ sò",
                "Đổi x99\nCon cua",
                "Đổi x99\nSao biển",
                "Đóng");
    }

    private void openFormDoiQuaMuaHe(Player player, int select) {
        int itemId = NGUYEN_LIEU_MUA_HE[select];
        int soLanToiDa = getSoLanDoiToiDa(player, itemId);
        if (soLanToiDa < 1) {
            Service.gI().sendThongBao(player,
                    "Bạn cần ít nhất x99 " + TEN_NGUYEN_LIEU_MUA_HE[select] + " để đổi quà.");
            return;
        }
        Input.gI().createFormDoiTheMuaHe(player, select, TEN_NGUYEN_LIEU_MUA_HE[select], soLanToiDa);
    }

    public static void doiQuaMuaHe(Player player, int select, int soLanDoi) {
        synchronized (player) {
            if (select < 0 || select >= NGUYEN_LIEU_MUA_HE.length) {
                return;
            }
            if (!EventControlService.gI().isEnabled(EventControlService.SUMMER)) {
                Service.gI().sendThongBao(player, "Sự kiện hè hiện đang tắt.");
                return;
            }
            int itemId = NGUYEN_LIEU_MUA_HE[select];
            String itemName = TEN_NGUYEN_LIEU_MUA_HE[select];
            int soLanToiDa = getSoLanDoiToiDa(player, itemId);

            if (soLanDoi < 1 || soLanDoi > soLanToiDa || soLanDoi > SO_LAN_DOI_TOI_DA_MOI_LAN) {
                Service.gI().sendThongBao(player,
                        "Số lần đổi không hợp lệ. Bạn có thể đổi từ 1 đến "
                        + Math.min(soLanToiDa, SO_LAN_DOI_TOI_DA_MOI_LAN) + " lần.");
                return;
            }

            int[] soLuongThe = new int[THE_SUU_TAP_MUA_HE.length];
            for (int i = 0; i < soLanDoi; i++) {
                soLuongThe[randomChiSoTheSuuTapMuaHe()] += SO_LUONG_THE_NHAN_DUOC;
            }

            int soNguyenLieuCan = soLanDoi * SO_LUONG_NGUYEN_LIEU;
            int soOTrongSauKhiTru = demSoOTrongSauKhiTruNguyenLieu(player, itemId, soNguyenLieuCan);
            int soOCanChoThe = demSoOCanChoThe(player, soLuongThe);
            if (soOTrongSauKhiTru < soOCanChoThe) {
                Service.gI().sendThongBao(player,
                        "Hành trang không đủ chỗ cho " + soOCanChoThe
                        + " loại thẻ có thể nhận. Hãy chừa thêm ô trống rồi thử lại.");
                return;
            }

            truItemTrongHanhTrang(player, itemId, soNguyenLieuCan);
            int[] soLuongDaThem = new int[THE_SUU_TAP_MUA_HE.length];
            for (int i = 0; i < THE_SUU_TAP_MUA_HE.length; i++) {
                if (soLuongThe[i] <= 0) {
                    continue;
                }
                Item qua = ItemService.gI().createNewItem((short) THE_SUU_TAP_MUA_HE[i], soLuongThe[i]);
                if (qua == null || qua.template == null || !InventoryService.gI().addItemBag(player, qua)) {
                    for (int j = 0; j < soLuongDaThem.length; j++) {
                        if (soLuongDaThem[j] > 0) {
                            truItemTrongHanhTrang(player, THE_SUU_TAP_MUA_HE[j], soLuongDaThem[j]);
                        }
                    }
                    Item hoanTra = ItemService.gI().createNewItem((short) itemId, soNguyenLieuCan);
                    InventoryService.gI().addItemBag(player, hoanTra);
                    InventoryService.gI().sendItemBags(player);
                    Service.gI().sendThongBao(player,
                            "Không thể thêm đủ thẻ vào hành trang, nguyên liệu đã được hoàn trả.");
                    return;
                }
                soLuongDaThem[i] = soLuongThe[i];
            }

            long diemSuKien = (long) soLanDoi * SO_LUONG_THE_NHAN_DUOC;
            player.point_summer_cards += diemSuKien;
            DailyRankingService.recordSummerEventPoints(player, diemSuKien);
            InventoryService.gI().sendItemBags(player);
            StringBuilder ketQua = new StringBuilder("Đổi thành công ")
                    .append(soLanDoi).append(" lần, đã dùng x")
                    .append(soNguyenLieuCan).append(" ").append(itemName).append(".\nNhận được:");
            for (int i = 0; i < soLuongThe.length; i++) {
                if (soLuongThe[i] > 0) {
                    ketQua.append("\n- x").append(soLuongThe[i]).append(" ")
                            .append(TEN_THE_SUU_TAP_MUA_HE[i]);
                }
            }
            Service.gI().sendThongBao(player, ketQua.toString());
        }
    }

    private static int randomChiSoTheSuuTapMuaHe() {
        int random = Util.nextInt(1, 100);
        int tongTyLe = 0;
        for (int i = 0; i < THE_SUU_TAP_MUA_HE.length; i++) {
            tongTyLe += TY_LE_THE_SUU_TAP[i];
            if (random <= tongTyLe) {
                return i;
            }
        }
        return THE_SUU_TAP_MUA_HE.length - 1;
    }

    private static int getSoLanDoiToiDa(Player player, int itemId) {
        return Math.min(getItemQuantity(player, itemId) / SO_LUONG_NGUYEN_LIEU,
                SO_LAN_DOI_TOI_DA_MOI_LAN);
    }

    private static int getItemQuantity(Player player, int itemId) {
        long total = 0;
        for (Item item : player.inventory.itemsBag) {
            if (item != null && item.template != null && item.template.id == itemId && item.quantity > 0) {
                total += item.quantity;
                if (total >= Integer.MAX_VALUE) {
                    return Integer.MAX_VALUE;
                }
            }
        }
        return (int) total;
    }

    private static int demSoOTrongSauKhiTruNguyenLieu(Player player, int itemId, int quantity) {
        int soOTrong = InventoryService.gI().getCountEmptyBag(player);
        int conLaiCanTru = quantity;
        for (Item item : player.inventory.itemsBag) {
            if (item != null && item.template != null && item.template.id == itemId && item.quantity > 0) {
                if (item.quantity <= conLaiCanTru) {
                    soOTrong++;
                    conLaiCanTru -= item.quantity;
                    if (conLaiCanTru <= 0) {
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        return soOTrong;
    }

    private static int demSoOCanChoThe(Player player, int[] soLuongThe) {
        int soOCan = 0;
        for (int i = 0; i < THE_SUU_TAP_MUA_HE.length; i++) {
            if (soLuongThe[i] <= 0) {
                continue;
            }
            Item dangCo = InventoryService.gI().findItemBag(player, THE_SUU_TAP_MUA_HE[i]);
            if (dangCo == null || dangCo.quantity > 99999 - soLuongThe[i]) {
                soOCan++;
            }
        }
        return soOCan;
    }

    private static void truItemTrongHanhTrang(Player player, int itemId, int quantity) {
        int conLai = quantity;
        List<Item> cacStack = new ArrayList<>();
        for (Item item : player.inventory.itemsBag) {
            if (item != null && item.template != null && item.template.id == itemId && item.quantity > 0) {
                cacStack.add(item);
            }
        }
        for (Item item : cacStack) {
            if (conLai <= 0) {
                break;
            }
            int soLuongTru = Math.min(conLai, item.quantity);
            InventoryService.gI().subQuantityItemsBag(player, item, soLuongTru);
            conLai -= soLuongTru;
        }
    }

    private void openMenuXoaVatPhamHanSuDung(Player player) {
        long soLuong = demVatPhamHanSuDungTrongHanhTrang(player);
        createOtherMenu(player, MENU_XOA_VAT_PHAM_HSD,
                "Bạn có chắc muốn vứt hết vật phẩm hạn sử dụng không?\n"
                + "Số vật phẩm sẽ vứt: " + soLuong + "\n"
                + "Vật phẩm đang mặc sẽ không bị ảnh hưởng.",
                "Có",
                "Không");
    }

    /**
     * Chỉ quét itemsBag. Khóa theo người chơi và quét lại khi xác nhận để gói
     * tin lặp/bấm nhanh không thể tác động hai lần hoặc xóa nhầm đồ vừa mặc.
     */
    private void xoaVatPhamHanSuDungTrongHanhTrang(Player player) {
        synchronized (player) {
            List<Item> itemsCanXoa = new ArrayList<>();
            long soLuongDaXoa = 0;

            for (Item item : player.inventory.itemsBag) {
                if (laVatPhamHanSuDung(item)) {
                    itemsCanXoa.add(item);
                    soLuongDaXoa += Math.max(1, item.quantity);
                }
            }

            if (itemsCanXoa.isEmpty()) {
                Service.gI().sendThongBao(player, "Hành trang không có vật phẩm hạn sử dụng để vứt.");
                return;
            }

            for (Item item : itemsCanXoa) {
                InventoryService.gI().removeItemBag(player, item);
            }
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player,
                    "Đã vứt " + soLuongDaXoa + " vật phẩm hạn sử dụng trong hành trang.");
        }
    }

    private long demVatPhamHanSuDungTrongHanhTrang(Player player) {
        long total = 0;
        for (Item item : player.inventory.itemsBag) {
            if (laVatPhamHanSuDung(item)) {
                total += Math.max(1, item.quantity);
            }
        }
        return total;
    }

    private boolean laVatPhamHanSuDung(Item item) {
        if (item == null || item.template == null || item.itemOptions == null) {
            return false;
        }
        for (Item.ItemOption option : item.itemOptions) {
            if (option != null && option.optionTemplate != null
                    && option.optionTemplate.id == 93 && option.param > 0) {
                return true;
            }
        }
        return false;
    }

    private boolean isOldChiChiEventEnabled(EventControlService eventControl) {
        return eventControl.isEnabled(CHILDRENS_DAY)
                || eventControl.isEnabled(SUGARCANE)
                || eventControl.isEnabled(FRUIT_ICE_CREAM);
    }
}
