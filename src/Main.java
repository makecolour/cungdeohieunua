import io.socket.emitter.Emitter;
import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.weapon.Weapon;
import jsclub.codefest.sdk.model.healing_items.HealingItem;
import jsclub.codefest.sdk.model.armors.Armor;
import jsclub.codefest.sdk.model.obstacles.Obstacle;
import jsclub.codefest.sdk.model.npcs.Ally;
import jsclub.codefest.sdk.model.npcs.Enemy;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Main {
    private static final String SERVER_URL = "https://cf25-server.jsclub.dev";
    private static final String GAME_ID = "190716";
    private static final String PLAYER_NAME = "CF25_7_Bot_2";
    private static final String SECRET_KEY = "sk-4WqZJ7o2SMCWqz2W4PCHjw:hcZoNXuEaXUpZAZxcqUQLnxz5jfLPIkCjvlFsAdhIDBM7PJyqri_nhYbMneOLWtUeDC6HBWmFXBM2wXQu3rcdA";

    public static void main(String[] args) throws IOException {
        Hero hero = new Hero(GAME_ID, PLAYER_NAME, SECRET_KEY);
        IntelligentBot bot = new IntelligentBot(hero);

        hero.setOnMapUpdate(bot);
        hero.start(SERVER_URL);
    }
}

class IntelligentBot implements Emitter.Listener {
    private final Hero hero;
    private GameState gameState;
    private ThreatTracker threatTracker;
    private ItemTracker itemTracker;
    private DecisionTree decisionTree;
    private int stepCounter = 0;

    public IntelligentBot(Hero hero) {
        this.hero = hero;
        this.gameState = new GameState();
        this.threatTracker = new ThreatTracker();
        this.itemTracker = new ItemTracker();
        this.decisionTree = new DecisionTree(hero, threatTracker, itemTracker);
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

            stepCounter++;

            // Update all tracking systems
            gameState.update(gameMap, player, hero);
            threatTracker.update(gameState, stepCounter);
            itemTracker.update(gameState, stepCounter);

            // Make intelligent decision using enhanced decision tree
            Action action = decisionTree.makeDecision(gameState);

            // Execute the action
            executeAction(action, gameMap, player);

        } catch (Exception e) {
            System.err.println("Critical error in bot logic: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void executeAction(Action action, GameMap gameMap, Player player) throws IOException {
        if (action == null) return;

        switch (action.getType()) {
            case MOVE:
                if (action.getPath() != null && !action.getPath().isEmpty()) {
                    hero.move(action.getPath());
                }
                break;
            case ATTACK:
                if (action.getDirection() != null) {
                    if (hero.getInventory().getGun() != null) {
                        hero.shoot(action.getDirection());
                    } else if (hero.getInventory().getMelee() != null) {
                        hero.attack(action.getDirection());
                    }
                }
                break;
            case PICKUP:
                hero.pickupItem();
                break;
            case USE_ITEM:
                if (action.getItemId() != null) {
                    hero.useItem(action.getItemId());
                }
                break;
            case THROW:
                if (action.getDirection() != null && action.getDistance() > 0) {
                    hero.throwItem(action.getDirection(), action.getDistance());
                }
                break;
            case USE_SPECIAL:
                if (action.getDirection() != null) {
                    hero.useSpecial(action.getDirection());
                }
                break;
            case WAIT:
                // Do nothing for this step
                break;
        }
    }
}

// Enhanced GameState class
class GameState {
    private Player currentPlayer;
    private GameMap gameMap;
    private List<Player> enemies;
    private List<Weapon> availableWeapons;
    private List<HealingItem> healingItems;
    private List<Armor> armors;
    private List<Enemy> hostileNPCs;
    private List<Ally> friendlyNPCs;
    private boolean inSafeZone;
    private int remainingTime;
    private ThreatLevel threatLevel;

    public void update(GameMap gameMap, Player player, Hero hero) {
        this.currentPlayer = player;
        this.gameMap = gameMap;
        this.enemies = gameMap.getOtherPlayerInfo();
        this.availableWeapons = gameMap.getListWeapons();
        this.healingItems = gameMap.getListHealingItems();
        this.armors = gameMap.getListArmors();
        this.hostileNPCs = gameMap.getListEnemies();
        this.friendlyNPCs = gameMap.getListAllies();

        // Check if in safe zone
        this.inSafeZone = PathUtils.checkInsideSafeArea(player, gameMap.getSafeZone(), gameMap.getMapSize());

        // Calculate threat level
        this.threatLevel = calculateThreatLevel();
    }

