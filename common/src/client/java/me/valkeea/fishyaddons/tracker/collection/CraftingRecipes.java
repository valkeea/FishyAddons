package me.valkeea.fishyaddons.tracker.collection;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import me.valkeea.fishyaddons.util.JsonUtil;
import net.minecraft.item.ItemStack;

/**
 * Handles crafting recipe conversions and goal tracking.
 */
public class CraftingRecipes {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONVERSIONS_FILE = new File("config/fishyaddons/data/craftmapping.json");
    private static final File GOALS_FILE = new File("config/fishyaddons/data/recipegoals.json");
    private static final int MAX_RECURSION_DEPTH = 50;
    
    // High tier item name -> base item name
    private static final Map<String, String> craftToBase = new HashMap<>();
    
    // High tier item name -> base item count needed
    private static final Map<String, Integer> conversionRatios = new HashMap<>();
    
    // Recipe name -> goal recipe data
    private static final Map<String, GoalRecipe> savedGoals = new HashMap<>();
    
    private CraftingRecipes() {}

    /**
     * Register a conversion ratio between a crafted item and its base ingredient.
     */
    protected static void registerConversion(String craftedName, String baseName, int baseAmount) {
        craftedName = normalize(craftedName);
        baseName = normalize(baseName);
        
        craftToBase.put(craftedName, baseName);
        conversionRatios.put(craftedName, baseAmount);
        CollectionData.refreshDisplays();
    }

    /**
     * Get the ultimate base item name by following the conversion chain.
     */
    protected static String getBaseItem(String craftedName) {
        return getBaseItemRecursive(normalize(craftedName), new HashMap<>(), 0);
    }
    
    /**
     * Recursive helper to follow conversion chain with cycle and depth protection.
     */
    private static String getBaseItemRecursive(String itemName, Map<String, Boolean> visited, int depth) {

        if (depth > MAX_RECURSION_DEPTH) {
            System.err.println("[CollectionTracker] Max recursion depth exceeded for " + itemName);
            return itemName;
        }

        if (visited.containsKey(itemName)) {
            System.err.println("[CollectionTracker] Cycle detected in conversion chain for " + itemName);
            return itemName;
        }
        
        visited.put(itemName, true);
        
        String directBase = craftToBase.get(itemName);
        if (directBase == null) return itemName;
        
        return getBaseItemRecursive(directBase, visited, depth + 1);
    }

    /**
     * Get the total conversion ratio by following the full chain.
     */
    protected static Integer getConversionRatio(String craftedName) {
        return getConversionRatioRecursive(normalize(craftedName), new HashMap<>(), 0);
    }
    
    /**
     * Recursive helper to calculate total ratio with cycle and depth protection
     */
    private static Integer getConversionRatioRecursive(String itemName, Map<String, Boolean> visited, int depth) {

        if (depth > MAX_RECURSION_DEPTH) {
            System.err.println("[CollectionTracker] Max recursion depth exceeded for " + itemName);
            return 1;
        }

        if (visited.containsKey(itemName)) {
            System.err.println("[CollectionTracker] Cycle detected in conversion chain for " + itemName);
            return 1;
        }
        
        visited.put(itemName, true);
        
        Integer directRatio = conversionRatios.get(itemName);
        if (directRatio == null) return null;
        
        String directBase = craftToBase.get(itemName);
        if (directBase == null) {
            // Has ratio but no base? Shouldn't happen but handled here
            return directRatio;
        }
        
        Integer nextTierRatio = getConversionRatioRecursive(directBase, visited, depth + 1);
        if (nextTierRatio == null) return directRatio; // Next tier is base item
        
        return directRatio * nextTierRatio; // Multiply ratios through the chain
    }

    /**
     * Check if an item is a registered craft
     */
    protected static boolean known(String itemName) {
        return craftToBase.containsKey(normalize(itemName));
    }

    /**
     * Convert crafted item quantity to base item quantity
     * @return Amount of base items, or 0 if not registered
     */
    protected static int convertToBase(String craftedName, int craftedQuantity) {
        Integer ratio = getConversionRatio(craftedName);
        if (ratio == null) {
            return 0;
        }
        return craftedQuantity * ratio;
    }

    private static String normalize(String name) {
        return CollectionData.normalize(name);
    }

    public static void save() {
        try {
            CONVERSIONS_FILE.getParentFile().mkdirs();
            
            Map<String, ConversionData> data = new HashMap<>();
            for (Map.Entry<String, String> entry : craftToBase.entrySet()) {
                String crafted = entry.getKey();
                String base = entry.getValue();
                Integer ratio = conversionRatios.get(crafted);
                
                if (ratio != null) {
                    data.put(crafted, new ConversionData(base, ratio));
                }
            }
            
            try (FileWriter writer = new FileWriter(CONVERSIONS_FILE)) {
                GSON.toJson(data, writer);
            }

        } catch (Exception e) {
            System.err.println("[CollectionTracker] Failed to save conversions: " + e.getMessage());
            e.printStackTrace();
        }
    }

