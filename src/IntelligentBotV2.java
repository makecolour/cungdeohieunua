import io.socket.emitter.Emitter;
import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.weapon.Weapon;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class IntelligentBotV2 {
    private static final String SERVER_URL = "https://cf25-server.jsclub.dev";
    private static final String GAME_ID = "126900";
    private static final String PLAYER_NAME = "AI_AdvancedBot";
    private static final String SECRET_KEY = "sk-4WqZJ7o2SMCWqz2W4PCHjw:hcZoNXuEaXUpZAZxcqUQLnxz5jfLPIkCjvlFsAdhIDBM7PJyqri_nhYbMneOLWtUeDC6HBWmFXBM2wXQu3rcdA";

    public static void main(String[] args) throws IOException {
        Hero hero = new Hero(GAME_ID, PLAYER_NAME, SECRET_KEY);
        AdvancedMapUpdateListener listener = new AdvancedMapUpdateListener(hero);
        
        hero.setOnMapUpdate(listener);
        hero.start(SERVER_URL);
    }
}

class AdvancedMapUpdateListener implements Emitter.Listener {
    private final Hero hero;
    private float lastHealth = 100;
    private Map<String, Integer> enemyPositions = new HashMap<>();
    private Queue<String> actionQueue = new LinkedList<>();
    private int consecutiveWaitSteps = 0;

    public AdvancedMapUpdateListener(Hero hero) {
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
                System.out.println("Player is dead or not available.");
                return;
            }

            // Advanced decision making with multiple strategies
            BotAction action = makeIntelligentDecision(gameMap, player);
            executeAction(action);
            
