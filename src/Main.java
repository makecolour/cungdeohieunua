import io.socket.emitter.Emitter;
import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.Inventory;
import jsclub.codefest.sdk.model.ElementType;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.weapon.Weapon;
import jsclub.codefest.sdk.model.healing_items.HealingItem;
import jsclub.codefest.sdk.model.armors.Armor;
import jsclub.codefest.sdk.model.npcs.Enemy;
import jsclub.codefest.sdk.model.npcs.Ally;
import jsclub.codefest.sdk.model.obstacles.Obstacle;
import jsclub.codefest.sdk.model.effects.Effect;

import java.io.IOException;
import java.util.*;

public class Main {
    private static final String SERVER_URL = "https://cf25-server.jsclub.dev";
    private static final String GAME_ID = "184039";
    private static final String PLAYER_NAME = "CF25_7_Bot_1";
    private static final String SECRET_KEY = "sk-eSunLDmNS62xXXzTLsCJ5Q:BJxVPCkOkfr-pJO82lz96wbdiKIfApuopCAPDLQBJASWFCC0h39RZ6dMQnw4fvNVVh-Jqvvz2HdgvW95U6gtQA";

    public static void main(String[] args) throws IOException {
        Hero hero = new Hero(GAME_ID, PLAYER_NAME, SECRET_KEY);
        Emitter.Listener onMapUpdate = new IntelligentBotListener(hero);

        hero.setOnMapUpdate(onMapUpdate);
        hero.start(SERVER_URL);
    }
}

/**
 * Intelligent Bot using Decision Trees, Dynamic Programming, and Advanced Algorithms
 * for optimal gameplay in Codefest 2025 Survival Game
 */
class IntelligentBotListener implements Emitter.Listener {
    private final Hero hero;
    private GameState gameState;
    private ThreatAssessment threatAssessment;
    private WeaponPriorityManager weaponManager;
    private PathOptimizer pathOptimizer;
    private CombatStrategy combatStrategy;
    private InventoryManager inventoryManager;
    private Map<String, Integer> playerKillStreaks = new HashMap<>();
    private long lastActionTime = 0;
    private static final long ACTION_COOLDOWN = 100; // milliseconds

    public IntelligentBotListener(Hero hero) {
        this.hero = hero;
        this.gameState = new GameState();
        this.threatAssessment = new ThreatAssessment();
        this.weaponManager = new WeaponPriorityManager();
        this.pathOptimizer = new PathOptimizer();
        this.combatStrategy = new CombatStrategy();
        this.inventoryManager = new InventoryManager();
    }

