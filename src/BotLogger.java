/**
 * Simple logging utility for the j-surviv bot
 */
public class BotLogger {
    private static final boolean DEBUG_ENABLED = true;
    
    public static void info(String message) {
        if (DEBUG_ENABLED) {
            System.out.println("[BOT-INFO] " + message);
        }
    }
    
    public static void error(String message) {
        if (DEBUG_ENABLED) {
            System.err.println("[BOT-ERROR] " + message);
        }
    }
    
    public static void debug(String message) {
        if (DEBUG_ENABLED) {
            System.out.println("[BOT-DEBUG] " + message);
        }
    }
    
    public static void combat(String message) {
        if (DEBUG_ENABLED) {
            System.out.println("[BOT-COMBAT] " + message);
        }
    }
    
    public static void strategy(String message) {
        if (DEBUG_ENABLED) {
            System.out.println("[BOT-STRATEGY] " + message);
        }
    }
}
