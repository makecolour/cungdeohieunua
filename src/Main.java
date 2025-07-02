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
    private static final String GAME_ID = "144303";
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
            
            // Check if we're attacking a chest or enemy
            Player target = findTargetInDirection(direction);
            Obstacle chest = findChestInDirection(direction);
            
            switch (weaponType) {
                case "gun":
                    if (gameState.inventory.getGun() != null) {
                        int gunRange = getGunWeaponRange(gameState.inventory.getGun());
                        if (target != null) {
                            int distance = PathUtils.distance(gameState.player, target);
                            if (distance <= gunRange) {
                                hero.shoot(direction);
                                System.out.println("Shooting at enemy with gun (range: " + gunRange + ") at distance " + distance);
                            } else {
                                System.out.println("Enemy too far for gun - distance: " + distance + ", range: " + gunRange);
                            }
                        } else if (chest != null) {
                            int distance = PathUtils.distance(gameState.player, chest);
                            if (distance <= gunRange) {
                                hero.shoot(direction);
                                System.out.println("Shooting at chest with gun (range: " + gunRange + ") at distance " + distance);
                            } else {
                                System.out.println("Chest too far for gun - distance: " + distance + ", range: " + gunRange);
                            }
                        } else {
                            hero.shoot(direction);
                        }
                    }
                    break;
                case "melee":
                    if (gameState.inventory.getMelee() != null) {
                        int meleeRange = getMeleeWeaponRange(gameState.inventory.getMelee());
                        if (target != null) {
                            int distance = PathUtils.distance(gameState.player, target);
                            if (distance <= meleeRange) {
                                hero.attack(direction);
                                System.out.println("Attacking enemy with melee weapon (range: " + meleeRange + ") at distance " + distance);
                            } else {
                                System.out.println("Enemy too far for melee weapon - distance: " + distance + ", range: " + meleeRange);
                            }
                        } else if (chest != null) {
                            int distance = PathUtils.distance(gameState.player, chest);
                            if (distance <= meleeRange) {
                                hero.attack(direction);
                                System.out.println("Attacking chest with melee weapon (range: " + meleeRange + ") at distance " + distance);
                            } else {
                                System.out.println("Chest too far for melee weapon - distance: " + distance + ", range: " + meleeRange);
                            }
                        } else {
                            hero.attack(direction);
                        }
                    }
                    break;
                case "throwable":
                    if (gameState.inventory.getThrowable() != null) {
                        int throwableRange = getThrowableWeaponRange(gameState.inventory.getThrowable());
                        if (target != null) {
                            int distance = PathUtils.distance(gameState.player, target);
                            if (distance <= throwableRange) {
                                hero.throwItem(direction, Math.min(distance, throwableRange));
                                System.out.println("Throwing at enemy (range: " + throwableRange + ") at distance " + distance);
                            } else {
                                System.out.println("Enemy too far for throwable - distance: " + distance + ", range: " + throwableRange);
                            }
                        } else if (chest != null) {
                            int distance = PathUtils.distance(gameState.player, chest);
                            if (distance <= throwableRange) {
                                hero.throwItem(direction, Math.min(distance, throwableRange));
                                System.out.println("Throwing at chest (range: " + throwableRange + ") at distance " + distance);
                            } else {
                                System.out.println("Chest too far for throwable - distance: " + distance + ", range: " + throwableRange);
                            }
                        }
                    }
                    break;
                case "special":
                    if (gameState.inventory.getSpecial() != null) {
                        hero.useSpecial(direction);
                        if (chest != null) {
                            System.out.println("Using special weapon on chest in direction " + direction);
                        }
                    }
                    break;
                default:
                    hero.attack(direction);
                    if (chest != null) {
                        System.out.println("Default attacking chest in direction " + direction);
                    }
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
        // First check for enemy players
        for (Player enemy : gameState.getNearbyEnemies()) {
            String enemyDirection = calculateAttackDirection(gameState.player, enemy);
            if (enemyDirection.equals(direction)) {
                return enemy;
            }
        }
        return null;
    }
    
    private Obstacle findChestInDirection(String direction) {
        // Check for chests in the specified direction within weapon range
        for (Obstacle chest : gameState.gameMap.getListChests()) {
            int distance = PathUtils.distance(gameState.player, chest);
            int maxWeaponRange = getMaxAvailableWeaponRange();
            
            if (distance <= maxWeaponRange) {
                String chestDirection = calculateDirectionToTarget(gameState.player, chest);
                if (chestDirection.equals(direction)) {
                    System.out.println("Found chest at distance " + distance + " in direction " + direction + 
                                     ", type: " + chest.getType() + " (max weapon range: " + maxWeaponRange + ")");
                    return chest;
                }
            }
        }
        System.out.println("No chest found in direction " + direction + " within weapon range");
        return null;
    }
    
    /**
     * Get the maximum attack range of all available weapons
     */
    private int getMaxAvailableWeaponRange() {
        int maxRange = 1; // Default melee range
        
        if (gameState.inventory.getGun() != null) {
            maxRange = Math.max(maxRange, getGunWeaponRange(gameState.inventory.getGun()));
        }
        if (gameState.inventory.getMelee() != null) {
            maxRange = Math.max(maxRange, getMeleeWeaponRange(gameState.inventory.getMelee()));
        }
        if (gameState.inventory.getThrowable() != null) {
            maxRange = Math.max(maxRange, getThrowableWeaponRange(gameState.inventory.getThrowable()));
        }
        if (gameState.inventory.getSpecial() != null) {
            // Special weapons vary in range - estimate based on type
            String specialId = gameState.inventory.getSpecial().getId();
            if ("ROPE".equals(specialId)) {
                maxRange = Math.max(maxRange, 6); // ROPE has 1*6 range
            } else {
                maxRange = Math.max(maxRange, 3); // Default special range
            }
        }
        
        return maxRange;
    }
    
    private String calculateDirectionToTarget(Node from, Node to) {
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        
        // Improved direction calculation to be more precise
        if (Math.abs(dx) == 0 && Math.abs(dy) == 0) {
            return ""; // Same position
        }
        
        if (Math.abs(dx) > Math.abs(dy)) {
            return dx > 0 ? "r" : "l";
        } else {
            return dy > 0 ? "u" : "d";
        }
    }
    
    private String calculateAttackDirection(Node from, Node to) {
        // More precise attack direction calculation
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        
        // Handle diagonal cases better
        if (dx == 0 && dy == 0) return "";
        
        // Prioritize the axis with greater difference
        if (Math.abs(dx) > Math.abs(dy)) {
            return dx > 0 ? "r" : "l";
        } else if (Math.abs(dy) > Math.abs(dx)) {
            return dy > 0 ? "u" : "d";
        } else {
            // Equal distance - choose based on preference or context
            if (dx > 0) return "r";
            if (dx < 0) return "l";
            if (dy > 0) return "u";
            return "d";
        }
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

    private Obstacle findNearbyChest(GameState gameState) {
        List<Obstacle> chests = gameState.gameMap.getListChests();
        Obstacle bestChest = null;
        int minDistance = Integer.MAX_VALUE;
        
        for (Obstacle chest : chests) {
            int distance = PathUtils.distance(gameState.player, chest);
            if (distance <= 8 && distance < minDistance) {
                minDistance = distance;
                bestChest = chest;
            }
        }
        
        return bestChest;
    }
    
    /**
     * Debug method to test chest attack logic
     */
    private void debugChestAttack() {
        try {
            GameMap gameMap = hero.getGameMap();
            if (gameMap != null) {
                Player player = gameMap.getCurrentPlayer();
                if (player != null) {
                    System.out.println("=== CHEST DEBUG ===");
                    System.out.println("Player position: (" + player.getX() + "," + player.getY() + ")");
                    
                    List<Obstacle> chests = gameMap.getListChests();
                    System.out.println("Total chests on map: " + chests.size());
                    
                    for (Obstacle chest : chests) {
                        int distance = PathUtils.distance(player, chest);
                        System.out.println("Chest " + chest.getId() + " at (" + chest.getX() + "," + chest.getY() + 
                                         "), distance: " + distance);
                        
                        if (distance <= 5) {
                            String direction = calculateDirectionToTarget(player, chest);
                            System.out.println("  -> Can attack in direction: " + direction);
                        }
                    }
                    System.out.println("=== END CHEST DEBUG ===");
                }
            }
        } catch (Exception e) {
            System.err.println("Debug error: " + e.getMessage());
        }
    }
    
    /**
     * Get the attack range for a melee weapon based on weapon data from CSV
     */
    private int getMeleeWeaponRange(Weapon meleeWeapon) {
        if (meleeWeapon == null) return 1;
        
        String weaponId = meleeWeapon.getId();
        switch (weaponId) {
            case "KNIFE":
            case "TREE_BRANCH":
            case "AXE":
                return 3; // 3*1 range
            case "MACE":
                return 3; // 3*3 range (diagonal distance)
            case "HAND":
            case "BONE":
                return 1; // 1*1 range
            default:
                return 2; // Default safe range for unknown weapons
        }
    }
    
    /**
     * Get the attack range for a gun weapon
     */
    private int getGunWeaponRange(Weapon gunWeapon) {
        if (gunWeapon == null) return 6;
        
        String weaponId = gunWeapon.getId();
        switch (weaponId) {
            case "SCEPTER":
                return 12; // 1*12 range
            case "CROSSBOW":
                return 8; // 1*8 range
            case "RUBBER_GUN":
                return 6; // 1*6 range
            case "SHOTGUN":
                return 2; // 1*2 range
            default:
                return 6; // Default gun range
        }
    }
    
    /**
     * Get the attack range for a throwable weapon
     */
    private int getThrowableWeaponRange(Weapon throwableWeapon) {
        if (throwableWeapon == null) return 5;
        
        String weaponId = throwableWeapon.getId();
        switch (weaponId) {
            case "BANANA":
            case "METEORITE_FRAGMENT":
            case "CRYSTAL":
                return 6; // 1*6 range
            case "SEED":
                return 5; // 1*5 range
            case "SMOKE":
                return 3; // 1*3 range
            default:
                return 5; // Default throwable range
        }
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
    private static final Map<String, NPCThreatInfo> NPC_THREAT_DATA = new HashMap<>();
    
    static {
        initializeNPCThreatData();
    }
    
    private static void initializeNPCThreatData() {
        // Based on NPCS.csv data - attack range is 3x3 for most NPCs
        NPC_THREAT_DATA.put("NATIVE", new NPCThreatInfo(3, 2, 10, false));
        NPC_THREAT_DATA.put("GHOST", new NPCThreatInfo(3, 2, 10, false));
        NPC_THREAT_DATA.put("LEOPARD", new NPCThreatInfo(3, 2, 5, false)); // Bleed effect
        NPC_THREAT_DATA.put("ANACONDA", new NPCThreatInfo(3, 2, 5, false)); // Poison effect
        NPC_THREAT_DATA.put("RHINO", new NPCThreatInfo(3, 2, 15, false));
        NPC_THREAT_DATA.put("GOLEM", new NPCThreatInfo(3, 2, 15, false)); // Stun effect
        NPC_THREAT_DATA.put("SPIRIT", new NPCThreatInfo(3, 2, 0, true)); // Friendly - heals
    }
    
    public List<Node> getImmediateThreats(GameState gameState) {
        List<Node> threats = new ArrayList<>();
        
        // Calculate NPC danger zones based on their movement patterns and attack ranges
        for (Enemy npc : gameState.gameMap.getListEnemies()) {
            List<Node> dangerZone = calculateNPCDangerZone(npc, gameState);
            threats.addAll(dangerZone);
            System.out.println("NPC " + npc.getId() + " at (" + npc.getX() + "," + npc.getY() + ") creates " + dangerZone.size() + " danger cells");
        }
        
        // Add player threats with weapon ranges
        for (Player enemy : gameState.gameMap.getOtherPlayerInfo()) {
            if (enemy.getHealth() > 0) {
                List<Node> playerDangerZone = calculatePlayerDangerZone(enemy, 8); // Assume max weapon range
                threats.addAll(playerDangerZone);
            }
        }
        
        System.out.println("Total threat cells calculated: " + threats.size());
        return threats;
    }
    
    private List<Node> calculateNPCDangerZone(Enemy npc, GameState gameState) {
        Set<Node> dangerZone = new HashSet<>(); // Use Set to avoid duplicates
        NPCThreatInfo threatInfo = NPC_THREAT_DATA.get(npc.getId());
        
        if (threatInfo == null) {
            // Unknown NPC, assume basic threat with 3x3 attack range
            threatInfo = new NPCThreatInfo(3, 2, 10, false);
        }
        
        // Skip friendly NPCs (SPIRIT heals us)
        if (threatInfo.isFriendly) {
            return new ArrayList<>();
        }
        
        int attackRange = threatInfo.attackRange; // 3x3 = 3
        int speed = threatInfo.speed; // 2 cells per step
        int movementDistance = 9; // NPCs move 9 cells total as mentioned by user
        
        // Model linear movement in one direction (we don't know which direction, so consider worst case)
        // The danger zone will be: movement line (9 cells) × attack width (3 cells) + extensions at ends
        
        // For simplicity, assume NPC moves horizontally (worst case for path planning)
        // Create danger zone: 9 cells long × 3 cells wide, plus 1-cell extensions at both ends
        
        int halfAttackRange = (attackRange - 1) / 2; // 1 cell on each side of center
        
        // Main movement corridor: 9 cells long × 3 cells wide
        for (int pathStep = 0; pathStep < movementDistance; pathStep++) {
            for (int sideOffset = -halfAttackRange; sideOffset <= halfAttackRange; sideOffset++) {
                // For horizontal movement
                int dangerX = npc.getX() + pathStep - (movementDistance / 2); // Center the path around NPC
                int dangerY = npc.getY() + sideOffset;
                
                if (isValidPosition(dangerX, dangerY, gameState)) {
                    dangerZone.add(new Node(dangerX, dangerY));
                }
                
                // Also consider vertical movement possibility
                int dangerX2 = npc.getX() + sideOffset;
                int dangerY2 = npc.getY() + pathStep - (movementDistance / 2);
                
                if (isValidPosition(dangerX2, dangerY2, gameState)) {
                    dangerZone.add(new Node(dangerX2, dangerY2));
                }
            }
        }
        
        // Add 1-cell extensions at the start and end of movement line
        // These account for attack range extending beyond the movement path
        int extensionLength = 1;
        
        // Extensions for horizontal movement
        for (int ext = 1; ext <= extensionLength; ext++) {
            for (int sideOffset = -halfAttackRange; sideOffset <= halfAttackRange; sideOffset++) {
                // Extension at start of path
                int startX = npc.getX() - (movementDistance / 2) - ext;
                int startY = npc.getY() + sideOffset;
                if (isValidPosition(startX, startY, gameState)) {
                    dangerZone.add(new Node(startX, startY));
                }
                
                // Extension at end of path
                int endX = npc.getX() + (movementDistance / 2) + ext;
                int endY = npc.getY() + sideOffset;
                if (isValidPosition(endX, endY, gameState)) {
                    dangerZone.add(new Node(endX, endY));
                }
            }
        }
        
        // Extensions for vertical movement
        for (int ext = 1; ext <= extensionLength; ext++) {
            for (int sideOffset = -halfAttackRange; sideOffset <= halfAttackRange; sideOffset++) {
                // Extension at start of path
                int startX = npc.getX() + sideOffset;
                int startY = npc.getY() - (movementDistance / 2) - ext;
                if (isValidPosition(startX, startY, gameState)) {
                    dangerZone.add(new Node(startX, startY));
                }
                
                // Extension at end of path
                int endX = npc.getX() + sideOffset;
                int endY = npc.getY() + (movementDistance / 2) + ext;
                if (isValidPosition(endX, endY, gameState)) {
                    dangerZone.add(new Node(endX, endY));
                }
            }
        }
        
        return new ArrayList<>(dangerZone);
    }
    
    private List<Node> calculatePlayerDangerZone(Player enemy, int maxWeaponRange) {
        List<Node> dangerZone = new ArrayList<>();
        
        // Players can attack in cardinal directions within weapon range
        // Add danger zones for different weapon types
        int[] dx = {0, 0, 1, -1}; // up, down, right, left
        int[] dy = {1, -1, 0, 0};
        
        for (int dir = 0; dir < 4; dir++) {
            for (int range = 1; range <= maxWeaponRange; range++) {
                int dangerX = enemy.getX() + (dx[dir] * range);
                int dangerY = enemy.getY() + (dy[dir] * range);
                dangerZone.add(new Node(dangerX, dangerY));
            }
        }
        
        return dangerZone;
    }
    
    private boolean isValidPosition(int x, int y, GameState gameState) {
        int mapSize = gameState.gameMap.getMapSize();
        return x >= 0 && x < mapSize && y >= 0 && y < mapSize;
    }

    public double calculateThreatLevel(Node threat, GameState gameState) {
        double level = 0.0;
        
        // Calculate threat from NPCs based on their actual damage and attack ranges
        for (Enemy npc : gameState.gameMap.getListEnemies()) {
            double distance = PathUtils.distance(threat, npc);
            NPCThreatInfo threatInfo = NPC_THREAT_DATA.get(npc.getId());
            
            if (threatInfo != null && !threatInfo.isFriendly) {
                // Higher threat if within attack range
                if (distance <= threatInfo.attackRange) {
                    level += threatInfo.damage * 2.0; // Double threat if in attack range
                } else {
                    level += threatInfo.damage / Math.max(1, distance - threatInfo.attackRange);
                }
            }
        }
        
        // Calculate threat from enemy players
        for (Player enemy : gameState.gameMap.getOtherPlayerInfo()) {
            if (enemy.getHealth() > 0) {
                double distance = PathUtils.distance(threat, enemy);
                double playerThreat = (enemy.getHealth() / 100.0) * 50; // Assume average weapon damage
                level += playerThreat / Math.max(1, distance);
            }
        }
        
        return level;
    }
    
    private static class NPCThreatInfo {
        final int attackRange;
        final int speed;
        final int damage;
        final boolean isFriendly;
        
        NPCThreatInfo(int attackRange, int speed, int damage, boolean isFriendly) {
            this.attackRange = attackRange;
            this.speed = speed;
            this.damage = damage;
            this.isFriendly = isFriendly;
        }
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
        
        // Add static obstacles that can't be passed through
        avoid.addAll(gameState.gameMap.getListIndestructibles());
        avoid.removeAll(gameState.gameMap.getObstaclesByTag("CAN_GO_THROUGH"));
        
        // Add traps
        avoid.addAll(gameState.gameMap.getListTraps());
        
        // CRITICAL: Add NPC and player danger zones using our improved threat assessment
        ThreatAssessment threatAssessment = new ThreatAssessment();
        List<Node> dangerZones = threatAssessment.getImmediateThreats(gameState);
        avoid.addAll(dangerZones);
        
        // Add direct enemy positions
        avoid.addAll(gameState.gameMap.getListEnemies());
        
        // Add buffer zones around other players (1 cell buffer)
        for (Player otherPlayer : gameState.gameMap.getOtherPlayerInfo()) {
            if (otherPlayer.getHealth() > 0) {
                avoid.add(otherPlayer); // Add the player position itself
                
                // Add 1-cell buffer around player
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        if (dx == 0 && dy == 0) continue; // Skip center (already added)
                        
                        int bufferX = otherPlayer.getX() + dx;
                        int bufferY = otherPlayer.getY() + dy;
                        if (bufferX >= 0 && bufferX < gameState.gameMap.getMapSize() && 
                            bufferY >= 0 && bufferY < gameState.gameMap.getMapSize()) {
                            avoid.add(new Node(bufferX, bufferY));
                        }
                    }
                }
            }
        }
        
        System.out.println("Pathfinding avoiding " + avoid.size() + " dangerous cells (including " + dangerZones.size() + " NPC danger zones)");
        return avoid;
    }
    
    private double calculatePositionSafety(Node start, GameState gameState) {
        double safety = 100.0;
        
        // Penalty for being near threats
        for (Node threat : gameState.gameMap.getListEnemies()) {
            int distance = PathUtils.distance(start, threat);
            safety -= Math.max(0, 50 - distance * 5);
        }
        
        for (Player otherPlayer : gameState.gameMap.getOtherPlayerInfo()) {
            if (otherPlayer.getHealth() > 0) {
                int distance = PathUtils.distance(start, otherPlayer);
                safety -= Math.max(0, 40 - distance * 4);
            }
        }
        
        // Bonus for being in safe zone
        if (PathUtils.checkInsideSafeArea(start, gameState.gameMap.getSafeZone(), gameState.gameMap.getMapSize())) {
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
        // Multi-criteria decision analysis with game-specific tactics
        double survivalScore = calculateSurvivalScore(gameState);
        double combatScore = calculateCombatScore(gameState);
        double resourceScore = calculateResourceScore(gameState);
        double positionScore = calculatePositionScore(gameState);
        double dragonEggScore = calculateDragonEggThreat(gameState);
        double killStreakOpportunity = calculateKillStreakOpportunity(gameState);
        
        System.out.println("Decision Analysis - Survival: " + survivalScore + 
                          ", Combat: " + combatScore + 
                          ", Resource: " + resourceScore + 
                          ", Position: " + positionScore +
                          ", DragonEgg: " + dragonEggScore +
                          ", KillStreak: " + killStreakOpportunity);
        
        // Dragon Egg Threat - Highest Priority (avoid instant death)
        if (dragonEggScore > 0.8) {
            System.out.println("Decision: EMERGENCY_DRAGON_EGG_AVOIDANCE");
            return DecisionType.EMERGENCY_SURVIVAL;
        }
        
        // Late-game survival prioritization (last 45 seconds = no respawn)
        if (isLateGame(gameState) && survivalScore < 0.6) {
            System.out.println("Decision: LATE_GAME_SURVIVAL");
            return DecisionType.EMERGENCY_SURVIVAL;
        }
        
        // Kill streak opportunity - high scoring potential
        if (killStreakOpportunity > 0.7 && survivalScore > 0.4) {
            System.out.println("Decision: KILL_STREAK_HUNTING");
            return DecisionType.AGGRESSIVE_COMBAT;
        }
        
        // Emergency survival - dark zone or critical health
        if (survivalScore < 0.3) {
            System.out.println("Decision: EMERGENCY_SURVIVAL");
            return DecisionType.EMERGENCY_SURVIVAL;
        }
        
        // Combat when well-equipped and healthy
        if (combatScore > 0.6 && survivalScore > 0.5) {
            System.out.println("Decision: AGGRESSIVE_COMBAT");
            return DecisionType.AGGRESSIVE_COMBAT;
        }
        
        // Prioritize high-value resources (Dragon Eggs, Premium Weapons)
        if (resourceScore > 0.25) {
            System.out.println("Decision: RESOURCE_GATHERING (targeting valuable loot!)");
            return DecisionType.RESOURCE_GATHERING;
        }
        
        // Positioning for tactical advantage
        if (positionScore < 0.4) {
            System.out.println("Decision: REPOSITIONING");
            return DecisionType.REPOSITIONING;
        }
        
        System.out.println("Decision: EXPLORATION");
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
        Node playerPos = gameState.player;
        
        // DRAGON EGG - HIGHEST VALUE (premium items: MACE, COMPASS)
        for (Obstacle chest : gameState.gameMap.getListChests()) {
            double distance = PathUtils.distance(playerPos, chest);
            if (distance <= 15) {
                double chestValue;
                if (chest.getId().equals("DRAGON_EGG")) {
                    chestValue = 500.0; // Extremely high value - contains premium items
                } else if (chest.getId().equals("CHEST")) {
                    chestValue = 200.0; // High value but less than dragon egg
                } else {
                    chestValue = 150.0; // Standard chest value
                }
                
                double chestScore = chestValue / Math.max(1, distance);
                score += chestScore / 100.0;
                System.out.println("Chest " + chest.getId() + " contributes " + (chestScore/100.0) + " to resource score");
            }
        }
        
        // PREMIUM WEAPONS (from game design documents)
        for (Weapon weapon : gameState.gameMap.getListWeapons()) {
            double distance = PathUtils.distance(playerPos, weapon);
            if (distance <= 10) {
                double weaponValue = calculateWeaponValue(weapon);
                double weaponScore = weaponValue / Math.max(1, distance);
                score += weaponScore / 100.0;
            }
        }
        
        // PREMIUM HEALING/SUPPORTING ITEMS
        for (HealingItem healing : gameState.gameMap.getListHealingItems()) {
            double distance = PathUtils.distance(playerPos, healing);
            if (distance <= 8 && gameState.inventory.getListHealingItem().size() < 5) {
                double healingValue = calculateHealingValue(healing);
                double healingScore = healingValue / Math.max(1, distance);
                score += healingScore / 50.0;
            }
        }
        
        // ARMOR with damage reduction
        for (Armor armor : gameState.gameMap.getListArmors()) {
            double distance = PathUtils.distance(playerPos, armor);
            if (distance <= 8) {
                double armorValue = calculateArmorValue(armor);
                double armorScore = armorValue / Math.max(1, distance);
                score += armorScore / 75.0;
            }
        }
        
        System.out.println("Total resource score: " + score);
        return Math.min(1.0, score);
    }
    
    private static double calculateWeaponValue(Weapon weapon) {
        // Premium weapons based on game design documents
        switch (weapon.getId()) {
            case "MACE": return 300.0; // Premium thính item, AoE stun
            case "SAHUR_BAT": return 250.0; // Knock back special weapon
            case "ROPE": return 200.0; // Pull mechanic special weapon
            case "BELL": return 200.0; // Reverse effect special weapon
            case "SHOTGUN": return 180.0; // High damage gun
            case "AXE": return 150.0; // High damage melee
            case "CROSSBOW": return 140.0; // Good range gun
            case "SCEPTER": return 130.0; // Magic weapon
            case "CRYSTAL": return 120.0; // High damage throwable
            case "METEORITE_FRAGMENT": return 110.0; // Good throwable
            case "KNIFE": return 100.0; // Decent melee
            case "SEED": return 90.0; // Stun throwable
            case "BANANA": return 80.0; // Trap creation
            case "SMOKE": return 70.0; // Stealth/blind
            case "BONE": return 60.0; // Basic melee
            case "RUBBER_GUN": return 50.0; // Basic gun
            case "TREE_BRANCH": return 30.0; // Weak melee
            default: return weapon.getPickupPoints(); // Fallback to pickup points
        }
    }
    
    private static double calculateHealingValue(HealingItem healing) {
        // Premium healing items based on game design documents
        switch (healing.getId()) {
            case "ELIXIR_OF_LIFE": return 500.0; // Revival effect - extremely valuable
            case "COMPASS": return 400.0; // AoE stun - premium thính item
            case "ELIXIR": return 300.0; // Control immunity
            case "MAGIC": return 250.0; // Invisibility
            case "UNICORN_BLOOD": return 200.0; // High HP healing
            case "PHOENIX_FEATHERS": return 150.0; // Good HP healing
            case "MERMAID_TAIL": return 100.0; // Medium HP healing
            case "SPIRIT_TEAR": return 75.0; // Low-medium HP healing
            case "GOD_LEAF": return 50.0; // Basic HP healing
            default: return healing.getPoint(); // Fallback to point value
        }
    }
    
    private static double calculateArmorValue(Armor armor) {
        // Armor value based on HP and damage reduction
        switch (armor.getId()) {
            case "MAGIC_ARMOR": return 200.0; // 75 HP, 30% reduction
            case "MAGIC_HELMET": return 150.0; // 50 HP, 20% reduction
            case "ARMOR": return 120.0; // 50 HP, 20% reduction
            case "WOODEN_HELMET": return 80.0; // 20 HP, 5% reduction
            default: return armor.getHealthPoint(); // Fallback to HP value
        }
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
    
    private static double calculateDragonEggThreat(GameState gameState) {
        // Check if dragon is flying (indicates dragon egg will drop soon)
        // This is critical to avoid instant death
        // TODO: Implement dragon detection logic when available in SDK
        // For now, return 0 (no immediate threat)
        return 0.0;
    }
    
    private static double calculateKillStreakOpportunity(GameState gameState) {
        double opportunity = 0.0;
        
        // Check for nearby vulnerable enemies
        List<Player> enemies = gameState.getNearbyEnemies();
        for (Player enemy : enemies) {
            // Prioritize low-health enemies for easy kills
            if (enemy.getHealth() < 30) {
                opportunity += 0.3;
            }
            
            // Check if we have weapon advantage
            if (gameState.inventory.getGun() != null && 
                PathUtils.distance(gameState.player, enemy) <= 8) {
                opportunity += 0.2;
            }
            
            // Melee advantage at close range
            if (gameState.inventory.getMelee() != null && 
                !gameState.inventory.getMelee().getId().equals("HAND") &&
                PathUtils.distance(gameState.player, enemy) <= 3) {
                opportunity += 0.3;
            }
        }
        
        // Our health factor - don't engage if too low
        opportunity *= (gameState.player.getHealth() / 100.0);
        
        return Math.min(1.0, opportunity);
    }
    
    private static boolean isLateGame(GameState gameState) {
        // Determine if we're in the last 45 seconds (no respawn period)
        // TODO: Add game time tracking when available in SDK
        // For now, use enemy count as proxy
        int enemyCount = gameState.gameMap.getOtherPlayerInfo().size();
        return enemyCount <= 3; // Likely late game with few players left
    }
    
    private static double calculateTimeBasedScore(GameState gameState) {
        // Calculate score based on game time and phase
        int enemyCount = gameState.gameMap.getOtherPlayerInfo().size();
        
        if (enemyCount <= 2) {
            // Very late game - survival is paramount
            return 0.9;
        } else if (enemyCount <= 4) {
            // Late game - be more cautious
            return 0.7;
        } else {
            // Early/mid game - more aggressive
            return 0.3;
        }
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
            String weaponChoice = selectOptimalWeaponForTarget(gameState, target);
            
            // Advanced combat tactics
            int distance = PathUtils.distance(gameState.player, target);
            
            // Kiting strategy for ranged weapons
           
            if (weaponChoice.equals("gun") && distance <= 2) {
                // Too close for guns - retreat while shooting
                String retreatDirection = getRetreatDirection(gameState, target);
                actions.add("move:" + retreatDirection);
                actions.add("attack:" + weaponChoice + ":" + direction);
            }
            // Special weapon tactics
            else if (weaponChoice.equals("special")) {
                String specialWeaponId = gameState.inventory.getSpecial().getId();
                switch (specialWeaponId) {
                    case "ROPE":
                        // Pull enemy closer for melee follow-up
                        actions.add("attack:special:" + direction);
                        if (gameState.inventory.getMelee() != null) {
                            actions.add("attack:melee:" + direction);
                        }
                        break;
                    case "SAHUR_BAT":
                        // Knock back - position near obstacles for stun
                        actions.add("attack:special:" + direction);
                        break;
                    case "BELL":
                        // Reverse controls in AoE
                        actions.add("attack:special:" + direction);
                        break;
                    default:
                        actions.add("attack:" + weaponChoice + ":" + direction);
                        break;
                }
            }
            // Standard attack
            else {
                actions.add("attack:" + weaponChoice + ":" + direction);
            }
            
            // Plan follow-up positioning
            String repositionMove = planCombatRepositioning(gameState, target);
            if (repositionMove != null) {
                actions.add("move:" + repositionMove);
            }
        }
        
        return actions;
    }
    
    private static String selectOptimalWeaponForTarget(GameState gameState, Player target) {
        int distance = PathUtils.distance(gameState.player, target);
        
        // Special weapons have unique tactical advantages
        if (gameState.inventory.getSpecial() != null) {
            String specialId = gameState.inventory.getSpecial().getId();
            switch (specialId) {
                case "MACE":
                    if (distance <= 3) return "special"; // AoE stun attack
                    break;
                case "ROPE":
                    if (distance <= 6) return "special"; // Pull + stun combo
                    break;
                case "SAHUR_BAT":
                    if (distance <= 5) return "special"; // Knock back + potential stun
                    break;
                case "BELL":
                    if (distance <= 7) return "special"; // AoE reverse controls
                    break;
            }
        }
        
        // Gun tactics - medium to long range
        if (gameState.inventory.getGun() != null && distance >= 3 && distance <= 8) {
            return "gun";
        }
        
        // Throwable tactics - can cause area effects
        if (gameState.inventory.getThrowable() != null && distance >= 2 && distance <= 6) {
            String throwableId = gameState.inventory.getThrowable().getId();
            switch (throwableId) {
                case "SEED":
                    return "throwable"; // Stun effect
                case "SMOKE":
                    return "throwable"; // Blind + invisibility
                case "BANANA":
                    return "throwable"; // Creates trap
                default:
                    if (distance >= 4) return "throwable"; // Safe distance for explosives
                    break;
            }
        }
        
        // Melee for close combat
        if (gameState.inventory.getMelee() != null && distance <= 3) {
            return "melee";
        }
        
        return "melee"; // Default fallback
    }
    
    private static String getRetreatDirection(GameState gameState, Player target) {
        int dx = gameState.player.getX() - target.getX();
        int dy = gameState.player.getY() - target.getY();
        
        // Move away from target
        if (Math.abs(dx) > Math.abs(dy)) {
            return dx > 0 ? "r" : "l";
        } else {
            return dy > 0 ? "u" : "d";
        }
    }
    
    private static List<String> planResourceGathering(GameState gameState) {
        List<String> actions = new ArrayList<>();
        
        // Check for nearby chests to attack first - this is HIGH PRIORITY
        Obstacle nearbyChest = findNearbyChest(gameState);
        if (nearbyChest != null) {
            int distance = PathUtils.distance(gameState.player, nearbyChest);
            String weaponChoice = selectBestWeaponForChest(gameState);
            int weaponRange = getWeaponRange(gameState, weaponChoice);
            
            System.out.println("Found nearby chest: " + nearbyChest.getId() + " at distance " + distance + 
                             ", selected weapon: " + weaponChoice + " (range: " + weaponRange + ")");
            
            if (distance <= weaponRange) {
                // Attack chest directly - we're within weapon range
                String direction = calculateDirection(gameState.player, nearbyChest);
                actions.add("attack:" + weaponChoice + ":" + direction);
                System.out.println("Planning to attack chest with " + weaponChoice + " in direction " + direction);
                return actions;
            } else if (distance <= 10) {
                // Move closer to chest - it's our priority target
                String path = PathUtils.getShortestPath(gameState.gameMap, 
                    generateObstacleList(gameState), gameState.player, nearbyChest, false);
                if (path != null && !path.isEmpty()) {
                    actions.add("move:" + path.substring(0, 1));
                    System.out.println("Moving towards chest: " + path.substring(0, 1) + " (need to get within range " + weaponRange + ")");
                    return actions;
                }
            }
        }
        
        // If no chests nearby, look for other resources
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
    
    private static Obstacle findNearbyChest(GameState gameState) {
        List<Obstacle> chests = gameState.gameMap.getListChests();
        Obstacle bestChest = null;
        int minDistance = Integer.MAX_VALUE;
        
        System.out.println("Searching for chests. Total chests available: " + chests.size());
        
        for (Obstacle chest : chests) {
            int distance = PathUtils.distance(gameState.player, chest);
            System.out.println("Chest " + chest.getId() + " at (" + chest.getX() + "," + chest.getY() + "), distance: " + distance);
            
            if (distance <= 15 && distance < minDistance) { // Increased search range
                minDistance = distance;
                bestChest = chest;
                System.out.println("Selected as best chest candidate: " + chest.getId() + " at distance " + distance);
            }
        }
        
        if (bestChest != null) {
            System.out.println("Best chest found: " + bestChest.getId() + " at distance " + minDistance);
        } else {
            System.out.println("No suitable chest found within range");
        }
        
        return bestChest;
    }
    
    private static String selectBestWeaponForChest(GameState gameState) {
        // For chests, prefer weapons with good damage
        if (gameState.inventory.getMelee() != null && 
            !gameState.inventory.getMelee().getId().equals("HAND")) {
            // Use melee weapons for close-range chest breaking
            String meleeId = gameState.inventory.getMelee().getId();
            if (meleeId.equals("AXE") || meleeId.equals("KNIFE") || meleeId.equals("BONE")) {
                return "melee";
            }
        }
        
        if (gameState.inventory.getGun() != null) {
            return "gun";
        }
        
        if (gameState.inventory.getThrowable() != null) {
            return "throwable";
        }
        
        return "melee"; // Use hand as last resort
    }
    
    private static List<String> planRepositioning(GameState gameState) {
        List<String> actions = new ArrayList<>();
        
        // Priority 1: Get to safe zone if in dark zone
        if (!PathUtils.checkInsideSafeArea(gameState.player, gameState.gameMap.getSafeZone(), gameState.gameMap.getMapSize())) {
            Node safePosition = findOptimalSafeZonePosition(gameState);
            if (safePosition != null) {
                String path = PathUtils.getShortestPath(gameState.gameMap, 
                    generateObstacleList(gameState), gameState.player, safePosition, false);
                
                if (path != null && !path.isEmpty()) {
                    actions.add("move:" + path.substring(0, 1));
                    System.out.println("Emergency move to safe zone: " + path.substring(0, 1));
                    return actions;
                }
            }
        }
        
        // Priority 2: Position near valuable resources but maintain safety
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
    
    private static Node findOptimalSafeZonePosition(GameState gameState) {
        // Find the best position within the safe zone
        int mapSize = gameState.gameMap.getMapSize();
        int safeZone = gameState.gameMap.getSafeZone();
        
        // Position slightly inside safe zone boundary for buffer
        int bufferDistance = Math.max(2, safeZone / 10); // 10% buffer or minimum 2 cells
        int targetRadius = safeZone - bufferDistance;
        
        // Prefer center of safe zone for maximum safety
        int centerX = mapSize / 2;
        int centerY = mapSize / 2;
        
        return new Node(centerX, centerY);
    }
    
    private static Node findStrategicPosition(GameState gameState) {
        // Advanced positioning strategy
        Node bestPosition = null;
        double bestScore = -1;
        
        int mapSize = gameState.gameMap.getMapSize();
        int searchRadius = Math.min(10, mapSize / 4);
        
        // Search in a radius around current position
        for (int dx = -searchRadius; dx <= searchRadius; dx++) {
            for (int dy = -searchRadius; dy <= searchRadius; dy++) {
                int newX = gameState.player.getX() + dx;
                int newY = gameState.player.getY() + dy;
                
                // Check bounds
                if (newX < 0 || newX >= mapSize || newY < 0 || newY >= mapSize) {
                    continue;
                }
                
                Node candidate = new Node(newX, newY);
                double score = calculatePositionScore(candidate, gameState);
                
                if (score > bestScore) {
                    bestScore = score;
                    bestPosition = candidate;
                }
            }
        }
        
        return bestPosition;
    }
    
    private static double calculatePositionScore(Node position, GameState gameState) {
        double score = 0.0;
        
        // Safe zone factor (most important)
        if (PathUtils.checkInsideSafeArea(position, gameState.gameMap.getSafeZone(), gameState.gameMap.getMapSize())) {
            score += 50.0;
        } else {
            return 0.0; // Never go outside safe zone unless already there
        }
        
        // Distance to valuable resources
        for (Obstacle chest : gameState.gameMap.getListChests()) {
            int distance = PathUtils.distance(position, chest);
            if (distance <= 8) {
                double chestValue = chest.getId().equals("DRAGON_EGG") ? 30.0 : 20.0;
                score += chestValue / Math.max(1, distance);
            }
        }
        
        // Distance from enemies (safety)
        for (Player enemy : gameState.gameMap.getOtherPlayerInfo()) {
            if (enemy.getHealth() > 0) {
                int distance = PathUtils.distance(position, enemy);
                if (distance <= 3) {
                    score -= 20.0; // Too close to enemy
                } else if (distance <= 6) {
                    score += 5.0; // Good engagement distance
                } else {
                    score += 2.0; // Safe distance
                }
            }
        }
        
        // Cover and tactical advantage
        score += calculateCoverScore(position, gameState);
        
        return score;
    }
    
    private static double calculateCoverScore(Node position, GameState gameState) {
        double coverScore = 0.0;
        
        // Count nearby obstacles that can provide cover
        int coverCount = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                if (dx == 0 && dy == 0) continue;
                
                int checkX = position.getX() + dx;
                int checkY = position.getY() + dy;
                
                // Check if there's an obstacle at this position
                for (Obstacle obstacle : gameState.gameMap.getListObstacles()) {
                    if (obstacle.getX() == checkX && obstacle.getY() == checkY) {
                        // Prefer destructible cover (can be cleared) over indestructible
                        if (obstacle.getId().equals("CHEST") || obstacle.getId().equals("DRAGON_EGG")) {
                            coverCount += 2; // Chests provide valuable cover
                        } else {
                            coverCount += 1;
                        }
                        break;
                    }
                }
            }
        }
        
        // Optimal cover is 2-4 nearby obstacles
        if (coverCount >= 2 && coverCount <= 4) {
            coverScore = 10.0;
        } else if (coverCount >= 1) {
            coverScore = 5.0;
        }
        
        return coverScore;
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
        
        // Add all indestructible obstacles (except those we can go through)
        for (Obstacle obstacle : gameState.gameMap.getListObstacles()) {
            // Skip destructible obstacles (chests) as they can be attacked
            if (!obstacle.getId().equals("CHEST") && !obstacle.getId().equals("DRAGON_EGG")) {
                obstacles.add(obstacle);
            }
        }
        
        // Remove obstacles we can pass through
        obstacles.removeAll(gameState.gameMap.getObstaclesByTag("CAN_GO_THROUGH"));
        
        // CRITICAL: Add NPC danger zones based on their attack ranges and movement patterns
        ThreatAssessment threatAssessment = new ThreatAssessment();
        List<Node> npcDangerZones = threatAssessment.getImmediateThreats(gameState);
        obstacles.addAll(npcDangerZones);
        
        // Add enemy players as obstacles for pathfinding
        for (Player enemy : gameState.gameMap.getOtherPlayerInfo()) {
            if (enemy.getHealth() > 0) {
                obstacles.add(enemy);
            }
        }
        
        // Add traps
        obstacles.addAll(gameState.gameMap.getListTraps());
        
        System.out.println("Strategic pathfinding using " + obstacles.size() + " obstacles (including " + npcDangerZones.size() + " NPC danger cells)");
        return obstacles;
    }
    
    /**
     * Get the range of a weapon based on its type
     */
    private static int getWeaponRange(GameState gameState, String weaponType) {
        switch (weaponType) {
            case "gun":
                if (gameState.inventory.getGun() != null) {
                    return getGunWeaponRangeStatic(gameState.inventory.getGun());
                }
                return 6; // Default gun range
            case "melee":
                if (gameState.inventory.getMelee() != null) {
                    return getMeleeWeaponRangeStatic(gameState.inventory.getMelee());
                }
                return 1; // Default melee range
            case "throwable":
                if (gameState.inventory.getThrowable() != null) {
                    return getThrowableWeaponRangeStatic(gameState.inventory.getThrowable());
                }
                return 5; // Default throwable range
            case "special":
                if (gameState.inventory.getSpecial() != null) {
                    String specialId = gameState.inventory.getSpecial().getId();
                    if ("ROPE".equals(specialId)) {
                        return 6; // ROPE has 1*6 range
                    } else {
                        return 3; // Default special range
                    }
                }
                return 3;
            default:
                return 1;
        }
    }
    
    /**
     * Static version of getMeleeWeaponRange for use in static methods
     */
    private static int getMeleeWeaponRangeStatic(Weapon meleeWeapon) {
        if (meleeWeapon == null) return 1;
        
        String weaponId = meleeWeapon.getId();
        switch (weaponId) {
            case "KNIFE":
            case "TREE_BRANCH":
            case "AXE":
                return 3; // 3*1 range
            case "MACE":
                return 3; // 3*3 range (diagonal distance)
            case "HAND":
            case "BONE":
                return 1; // 1*1 range
            default:
                return 2; // Default safe range for unknown weapons
        }
    }
    
    /**
     * Static version of getGunWeaponRange for use in static methods
     */
    private static int getGunWeaponRangeStatic(Weapon gunWeapon) {
        if (gunWeapon == null) return 6;
        
        String weaponId = gunWeapon.getId();
        switch (weaponId) {
            case "SCEPTER":
                return 12; // 1*12 range
            case "CROSSBOW":
                return 8; // 1*8 range
            case "RUBBER_GUN":
                return 6; // 1*6 range
            case "SHOTGUN":
                return 2; // 1*2 range
            default:
                return 6; // Default gun range
        }
    }
    
    /**
     * Static version of getThrowableWeaponRange for use in static methods
     */
    private static int getThrowableWeaponRangeStatic(Weapon throwableWeapon) {
        if (throwableWeapon == null) return 5;
        
        String weaponId = throwableWeapon.getId();
        switch (weaponId) {
            case "BANANA":
            case "METEORITE_FRAGMENT":
            case "CRYSTAL":
                return 6; // 1*6 range
            case "SEED":
                return 5; // 1*5 range
            case "SMOKE":
                return 3; // 1*3 range
            default:
                return 5; // Default throwable range
        }
    }
}