    protected static void load() {
        if (!CONVERSIONS_FILE.exists()) return;
        
        try (FileReader reader = new FileReader(CONVERSIONS_FILE)) {
            Type type = new TypeToken<Map<String, ConversionData>>(){}.getType();
            Map<String, ConversionData> data = GSON.fromJson(reader, type);
            
            if (data != null) {
                craftToBase.clear();
                conversionRatios.clear();
                
                for (Map.Entry<String, ConversionData> entry : data.entrySet()) {
                    String crafted = entry.getKey();
                    ConversionData conversion = entry.getValue();
                    
                    craftToBase.put(crafted, conversion.baseName);
                    conversionRatios.put(crafted, conversion.baseAmount);
                }

            }
        } catch (Exception e) {
            System.err.println("[CollectionTracker] Failed to load conversions: " + e.getMessage());
            e.printStackTrace();
        }

        loadGoals();
    }

    protected static void clear() {
        craftToBase.clear();
        conversionRatios.clear();
        savedGoals.clear();
    }

    // --- Goal management ---
    
    /**
     * Save a recipe as a goal
     * @param recipeName Name of the recipe
     * @param ingredients Map of ingredient name -> quantity required
     * @param itemStack The ItemStack representing the crafted item
     */
    protected static void setGoal(String recipeName, Map<String, Integer> ingredients, ItemStack itemStack) {
        recipeName = normalize(recipeName);
        
        Map<String, Integer> normalizedIngredients = new HashMap<>();
        for (Map.Entry<String, Integer> entry : ingredients.entrySet()) {
            normalizedIngredients.put(normalize(entry.getKey()), entry.getValue());
        }
        
        String serializedStack = JsonUtil.serializeItemStack(itemStack);
        
        savedGoals.put(recipeName, new GoalRecipe(normalizedIngredients, serializedStack));
        saveGoals();
    }
    
    /** Get a saved goal recipe */
    public static GoalRecipe getGoal(String recipeName) {
        return savedGoals.get(normalize(recipeName));
    }
    
    /** Get all saved goal recipes */
    public static Map<String, GoalRecipe> getAllGoals() {
        return new HashMap<>(savedGoals);
    }
    
    /** Remove a goal recipe */
    public static void removeGoal(String recipeName) {
        savedGoals.remove(normalize(recipeName));
        saveGoals();
    }
    
    /** Check if a recipe is saved as a goal */
    public static boolean hasGoal(String recipeName) {
        return savedGoals.containsKey(normalize(recipeName));
    }
    
    /** Clear all goal recipes */
    public static void clearGoals() {
        savedGoals.clear();
        saveGoals();
    }
    
    private static void saveGoals() {
        try {
            GOALS_FILE.getParentFile().mkdirs();
            
            try (FileWriter writer = new FileWriter(GOALS_FILE)) {
                GSON.toJson(savedGoals, writer);
            }

        } catch (Exception e) {
            System.err.println("[CollectionTracker] Failed to save goals: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void loadGoals() {
        if (!GOALS_FILE.exists()) return;
        
        try (FileReader reader = new FileReader(GOALS_FILE)) {
            Type type = new TypeToken<Map<String, GoalRecipe>>(){}.getType();
            Map<String, GoalRecipe> data = GSON.fromJson(reader, type);
            
            if (data != null) {
                savedGoals.clear();
                savedGoals.putAll(data);
            }
        } catch (Exception e) {
            System.err.println("[CollectionTracker] Failed to load goals: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static class GoalRecipe {
        private final Map<String, Integer> ingredients;
        private final String serializedItemStack;
        
        public GoalRecipe(Map<String, Integer> ingredients, String serializedItemStack) {
            this.ingredients = ingredients;
            this.serializedItemStack = serializedItemStack;
        }
        
        public Map<String, Integer> getIngredients() {
            return new HashMap<>(ingredients);
        }
        
        public String getSerializedItemStack() {
            return serializedItemStack;
        }
        
        public ItemStack getItemStack() {
            if (serializedItemStack == null || serializedItemStack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            return JsonUtil.deserializeItemStack(serializedItemStack);
        }
    }
    
    private static class ConversionData {
        String baseName;
        int baseAmount;
        
        ConversionData(String baseName, int baseAmount) {
            this.baseName = baseName;
            this.baseAmount = baseAmount;
        }
    }
}