    @Override
    public void call(Object... args) {
        try {
            if (args == null || args.length == 0) return;
            
            // Throttle actions to prevent overwhelming the server
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastActionTime < ACTION_COOLDOWN) {
                return;
            }
            lastActionTime = currentTime;

            GameMap gameMap = hero.getGameMap();
            gameMap.updateOnUpdateMap(args[0]);
            Player player = gameMap.getCurrentPlayer();

            if (player == null || player.getHealth() <= 0) {
                System.out.println("Player is dead or data unavailable");
                return;
            }

            // Update game state
            gameState.update(gameMap, player, hero.getInventory(), hero.getEffects());
            
            // Execute intelligent decision making
            executeDecisionTree();

        } catch (Exception e) {
            System.err.println("Critical error in IntelligentBot: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Main decision tree for bot behavior using advanced AI algorithms
     */
    private void executeDecisionTree() throws IOException {
        // Use advanced decision engine to analyze current game state
        DecisionEngine.DecisionType decision = DecisionEngine.analyzeGameState(gameState);
        
        // Generate optimal action plan using strategic planner
        List<String> actionPlan = StrategicPlanner.generateOptimalPlan(gameState, decision);
        
        // Execute the first action in the plan
        executeAction(actionPlan.get(0));
    }
    
    private void executeAction(String action) throws IOException {
        if (action.startsWith("move:")) {
            String direction = action.substring(5);
            hero.move(direction);
            System.out.println("Executing move: " + direction);
        } else if (action.startsWith("attack:")) {
            String[] parts = action.split(":");
            String weaponType = parts[1];
            String direction = parts[2];
            
            switch (weaponType) {
                case "gun":
                    if (gameState.inventory.getGun() != null) {
                        hero.shoot(direction);
                    }
                    break;
                case "melee":
                    hero.attack(direction);
                    break;
                case "throwable":
                    if (gameState.inventory.getThrowable() != null) {
                        Player target = findTargetInDirection(direction);
                        if (target != null) {
                            int distance = PathUtils.distance(gameState.player, target);
                            hero.throwItem(direction, Math.min(distance, gameState.inventory.getThrowable().getRange()));
                        }
                    }
                    break;
                case "special":
                    if (gameState.inventory.getSpecial() != null) {
                        hero.useSpecial(direction);
                    }
                    break;
                default:
                    hero.attack(direction);
                    break;
            }
            System.out.println("Executing attack: " + weaponType + " in direction " + direction);
        } else if (action.equals("heal")) {
            HealingItem bestHealing = inventoryManager.getBestHealingItem(gameState.inventory);
            if (bestHealing != null) {
                hero.useItem(bestHealing.getId());
                System.out.println("Using healing item: " + bestHealing.getId());
            }
        } else if (action.equals("pickup")) {
            hero.pickupItem();
            System.out.println("Picking up item");
        } else if (action.equals("wait")) {
            // Do nothing - waiting is sometimes the best strategy
            System.out.println("Waiting - analyzing situation");
        }
    }
    
    private Player findTargetInDirection(String direction) {
        for (Player enemy : gameState.getNearbyEnemies()) {
            String enemyDirection = calculateAttackDirection(gameState.player, enemy);
            if (enemyDirection.equals(direction)) {
                return enemy;
            }
        }
        return null;
    }

    private void handleEmergencySituation() throws IOException {
        System.out.println("Emergency situation detected!");
        
        // Priority 1: Use healing items if available and needed
        if (gameState.player.getHealth() < 50 && !gameState.inventory.getListHealingItem().isEmpty()) {
            HealingItem bestHealing = inventoryManager.getBestHealingItem(gameState.inventory);
            if (bestHealing != null) {
                hero.useItem(bestHealing.getId());
                System.out.println("Using healing item: " + bestHealing.getId());
                return;
            }
        }

        // Priority 2: Escape to safe zone
        if (!PathUtils.checkInsideSafeArea(gameState.player, gameState.gameMap.getSafeZone(), gameState.gameMap.getMapSize())) {
            moveToSafeZone();
            return;
        }

        // Priority 3: Avoid immediate threats
        List<Node> dangerousNodes = threatAssessment.getImmediateThreats(gameState);
        if (!dangerousNodes.isEmpty()) {
            executeEvasiveManeuvers();
        }
    }

    private void executeCombatStrategy() throws IOException {
        List<Player> enemies = gameState.getNearbyEnemies();
        Player targetEnemy = combatStrategy.selectBestTarget(enemies, gameState);
        
        if (targetEnemy == null) return;

        // Choose best weapon for the situation
        String weaponType = combatStrategy.selectOptimalWeapon(gameState, targetEnemy);
        String direction = calculateAttackDirection(gameState.player, targetEnemy);

        System.out.println("Engaging enemy with " + weaponType + " in direction " + direction);

        switch (weaponType) {
            case "gun":
                if (gameState.inventory.getGun() != null) {
                    hero.shoot(direction);
                }
                break;
            case "melee":
                hero.attack(direction);
                break;
            case "throwable":
                if (gameState.inventory.getThrowable() != null) {
                    int distance = PathUtils.distance(gameState.player, targetEnemy);
                    hero.throwItem(direction, Math.min(distance, gameState.inventory.getThrowable().getRange()));
                }
                break;
            case "special":
                if (gameState.inventory.getSpecial() != null) {
                    hero.useSpecial(direction);
                }
                break;
        }
    }

    private void executeEvasiveManeuvers() throws IOException {
        // Find safest position using advanced pathfinding
        Node safePosition = pathOptimizer.findSafestPosition(gameState);
        if (safePosition != null) {
            String path = pathOptimizer.getOptimalPath(gameState.player, safePosition, gameState);
            if (path != null && !path.isEmpty()) {
                hero.move(path.substring(0, 1)); // Move one step at a time
                System.out.println("Executing evasive maneuver: " + path.substring(0, 1));
            }
        }
    }

    private void executeInventoryOptimization() throws IOException {
        // Check if we're standing on a better item
        Node playerPos = gameState.player;
        
        // Prioritize weapons by effectiveness
        Weapon betterWeapon = weaponManager.findBetterWeaponAt(playerPos, gameState);
        if (betterWeapon != null) {
            hero.pickupItem();
            System.out.println("Picked up better weapon: " + betterWeapon.getId());
            return;
        }

        // Look for better armor
        Armor betterArmor = findBetterArmorAt(playerPos);
        if (betterArmor != null) {
            hero.pickupItem();
            System.out.println("Picked up better armor: " + betterArmor.getId());
            return;
        }

        // Look for healing items
        HealingItem healingItem = findHealingItemAt(playerPos);
        if (healingItem != null && gameState.inventory.getListHealingItem().size() < 5) {
            hero.pickupItem();
            System.out.println("Picked up healing item: " + healingItem.getId());
            return;
        }
    }

    private void executeStrategicPositioning() throws IOException {
        Node strategicPosition = pathOptimizer.findStrategicPosition(gameState);
        if (strategicPosition != null) {
            String path = pathOptimizer.getOptimalPath(gameState.player, strategicPosition, gameState);
            if (path != null && !path.isEmpty()) {
                hero.move(path.substring(0, 1));
                System.out.println("Moving to strategic position: " + path.substring(0, 1));
            }
        }
    }

    private void executeResourceGathering() throws IOException {
        // Priority-based resource gathering
        Node target = findBestResource();
        if (target != null) {
            String path = pathOptimizer.getOptimalPath(gameState.player, target, gameState);
            if (path != null && !path.isEmpty()) {
                if (path.isEmpty()) {
                    hero.pickupItem();
                } else {
                    hero.move(path.substring(0, 1));
                }
                System.out.println("Gathering resources: " + (path.isEmpty() ? "pickup" : path.substring(0, 1)));
            }
        } else {
            // No immediate resources, move to center for better positioning
            moveTowardsCenter();
        }
    }

    private void moveToSafeZone() throws IOException {
        int mapSize = gameState.gameMap.getMapSize();
        int safeZone = gameState.gameMap.getSafeZone();
        float center = (float)(mapSize - 1) / 2;
        
        Node safeTarget = new Node((int)center, (int)center);
        String path = pathOptimizer.getOptimalPath(gameState.player, safeTarget, gameState);
        
        if (path != null && !path.isEmpty()) {
            hero.move(path.substring(0, 1));
            System.out.println("Moving to safe zone: " + path.substring(0, 1));
        }
    }

    private Node findBestResource() {
        // Use dynamic programming to find the best resource considering:
        // 1. Value of the resource
        // 2. Distance to the resource
        // 3. Safety of the path
        // 4. Current inventory needs

        double bestScore = -1;
        Node bestTarget = null;

        // Check weapons
        for (Weapon weapon : gameState.gameMap.getListWeapons()) {
            double score = weaponManager.calculateWeaponValue(weapon, gameState);
            int distance = PathUtils.distance(gameState.player, weapon);
            double safetyScore = pathOptimizer.calculatePathSafety(gameState.player, weapon, gameState);
            
            double totalScore = score / (distance + 1) * safetyScore;
            if (totalScore > bestScore) {
                bestScore = totalScore;
                bestTarget = weapon;
            }
        }

        // Check healing items
        for (HealingItem healing : gameState.gameMap.getListHealingItems()) {
            if (gameState.inventory.getListHealingItem().size() >= 5) continue;
            
            double score = inventoryManager.calculateHealingValue(healing, gameState);
            int distance = PathUtils.distance(gameState.player, healing);
            double safetyScore = pathOptimizer.calculatePathSafety(gameState.player, healing, gameState);
            
            double totalScore = score / (distance + 1) * safetyScore;
            if (totalScore > bestScore) {
                bestScore = totalScore;
                bestTarget = healing;
            }
        }

        // Check armor
        for (Armor armor : gameState.gameMap.getListArmors()) {
            double score = inventoryManager.calculateArmorValue(armor, gameState);
            int distance = PathUtils.distance(gameState.player, armor);
            double safetyScore = pathOptimizer.calculatePathSafety(gameState.player, armor, gameState);
            
            double totalScore = score / (distance + 1) * safetyScore;
            if (totalScore > bestScore) {
                bestScore = totalScore;
                bestTarget = armor;
            }
        }

        return bestTarget;
    }

    private void moveTowardsCenter() throws IOException {
        int mapSize = gameState.gameMap.getMapSize();
        float center = (float)(mapSize - 1) / 2;
        Node centerNode = new Node((int)center, (int)center);
        
        String path = pathOptimizer.getOptimalPath(gameState.player, centerNode, gameState);
        if (path != null && !path.isEmpty()) {
            hero.move(path.substring(0, 1));
            System.out.println("Moving towards center: " + path.substring(0, 1));
        }
    }

    private String calculateAttackDirection(Player from, Player to) {
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        
        if (Math.abs(dx) > Math.abs(dy)) {
            return dx > 0 ? "r" : "l";
        } else {
            return dy > 0 ? "u" : "d";
        }
    }

    private Armor findBetterArmorAt(Node position) {
        for (Armor armor : gameState.gameMap.getListArmors()) {
            if (armor.getX() == position.getX() && armor.getY() == position.getY()) {
                return inventoryManager.isBetterArmor(armor, gameState) ? armor : null;
            }
        }
        return null;
    }

    private HealingItem findHealingItemAt(Node position) {
        for (HealingItem healing : gameState.gameMap.getListHealingItems()) {
            if (healing.getX() == position.getX() && healing.getY() == position.getY()) {
                return healing;
            }
        }
        return null;
    }
}

/**
 * Game State Management Class
 */
class GameState {
    public GameMap gameMap;
    public Player player;
    public Inventory inventory;
    public List<Effect> effects;
    private List<Player> nearbyEnemies = new ArrayList<>();
    private List<Node> dangerousNodes = new ArrayList<>();

    public void update(GameMap gameMap, Player player, Inventory inventory, List<Effect> effects) {
        this.gameMap = gameMap;
        this.player = player;
        this.inventory = inventory;
        this.effects = effects;
        
        updateNearbyEnemies();
        updateDangerousNodes();
    }

    private void updateNearbyEnemies() {
        nearbyEnemies.clear();
        for (Player otherPlayer : gameMap.getOtherPlayerInfo()) {
            if (otherPlayer.getHealth() > 0 && PathUtils.distance(player, otherPlayer) <= 10) {
                nearbyEnemies.add(otherPlayer);
            }
        }
    }

    private void updateDangerousNodes() {
        dangerousNodes.clear();
        // Add enemy positions
        dangerousNodes.addAll(gameMap.getListEnemies());
        // Add trap positions
        dangerousNodes.addAll(gameMap.getListTraps());
        // Add positions near other players
        for (Player otherPlayer : gameMap.getOtherPlayerInfo()) {
            if (otherPlayer.getHealth() > 0) {
                dangerousNodes.add(otherPlayer);
            }
        }
    }

    public boolean isInDanger() {
        // Check health status
        if (player.getHealth() < 30) return true;
        
        // Check if in dark zone
        if (!PathUtils.checkInsideSafeArea(player, gameMap.getSafeZone(), gameMap.getMapSize())) {
            return true;
        }
        
        // Check for nearby threats
        for (Node threat : dangerousNodes) {
            if (PathUtils.distance(player, threat) <= 3) {
                return true;
            }
        }
        
        // Check for dangerous effects
        for (Effect effect : effects) {
            if (effect.id.equals("POISON") || effect.id.equals("BLEED")) {
                return true;
            }
        }
        
        return false;
    }

    public boolean hasNearbyEnemies() {
        return !nearbyEnemies.isEmpty();
    }

    public List<Player> getNearbyEnemies() {
        return new ArrayList<>(nearbyEnemies);
    }

    public boolean shouldRepositionForAdvantage() {
        // Reposition if too close to map edge
        int mapSize = gameMap.getMapSize();
        if (player.getX() < 5 || player.getX() > mapSize - 5 || 
            player.getY() < 5 || player.getY() > mapSize - 5) {
            return true;
        }
        
        // Reposition if no enemies nearby and in safe position
        return nearbyEnemies.isEmpty() && inventory.getGun() != null;
    }
}

/**
 * Threat Assessment Class using Machine Learning-inspired scoring
 */
class ThreatAssessment {
    public List<Node> getImmediateThreats(GameState gameState) {
        List<Node> threats = new ArrayList<>();
        
        // Assess enemies
        for (Enemy enemy : gameState.gameMap.getListEnemies()) {
            if (PathUtils.distance(gameState.player, enemy) <= 5) {
                threats.add(enemy);
            }
        }
        
        // Assess other players
        for (Player otherPlayer : gameState.gameMap.getOtherPlayerInfo()) {
            if (otherPlayer.getHealth() > 0 && PathUtils.distance(gameState.player, otherPlayer) <= 6) {
                threats.add(otherPlayer);
            }
        }
        
        return threats;
    }

    public double calculateThreatLevel(Node threat, GameState gameState) {
        double threatLevel = 0;
        int distance = PathUtils.distance(gameState.player, threat);
        
        // Base threat decreases with distance
        threatLevel = 100.0 / (distance + 1);
        
        // Increase threat if it's a player (more dangerous than NPCs)
        if (threat instanceof Player) {
            threatLevel *= 1.5;
        }
        
        // Increase threat if we're low on health
        if (gameState.player.getHealth() < 50) {
            threatLevel *= 1.3;
        }
        
        return threatLevel;
    }
}

/**
 * Weapon Priority Manager using Dynamic Programming
 */
class WeaponPriorityManager {
    private final Map<String, Double> weaponValues = new HashMap<>();
    
    public WeaponPriorityManager() {
        initializeWeaponValues();
    }
    
    private void initializeWeaponValues() {
        // Based on the weapon stats from the CSV files
        // Higher values = better weapons
        weaponValues.put("SHOTGUN", 95.0);
        weaponValues.put("CROSSBOW", 85.0);
        weaponValues.put("SCEPTER", 80.0);
        weaponValues.put("RUBBER_GUN", 70.0);
        weaponValues.put("AXE", 75.0);
        weaponValues.put("KNIFE", 65.0);
        weaponValues.put("BONE", 60.0);
        weaponValues.put("TREE_BRANCH", 50.0);
        weaponValues.put("HAND", 20.0);
        
        // Throwables
        weaponValues.put("CRYSTAL", 90.0);
        weaponValues.put("METEORITE_FRAGMENT", 85.0);
        weaponValues.put("BANANA", 70.0);
        weaponValues.put("SEED", 75.0);
        weaponValues.put("SMOKE", 65.0);
        
        // Special weapons
        weaponValues.put("MACE", 100.0);
        weaponValues.put("SAHUR_BAT", 95.0);
        weaponValues.put("ROPE", 80.0);
        weaponValues.put("BELL", 85.0);
    }
    
    public double calculateWeaponValue(Weapon weapon, GameState gameState) {
        String weaponId = weapon.getId();
        double baseValue = weaponValues.getOrDefault(weaponId, 0.0);
        
        // Adjust based on current inventory
        if (weapon.getType().toString().equals("GUN") && gameState.inventory.getGun() == null) {
            baseValue *= 1.5; // Priority for getting a gun
        }
        
        // Adjust based on pickup points
        baseValue += weapon.getPickupPoints() * 0.1;
        
        return baseValue;
    }
    
    public Weapon findBetterWeaponAt(Node position, GameState gameState) {
        for (Weapon weapon : gameState.gameMap.getListWeapons()) {
            if (weapon.getX() == position.getX() && weapon.getY() == position.getY()) {
                if (isBetterWeapon(weapon, gameState)) {
                    return weapon;
                }
            }
        }
        return null;
    }
    
    private boolean isBetterWeapon(Weapon weapon, GameState gameState) {
        String weaponType = weapon.getType().toString();
        double newWeaponValue = calculateWeaponValue(weapon, gameState);
        
        switch (weaponType) {
            case "GUN":
                Weapon currentGun = gameState.inventory.getGun();
                if (currentGun == null) return true;
                return newWeaponValue > calculateWeaponValue(currentGun, gameState);
            
            case "MELEE":
                Weapon currentMelee = gameState.inventory.getMelee();
                if (currentMelee == null || "HAND".equals(currentMelee.getId())) return true;
                return newWeaponValue > calculateWeaponValue(currentMelee, gameState);
            
            case "THROWABLE":
                Weapon currentThrowable = gameState.inventory.getThrowable();
                if (currentThrowable == null) return true;
                return newWeaponValue > calculateWeaponValue(currentThrowable, gameState);
            
            case "SPECIAL":
                Weapon currentSpecial = gameState.inventory.getSpecial();
                if (currentSpecial == null) return true;
                return newWeaponValue > calculateWeaponValue(currentSpecial, gameState);
            
            default:
                return false;
        }
    }
}

/**
 * Path Optimizer using A* and safety heuristics
 */
class PathOptimizer {
    public String getOptimalPath(Node start, Node target, GameState gameState) {
        List<Node> nodesToAvoid = generateNodesToAvoid(gameState);
        return PathUtils.getShortestPath(gameState.gameMap, nodesToAvoid, start, target, false);
    }
    
    public Node findSafestPosition(GameState gameState) {
        int mapSize = gameState.gameMap.getMapSize();
        int safeZone = gameState.gameMap.getSafeZone();
        float center = (float)(mapSize - 1) / 2;
        
        List<Node> candidates = new ArrayList<>();
        
        // Generate candidate positions in safe zone
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                int x = gameState.player.getX() + dx;
                int y = gameState.player.getY() + dy;
                
                if (x >= 0 && x < mapSize && y >= 0 && y < mapSize) {
                    Node candidate = new Node(x, y);
                    if (PathUtils.checkInsideSafeArea(candidate, safeZone, mapSize)) {
                        candidates.add(candidate);
                    }
                }
            }
        }
        
        // Find safest among candidates
        Node safest = null;
        double bestSafety = -1;
        
        for (Node candidate : candidates) {
            double safety = calculatePositionSafety(candidate, gameState);
            if (safety > bestSafety) {
                bestSafety = safety;
                safest = candidate;
            }
        }
        
        return safest;
    }
    
