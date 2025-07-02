import io.socket.emitter.Emitter;
import jsclub.codefest.sdk.Hero;

import java.io.IOException;

public class Main {
    private static final String SERVER_URL = "https://cf25-server.jsclub.dev";
    private static final String GAME_ID = "184039";
    private static final String PLAYER_NAME = "CF25_7_Bot_3";
    private static final String SECRET_KEY = "sk-4pVp8RosQY6ZRJoQZD2LNw:-bkx-xv_9tsL3_q40y7v7lT5dGLreyU0SC2d_20QqaWXc_LsuS3QKRjNlqUSDausdIRD6D4efMpZXIlgUl1vGA";

    public static void main(String[] args) throws IOException {
        Hero hero = new Hero(GAME_ID, PLAYER_NAME, SECRET_KEY);
        Emitter.Listener botController = new ThreatAwareBot(hero);
        hero.setOnMapUpdate(botController);
        hero.start(SERVER_URL);
    }
}