    private ThreatLevel calculateThreatLevel() {
        int nearbyEnemies = (int) enemies.stream()
                .mapToInt(enemy -> PathUtils.distance(currentPlayer, enemy))
                .filter(distance -> distance <= 5)
                .count();

        int nearbyHostileNPCs = (int) hostileNPCs.stream()
                .mapToInt(npc -> PathUtils.distance(currentPlayer, npc))
                .filter(distance -> distance <= 3)
                .count();

        if (nearbyEnemies >= 2 || nearbyHostileNPCs >= 2) return ThreatLevel.HIGH;
        if (nearbyEnemies >= 1 || nearbyHostileNPCs >= 1) return ThreatLevel.MEDIUM;
        if (!inSafeZone) return ThreatLevel.LOW;
        return ThreatLevel.SAFE;
    }

    // Getters
    public Player getCurrentPlayer() { return currentPlayer; }
    public GameMap getGameMap() { return gameMap; }
    public List<Player> getEnemies() { return enemies; }
    public List<Weapon> getAvailableWeapons() { return availableWeapons; }
    public List<HealingItem> getHealingItems() { return healingItems; }
    public List<Armor> getArmors() { return armors; }
    public List<Enemy> getHostileNPCs() { return hostileNPCs; }
    public List<Ally> getFriendlyNPCs() { return friendlyNPCs; }
    public boolean isInSafeZone() { return inSafeZone; }
    public ThreatLevel getThreatLevel() { return threatLevel; }
}

// Enhanced Threat Tracker for enemy movement prediction
class ThreatTracker {
    private Map<String, List<Node>> enemyRoutes = new HashMap<>();
    private Set<Node> dangerousZones = new HashSet<>();
    private int lastUpdateStep = 0;

    public void update(GameState state, int currentStep) {
        lastUpdateStep = currentStep;

        // Track enemy routes but only keep recent positions
        trackEnemyRoutes(state.getEnemies(), currentStep);
        trackNPCRoutes(state.getHostileNPCs(), currentStep);

        // Only mark current and very recent positions as dangerous
        updateDangerousZones();
    }

    private void trackEnemyRoutes(List<Player> enemies, int step) {
        for (Player enemy : enemies) {
            String enemyId = enemy.getId();
            List<Node> route = enemyRoutes.computeIfAbsent(enemyId, k -> new ArrayList<>());

            Node currentPos = new Node(enemy.getX(), enemy.getY());

            // Only add if it's a new position
            if (route.isEmpty() || !route.get(route.size() - 1).equals(currentPos)) {
                route.add(currentPos);
            }

            // Keep only last 5 positions (much shorter history)
            if (route.size() > 5) {
                route.remove(0);
            }
        }
    }

    private void trackNPCRoutes(List<Enemy> npcs, int step) {
        for (Enemy npc : npcs) {
            String npcId = npc.getId();
            List<Node> route = enemyRoutes.computeIfAbsent(npcId, k -> new ArrayList<>());

            Node currentPos = new Node(npc.getX(), npc.getY());

            // Only add if it's a new position
            if (route.isEmpty() || !route.get(route.size() - 1).equals(currentPos)) {
                route.add(currentPos);
            }

            // Keep only last 5 positions for NPCs
            if (route.size() > 5) {
                route.remove(0);
            }
        }
    }

    private void updateDangerousZones() {
        dangerousZones.clear();

        // Only mark danger zones around the most recent 2 positions of each enemy
        for (List<Node> route : enemyRoutes.values()) {
            int startIndex = Math.max(0, route.size() - 2); // Only last 2 positions
            for (int i = startIndex; i < route.size(); i++) {
                Node position = route.get(i);
                markDangerousZone(position.getX(), position.getY(), 2); // Reduced radius to 2
            }
        }
    }

    private void markDangerousZone(int centerX, int centerY, int radius) {
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int y = centerY - radius; y <= centerY + radius; y++) {
                dangerousZones.add(new Node(x, y));
            }
        }
    }

    public boolean isDangerous(Node position) {
        return dangerousZones.contains(position);
    }

    public List<Node> getDangerousNodes() {
        return new ArrayList<>(dangerousZones);
    }
}

// Enhanced Item Tracker to prevent getting stuck on items
class ItemTracker {
    private Map<String, ItemAttempt> pickupAttempts = new HashMap<>();
    private Set<String> blacklistedItems = new HashSet<>();
    private int lastUpdateStep = 0;

    public void update(GameState state, int currentStep) {
        lastUpdateStep = currentStep;

        // Clean up old attempts
        pickupAttempts.entrySet().removeIf(entry -> currentStep - entry.getValue().lastAttemptStep > 20);

        // Validate current items still exist
        validateItems(state);
    }

    private void validateItems(GameState state) {
        Set<String> currentItemIds = new HashSet<>();

        // Collect all current item IDs
        if (state.getAvailableWeapons() != null) {
            currentItemIds.addAll(state.getAvailableWeapons().stream().map(Weapon::getId).collect(Collectors.toSet()));
        }
        if (state.getHealingItems() != null) {
            currentItemIds.addAll(state.getHealingItems().stream().map(HealingItem::getId).collect(Collectors.toSet()));
        }
        if (state.getArmors() != null) {
            currentItemIds.addAll(state.getArmors().stream().map(Armor::getId).collect(Collectors.toSet()));
        }

        // Remove attempts for items that no longer exist
        pickupAttempts.keySet().retainAll(currentItemIds);
        blacklistedItems.retainAll(currentItemIds);
    }

