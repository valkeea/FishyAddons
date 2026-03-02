package me.valkeea.fishyaddons.config;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    
    private static boolean recreated = false;
    private static boolean corrupted = false;
    
    private static final String CFG = "config";
    private static final String FA = "fishyaddons";
    private static final String DATA_DIR = "data";
    private static final String SEA_CREATURES_FILE = "sea_creatures.json";
    private static final String RESOURCE_PATH = "assets/fishyaddons/data/" + SEA_CREATURES_FILE;
    
    public static class SeaCreatureData {
        private Map<String, CategoryConfig> categories;
        private List<String> triggerMessages;
        private List<CreatureConfig> creatures;
        
        public record CategoryConfig(
            int priority,
            String prefix,
            String doubleHookPrefix,
            boolean enabled
        ) {}
        
        public record CreatureConfig(
            String id,
            String triggerText,
            String displayName,
            String displayNamePlural,
            String category,
            String emoji,
            String customMessage
        ) {}
    }

    private static final String[] DH_TRIGGERS = new String[] {
        "It's a Double Hook!",
        "It's a Double Hook! Woot woot!"
    };
    
    public static Path getFilePath() {
        return Paths.get(CFG, FA, DATA_DIR, SEA_CREATURES_FILE);
    }
    
    public static Path getDataDir() {
        return Paths.get(CFG, FA, DATA_DIR);
    }
    
    public static boolean isRecreated() {
        return recreated;
    }

    public static boolean isCorrupted() {
        return corrupted;
    }
    
    public static void resetFlags() {
        recreated = false;
        corrupted = false;
    }
    
    public static void ensureFile() {
        var targetPath = getFilePath();

        if (!Files.exists(targetPath)) {
            cloneToConfig(targetPath);
        }
    }
    
    private static void cloneToConfig(Path targetPath) {
        
        try (var inputStream = RuleFactory.class.getResourceAsStream("/" + RESOURCE_PATH)) {
            if (inputStream == null) {
                System.err.println("[FishyAddons] Could not find sea_creatures.json at: " + RESOURCE_PATH);
                return;
            }
            
            Files.createDirectories(targetPath.getParent());
            Files.copy(inputStream, targetPath);
            recreated = true;
        } catch (Exception e) {
            System.err.println("[FishyAddons] Failed to copy sea_creatures.json: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static boolean validContent(SeaCreatureData data) {
        if (data == null) return false;
        if (data.categories == null || data.categories.isEmpty()) return false;
        if (data.creatures == null || data.creatures.isEmpty()) return false;
        if (data.triggerMessages == null || data.triggerMessages.isEmpty()) return false;
        
        for (var sc: data.creatures) {
            if (sc.category == null || !data.categories.containsKey(sc.category)) {
                System.err.println("[FishyAddons] Invalid sea creature data: Creature " + sc.id + " has unknown category '" + sc.category + "'");
                return false;
            }
        }
        return true;
    }
    
    private static SeaCreatureData loadFromAssets() {
        try {
            var client = MinecraftClient.getInstance();
            if (client != null && client.getResourceManager() != null) {
                var resourceId = Identifier.of(FA, "data/sea_creatures.json");
                var inputStream = client.getResourceManager()
                    .getResource(resourceId)
                    .orElseThrow()
                    .getInputStream();
                
                try (var reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                    SeaCreatureData data = GSON.fromJson(reader, SeaCreatureData.class);
                    if (validContent(data)) return data;
                }

            } else {
                var inputStream = RuleFactory.class.getClassLoader()
                    .getResourceAsStream(RESOURCE_PATH);
                
                if (inputStream != null) {
                    try (var reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                        SeaCreatureData data = GSON.fromJson(reader, SeaCreatureData.class);
                        if (validContent(data)) return data;
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
    
    public static SeaCreatureData loadSeaCreatureData() {
        if (cachedData != null) {
            return cachedData;
        }
        
        ensureFile();
        
        Path configPath = getFilePath();
        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                SeaCreatureData data = GSON.fromJson(json, SeaCreatureData.class);
                
                if (validContent(data)) {
                    cachedData = data;
                    return cachedData;
                } else {
                    System.err.println("[FishyAddons] Invalid sea creature data in config file, using default.");
                    corrupted = true;
                }
            } catch (Exception e) {
                System.err.println("[FishyAddons] Failed to load sea creature data from config: " + e.getMessage());
                e.printStackTrace();
                corrupted = true;
            }
        }
        
        var data = loadFromAssets();
        if (data != null) {
            cachedData = data;
        } else {
            System.err.println("[FishyAddons] Failed to load sea creature data from any source");
        }

        return cachedData;
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

        for (var sc : data.creatures) {
            SeaCreatureData.CategoryConfig category = data.categories.get(sc.category);
            if (category == null) {
                System.err.println("[FishyAddons] Unknown category '" + sc.category + "' for creature " + sc.id);
                continue;
            }
            
            String formattedMessage = buildCreatureMessage(sc, category);
            String ruleName = "sc_" + sc.id;
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
                sc.triggerText,
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
    
    private static String buildCreatureMessage(SeaCreatureData.CreatureConfig sc, 
                                             SeaCreatureData.CategoryConfig category) {
        String message = sc.customMessage;
        
        message = message.replace("{name}", sc.displayName);
        message = message.replace("{emoji}", sc.emoji);
        message = message.replace("{id}", sc.id);
        
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