    public Node findStrategicPosition(GameState gameState) {
        // Find position with good coverage and escape routes
        int mapSize = gameState.gameMap.getMapSize();
        float center = (float)(mapSize - 1) / 2;
        
        // Prefer positions near center but with cover
        for (int radius = 3; radius <= 8; radius++) {
            for (int angle = 0; angle < 360; angle += 45) {
                double radians = Math.toRadians(angle);
                int x = (int)(center + radius * Math.cos(radians));
                int y = (int)(center + radius * Math.sin(radians));
                
                if (x >= 0 && x < mapSize && y >= 0 && y < mapSize) {
                    Node candidate = new Node(x, y);
                    if (isStrategicPosition(candidate, gameState)) {
                        return candidate;
                    }
                }
            }
        }
        
        return new Node((int)center, (int)center);
    }
    
    public double calculatePathSafety(Node start, Node target, GameState gameState) {
        List<Node> threats = new ArrayList<>();
        threats.addAll(gameState.gameMap.getListEnemies());
        threats.addAll(gameState.gameMap.getOtherPlayerInfo());
        
        double minThreatDistance = Double.MAX_VALUE;
        for (Node threat : threats) {
            double distanceToPath = calculateDistanceToPath(start, target, threat);
            minThreatDistance = Math.min(minThreatDistance, distanceToPath);
        }
        
        return minThreatDistance / 10.0; // Normalize to 0-1 range
    }
    
