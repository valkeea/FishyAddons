package me.valkeea.fishyaddons.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import me.valkeea.fishyaddons.processor.BaseAnalysis;
import net.minecraft.client.MinecraftClient;

public class FilterConfig {
    private FilterConfig() {}
    private static File filterFile = null;
    
    public static class MessageContext {
        private MessageContext() {}
        
        private static final List<TimestampedMessage> recentMessages = new ArrayList<>();
        private static final int MAX_HISTORY = 10;
        
        public static class TimestampedMessage {
            final String message;
            final long timestamp;
            
            TimestampedMessage(String message) {
                this.message = message;
                this.timestamp = System.currentTimeMillis();
            }
        }
        
        public static void recordMessage(String message) {
            synchronized (recentMessages) {
                recentMessages.add(new TimestampedMessage(message));
                
                if (recentMessages.size() > MAX_HISTORY) {
                    recentMessages.remove(0);
                }
            }
        }
        
        public static boolean hasTriggerWithin(String[] triggerMessages, long timeoutMs) {
            if (triggerMessages == null || triggerMessages.length == 0) {
                return false;
            }
            
            long cutoff = System.currentTimeMillis() - timeoutMs;
            
            synchronized (recentMessages) {
                for (TimestampedMessage msg : recentMessages) {
                    if (msg.timestamp < cutoff) {
                        continue;
                    }
                    
                    for (String trigger : triggerMessages) {
                        if (msg.message.contains(trigger)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }
    
    public static class Rule {
        private String searchText;
        private String replacement;
        private int priority;
        private boolean enabled;
        private boolean requireFullMatch;
        private String dhPrefix;
        private String[] triggerMessages;
        private long contextTimeoutMs;
        
        public Rule() {
            this.searchText = "";
            this.replacement = "";
            this.priority = 50;
            this.enabled = true;
            this.requireFullMatch = true;
            this.contextTimeoutMs = 50;
        }
        
        /**
         * Simple string-based rules
         */
        public Rule(String searchText, String replacement, int priority, boolean enabled, boolean requireFullMatch) {
            this.searchText = searchText;
            this.replacement = replacement;
            this.priority = priority;
            this.enabled = enabled;
            this.requireFullMatch = requireFullMatch;
            this.contextTimeoutMs = 50;
        }
        
        /**
         * Contextual rules
         */
        public Rule(String searchText, String replacement, String dhPrefix, 
                   String[] triggerMessages, int priority, boolean enabled) {
            this.searchText = searchText;
            this.replacement = replacement;
            this.dhPrefix = dhPrefix;
            this.triggerMessages = triggerMessages;
            this.priority = priority;
            this.enabled = enabled;
            this.requireFullMatch = true;
            this.contextTimeoutMs = 50;
        }
        
        public String getSearchText() { return searchText; }
        public String getReplacement() { return replacement; }
        public int getPriority() { return priority; }
        public boolean isEnabled() { return enabled; }
        public boolean requireFullMatch() { return requireFullMatch; }
        public String getDhPrefix() { return dhPrefix; }
        public String[] getTriggerMessages() { return triggerMessages; }
        
        public boolean hasAlternativeFormat() {
            return dhPrefix != null && triggerMessages != null && triggerMessages.length > 0;
        }
        
        public String getContextualReplacement() {
            if (hasAlternativeFormat()) {
                boolean hasTriggered = MessageContext.hasTriggerWithin(triggerMessages, contextTimeoutMs);
                
                if (hasTriggered) {
                    String pluralizedReplacement = pluralize(replacement);
                    return dhPrefix + pluralizedReplacement;
                }
            }
            return replacement;
        }
        
        private String pluralize(String text) {
            if (text == null) return text;
            
            String result = text;
            
            try {
                // Sort creatures by displayName length (longest first) to avoid partial matches
                List<RuleFactory.SeaCreatureData.CreatureConfig> sortedCreatures = 
                    new ArrayList<>(RuleFactory.getCreatures());
                sortedCreatures.sort((a, b) -> {
                    if (a.displayName == null && b.displayName == null) return 0;
                    if (a.displayName == null) return 1;
                    if (b.displayName == null) return -1;
                    return Integer.compare(b.displayName.length(), a.displayName.length());
                });
                
                for (RuleFactory.SeaCreatureData.CreatureConfig creature : sortedCreatures) {
                    if (creature.displayName != null && creature.displayNamePlural != null) {
                        String quotedName = java.util.regex.Pattern.quote(creature.displayName);
                        String pluralName = creature.displayNamePlural;
                        String beforeReplacement = result;
                        
                        result = result.replaceAll("\\ba " + quotedName + "\\b", pluralName);
                        result = result.replaceAll("\\ban " + quotedName + "\\b", pluralName);
                        result = result.replaceAll("\\bA " + quotedName + "\\b", pluralName);
                        result = result.replaceAll("\\bAn " + quotedName + "\\b", pluralName);

                        if (pluralName.startsWith("two ")) {
                            result = result.replaceAll("\\bthe " + quotedName + "\\b", pluralName);
                            result = result.replaceAll("\\bThe " + quotedName + "\\b", pluralName);
                        } else {
                            result = result.replaceAll("\\bthe " + quotedName + "\\b", "the " + pluralName);
                            result = result.replaceAll("\\bThe " + quotedName + "\\b", "The " + pluralName);
                        }
                        
                        if (result.equals(beforeReplacement)) {
                            result = result.replaceAll("\\b" + quotedName + "\\b", pluralName);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[FishyAddons] Error during pluralization: " + e.getMessage());
                return text;
            }
            return result;
        }
        
        public void setSearchText(String searchText) { this.searchText = searchText; }
        public void setReplacement(String replacement) { this.replacement = replacement; }
        public void setDhPrefix(String dhPrefix) { this.dhPrefix = dhPrefix; }
        public void setPriority(int priority) { this.priority = priority; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public void setRequireFullMatch(boolean requireFullMatch) { this.requireFullMatch = requireFullMatch; }
    }
    
    private static final Map<String, Rule> DEFAULT_RULES = new ConcurrentHashMap<>();
    private static final Map<String, Rule> USER_RULES = new LinkedHashMap<>();

    public static Map<String, Rule> getUserConfig() {
        return USER_RULES;
    }
    
    static {
        initScRules();
        loadConfig();
    }
    
    private static void initScRules() {
        if (!areScRulesLoaded() && usingScRules()) {
            Map<String, Rule> seaCreatureRules = RuleFactory.generateSeaCreatureRules();
            DEFAULT_RULES.putAll(seaCreatureRules);
        }
    }

    public static boolean areScRulesLoaded() {
        return DEFAULT_RULES.keySet().stream().anyMatch(name -> name.startsWith("sc_"));
    }

    /**
     * Check if sea creature rules are needed by any feature
     */
    public static boolean usingScRules() {
        return FishyConfig.getState(Key.CHAT_FILTER_SC_ENABLED, false) || 
               FishyConfig.getState(Key.TRACK_SCS, false) ||
               FishyConfig.getState(Key.HUD_SKILL_XP_ENABLED, false) ||
               FishyConfig.getState(Key.CHAT_FILTER_SC_ENABLED, false);
    }

    /**
     * Refresh sea creature rules based on current feature states.
     */
    public static void refreshScRules() {
        boolean needed = usingScRules();
        boolean loaded = areScRulesLoaded();
        
        if (needed && !loaded) {
            Map<String, Rule> seaCreatureRules = RuleFactory.generateSeaCreatureRules();
            DEFAULT_RULES.putAll(seaCreatureRules);
        } else if (!needed && loaded) {
            DEFAULT_RULES.keySet().removeIf(name -> name.startsWith("sc_"));
        }
    }

    public static Map<String, Rule> getAllRules() {
        Map<String, Rule> allRules = new ConcurrentHashMap<>(DEFAULT_RULES);
        allRules.putAll(USER_RULES);
        return allRules;
    }

    public static Map<String, Rule> getDefaultRules() {
        return new ConcurrentHashMap<>(DEFAULT_RULES);
    }

    public static Map<String, Rule> getUserRules() {
        return new ConcurrentHashMap<>(USER_RULES);
    }
    
    /**
     * Gets only user-created rules (not modifications of default rules)
     */
    public static Map<String, Rule> getUserCreatedRules() {
        Map<String, Rule> userCreatedRules = new LinkedHashMap<>();
        
        for (Map.Entry<String, Rule> entry : USER_RULES.entrySet()) {
            String ruleName = entry.getKey();
            if (!DEFAULT_RULES.containsKey(ruleName)) {
                userCreatedRules.put(ruleName, entry.getValue());
            }
        }
        
        return userCreatedRules;
    }
    
    public static void setUserRule(String name, Rule rule) {
        USER_RULES.put(name, rule);
        
        if (name.startsWith("sc_")) {
            saveSeaCreatureConfig();
        } else {
            saveUserRules();
        }
        BaseAnalysis.clearCaches();
    }
    
    public static void removeUserRule(String name) {
        USER_RULES.remove(name);
        saveUserRules();
        BaseAnalysis.clearCaches();
    }

    public static void toggleUserRule(String name) {
        Rule rule = USER_RULES.get(name);
        if (rule != null) {
            rule.setEnabled(!rule.isEnabled());
            saveUserRules();
            BaseAnalysis.clearCaches();
        }
    }

    public static void setRequireFullMatch(String name, boolean requireFullMatch) {
        Rule rule = USER_RULES.get(name);
        if (rule != null) {
            rule.setRequireFullMatch(requireFullMatch);
            saveUserRules();
            BaseAnalysis.clearCaches();
        }
    }

    public static void setRuleEnabled(String name, boolean enabled) {
        Rule rule = USER_RULES.get(name);
        if (rule == null) {
            rule = DEFAULT_RULES.get(name);
            if (rule != null) {
                Rule clonedRule = clone(rule);
                clonedRule.setEnabled(enabled);
                USER_RULES.put(name, clonedRule);
            }
        } else {
            rule.setEnabled(enabled);
        }
        
        if (name.startsWith("sc_")) {
            saveSeaCreatureConfig();
        } else {
            saveUserRules();
        }

        BaseAnalysis.clearCaches();
    }
    
    public static boolean isRuleEnabled(String name) {
        Rule userRule = USER_RULES.get(name);
        if (userRule != null) {
            return userRule.isEnabled();
        }
        
        Rule defaultRule = DEFAULT_RULES.get(name);
        return defaultRule != null && defaultRule.isEnabled();
    }
    
    private static void saveUserRules() {
        try {
            saveUserCreatedRulesToFile(filterFile);
            
        } catch (Exception e) {
            System.err.println("[FishyAddons] Failed to save user created rules: " + e.getMessage());
        }
    }
    
    public static List<RuleFactory.SeaCreatureData.CreatureConfig> getSeaCreatureData() {
        return RuleFactory.getCreatures();
    }
    
    public static Map<String, RuleFactory.SeaCreatureData.CategoryConfig> getSeaCreatureCategories() {
        return RuleFactory.getCategories();
    }
    
    public static void saveSeaCreatureConfig() {
        try {
            saveSeaCreatureRulesToFile(filterFile);
        } catch (Exception e) {
            System.err.println("[FishyAddons] Failed to save sea creature configuration: " + e.getMessage());
        }
    }
    
    public static void loadConfig() {
        try {
            File configDir = new File(MinecraftClient.getInstance().runDirectory, "config/fishyaddons");
            filterFile = new File(configDir, "filter_rules.json");
            
            if (filterFile.exists()) {
                loadFilterRulesFromFile(filterFile);
            } else {
                System.out.println("[FishyAddons] No saved filter rules found, using defaults");
            }
        } catch (Exception e) {
            System.err.println("[FishyAddons] Failed to load filter configuration: " + e.getMessage());
        }
    }
    
    /**
     * Simplified config data structure for string-only user rules and hardcoded rule toggles
     */
    public static class FilterConfigData {
        private Map<String, UserRule> userRules = new LinkedHashMap<>();
        private Map<String, DefaultRuleModification> defaultRuleModifications = new ConcurrentHashMap<>();
        
        /**
         * User-created string-matching rules
         */
        public static class UserRule {
            private String searchText;
            private String replacement;
            private int priority;
            private boolean enabled;
            private boolean requireFullMatch;
            
            // Convert from full Rule
            public static UserRule fromFilterRule(Rule rule) {
                UserRule userRule = new UserRule();
                userRule.searchText = rule.getSearchText();
                userRule.replacement = rule.getReplacement();
                userRule.priority = rule.getPriority();
                userRule.enabled = rule.isEnabled();
                userRule.requireFullMatch = rule.requireFullMatch();
                return userRule;
            }
            
            public Rule toFilterRule() {
                return new Rule(searchText, replacement, priority, enabled, requireFullMatch);
            }
        }
        
        public static class DefaultRuleModification {
            private Boolean enabled;
            private String replacement;
            private String dhPrefix;
            private Integer priority;
            
            public DefaultRuleModification() {}
            
            public DefaultRuleModification(Boolean enabled, String replacement, String dhPrefix, Integer priority) {
                this.enabled = enabled;
                this.replacement = replacement;
                this.dhPrefix = dhPrefix;
                this.priority = priority;
            }
        }
    }
    
    private static void saveSeaCreatureRulesToFile(File file) throws IOException {
        FilterConfigData configData = new FilterConfigData();
        
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                FilterConfigData existingConfig = new Gson().fromJson(reader, FilterConfigData.class);
                if (existingConfig != null) {
                    configData.userRules = existingConfig.userRules != null ? 
                        existingConfig.userRules : new ConcurrentHashMap<>();
                }
            } catch (Exception e) {
                System.err.println("[FishyAddons] Warning: Could not load existing config, will overwrite: " + e.getMessage());
                configData.userRules = new ConcurrentHashMap<>();
            }
        } else {
            configData.userRules = new ConcurrentHashMap<>();
        }
        
        for (Map.Entry<String, Rule> entry : USER_RULES.entrySet()) {
            String ruleName = entry.getKey();
            Rule userRule = entry.getValue();
            Rule defaultRule = DEFAULT_RULES.get(ruleName);
            
            if (defaultRule != null && ruleName.startsWith("sc_")) {
                FilterConfigData.DefaultRuleModification mod = new FilterConfigData.DefaultRuleModification();
                boolean hasModifications = false;
                
                if (userRule.isEnabled() != defaultRule.isEnabled()) {
                    mod.enabled = userRule.isEnabled();
                    hasModifications = true;
                }
                if (!java.util.Objects.equals(userRule.getReplacement(), defaultRule.getReplacement())) {
                    mod.replacement = userRule.getReplacement();
                    hasModifications = true;
                }
                if (!java.util.Objects.equals(userRule.getDhPrefix(), defaultRule.getDhPrefix())) {
                    mod.dhPrefix = userRule.getDhPrefix();
                    hasModifications = true;
                }
                if (userRule.getPriority() != defaultRule.getPriority()) {
                    mod.priority = userRule.getPriority();
                    hasModifications = true;
                }
                
                if (hasModifications) {
                    configData.defaultRuleModifications.put(ruleName, mod);
                }
            }
        }
        
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(configData, writer);
        }
    }
    
    private static void saveUserCreatedRulesToFile(File file) throws IOException {
        FilterConfigData configData = new FilterConfigData();
        
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                FilterConfigData existingConfig = new Gson().fromJson(reader, FilterConfigData.class);
                if (existingConfig != null) {
                    configData.defaultRuleModifications = existingConfig.defaultRuleModifications != null ? 
                        existingConfig.defaultRuleModifications : new ConcurrentHashMap<>();
                }
            } catch (Exception e) {
                System.err.println("[FishyAddons] Warning: Could not load existing config, will overwrite: " + e.getMessage());
                configData.defaultRuleModifications = new ConcurrentHashMap<>();
            }
        } else {
            configData.defaultRuleModifications = new ConcurrentHashMap<>();
        }
        
        for (Map.Entry<String, Rule> entry : USER_RULES.entrySet()) {
            String ruleName = entry.getKey();
            Rule userRule = entry.getValue();
            
            if (!DEFAULT_RULES.containsKey(ruleName)) {
                configData.userRules.put(ruleName, FilterConfigData.UserRule.fromFilterRule(userRule));
            }
        }
        
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(configData, writer);
        }
    }
    
    private static void loadFilterRulesFromFile(File file) throws IOException {
        Gson gson = new Gson();
        
        try (FileReader reader = new FileReader(file)) {
            FilterConfigData configData = gson.fromJson(reader, FilterConfigData.class);
            
            if (configData == null) {
                System.err.println("[FishyAddons] Invalid filter config data");
                return;
            }
            
            USER_RULES.clear();
            
            if (configData.userRules != null) {
                for (Map.Entry<String, FilterConfigData.UserRule> entry : configData.userRules.entrySet()) {
                    String ruleName = entry.getKey();
                    FilterConfigData.UserRule userRule = entry.getValue();
                    
                    Rule fullRule = userRule.toFilterRule();
                    USER_RULES.put(ruleName, fullRule);
                }
            }
            
            if (configData.defaultRuleModifications != null) {
                for (Map.Entry<String, FilterConfigData.DefaultRuleModification> entry : 
                     configData.defaultRuleModifications.entrySet()) {
                    
                    String ruleName = entry.getKey();
                    FilterConfigData.DefaultRuleModification mod = entry.getValue();
                    Rule defaultRule = DEFAULT_RULES.get(ruleName);
                    
                    if (defaultRule != null) {
                        Rule modifiedRule = clone(defaultRule);
                        
                        if (mod.enabled != null) {
                            modifiedRule.setEnabled(mod.enabled);
                        }
                        if (mod.replacement != null) {
                            modifiedRule.setReplacement(mod.replacement);
                        }
                        if (mod.priority != null) {
                            modifiedRule.setPriority(mod.priority);
                        }
                        
                        USER_RULES.put(ruleName, modifiedRule);
                    } else {
                        System.err.println("[FishyAddons] Default rule not found for modification: " + ruleName);
                    }
                }
            }
        }
    }
    
    private static Rule clone(Rule original) {
        if (original.hasAlternativeFormat()) {
            return new Rule(
                original.getSearchText(),
                original.getReplacement(),
                original.getDhPrefix(),
                original.getTriggerMessages(),
                original.getPriority(),
                original.isEnabled()
            );
        } else {
            return new Rule(
                original.getSearchText(),
                original.getReplacement(),
                original.getPriority(),
                original.isEnabled(),
                original.requireFullMatch()
            );
        }
    }

    public static void saveBackup() {
        if (filterFile == null || !filterFile.exists()) {
            return;
        }
        
        try {
            File backupDir = new File(MinecraftClient.getInstance().runDirectory, "config/fishyaddons/backup");
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }
            
            File backupFile = new File(backupDir, "filter_rules.json");
            java.nio.file.Files.copy(filterFile.toPath(), backupFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            System.err.println("[FishyAddons] Failed to backup filter config: " + e.getMessage());
        }
    }
}
