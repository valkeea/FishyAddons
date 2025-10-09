package me.valkeea.fishyaddons.hud;

import java.awt.Rectangle;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.tracker.ActivityMonitor;
import me.valkeea.fishyaddons.tracker.fishing.Sc;
import me.valkeea.fishyaddons.tracker.fishing.ScData;
import me.valkeea.fishyaddons.tracker.fishing.ScRegistry;
import me.valkeea.fishyaddons.tracker.fishing.ScStats;
import me.valkeea.fishyaddons.util.text.Color;
import me.valkeea.fishyaddons.util.text.TextUtils;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ScDisplay implements HudElement {
    private static final String HUD_KEY = Key.HUD_CATCH_GRAPH_ENABLED;
    private static ScDisplay instance = null;

    private HudElementState cachedState = null;
    private boolean editingMode = false;

    // Visual constants
    private static final int CHART_WIDTH = 160;
    private static final int CHART_HEIGHT = 85;
    private static final int CHART_PADDING = 12;
    private static final int MAX_BARS_PER_CHART = 100;
    private static final int CHART_PADDING_X2 = CHART_PADDING * 2;
    private static final int CHART_HEIGHT_MINUS_35 = CHART_HEIGHT - 35;
    private static final double CHART_HEIGHT_SCALE = 0.9;

    private static final String CI_HOTSPOT = "crimson_hotspot";
    private static final String CI_POOL = "crimson_plhleg";
    private static final String GALATEA = "galatea";
    
    private static final float AXIS_FONT_SCALE = 0.75f;
    
    // Reusable Text objects and operations
    private static final Text EMPTY_MEAN_TEXT = Text.literal("§7Mean: §f--");
    private static final Text EMPTY_PERCENT_TEXT = Text.literal("§7Rate: §f--");
    
    private final Map<String, String> displayNameCache = new ConcurrentHashMap<>();
    private final Map<String, String> meanTextCache = new ConcurrentHashMap<>();
    private final Map<String, String> percentTextCache = new ConcurrentHashMap<>();
    private long lastDataUpdate = 0;
    
    // Cache for calculations
    private final Map<String, HistogramCache> histogramCache = new ConcurrentHashMap<>();
    
    private static class HistogramCache {
        final List<Map.Entry<Integer, Integer>> sortedEntries;
        final int maxFrequency;
        final int minBracket;
        final int maxBracket;
        final double minAttempts;
        final double maxAttempts;
        final double attemptRange;
        final int bracketSize;
        
        HistogramCache(Map<Integer, Integer> histogram, String creatureKey) {
            this.sortedEntries = histogram.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .toList();
            this.maxFrequency = histogram.values().stream().mapToInt(Integer::intValue).max().orElse(1);
            this.minBracket = sortedEntries.get(0).getKey();
            this.maxBracket = sortedEntries.get(sortedEntries.size() - 1).getKey();
            this.bracketSize = ScData.getInstance().bracketSizeFor(creatureKey);
            this.minAttempts = minBracket + (bracketSize / 2.0);
            this.maxAttempts = maxBracket + (bracketSize / 2.0);
            double range = maxAttempts - minAttempts;
            this.attemptRange = range == 0 ? ScData.getInstance().calculateBracketSize((int)minAttempts) : range;
        }
    }
    
    private String lastKnownArea = "";
    private long lastAreaChangeTime = 0;
    private static final long AREA_CHANGE_DEBOUNCE_MS = 2000;

    private ScDisplay() {}

    public static ScDisplay getInstance() {
        if (instance == null) {
            instance = new ScDisplay();
        }
        return instance;
    }

    public void register() {
        HudLayerRegistrationCallback.EVENT.register(layeredDrawer ->
            layeredDrawer.attachLayerAfter(
                IdentifiedLayer.MISC_OVERLAYS,
                Identifier.of("fishyaddons", "catch_graph_hud"),
                (context, tickCounter) -> render(context, 0, 0)
            )
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY) {
        if (!isEnabled() && !editingMode) {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return;
        }

        String currentArea = ScStats.getInstance().getCurrentAreaKey();
        checkAreaChange(currentArea);
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastDataUpdate > 5000) {
            clearAllCaches();
            lastDataUpdate = currentTime;
        }

        int x = getHudX();
        int y = getHudY();
        int size = getHudSize();
        float scale = size / 100.0f;

        context.getMatrices().push();
        context.getMatrices().scale(scale, scale, 1.0f);

        int scaledX = (int) (x / scale);
        int scaledY = (int) (y / scale);

        if (editingMode) {
            renderEditMode(context, scaledX, scaledY);
        } else {
            renderDisplays(context, scaledX, scaledY);
        }

        context.getMatrices().pop();
    }
    
    private void checkAreaChange(String currentArea) {
        if (!currentArea.equals(lastKnownArea)) {
            long currentTime = System.currentTimeMillis();
            
            if (lastAreaChangeTime == 0) {
                lastAreaChangeTime = currentTime;
            }
            
            if (currentTime - lastAreaChangeTime >= AREA_CHANGE_DEBOUNCE_MS) {
                invalidateCache();
                lastKnownArea = currentArea;
                lastAreaChangeTime = 0;
            }
        } else {
            lastAreaChangeTime = 0;
        }
    }

    private void renderEditMode(DrawContext context, int x, int y) {
        if (getHudBg()) {
            context.fill(x - 2, y, x + CHART_WIDTH, y + CHART_HEIGHT + 10, 0x80000000);
        }
        context.drawText(MinecraftClient.getInstance().textRenderer,
            Text.literal("Catch Graph"), x + 5, y + 5, getHudColor(), false);
        context.drawText(MinecraftClient.getInstance().textRenderer,
            Text.literal("§8[Edit Mode]"), x + 5, y + 20, getHudColor(), false);
    }

    private void renderDisplays(DrawContext context, int x, int y) {
        if (!ScStats.isEnabled()) {
            return;
        }

        String currentArea = ScStats.getInstance().getCurrentAreaKey();
        List<String> creatures = getCreaturesForArea(currentArea);
        
        if (creatures.isEmpty()) {
            return;
        }

        int currentX = x;
        boolean hasAnyData = false;

        for (String creatureKey : creatures) {
            Map<Integer, Integer> histogram = ScData.getInstance().getDataFor(creatureKey, MAX_BARS_PER_CHART);
            if (histogram != null && !histogram.isEmpty()) {
                renderBase(context, currentX, y, creatureKey, histogram);
                currentX += CHART_WIDTH + 10;
                hasAnyData = true;
            }
        }
        
        if (!hasAnyData && ActivityMonitor.getInstance().getPrimaryActivity() == ActivityMonitor.Currently.FISHING) {
            renderWaitingForData(context, x, y, currentArea, creatures);
        }
    }

    private void renderWaitingForData(DrawContext context, int x, int y, String area, List<String> creatures) {
        if (getHudBg()) {
            context.fill(x - 2, y, x + CHART_WIDTH, y + 80, 0x80000000);
        }

        context.drawText(MinecraftClient.getInstance().textRenderer,
            Text.literal("RNG Data"), x + 5, y + 5, getHudColor(), false);
        context.drawText(MinecraftClient.getInstance().textRenderer,
            Text.literal("§7Area: " + formatAreaName(area)), x + 5, y + 14, 0xFFFFFF, false);
        context.drawText(MinecraftClient.getInstance().textRenderer, 
            Text.literal("§7Catch rare scs!"), x + 5, y + 23, 0xFFFFFF, false);
        
        StringBuilder creatureList = new StringBuilder("§8");
        for (int i = 0; i < Math.min(2, creatures.size()); i++) {
            if (i > 0) creatureList.append(", ");
            creatureList.append(creatures.get(i).replace("_", " "));
        }
        if (creatures.size() > 2) {
            creatureList.append(", +").append(creatures.size() - 2);
        }
        context.drawText(MinecraftClient.getInstance().textRenderer, 
            Text.literal(creatureList.toString()), x + 5, y + 32, getHudColor(), false);
    }

    private void renderBase(DrawContext context, int x, int y, String creatureKey, Map<Integer, Integer> histogram) {
        String displayName = displayNameCache.computeIfAbsent(creatureKey, 
            key -> TextUtils.stripColor(Sc.displayName(key)));
        
        if (getHudBg()) {
            context.fill(x - 2, y, x + CHART_WIDTH, y + CHART_HEIGHT + 10, 0x80000000);
        }

        if (getHudOutline()) {
            TextUtils.drawOutlinedText(
                context,
                MinecraftClient.getInstance().textRenderer, 
                Text.literal(displayName),
                (float)x + 5, (float)y + 5, getHudColor(),
                0xFF000000               
            );

        } else {
            context.drawText(MinecraftClient.getInstance().textRenderer, 
                Text.literal(displayName), x + 5, y + 5, getHudColor(), false);
        }

        String meanText = getCachedMeanText(creatureKey);
        String percentText = getCachedPercentText(creatureKey);
        
        context.drawText(MinecraftClient.getInstance().textRenderer, 
            Text.literal(meanText), x + 5, y + 17, 0xFFFFFF, false);
        context.drawText(MinecraftClient.getInstance().textRenderer, 
            Text.literal(percentText), x + 80, y + 17, 0xFFFFFF, false);

        drawHistogram(context, x + CHART_PADDING, y + 35, histogram, creatureKey);
    }
    
    private void clearAllCaches() {
        displayNameCache.clear();
        meanTextCache.clear();
        percentTextCache.clear();
        histogramCache.clear();
    }
    
    private String getCachedMeanText(String creatureKey) {
        return meanTextCache.computeIfAbsent(creatureKey, key -> {
            double meanAttempts = ScData.getInstance().getMeanAttemptsFor(key);
            return meanAttempts > 0 ? String.format("§7Mean: §f%.1f", meanAttempts) : EMPTY_MEAN_TEXT.getString();
        });
    }
    
    private String getCachedPercentText(String creatureKey) {
        return percentTextCache.computeIfAbsent(creatureKey, key -> {
            double catchPercentage = ScData.getInstance().getCatchChance(key);
            return catchPercentage > 0 ? String.format("§7Rate: §f%.1f%%", catchPercentage) : EMPTY_PERCENT_TEXT.getString();
        });
    }

    private void drawHistogram(DrawContext context, int chartX, int chartY, Map<Integer, Integer> histogram, String creatureKey) {
        if (histogram.isEmpty()) return;

        HistogramCache cache = histogramCache.computeIfAbsent(creatureKey, 
            key -> new HistogramCache(histogram, key));
        
        if (cache.sortedEntries.size() != histogram.size()) {
            cache = new HistogramCache(histogram, creatureKey);
            histogramCache.put(creatureKey, cache);
        }

        int availableWidth = CHART_WIDTH - CHART_PADDING_X2;
        int availableHeight = CHART_HEIGHT_MINUS_35;

        int barWidth = availableWidth / MAX_BARS_PER_CHART;

        if (cache.sortedEntries.size() <= MAX_BARS_PER_CHART / 3) {
            barWidth *= 2.5;
        } else if (cache.sortedEntries.size() <= MAX_BARS_PER_CHART / 2) {
            barWidth *= 2;
        }

        int axisColor = 0xFF666666;
        context.fill(chartX, chartY, chartX + 1, chartY + availableHeight, axisColor);
        context.fill(chartX, chartY + availableHeight, chartX + availableWidth, chartY + availableHeight + 1, axisColor);
        drawAxisLabels(context, chartX, chartY, cache.sortedEntries, cache.maxFrequency, cache.minAttempts, cache.maxAttempts);

        for (Map.Entry<Integer, Integer> entry : cache.sortedEntries) {
            int bracket = entry.getKey();
            int frequency = entry.getValue();
            
            double bracketMidpoint = bracket + (cache.bracketSize / 2.0);
            double positionRatio = (bracketMidpoint - cache.minAttempts) / cache.attemptRange;

            int barX = chartX + (int) (positionRatio * (availableWidth - barWidth));
            barX = Math.clamp(barX, chartX, chartX + availableWidth - barWidth);
            int barHeight = Math.max(((int) ((double) frequency / cache.maxFrequency * availableHeight * CHART_HEIGHT_SCALE)), 1);
            int barY = chartY + availableHeight - barHeight;
            
            int barColor = getBarColor(frequency, cache.maxFrequency);
            context.fill(barX, barY, barX + barWidth, chartY + availableHeight, barColor);
        }
    }
    
    private void drawAxisLabels(DrawContext context, int chartX, int chartY, List<Map.Entry<Integer,
                                Integer>> sortedEntries, int maxFrequency, double minAttempts, double maxAttempts) {
        if (sortedEntries.isEmpty()) return;

        int availableWidth = CHART_WIDTH - CHART_PADDING_X2;
        int availableHeight = CHART_HEIGHT_MINUS_35;    
        
        context.getMatrices().push();
        context.getMatrices().scale(AXIS_FONT_SCALE, AXIS_FONT_SCALE, 1.0f);
        
        int scaledChartX = (int) (chartX / AXIS_FONT_SCALE);
        int scaledChartY = (int) (chartY / AXIS_FONT_SCALE);
        int scaledAvailableHeight = (int) (availableHeight / AXIS_FONT_SCALE);
        int scaledAvailableWidth = (int) (availableWidth / AXIS_FONT_SCALE);
        
        context.drawText(MinecraftClient.getInstance().textRenderer, 
            Text.literal("0"), scaledChartX - 15, scaledChartY + scaledAvailableHeight - 5, 0xFFAAAAAA, false);
        
        if (maxFrequency > 2) {
            int midFreq = maxFrequency / 2;
            int midY = scaledChartY + scaledAvailableHeight / 2;
            context.drawText(MinecraftClient.getInstance().textRenderer, 
                Text.literal(String.valueOf(midFreq)), scaledChartX - 15, midY, 0xFFAAAAAA, false);
        }
        
        context.drawText(MinecraftClient.getInstance().textRenderer, 
            Text.literal(String.valueOf(maxFrequency)), scaledChartX - 15, scaledChartY, 0xFFAAAAAA, false);
        
        double attemptRange = Math.max(1, maxAttempts - minAttempts);
        
        int minBracket = sortedEntries.get(0).getKey();
        int maxBracket = sortedEntries.get(sortedEntries.size() - 1).getKey();
        
        context.drawText(MinecraftClient.getInstance().textRenderer, 
            Text.literal(formatBracketLabel(minBracket)), scaledChartX, scaledChartY + scaledAvailableHeight + 5, 0xFFAAAAAA, false);
        
        String highestLabel = formatBracketLabel(maxBracket);
        int highestLabelWidth = MinecraftClient.getInstance().textRenderer.getWidth(highestLabel);
        context.drawText(MinecraftClient.getInstance().textRenderer, 
            Text.literal(highestLabel), scaledChartX + scaledAvailableWidth - (int)(highestLabelWidth * AXIS_FONT_SCALE), 
            scaledChartY + scaledAvailableHeight + 5, 0xFFAAAAAA, false);
        
        if (attemptRange > 1 && scaledAvailableWidth > 80 && sortedEntries.size() > 2) {

            int midBracket = (minBracket + maxBracket) / 2;
            String midLabel = formatBracketLabel(midBracket);
            int midLabelWidth = MinecraftClient.getInstance().textRenderer.getWidth(midLabel);
            context.drawText(MinecraftClient.getInstance().textRenderer, 
                Text.literal(midLabel), scaledChartX + (scaledAvailableWidth - (int)(midLabelWidth * AXIS_FONT_SCALE)) / 2, 
                scaledChartY + scaledAvailableHeight + 5, 0xFFAAAAAA, false);
        }
        
        context.getMatrices().pop();
    }

    private String formatBracketLabel(int bracket) {
        int bracketSize = ScData.getInstance().calculateBracketSize(bracket);
        if (bracket == 0) {
                return "0";
        } else if (bracketSize >= 1000) {
            return String.format("%.1fk", bracket / 1000.0);
        } else {
            return String.valueOf(bracket);
        }
    }

    private int getBarColor(int frequency, int maxFrequency) {
        float intensity = (float) frequency / maxFrequency;
        int baseColor = getHudColor();
        int gradientColor = Color.createGradient(baseColor, intensity);
        
        return Color.ensureOpaque(gradientColor);
    }

    private List<String> getCreaturesForArea(String areaKey) {
        return ScRegistry.getInstance().getCreaturesForArea(areaKey);
    }

    @Override
    public Rectangle getBounds(MinecraftClient mc) {
        String currentArea = ScStats.getInstance().getCurrentAreaKey();
        List<String> creatures = getCreaturesForArea(currentArea);
        int totalWidth = Math.max(200, creatures.size() * (CHART_WIDTH + 10) - 10);
        int height = (int) ((CHART_HEIGHT + 50) * (getHudSize() / 100.0f));
        int width = (int) (totalWidth * (getHudSize() / 100.0f));
        return new Rectangle(getHudX(), getHudY(), width, height);
    }

    @Override
    public HudElementState getCachedState() {
        if (cachedState == null) {
            cachedState = new HudElementState(getHudX(), getHudY(), getHudSize(), 
                getHudColor(), getHudOutline(), getHudBg());
        }
        return cachedState;
    }

    /**
     * Called when ScStats data changes to trigger HUD refresh
     */
    public void onDataChanged() {
        invalidateCache();
        displayNameCache.clear();
        meanTextCache.clear();
        percentTextCache.clear();
        histogramCache.clear();
        lastDataUpdate = System.currentTimeMillis();
    }
    
    private boolean isEnabled() {
        return ScData.isEnabled();
    }

    @Override
    public void setHudPosition(int x, int y) {
        FishyConfig.setHudX(HUD_KEY, x);
        FishyConfig.setHudY(HUD_KEY, y);
        invalidateCache();
    }

    @Override
    public void setHudSize(int size) {
        FishyConfig.setHudSize(HUD_KEY, size);
        invalidateCache();
    }

    @Override
    public void setHudColor(int color) {
        FishyConfig.setHudColor(HUD_KEY, color);
        invalidateCache();
    }

    @Override
    public void setHudOutline(boolean outline) {
        FishyConfig.setHudOutline(HUD_KEY, outline);
        invalidateCache();
    }

    @Override
    public void setHudBg(boolean bg) {
        FishyConfig.setHudBg(HUD_KEY, bg);
        invalidateCache();
    }
    
    private String formatAreaName(String areaKey) {
        switch (areaKey) {
            case "crimson_isles": return "Crimson Isles";
            case CI_POOL: return "Crimson Isles (Pool)";
            case CI_HOTSPOT: return "Crimson Isles (Hotspot)";
            case "jerry": return "Jerry Island";
            case GALATEA: return "Galatea";
            case "bayou": return "Bayou";
            case "hotspot": return "Hotspot";
            case "crystal_hollows": return "Crystal Hollows";
            case "invalid": return "Unknown Area";
            default: return areaKey.replace("_", " ");
        }
    }    

    @Override public void invalidateCache() { cachedState = null; }    
    @Override public int getHudX() { return FishyConfig.getHudX(HUD_KEY, 570); }
    @Override public int getHudY() { return FishyConfig.getHudY(HUD_KEY, 25); }
    @Override public int getHudSize() { return FishyConfig.getHudSize(HUD_KEY, 80); }
    @Override public int getHudColor() { return FishyConfig.getHudColor(HUD_KEY, 10155196); }
    @Override public boolean getHudOutline() { return FishyConfig.getHudOutline(HUD_KEY, true); }
    @Override public boolean getHudBg() { return FishyConfig.getHudBg(HUD_KEY, true); }
    @Override public void setEditingMode(boolean editing) { this.editingMode = editing; }
    @Override public String getDisplayName() { return "Catch Graphs"; }    
}