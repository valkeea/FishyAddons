package me.valkeea.fishyaddons.tracker.fishing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.config.StatConfig;
import me.valkeea.fishyaddons.util.FishyNotis;
import me.valkeea.fishyaddons.util.text.StringUtils;
import net.minecraft.text.Text;

public class ScData {
    private static final String TOTAL_ATTEMPTS = "total_attempts_";
    private static final String TOTAL_CATCHES = "total_catches_";
    private static final String GRAPH_PREFIX = "frequency_";
    private static final String SAVE_ERROR_MSG = "Failed to save frequency data for ";
    
    // Maximum attempts per creature for bracketing decisions
    private final Map<String, Integer> maxAttempts = new ConcurrentHashMap<>();

    // Raw catch data for accurate rate calculation
    private final Map<String, Integer> totalAttempts = new ConcurrentHashMap<>();
    private final Map<String, Integer> totalCatches = new ConcurrentHashMap<>();

    private final Map<String, Double> catchRates = new ConcurrentHashMap<>();
    private final Map<String, Integer> catchCounts = new ConcurrentHashMap<>();    

    // Histogram tracking: creature -> (attempts -> frequency)
    private final Map<String, Map<Integer, Integer>> catchGraph = new ConcurrentHashMap<>();

    private static boolean enabled = false;
    private static ScData instance = null;

    private ScData() {}

    public static ScData getInstance() {
        if (instance == null) {
            instance = new ScData();
        }
        return instance;
    }

    public static void refresh() {
        enabled = FishyConfig.getState(Key.HUD_CATCH_GRAPH_ENABLED, false) && 
                  FishyConfig.getState(Key.TRACK_SCS, false);
    }

    public static boolean isEnabled() {
        return enabled;
    }

    private void updateCatchRate(String creatureKey, int catchAmount) {
        updateData(creatureKey, catchAmount);
        
        int currentAttempts = totalAttempts.getOrDefault(creatureKey, 0);
        int currentCatches = totalCatches.getOrDefault(creatureKey, 0);

        currentAttempts += catchAmount + 1;
        currentCatches += 1;
        
        totalAttempts.put(creatureKey, currentAttempts);
        totalCatches.put(creatureKey, currentCatches);
        
        double newRate = (double) currentCatches / currentAttempts * 100.0;
        
        catchRates.put(creatureKey, newRate);
        catchCounts.put(creatureKey, currentCatches);
        
        StatConfig.beginBatch();
        StatConfig.setSince(TOTAL_ATTEMPTS + creatureKey, currentAttempts);
        StatConfig.setSince(TOTAL_CATCHES + creatureKey, currentCatches);
        StatConfig.endBatch();
        
        notifyHudDataChanged();
    }

    private void updateData(String creatureKey, int attemptCount) {

        int currentMax = maxAttempts.getOrDefault(creatureKey, 0);
        boolean isNewMax = attemptCount > currentMax;
        
        if (isNewMax) {
            maxAttempts.put(creatureKey, attemptCount);
            
            if (currentMax < 100 && attemptCount >= 100) {
                rebalanceToNewMax(creatureKey, attemptCount);
            } else if (attemptCount >= 100) {
                // Already using brackets, but may need rebalancing for new max
                rebalanceToNewMax(creatureKey, attemptCount);
            } else {
                addIndividualAttempt(creatureKey, attemptCount);
                Map<Integer, Integer> dataToSave = new ConcurrentHashMap<>(catchGraph.get(creatureKey));
                try {
                    saveDataFor(creatureKey, dataToSave);
                } catch (Exception e) {
                    System.err.println(SAVE_ERROR_MSG + creatureKey + ": " + e.getMessage());
                }
            }
        } else {
            // Not a new max, added normally based on current tracking mode
            if (currentMax < 100) {
                addIndividualAttempt(creatureKey, attemptCount);
            } else {
                addBracketedAttempt(creatureKey, attemptCount);
            }

            Map<Integer, Integer> dataToSave = new ConcurrentHashMap<>(catchGraph.get(creatureKey));
            try {
                saveDataFor(creatureKey, dataToSave);
            } catch (Exception e) {
                System.err.println(SAVE_ERROR_MSG + creatureKey + ": " + e.getMessage());
            }
        }
        
        notifyHudDataChanged();
    }
    
