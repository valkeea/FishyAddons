package me.valkeea.fishyaddons.tracker.collection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.valkeea.fishyaddons.hud.elements.interactive.CollectionDisplay;
import me.valkeea.fishyaddons.util.ServerCommand;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/**
 * Scans recipes to extract item craft conversion ratios.
 */
public class RecipeScanner {
    private static final int SUPERCRAFT_SLOT = 32;
    private static final int CRAFTED_SLOT = 25;
    private static final int MAX_RECURSION_DEPTH = 50;
    
    // Special recipe name replacements for /recipe command
    private static final Map<String, String> RECIPE_REPLACEMENTS = Map.of(
        "mushroom", "red mushroom",
        "gemstone", "ruby gemstone"
    );
    
    private static final Pattern RECIPE_PATTERN = Pattern.compile(
        "[✔✖]\\s*[\\d,]+/(\\d+)(?:\\s*\\([^)]+\\))?\\s*(.+)$"
    );
    
    private RecipeScanner() {} 

    /**
     * Scan a Supercraft GUI for item craft conversions
     * @param title The GUI title
     * @param getSlotStack Function to get ItemStack at slot index
     * @return true if a recipe was found and registered
     */
    public static boolean scanRecipeGui(ItemStack recipeSlot, ItemStack craftSlot) {
        if (recipeSlot == null || recipeSlot.isEmpty() || craftSlot == null || craftSlot.isEmpty()) {
            return false;
        }

        var itemName = craftSlot.getName().getString();
        if (itemName == null || itemName.isEmpty()) {
            return false;
        }
        
        var stackName = recipeSlot.getName().getString();
        if (stackName.equals("Supercraft")) {
            
            boolean foundCollectionRecipe = scanSupercraftSlot(itemName, recipeSlot);
            updateIfNested(itemName, recipeSlot);
            
            return foundCollectionRecipe;
        }
        
        return false;
    }

    public static boolean scanRecipeGui(SlotStackProvider getSlotStack) {
        return scanRecipeGui(getSlotStack.get(SUPERCRAFT_SLOT), getSlotStack.get(CRAFTED_SLOT));
    }

    /**
     * Scan Supercraft slot for recipe ingredients
     */
    private static boolean scanSupercraftSlot(String enchantedName, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        
        var lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) return false;
        
        List<Text> tooltip = lore.lines();
        if (tooltip == null || tooltip.isEmpty()) return false;
        
        int required = 0;
        int ingredientCount = 0;
        String itemName = null;

        for (Text line : tooltip) {

            String lineStr = line.getString();
            String cleanLine = lineStr.replaceAll("§.", "").trim();
            Matcher m = RECIPE_PATTERN.matcher(cleanLine);

            if (m.find()) {
                try {
                    required = Integer.parseInt(m.group(1));
                    itemName = m.group(2).trim();
                    ingredientCount++;
                } catch (NumberFormatException e) {
                    // Continue to next line
                }
            }
        }

        if (ingredientCount == 1 && required > 0 && itemName != null) {
            registerConversion(enchantedName, itemName, required);
            return true;
        }
        
