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
    private static final String GAME_ID = "184039";
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
    private StrategyManager strategyManager;
    private DecisionTree decisionTree;
    private Map<String, Integer> memoization;

    public IntelligentBot(Hero hero) {
        this.hero = hero;
        this.gameState = new GameState();
        this.strategyManager = new StrategyManager();
        this.decisionTree = new DecisionTree();
        this.decisionTree.setHero(hero); // Set hero reference
        this.memoization = new HashMap<>();
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

            // Update game state
            gameState.update(gameMap, player, hero);

            // Make intelligent decision using decision tree
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

class DecisionTree {
    public Action makeDecision(GameState state) {
        // Priority-based decision tree with dynamic programming optimization

        // 1. Survival priority - heal if critically low health
        if (state.getCurrentPlayer().getHealth() <= 30) {
            Action healAction = findBestHealingAction(state);
            if (healAction != null) return healAction;
        }

        // 2. Safety priority - move to safe zone if outside and zone is shrinking
        if (!state.isInSafeZone()) {
            Action safetyAction = moveToSafeZone(state);
            if (safetyAction != null) return safetyAction;
        }

        // 3. Combat priority - engage enemies based on threat level and equipment
        if (state.getThreatLevel() == ThreatLevel.HIGH || state.getThreatLevel() == ThreatLevel.MEDIUM) {
            Action combatAction = decideCombatAction(state);
            if (combatAction != null) return combatAction;
        }

        // 4. Equipment optimization - get better weapons/armor
        Action equipmentAction = optimizeEquipment(state);
        if (equipmentAction != null) return equipmentAction;

        // 5. Positioning strategy - move to advantageous positions
        Action positionAction = strategicPositioning(state);
        if (positionAction != null) return positionAction;

        // 6. Default action - explore safely
        return exploreMap(state);
    }

    private Action findBestHealingAction(GameState state) {
        // Find best healing item based on health deficit and item efficiency
        try {
            List<HealingItem> healingItems = hero.getInventory().getListHealingItem();

            if (healingItems != null && !healingItems.isEmpty()) {
                // Use most efficient healing item first
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

        // Look for healing items on map
        List<HealingItem> mapHealingItems = state.getHealingItems();
        if (mapHealingItems != null && !mapHealingItems.isEmpty()) {
            HealingItem nearest = findNearestSafeItem(mapHealingItems, state);
            if (nearest != null) {
                String path = PathUtils.getShortestPath(
                    state.getGameMap(),
                    getNodesToAvoid(state),
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
        // Calculate efficiency based on healing amount vs usage time vs current need
        int healthDeficit = 100 - currentHealth;
        int healAmount = Math.min(item.getHealingHP(), healthDeficit);

        // Consider usage time (prefer faster healing in combat)
        double efficiency = (double) healAmount / (item.getUsageTime() + 1);

        return (int) (efficiency * 100);
    }

    private Action moveToSafeZone(GameState state) {
        int mapSize = state.getGameMap().getMapSize();

        // Calculate center of safe zone
        Node safeCenter = new Node(mapSize / 2, mapSize / 2);

        String path = PathUtils.getShortestPath(
            state.getGameMap(),
            getNodesToAvoid(state),
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
        // Smart combat decision based on weapon type, enemy position, and tactical advantage
        Player nearestEnemy = findNearestThreat(state);
        if (nearestEnemy == null) return null;

        String direction = calculateDirection(state.getCurrentPlayer(), nearestEnemy);
        int distance = PathUtils.distance(state.getCurrentPlayer(), nearestEnemy);

        try {
            // Choose weapon based on distance and situation
            if (hero.getInventory().getGun() != null && distance > 2) {
                return new Action(ActionType.ATTACK, direction);
            } else if (hero.getInventory().getThrowable() != null && distance > 1 && distance <= 6) {
                return new Action(ActionType.THROW, direction, Math.min(distance, 6));
            } else if (hero.getInventory().getMelee() != null && distance <= 3) {
                return new Action(ActionType.ATTACK, direction);
            } else if (hero.getInventory().getSpecial() != null) {
                return new Action(ActionType.USE_SPECIAL, direction);
            }
        } catch (Exception e) {
            System.out.println("Error in combat decision: " + e.getMessage());
        }

        return null;
    }

    private Action optimizeEquipment(GameState state) {
        // Prioritize weapon acquisition based on current inventory and available weapons
        try {
            // Priority: Gun > Special > Throwable > Better Melee
            if (hero.getInventory().getGun() == null) {
                List<Weapon> guns = state.getAvailableWeapons().stream()
                    .filter(w -> "Guns".equals(w.getType()))
                    .collect(Collectors.toList());

                if (!guns.isEmpty()) {
                    Weapon bestGun = findBestWeapon(guns, state);
                    if (bestGun != null) {
                        return createMoveToItemAction(bestGun, state);
                    }
                }
            }

            // Look for special weapons
            if (hero.getInventory().getSpecial() == null) {
                List<Weapon> specials = state.getAvailableWeapons().stream()
                    .filter(w -> "Specials".equals(w.getType()))
                    .collect(Collectors.toList());

                if (!specials.isEmpty()) {
                    Weapon bestSpecial = findBestWeapon(specials, state);
                    if (bestSpecial != null) {
                        return createMoveToItemAction(bestSpecial, state);
                    }
                }
            }

            // Look for throwable weapons
            if (hero.getInventory().getThrowable() == null) {
                List<Weapon> throwables = state.getAvailableWeapons().stream()
                    .filter(w -> "Throwables".equals(w.getType()))
                    .collect(Collectors.toList());

                if (!throwables.isEmpty()) {
                    Weapon bestThrowable = findBestWeapon(throwables, state);
                    if (bestThrowable != null) {
                        return createMoveToItemAction(bestThrowable, state);
                    }
                }
            }

            // Look for armor if not equipped
            if (state.getArmors() != null && !state.getArmors().isEmpty()) {
                Armor bestArmor = state.getArmors().stream()
                    .max(Comparator.comparingDouble(this::calculateArmorValue))
                    .orElse(null);

                if (bestArmor != null) {
                    return createMoveToItemAction(bestArmor, state);
                }
            }
        } catch (Exception e) {
            System.out.println("Error in equipment optimization: " + e.getMessage());
        }

        return null;
    }

    private Action strategicPositioning(GameState state) {
        // Move to positions that provide tactical advantage
        Node bestPosition = findBestTacticalPosition(state);
        if (bestPosition != null) {
            String path = PathUtils.getShortestPath(
                state.getGameMap(),
                getNodesToAvoid(state),
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

    private Action exploreMap(GameState state) {
        // Safe exploration to find items and maintain map control
        List<Node> interestingNodes = new ArrayList<>();

        // Add weapon locations
        if (state.getAvailableWeapons() != null) {
            interestingNodes.addAll(state.getAvailableWeapons());
        }

        // Add healing item locations
        if (state.getHealingItems() != null) {
            interestingNodes.addAll(state.getHealingItems());
        }

        // Add ally locations (for healing)
        if (state.getFriendlyNPCs() != null) {
            interestingNodes.addAll(state.getFriendlyNPCs());
        }

        if (!interestingNodes.isEmpty()) {
            Node target = interestingNodes.stream()
                .min(Comparator.comparingInt(node -> PathUtils.distance(state.getCurrentPlayer(), node)))
                .orElse(null);

            if (target != null) {
                String path = PathUtils.getShortestPath(
                    state.getGameMap(),
                    getNodesToAvoid(state),
                    state.getCurrentPlayer(),
                    target,
                    false
                );

                if (path != null && !path.isEmpty()) {
                    return new Action(ActionType.MOVE, path);
                }
            }
        }

        return new Action(ActionType.WAIT);
    }

    // Helper methods
    private List<Node> getNodesToAvoid(GameState state) {
        List<Node> nodes = new ArrayList<>();

        try {
            // Add indestructible obstacles
            var indestructibles = state.getGameMap().getListIndestructibles();
            if (indestructibles != null) {
                nodes.addAll(indestructibles);
            }

            // Add other players (but allow attacking them)
            if (state.getEnemies() != null) {
                nodes.addAll(state.getEnemies());
            }

            // Add dangerous NPCs
            if (state.getHostileNPCs() != null) {
                nodes.addAll(state.getHostileNPCs());
            }

            // Remove nodes we can go through
            var passableNodes = state.getGameMap().getObstaclesByTag("CAN_GO_THROUGH");
            if (passableNodes != null) {
                nodes.removeAll(passableNodes);
            }
        } catch (Exception e) {
            System.out.println("Error building avoidance list: " + e.getMessage());
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

    private Weapon findBestWeapon(List<Weapon> weapons, GameState state) {
        return weapons.stream()
            .min(Comparator.comparingInt(weapon -> PathUtils.distance(state.getCurrentPlayer(), weapon)))
            .orElse(null);
    }

    private double calculateArmorValue(Armor armor) {
        // Simple armor value calculation - fixed method name
        return armor.getHealthPoint() + (armor.getDamageReduce() * 10);
    }

    private Action createMoveToItemAction(Node item, GameState state) {
        String path = PathUtils.getShortestPath(
            state.getGameMap(),
            getNodesToAvoid(state),
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

    private <T extends Node> T findNearestSafeItem(List<T> items, GameState state) {
        return items.stream()
            .filter(item -> PathUtils.checkInsideSafeArea(item, state.getGameMap().getSafeZone(), state.getGameMap().getMapSize()))
            .min(Comparator.comparingInt(item -> PathUtils.distance(state.getCurrentPlayer(), item)))
            .orElse(items.stream()
                .min(Comparator.comparingInt(item -> PathUtils.distance(state.getCurrentPlayer(), item)))
                .orElse(null));
    }

    private Node findBestTacticalPosition(GameState state) {
        // Advanced tactical positioning algorithm
        int mapSize = state.getGameMap().getMapSize();
        Node currentPos = state.getCurrentPlayer();

        // Look for positions that are in safe zone and accessible
        for (int x = Math.max(0, currentPos.getX() - 5); x <= Math.min(mapSize - 1, currentPos.getX() + 5); x++) {
            for (int y = Math.max(0, currentPos.getY() - 5); y <= Math.min(mapSize - 1, currentPos.getY() + 5); y++) {
                Node candidate = new Node(x, y);

                if (PathUtils.checkInsideSafeArea(candidate, state.getGameMap().getSafeZone(), mapSize)) {
                    // Check if position is safe and accessible
                    String path = PathUtils.getShortestPath(
                        state.getGameMap(),
                        getNodesToAvoid(state),
                        currentPos,
                        candidate,
                        false
                    );

                    if (path != null) {
                        return candidate;
                    }
                }
            }
        }

        return null;
    }

    // Make hero accessible to inner class
    private Hero hero;

    public DecisionTree() {
        // Will be set by parent class
    }

    public void setHero(Hero hero) {
        this.hero = hero;
    }
}

class StrategyManager {
    // Manages different strategies based on game phase and situation

    public Strategy getOptimalStrategy(GameState state, Hero hero) {
        // Dynamic strategy selection based on game state

        if (state.getCurrentPlayer().getHealth() < 30) {
            return Strategy.SURVIVAL;
        }

        if (!state.isInSafeZone()) {
            return Strategy.SAFETY;
        }

        if (state.getThreatLevel() == ThreatLevel.HIGH) {
            return Strategy.AGGRESSIVE_COMBAT;
        }

        if (state.getThreatLevel() == ThreatLevel.MEDIUM) {
            return Strategy.DEFENSIVE_COMBAT;
        }

        try {
            if (hero.getInventory().getGun() == null) {
                return Strategy.EQUIPMENT_GATHERING;
            }
        } catch (Exception e) {
            return Strategy.EQUIPMENT_GATHERING;
        }

        return Strategy.TACTICAL_POSITIONING;
    }
}

// Enums and Data Classes
enum ThreatLevel {
    SAFE, LOW, MEDIUM, HIGH
}

enum ActionType {
    MOVE, ATTACK, PICKUP, USE_ITEM, THROW, USE_SPECIAL, WAIT
}

enum Strategy {
    SURVIVAL, SAFETY, AGGRESSIVE_COMBAT, DEFENSIVE_COMBAT, EQUIPMENT_GATHERING, TACTICAL_POSITIONING
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
        } else if (type == ActionType.USE_ITEM) {
            this.itemId = pathOrDirectionOrItemId;
        } else {
            this.direction = pathOrDirectionOrItemId;
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