package me.valkeea.fishyaddons.vconfig.config.impl;

import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import me.valkeea.fishyaddons.feature.qol.ChatReplacement;
import me.valkeea.fishyaddons.feature.qol.CommandAlias;
import me.valkeea.fishyaddons.feature.qol.KeyShortcut;
import me.valkeea.fishyaddons.vconfig.config.BaseConfig;
import me.valkeea.fishyaddons.vconfig.config.ConfigSection;

@SuppressWarnings("squid:S6548")
public class ShortcutsConfig extends BaseConfig {
    private static final ShortcutsConfig INSTANCE = new ShortcutsConfig();

    private final ConfigSection<String> commandAliases =
        new ConfigSection<>("commandAliases", "toggledCommandAliases",
            new TypeToken<Map<String, String>>(){}.getType(),
            new TypeToken<Map<String, Boolean>>(){}.getType(),
            v -> { requestSave(); CommandAlias.refresh(); });
            
    private final ConfigSection<String> chatReplacements =
        new ConfigSection<>("chatReplacements", "toggledChatReplacements",
            new TypeToken<Map<String, String>>(){}.getType(),
            new TypeToken<Map<String, Boolean>>(){}.getType(),
            v -> { requestSave(); ChatReplacement.refresh(); });
            
    private final ConfigSection<String> keybinds =
        new ConfigSection<>("keybinds", "toggledKeybinds",
            new TypeToken<Map<String, String>>(){}.getType(),
            new TypeToken<Map<String, Boolean>>(){}.getType(),
            v -> { requestSave(); KeyShortcut.refresh(); });        
    
    private ShortcutsConfig() {
        super("shortcuts.json");
    }

    public static ShortcutsConfig getInstance() {
        return INSTANCE;
    }

    public static void save() {
        INSTANCE.requestSave();
    }

    @Override
    protected void loadFromJson(JsonObject json) {
        commandAliases.loadFromJson(json);
        chatReplacements.loadFromJson(json);
        keybinds.loadFromJson(json);
    }

    @Override
    protected void saveToJson(JsonObject json) {
        commandAliases.saveToJson(json);
        chatReplacements.saveToJson(json);
        keybinds.saveToJson(json);
    }
    
    // --- Command Aliases ---
    public static Map<String, String> getAliases() { return INSTANCE.commandAliases.getValues(); }
    public static void setAlias(String key, String value) { INSTANCE.commandAliases.set(key, value); }
    public static void removeAlias(String key) { INSTANCE.commandAliases.remove(key); }
    public static boolean isAliasToggled(String key) { return INSTANCE.commandAliases.isToggled(key); }
    public static void toggleAlias(String key, boolean enabled) { INSTANCE.commandAliases.toggle(key, enabled); }
    
    // --- Chat Replacements ---
    public static Map<String, String> getChat() { return INSTANCE.chatReplacements.getValues(); }
    public static void setChat(String key, String value) { INSTANCE.chatReplacements.set(key, value); }
    public static void removeChat(String key) { INSTANCE.chatReplacements.remove(key); }
    public static boolean isChatToggled(String key) { return INSTANCE.chatReplacements.isToggled(key); }
    public static void toggleChat(String key, boolean enabled) { INSTANCE.chatReplacements.toggle(key, enabled); }
    
    // --- Keybinds ---
    public static Map<String, String> getKeybinds() { return INSTANCE.keybinds.getValues(); }
    public static void setKeybind(String key, String value) { INSTANCE.keybinds.set(key, value); }
    public static void removeKeybind(String key) { INSTANCE.keybinds.remove(key); }
    public static boolean isKeybindToggled(String key) { return INSTANCE.keybinds.isToggled(key); }
    public static void toggleKeybind(String key, boolean enabled) { INSTANCE.keybinds.toggle(key, enabled); }
}