    public boolean canAttemptPickup(String itemId, Node itemPosition, Node playerPosition) {
        // Check if item is blacklisted
        if (blacklistedItems.contains(itemId)) {
            return false;
        }

        ItemAttempt attempt = pickupAttempts.get(itemId);
        if (attempt == null) {
            return true;
        }

        // Check if we've tried too many times
        if (attempt.attemptCount >= 3) {
            blacklistedItems.add(itemId);
            return false;
        }

        // Check if enough time has passed since last attempt
        if (lastUpdateStep - attempt.lastAttemptStep < 5) {
            return false;
        }

        // Check if we're actually at the item position
        if (PathUtils.distance(playerPosition, itemPosition) == 0) {
            return true;
        }

        return true;
    }

    public void recordPickupAttempt(String itemId, boolean success) {
        if (success) {
            pickupAttempts.remove(itemId);
            blacklistedItems.remove(itemId);
        } else {
            ItemAttempt attempt = pickupAttempts.computeIfAbsent(itemId, k -> new ItemAttempt());
            attempt.attemptCount++;
            attempt.lastAttemptStep = lastUpdateStep;
        }
    }

    public boolean isBlacklisted(String itemId) {
        return blacklistedItems.contains(itemId);
    }
}

// Helper classes for tracking
class EnemyInfo {
    private List<Node> recentPositions = new ArrayList<>();
    private List<Integer> timestamps = new ArrayList<>();

    public void addPosition(Node position, int step) {
        recentPositions.add(position);
        timestamps.add(step);

        // Keep only recent data
        if (recentPositions.size() > 10) {
            recentPositions.remove(0);
            timestamps.remove(0);
        }
    }

    public List<Node> getRecentPositions() {
        return new ArrayList<>(recentPositions);
    }
}

class ItemAttempt {
    int attemptCount = 0;
    int lastAttemptStep = 0;
}

// Enhanced Decision Tree with proper threat analysis
class DecisionTree {
    private final Hero hero;
    private final ThreatTracker threatTracker;
    private final ItemTracker itemTracker;

    public DecisionTree(Hero hero, ThreatTracker threatTracker, ItemTracker itemTracker) {
        this.hero = hero;
        this.threatTracker = threatTracker;
        this.itemTracker = itemTracker;
    }

    public Action makeDecision(GameState state) {
        // ABSOLUTE PRIORITY: SAFE ZONE - this overrides everything else
        if (!state.isInSafeZone()) {
            Action safeZoneAction = moveToSafeZoneUrgently(state);
            if (safeZoneAction != null) {
                System.out.println("CRITICAL: Moving to safe zone - outside safe area!");
                return safeZoneAction;
            }
        }

        // SECONDARY PRIORITY: Survival - heal if critically low health (but only if in safe zone)
        if (state.getCurrentPlayer().getHealth() <= 30 && state.isInSafeZone()) {
            Action healAction = findBestHealingAction(state);
            if (healAction != null) return healAction;
        }

        // TERTIARY PRIORITY: Combat - but only if we're safely in the safe zone
        if (state.isInSafeZone() && (state.getThreatLevel() == ThreatLevel.HIGH || state.getThreatLevel() == ThreatLevel.MEDIUM)) {
            Action combatAction = decideCombatAction(state);
            if (combatAction != null) return combatAction;
        }

        // LOWER PRIORITIES: Only execute if we're in safe zone
        if (state.isInSafeZone()) {
            // Equipment optimization
            Action equipmentAction = optimizeEquipmentSafely(state);
            if (equipmentAction != null) return equipmentAction;

            // Positioning strategy
            Action positionAction = strategicPositioning(state);
            if (positionAction != null) return positionAction;

            // Default action - explore safely
            return exploreMapSafely(state);
        }

        // Fallback: If somehow we can't get to safe zone, just wait
        return new Action(ActionType.WAIT);
    }

