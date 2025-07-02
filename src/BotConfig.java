/**
 * Configuration class for fine-tuning bot behavior
 */
public class BotConfig {
    // Combat Configuration
    public static final double ENGAGEMENT_THRESHOLD = 1.2; // Only fight if we have 20% advantage
    public static final int MIN_HEALTH_FOR_COMBAT = 30;
    public static final int DANGER_ZONE_RADIUS = 3;
    public static final int ENEMY_DETECTION_RANGE = 10;
    
    // Pathfinding Configuration
    public static final int MAX_PATH_SEARCH_DISTANCE = 15;
    public static final double SAFETY_WEIGHT = 0.3;
    public static final int MIN_ESCAPE_ROUTES = 2;
    
    // Inventory Configuration
    public static final int MAX_HEALING_ITEMS = 5;
    public static final double HEALING_EFFICIENCY_THRESHOLD = 10.0;
    public static final int LOW_HEALTH_THRESHOLD = 50;
    
    // Weapon Priorities (higher = better)
    public static final double SHOTGUN_VALUE = 95.0;
    public static final double CROSSBOW_VALUE = 85.0;
    public static final double MACE_VALUE = 100.0;
    public static final double ROPE_VALUE = 80.0;
    
    // Strategic Configuration
    public static final int STRATEGIC_POSITION_RADIUS = 8;
    public static final int CENTER_PREFERENCE_RADIUS = 3;
    public static final double POSITION_SAFETY_THRESHOLD = 50.0;
    
    // Performance Configuration
    public static final long ACTION_COOLDOWN_MS = 100;
    public static final int MAX_CALCULATIONS_PER_FRAME = 50;
    
    // Game State Configuration
    public static final int EARLY_GAME_TIME_THRESHOLD = 300; // 5 minutes
    public static final int LATE_GAME_TIME_THRESHOLD = 60;   // 1 minute
    public static final double DARK_ZONE_PENALTY = 2.0;
    
    // Emergency Thresholds
    public static final int CRITICAL_HEALTH = 20;
    public static final int EMERGENCY_DISTANCE = 2;
    public static final double PANIC_MODE_THRESHOLD = 0.5;
}
