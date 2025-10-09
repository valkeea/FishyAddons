package me.valkeea.fishyaddons.api.skyblock;

import me.valkeea.fishyaddons.config.StatConfig;

/**
 * Tracks the current chat mode on Hypixel (all, party, guild).
 */
public class GameChat {
    private GameChat() {}
    
    public enum Channel {
        ALL("all"),
        PARTY("party"), 
        GUILD("guild");
        
        private final String command;
        
        Channel(String command) {
            this.command = command;
        }
        
        public String getCommand() {
            return command;
        }
        
        public static Channel fromCommand(String command) {
            if (command == null) return null;
            
            String cmd = command.toLowerCase().trim();
            
            for (Channel mode : values()) {
                if (mode.command.equalsIgnoreCase(cmd)) {
                    return mode;
                }
            }
            
            switch (cmd) {
                case "a":
                case "all":
                    return ALL;
                case "p":
                case "party":
                    return PARTY;
                case "g":
                case "guild":
                    return GUILD;
                default:
                    return null;
            }
        }
    }

    private static Channel currentMode = null;
    private static boolean initialized = false;
    
    public static void init() {
        if (!initialized) {
            loadChatMode();
            initialized = true;
        }
    }
    
    private static void loadChatMode() {
        try {
            String savedMode = StatConfig.getChatMode();
            Channel mode = Channel.valueOf(savedMode);
            currentMode = mode;
        } catch (IllegalArgumentException e) {
            currentMode = Channel.ALL;
            saveChatMode();
        }
    }
    
    private static void saveChatMode() {
        if (currentMode != null) {
            StatConfig.setChatMode(currentMode.name());
        }
    }
    
    private static Channel ensureInitialized() {
        if (!initialized) {
            init();
        }
        return currentMode != null ? currentMode : Channel.ALL;
    }
    
    /**
     * Updates the chat mode based on intercepted chat commands.
     * 
     * @param command The chat command (e.g., "all", "a", "party", "p", "guild", "g")
     */
    public static void changedChannel(String command) {
        Channel newMode = Channel.fromCommand(command);
        if (newMode != null) {
            currentMode = newMode;
            saveChatMode();
        }
    }

    public static void setChannel(Channel mode) {
        if (mode != null) {
            currentMode = mode;
            saveChatMode();
        }
    }
    
    /**
     * Gets the current chat mode.
     * 
     * @return Current Channel
     */
    public static Channel getCurrentMode() {
        return ensureInitialized();
    }
    
    /**
     * Checks if the current chat mode is party chat.
     * 
     * @return true if in party chat mode
     */
    public static boolean partyToggled() {
        return ensureInitialized() == Channel.PARTY;
    }
}