    private void addIndividualAttempt(String creatureKey, int attemptCount) {
        catchGraph.computeIfAbsent(creatureKey, k -> new ConcurrentHashMap<>())
                     .merge(attemptCount, 1, Integer::sum);
    }
    
    private void addBracketedAttempt(String creatureKey, int attemptCount) {
        int bracketSize = calculateBracketSize(maxAttempts.get(creatureKey));
        int bracket = getBracketWithSize(attemptCount, bracketSize);
        
        catchGraph.computeIfAbsent(creatureKey, k -> new ConcurrentHashMap<>())
                     .merge(bracket, 1, Integer::sum);
    }
    
    /**
     * Rebalance histogram when max attempts changes significantly
     */
    private void rebalanceToNewMax(String creatureKey, int newMax) {
        List<Integer> allAttempts = reconstructOriginalAttempts(creatureKey);
        
        allAttempts.add(newMax);
        
        int bracketSize = calculateBracketSize(newMax);

        if (bracketSize > 250) {
            FishyNotis.warn("[FA] ScData caught an error while merging data for " + creatureKey + 
                ": Safe limit exceeded.");
            return;
        }

        Map<Integer, Integer> newHistogram = new ConcurrentHashMap<>();
        if (newMax < 100) {
            for (int attempt : allAttempts) {
                newHistogram.merge(attempt, 1, Integer::sum);
            }
        } else {
            for (int attempt : allAttempts) {
                int bracket = getBracketWithSize(attempt, bracketSize);
                newHistogram.merge(bracket, 1, Integer::sum);
            }
        }
        
        try {
            saveDataFor(creatureKey, newHistogram);
            catchGraph.put(creatureKey, newHistogram);
            if (newMax > 200 && newMax > maxAttempts.getOrDefault(creatureKey, 0)) {
                FishyNotis.themed("New worst drystreak recorded for §3" + creatureKey + "§7: §8" + newMax + ". §dGG§7!");
            }
        } catch (Exception e) {
            System.err.println("Failed to save rebalanced data for " + creatureKey + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public int calculateBracketSize(int maxAttempts) {
        if (maxAttempts < 100) {
            return 1;
        }
        
        if (maxAttempts <= 200) return 2;
        if (maxAttempts <= 500) return 5;
        if (maxAttempts <= 1000) return 10;
        if (maxAttempts <= 2500) return 25;
        if (maxAttempts <= 5000) return 50;
        if (maxAttempts <= 10000) return 100;
        if (maxAttempts <= 25000) return 250;
        
        return 500; // Invalid
    }
    
    /**
     * Reconstruct original attempt values from stored config data
     * @param creatureKey The creature identifier
     * @return List of all individual attempt counts
     */
    private List<Integer> reconstructOriginalAttempts(String creatureKey) {
        List<Integer> allAttempts = new ArrayList<>();
        
        Map<String, Integer> allHistogramData = StatConfig.getAllData();
        String prefix = GRAPH_PREFIX + creatureKey + "_";
        
        for (Map.Entry<String, Integer> entry : allHistogramData.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(prefix)) {
                try {
                    String attemptsStr = key.substring(prefix.length());
                    int attempts = Integer.parseInt(attemptsStr);
                    int frequency = entry.getValue();
                    
                    for (int i = 0; i < frequency; i++) {
                        allAttempts.add(attempts);
                    }
                } catch (NumberFormatException e) {
                    // Invalid
                }
            }
        }
        
        return allAttempts;
    }
    
    private void notifyHudDataChanged() {
        try {
            me.valkeea.fishyaddons.hud.elements.custom.ScDisplay hudInstance = 
                me.valkeea.fishyaddons.hud.elements.custom.ScDisplay.getInstance();
            if (hudInstance != null) {
                hudInstance.onDataChanged();
            }
        } catch (Exception e) {
            // Not available
        }
    }


    /**
     * Get bracket using specified size
     * @param attempts Actual attempt count
     * @param bracketSize The size of brackets to use
     * @return The bracket value
     */
    private int getBracketWithSize(int attempts, int bracketSize) {
        if (bracketSize == 1) {
            return attempts;
        }
        return (attempts / bracketSize) * bracketSize; // Bracket start value
    }
    
    /**
     * Determine bracket size for an existing data set
     * @param creatureKey The sc this data belongs to
     * @return The bracket size used for its attempt range
     */
    public int bracketSizeFor(String creatureKey) {
        // Use the bracket size based on max attempts for this creature
        int creatureMaxAttempts = this.maxAttempts.getOrDefault(creatureKey, 0);
        return calculateBracketSize(creatureMaxAttempts);
    }
    
    /**
     * Get data for display graph with merged brackets if needed
     * @param creatureKey Sc identifier
     * @param maxBars Maximum number of bars to display
     * @return Optimized graph data
     */
    public Map<Integer, Integer> getDataFor(String creatureKey, int maxBars) {
        Map<Integer, Integer> originalHistogram = catchGraph.get(creatureKey);
        if (originalHistogram == null || originalHistogram.isEmpty()) {
            return new ConcurrentHashMap<>();
        }
        
        if (originalHistogram.size() <= maxBars) {
            return originalHistogram;
        }
        
        return mergeBrackets(originalHistogram, maxBars);
    }
    
    private Map<Integer, Integer> mergeBrackets(Map<Integer, Integer> originalHistogram, int maxBars) {
        Map<Integer, Integer> mergedHistogram = new ConcurrentHashMap<>();
        
        List<Map.Entry<Integer, Integer>> sortedEntries = originalHistogram.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .toList();
        
        if (sortedEntries.isEmpty()) {
            return mergedHistogram;
        }
        
        int originalBars = sortedEntries.size();
        int mergeRatio = (int) Math.ceil((double) originalBars / maxBars);
        
        for (int i = 0; i < sortedEntries.size(); i += mergeRatio) {
            int mergedBracket = sortedEntries.get(i).getKey();
            int mergedFrequency = 0;
            
            for (int j = i; j < Math.min(i + mergeRatio, sortedEntries.size()); j++) {
                mergedFrequency += sortedEntries.get(j).getValue();
            }
            
            mergedHistogram.put(mergedBracket, mergedFrequency);
        }
        
        return mergedHistogram;
    }
    
    /**
     * Calculate mean attempts for a creature from catch data
     * @param creatureKey Sc identifier
     * @return Mean attempts per catch, using midpoint unless highest attempts < 100
     */
    public double getMeanAttemptsFor(String creatureKey) {
        Map<Integer, Integer> histogram = catchGraph.get(creatureKey);
        if (histogram == null || histogram.isEmpty()) {
            return 0.0;
        }

        if (histogram.size() == 1) {
            Map.Entry<Integer, Integer> entry = histogram.entrySet().iterator().next();
            return entry.getKey();
        }
        
        int histogramCatches = 0;
        double histogramAttempts = 0;
        
        for (Map.Entry<Integer, Integer> entry : histogram.entrySet()) {
            int bracket = entry.getKey();
            int frequency = entry.getValue();

            int bracketSize = bracketSizeFor(creatureKey);
            double bracketMidpoint = bracketSize == 1 ? bracket : bracket + (bracketSize / 2.0);
            
            histogramCatches += frequency;
            histogramAttempts += bracketMidpoint * frequency;
        }

        return histogramCatches > 0 ? histogramAttempts / histogramCatches : 0.0;
    }
    
    public double getCatchChance(String creatureKey) {
        return catchRates.getOrDefault(creatureKey, 0.0);
    }

    private void saveDataFor(String creatureKey, Map<Integer, Integer> histogram) {
        // Save new data first, then clear old data to prevent data loss
        Map<String, Integer> newDataToSave = new LinkedHashMap<>();
        
        for (Map.Entry<Integer, Integer> entry : histogram.entrySet()) {
            int attempts = entry.getKey();
            int frequency = entry.getValue();
            if (frequency > 0) {
                String key = GRAPH_PREFIX + creatureKey + "_" + attempts;
                newDataToSave.put(key, frequency);
            }
        }
        
        if (!newDataToSave.isEmpty()) {
            try {
                StatConfig.beginBatch();
                for (Map.Entry<String, Integer> entry : newDataToSave.entrySet()) {
                    StatConfig.setData(entry.getKey(), entry.getValue());
                }
                
                clearDataFor(creatureKey);
                
                for (Map.Entry<String, Integer> entry : newDataToSave.entrySet()) {
                    StatConfig.setData(entry.getKey(), entry.getValue());
                }
                
                StatConfig.endBatch();
                
            } catch (Exception e) {
                System.err.println(SAVE_ERROR_MSG + creatureKey + ": " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("Warning: No valid frequency data to save for " + creatureKey);
        }
    }

    private void loadDataFor(String creatureKey) {
        
        Map<String, Integer> allHistogramData = StatConfig.getAllData();
        String prefix = GRAPH_PREFIX + creatureKey + "_";
        
        List<Integer> allAttempts = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : allHistogramData.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(prefix)) {
                try {
                    String attemptsStr = key.substring(prefix.length());
                    int attempts = Integer.parseInt(attemptsStr);
                    int frequency = entry.getValue();
                    
                    for (int i = 0; i < frequency; i++) {
                        allAttempts.add(attempts);
                    }
                } catch (NumberFormatException e) {
                    // Skip invalid entries
                }
            }
        }
        
        if (!allAttempts.isEmpty()) {
            buildGraphFor(creatureKey, allAttempts);
        }
    }

    private void buildGraphFor(String creatureKey, List<Integer> allAttempts) {
        Map<Integer, Integer> histogram = new ConcurrentHashMap<>();

        int maxAttemptValue = allAttempts.stream().mapToInt(Integer::intValue).max().orElse(0);
        int bracketSize = calculateBracketSize(maxAttemptValue);        

        maxAttempts.put(creatureKey, maxAttemptValue);
            
        if (maxAttemptValue < 100) {
            for (int attempt : allAttempts) {
                histogram.merge(attempt, 1, Integer::sum);
            }
        } else {
            for (int attempt : allAttempts) {
                int bracket = getBracketWithSize(attempt, bracketSize);
                histogram.merge(bracket, 1, Integer::sum);
            }
        }
            
        catchGraph.put(creatureKey, histogram);
    }

    private void clearDataFor(String creatureKey) {
        Map<String, Integer> allHistogramData = StatConfig.getAllData();
        String prefix = GRAPH_PREFIX + creatureKey + "_";
        
        List<String> keysToRemove = new ArrayList<>();
        for (String key : allHistogramData.keySet()) {
            if (key.startsWith(prefix)) {
                String remainder = key.substring(prefix.length());
                try {
                    Integer.parseInt(remainder);
                    keysToRemove.add(key);
                } catch (NumberFormatException e) {
                    // Skip keys that don't match the exact pattern (area key)
                }
            }
        }

        for (String key : keysToRemove) {
            StatConfig.removeData(key);
        }
    }

    public void loadCatchRates() {
        String[] creatureKeys = Sc.getTrackedCreatures().toArray(new String[0]);
        
        for (String creatureKey : creatureKeys) {
            loadDataFor(creatureKey);
            
            int savedTotalAttempts = StatConfig.getSince(TOTAL_ATTEMPTS + creatureKey);
            int savedTotalCatches = StatConfig.getSince(TOTAL_CATCHES + creatureKey);
            
            if (savedTotalAttempts > 0 && savedTotalCatches > 0) {
                totalAttempts.put(creatureKey, savedTotalAttempts);
                totalCatches.put(creatureKey, savedTotalCatches);
                
                double rate = (double) savedTotalCatches / savedTotalAttempts * 100.0;
                catchRates.put(creatureKey, rate);
                catchCounts.put(creatureKey, savedTotalCatches);
            }
        }
    }

    public void save() {

        boolean allSavedSuccessfully = true;
        
        try {
            StatConfig.beginBatch();
            for (Map.Entry<String, Integer> entry : totalAttempts.entrySet()) {
                StatConfig.setSince(TOTAL_ATTEMPTS + entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, Integer> entry : totalCatches.entrySet()) {
                StatConfig.setSince(TOTAL_CATCHES + entry.getKey(), entry.getValue());
            }
            
            StatConfig.endBatch();
            
        } catch (Exception e) {
            System.err.println("Failed to save total attempts/catches data: " + e.getMessage());
            allSavedSuccessfully = false;
        }
        
        for (Map.Entry<String, Map<Integer, Integer>> entry : catchGraph.entrySet()) {
            try {
                saveDataFor(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                System.err.println(SAVE_ERROR_MSG + entry.getKey() + " during bulk save: " + e.getMessage());
                allSavedSuccessfully = false;
            }
        }
        
        if (allSavedSuccessfully) {
            catchRates.clear();
            catchCounts.clear();
            totalAttempts.clear();
            totalCatches.clear();
            catchGraph.clear();
        } else {
            System.err.println("ScData: Some saves failed, keeping data in memory");
        }
    }

    /**
     * Records a catch event for data tracking
     * @param creature The creature caught (raw name)
     * @param count The number of attempts it took to catch
     */
    public static void onCatch(String creature, int count) {
        getInstance().updateCatchRate(creature, count);
    }
    
    /**
     * Validate that frequency data exists for a creature in config
     * @param creatureKey The creature to validate
     * @return true if data exists, false otherwise
     */
    public boolean validateFrequencyDataExists(String creatureKey) {
        Map<String, Integer> allHistogramData = StatConfig.getAllData();
        String prefix = GRAPH_PREFIX + creatureKey + "_";
        
        boolean hasData = allHistogramData.entrySet().stream()
            .anyMatch(entry -> {
                String key = entry.getKey();
                if (key.startsWith(prefix) && entry.getValue() > 0) {
                    // Ensure exact match by checking the remainder is a number
                    String remainder = key.substring(prefix.length());
                    try {
                        Integer.parseInt(remainder);
                        return true;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }
                return false;
            });
            
        if (!hasData && catchGraph.containsKey(creatureKey) && !catchGraph.get(creatureKey).isEmpty()) {
            try {
                saveDataFor(creatureKey, catchGraph.get(creatureKey));
                return true;
            } catch (Exception e) {
                System.err.println("Failed to recover data for " + creatureKey + ": " + e.getMessage());
                return false;
            }
        }
        
        return hasData;
    } 

    public void sendCatchRates() {

        if (catchRates.isEmpty()) {
            FishyNotis.send(Text.literal("§3No catch % data available yet."));
            return;
        }

        FishyNotis.themed("Catch Rates:");
        Map<String, List<Map.Entry<String, Double>>> groupedRates = new LinkedHashMap<>();
        
        for (Map.Entry<String, Double> entry : catchRates.entrySet()) {
            String creatureKey = entry.getKey();
            String baseCreature = getBaseCreatureType(creatureKey);
            
            groupedRates.computeIfAbsent(baseCreature, k -> new ArrayList<>()).add(entry);
        }
        
        for (Map.Entry<String, List<Map.Entry<String, Double>>> group : groupedRates.entrySet()) {
            String baseCreature = group.getKey();
            List<Map.Entry<String, Double>> variants = group.getValue();
            
            if (variants.size() == 1) {
                Map.Entry<String, Double> entry = variants.get(0);
                String creatureKey = entry.getKey();
                double rate = entry.getValue();
                int count = catchCounts.getOrDefault(creatureKey, 0);

                String displayName = Sc.displayName(creatureKey);
                String message = String.format("§7%s: §b%.2f%% §7(§b%d catches§7)",
                    displayName, rate, count);
                FishyNotis.alert(Text.literal(message));
            } else {
                FishyNotis.alert(Text.literal("§b" + baseCreature + ":"));
                
                variants.stream()
                    .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                    .forEach(entry -> {
                        String creatureKey = entry.getKey();
                        double rate = entry.getValue();
                        int count = catchCounts.getOrDefault(creatureKey, 0);

                        String displayName = Sc.displayName(creatureKey);
                        String message = String.format("§7  %s: §b%.2f%% §7(§b%d catches§7)", 
                            displayName, rate, count);
                        FishyNotis.alert(Text.literal(message));
                    });
            }
        }
    }
    
    private String getBaseCreatureType(String creatureKey) {
        if (creatureKey.startsWith(Sc.THUNDER)) return "Thunder";
        if (creatureKey.startsWith(Sc.JAWBUS)) return "Lord Jawbus";
        return creatureKey.replace("_", " ");
    }

    public void sendHistogramSummary(String creatureKey) {

        Map<Integer, Integer> histogram = catchGraph.get(creatureKey);
        var dpName = Sc.displayName(creatureKey);

        if (histogram == null || histogram.isEmpty()) {

            FishyNotis.send(Text.literal("§3No catch data for " + dpName));

                if (histogram == null) {
                FishyNotis.themed("Did you mean;");

                List<String> similar = new ArrayList<>();
                Sc.getTrackedCreatures().stream()
                .filter(key -> StringUtils.closeMatch(creatureKey, key))
                .forEach(similar::add);
                
                if (!similar.isEmpty()) {
                    FishyNotis.themed("Did you mean;");
                    similar
                    .forEach(key -> FishyNotis.alert(Text.literal("§7- §8" + key + " §7(§8" + Sc.displayName(key) + "§7)")
                    .styled(style -> style.withClickEvent(new net.minecraft.text.ClickEvent.RunCommand("fa sc " + key)))));
                }
            }
            return;
        }

        int histogramCatches = histogram.values().stream().mapToInt(Integer::intValue).sum();
        double mean = getMeanAttemptsFor(creatureKey);

        FishyNotis.alert(Text.literal(Sc.displayName(creatureKey) + " §bCatch Summary:"));
        FishyNotis.alert(Text.literal(String.format("§7Caught: §b%d §7times §8| §7Mean: §b%.1f §7attempts", 
            histogramCatches, mean)));
        FishyNotis.alert(Text.literal(String.format("§7Chance: §b%.2f%% §7", 
            getCatchChance(creatureKey))));
        
        FishyNotis.alert(Text.literal("§3Top frequencies:"));

        histogram.entrySet().stream()
        .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue())).limit(5)
        .forEach(entry -> {
            int attempts = entry.getKey();
            int frequency = entry.getValue();
            double rate = (double) frequency / histogramCatches * 100;
            FishyNotis.alert(Text.literal(String.format("§7  §b%d §7attempts: §b%d §7times §8(§b%.1f%%§7§8)", 
                attempts, frequency, rate)));
        });
            
        FishyNotis.alert(Text.literal(String.format("§dYou have caught: §b%d §7Sc without ", 
        StatConfig.getSince("since_" + creatureKey)) + dpName));

        int worstBracket = histogram.entrySet().stream()
        .max((a, b) -> Integer.compare(a.getKey(), b.getKey()))
        .map(Map.Entry::getKey)
        .orElse(0);

        FishyNotis.alert(Text.literal(String.format("§dWorst drystreak: §b%d §7Sc without ", worstBracket) + dpName));   
    }
}
