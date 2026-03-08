package me.valkeea.fishyaddons.tracker.collection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.valkeea.fishyaddons.util.text.Color;
import net.minecraft.text.Text;

@SuppressWarnings("squid:S6548")
public class ActiveDisplay {
    private static ActiveDisplay instance;

    private List<ProgressData> cachedVisibleCollections = List.of();
    private List<String> cachedRecipePrompts = null;
    private Map<String, Double> cachedRates = new HashMap<>();
    
    private List<Text> cachedFormattedLines = null;
    private List<Text> cachedRecipePromptLines = null;
    private static final long LINE_FORMAT_REFRESH_MS = 5000; 
    
    private Text cachedTimerLine = null;
    private long lastTimerUpdate = 0;
    private static final long TIMER_REFRESH_MS = 1000;
    
    private static final Map<String, String> enhancedNameCache = new HashMap<>();
    
    private boolean collectionsInvalidated = true;
    private boolean promptsInvalidated = true;
    private boolean linesInvalidated = true;
    
    private Map<String, Long> lastKnownGain = new HashMap<>();
    private long lastElapsedTime = 0;
    
    private ActiveDisplay() {}
    
    public static ActiveDisplay getInstance() {
        if (instance == null) {
            instance = new ActiveDisplay();
        }
        return instance;
    }
    
    /**
     * Get visible collections.
     */
    public List<ProgressData> getVisibleCollections() {
        if (collectionsInvalidated) {
            refreshVisibleCollections();
            collectionsInvalidated = false;
        }
        return cachedVisibleCollections;
    }
    
    /**
     * Get recipe prompts.
     */
    public List<String> getRecipePrompts() {
        if (promptsInvalidated) {
            refreshRecipePrompts();
            promptsInvalidated = false;
        }
        return cachedRecipePrompts;
    }
    
    /**
     * Get the rate for an item.
     */
    public double getCachedRate(String itemName) {

        long currentGain = CollectionData.getSessionGain(itemName);
        Long lastGain = lastKnownGain.get(itemName);
        long currentElapsed = CollectionData.getSessionDuration();
        boolean gainChanged = lastGain == null || lastGain != currentGain;
        boolean timeChanged = Math.abs(currentElapsed - lastElapsedTime) > LINE_FORMAT_REFRESH_MS;

        if (gainChanged || timeChanged) {
            double rate = CollectionData.getGainRatePerHour(itemName);
            cachedRates.put(itemName, rate);
            lastKnownGain.put(itemName, currentGain);
            linesInvalidated = true;
        }
        
        return cachedRates.getOrDefault(itemName, 0.0);
    }
    
    /**
     * Get formatted collection lines.
     */
    public List<Text> getFormattedCollectionLines(int color) {
        if (linesInvalidated || cachedFormattedLines == null) {
            refreshFormattedLines(color);
            linesInvalidated = false;
        }
        return cachedFormattedLines;
    }
    
    /**
     * Get formatted recipe prompt lines.
     */
    public List<Text> getFormattedRecipePromptLines() {
        if (promptsInvalidated || cachedRecipePromptLines == null) {
            refreshRecipePromptLines();
        }
        return cachedRecipePromptLines;
    }
    
    /**
     * Get formatted timer line with 1-second caching.
     */
    public Text getTimerLine(int displayColor) {
        long now = System.currentTimeMillis();
        if (cachedTimerLine == null || (now - lastTimerUpdate) >= TIMER_REFRESH_MS) {
            cachedTimerLine = formatTimerLine(displayColor);
            lastTimerUpdate = now;
        }
        return cachedTimerLine;
    }
    
    /**
     * Build formatted Text objects for visible collections.
     */
    private void refreshFormattedLines(int color) {
        if (collectionsInvalidated) {
            refreshVisibleCollections();
            collectionsInvalidated = false;
        }
        
        cachedFormattedLines = new ArrayList<>();
        for (ProgressData col : cachedVisibleCollections) {
            cachedFormattedLines.add(formatCollectionLine(col, color));
        }

        lastElapsedTime = CollectionData.getSessionDuration();       
    }
    
    /**
     * Format a single collection line as a Text object.
     */
    private Text formatCollectionLine(ProgressData col, int color) {
        long gained = CollectionData.getSessionGain(col.itemName);
        long total = CollectionData.getCurrentCollection(col.itemName);
        double rate = getCachedRate(col.itemName);
        
        String enhancedName = enhanceItemName(col.itemName);
        String totalStr = " §8| §r" + formatNum(total) + " §8|";
        String rateStr = rate > 0 ? formatNum((long)rate) + "§8/h " : "§80/h ";
        
        return Text.literal(enhancedName)
            .styled(s -> s.withColor(color))
            .append(style(totalStr, 0xFF888888))
            .append(style(" " + rateStr, Color.desaturate(color, 0.8f)))
            .append(style(" +" + formatNum(gained), color));
    }
    
