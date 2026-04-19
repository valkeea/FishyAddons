package me.valkeea.fishyaddons.tracker.collection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import me.valkeea.fishyaddons.tool.FishyMode;
import me.valkeea.fishyaddons.tracker.collection.CraftingRecipes.GoalRecipe;
import me.valkeea.fishyaddons.tracker.profit.TrackedItemData;
import me.valkeea.fishyaddons.util.text.StringUtils;
import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.api.StringKey;
import net.minecraft.text.Text;

/**
 * Manages active goal selection and progress calculation.
 */
@SuppressWarnings("squid:S6548")
public class GoalManager {
    private static GoalManager instance;
    
    private String activeGoal = null;
    private GoalRecipe activeGoalRecipe = null;
    private double cachedProgress = 0.0;
    private boolean progressInvalidated = true;
    
    private List<Text> cachedBreakdown = null;
    private boolean breakdownInvalidated = true;
    private long lastPriceFetch = 0;
    private static final long PRICE_CACHE_MS = 300000;
    private final Map<String, PriceData> ingredientPriceCache = new java.util.HashMap<>();
    private PriceData cachedCraftedPrice = null;
    
    private GoalManager() {
        load();
    }
    
    public static GoalManager getInstance() {
        if (instance == null) {
            instance = new GoalManager();
        }
        return instance;
    }
    
    public String getActiveGoal() {
        return activeGoal;
    }

    public GoalRecipe getActiveGoalRecipe() {
        if (activeGoal == null) return null;
        return activeGoalRecipe != null ? activeGoalRecipe : CraftingRecipes.getGoal(activeGoal);
    }
    
    public boolean hasActiveGoal() {
        return activeGoal != null;
    }

    /**
     * Remove the active goal and delete it from known goals.
     */
    public static void removeActive() {
        if (instance.activeGoal != null) {
            CraftingRecipes.removeGoal(instance.activeGoal);
            instance.activeGoal = null;
            instance.activeGoalRecipe = null;
            instance.save();
            instance.invalidateProgressCache();
            instance.invalidateBreakdownCache();
        }
    }

    /**
     * Set the active goal.
     * @param goalName The goal name, or null to clear.
     */
    public void setActiveGoal(String goalName) {
        if (goalName != null && goalName.equals(activeGoal)) {
            this.activeGoal = null;
            this.activeGoalRecipe = null;
        } else {
            this.activeGoal = goalName;
            this.activeGoalRecipe = CraftingRecipes.getGoal(goalName);
        }
        save();
        invalidateProgressCache();
        invalidateBreakdownCache();
    }
    
    /**
     * Get cached goal progress.
     */
    public double getProgress() {
        if (progressInvalidated) {
            cachedProgress = calculateGoalProgress();
            progressInvalidated = false;
        }
        return cachedProgress;
    }
    
    public void invalidateProgressCache() {
        progressInvalidated = true;
    }

    public void invalidateBreakdownCache() {
        breakdownInvalidated = true;
    }

    public void clearAll() {
        invalidateProgressCache();
        invalidateBreakdownCache();
        cachedBreakdown = null;
    }
    
    /**
     * Calculate progress towards active goal based on session gains.
     * Returns the minimum progress (bottleneck) among ingredients with gains.
     * Ingredients with 0 gains are assumed to be filler items and ignored.
     */
    private double calculateGoalProgress() {
        if (activeGoal == null) return 0.0;
        
        var goalRecipe = CraftingRecipes.getGoal(activeGoal);
        if (goalRecipe == null) return 0.0;
        
        Map<String, Integer> ingredients = goalRecipe.getIngredients();
        if (ingredients.isEmpty()) return 0.0;
        
        double minProgress = Double.MAX_VALUE;
        boolean hasAnyProgress = false;
        
        for (Map.Entry<String, Integer> entry : ingredients.entrySet()) {
            String ingredientName = entry.getKey();
            int requiredAmount = entry.getValue();
            
            long gainAmount = getIngredientSessionGain(ingredientName);
            if (gainAmount > 0) {
                double progress = (double) gainAmount / requiredAmount;
                minProgress = Math.min(minProgress, progress);
                hasAnyProgress = true;
            }
        }
        
        return hasAnyProgress ? minProgress : 0.0;
    }
    
