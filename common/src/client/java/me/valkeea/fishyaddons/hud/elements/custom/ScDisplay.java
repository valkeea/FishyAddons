package me.valkeea.fishyaddons.hud.elements.custom;

import java.awt.Rectangle;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas.Island;
import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.hud.core.HudElement;
import me.valkeea.fishyaddons.hud.core.HudElementState;
import me.valkeea.fishyaddons.render.OutlinedText;
import me.valkeea.fishyaddons.tracker.fishing.Sc;
import me.valkeea.fishyaddons.tracker.fishing.ScData;
import me.valkeea.fishyaddons.tracker.fishing.ScRegistry;
import me.valkeea.fishyaddons.tracker.fishing.ScStats;
import me.valkeea.fishyaddons.tracker.monitoring.ActivityMonitor;
import me.valkeea.fishyaddons.tracker.monitoring.Currently;
import me.valkeea.fishyaddons.util.text.Color;
import me.valkeea.fishyaddons.util.text.TextUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class ScDisplay implements HudElement {
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
    
    private static final float AXIS_FONT_SCALE = 0.75f;

    private static final String HUD_KEY = Key.HUD_CATCH_GRAPH_ENABLED;    
    
    // Reusable Text objects and operations
    private static final Text EMPTY_MEAN_TEXT = Text.literal("§7Mean: §f--");
    private static final Text EMPTY_RATE_TEXT = Text.literal("§7Rate: §f--");
    
    private final Map<String, String> displayNameCache = new ConcurrentHashMap<>();
    private final Map<String, Text> meanTextCache = new ConcurrentHashMap<>();
    private final Map<String, Text> rateTextCache = new ConcurrentHashMap<>();
    private long lastDataVersion = -1;
    
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

    private Island lastKnownArea = Island.NA;
    private long lastAreaChangeTime = 0;
    private static final long AREA_CHANGE_DEBOUNCE_MS = 2000;

    private ScDisplay() {}

    public static ScDisplay getInstance() {
        if (instance == null) {
            instance = new ScDisplay();
        }
        return instance;
    }

    @Override
    public void render(DrawContext context, MinecraftClient mc, int mouseX, int mouseY) {
        if ((!isEnabled() || !ActivityMonitor.getInstance().isActive(Currently.FISHING)) &&
            !editingMode) {
            return;
        }

        if (mc.player == null) return;

        Island currentArea = ScStats.getInstance().getCurrentAreaKey();
        checkAreaChange(currentArea);
        
        long currentDataVersion = ScData.getInstance().getDataVersion();
        if (currentDataVersion != lastDataVersion) {
            clearAllCaches();
            lastDataVersion = currentDataVersion;
        }

        int x = getHudX();
        int y = getHudY();
        int size = getHudSize();
        float scale = size / 100.0f;

        context.getMatrices().pushMatrix();
        context.getMatrices().scale(scale, scale);

        int scaledX = (int) (x / scale);
        int scaledY = (int) (y / scale);

        if (editingMode) {
            renderEditMode(context, mc, scaledX, scaledY);
        } else {
            renderDisplays(context, mc, scaledX, scaledY);
        }

        context.getMatrices().popMatrix();
    }

    private void checkAreaChange(Island currentArea) {
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

    private void renderDisplays(DrawContext context, MinecraftClient mc, int x, int y) {
        if (!ScStats.isEnabled()) return;

        Island currentArea = ScStats.getInstance().getCurrentAreaKey();
        List<String> creatures = getCreaturesForArea(currentArea);
        if (creatures.isEmpty()) return;

        int screenWidth = mc.getWindow().getScaledWidth();
        int leftThreshold = screenWidth / 3;
        int rightThreshold = (2 * screenWidth) / 3;

        boolean anchorOnLeft = x < leftThreshold;
        boolean anchorOnRight = x >= rightThreshold;
        
        boolean hasAnyData = drawGraphs(context, mc, x, y, creatures, anchorOnLeft, anchorOnRight);
        
        if (!hasAnyData && ActivityMonitor.getInstance().getPrimaryActivity() == Currently.FISHING) {
            renderWaitingForData(context, mc, x, y, currentArea, creatures);
        }
    }
    
    private boolean drawGraphs(DrawContext context, MinecraftClient mc, int x, int y, List<String> creatures, 
                                         boolean anchorOnLeft, boolean anchorOnRight) {
        boolean hasAnyData = false;
        int graphIdx = 0;
        
        for (String creatureKey : creatures) {
            Map<Integer, Integer> data = ScData.getInstance().getDataFor(creatureKey, MAX_BARS_PER_CHART);
            if (data != null && !data.isEmpty()) {
                int currentX = calcGraphX(x, graphIdx, anchorOnLeft, anchorOnRight);
                renderBase(context, mc, currentX, y, creatureKey, data);
                graphIdx++;
                hasAnyData = true;
            }
        }
        
        return hasAnyData;
    }
    
    private int calcGraphX(int baseX, int graphIdx, boolean anchorOnLeft, boolean anchorOnRight) {
        if (anchorOnLeft) {
            return baseX + (graphIdx * (CHART_WIDTH + 10));
        } else if (anchorOnRight) {
            return baseX - (graphIdx * (CHART_WIDTH + 10));
        } else {
            return calcMidAnchorX(baseX, graphIdx);
        }
    }
    
    private int calcMidAnchorX(int baseX, int graphIdx) {
        if (graphIdx == 0) {
            return baseX;
        } else if (graphIdx % 2 == 1) {
            int expansionLvl = (graphIdx + 1) / 2;
            return baseX + (expansionLvl * (CHART_WIDTH + 10));
        } else {
            int expansionLvl = (graphIdx + 1) / 2;
            return baseX - (expansionLvl * (CHART_WIDTH + 10));
        }
    }

    private void renderBase(DrawContext context, MinecraftClient mc, int x, int y, String creatureKey, Map<Integer, Integer> data) {
        String displayName = displayNameCache.computeIfAbsent(creatureKey, 
            key -> TextUtils.stripColor(Sc.displayName(key)));
        
        var textRenderer = mc.textRenderer;
        
        if (getHudBg()) {
            context.fill(x - 2, y, x + CHART_WIDTH, y + CHART_HEIGHT + 10, 0x80000000);
        }

        if (getHudOutline()) {
            OutlinedText.withColor(
                context,
                textRenderer, 
                Text.literal(displayName),
                x + 5, y + 5, getHudColor()              
            );

        } else {
            context.drawText(textRenderer, 
                Text.literal(displayName), x + 5, y + 5, getHudColor(), false);
        }

        Text meanText = getCachedMeanText(creatureKey);
        Text rateText = getCachedRateText(creatureKey);
        
        context.drawText(textRenderer, meanText, x + 5, y + 17, 0xFFFFFFFF, false);
        context.drawText(textRenderer, rateText, x + 80, y + 17, 0xFFFFFFFF, false);

        drawHistogram(context, mc, x + CHART_PADDING, y + 35, data, creatureKey);
    }

    private void renderWaitingForData(DrawContext context, MinecraftClient mc, int x, int y, Island area, List<String> creatures) {
        if (getHudBg()) {
            context.fill(x - 2, y, x + CHART_WIDTH, y + 80, 0x80000000);
        }

        var textRenderer = mc.textRenderer;
        context.drawText(textRenderer,
            Text.literal("RNG Data"), x + 5, y + 5, getHudColor(), false);
        context.drawText(textRenderer,
            Text.literal("§7Area: " + formatAreaName(area)), x + 5, y + 14, 0xFFFFFFFF, false);
        context.drawText(textRenderer, 
            Text.literal("§7Catch rare scs!"), x + 5, y + 23, 0xFFFFFFFF, false);

        var creatureList = new StringBuilder("§8");
        for (int i = 0; i < Math.min(2, creatures.size()); i++) {
            if (i > 0) creatureList.append(", ");
            creatureList.append(creatures.get(i).replace("_", " "));
        }
        if (creatures.size() > 2) {
            creatureList.append(", +").append(creatures.size() - 2);
        }
        context.drawText(textRenderer, 
            Text.literal(creatureList.toString()), x + 5, y + 32, getHudColor(), false);
    }

    private void renderEditMode(DrawContext context, MinecraftClient mc, int x, int y) {
        if (getHudBg()) {
            context.fill(x - 2, y, x + CHART_WIDTH, y + CHART_HEIGHT + 10, 0x80000000);
        }
        var textRenderer = mc.textRenderer;
        context.drawText(textRenderer,
            Text.literal("Catch Graph"), x + 5, y + 5, getHudColor(), false);
        context.drawText(textRenderer,
            Text.literal("§8[Edit Mode]"), x + 5, y + 20, getHudColor(), false);
        context.drawText(textRenderer,
            Text.literal("§bThis element auto-adjusts!"), x + 5, y + 35, getHudColor(), false);            
    }

    private void drawHistogram(DrawContext context, MinecraftClient mc, int chartX, int chartY, Map<Integer, Integer> data, String creatureKey) {
        if (data.isEmpty()) return;

        HistogramCache cache = histogramCache.computeIfAbsent(creatureKey, 
            key -> new HistogramCache(data, key));
        
        if (cache.sortedEntries.size() != data.size()) {
            cache = new HistogramCache(data, creatureKey);
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
        drawAxisLabels(context, mc, chartX, chartY, cache.sortedEntries, cache.maxFrequency, cache.minAttempts, cache.maxAttempts);

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
    
    @SuppressWarnings("squid:S107")
    private void drawAxisLabels(DrawContext context, MinecraftClient mc, int chartX, int chartY, List<Map.Entry<Integer,
        Integer>> sortedEntries, int maxFrequency, double minAttempts, double maxAttempts
    ) {
        if (sortedEntries.isEmpty()) return;

        int availableWidth = CHART_WIDTH - CHART_PADDING_X2;
        int availableHeight = CHART_HEIGHT_MINUS_35;    

        context.getMatrices().pushMatrix();
        context.getMatrices().scale(AXIS_FONT_SCALE, AXIS_FONT_SCALE);

        var textRenderer = mc.textRenderer;
        int scaledChartX = (int) (chartX / AXIS_FONT_SCALE);
        int scaledChartY = (int) (chartY / AXIS_FONT_SCALE);
        int scaledAvailableHeight = (int) (availableHeight / AXIS_FONT_SCALE);
        int scaledAvailableWidth = (int) (availableWidth / AXIS_FONT_SCALE);
        
        context.drawText(textRenderer, 
            Text.literal("0"), scaledChartX - 15, scaledChartY + scaledAvailableHeight - 5, 0xFFAAAAAA, false);
        
        if (maxFrequency > 2) {
            int midFreq = maxFrequency / 2;
            int midY = scaledChartY + scaledAvailableHeight / 2;
            context.drawText(textRenderer, 
                Text.literal(String.valueOf(midFreq)), scaledChartX - 15, midY, 0xFFAAAAAA, false);
        }
        
        context.drawText(textRenderer, 
            Text.literal(String.valueOf(maxFrequency)), scaledChartX - 15, scaledChartY, 0xFFAAAAAA, false);
        
        double attemptRange = Math.max(1, maxAttempts - minAttempts);
        
        int minBracket = sortedEntries.get(0).getKey();
        int maxBracket = sortedEntries.get(sortedEntries.size() - 1).getKey();
        
        context.drawText(textRenderer, 
            Text.literal(formatBracketLabel(minBracket)), scaledChartX, scaledChartY + scaledAvailableHeight + 5, 0xFFAAAAAA, false);
        
        String highestLabel = formatBracketLabel(maxBracket);
        int highestLabelWidth = textRenderer.getWidth(highestLabel);
        context.drawText(textRenderer, 
            Text.literal(highestLabel), scaledChartX + scaledAvailableWidth - (int)(highestLabelWidth * AXIS_FONT_SCALE), 
            scaledChartY + scaledAvailableHeight + 5, 0xFFAAAAAA, false);
        
        if (attemptRange > 1 && scaledAvailableWidth > 80 && sortedEntries.size() > 2) {

            int midBracket = (minBracket + maxBracket) / 2;
            String midLabel = formatBracketLabel(midBracket);
            int midLabelWidth = textRenderer.getWidth(midLabel);
            context.drawText(textRenderer, 
                Text.literal(midLabel), scaledChartX + (scaledAvailableWidth - (int)(midLabelWidth * AXIS_FONT_SCALE)) / 2, 
                scaledChartY + scaledAvailableHeight + 5, 0xFFAAAAAA, false);
        }

        context.getMatrices().popMatrix();
    }
    
    private Text getCachedMeanText(String creatureKey) {
        return meanTextCache.computeIfAbsent(creatureKey, key -> {
            double meanAttempts = ScData.getInstance().getMeanAttemptsFor(key);
            return meanAttempts > 0 ? Text.literal(String.format("§7Mean: §f%.1f", meanAttempts)) : EMPTY_MEAN_TEXT;
        });
    }
    
    private Text getCachedRateText(String creatureKey) {
        return rateTextCache.computeIfAbsent(creatureKey, key -> {
            double catchRate = ScData.getInstance().getCatchChance(key);
            return catchRate > 0 ? Text.literal(String.format("§7Rate: §f%.1f%%", catchRate)) : EMPTY_RATE_TEXT;
        });
    }

    private void clearAllCaches() {
        displayNameCache.clear();
        meanTextCache.clear();
        rateTextCache.clear();
        histogramCache.clear();
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

    private List<String> getCreaturesForArea(Island area) {
        return ScRegistry.getInstance().getCreaturesForArea(area);
    }

    @Override
    public Rectangle getBounds(MinecraftClient mc) {
        Island currentArea = ScStats.getInstance().getCurrentAreaKey();
        List<String> creatures = getCreaturesForArea(currentArea);
        int spacing = CHART_WIDTH + 10;
        int count = creatures.size();

        int x = getHudX();
        int y = getHudY();

        int screenW = mc.getWindow().getScaledWidth();
        int leftThreshold = screenW / 3;
        int rightThreshold = (2 * screenW) / 3;

        boolean anchorOnLeft = x < leftThreshold;
        boolean anchorOnRight = x >= rightThreshold;

        float scale = getHudSize() / 100.0f;
        int scaledSpacing = Math.round(spacing * scale);
        int scaledChartWidth = Math.round(CHART_WIDTH * scale);

        int minX = x;
        int maxX = x + scaledChartWidth;

        if (count > 1) {
            if (anchorOnLeft) {
                maxX = x + (count - 1) * scaledSpacing + scaledChartWidth;
            } else if (anchorOnRight) {
                minX = x - (count - 1) * scaledSpacing;
                maxX = x + scaledChartWidth;
            } else {
                int rightCount = (count - 1 + 1) / 2;
                int leftCount = (count - 1) / 2;
                maxX = x + rightCount * scaledSpacing + scaledChartWidth;
                minX = x - leftCount * scaledSpacing;
            }
        }

        int totalWidth = Math.max(scaledChartWidth, maxX - minX);
        int height = Math.round((CHART_HEIGHT + 50) * scale);
        return new Rectangle(minX, y, totalWidth, height);
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
        rateTextCache.clear();
        histogramCache.clear();
        lastDataVersion = -1;  // Force refresh on next render
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
    
    private String formatAreaName(Island areaKey) {
        switch (areaKey) {
            case PLHLEGBLAST: return "Crimson Isles (Pool)";
            case CI_HOTSPOT: return "Crimson Isles (Hotspot)";
            case NA: return "Unknown Area";
            default: return areaKey.displayName();
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