    private Action moveToSafeZoneUrgently(GameState state) {
        // Calculate the center of the safe zone more accurately
        int mapSize = state.getGameMap().getMapSize();
        Node currentPos = state.getCurrentPlayer();

        // Try multiple safe zone positions to find the best path
        List<Node> safeTargets = new ArrayList<>();

        // Add center of map (most likely to be safe)
        safeTargets.add(new Node(mapSize / 2, mapSize / 2));

        // Add positions around center in case center is blocked
        for (int radius = 1; radius <= 5; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    if (Math.abs(dx) + Math.abs(dy) == radius) { // Only perimeter
                        int x = mapSize / 2 + dx;
                        int y = mapSize / 2 + dy;
                        if (x >= 0 && x < mapSize && y >= 0 && y < mapSize) {
                            safeTargets.add(new Node(x, y));
                        }
                    }
                }
            }
        }

        // Find the closest reachable safe position
        for (Node target : safeTargets) {
            // Check if this position is actually in safe zone
            if (PathUtils.checkInsideSafeArea(target, state.getGameMap().getSafeZone(), mapSize)) {
                // Use minimal obstacles to avoid to get there as fast as possible
                List<Node> minimalObstacles = getMinimalObstaclesToAvoid(state);

                String path = PathUtils.getShortestPath(
                        state.getGameMap(),
                        minimalObstacles,
                        currentPos,
                        target,
                        false
                );

                if (path != null && !path.isEmpty()) {
                    return new Action(ActionType.MOVE, path);
                }
            }
        }

        // If no specific safe target works, try to move toward center anyway
        String path = PathUtils.getShortestPath(
                state.getGameMap(),
                getMinimalObstaclesToAvoid(state),
                currentPos,
                new Node(mapSize / 2, mapSize / 2),
                false
        );

        if (path != null && !path.isEmpty()) {
            return new Action(ActionType.MOVE, path);
        }

        return null;
    }

    private List<Node> getMinimalObstaclesToAvoid(GameState state) {
        List<Node> nodes = new ArrayList<>();

        try {
            // Only avoid absolutely necessary obstacles when heading to safe zone
            var indestructibles = state.getGameMap().getListIndestructibles();
            if (indestructibles != null) {
                nodes.addAll(indestructibles);
            }

            // Add destructible obstacles that we can't go through
            var destructibles = state.getGameMap().getListObstacles();
            if (destructibles != null) {
                nodes.addAll(destructibles);
            }

            // Don't avoid enemy danger zones when rushing to safe zone - HP loss from zone is worse
            // Only avoid immediate enemy positions (current step only)
            for (Player enemy : state.getEnemies()) {
                nodes.add(new Node(enemy.getX(), enemy.getY()));
            }
            for (Enemy npc : state.getHostileNPCs()) {
                nodes.add(new Node(npc.getX(), npc.getY()));
            }

            // Remove nodes we can go through
            var passableNodes = state.getGameMap().getObstaclesByTag("CAN_GO_THROUGH");
            if (passableNodes != null) {
                nodes.removeAll(passableNodes);
            }
        } catch (Exception e) {
            System.out.println("Error building minimal avoidance list: " + e.getMessage());
        }

        return nodes;
    }

    private Action findBestHealingAction(GameState state) {
        try {
            List<HealingItem> healingItems = hero.getInventory().getListHealingItem();

            if (healingItems != null && !healingItems.isEmpty()) {
                HealingItem bestItem = healingItems.stream()
                        .max(Comparator.comparing(item -> calculateHealingEfficiency(item, Math.round(state.getCurrentPlayer().getHealth()))))
                        .orElse(null);

                if (bestItem != null) {
                    return new Action(ActionType.USE_ITEM, bestItem.getId());
                }
            }
        } catch (Exception e) {
            System.out.println("Error accessing inventory healing items: " + e.getMessage());
        }

        // Look for healing items on map with safety validation
        List<HealingItem> mapHealingItems = state.getHealingItems();
        if (mapHealingItems != null && !mapHealingItems.isEmpty()) {
            HealingItem nearest = findNearestSafeItem(mapHealingItems, state);
            if (nearest != null && itemTracker.canAttemptPickup(nearest.getId(), nearest, state.getCurrentPlayer())) {
                String path = PathUtils.getShortestPath(
                        state.getGameMap(),
                        getSafeNodesToAvoid(state),
                        state.getCurrentPlayer(),
                        nearest,
                        false
                );

                if (path != null) {
                    if (path.isEmpty()) {
                        return new Action(ActionType.PICKUP);
                    } else {
                        return new Action(ActionType.MOVE, path);
                    }
                }
            }
        }

        return null;
    }

    private int calculateHealingEfficiency(HealingItem item, int currentHealth) {
        int healthDeficit = 100 - currentHealth;
        int healAmount = Math.min(item.getHealingHP(), healthDeficit);
        double efficiency = (double) healAmount / (item.getUsageTime() + 1);
        return (int) (efficiency * 100);
    }

    private Action moveToSafeZone(GameState state) {
        int mapSize = state.getGameMap().getMapSize();
        Node safeCenter = new Node(mapSize / 2, mapSize / 2);

        String path = PathUtils.getShortestPath(
                state.getGameMap(),
                getSafeNodesToAvoid(state),
                state.getCurrentPlayer(),
                safeCenter,
                false
        );

        if (path != null && !path.isEmpty()) {
            return new Action(ActionType.MOVE, path);
        }

        return null;
    }

    private Action decideCombatAction(GameState state) {
        Player nearestEnemy = findNearestThreat(state);
        if (nearestEnemy == null) return null;

        String direction = calculateDirection(state.getCurrentPlayer(), nearestEnemy);
        int distance = PathUtils.distance(state.getCurrentPlayer(), nearestEnemy);

        try {
            // PRIORITY 1: AVOID COMBAT if enemy is too close or we're outgunned
            if (shouldAvoidCombat(state, nearestEnemy, distance)) {
                return createEvasiveAction(state, nearestEnemy);
            }

            // PRIORITY 2: PRECISE RANGED COMBAT - maintain safe distance
            if (hero.getInventory().getGun() != null && distance >= 4 && distance <= 8) {
                // Optimal gun range: far enough to be safe, close enough to be accurate
                return new Action(ActionType.ATTACK, direction);
            }

            // PRIORITY 3: THROWABLE COMBAT - medium range, explosive damage
            if (hero.getInventory().getThrowable() != null && distance >= 3 && distance <= 6) {
                // Throwables are effective at medium range with area damage
                return new Action(ActionType.THROW, direction, Math.min(distance, 6));
            }

            // PRIORITY 4: SPECIAL WEAPONS - when we have tactical advantage
            if (hero.getInventory().getSpecial() != null && distance >= 3 && distance <= 7 && hasStrengtAdvantage(state)) {
                return new Action(ActionType.USE_SPECIAL, direction);
            }

            // PRIORITY 5: MELEE only as last resort when cornered
            if (hero.getInventory().getMelee() != null && distance <= 2 && isCornered(state)) {
                return new Action(ActionType.ATTACK, direction);
            }

            // DEFAULT: If no good combat option, evade
            return createEvasiveAction(state, nearestEnemy);

        } catch (Exception e) {
            System.out.println("Error in combat decision: " + e.getMessage());
            return createEvasiveAction(state, nearestEnemy);
        }
    }

    private boolean shouldAvoidCombat(GameState state, Player enemy, int distance) {
        // Avoid combat if:

        // 1. Enemy is too close (within their attack range + safety margin)
        if (distance <= 3) {
            return true;
        }

        // 2. We're at low health
        if (state.getCurrentPlayer().getHealth() <= 40) {
            return true;
        }

        // 3. Multiple enemies nearby (outnumbered)
        long nearbyEnemyCount = state.getEnemies().stream()
                .mapToInt(e -> PathUtils.distance(state.getCurrentPlayer(), e))
                .filter(d -> d <= 6)
                .count();

        if (nearbyEnemyCount >= 2) {
            return true;
        }

        // 4. We don't have good weapons for current distance
        if (!hasViableWeapon(distance)) {
            return true;
        }

        // 5. Enemy has better positioning (near cover, high ground, etc.)
        if (enemyHasAdvantage(state, enemy)) {
            return true;
        }

        return false;
    }

    private boolean hasViableWeapon(int distance) {
        try {
            // Check if we have appropriate weapons for the distance
            if (distance >= 4 && distance <= 8 && hero.getInventory().getGun() != null) {
                return true; // Guns are good at medium-long range
            }

            if (distance >= 3 && distance <= 6 && hero.getInventory().getThrowable() != null) {
                return true; // Throwables good at medium range
            }

            if (distance >= 3 && distance <= 7 && hero.getInventory().getSpecial() != null) {
                return true; // Specials have various ranges but generally safer
            }

            // Melee only viable if very close and no other option
            if (distance <= 2 && hero.getInventory().getMelee() != null) {
                return true;
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasStrengtAdvantage(GameState state) {
        // Check if we have tactical advantage for using special weapons
        try {
            // Good health
            if (state.getCurrentPlayer().getHealth() < 60) {
                return false;
            }

            // Not too many enemies nearby
            long nearbyEnemies = state.getEnemies().stream()
                    .mapToInt(e -> PathUtils.distance(state.getCurrentPlayer(), e))
                    .filter(d -> d <= 5)
                    .count();

            return nearbyEnemies <= 1;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isCornered(GameState state) {
        // Check if we have limited escape routes (cornered)
        Node currentPos = state.getCurrentPlayer();
        int escapeRoutes = 0;

        // Check 4 directions for escape routes
        int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};

        for (int[] dir : directions) {
            Node escapePos = new Node(currentPos.getX() + dir[0] * 3, currentPos.getY() + dir[1] * 3);

            // Check if this direction leads to safety
            if (!threatTracker.isDangerous(escapePos) && isValidPosition(escapePos, state)) {
                escapeRoutes++;
            }
        }

        return escapeRoutes <= 1; // Cornered if only 1 or no escape routes
    }

    private boolean enemyHasAdvantage(GameState state, Player enemy) {
        // Check if enemy has positional advantage

        // Enemy near cover (obstacles around them)
        int enemyCover = countNearbyObstacles(enemy, state, 2);
        int ourCover = countNearbyObstacles(state.getCurrentPlayer(), state, 2);

        if (enemyCover > ourCover + 1) {
            return true; // Enemy has significantly better cover
        }

        // Enemy has height advantage (higher Y position in this context means better position)
        // This is game-specific logic - adjust based on actual map layout
        if (enemy.getY() > state.getCurrentPlayer().getY() + 3) {
            return true;
        }

        return false;
    }

    private Action createEvasiveAction(GameState state, Player enemy) {
        // Create action to evade the enemy
        Node currentPos = state.getCurrentPlayer();
        Node bestEscapePos = findBestEscapePosition(state, enemy);

        if (bestEscapePos != null) {
            String path = PathUtils.getShortestPath(
                    state.getGameMap(),
                    getSafeNodesToAvoid(state),
                    currentPos,
                    bestEscapePos,
                    false
            );

            if (path != null && !path.isEmpty()) {
                return new Action(ActionType.MOVE, path);
            }
        }

        // If no good escape route, try to create distance in the opposite direction
        return createDistanceAction(state, enemy);
    }

    private Node findBestEscapePosition(GameState state, Player enemy) {
        Node currentPos = state.getCurrentPlayer();
        Node enemyPos = enemy;

        // Look for positions that maximize distance from enemy and minimize danger
        Node bestPos = null;
        double bestScore = -1;

        // Search in expanding radius
        for (int radius = 4; radius <= 8; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    if (Math.abs(dx) + Math.abs(dy) != radius) continue; // Only check perimeter

                    int x = currentPos.getX() + dx;
                    int y = currentPos.getY() + dy;
                    Node candidate = new Node(x, y);

                    if (!isValidPosition(candidate, state) || threatTracker.isDangerous(candidate)) {
                        continue;
                    }

                    // Score based on distance from enemy and safety
                    double distanceFromEnemy = PathUtils.distance(candidate, enemyPos);
                    double safetyScore = calculateSafetyScore(candidate, state);
                    double totalScore = distanceFromEnemy * 2 + safetyScore;

                    if (totalScore > bestScore) {
                        bestScore = totalScore;
                        bestPos = candidate;
                    }
                }
            }

            if (bestPos != null) break; // Found good position at this radius
        }

        return bestPos;
    }

    private Action createDistanceAction(GameState state, Player enemy) {
        // Move in direction opposite to enemy
        Node currentPos = state.getCurrentPlayer();
        int dx = currentPos.getX() - enemy.getX();
        int dy = currentPos.getY() - enemy.getY();

        // Normalize and extend the direction
        if (dx != 0) dx = dx > 0 ? 3 : -3;
        if (dy != 0) dy = dy > 0 ? 3 : -3;

        Node escapePos = new Node(currentPos.getX() + dx, currentPos.getY() + dy);

        if (isValidPosition(escapePos, state) && !threatTracker.isDangerous(escapePos)) {
            String path = PathUtils.getShortestPath(
                    state.getGameMap(),
                    getSafeNodesToAvoid(state),
                    currentPos,
                    escapePos,
                    false
            );

            if (path != null && !path.isEmpty()) {
                return new Action(ActionType.MOVE, path);
            }
        }

        return new Action(ActionType.WAIT); // Last resort
    }

    private double calculateSafetyScore(Node position, GameState state) {
        double score = 0;

        // Points for being away from all enemies
        for (Player enemy : state.getEnemies()) {
            double distance = PathUtils.distance(position, enemy);
            score += Math.min(distance, 10); // Cap at 10 for diminishing returns
        }

        // Points for being away from hostile NPCs
        for (Enemy npc : state.getHostileNPCs()) {
            double distance = PathUtils.distance(position, npc);
            score += Math.min(distance * 0.5, 5); // NPCs are less threatening than players
        }

        // Points for nearby cover (but not too much - don't want to be trapped)
        int nearbyCover = countNearbyObstacles(position, state, 2);
        if (nearbyCover >= 2 && nearbyCover <= 4) {
            score += 5; // Good amount of cover
        }

        // Points for being in safe zone
        if (PathUtils.checkInsideSafeArea(position, state.getGameMap().getSafeZone(), state.getGameMap().getMapSize())) {
            score += 10;
        }

        return score;
    }

    private int countNearbyObstacles(Node position, GameState state, int radius) {
        int count = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                Node checkPos = new Node(position.getX() + dx, position.getY() + dy);
                if (state.getGameMap().getListIndestructibles().contains(checkPos) ||
                        state.getGameMap().getListObstacles().contains(checkPos)) {
                    count++;
                }
            }
        }
        return count;
    }

    private boolean isValidPosition(Node position, GameState state) {
        int mapSize = state.getGameMap().getMapSize();
        return position.getX() >= 0 && position.getX() < mapSize &&
                position.getY() >= 0 && position.getY() < mapSize &&
                !state.getGameMap().getListIndestructibles().contains(position) &&
                !state.getGameMap().getListObstacles().contains(position);
    }

    private Action optimizeEquipmentSafely(GameState state) {
        try {
            // Priority: Gun > Special > Throwable > Better Melee
            if (hero.getInventory().getGun() == null) {
                List<Weapon> guns = state.getAvailableWeapons().stream()
                        .filter(w -> "Guns".equals(w.getType()))
                        .filter(w -> itemTracker.canAttemptPickup(w.getId(), w, state.getCurrentPlayer()))
                        .collect(Collectors.toList());

                if (!guns.isEmpty()) {
                    Weapon bestGun = findBestSafeWeapon(guns, state);
                    if (bestGun != null) {
                        return createSafeMoveToItemAction(bestGun, state);
                    }
                }
            }

            // Look for special weapons
            if (hero.getInventory().getSpecial() == null) {
                List<Weapon> specials = state.getAvailableWeapons().stream()
                        .filter(w -> "Specials".equals(w.getType()))
                        .filter(w -> itemTracker.canAttemptPickup(w.getId(), w, state.getCurrentPlayer()))
                        .collect(Collectors.toList());

                if (!specials.isEmpty()) {
                    Weapon bestSpecial = findBestSafeWeapon(specials, state);
                    if (bestSpecial != null) {
                        return createSafeMoveToItemAction(bestSpecial, state);
                    }
                }
            }

            // Look for throwable weapons
            if (hero.getInventory().getThrowable() == null) {
                List<Weapon> throwables = state.getAvailableWeapons().stream()
                        .filter(w -> "Throwables".equals(w.getType()))
                        .filter(w -> itemTracker.canAttemptPickup(w.getId(), w, state.getCurrentPlayer()))
                        .collect(Collectors.toList());

                if (!throwables.isEmpty()) {
                    Weapon bestThrowable = findBestSafeWeapon(throwables, state);
                    if (bestThrowable != null) {
                        return createSafeMoveToItemAction(bestThrowable, state);
                    }
                }
            }

            // Look for armor if not equipped
            if (state.getArmors() != null && !state.getArmors().isEmpty()) {
                Armor bestArmor = state.getArmors().stream()
                        .filter(armor -> itemTracker.canAttemptPickup(armor.getId(), armor, state.getCurrentPlayer()))
                        .filter(armor -> !threatTracker.isDangerous(armor))
                        .max(Comparator.comparingDouble(this::calculateArmorValue))
                        .orElse(null);

                if (bestArmor != null) {
                    return createSafeMoveToItemAction(bestArmor, state);
                }
            }
        } catch (Exception e) {
            System.out.println("Error in equipment optimization: " + e.getMessage());
        }

        return null;
    }

    private Action strategicPositioning(GameState state) {
        Node bestPosition = findBestTacticalPosition(state);
        if (bestPosition != null && !threatTracker.isDangerous(bestPosition)) {
            String path = PathUtils.getShortestPath(
                    state.getGameMap(),
                    getSafeNodesToAvoid(state),
                    state.getCurrentPlayer(),
                    bestPosition,
                    false
            );

            if (path != null && !path.isEmpty()) {
                return new Action(ActionType.MOVE, path);
            }
        }

        return null;
    }

    private Action exploreMapSafely(GameState state) {
        List<Node> interestingNodes = new ArrayList<>();

        // Add safe weapon locations
        if (state.getAvailableWeapons() != null) {
            interestingNodes.addAll(state.getAvailableWeapons().stream()
                    .filter(w -> !threatTracker.isDangerous(w))
                    .filter(w -> itemTracker.canAttemptPickup(w.getId(), w, state.getCurrentPlayer()))
                    .collect(Collectors.toList()));
        }

        // Add safe healing item locations
        if (state.getHealingItems() != null) {
            interestingNodes.addAll(state.getHealingItems().stream()
                    .filter(h -> !threatTracker.isDangerous(h))
                    .filter(h -> itemTracker.canAttemptPickup(h.getId(), h, state.getCurrentPlayer()))
                    .collect(Collectors.toList()));
        }

        // Add ally locations (for healing)
        if (state.getFriendlyNPCs() != null) {
            interestingNodes.addAll(state.getFriendlyNPCs().stream()
                    .filter(ally -> !threatTracker.isDangerous(ally))
                    .collect(Collectors.toList()));
        }

        if (!interestingNodes.isEmpty()) {
            Node target = interestingNodes.stream()
                    .min(Comparator.comparingInt(node -> PathUtils.distance(state.getCurrentPlayer(), node)))
                    .orElse(null);

            if (target != null) {
                String path = PathUtils.getShortestPath(
                        state.getGameMap(),
                        getSafeNodesToAvoid(state),
                        state.getCurrentPlayer(),
                        target,
                        false
                );

                if (path != null && !path.isEmpty()) {
                    return new Action(ActionType.MOVE, path);
                } else if (path != null && path.isEmpty()) {
                    // We're at the target, try to pick up
                    return new Action(ActionType.PICKUP);
                }
            }
        }

        return new Action(ActionType.WAIT);
    }

    // Enhanced helper methods with threat analysis
    private List<Node> getSafeNodesToAvoid(GameState state) {
        List<Node> nodes = new ArrayList<>();

        try {
            // Add indestructible obstacles
            var indestructibles = state.getGameMap().getListIndestructibles();
            if (indestructibles != null) {
                nodes.addAll(indestructibles);
            }

            // Add dangerous zones from threat tracker
            nodes.addAll(threatTracker.getDangerousNodes());

            // Add destructible obstacles that we can't go through
            var destructibles = state.getGameMap().getListObstacles();
            if (destructibles != null) {
                nodes.addAll(destructibles);
            }

            // Remove nodes we can go through
            var passableNodes = state.getGameMap().getObstaclesByTag("CAN_GO_THROUGH");
            if (passableNodes != null) {
                nodes.removeAll(passableNodes);
            }
        } catch (Exception e) {
            System.out.println("Error building safe avoidance list: " + e.getMessage());
        }

        return nodes;
    }

    private Player findNearestThreat(GameState state) {
        if (state.getEnemies() == null || state.getEnemies().isEmpty()) {
            return null;
        }

        return state.getEnemies().stream()
                .min(Comparator.comparingInt(enemy -> PathUtils.distance(state.getCurrentPlayer(), enemy)))
                .orElse(null);
    }

    private String calculateDirection(Node from, Node to) {
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();

        if (Math.abs(dx) > Math.abs(dy)) {
            return dx > 0 ? "r" : "l";
        } else {
            return dy > 0 ? "u" : "d";
        }
    }

    private Weapon findBestSafeWeapon(List<Weapon> weapons, GameState state) {
        return weapons.stream()
                .filter(weapon -> !threatTracker.isDangerous(weapon))
                .min(Comparator.comparingInt(weapon -> PathUtils.distance(state.getCurrentPlayer(), weapon)))
                .orElse(null);
    }

    private <T extends Node> T findNearestSafeItem(List<T> items, GameState state) {
        return items.stream()
                .filter(item -> !threatTracker.isDangerous(item))
                .min(Comparator.comparingInt(item -> PathUtils.distance(state.getCurrentPlayer(), item)))
                .orElse(null);
    }

    private Action createSafeMoveToItemAction(Node item, GameState state) {
        String path = PathUtils.getShortestPath(
                state.getGameMap(),
                getSafeNodesToAvoid(state),
                state.getCurrentPlayer(),
                item,
                false
        );

        if (path != null) {
            if (path.isEmpty()) {
                return new Action(ActionType.PICKUP);
            } else {
                return new Action(ActionType.MOVE, path);
            }
        }

        return null;
    }

    private double calculateArmorValue(Armor armor) {
        return armor.getHealthPoint() + (armor.getDamageReduce() * 10);
    }

    private Node findBestTacticalPosition(GameState state) {
        // Find positions that are safe and provide tactical advantage
        int mapSize = state.getGameMap().getMapSize();

        // Look for positions near cover but not in danger zones
        for (int radius = 4; radius <= 10; radius++) {
            Node current = state.getCurrentPlayer();
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    int x = current.getX() + dx;
                    int y = current.getY() + dy;

                    if (x >= 0 && x < mapSize && y >= 0 && y < mapSize) {
                        Node pos = new Node(x, y);
                        if (!threatTracker.isDangerous(pos) && isGoodTacticalPosition(pos, state)) {
                            return pos;
                        }
                    }
                }
            }
        }

        return null;
    }

    private boolean isGoodTacticalPosition(Node position, GameState state) {
        // Check if position has cover nearby and is not in a danger zone
        int nearbyObstacles = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                Node checkPos = new Node(position.getX() + dx, position.getY() + dy);
                if (state.getGameMap().getListIndestructibles().contains(checkPos)) {
                    nearbyObstacles++;
                }
            }
        }

        return nearbyObstacles >= 2 && nearbyObstacles <= 6; // Some cover but not trapped
    }
}

// Action and ThreatLevel enums
enum ActionType {
    MOVE, ATTACK, PICKUP, USE_ITEM, THROW, USE_SPECIAL, WAIT
}

enum ThreatLevel {
    SAFE, LOW, MEDIUM, HIGH
}

class Action {
    private ActionType type;
    private String path;
    private String direction;
    private String itemId;
    private int distance;

    public Action(ActionType type) {
        this.type = type;
    }

    public Action(ActionType type, String pathOrDirectionOrItemId) {
        this.type = type;
        if (type == ActionType.MOVE) {
            this.path = pathOrDirectionOrItemId;
        } else if (type == ActionType.ATTACK || type == ActionType.USE_SPECIAL) {
            this.direction = pathOrDirectionOrItemId;
        } else if (type == ActionType.USE_ITEM) {
            this.itemId = pathOrDirectionOrItemId;
        }
    }

    public Action(ActionType type, String direction, int distance) {
        this.type = type;
        this.direction = direction;
        this.distance = distance;
    }

    // Getters
    public ActionType getType() { return type; }
    public String getPath() { return path; }
    public String getDirection() { return direction; }
    public String getItemId() { return itemId; }
    public int getDistance() { return distance; }
}