    /**
     * Refresh recipe prompt lines.
     */
    private void refreshRecipePromptLines() {
        if (promptsInvalidated) {
            refreshRecipePrompts();
            promptsInvalidated = false;
        }
        
        cachedRecipePromptLines = new ArrayList<>();
        if (!cachedRecipePrompts.isEmpty()) {
            cachedRecipePromptLines.add(Text.literal("§cMissing crafts! Use /recipe or click the line."));
            for (String recipe : cachedRecipePrompts) {
                cachedRecipePromptLines.add(Text.literal(" §7- " + enhanceItemName(recipe)));
            }
        }
    }
    
    /**
     * Format the timer line based on current tracking state.
     */
    private Text formatTimerLine(int displayColor) {
        int color;
        String timeStr;

        if (CollectionTracker.isDownTiming()) {
            long downTime = CollectionTracker.getCurrentPauseDurationMs();
            timeStr = String.format("Downtiming§8: §7%02d:%02d", 
                (downTime / 60000) % 60, 
                (downTime / 1000) % 60);
            color = 0xFFFF5555;

        } else if (CollectionTracker.isPaused()) {
            long pausedFor = CollectionTracker.getCurrentPauseDurationMs();
            long resetIn = 15 * 60 * 1000 - pausedFor;

            timeStr = String.format("Paused for§8: §7%02d:%02d§7, §8Reset in§8: §7%02d:%02d", 
                (pausedFor / 60000) % 60, 
                (pausedFor / 1000) % 60,
                (resetIn / 60000) % 60,
                (resetIn / 1000) % 60);
            color = 0xFFAAAAAA;            

        } else {
            long elapsed = CollectionTracker.getTimeElapsedMs();
            timeStr = String.format("Tracked for§8: §7%02d:%02d:%02d",
                (elapsed / 3600000) % 60,
                (elapsed / 60000) % 60,
                (elapsed / 1000) % 60);
            color = Color.brighten(displayColor, 0.5f);
        }
        
        return style(timeStr, color);
    }
    
    /**
     * Force refresh of visible collections
     */
    private void refreshVisibleCollections() {
        Map<String, Long> tracked = CollectionData.getActiveTracked();
        VisibilityManager manager = VisibilityManager.getInstance();

        if (tracked.isEmpty()) {
            cachedVisibleCollections = List.of();
            return;
        }
        
        cachedVisibleCollections = tracked.entrySet().stream()
            .filter(e -> !manager.isHidden(e.getKey()))
            .map(e -> new ProgressData(e.getKey(), e.getValue()))
            .sorted((a, b) -> Long.compare(b.currentAmount, a.currentAmount))
            .toList();
        
        List<String> visibleItems = cachedVisibleCollections.stream()
            .map(pd -> pd.itemName)
            .toList();
        
        lastKnownGain.keySet().removeIf(item -> !visibleItems.contains(item));
        cachedRates.keySet().removeIf(item -> !visibleItems.contains(item));
    }
    
    /**
     * Force refresh of recipe prompts cache
     */
    private void refreshRecipePrompts() {
        cachedRecipePrompts = CollectionData.getActivePendingRecipes();
    }

    public static List<Text> getGoalBreakdown() {
        return GoalManager.getInstance().getGoalBreakdown();
    }

    public void invalidateAll() {
        collectionsInvalidated = true;
        promptsInvalidated = true;
        linesInvalidated = true;
    }
    
    public void invalidateCollections() {
        collectionsInvalidated = true;
        linesInvalidated = true;
    }
    
    public void invalidatePrompts() {
        promptsInvalidated = true;
    }
    
    public void reset() {
        cachedVisibleCollections = null;
        cachedRecipePrompts = null;
        cachedFormattedLines = null;
        cachedRecipePromptLines = null;
        cachedTimerLine = null;
        cachedRates.clear();
        lastKnownGain.clear();
        collectionsInvalidated = true;
        promptsInvalidated = true;
        linesInvalidated = true;
        lastElapsedTime = 0;
        lastTimerUpdate = 0;
    }
    
    // --- Formatting utilities ---
    
    private static String enhanceItemName(String itemName) {
        if (itemName == null || itemName.isEmpty()) {
            return "Unknown";
        }
        
        String cached = enhancedNameCache.get(itemName);
        if (cached != null) return cached;
    
        String[] words = itemName.split("_");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1).toLowerCase());
                }
                if (i < words.length - 1) {
                    result.append(" ");
                }
            }
        }
        
        String enhanced = result.toString();
        enhancedNameCache.put(itemName, enhanced);
        return enhanced;
    }
    
    private static String formatNum(long num) {
        if (num >= 1_000_000_000) {
            return String.format("%.2fB", num / 1_000_000_000.0);
        } else if (num >= 1_000_000) {
            return String.format("%.2fM", num / 1_000_000.0);
        } else if (num >= 1_000) {
            return String.format("%.2fK", num / 1_000.0);
        }
        return String.valueOf(num);
    }
    
    private static Text style(String s, int color) {
        return Text.literal(s).styled(style -> style.withColor(color));
    }

    public static class ProgressData {
        public final String itemName;
        public final long currentAmount;
        
        public ProgressData(String itemName, long currentAmount) {
            this.itemName = itemName;
            this.currentAmount = currentAmount;
        }
    }
}