    /**
     * Get session gain for an ingredient, handling conversions if needed.
     */
    private long getIngredientSessionGain(String name) {
        name = CollectionData.normalize(name);
        
        if (CollectionData.hasBaseline(name)) {
            return CollectionData.getSessionGain(name);
        }
        
        if (CraftingRecipes.known(name)) {

            String baseItem = CraftingRecipes.getBaseItem(name);
            if (baseItem != null && CollectionData.hasBaseline(baseItem)) {

                long baseGain = CollectionData.getSessionGain(baseItem);
                Integer conversionRate = CraftingRecipes.getConversionRatio(name);
                if (conversionRate != null && conversionRate > 0) {
                    return baseGain / conversionRate;
                }
            }
        }
        
        String mapped = CollectionData.getMappedCollection(name);
        if (mapped != null && CollectionData.hasBaseline(mapped)) {
            return CollectionData.getSessionGain(mapped);
        }
        
        return 0;
    }
   
    private void load() {
        String saved = Config.get(StringKey.COLLECTION_GOAL);
        activeGoal = saved.isEmpty() ? null : saved;
    }
    
    private void save() {
        Config.set(StringKey.COLLECTION_GOAL, activeGoal == null ? "" : activeGoal);
    }
    
    public void reset() {
        activeGoal = null;
        activeGoalRecipe = null;
        save();
        invalidateProgressCache();
        invalidateBreakdownCache();
    }

    // --- Goal Breakdown ---

    private GoalBreakdownData buildGBD(Map<String, Integer> ingredients) {

        double gainedRawValue = 0.0;
        boolean hasInvalidPrices = false;
        List<Text> lines = new ArrayList<>();
        List<Text> missing = new ArrayList<>();
        int fillerCount = 0;
        
        for (var entry : ingredients.entrySet()) {

            String ingredientName = entry.getKey();
            int required = entry.getValue();
            
            long gain = getIngredientSessionGain(ingredientName);
            double itemValue = getCachedIngredientPrice(ingredientName);
            String displayName = formatIngredientName(ingredientName);
            String statusClr = gain > 0 ? "§a" : "§7";
            
            if (gain > 0) {

                double itemProgress = (double) gain / required * 100;
                double ingredientTotalValue = itemValue * required;
                String priceStatus = itemValue > 0 ? "§6" : "§c";
                lines.add(Text.literal(String.format("  %s%s: §f%,d§7/§f%,d §7(§e%.1f%%§7) %s%,.0f coins", 
                    statusClr, displayName, gain, required, itemProgress, priceStatus, ingredientTotalValue)));  

                if (itemValue > 0) {
                    gainedRawValue += itemValue * gain;
                } else {
                    hasInvalidPrices = true;
                }

            } else {
                missing.add(Text.literal(String.format("  %s%s: §f0§7/§f%,d §7(§70%%§7)", 
                    statusClr, displayName, required)));
                fillerCount++;
            }
        }

        if (!missing.isEmpty()) {
            lines.addAll(missing);
            if (fillerCount > 0) {
                lines.add(Text.literal("    §7§o(0-gain ingredients ignored in calculation)"));
            }
        }
        
        return new GoalBreakdownData(lines, gainedRawValue, hasInvalidPrices);
    }    

    private static final String LINE = "§8━━━━━━━━━━━━━━━━━━━━━━";

