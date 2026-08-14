package nro.models.player;

import java.lang.reflect.Method;
import nro.models.network.MySession;

public final class PlayerSessionAccessorTest {

    private PlayerSessionAccessorTest() {
    }

    public static void main(String[] args) throws Exception {
        Method getter = Player.class.getMethod("getSession");
        if (getter.getReturnType() != MySession.class) {
            throw new AssertionError("Player.getSession must return MySession");
        }

        Method setter = Player.class.getMethod("setSession", MySession.class);
        if (setter.getReturnType() != void.class) {
            throw new AssertionError("Player.setSession must return void");
        }

        System.out.println("PLAYER_SESSION_ACCESSORS_OK");
    }
}