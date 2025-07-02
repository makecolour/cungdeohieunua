import io.socket.emitter.Emitter;
import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.Inventory;
import jsclub.codefest.sdk.model.healing_items.HealingItem;
import jsclub.codefest.sdk.model.npcs.Ally;
import jsclub.codefest.sdk.model.npcs.Enemy;
import jsclub.codefest.sdk.model.obstacles.Obstacle;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.weapon.Weapon;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ThreatAwareBot implements Emitter.Listener {
    private final Hero hero;
    private ActionPlan currentActionPlan;
    private long lastActionTime = 0;
    private final long ACTION_TIMEOUT = 3000; // 3 seconds timeout
    private String lastMovement = "";
    private int stuckCounter = 0;
    private Player lastPlayerPosition;

    // NEW: Enemy tracking system
    private Map<String, EnemyTracker> enemyTrackers = new HashMap<>();
    private Set<Node> dangerZones = new HashSet<>();
    private long lastDangerZoneUpdate = 0;
    private final long DANGER_ZONE_UPDATE_INTERVAL = 2000; // Update every 2 seconds

    public ThreatAwareBot(Hero hero) {
        this.hero = hero;
        this.currentActionPlan = null;
    }

    @Override
    public void call(Object... args) {
        try {
            if (args == null || args.length == 0) return;

            GameMap gameMap = hero.getGameMap();
            gameMap.updateOnUpdateMap(args[0]);

            Player currentPlayer = gameMap.getCurrentPlayer();
            if (currentPlayer == null || currentPlayer.getHealth() <= 0) {
                System.out.println("Player is dead or unavailable");
                return;
            }

            // Enhanced game state analysis
            GameState gameState = analyzeGameState(gameMap, currentPlayer);

            // Print detailed debug info
            System.out.println("=== GAME STATE DEBUG ===");
            System.out.println("Health: " + currentPlayer.getHealth() + "/100");
            System.out.println("Position: (" + currentPlayer.x + ", " + currentPlayer.y + ")");
            System.out.println("Player threats: " + gameState.immediateThreats.size());
            System.out.println("NPC threats: " + gameState.dangerousNPCs.size());
            System.out.println("Has weapon: " + gameState.hasWeapon);
            System.out.println("Nearby weapons: " + gameState.nearbyWeapons.size());
            System.out.println("Nearby healing: " + gameState.nearbyHealingItems.size());
            System.out.println("Dragon eggs: " + gameState.nearbyDragonEggs.size());

            // Update enemy trackers and danger zones
            updateEnemyTrackers(gameState);
            updateDangerZones(gameState);

            // Check if we need a new plan
            boolean needNewPlan = shouldCreateNewPlan(gameState);

            if (needNewPlan) {
                currentActionPlan = createActionPlan(gameState);
                lastActionTime = System.currentTimeMillis();
                System.out.println("NEW PLAN: " + currentActionPlan.action + " (Priority: " + currentActionPlan.priority + ")");
            }

            // Execute current plan
            if (currentActionPlan != null) {
                executeAction(currentActionPlan, gameState);
            }

            // Track movement for stuck detection
            trackPlayerMovement(currentPlayer);

        } catch (Exception e) {
            System.err.println("Bot error: " + e.getMessage());
            e.printStackTrace();

            try {
                emergencyMove();
            } catch (Exception ignored) {}
        }
    }

    private GameState analyzeGameState(GameMap gameMap, Player currentPlayer) {
        GameState state = new GameState();
        state.currentPlayer = currentPlayer;
        state.gameMap = gameMap;

        // Analyze threats
        state.nearbyEnemies = findNearbyEnemies(gameMap, currentPlayer);
        state.nearbyPlayers = findNearbyPlayers(gameMap, currentPlayer);
        state.immediateThreats = identifyImmediateThreats(state);
        state.dangerousNPCs = findDangerousNPCs(gameMap, currentPlayer);

        // Analyze opportunities
        state.nearbyWeapons = findNearbyWeapons(gameMap, currentPlayer);
        state.nearbyHealingItems = findNearbyHealingItems(gameMap, currentPlayer);
        state.nearbyChests = findNearbyChests(gameMap, currentPlayer);
        state.nearbyDragonEggs = findNearbyDragonEggs(gameMap, currentPlayer);
        state.nearbyAllies = findNearbyAllies(gameMap, currentPlayer);
        state.nearbyBushes = findNearbyBushes(gameMap, currentPlayer);

        // Safety analysis
        state.isInSafeZone = isInSafeZone(gameMap, currentPlayer);
        state.safeZoneDistance = calculateSafeZoneDistance(gameMap, currentPlayer);

        // Enhanced inventory analysis
        Inventory inventory = hero.getInventory();
        state.hasWeapon = hasAnyWeapon(inventory);
        state.needsHealing = currentPlayer.getHealth() < 60;
        state.criticalHealth = currentPlayer.getHealth() < 30;
        state.lowHealth = currentPlayer.getHealth() < 50;

        return state;
    }

    private boolean hasAnyWeapon(Inventory inventory) {
        try {
            return inventory.getGun() != null ||
                   inventory.getMelee() != null ||
                   inventory.getThrowable() != null ||
                   inventory.getSpecial() != null;
        } catch (Exception e) {
            System.err.println("Error checking weapons: " + e.getMessage());
            return false;
        }
    }

    private boolean shouldCreateNewPlan(GameState gameState) {
        if (currentActionPlan == null) return true;

        // Check for immediate threats
        if (!gameState.immediateThreats.isEmpty() || !gameState.dangerousNPCs.isEmpty()) {
            if (currentActionPlan.action != ActionType.FLEE_FROM_THREAT &&
                currentActionPlan.action != ActionType.FIGHT_BACK &&
                currentActionPlan.action != ActionType.HIDE_IN_BUSH) {
                return true;
            }
        }

        // Check timeout
        if (System.currentTimeMillis() - lastActionTime > ACTION_TIMEOUT) {
            return true;
        }

        // Check critical health
        if (gameState.criticalHealth && currentActionPlan.action != ActionType.EMERGENCY_HEAL) {
            return true;
        }

        // Check if stuck
        if (stuckCounter > 2) {
            stuckCounter = 0;
            return true;
        }

        return false;
    }

    private ActionPlan createActionPlan(GameState gameState) {
        List<ActionPlan> possibleActions = new ArrayList<>();

        // Priority 1: Emergency healing if critical health (1000)
        if (gameState.criticalHealth) {
            if (!gameState.nearbyHealingItems.isEmpty()) {
                HealingItem healingItem = findBestHealingItem(gameState.nearbyHealingItems);
                possibleActions.add(new ActionPlan(ActionType.EMERGENCY_HEAL, 1000, healingItem));
            }

            if (!gameState.nearbyAllies.isEmpty()) {
                Ally ally = gameState.nearbyAllies.get(0);
                possibleActions.add(new ActionPlan(ActionType.HEAL_FROM_ALLY, 950, ally));
            }
        }

        // Priority 2: ALWAYS flee from dangerous NPCs (940)
        if (!gameState.dangerousNPCs.isEmpty()) {
            Node safeLocation = findSafeLocationFromNPCs(gameState);
            if (safeLocation != null) {
                possibleActions.add(new ActionPlan(ActionType.FLEE_FROM_THREAT, 940, safeLocation));
            }
        }

        // Priority 3: ALWAYS flee from ALL player threats (900)
        if (!gameState.nearbyPlayers.isEmpty()) {
            Node safeLocation = findSafeLocation(gameState);
            if (safeLocation != null) {
                possibleActions.add(new ActionPlan(ActionType.FLEE_FROM_THREAT, 900, safeLocation));
            }

            if (!gameState.nearbyBushes.isEmpty()) {
                Obstacle bush = gameState.nearbyBushes.get(0);
                possibleActions.add(new ActionPlan(ActionType.HIDE_IN_BUSH, 850, bush));
            }
        }

        // Priority 4: Get to safe zone if outside (800)
        if (!gameState.isInSafeZone && gameState.safeZoneDistance > 0) {
            Node safeZone = findNearestSafeZone(gameState);
            if (safeZone != null) {
                possibleActions.add(new ActionPlan(ActionType.MOVE_TO_SAFE_ZONE, 800, safeZone));
            }
        }

        // Priority 5: Get NORMAL weapons ONLY if no threats nearby (600)
        if (!gameState.hasWeapon &&
            !gameState.nearbyWeapons.isEmpty() &&
            gameState.nearbyPlayers.isEmpty() &&
            gameState.dangerousNPCs.isEmpty()) {
            Weapon weapon = findBestNormalWeapon(gameState.nearbyWeapons);
            if (weapon != null) {
                possibleActions.add(new ActionPlan(ActionType.COLLECT_WEAPON, 600, weapon));
            }
        }

        // Priority 6: Heal if needed and COMPLETELY safe (500)
        if (gameState.needsHealing &&
            gameState.nearbyPlayers.isEmpty() &&
            gameState.dangerousNPCs.isEmpty() &&
            gameState.nearbyEnemies.isEmpty()) {
            if (!gameState.nearbyHealingItems.isEmpty()) {
                HealingItem healingItem = findBestHealingItem(gameState.nearbyHealingItems);
                possibleActions.add(new ActionPlan(ActionType.HEAL, 500, healingItem));
            }

            if (!gameState.nearbyAllies.isEmpty()) {
                Ally ally = gameState.nearbyAllies.get(0);
                possibleActions.add(new ActionPlan(ActionType.HEAL_FROM_ALLY, 450, ally));
            }
        }

        // Priority 7: Hide in bush if ANY threats nearby (400)
        if ((!gameState.nearbyPlayers.isEmpty() || !gameState.dangerousNPCs.isEmpty() || !gameState.nearbyEnemies.isEmpty()) &&
            !gameState.nearbyBushes.isEmpty()) {
            Obstacle bush = gameState.nearbyBushes.get(0);
            possibleActions.add(new ActionPlan(ActionType.HIDE_IN_BUSH, 400, bush));
        }

        // Priority 8: Safe exploration (100)
        if (gameState.nearbyPlayers.isEmpty() &&
            gameState.dangerousNPCs.isEmpty() &&
            gameState.nearbyEnemies.isEmpty()) {
            possibleActions.add(new ActionPlan(ActionType.EXPLORE, 100, null));
        }

        // Priority 9: Emergency combat - ONLY if cornered with weapon and players nearby (75)
        if (gameState.hasWeapon &&
            !gameState.nearbyPlayers.isEmpty() &&
            possibleActions.isEmpty()) {
            Player nearestPlayer = gameState.nearbyPlayers.get(0);
            possibleActions.add(new ActionPlan(ActionType.FIGHT_BACK, 75, nearestPlayer));
        }

        // Priority 10: Emergency random movement if just enemies (no players) (60)
        if (!gameState.nearbyEnemies.isEmpty() &&
            gameState.nearbyPlayers.isEmpty() &&
            possibleActions.isEmpty()) {
            possibleActions.add(new ActionPlan(ActionType.EMERGENCY_FLEE, 60, null));
        }

        // Priority 11: Emergency hide if no other options (50)
        possibleActions.add(new ActionPlan(ActionType.HIDE_IN_BUSH, 50, null));

        return possibleActions.stream()
            .max(Comparator.comparingInt(a -> a.priority))
            .orElse(new ActionPlan(ActionType.EXPLORE, 25, null));
    }

    private void executeAction(ActionPlan plan, GameState gameState) throws IOException {
        System.out.println("EXECUTING: " + plan.action + " (Priority: " + plan.priority + ")");

        switch (plan.action) {
            case FLEE_FROM_THREAT:
                executeFleeFromThreat(plan, gameState);
                break;
            case MOVE_TO_SAFE_ZONE:
                executeMoveToSafeZone(plan, gameState);
                break;
            case FIGHT_BACK:
                executeFightBack(plan, gameState);
                break;
            case COLLECT_WEAPON:
                executeCollectWeapon(plan, gameState);
                break;
            case EMERGENCY_HEAL:
            case HEAL:
                executeHeal(plan, gameState);
                break;
            case LOOT_CHEST:
                executeLootChest(plan, gameState);
                break;
            case LOOT_DRAGON_EGG:
                executeLootDragonEgg(plan, gameState);
                break;
            case HIDE_IN_BUSH:
                executeHideInBush(plan, gameState);
                break;
            case HEAL_FROM_ALLY:
                executeHealFromAlly(plan, gameState);
                break;
            case ATTACK_PLAYER:
                executeAttackPlayer(plan, gameState);
                break;
            case EMERGENCY_FLEE:
                executeEmergencyFlee(plan, gameState);
                break;
            default:
                executeExplore(gameState);
        }
    }

    private void executeFleeFromThreat(ActionPlan plan, GameState gameState) throws IOException {
        if (plan.target instanceof Node) {
            Node safeLocation = (Node) plan.target;
            moveToLocation(safeLocation, gameState, "fleeing to safety");
        } else {
            // Emergency escape from nearest threat
            if (!gameState.immediateThreats.isEmpty()) {
                Player nearestThreat = gameState.immediateThreats.get(0);
                String escapePath = getEscapeDirection(gameState.currentPlayer, nearestThreat);
                if (escapePath != null) {
                    hero.move(escapePath);
                    System.out.println("Emergency escape from player: " + escapePath);
                }
            } else if (!gameState.dangerousNPCs.isEmpty()) {
                Enemy nearestNPC = gameState.dangerousNPCs.get(0);
                String escapePath = getEscapeDirectionFromNPC(gameState.currentPlayer, nearestNPC);
                if (escapePath != null) {
                    hero.move(escapePath);
                    System.out.println("Emergency escape from NPC: " + escapePath);
                }
            }
        }
    }

    private void executeMoveToSafeZone(ActionPlan plan, GameState gameState) throws IOException {
        Node safeZone = (Node) plan.target;
        moveToLocation(safeZone, gameState, "moving to safe zone");
    }

    // FIXED: Use correct Hero API methods based on SDK documentation
    private void executeFightBack(ActionPlan plan, GameState gameState) throws IOException {
        Player target = (Player) plan.target;
        Inventory inventory = hero.getInventory();

        if (hasAnyWeapon(inventory)) {
            double distance = PathUtils.distance(gameState.currentPlayer, target);
            String direction = getDirectionToTarget(gameState.currentPlayer, target);
            
            if (distance <= 3) {
                // Close range - use melee attack with direction
                try {
                    hero.attack(direction);
                    System.out.println("Melee attacking player in direction: " + direction);
                    currentActionPlan = null;
                } catch (Exception e) {
                    System.err.println("Melee attack failed: " + e.getMessage());
                    moveToLocation(target, gameState, "moving to attack range");
                }
            } else if (distance <= 8) {
                // Medium range - try ranged attack
                try {
                    if (inventory.getGun() != null) {
                        hero.shoot(direction);
                        System.out.println("Shooting at player in direction: " + direction);
                    } else if (inventory.getSpecial() != null) {
                        hero.useSpecial(direction);
                        System.out.println("Using special weapon in direction: " + direction);
                    } else if (inventory.getThrowable() != null) {
                        int throwDistance = Math.max(1, Math.min(8, (int)distance));
                        hero.throwItem(direction, throwDistance);
                        System.out.println("Throwing item in direction: " + direction + " distance: " + throwDistance);
                    } else {
                        hero.attack(direction);
                        System.out.println("Melee attacking in direction: " + direction);
                    }
                    currentActionPlan = null;
                } catch (Exception e) {
                    System.err.println("Ranged attack failed: " + e.getMessage());
                    moveToLocation(target, gameState, "moving closer to attack");
                }
            } else {
                // Too far - move closer
                moveToLocation(target, gameState, "moving to attack range");
            }
        }
    }

    // FIXED: Use correct Hero API for attack with direction
    private void executeAttackPlayer(ActionPlan plan, GameState gameState) throws IOException {
        Player target = (Player) plan.target;
        double distance = PathUtils.distance(gameState.currentPlayer, target);
        
        if (distance <= 8) {
            try {
                String direction = getDirectionToTarget(gameState.currentPlayer, target);
                
                Inventory inventory = hero.getInventory();
                if (inventory.getGun() != null && distance >= 2) {
                    hero.shoot(direction);
                    System.out.println("Shooting at player in direction: " + direction);
                } else if (inventory.getSpecial() != null) {
                    hero.useSpecial(direction);
                    System.out.println("Using special weapon in direction: " + direction);
                } else if (inventory.getThrowable() != null && distance >= 3) {
                    int throwDistance = Math.max(1, Math.min(8, (int)distance));
                    hero.throwItem(direction, throwDistance);
                    System.out.println("Throwing item in direction: " + direction);
                } else {
                    hero.attack(direction);
                    System.out.println("Attacking player in direction: " + direction);
                }
                currentActionPlan = null;
            } catch (Exception e) {
                System.err.println("Attack failed: " + e.getMessage());
                moveToLocation(target, gameState, "moving to attack player");
            }
        } else {
            moveToLocation(target, gameState, "moving to attack player");
        }
    }

    // FIXED: Don't attack chests - just move to them to auto-collect
    private void executeLootChest(ActionPlan plan, GameState gameState) throws IOException {
        Obstacle chest = (Obstacle) plan.target;
        double distance = PathUtils.distance(gameState.currentPlayer, chest);
        
        if (distance < 1.5) {
            // Just being near a chest should auto-collect it, no need to attack
            System.out.println("Near chest - auto-collecting loot");
            currentActionPlan = null;
        } else {
            moveToLocation(chest, gameState, "moving to chest");
        }
    }

    // FIXED: Don't attack dragon eggs - just move to them to auto-collect
    private void executeLootDragonEgg(ActionPlan plan, GameState gameState) throws IOException {
        Obstacle dragonEgg = (Obstacle) plan.target;
        double distance = PathUtils.distance(gameState.currentPlayer, dragonEgg);
        
        if (distance < 1.5) {
            // Just being near a dragon egg should auto-collect it, no need to attack
            System.out.println("Near DRAGON EGG - auto-collecting premium loot!");
            currentActionPlan = null;
        } else {
            moveToLocation(dragonEgg, gameState, "moving to DRAGON EGG");
        }
    }

    // NEW: Helper method to get direction to obstacle
    private String getDirectionToObstacle(Player from, Obstacle to) {
        int dx = to.x - from.x;
        int dy = to.y - from.y;

        if (Math.abs(dx) > Math.abs(dy)) {
            return dx > 0 ? "r" : "l";
        } else {
            return dy > 0 ? "d" : "u";
        }
    }

    private void executeHideInBush(ActionPlan plan, GameState gameState) throws IOException {
        Obstacle bush = (Obstacle) plan.target;
        double distance = PathUtils.distance(gameState.currentPlayer, bush);

        if (distance < 1.5) {
            System.out.println("Hiding in bush - staying put");
            currentActionPlan = null;
        } else {
            moveToLocation(bush, gameState, "moving to hide in bush");
        }
    }

    private void executeHealFromAlly(ActionPlan plan, GameState gameState) throws IOException {
        Ally ally = (Ally) plan.target;
        moveToLocation(ally, gameState, "moving to ally for healing");
    }

    private void executeCollectWeapon(ActionPlan plan, GameState gameState) throws IOException {
        Weapon weapon = (Weapon) plan.target;
        moveToLocation(weapon, gameState, "collecting weapon: " + weapon.getId());
    }

    private void executeHeal(ActionPlan plan, GameState gameState) throws IOException {
        HealingItem healingItem = (HealingItem) plan.target;
        double distance = PathUtils.distance(gameState.currentPlayer, healingItem);

        if (distance < 1.5) {
            try {
                hero.useItem(healingItem.getId());
                System.out.println("Using healing item: " + healingItem.getId());
                currentActionPlan = null;
            } catch (Exception e) {
                System.err.println("Failed to heal: " + e.getMessage());
                moveToLocation(healingItem, gameState, "moving to healing item");
            }
        } else {
            moveToLocation(healingItem, gameState, "moving to healing item");
        }
    }

    private void executeEmergencyFlee(ActionPlan plan, GameState gameState) throws IOException {
        // Random movement when only enemies (no players) are nearby
        String[] directions = {"u", "d", "l", "r"};
        String direction = directions[new Random().nextInt(directions.length)];

        try {
            hero.move(direction);
            System.out.println("Emergency flee from enemies: " + direction);
            currentActionPlan = null;
        } catch (Exception e) {
            System.err.println("Emergency flee failed: " + e.getMessage());
            // Try a different random direction
            direction = directions[new Random().nextInt(directions.length)];
            try {
                hero.move(direction);
                System.out.println("Backup emergency flee: " + direction);
            } catch (Exception ignored) {}
        }
    }

    private void executeExplore(GameState gameState) throws IOException {
        String[] directions = {"u", "d", "l", "r"};
        List<String> safeDirections = new ArrayList<>();

        for (String direction : directions) {
            if (isSafeDirection(direction, gameState)) {
                safeDirections.add(direction);
            }
        }

        String direction;
        if (!safeDirections.isEmpty()) {
            direction = safeDirections.get(new Random().nextInt(safeDirections.size()));
            System.out.println("Safe exploration: " + direction);
        } else {
            direction = directions[new Random().nextInt(directions.length)];
            System.out.println("Random exploration: " + direction);
        }

        try {
            hero.move(direction);
        } catch (Exception e) {
            System.err.println("Move failed: " + e.getMessage());
        }
    }

    // FIXED: Generic move to location method
    private void moveToLocation(Object target, GameState gameState, String purpose) throws IOException {
        try {
            String path = PathUtils.getShortestPath(
                gameState.gameMap,
                getObstaclesToAvoid(gameState.gameMap),
                gameState.currentPlayer,
                (Node) target,
                false
            );

            if (path != null && !path.isEmpty()) {
                hero.move(path);
                System.out.println(purpose + ": " + path);
            } else {
                // Fallback: try simple direction
                if (target instanceof Node) {
                    Node targetNode = (Node) target;
                    String direction = getSimpleDirection(gameState.currentPlayer, targetNode);
                    if (direction != null) {
                        hero.move(direction);
                        System.out.println(purpose + " (simple): " + direction);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Move failed for " + purpose + ": " + e.getMessage());
            // Try emergency move
            emergencyMove();
        }
    }

    // Helper methods
    private List<Enemy> findDangerousNPCs(GameMap gameMap, Player currentPlayer) {
        return gameMap.getListEnemies().stream()
            .filter(enemy -> PathUtils.distance(currentPlayer, enemy) < 6)
            .sorted(Comparator.comparingDouble(e -> PathUtils.distance(currentPlayer, e)))
            .collect(Collectors.toList());
    }

    private List<Obstacle> findNearbyDragonEggs(GameMap gameMap, Player currentPlayer) {
        return gameMap.getListObstacles().stream()
            .filter(obstacle -> "DRAGON_EGG".equals(obstacle.getId()))
            .filter(dragonEgg -> PathUtils.distance(currentPlayer, dragonEgg) < 25)
            .sorted(Comparator.comparingDouble(d -> PathUtils.distance(currentPlayer, d)))
            .collect(Collectors.toList());
    }

    private Node findSafeLocationFromNPCs(GameState gameState) {
        Player currentPlayer = gameState.currentPlayer;
        int[][] directions = {{0, 10}, {0, -10}, {10, 0}, {-10, 0}, {7, 7}, {-7, -7}, {7, -7}, {-7, 7}};

        for (int[] dir : directions) {
            int newX = Math.max(1, Math.min(gameState.gameMap.getMapSize() - 2, currentPlayer.x + dir[0]));
            int newY = Math.max(1, Math.min(gameState.gameMap.getMapSize() - 2, currentPlayer.y + dir[1]));

            boolean safe = true;
            for (Enemy npc : gameState.dangerousNPCs) {
                if (PathUtils.distance(new Node(newX, newY), npc) < 8) {
                    safe = false;
                    break;
                }
            }

            if (safe) {
                return new Node(newX, newY);
            }
        }
        return null;
    }

    private Weapon findBestNormalWeapon(List<Weapon> weapons) {
        return weapons.stream()
            .filter(weapon -> !Arrays.asList("MACE", "ROPE", "BELL").contains(weapon.getId())) // Exclude dragon egg weapons
            .sorted((w1, w2) -> Integer.compare(getWeaponPriority(w2), getWeaponPriority(w1)))
            .findFirst()
            .orElse(null);
    }

    private int getWeaponPriority(Weapon weapon) {
        String type = weapon.getId();

        // Dragon Egg weapons - HIGHEST PRIORITY
        if ("MACE".equals(type)) return 10; // 60 damage + stun
        if ("ROPE".equals(type)) return 9; // Pull + stun
        if ("BELL".equals(type)) return 8; // Reverse controls

        // High-damage guns
        if ("SHOTGUN".equals(type)) return 7; // 45 damage
        if ("CROSSBOW".equals(type)) return 6; // 30 damage
        if ("SCEPTER".equals(type)) return 5; // 20 damage
        if ("RUBBER_GUN".equals(type)) return 4; // 15 damage

        // High-damage throwables
        if ("CRYSTAL".equals(type)) return 5; // 45 damage
        if ("METEORITE_FRAGMENT".equals(type)) return 4; // 35 damage
        if ("BANANA".equals(type)) return 4; // 30 damage + trap
        if ("SEED".equals(type)) return 3; // 20 damage + stun

        // Special utility
        if ("SAHUR_BAT".equals(type)) return 6; // Knockback + stun
        if ("SMOKE".equals(type)) return 3; // Blind + stealth

        // Melee weapons
        if ("AXE".equals(type)) return 4; // 40 damage
        if ("KNIFE".equals(type) || "BONE".equals(type)) return 3; // 30 damage
        if ("TREE_BRANCH".equals(type)) return 2; // 15 damage
        if ("HAND".equals(type)) return 1; // 5 damage

        return 2;
    }

    private HealingItem findBestHealingItem(List<HealingItem> items) {
        return items.stream()
            .max(Comparator.comparingInt(item -> getHealingValue(item.getId())))
            .orElse(null);
    }

    // FIXED: Replace switch expression with traditional switch for Java 8 compatibility
    private int getHealingValue(String itemType) {
        switch (itemType) {
            case "ELIXIR_OF_LIFE": return 100; // Dragon Egg item
            case "UNICORN_BLOOD": return 80;
            case "PHOENIX_FEATHERS": return 40;
            case "MERMAID_TAIL": return 20;
            case "SPIRIT_TEAR": return 15;
            case "GOD_LEAF": return 10;
            case "ELIXIR": return 5;
            default: return 5;
        }
    }

    private boolean isSafeDirection(String direction, GameState gameState) {
        Player currentPos = gameState.currentPlayer;
        int newX = currentPos.x;
        int newY = currentPos.y;

        switch (direction.toLowerCase()) {
            case "u": newY--; break;
            case "d": newY++; break;
            case "l": newX--; break;
            case "r": newX++; break;
        }

        // Check map boundaries
        int mapSize = gameState.gameMap.getMapSize();
        if (newX < 1 || newX >= mapSize - 1 || newY < 1 || newY >= mapSize - 1) {
            return false;
        }

        Node newPosition = new Node(newX, newY);

        // Check distance from player threats
        for (Player threat : gameState.immediateThreats) {
            if (PathUtils.distance(newPosition, threat) < 3) {
                return false;
            }
        }

        // Check distance from NPC threats
        for (Enemy npc : gameState.dangerousNPCs) {
            if (PathUtils.distance(newPosition, npc) < 4) {
                return false;
            }
        }

        // NEW: Check danger zones from tracked enemies
        for (Node dangerZone : dangerZones) {
            if (PathUtils.distance(newPosition, dangerZone) < 3) {
                System.out.println("Avoiding danger zone at (" + dangerZone.x + ", " + dangerZone.y + ")");
                return false;
            }
        }

        return true;
    }

    // FIXED: Properly track player movement without invalid constructor
    private void trackPlayerMovement(Player currentPlayer) {
        if (lastPlayerPosition != null) {
            if (lastPlayerPosition.x == currentPlayer.x && lastPlayerPosition.y == currentPlayer.y) {
                stuckCounter++;
            } else {
                stuckCounter = 0;
            }
        }
        // FIXED: Store position data instead of creating invalid Player object
        lastPlayerPosition = currentPlayer;
    }

    private void emergencyMove() throws IOException {
        String[] directions = {"u", "d", "l", "r"};
        String direction = directions[new Random().nextInt(directions.length)];
        try {
            hero.move(direction);
            System.out.println("Emergency move: " + direction);
        } catch (Exception e) {
            System.err.println("Emergency move failed: " + e.getMessage());
        }
    }

    private String getEscapeDirectionFromNPC(Player currentPlayer, Enemy npc) {
        int dx = currentPlayer.x - npc.x;
        int dy = currentPlayer.y - npc.y;

        if (Math.abs(dx) > Math.abs(dy)) {
            return dx > 0 ? "r" : "l";
        } else {
            return dy > 0 ? "d" : "u";
        }
    }

    private String getDirectionToTarget(Player from, Player to) {
        int dx = to.x - from.x;
        int dy = to.y - from.y;

        if (Math.abs(dx) > Math.abs(dy)) {
            return dx > 0 ? "r" : "l";
        } else {
            return dy > 0 ? "d" : "u";
        }
    }

    private String getSimpleDirection(Player from, Node to) {
        int dx = to.x - from.x;
        int dy = to.y - from.y;

        if (Math.abs(dx) > Math.abs(dy)) {
            return dx > 0 ? "r" : "l";
        } else if (dy != 0) {
            return dy > 0 ? "d" : "u";
        }
        return null;
    }

    // Standard helper methods
    private List<Enemy> findNearbyEnemies(GameMap gameMap, Player currentPlayer) {
        return gameMap.getListEnemies().stream()
            .filter(enemy -> PathUtils.distance(currentPlayer, enemy) < 10)
            .collect(Collectors.toList());
    }

    private List<Player> findNearbyPlayers(GameMap gameMap, Player currentPlayer) {
        return gameMap.getOtherPlayerInfo().stream()
            .filter(player -> PathUtils.distance(currentPlayer, player) < 15)
            .collect(Collectors.toList());
    }

    private List<Player> identifyImmediateThreats(GameState gameState) {
        return gameState.nearbyPlayers.stream()
            .filter(player -> PathUtils.distance(gameState.currentPlayer, player) < 5)
            .collect(Collectors.toList());
    }

    private List<Weapon> findNearbyWeapons(GameMap gameMap, Player currentPlayer) {
        return gameMap.getListWeapons().stream()
            .filter(weapon -> PathUtils.distance(currentPlayer, weapon) < 20)
            .sorted(Comparator.comparingDouble(w -> PathUtils.distance(currentPlayer, w)))
            .collect(Collectors.toList());
    }

    private List<HealingItem> findNearbyHealingItems(GameMap gameMap, Player currentPlayer) {
        return gameMap.getListHealingItems().stream()
            .filter(item -> PathUtils.distance(currentPlayer, item) < 20)
            .sorted(Comparator.comparingDouble(i -> PathUtils.distance(currentPlayer, i)))
            .collect(Collectors.toList());
    }

    private List<Obstacle> findNearbyChests(GameMap gameMap, Player currentPlayer) {
        return gameMap.getListObstacles().stream()
            .filter(obstacle -> "CHEST".equals(obstacle.getId()))
            .filter(chest -> PathUtils.distance(currentPlayer, chest) < 20)
            .sorted(Comparator.comparingDouble(c -> PathUtils.distance(currentPlayer, c)))
            .collect(Collectors.toList());
    }

    private List<Ally> findNearbyAllies(GameMap gameMap, Player currentPlayer) {
        return gameMap.getListAllies().stream()
            .filter(ally -> PathUtils.distance(currentPlayer, ally) < 15)
            .collect(Collectors.toList());
    }

    private List<Obstacle> findNearbyBushes(GameMap gameMap, Player currentPlayer) {
        return gameMap.getListObstacles().stream()
            .filter(obstacle -> "BUSH".equals(obstacle.getId()))
            .filter(bush -> PathUtils.distance(currentPlayer, bush) < 10)
            .collect(Collectors.toList());
    }

    private boolean isInSafeZone(GameMap gameMap, Player currentPlayer) {
        try {
            int mapCenter = gameMap.getMapSize() / 2;
            double distanceFromCenter = PathUtils.distance(
                new Node(currentPlayer.x, currentPlayer.y),
                new Node(mapCenter, mapCenter)
            );
            return distanceFromCenter <= gameMap.getSafeZone();
        } catch (Exception e) {
            return true; // Assume safe if we can't determine
        }
    }

    private double calculateSafeZoneDistance(GameMap gameMap, Player currentPlayer) {
        try {
            int mapCenter = gameMap.getMapSize() / 2;
            double distanceFromCenter = PathUtils.distance(
                new Node(currentPlayer.x, currentPlayer.y),
                new Node(mapCenter, mapCenter)
            );
            return Math.max(0, distanceFromCenter - gameMap.getSafeZone());
        } catch (Exception e) {
            return 0; // Assume safe if we can't determine
        }
    }

    private Node findSafeLocation(GameState gameState) {
        Player currentPlayer = gameState.currentPlayer;
        int[][] directions = {{0, 8}, {0, -8}, {8, 0}, {-8, 0}, {5, 5}, {-5, -5}, {5, -5}, {-5, 5}};

        for (int[] dir : directions) {
            int newX = Math.max(1, Math.min(gameState.gameMap.getMapSize() - 2, currentPlayer.x + dir[0]));
            int newY = Math.max(1, Math.min(gameState.gameMap.getMapSize() - 2, currentPlayer.y + dir[1]));

            boolean safe = true;
            for (Player threat : gameState.immediateThreats) {
                if (PathUtils.distance(new Node(newX, newY), threat) < 8) {
                    safe = false;
                    break;
                }
            }

            if (safe) {
                return new Node(newX, newY);
            }
        }
        return null;
    }

    private Node findNearestSafeZone(GameState gameState) {
        int mapCenter = gameState.gameMap.getMapSize() / 2;
        return new Node(mapCenter, mapCenter);
    }

    private List<Node> getObstaclesToAvoid(GameMap gameMap) {
        List<Node> obstacles = new ArrayList<>();

        try {
            obstacles.addAll(gameMap.getListIndestructibles());

            obstacles.addAll(gameMap.getListObstacles().stream()
                .filter(obstacle -> !"BUSH".equals(obstacle.getId()) &&
                                  !"HUNT_TRAP".equals(obstacle.getId()) &&
                                  !"BANANA_PEEL".equals(obstacle.getId()))
                .collect(Collectors.toList()));

            obstacles.addAll(gameMap.getListEnemies());
        } catch (Exception e) {
            System.err.println("Error getting obstacles to avoid: " + e.getMessage());
        }

        return obstacles;
    }

    private String getEscapeDirection(Player currentPlayer, Player threat) {
        int dx = currentPlayer.x - threat.x;
        int dy = currentPlayer.y - threat.y;

        if (Math.abs(dx) > Math.abs(dy)) {
            return dx > 0 ? "r" : "l";
        } else {
            return dy > 0 ? "d" : "u";
        }
    }

    // NEW: Update enemy trackers and danger zones
    private void updateEnemyTrackers(GameState gameState) {
        long currentTime = System.currentTimeMillis();

        for (Enemy enemy : gameState.nearbyEnemies) {
            String enemyId = enemy.getId();

            // Update existing tracker
            if (enemyTrackers.containsKey(enemyId)) {
                EnemyTracker tracker = enemyTrackers.get(enemyId);
                tracker.update(enemy, currentTime);
            } else {
                // Add new tracker
                enemyTrackers.put(enemyId, new EnemyTracker(enemy, currentTime));
            }
        }

        // Remove outdated trackers
        enemyTrackers.values().removeIf(tracker -> currentTime - tracker.lastSeen > 5000);

        // Update danger zones
        updateDangerZones(gameState);
    }

    // NEW: Update danger zones based on enemy positions
    private void updateDangerZones(GameState gameState) {
        long currentTime = System.currentTimeMillis();

        // Clear old danger zones
        dangerZones.clear();

        for (EnemyTracker tracker : enemyTrackers.values()) {
            if (currentTime - tracker.lastSeen <= 5000) {
                // Enemy is recent - add danger zone
                dangerZones.add(tracker.getLastPosition());
            }
        }
    }

    // NEW: Enemy tracking class
    private static class EnemyTracker {
        String enemyId;
        Node lastPosition;
        long lastSeen;

        EnemyTracker(Enemy enemy, long timestamp) {
            this.enemyId = enemy.getId();
            update(enemy, timestamp);
        }

        void update(Enemy enemy, long timestamp) {
            this.lastPosition = new Node(enemy.x, enemy.y);
            this.lastSeen = timestamp;
        }

        Node getLastPosition() {
            return lastPosition;
        }
    }
}

// Supporting classes
class GameState {
    Player currentPlayer;
    GameMap gameMap;
    List<Enemy> nearbyEnemies = new ArrayList<>();
    List<Player> nearbyPlayers = new ArrayList<>();
    List<Player> immediateThreats = new ArrayList<>();
    List<Enemy> dangerousNPCs = new ArrayList<>();
    List<Weapon> nearbyWeapons = new ArrayList<>();
    List<HealingItem> nearbyHealingItems = new ArrayList<>();
    List<Obstacle> nearbyChests = new ArrayList<>();
    List<Obstacle> nearbyDragonEggs = new ArrayList<>();
    List<Ally> nearbyAllies = new ArrayList<>();
    List<Obstacle> nearbyBushes = new ArrayList<>();
    boolean isInSafeZone;
    double safeZoneDistance;
    boolean hasWeapon;
    boolean needsHealing;
    boolean criticalHealth;
    boolean lowHealth;
}

class ActionPlan {
    ActionType action;
    int priority;
    Object target;

    ActionPlan(ActionType action, int priority, Object target) {
        this.action = action;
        this.priority = priority;
        this.target = target;
    }
}

enum ActionType {
    FLEE_FROM_THREAT,
    MOVE_TO_SAFE_ZONE,
    FIGHT_BACK,
    COLLECT_WEAPON,
    EMERGENCY_HEAL,
    HEAL,
    LOOT_CHEST,
    LOOT_DRAGON_EGG,
    HIDE_IN_BUSH,
    HEAL_FROM_ALLY,
    ATTACK_PLAYER,
    EXPLORE,
    EMERGENCY_FLEE
}
