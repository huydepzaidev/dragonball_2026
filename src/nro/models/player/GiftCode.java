package nro.models.player;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author By Mr Blue
 * 
 */

public class GiftCode {

    private static final int MAX_RELOADS_PER_LOGIN = 2;

    public List<String> rewards;
    private int reloadCount;

    public GiftCode() {
        this.rewards = new ArrayList<>();
    }

    public void add(String code) {
        this.rewards.add(code);
    }

    public boolean isUsedGiftCode(String code) {
        return rewards.contains(code);
    }

    /**
     * Mỗi Player được tạo mới khi đăng nhập, vì vậy bộ đếm này tự đặt lại ở
     * lần đăng nhập tiếp theo và không cần lưu xuống database.
     */
    public synchronized boolean tryAcquireReload() {
        if (reloadCount >= MAX_RELOADS_PER_LOGIN) {
            return false;
        }
        reloadCount++;
        return true;
    }

    public void dispose() {
        if (rewards != null) {
            rewards.clear();
            rewards = null;
        }
    }

}