    /**
     * Get a breakdown of progress for each ingredient in the active goal, for tooltip display.
     */
    public List<Text> getGoalBreakdown() {
        if (!breakdownInvalidated && cachedBreakdown != null) {
            long now = System.currentTimeMillis();
            if ((now - lastPriceFetch) < PRICE_CACHE_MS) {
                return new ArrayList<>(cachedBreakdown);
            }
            breakdownInvalidated = true;
        }
        
        List<Text> lines = new ArrayList<>();
        
        if (activeGoal == null) {
            lines.add(Text.literal("§cNo active goal"));
            return lines;
        }
        
        var goalRecipe = getActiveGoalRecipe();
        if (goalRecipe == null) {
            lines.add(Text.literal("§cGoal recipe not found"));
            return lines;
        }
        
        Map<String, Integer> ingredients = goalRecipe.getIngredients();
        if (ingredients.isEmpty()) {
            lines.add(Text.literal("§cNo ingredients in recipe"));
            return lines;
        }
        
        int theme = FishyMode.getThemeColor();
        double goalValue = getCachedCraftedPrice();
        boolean goalValueInvalid = (goalValue == 0);
        var upper = StringUtils.capitalize(activeGoal);

        lines.add(styleText("§l" + upper + " §7(§6" + (goalValueInvalid ? "?" : String.format("%,.0f", goalValue)) + " coins§7)", theme));
        lines.add(Text.literal(LINE));
        
        GoalBreakdownData gbd = buildGBD(ingredients);
        lines.addAll(gbd.lines);
        
        lines.add(Text.literal(LINE));
        lines.add(styleText("§lGained Profit:", theme));
        addProfitLines(lines, gbd.gainedRawValue, gbd.hasInvalidPrices, goalValue, goalValueInvalid);
        
        addRateLines(lines, theme, gbd.gainedRawValue, gbd.hasInvalidPrices, goalValue, goalValueInvalid);

        lines.add(Text.literal(LINE));
        lines.add(styleText("Click the line to forget this goal!", 0x555555));
        
        cachedBreakdown = new ArrayList<>(lines);
        breakdownInvalidated = false;
        lastPriceFetch = System.currentTimeMillis();
        
        return lines;
    }

    private void addProfitLines(List<Text> lines, double rawGained, boolean hasInvalidPrices, double goalValue, boolean goalValueInvalid) {
        if (hasInvalidPrices) {
            lines.add(Text.literal("  §7Ingredient value: §c[Compromised]"));
        } else {
            lines.add(Text.literal(String.format("  §7Ingredient value: §f%,.0f coins", rawGained)));
        }
        
        double gainedGoalValue = goalValue * getProgress();

        if (goalValueInvalid) {
            lines.add(Text.literal("  §7Craft value: §c[Invalid/Unknown]"));
        } else {
            lines.add(Text.literal(String.format("  §7Craft value: §f%,.0f coins", gainedGoalValue)));
        }
        
        if (!hasInvalidPrices && !goalValueInvalid && rawGained > 0) {
            double profit = gainedGoalValue - rawGained;
            String profitColor = profit >= 0 ? "§a" : "§c";
            String profitPrefix = profit >= 0 ? "+" : "";
            lines.add(Text.literal(String.format("  §7Craft Profit: %s%s%,.0f coins", 
                profitColor, profitPrefix, profit)));
        }
    }