        return false;
    }

    private static void registerConversion(String enchantedName, String itemName, int required) {

        CraftingRecipes.registerConversion(
            enchantedName,
            itemName,
            required
        );
        
        CraftingRecipes.save();
        analyze(enchantedName, itemName);
        CollectionTracker.onRecipeDiscovered(enchantedName);
    }    

    /**
     * Scan supercraft slot and add as goal if it maps to a known collection.
     */
    public static void addAsGoal() {
        
        var supercraftStack = CollectionTracker.getProvider().get(SUPERCRAFT_SLOT);
        if (supercraftStack == null || supercraftStack.isEmpty()) return;
        
        var craftedItemStack = CollectionTracker.getProvider().get(CRAFTED_SLOT);
        if (craftedItemStack == null || craftedItemStack.isEmpty()) return;

        String itemName = craftedItemStack.getName().getString();
        if (itemName == null || itemName.isEmpty()) return;      

        Map<String, Integer> ingredients = extractAllIngredients(supercraftStack);
        if (ingredients.isEmpty()) return;
        
        CraftingRecipes.setGoal(itemName, ingredients, craftedItemStack);
        CollectionDisplay.refreshDisplay();

    }
    
    /**
     * Extract all ingredients from Supercraft lore
     * @return Map of ingredient name -> quantity required
     */
    private static Map<String, Integer> extractAllIngredients(ItemStack supercraftStack) {
        Map<String, Integer> ingredients = new HashMap<>();
        
        var lore = supercraftStack.get(DataComponentTypes.LORE);
        if (lore == null) return ingredients;
        
        List<Text> tooltip = lore.lines();
        if (tooltip == null || tooltip.isEmpty()) return ingredients;
        
        for (Text line : tooltip) {
            String lineStr = line.getString();
            String cleanLine = lineStr.replaceAll("§.", "").trim();
            
            Matcher m = RECIPE_PATTERN.matcher(cleanLine);
            if (m.find()) {

                try {
                    int required = Integer.parseInt(m.group(1));
                    String ingredientName = m.group(2).trim();
                    ingredients.put(ingredientName, required);

                } catch (NumberFormatException e) {
                    // Skip malformed lines
                }
            }
        }
        
        return ingredients;
    }
    
    /**
     * If the crafted item is used as an ingredient in any known goal recipes,
     * expand those recipes to include the full costs of the sub-recipe.
     */
    private static void updateIfNested(String recipeName, ItemStack supercraftStack) {

        var recipeIngredients = extractAllIngredients(supercraftStack);
        if (recipeIngredients.isEmpty()) return;
        
        String normalizedRecipeName = normalizeName(recipeName);
        
        var allGoals = CraftingRecipes.getAllGoals();
        
        for (var goalEntry : allGoals.entrySet()) {
            var goalName = goalEntry.getKey();
            var goal = goalEntry.getValue();
            var goalIngredients = goal.getIngredients();
            
            for (var ingredientEntry : goalIngredients.entrySet()) {
                if (normalizeName(ingredientEntry.getKey()).equals(normalizedRecipeName)) {
                    expandGoalRecipe(goalName, normalizedRecipeName, recipeIngredients, 
                                   ingredientEntry.getValue(), goal);
                    break;
                }
            }
        }
    }
    
    /**
     * Expand a goal's ingredients to include the full costs of a sub-recipe
     */
    private static void expandGoalRecipe(String goalName, String subRecipeName, 
                                         Map<String, Integer> subIngredients,
                                         int quantityNeeded,
                                         CraftingRecipes.GoalRecipe existingGoal) {
        var expanded = new HashMap<>(existingGoal.getIngredients());
        
        expanded.remove(subRecipeName);
        
        for (var subIngredient : subIngredients.entrySet()) {
            String ingredientName = normalizeName(subIngredient.getKey());
            int ingredientCost = subIngredient.getValue();
            int totalCost = ingredientCost * quantityNeeded;
            
            expanded.merge(ingredientName, totalCost, (a, b) -> a + b);
        }
        
        CraftingRecipes.setGoal(goalName, expanded, existingGoal.getItemStack());
    }

    /**
     * Analyze a recipe to determine if the crafted item should map to a collection.
     * Only proceeds if:
     * - Recipe has 1-2 unique item types
     * - Recipe ingredient can be traced to a known collection item
     */
    private static void analyze(String craftedItem, String ingredient) {
        craftedItem = normalizeName(craftedItem);
        ingredient = normalizeName(ingredient);
        
        String baseCollection = traceToCollection(ingredient);
        
        if (baseCollection != null) {
            // Map the crafted item to base collection
            CollectionData.learnCraftMapping(craftedItem, baseCollection);
            
            // Might be an intermediate craft - save for future reference
            CollectionData.learnCraftMapping(ingredient, baseCollection);
        }
    }
    
    /**
     * Trace an item back to its base collection through the craft conversion chain.
     * 
     * @param itemName The item to trace
     * @return The base collection name if found, null otherwise
     */
    private static String traceToCollection(String itemName) {
        return traceToCollectionRecursive(itemName, new HashSet<>(), 0);
    }
    
    /**
     * Recursive helper with cycle and depth protection
     */
    private static String traceToCollectionRecursive(String itemName, Set<String> visited, int depth) {
        itemName = normalizeName(itemName);
        
        if (depth > MAX_RECURSION_DEPTH) {
            System.err.println("[RecipeScanner] Max recursion depth exceeded for " + itemName);
            return null;
        }
        
        if (visited.contains(itemName)) {
            System.err.println("[RecipeScanner] Cycle detected while tracing " + itemName);
            return null;
        }
        
        visited.add(itemName);
        
        if (CollectionData.knownBaseDrop(itemName)) return itemName;
        
        String existing = CollectionData.getMappedCollection(itemName);
        if (existing != null) return existing;
        
        if (CraftingRecipes.known(itemName)) {
            String baseItem = CraftingRecipes.getBaseItem(itemName);
            if (baseItem != null) {
                return traceToCollectionRecursive(baseItem, visited, depth + 1);
            }
        }
        
        return findCollectionInName(itemName);
    }
    
    /**
     * Search for a known collection within an item name by checking individual words and substrings.
     * 
     * @param itemName The item name to search
     * @return The collection name if found, null otherwise
     */
    private static String findCollectionInName(String itemName) {
        String[] words = itemName.split(" ");
        
        for (String word : words) {
            if (isCandidate(word) && CollectionData.knownBaseDrop(word)) {
                return word;
            }
        }
        
        return findMatchingSubstring(words);
    }
    
    /**
     * Check if a word should be considered for collection matching.
     */
    private static boolean isCandidate(String word) {
        return !word.equals("enchanted") && !word.equals("red") && !word.equals("brown");
    }
    
    /**
     * Find a substring of words that matches a known collection.
     */
    private static String findMatchingSubstring(String[] words) {
        for (int i = 0; i < words.length; i++) {
            for (int j = i + 1; j <= words.length; j++) {
                String potential = buildSubstring(words, i, j);
                if (!potential.isEmpty() && CollectionData.knownBaseDrop(potential)) {
                    return potential;
                }
            }
        }
        return null;
    }
    
    /**
     * Build a substring from word array.
     */
    private static String buildSubstring(String[] words, int start, int end) {
        StringBuilder substring = new StringBuilder();
        for (int k = start; k < end; k++) {
            if (k > start) substring.append(" ");
            String word = words[k];
            if (!word.equals("enchanted")) {
                substring.append(word);
            }
        }
        return substring.toString().trim();
    }

    private static String normalizeName(String name) {
        return CollectionData.normalize(name);
    }

    public static void openRecipeFor(String itemName) {
        for (Map.Entry<String, String> entry : RECIPE_REPLACEMENTS.entrySet()) {
            String keyword = entry.getKey();
            String replacement = entry.getValue();
            
            if (itemName.contains(keyword)) {
                itemName = itemName.replace(keyword, replacement);
            }
        }
        ServerCommand.send("recipe " + itemName);
    }

    /**
     * Functional interface for getting ItemStack at slot
     */
    @FunctionalInterface
    public interface SlotStackProvider {
        ItemStack get(int slotIndex);
    }
}