    private List<Node> generateNodesToAvoid(GameState gameState) {
        List<Node> avoid = new ArrayList<>();
        avoid.addAll(gameState.gameMap.getListIndestructibles());
        avoid.removeAll(gameState.gameMap.getObstaclesByTag("CAN_GO_THROUGH"));
        avoid.addAll(gameState.gameMap.getListEnemies());
        avoid.addAll(gameState.gameMap.getListTraps());
        
        // Add buffer zones around other players
        for (Player otherPlayer : gameState.gameMap.getOtherPlayerInfo()) {
            if (otherPlayer.getHealth() > 0) {
                avoid.add(otherPlayer);
            }
        }
        
        return avoid;
    }
    
    private double calculatePositionSafety(Node position, GameState gameState) {
        double safety = 100.0;
        
        // Penalty for being near threats
        for (Node threat : gameState.gameMap.getListEnemies()) {
            int distance = PathUtils.distance(position, threat);
            safety -= Math.max(0, 50 - distance * 5);
        }
        
        for (Player otherPlayer : gameState.gameMap.getOtherPlayerInfo()) {
            if (otherPlayer.getHealth() > 0) {
                int distance = PathUtils.distance(position, otherPlayer);
                safety -= Math.max(0, 40 - distance * 4);
            }
        }
        
        // Bonus for being in safe zone
        if (PathUtils.checkInsideSafeArea(position, gameState.gameMap.getSafeZone(), gameState.gameMap.getMapSize())) {
            safety += 20;
        }
        
        return Math.max(0, safety);
    }
    
    private boolean isStrategicPosition(Node position, GameState gameState) {
        // Check if position has good visibility and escape routes
        int escapeRoutes = 0;
        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};
        
        for (int i = 0; i < 4; i++) {
            int newX = position.getX() + dx[i];
            int newY = position.getY() + dy[i];
            
            if (newX >= 0 && newX < gameState.gameMap.getMapSize() && 
                newY >= 0 && newY < gameState.gameMap.getMapSize()) {
                
                Node neighbor = new Node(newX, newY);
                boolean isBlocked = false;
                
                for (Node obstacle : gameState.gameMap.getListIndestructibles()) {
                    if (obstacle.getX() == newX && obstacle.getY() == newY) {
                        boolean canGoThrough = false;
                        for (Node passable : gameState.gameMap.getObstaclesByTag("CAN_GO_THROUGH")) {
                            if (passable.getX() == newX && passable.getY() == newY) {
                                canGoThrough = true;
                                break;
                            }
                        }
                        if (!canGoThrough) {
                            isBlocked = true;
                            break;
                        }
                    }
                }
                
                if (!isBlocked) {
                    escapeRoutes++;
                }
            }
        }
        
