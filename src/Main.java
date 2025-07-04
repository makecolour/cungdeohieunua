import io.socket.emitter.Emitter;
import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.players.Player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;

public class Main {
    // Update these values with your game information
    private static final String SERVER_URL = "https://cf25-server.jsclub.dev";
    private static final String GAME_ID = "YOUR_GAME_ID";
    private static final String PLAYER_NAME = "YOUR_PLAYER_NAME";
    private static final String SECRET_KEY = "YOUR_SECRET_KEY";

    private static Hero hero;
    private static boolean gameRunning = true;

    public static void main(String[] args) throws IOException {
        // Initialize the hero
        hero = new Hero(GAME_ID, PLAYER_NAME, SECRET_KEY);

        // Set up map update listener
        Emitter.Listener onMapUpdate = new MapUpdateListener(hero);
        hero.setOnMapUpdate(onMapUpdate);

        // Connect to the server
        hero.start(SERVER_URL);

        System.out.println("Connected to game server. Starting keyboard control.");
        System.out.println("Commands:");
        System.out.println("w - Move up");
        System.out.println("a - Move left");
        System.out.println("s - Move down");
        System.out.println("d - Move right");
        System.out.println("p - Pick up item");
        System.out.println("f - Fire weapon");
        System.out.println("h - Use healing item");
        System.out.println("q - Quit");

        // Start keyboard input listener in a separate thread
        Thread keyboardThread = new Thread(() -> handleKeyboardInput());
        keyboardThread.start();
    }

    private static void handleKeyboardInput() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (gameRunning) {
                String input = scanner.nextLine().trim().toLowerCase();

                try {
                    switch (input) {
                        case "w":
                            System.out.println("Moving up");
                            hero.move("UP");
                            break;
                        case "a":
                            System.out.println("Moving left");
                            hero.move("LEFT");
                            break;
                        case "s":
                            System.out.println("Moving down");
                            hero.move("DOWN");
                            break;
                        case "d":
                            System.out.println("Moving right");
                            hero.move("RIGHT");
                            break;
                        case "p":
                            System.out.println("Picking up item");
                            hero.pickupItem();
                            break;
                        case "f":
                            System.out.println("Firing weapon");
                            // You may need to specify direction or target
                            hero.shoot("RIGHT"); // Default direction, change as needed
                            break;
                        case "h":
                            System.out.println("Using healing item");
                            hero.useItem();
                            break;
                        case "q":
                            System.out.println("Quitting game");
                            gameRunning = false;
                            hero.disconnect();
                            System.exit(0);
                            break;
                        default:
                            System.out.println("Unknown command: " + input);
                            break;
                    }
                } catch (IOException e) {
                    System.err.println("Error executing command: " + e.getMessage());
                }
            }
        }
    }
}

class MapUpdateListener implements Emitter.Listener {
    private final Hero hero;

    public MapUpdateListener(Hero hero) {
        this.hero = hero;
    }

    @Override
    public void call(Object... args) {
        try {
            if (args == null || args.length == 0) return;

            GameMap gameMap = hero.getGameMap();
            gameMap.updateOnUpdateMap(args[0]);
            Player player = gameMap.getCurrentPlayer();

            if (player == null || player.getHealth() == 0) {
                System.out.println("Player is dead or data is not available.");
                return;
            }

            // Display current game state
            System.out.println("\nCurrent Player Position: (" + player.getX() + ", " + player.getY() + ")");
            System.out.println("Health: " + player.getHealth());
            System.out.println("Current weapon: " + (hero.getInventory().getGun() != null ?
                              hero.getInventory().getGun().getName() : "None"));
            System.out.println("Healing items: " + hero.getInventory().getHealingItems().size());

            // Show available commands
            System.out.println("\nEnter command (w/a/s/d/p/f/h/q): ");

        } catch (Exception e) {
            System.err.println("Error in map update: " + e.getMessage());
        }
    }
}