    private void addRateLines(List<Text> lines, int theme, double rawGained, boolean hasInvalidPrices, double goalValue, boolean goalValueInvalid) {
        long durationMs = CollectionData.getSessionDuration();
        long durationMinutes = durationMs / 60000;
        
        if (durationMinutes <= 0) {
            lines.add(Text.literal(LINE));
            lines.add(Text.literal("§7No session time data"));
            return;
        }
        
        lines.add(Text.literal(LINE));
        lines.add(styleText("§lProfit difference (per hour):", theme));
        
        double hoursElapsed = durationMinutes / 60.0;
        double gainedGoalValue = goalValue * getProgress();
        
        if (hasInvalidPrices) {
            lines.add(Text.literal("  §7Raw Value/hr: §c[Compromised]"));
        } else {
            double rawValuePerHour = rawGained / hoursElapsed;
            lines.add(Text.literal(String.format("  §7Raw Value/hr: §f%,.0f§7 coins", rawValuePerHour)));
        }
        
        if (goalValueInvalid) {
            lines.add(Text.literal("  §7Craft Value/hr: §c[Invalid/Unknown]"));
        } else {
            double craftedValuePerHour = gainedGoalValue / hoursElapsed;
            lines.add(Text.literal(String.format("  §7Craft Value/hr: §f%,.0f§7 coins", craftedValuePerHour)));
        }
        
        if (!hasInvalidPrices && !goalValueInvalid && rawGained > 0) {
            addProfitabilityComparison(lines, rawGained / hoursElapsed, gainedGoalValue / hoursElapsed);
        }
    }

    private void addProfitabilityComparison(List<Text> lines, double rawPerHr, double goalPerHr) {
        if (goalPerHr > rawPerHr) {
            double improvement = ((goalPerHr - rawPerHr) / rawPerHr) * 100;
            lines.add(Text.literal(String.format("  §a✓ Crafting is §e%.1f%%§a more profitable", improvement)));
        } else if (rawPerHr > goalPerHr) {
            double loss = ((rawPerHr - goalPerHr) / rawPerHr) * 100;
            lines.add(Text.literal(String.format("  §c✖ Selling raw is §e%.1f%%§c more profitable", loss)));
        } else {
            lines.add(Text.literal("  §7= Equal profitability"));
        }
    }

    private double getCachedIngredientPrice(String ingredientName) {
        long now = System.currentTimeMillis();
        var cached = ingredientPriceCache.get(ingredientName);
        
        if (cached != null && cached.isValid(now)) return cached.price;
        
        double value = TrackedItemData.getPrice(ingredientName);
        
        if (value > 0) {
            ingredientPriceCache.put(ingredientName, new PriceData(value, now));
        }
        
        return value;
    }
    
    /**
     * Get price for the crafted item with TTL.
     */
    private double getCachedCraftedPrice() {

        long now = System.currentTimeMillis();
        if (cachedCraftedPrice != null && cachedCraftedPrice.isValid(now)) {
            return cachedCraftedPrice.price;
        }
        
        double value = TrackedItemData.getPrice(activeGoal);
        if (value > 0) cachedCraftedPrice = new PriceData(value, now);
        
        return value;
    }
    
    /**
     * Format ingredient name for display
     */
    private String formatIngredientName(String ingredientName) {
        if (ingredientName == null || ingredientName.isEmpty()) {
            return "Unknown";
        }
        
        String[] parts = ingredientName.split("_");
        StringBuilder formatted = new StringBuilder();
        
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (!part.isEmpty()) {
                formatted.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    formatted.append(part.substring(1));
                }
                if (i < parts.length - 1) {
                    formatted.append(" ");
                }
            }
        }
        
        return formatted.toString();
    }    

    private Text styleText(String text, int color) {
        return Text.literal(text).styled(s -> s.withColor(color));
    }
    
    private static class GoalBreakdownData {
        final List<Text> lines;
        final double gainedRawValue;
        final boolean hasInvalidPrices;
        
        GoalBreakdownData(List<Text> lines, double gainedRawValue, boolean hasInvalidPrices) {
            this.lines = lines;
            this.gainedRawValue = gainedRawValue;
            this.hasInvalidPrices = hasInvalidPrices;
        }
    }
    
    private static class PriceData {
        final double price;
        final long timestamp;
        
        PriceData(double price, long timestamp) {
            this.price = price;
            this.timestamp = timestamp;
        }
        
        boolean isValid(long now) {
            return (now - timestamp) < PRICE_CACHE_MS;
        }
    }
}