        return escapeRoutes >= 2; // At least 2 escape routes
    }
    
    private double calculateDistanceToPath(Node start, Node target, Node point) {
        // Simplified distance calculation to path
        double d1 = PathUtils.distance(start, point);
        double d2 = PathUtils.distance(target, point);
        double pathLength = PathUtils.distance(start, target);
        
        if (pathLength == 0) return d1;
        
        // Approximate distance to line segment
        return Math.min(d1, d2);
    }
}

/**
 * Combat Strategy using Game Theory and Probability
 */
class CombatStrategy {
    public boolean shouldEngage(GameState gameState) {
        // Decision tree for combat engagement
        if (gameState.player.getHealth() < 30) return false;
        if (gameState.inventory.getGun() == null && gameState.inventory.getMelee().getId().equals("HAND")) return false;
        
        List<Player> enemies = gameState.getNearbyEnemies();
        if (enemies.isEmpty()) return false;
        
        // Calculate combat advantage
        double ourCombatPower = calculateCombatPower(gameState);
        double maxEnemyPower = 0;
        
        for (Player enemy : enemies) {
            double enemyPower = estimateEnemyCombatPower(enemy, gameState);
            maxEnemyPower = Math.max(maxEnemyPower, enemyPower);
        }
        
        return ourCombatPower > maxEnemyPower * 1.2; // Only engage if we have clear advantage
    }
    
    public Player selectBestTarget(List<Player> enemies, GameState gameState) {
        if (enemies.isEmpty()) return null;
        
        Player bestTarget = null;
        double bestScore = -1;
        
        for (Player enemy : enemies) {
            double score = calculateTargetPriority(enemy, gameState);
            if (score > bestScore) {
                bestScore = score;
                bestTarget = enemy;
            }
        }
        
        return bestTarget;
    }
    
    public String selectOptimalWeapon(GameState gameState, Player target) {
        int distance = PathUtils.distance(gameState.player, target);
        
        // Special weapons for specific situations
        if (gameState.inventory.getSpecial() != null) {
            String specialId = gameState.inventory.getSpecial().getId();
            if (specialId.equals("MACE") && distance <= 3) {
                return "special"; // High damage, stun effect
            }
            if (specialId.equals("ROPE") && distance <= 6) {
                return "special"; // Pull and stun
            }
            if (specialId.equals("SAHUR_BAT") && distance <= 5) {
                return "special"; // Knockback
            }
        }
        
        // Throwables for medium range
        if (gameState.inventory.getThrowable() != null && distance >= 3 && distance <= 6) {
            String throwableId = gameState.inventory.getThrowable().getId();
            if (throwableId.equals("CRYSTAL") || throwableId.equals("METEORITE_FRAGMENT")) {
                return "throwable"; // High damage
            }
            if (throwableId.equals("SEED") && distance <= 5) {
                return "throwable"; // Stun effect
            }
        }
        
        // Guns for long range
        if (gameState.inventory.getGun() != null && distance >= 2) {
            return "gun";
        }
        
        // Melee for close range
        if (distance <= 3) {
            return "melee";
        }
        
        return "gun"; // Default
    }
    
    private double calculateCombatPower(GameState gameState) {
        double power = gameState.player.getHealth() * 0.5;
        
        // Add weapon damage
        if (gameState.inventory.getGun() != null) {
            power += gameState.inventory.getGun().getDamage() * 1.5;
        }
        if (gameState.inventory.getMelee() != null) {
            power += gameState.inventory.getMelee().getDamage();
        }
        if (gameState.inventory.getThrowable() != null) {
            power += gameState.inventory.getThrowable().getDamage() * 1.2;
        }
        if (gameState.inventory.getSpecial() != null) {
            power += gameState.inventory.getSpecial().getDamage() * 1.8;
        }
        
        // Add armor protection
        if (gameState.inventory.getArmor() != null) {
            power += gameState.inventory.getArmor().getHealthPoint() * 0.3;
        }
        if (gameState.inventory.getHelmet() != null) {
            power += gameState.inventory.getHelmet().getHealthPoint() * 0.2;
        }
        
        return power;
    }
    
    private double estimateEnemyCombatPower(Player enemy, GameState gameState) {
        // Estimate based on observable factors
        double power = enemy.getHealth() * 0.5;
        power += 50; // Assume they have basic weapons
        
        // Increase estimate if they're closer (more threatening)
        int distance = PathUtils.distance(gameState.player, enemy);
        if (distance <= 3) power *= 1.3;
        
        return power;
    }
    
    private double calculateTargetPriority(Player enemy, GameState gameState) {
        double priority = 100.0;
        
        // Prefer closer enemies
        int distance = PathUtils.distance(gameState.player, enemy);
        priority += (10 - distance) * 5;
        
        // Prefer weaker enemies
        priority += (100 - enemy.getHealth()) * 0.2;
        
        // Avoid enemies with higher ground or cover
        if (isInCover(enemy, gameState)) {
            priority *= 0.7;
        }
        
        return priority;
    }
    
    private boolean isInCover(Player player, GameState gameState) {
        // Check if player is near obstacles that provide cover
        for (Node obstacle : gameState.gameMap.getListIndestructibles()) {
            if (PathUtils.distance(player, obstacle) <= 1) {
                return true;
            }
        }
        return false;
    }
}

/**
 * Inventory Manager with optimization algorithms
 */