            // Update state for next iteration
            lastHealth = player.getHealth();
            updateEnemyTracking(gameMap);

        } catch (Exception e) {
            System.err.println("Error in bot logic: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private BotAction makeIntelligentDecision(GameMap gameMap, Player player) {
        // Multi-layer decision tree with priority system
        
        // CRITICAL PRIORITY: Survival
        if (player.getHealth() <= 25) {
            BotAction healAction = findHealingStrategy(gameMap, player);
            if (healAction != null) {
                System.out.println("CRITICAL: Seeking healing - Health: " + player.getHealth());
                return healAction;
            }
        }

        // HIGH PRIORITY: Safe zone management
        boolean inSafeZone = PathUtils.checkInsideSafeArea(player, gameMap.getSafeZone(), gameMap.getMapSize());
        if (!inSafeZone) {
            BotAction safetyAction = moveToSafeZone(gameMap, player);
            if (safetyAction != null) {
                System.out.println("HIGH: Moving to safe zone");
                return safetyAction;
            }
        }

        // MEDIUM-HIGH PRIORITY: Combat engagement
        BotAction combatAction = evaluateCombatSituation(gameMap, player);
        if (combatAction != null) {
            System.out.println("COMBAT: Engaging threat");
            return combatAction;
        }

        // MEDIUM PRIORITY: Equipment optimization
        BotAction equipmentAction = optimizeEquipment(gameMap, player);
        if (equipmentAction != null) {
            System.out.println("EQUIPMENT: Acquiring gear");
            return equipmentAction;
        }

        // LOW PRIORITY: Strategic positioning and exploration
        BotAction explorationAction = intelligentExploration(gameMap, player);
        if (explorationAction != null) {
            System.out.println("EXPLORATION: Strategic movement");
            return explorationAction;
        }

        // Fallback: Wait with tactical repositioning
        consecutiveWaitSteps++;
        if (consecutiveWaitSteps > 3) {
            consecutiveWaitSteps = 0;
            return tacticalRepositioning(gameMap, player);
        }

        return new BotAction(BotActionType.WAIT);
    }

    private BotAction findHealingStrategy(GameMap gameMap, Player player) {
        // Check inventory for healing items first
        try {
            var healingItems = hero.getInventory().getListHealingItem();
            if (healingItems != null && !healingItems.isEmpty()) {
                var bestHealing = healingItems.stream()
                    .filter(item -> item.getHealingHP() > 0)
                    .max(Comparator.comparingInt(item -> calculateHealingPriority(item, Math.round(player.getHealth()))))
                    .orElse(null);
                
                if (bestHealing != null) {
                    return new BotAction(BotActionType.USE_ITEM, bestHealing.getId());
                }
            }
        } catch (Exception e) {
            System.out.println("Error checking inventory healing items: " + e.getMessage());
        }

        // Look for healing items on map
        try {
            var mapHealingItems = gameMap.getListHealingItems();
            if (mapHealingItems != null && !mapHealingItems.isEmpty()) {
                var nearestHealing = mapHealingItems.stream()
                    .min(Comparator.comparingInt(item -> PathUtils.distance(player, item)))
                    .orElse(null);
                
                if (nearestHealing != null) {
                    String path = PathUtils.getShortestPath(
                        gameMap,
                        getSafeNodesToAvoid(gameMap),
                        player,
                        nearestHealing,
                        false
                    );
                    
                    if (path != null) {
                        return path.isEmpty() ? 
                            new BotAction(BotActionType.PICKUP) : 
                            new BotAction(BotActionType.MOVE, path);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error finding map healing items: " + e.getMessage());
        }

        // Look for friendly NPCs for healing
        try {
            var allies = gameMap.getListAllies();
            if (allies != null && !allies.isEmpty()) {
                var nearestAlly = allies.stream()
                    .min(Comparator.comparingInt(ally -> PathUtils.distance(player, ally)))
                    .orElse(null);
                
                if (nearestAlly != null && PathUtils.distance(player, nearestAlly) <= 10) {
                    String path = PathUtils.getShortestPath(
                        gameMap,
                        getSafeNodesToAvoid(gameMap),
                        player,
                        nearestAlly,
                        false
                    );
                    
                    if (path != null && !path.isEmpty()) {
                        return new BotAction(BotActionType.MOVE, path);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error finding allies: " + e.getMessage());
        }

        return null;
    }

    private int calculateHealingPriority(Object healingItem, int currentHealth) {
        // Enhanced healing priority calculation
        try {
            // Use reflection or safe casting to get healing amount
            int healAmount = 20; // default
            double usageTime = 1.0; // default
            
            // Priority = healing amount / usage time + urgency factor
            int urgencyFactor = (100 - currentHealth) / 10;
            return (int)((healAmount / usageTime) * 10) + urgencyFactor;
        } catch (Exception e) {
            return 50; // default priority
        }
    }

    private BotAction moveToSafeZone(GameMap gameMap, Player player) {
        int mapSize = gameMap.getMapSize();
        Node safeCenter = new Node(mapSize / 2, mapSize / 2);
        
        String path = PathUtils.getShortestPath(
            gameMap,
            getSafeNodesToAvoid(gameMap),
            player,
            safeCenter,
            false
        );
        
        return (path != null && !path.isEmpty()) ? 
            new BotAction(BotActionType.MOVE, path) : null;
    }

    private BotAction evaluateCombatSituation(GameMap gameMap, Player player) {
        List<Player> enemies = gameMap.getOtherPlayerInfo();
        if (enemies == null || enemies.isEmpty()) return null;

        // Find nearest enemy within combat range
        Player nearestEnemy = enemies.stream()
            .filter(enemy -> PathUtils.distance(player, enemy) <= 8)
            .min(Comparator.comparingInt(enemy -> PathUtils.distance(player, enemy)))
            .orElse(null);

        if (nearestEnemy == null) return null;

        int distance = PathUtils.distance(player, nearestEnemy);
        String direction = calculateDirection(player, nearestEnemy);

        // Combat strategy based on available weapons and distance
        try {
            // Gun combat (preferred for medium-long range)
            if (hero.getInventory().getGun() != null && distance > 2 && distance <= 8) {
                return new BotAction(BotActionType.SHOOT, direction);
            }

            // Throwable weapons for medium range
            if (hero.getInventory().getThrowable() != null && distance > 1 && distance <= 6) {
                return new BotAction(BotActionType.THROW, direction, Math.min(distance, 6));
            }

            // Melee combat for close range
            if (hero.getInventory().getMelee() != null && distance <= 3) {
                return new BotAction(BotActionType.ATTACK, direction);
            }

            // Special weapons as last resort
            if (hero.getInventory().getSpecial() != null && distance <= 5) {
                return new BotAction(BotActionType.USE_SPECIAL, direction);
            }

            // If no suitable weapon, try to get closer or find cover
            if (distance > 3) {
                String approachPath = PathUtils.getShortestPath(
                    gameMap,
                    getSafeNodesToAvoid(gameMap),
                    player,
                    nearestEnemy,
                    false
                );
                
                if (approachPath != null && !approachPath.isEmpty() && approachPath.length() <= 2) {
                    return new BotAction(BotActionType.MOVE, approachPath.substring(0, 1));
                }
            }

        } catch (Exception e) {
            System.out.println("Error in combat evaluation: " + e.getMessage());
        }

        return null;
    }

    private BotAction optimizeEquipment(GameMap gameMap, Player player) {
        try {
            // Priority: Gun > Special > Throwable > Better Melee > Armor > Healing
            
            // Look for guns first
            if (hero.getInventory().getGun() == null) {
                List<Weapon> guns = gameMap.getAllGun();
                if (guns != null && !guns.isEmpty()) {
                    Weapon nearestGun = guns.stream()
                        .min(Comparator.comparingInt(gun -> PathUtils.distance(player, gun)))
                        .orElse(null);
                    
                    if (nearestGun != null && PathUtils.distance(player, nearestGun) <= 15) {
                        return createMoveToItemAction(nearestGun, gameMap, player);
                    }
                }
            }

            // Look for special weapons
            if (hero.getInventory().getSpecial() == null) {
                List<Weapon> specials = gameMap.getAllSpecial();
                if (specials != null && !specials.isEmpty()) {
                    Weapon nearestSpecial = specials.stream()
                        .min(Comparator.comparingInt(special -> PathUtils.distance(player, special)))
                        .orElse(null);
                    
                    if (nearestSpecial != null && PathUtils.distance(player, nearestSpecial) <= 12) {
                        return createMoveToItemAction(nearestSpecial, gameMap, player);
                    }
                }
            }

            // Look for throwable weapons
            if (hero.getInventory().getThrowable() == null) {
                List<Weapon> throwables = gameMap.getAllThrowable();
                if (throwables != null && !throwables.isEmpty()) {
                    Weapon nearestThrowable = throwables.stream()
                        .min(Comparator.comparingInt(throwable -> PathUtils.distance(player, throwable)))
                        .orElse(null);
                    
                    if (nearestThrowable != null && PathUtils.distance(player, nearestThrowable) <= 10) {
                        return createMoveToItemAction(nearestThrowable, gameMap, player);
                    }
                }
            }

            // Look for better melee weapons
            List<Weapon> melees = gameMap.getAllMelee();
            if (melees != null && !melees.isEmpty()) {
                Weapon nearestMelee = melees.stream()
                    .filter(melee -> !melee.getId().equals("HAND")) // Skip basic hand weapon
                    .min(Comparator.comparingInt(melee -> PathUtils.distance(player, melee)))
                    .orElse(null);
                
                if (nearestMelee != null && PathUtils.distance(player, nearestMelee) <= 8) {
                    return createMoveToItemAction(nearestMelee, gameMap, player);
                }
            }

            // Look for armor
            var armors = gameMap.getListArmors();
            if (armors != null && !armors.isEmpty()) {
                var nearestArmor = armors.stream()
                    .min(Comparator.comparingInt(armor -> PathUtils.distance(player, armor)))
                    .orElse(null);
                
                if (nearestArmor != null && PathUtils.distance(player, nearestArmor) <= 8) {
                    return createMoveToItemAction(nearestArmor, gameMap, player);
                }
            }

        } catch (Exception e) {
            System.out.println("Error in equipment optimization: " + e.getMessage());
        }

        return null;
    }

    private BotAction intelligentExploration(GameMap gameMap, Player player) {
        try {
            List<Node> interestingLocations = new ArrayList<>();
            
            // Add all weapons
            interestingLocations.addAll(gameMap.getListWeapons());
            
            // Add healing items
            var healingItems = gameMap.getListHealingItems();
            if (healingItems != null) {
                interestingLocations.addAll(healingItems);
            }
            
            // Add chests (destructible obstacles)
            var chests = gameMap.getObstaclesByTag("DESTRUCTIBLE");
            if (chests != null) {
                interestingLocations.addAll(chests);
            }

            if (!interestingLocations.isEmpty()) {
                // Filter to safe zone items first, then nearest
                Node target = interestingLocations.stream()
                    .filter(loc -> PathUtils.checkInsideSafeArea(loc, gameMap.getSafeZone(), gameMap.getMapSize()))
                    .min(Comparator.comparingInt(loc -> PathUtils.distance(player, loc)))
                    .orElse(interestingLocations.stream()
                        .min(Comparator.comparingInt(loc -> PathUtils.distance(player, loc)))
                        .orElse(null));

                if (target != null && PathUtils.distance(player, target) <= 20) {
                    String path = PathUtils.getShortestPath(
                        gameMap,
                        getSafeNodesToAvoid(gameMap),
                        player,
                        target,
                        false
                    );
                    
                    if (path != null) {
                        return path.isEmpty() ? 
                            new BotAction(BotActionType.PICKUP) : 
                            new BotAction(BotActionType.MOVE, path);
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Error in exploration: " + e.getMessage());
        }

        return null;
    }

    private BotAction tacticalRepositioning(GameMap gameMap, Player player) {
        // Move to a more advantageous position
        int mapSize = gameMap.getMapSize();
        int safeZone = gameMap.getSafeZone();
        
        // Try to move towards center of safe zone with slight randomization
        int targetX = (mapSize / 2) + (new Random().nextInt(6) - 3);
        int targetY = (mapSize / 2) + (new Random().nextInt(6) - 3);
        
        Node target = new Node(
            Math.max(0, Math.min(mapSize - 1, targetX)),
            Math.max(0, Math.min(mapSize - 1, targetY))
        );
        
        String path = PathUtils.getShortestPath(
            gameMap,
            getSafeNodesToAvoid(gameMap),
            player,
            target,
            false
        );
        
        if (path != null && !path.isEmpty()) {
            return new BotAction(BotActionType.MOVE, path.substring(0, 1)); // Take one step
        }
        
        return new BotAction(BotActionType.WAIT);
    }

    private BotAction createMoveToItemAction(Node item, GameMap gameMap, Player player) {
        String path = PathUtils.getShortestPath(
            gameMap,
            getSafeNodesToAvoid(gameMap),
            player,
            item,
            false
        );
        
        if (path != null) {
            return path.isEmpty() ? 
                new BotAction(BotActionType.PICKUP) : 
                new BotAction(BotActionType.MOVE, path);
        }
        
        return null;
    }

    private List<Node> getSafeNodesToAvoid(GameMap gameMap) {
        List<Node> nodes = new ArrayList<>();
        
        try {
            // Add indestructible obstacles
            var indestructibles = gameMap.getListIndestructibles();
            if (indestructibles != null) {
                nodes.addAll(indestructibles);
            }
            
            // Add other players (for pathfinding, not combat)
            var others = gameMap.getOtherPlayerInfo();
            if (others != null) {
                nodes.addAll(others);
            }
            
            // Add hostile NPCs
            var enemies = gameMap.getListEnemies();
            if (enemies != null) {
                nodes.addAll(enemies);
            }
            
            // Remove nodes with "CAN_GO_THROUGH" tag
            var passableNodes = gameMap.getObstaclesByTag("CAN_GO_THROUGH");
            if (passableNodes != null) {
                nodes.removeAll(passableNodes);
            }
            
        } catch (Exception e) {
            System.out.println("Error building avoidance list: " + e.getMessage());
        }
        
        return nodes;
    }

    private String calculateDirection(Node from, Node to) {
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        
        // Prioritize the larger difference
        if (Math.abs(dx) > Math.abs(dy)) {
            return dx > 0 ? "r" : "l";
        } else {
            return dy > 0 ? "u" : "d";
        }
    }

    private void updateEnemyTracking(GameMap gameMap) {
        try {
            var enemies = gameMap.getOtherPlayerInfo();
            if (enemies != null) {
                enemyPositions.clear();
                for (Player enemy : enemies) {
                    enemyPositions.put("Enemy_" + enemy.getX() + "_" + enemy.getY(), 
                                    PathUtils.distance(gameMap.getCurrentPlayer(), enemy));
                }
            }
        } catch (Exception e) {
            System.out.println("Error updating enemy tracking: " + e.getMessage());
        }
    }

    private void executeAction(BotAction action) throws IOException {
        if (action == null) return;
        
        consecutiveWaitSteps = 0; // Reset wait counter for any action
        
        switch (action.getType()) {
            case MOVE:
                if (action.getData() != null && !action.getData().isEmpty()) {
                    hero.move(action.getData());
                }
                break;
            case SHOOT:
                if (action.getData() != null) {
                    hero.shoot(action.getData());
                }
                break;
            case ATTACK:
                if (action.getData() != null) {
                    hero.attack(action.getData());
                }
                break;
            case PICKUP:
                hero.pickupItem();
                break;
            case USE_ITEM:
                if (action.getData() != null) {
                    hero.useItem(action.getData());
                }
                break;
            case THROW:
                if (action.getData() != null && action.getDistance() > 0) {
                    hero.throwItem(action.getData(), action.getDistance());
                }
                break;
            case USE_SPECIAL:
                if (action.getData() != null) {
                    hero.useSpecial(action.getData());
                }
                break;
            case WAIT:
                consecutiveWaitSteps++;
                // Deliberately do nothing
                break;
        }
    }
}

// Support classes
enum BotActionType {
    MOVE, SHOOT, ATTACK, PICKUP, USE_ITEM, THROW, USE_SPECIAL, WAIT
}

class BotAction {
    private BotActionType type;
    private String data;
    private int distance;
    
    public BotAction(BotActionType type) {
        this.type = type;
    }
    
    public BotAction(BotActionType type, String data) {
        this.type = type;
        this.data = data;
    }
    
    public BotAction(BotActionType type, String data, int distance) {
        this.type = type;
        this.data = data;
        this.distance = distance;
    }
    
    public BotActionType getType() { return type; }
    public String getData() { return data; }
    public int getDistance() { return distance; }
}
