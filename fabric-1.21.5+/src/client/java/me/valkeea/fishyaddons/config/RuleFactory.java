package me.valkeea.fishyaddons.config;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

public class RuleFactory {
    private RuleFactory() {}
    private static final Gson GSON = new Gson();
    private static SeaCreatureData cachedData = null;
    
    public static class SeaCreatureData {
        private Map<String, CategoryConfig> categories;
        private List<String> triggerMessages;
        private List<CreatureConfig> creatures;
        
        public static class CategoryConfig {
            int priority;
            String prefix;
            String doubleHookPrefix;
            boolean enabled;

            public String getPrefix() { return prefix; }
            public String getDoubleHookPrefix() { return doubleHookPrefix; }
            public boolean isEnabled() { return enabled; }
        }
        
        public static class CreatureConfig {
            String id;
            String triggerText;
            String displayName;
            String displayNamePlural;
            String category;
            String emoji;
            String customMessage;

            public String getId() { return id; }
            public String getDisplayName() { return displayName; }
            public String getCategory() { return category; }
            public String getEmoji() { return emoji; }
            public String getCustomMessage() { return customMessage; }
        }
    }

    private static final String[] DH_TRIGGERS = new String[] {
        "It's a Double Hook!",
        "It's a Double Hook! Woot woot!"
    };    
    
    public static SeaCreatureData loadSeaCreatureData() {
        if (cachedData != null) {
            return cachedData;
        }
        
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getResourceManager() != null) {
                Identifier resourceId = Identifier.of("fishyaddons", "data/sea_creatures.json");
                InputStream inputStream = client.getResourceManager()
                    .getResource(resourceId)
                    .orElseThrow()
                    .getInputStream();
                
                try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                    cachedData = GSON.fromJson(reader, SeaCreatureData.class);
                    return cachedData;
                }
            } else {

                InputStream inputStream = RuleFactory.class.getClassLoader()
                    .getResourceAsStream("assets/fishyaddons/data/sea_creatures.json");
                
                if (inputStream != null) {
                    try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                        cachedData = GSON.fromJson(reader, SeaCreatureData.class);
                        return cachedData;
                    }
                } else {
                    System.err.println("[FishyAddons] Could not find sea_creatures.json in classpath");
                }
            }
        } catch (Exception e) {
            System.err.println("[FishyAddons] Failed to load sea creature data: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    public static Map<String, FilterConfig.Rule> generateSeaCreatureRules() {
        Map<String, FilterConfig.Rule> rules = new ConcurrentHashMap<>();
        SeaCreatureData data = loadSeaCreatureData();
        
        if (data == null || data.creatures == null || data.categories == null) {
            System.err.println("[FishyAddons] Invalid sea creature data, skipping rule generation");
            return rules;
        }

        for (String trigger : DH_TRIGGERS) {
            rules.put("dh_trigger_" + trigger.toLowerCase().replaceAll("\\W+", "_"), new FilterConfig.Rule(
                trigger,
                "",
                40,
                true,
                true
            ));
        }

        for (SeaCreatureData.CreatureConfig creature : data.creatures) {
            SeaCreatureData.CategoryConfig category = data.categories.get(creature.category);
            if (category == null) {
                System.err.println("[FishyAddons] Unknown category '" + creature.category + "' for creature " + creature.id);
                continue;
            }
            
            String formattedMessage = buildCreatureMessage(creature, category);
            String ruleName = "sc_" + creature.id;
            FilterConfig.Rule existingUserRule = FilterConfig.getUserRules().get(ruleName);
            String actualPrefix = category.doubleHookPrefix;
            String actualMessage = formattedMessage;
            boolean actualEnabled = category.enabled;
            
            if (existingUserRule != null) {
                if (existingUserRule.getDhPrefix() != null) {
                    actualPrefix = existingUserRule.getDhPrefix();
                }
                if (existingUserRule.getReplacement() != null) {
                    actualMessage = existingUserRule.getReplacement();
                }
                actualEnabled = existingUserRule.isEnabled();
            }

            FilterConfig.Rule rule = new FilterConfig.Rule(
                creature.triggerText,
                actualMessage,
                actualPrefix,
                data.triggerMessages.toArray(new String[0]),
                category.priority,
                actualEnabled
            );
            rules.put(ruleName, rule);
        }
        return rules;
    }
    
    private static String buildCreatureMessage(SeaCreatureData.CreatureConfig creature, 
                                             SeaCreatureData.CategoryConfig category) {
        String message = creature.customMessage;
        
        message = message.replace("{name}", creature.displayName);
        message = message.replace("{emoji}", creature.emoji);
        message = message.replace("{id}", creature.id);
        
        return category.prefix + message;
    }
    
    public static Map<String, SeaCreatureData.CategoryConfig> getCategories() {
        SeaCreatureData data = loadSeaCreatureData();
        return data != null ? data.categories : new LinkedHashMap<>();
    }
    
    public static List<SeaCreatureData.CreatureConfig> getCreatures() {
        SeaCreatureData data = loadSeaCreatureData();
        return data != null ? data.creatures : new ArrayList<>();
    }

    public static String[] getDhTriggers() {
        return DH_TRIGGERS;
    }
}