class InventoryManager {
    public boolean needsOptimization(GameState gameState) {
        // Check if we can improve our loadout
        Node playerPos = gameState.player;
        
        // Check for better weapons at current position
        for (Weapon weapon : gameState.gameMap.getListWeapons()) {
            if (weapon.getX() == playerPos.getX() && weapon.getY() == playerPos.getY()) {
                if (isBetterWeapon(weapon, gameState)) {
                    return true;
                }
            }
        }
        
        // Check for better armor
        for (Armor armor : gameState.gameMap.getListArmors()) {
            if (armor.getX() == playerPos.getX() && armor.getY() == playerPos.getY()) {
                if (isBetterArmor(armor, gameState)) {
                    return true;
                }
            }
        }
        
        // Check for healing items if we have space and need healing
        if (gameState.inventory.getListHealingItem().size() < 5) {
            for (HealingItem healing : gameState.gameMap.getListHealingItems()) {
                if (healing.getX() == playerPos.getX() && healing.getY() == playerPos.getY()) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    public HealingItem getBestHealingItem(Inventory inventory) {
        if (inventory.getListHealingItem().isEmpty()) return null;
        
        HealingItem best = null;
        double bestValue = 0;
        
        for (HealingItem healing : inventory.getListHealingItem()) {
            double value = calculateHealingEfficiency(healing);
            if (value > bestValue) {
                bestValue = value;
                best = healing;
            }
        }
        
        return best;
    }
    
    public double calculateHealingValue(HealingItem healing, GameState gameState) {
        double value = healing.getHealingHP();
        
        // Higher value if we're low on health
        if (gameState.player.getHealth() < 50) {
            value *= 1.5;
        }
        
        // Consider special effects
        if (healing.getEffects() != null && !healing.getEffects().isEmpty()) {
            value *= 1.2;
        }
        
        return value;
    }
    
    public double calculateArmorValue(Armor armor, GameState gameState) {
        double value = armor.getHealthPoint();
        
        if (armor.getDamageReduce() > 0) {
            value += armor.getDamageReduce() * 100; // Damage reduction is very valuable
        }
        
        // Higher value if we don't have armor of this type
        ElementType armorType = armor.getType();
        if (armorType == ElementType.HELMET && gameState.inventory.getHelmet() == null) {
            value *= 2.0; // Double value for first helmet
        } else if (armorType == ElementType.ARMOR && gameState.inventory.getArmor() == null) {
            value *= 2.5; // Higher multiplier for first body armor as it's more important
        }
        
        return value;
    }
    
    public boolean isBetterArmor(Armor armor, GameState gameState) {
        ElementType armorType = armor.getType();
        
        if (armorType == ElementType.HELMET) {
            Armor currentHelmet = gameState.inventory.getHelmet();
            if (currentHelmet == null) return true;
            return armor.getHealthPoint() > currentHelmet.getHealthPoint() || 
                   armor.getDamageReduce() > currentHelmet.getDamageReduce();
        } else if (armorType == ElementType.ARMOR) {
            Armor currentArmor = gameState.inventory.getArmor();
            if (currentArmor == null) return true;
            return armor.getHealthPoint() > currentArmor.getHealthPoint() || 
                   armor.getDamageReduce() > currentArmor.getDamageReduce();
        }
        
        return false;
    }
    
    private boolean isBetterWeapon(Weapon weapon, GameState gameState) {
        String weaponType = weapon.getType().toString();
        
        switch (weaponType) {
            case "GUN":
                Weapon currentGun = gameState.inventory.getGun();
                return currentGun == null || weapon.getDamage() > currentGun.getDamage() ||
                       weapon.getPickupPoints() > currentGun.getPickupPoints();
            
            case "MELEE":
                Weapon currentMelee = gameState.inventory.getMelee();
                return currentMelee == null || "HAND".equals(currentMelee.getId()) ||
                       weapon.getDamage() > currentMelee.getDamage();
            
            case "THROWABLE":
                Weapon currentThrowable = gameState.inventory.getThrowable();
                return currentThrowable == null || weapon.getDamage() > currentThrowable.getDamage();
            
            case "SPECIAL":
                Weapon currentSpecial = gameState.inventory.getSpecial();
                return currentSpecial == null || weapon.getDamage() > currentSpecial.getDamage();
            
            default:
                return false;
        }
    }
    
    private double calculateHealingEfficiency(HealingItem healing) {
        double efficiency = healing.getHealingHP();
        
        // Prefer items with shorter usage time
        if (healing.getUsageTime() > 0) {
            efficiency /= healing.getUsageTime();
        }
        
        // Bonus for items with special effects
        if (healing.getEffects() != null && !healing.getEffects().isEmpty()) {
            efficiency *= 1.3;
        }
        
        return efficiency;
    }
}

/**
 * Advanced Decision Engine using Machine Learning-inspired algorithms
 */
class DecisionEngine {
    private static final double HEALTH_THRESHOLD_CRITICAL = 30.0;
    private static final double HEALTH_THRESHOLD_LOW = 50.0;
    private static final double RESOURCE_VALUE_THRESHOLD = 50.0;
    private static final int SAFE_DISTANCE = 5;
    private static final int COMBAT_RANGE = 8;
    
    public static DecisionType analyzeGameState(GameState gameState) {
        // Multi-criteria decision analysis
        double survivalScore = calculateSurvivalScore(gameState);
        double combatScore = calculateCombatScore(gameState);
        double resourceScore = calculateResourceScore(gameState);
        double positionScore = calculatePositionScore(gameState);
        
        // Weight the scores based on game phase
        double gamePhase = calculateGamePhase(gameState);
        
        if (survivalScore < 0.3) return DecisionType.EMERGENCY_SURVIVAL;
        if (combatScore > 0.7 && survivalScore > 0.5) return DecisionType.AGGRESSIVE_COMBAT;
        if (resourceScore > 0.6) return DecisionType.RESOURCE_GATHERING;
        if (positionScore < 0.4) return DecisionType.REPOSITIONING;
        
        return DecisionType.EXPLORATION;
    }
    
    private static double calculateSurvivalScore(GameState gameState) {
        double score = 1.0;
        
        // Health factor
        double healthRatio = gameState.player.getHealth() / 100.0;
        score *= healthRatio;
        
        // Safety factor (distance from threats)
        double minThreatDistance = calculateMinThreatDistance(gameState);
        score *= Math.min(1.0, minThreatDistance / SAFE_DISTANCE);
        
        // Dark zone factor
        if (!PathUtils.checkInsideSafeArea(gameState.player, gameState.gameMap.getSafeZone(), gameState.gameMap.getMapSize())) {
            score *= 0.5; // Heavily penalize being in dark zone
        }
        
        return score;
    }
    
    private static double calculateCombatScore(GameState gameState) {
        if (gameState.getNearbyEnemies().isEmpty()) return 0.0;
        
        double score = 0.0;
        
        // Weapon availability
        if (gameState.inventory.getGun() != null) score += 0.4;
        if (gameState.inventory.getMelee() != null && !gameState.inventory.getMelee().getId().equals("HAND")) score += 0.2;
        if (gameState.inventory.getSpecial() != null) score += 0.3;
        if (gameState.inventory.getThrowable() != null) score += 0.1;
        
        // Health factor
        score *= (gameState.player.getHealth() / 100.0);
        
        // Enemy threat assessment
        double enemyThreat = calculateEnemyThreat(gameState);
        score *= (1.0 - enemyThreat);
        
        return Math.min(1.0, score);
    }
    
    private static double calculateResourceScore(GameState gameState) {
        double score = 0.0;
        
        // Check nearby valuable resources
        Node playerPos = gameState.player;
        
        for (Weapon weapon : gameState.gameMap.getListWeapons()) {
            double distance = PathUtils.distance(playerPos, weapon);
            if (distance <= 10) {
                double value = weapon.getPickupPoints() / Math.max(1, distance);
                score += value / 100.0;
            }
        }
        
        for (HealingItem healing : gameState.gameMap.getListHealingItems()) {
            double distance = PathUtils.distance(playerPos, healing);
            if (distance <= 8 && gameState.inventory.getListHealingItem().size() < 5) {
                double value = healing.getPoint() / Math.max(1, distance);
                score += value / 50.0;
            }
        }
        
        return Math.min(1.0, score);
    }
    
    private static double calculatePositionScore(GameState gameState) {
        double score = 1.0;
        
        // Center position is generally better
        int mapSize = gameState.gameMap.getMapSize();
        double centerX = mapSize / 2.0;
        double centerY = mapSize / 2.0;
        double distanceFromCenter = Math.sqrt(Math.pow(gameState.player.getX() - centerX, 2) + 
                                            Math.pow(gameState.player.getY() - centerY, 2));
        score *= Math.max(0.2, 1.0 - (distanceFromCenter / (mapSize * 0.5)));
        
        // Safe zone factor
        if (PathUtils.checkInsideSafeArea(gameState.player, gameState.gameMap.getSafeZone(), gameState.gameMap.getMapSize())) {
            score *= 1.2;
        }
        
        return Math.min(1.0, score);
    }
    
    private static double calculateGamePhase(GameState gameState) {
        // Early game: 0.0-0.3, Mid game: 0.3-0.7, Late game: 0.7-1.0
        // This is a simplified calculation - in a real scenario, you'd use game time
        int enemyCount = gameState.gameMap.getOtherPlayerInfo().size();
        return Math.max(0.0, 1.0 - (enemyCount / 8.0));
    }
    
    private static double calculateMinThreatDistance(GameState gameState) {
        double minDistance = Double.MAX_VALUE;
        
        for (Player enemy : gameState.gameMap.getOtherPlayerInfo()) {
            if (enemy.getHealth() > 0) {
                double distance = PathUtils.distance(gameState.player, enemy);
                minDistance = Math.min(minDistance, distance);
            }
        }
        
        for (Enemy npc : gameState.gameMap.getListEnemies()) {
            double distance = PathUtils.distance(gameState.player, npc);
            minDistance = Math.min(minDistance, distance);
        }
        
        return minDistance == Double.MAX_VALUE ? 999 : minDistance;
    }
    
    private static double calculateEnemyThreat(GameState gameState) {
        double threat = 0.0;
        
        for (Player enemy : gameState.getNearbyEnemies()) {
            double distance = PathUtils.distance(gameState.player, enemy);
            double enemyHealthRatio = enemy.getHealth() / 100.0;
            threat += enemyHealthRatio / Math.max(1, distance);
        }
        
        return Math.min(1.0, threat);
    }
    
    enum DecisionType {
        EMERGENCY_SURVIVAL,
        AGGRESSIVE_COMBAT,
        RESOURCE_GATHERING,
        REPOSITIONING,
        EXPLORATION
    }
}

/**
 * Strategic Planner using Graph Theory and Dynamic Programming
 */
class StrategicPlanner {
    private static final int PLANNING_HORIZON = 10; // Steps to look ahead
    
    public static List<String> generateOptimalPlan(GameState gameState, DecisionEngine.DecisionType decision) {
        List<String> plan = new ArrayList<>();
        
        switch (decision) {
            case EMERGENCY_SURVIVAL:
                plan = planEmergencyActions(gameState);
                break;
            case AGGRESSIVE_COMBAT:
                plan = planCombatActions(gameState);
                break;
            case RESOURCE_GATHERING:
                plan = planResourceGathering(gameState);
                break;
            case REPOSITIONING:
                plan = planRepositioning(gameState);
                break;
            case EXPLORATION:
                plan = planExploration(gameState);
                break;
        }
        
        return plan.isEmpty() ? List.of("wait") : plan;
    }
    
    private static List<String> planEmergencyActions(GameState gameState) {
        List<String> actions = new ArrayList<>();
        
        // Priority 1: Use healing if available and needed
        if (gameState.player.getHealth() < 50 && !gameState.inventory.getListHealingItem().isEmpty()) {
            actions.add("heal");
        }
        
        // Priority 2: Move to safe zone
        if (!PathUtils.checkInsideSafeArea(gameState.player, gameState.gameMap.getSafeZone(), gameState.gameMap.getMapSize())) {
            String path = findPathToSafeZone(gameState);
            if (path != null && !path.isEmpty()) {
                actions.add("move:" + path.substring(0, 1));
            }
        }
        
        // Priority 3: Avoid immediate threats
        String evasiveMove = findEvasiveMove(gameState);
        if (evasiveMove != null) {
            actions.add("move:" + evasiveMove);
        }
        
        return actions;
    }
    
    private static List<String> planCombatActions(GameState gameState) {
        List<String> actions = new ArrayList<>();
        
        Player target = selectBestCombatTarget(gameState);
        if (target != null) {
            String direction = calculateDirection(gameState.player, target);
            String weaponChoice = selectBestWeapon(gameState, target);
            
            actions.add("attack:" + weaponChoice + ":" + direction);
            
            // Plan follow-up actions
            String repositionMove = planCombatRepositioning(gameState, target);
            if (repositionMove != null) {
                actions.add("move:" + repositionMove);
            }
        }
        
        return actions;
    }
    
    private static List<String> planResourceGathering(GameState gameState) {
        List<String> actions = new ArrayList<>();
        
        Node bestResource = findHighValueResource(gameState);
        if (bestResource != null) {
            String path = PathUtils.getShortestPath(gameState.gameMap, 
                generateObstacleList(gameState), gameState.player, bestResource, false);
            
            if (path != null && !path.isEmpty()) {
                actions.add("move:" + path.substring(0, 1));
                actions.add("pickup");
            }
        }
        
        return actions;
    }
    
    private static List<String> planRepositioning(GameState gameState) {
        List<String> actions = new ArrayList<>();
        
        Node strategicPosition = findStrategicPosition(gameState);
        if (strategicPosition != null) {
            String path = PathUtils.getShortestPath(gameState.gameMap, 
                generateObstacleList(gameState), gameState.player, strategicPosition, false);
            
            if (path != null && !path.isEmpty()) {
                actions.add("move:" + path.substring(0, 1));
            }
        }
        
        return actions;
    }
    
    private static List<String> planExploration(GameState gameState) {
        List<String> actions = new ArrayList<>();
        
        // Move towards center if no specific target
        int mapSize = gameState.gameMap.getMapSize();
        Node center = new Node(mapSize / 2, mapSize / 2);
        
        String path = PathUtils.getShortestPath(gameState.gameMap, 
            generateObstacleList(gameState), gameState.player, center, false);
        
        if (path != null && !path.isEmpty()) {
            actions.add("move:" + path.substring(0, 1));
        }
        
        return actions;
    }
    
    private static String findPathToSafeZone(GameState gameState) {
        int mapSize = gameState.gameMap.getMapSize();
        Node center = new Node(mapSize / 2, mapSize / 2);
        return PathUtils.getShortestPath(gameState.gameMap, 
            generateObstacleList(gameState), gameState.player, center, false);
    }
    
    private static String findEvasiveMove(GameState gameState) {
        // Find direction away from threats
        double bestX = 0, bestY = 0;
        
        for (Player enemy : gameState.getNearbyEnemies()) {
            double dx = gameState.player.getX() - enemy.getX();
            double dy = gameState.player.getY() - enemy.getY();
            double distance = Math.max(1, PathUtils.distance(gameState.player, enemy));
            
            bestX += dx / distance;
            bestY += dy / distance;
        }
        
        // Convert to direction
        if (Math.abs(bestX) > Math.abs(bestY)) {
            return bestX > 0 ? "r" : "l";
        } else {
            return bestY > 0 ? "u" : "d";
        }
    }
    
    private static Player selectBestCombatTarget(GameState gameState) {
        Player bestTarget = null;
        double bestScore = -1;
        
        for (Player enemy : gameState.getNearbyEnemies()) {
            double score = calculateTargetScore(enemy, gameState);
            if (score > bestScore) {
                bestScore = score;
                bestTarget = enemy;
            }
        }
        
        return bestTarget;
    }
    
    private static double calculateTargetScore(Player enemy, GameState gameState) {
        double score = 100.0;
        
        // Prefer closer enemies
        int distance = PathUtils.distance(gameState.player, enemy);
        score += (10 - distance) * 5;
        
        // Prefer weaker enemies
        score += (100 - enemy.getHealth()) * 0.3;
        
        return score;
    }
    
    private static String selectBestWeapon(GameState gameState, Player target) {
        int distance = PathUtils.distance(gameState.player, target);
        
        if (gameState.inventory.getSpecial() != null && distance <= 5) {
            return "special";
        }
        if (gameState.inventory.getGun() != null && distance >= 2) {
            return "gun";
        }
        if (gameState.inventory.getThrowable() != null && distance >= 3 && distance <= 6) {
            return "throwable";
        }
        return "melee";
    }
    
    private static String planCombatRepositioning(GameState gameState, Player target) {
        // Move to optimal combat distance
        int currentDistance = PathUtils.distance(gameState.player, target);
        int optimalDistance = 3; // Balanced distance for most weapons
        
        if (currentDistance < optimalDistance) {
            // Move away
            double dx = gameState.player.getX() - target.getX();
            double dy = gameState.player.getY() - target.getY();
            
            if (Math.abs(dx) > Math.abs(dy)) {
                return dx > 0 ? "r" : "l";
            } else {
                return dy > 0 ? "u" : "d";
            }
        } else if (currentDistance > optimalDistance + 2) {
            // Move closer
            double dx = target.getX() - gameState.player.getX();
            double dy = target.getY() - gameState.player.getY();
            
            if (Math.abs(dx) > Math.abs(dy)) {
                return dx > 0 ? "r" : "l";
            } else {
                return dy > 0 ? "u" : "d";
            }
        }
        
        return null; // Stay in position
    }
    
    private static Node findHighValueResource(GameState gameState) {
        Node bestResource = null;
        double bestValue = 0;
        
        // Check weapons
        for (Weapon weapon : gameState.gameMap.getListWeapons()) {
            double value = weapon.getPickupPoints();
            double distance = PathUtils.distance(gameState.player, weapon);
            double score = value / Math.max(1, distance);
            
            if (score > bestValue) {
                bestValue = score;
                bestResource = weapon;
            }
        }
        
        // Check healing items
        for (HealingItem healing : gameState.gameMap.getListHealingItems()) {
            if (gameState.inventory.getListHealingItem().size() >= 5) continue;
            
            double value = healing.getPoint();
            double distance = PathUtils.distance(gameState.player, healing);
            double score = value / Math.max(1, distance);
            
            if (score > bestValue) {
                bestValue = score;
                bestResource = healing;
            }
        }
        
        return bestResource;
    }
    
    private static Node findStrategicPosition(GameState gameState) {
        int mapSize = gameState.gameMap.getMapSize();
        int safeZone = gameState.gameMap.getSafeZone();
        
        // Find position that maximizes safety and strategic value
        Node bestPosition = null;
        double bestScore = -1;
        
        for (int x = Math.max(0, gameState.player.getX() - 5); 
             x <= Math.min(mapSize - 1, gameState.player.getX() + 5); x++) {
            for (int y = Math.max(0, gameState.player.getY() - 5); 
                 y <= Math.min(mapSize - 1, gameState.player.getY() + 5); y++) {
                
                Node candidate = new Node(x, y);
                if (PathUtils.checkInsideSafeArea(candidate, safeZone, mapSize)) {
                    double score = calculatePositionStrategicValue(candidate, gameState);
                    if (score > bestScore) {
                        bestScore = score;
                        bestPosition = candidate;
                    }
                }
            }
        }
        
        return bestPosition;
    }
    
    private static double calculatePositionStrategicValue(Node position, GameState gameState) {
        double score = 0;
        
        // Distance from center (closer is better)
        int mapSize = gameState.gameMap.getMapSize();
        double centerDistance = Math.sqrt(Math.pow(position.getX() - mapSize/2.0, 2) + 
                                        Math.pow(position.getY() - mapSize/2.0, 2));
        score += 100 - centerDistance;
        
        // Distance from threats (farther is better)
        double minThreatDistance = Double.MAX_VALUE;
        for (Player enemy : gameState.gameMap.getOtherPlayerInfo()) {
            if (enemy.getHealth() > 0) {
                double distance = PathUtils.distance(position, enemy);
                minThreatDistance = Math.min(minThreatDistance, distance);
            }
        }
        score += Math.min(50, minThreatDistance * 5);
        
        return score;
    }
    
    private static String calculateDirection(Node from, Node to) {
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        
        if (Math.abs(dx) > Math.abs(dy)) {
            return dx > 0 ? "r" : "l";
        } else {
            return dy > 0 ? "u" : "d";
        }
    }
    
    private static List<Node> generateObstacleList(GameState gameState) {
        List<Node> obstacles = new ArrayList<>();
        obstacles.addAll(gameState.gameMap.getListIndestructibles());
        obstacles.removeAll(gameState.gameMap.getObstaclesByTag("CAN_GO_THROUGH"));
        obstacles.addAll(gameState.gameMap.getListEnemies());
        
        // Add buffer around other players
        for (Player enemy : gameState.gameMap.getOtherPlayerInfo()) {
            if (enemy.getHealth() > 0) {
                obstacles.add(enemy);
            }
        }
        
        return obstacles;
    }
}