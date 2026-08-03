package nro.models.player;

import lombok.Getter;
import lombok.Setter;
import nro.models.player.Player;

/**
 * @author By Mr Blue
 */
@Setter
@Getter
public class PlayerEvent {
    private int eventPoint;
    private Player player;
    public int luotNhanNgocMienPhi = 1;
    public int luotNhanCapsuleBang = 1;

    public PlayerEvent(Player player) {
        this.player = player;
    }
    
    public synchronized void addEventPoint(int num) {
        if (num <= 0) {
            return;
        }
        eventPoint = (int) Math.min(Integer.MAX_VALUE, (long) eventPoint + num);
    }

    public synchronized void subEventPoint(int num) {
        if (num > 0) {
            eventPoint = Math.max(0, eventPoint - num);
        }
    }

    public synchronized boolean trySpendEventPoint(int num) {
        if (num <= 0 || eventPoint < num) {
            return false;
        }
        eventPoint -= num;
        return true;
    }

    public void update() {
       
    }

}
