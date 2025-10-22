package me.valkeea.fishyaddons.hud;

import java.awt.Rectangle;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.tracker.SkillTracker;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class SkillXpDisplay implements HudElement {
    private boolean editingMode = false;
    private static final String HUD_KEY = me.valkeea.fishyaddons.config.Key.HUD_SKILL_XP_ENABLED;
    private HudElementState cachedState = null;

    // Constants for formatting
    private static final String RATE_SUFFIX = "§8/h ";
    private static final String TOTAL_FORMAT = "§8(§7%,d§8) ";
    
    private static class SkillDisplayCache {
        final Text skillLabel;
        final Text rateValue;
        final Text xpValue;
        final int totalWidth;
        final Text catchLabel;
        final Text mobLabel; 
        final Text catchRateText;
        final Text mobRateText;
        final Text catchTotal;
        final Text mobTotal;
        final int fishingWidth;
        final boolean hasFishingData;
        
        SkillDisplayCache(SkillData data, MinecraftClient mc) {

            String formattedXp = String.format("%,d", data.xp);
            String formattedRate = String.format("%,d", data.rate);
            
            this.skillLabel = Text.literal(data.skillName + "§7: ");
            this.rateValue = Text.literal(formattedRate + RATE_SUFFIX);
            this.xpValue = Text.literal("§8(§7" + formattedXp + "§8)");
            
            this.totalWidth = mc.textRenderer.getWidth(skillLabel) + 
                             mc.textRenderer.getWidth(rateValue) + 
                             mc.textRenderer.getWidth(xpValue);
            
            boolean isFishing = data.skillName.toLowerCase().contains("fishing");
            this.hasFishingData = isFishing && data.catches > 0 && data.mobs > 0;
            
            if (hasFishingData) {
                this.catchLabel = Text.literal(" Catches: ");
                this.mobLabel = Text.literal("Mobs: ");
                this.catchRateText = Text.literal(String.format("%,d", data.catchRate) + RATE_SUFFIX);
                this.mobRateText = Text.literal(String.format("%,d", data.mobRate) + RATE_SUFFIX);
                this.catchTotal = Text.literal(String.format(TOTAL_FORMAT, data.catches));
                this.mobTotal = Text.literal(String.format(TOTAL_FORMAT, data.mobs));
                
                this.fishingWidth = mc.textRenderer.getWidth(catchLabel) + 
                                   mc.textRenderer.getWidth(catchRateText) + 
                                   mc.textRenderer.getWidth(catchTotal) +
                                   mc.textRenderer.getWidth(mobLabel) + 
                                   mc.textRenderer.getWidth(mobRateText) + 
                                   mc.textRenderer.getWidth(mobTotal);
            } else {
                this.catchLabel = this.mobLabel = this.catchRateText = this.mobRateText = 
                this.catchTotal = this.mobTotal = null;
                this.fishingWidth = 0;
            }
        }
        
        int getFullWidth() {
            return totalWidth + fishingWidth;
        }
    }
    
    private static class SkillData {
        final String skillName;
        final int xp;
        final int rate;
        final int catches;
        final int mobs;
        final int catchRate;
        final int mobRate;
        
        SkillData(String skillName, int xp, int rate, int catches, int mobs, int catchRate, int mobRate) {
            this.skillName = skillName;
            this.xp = xp;
            this.rate = rate;
            this.catches = catches;
            this.mobs = mobs;
            this.catchRate = catchRate;
            this.mobRate = mobRate;
        }
    }
    
    private static final java.util.Map<String, SkillDisplayCache> skillCaches = new java.util.concurrent.ConcurrentHashMap<>();

    public void register() {
        HudLayerRegistrationCallback.EVENT.register(layeredDrawer ->
            layeredDrawer.attachLayerAfter(
                IdentifiedLayer.MISC_OVERLAYS,
                Identifier.of("fishyaddons", "skill_xp_hud"),
                (context, tickCounter) -> render(context, 0, 0)
            )
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY) {
        if (!editingMode && !SkillTracker.isEnabled()) return;
        
        SkillTracker tracker = SkillTracker.getInstance();
        if (!editingMode && tracker.getTrackedSkills().isEmpty()) return;

        var mc = MinecraftClient.getInstance();
        var state = getCachedState();
        
        if (tracker.hasMultipleSkills()) {
            renderMultipleSkills(context, state, mc);
        } else {
            renderSingleSkill(context, tracker, state, mc);
        }
    }

    public static void refreshDisplay(SkillTracker tracker) {

        skillCaches.clear();
        for (String skillName : tracker.getTrackedSkills()) {
            SkillData data = new SkillData(
                skillName,
                tracker.getSkillXp(skillName),
                tracker.getXpPerHour(skillName),
                tracker.getCatchCount(),
                tracker.getMobCount(),
                tracker.getCatchRate(),
                tracker.getMobRate()
            );

            skillCaches.put(skillName, new SkillDisplayCache(data, MinecraftClient.getInstance()));
        }
    }

    private void renderSingleSkill(DrawContext context, SkillTracker tracker, HudElementState state, MinecraftClient mc) {
        String skillName = tracker.getTrackedSkill();
        if (skillName == null) return;
        
        SkillDisplayCache cache = skillCaches.get(skillName);
        if (cache == null) return;
        
        renderSkillDisplay(context, java.util.List.of(cache), state, mc);
    }

    private void renderMultipleSkills(DrawContext context, HudElementState state, MinecraftClient mc) {
        java.util.List<SkillDisplayCache> caches = skillCaches.values().stream()
            .sorted((a, b) -> a.skillLabel.getString().compareTo(b.skillLabel.getString()))
            .toList();
        
        renderSkillDisplay(context, caches, state, mc);
    }

    private void drawBackground(DrawContext context, int x, int y, int width, int height) {
        context.fill(x + 1, y + 2, x + width + 2, y + height - 1, 0x80000000);
    }

    private void renderSkillDisplay(DrawContext context, java.util.List<SkillDisplayCache> caches, 
                                   HudElementState state, MinecraftClient mc) {
        if (caches.isEmpty()) return;
        
        int hudX = state.x;
        int hudY = state.y;
        int size = state.size;
        boolean showBg = state.bg;
        float scale = size / 12.0F;
        
        int lineHeight = (int)(size * 1.2F);
        int maxWidth = caches.stream().mapToInt(SkillDisplayCache::getFullWidth).max().orElse(0);
        int totalHeight = caches.size() * lineHeight;
        
        if (editingMode || showBg) {
            drawBackground(context, hudX, hudY, (int)(maxWidth * scale), totalHeight);
        }
        
        context.getMatrices().push();
        context.getMatrices().translate(hudX, hudY, 0);
        context.getMatrices().scale(scale, scale, 1.0F);
        
        for (int i = 0; i < caches.size(); i++) {
            SkillDisplayCache cache = caches.get(i);
            int yOffset = (int)(i * lineHeight / scale);
            drawSkillLine(context, mc, cache, yOffset, state);
        }
        
        context.getMatrices().pop();
        
        if (editingMode) {
            context.drawBorder(hudX - 2, hudY - 2, (int)(maxWidth * scale) + 4, totalHeight + 4, 0xFFFFFFFF);
        }
    }
    
    private void drawSkillLine(DrawContext context, MinecraftClient mc, SkillDisplayCache cache, 
                               int yOffset, HudElementState state) {
        int color = SkillTracker.getInstance().isPaused() ? 0xAAAAAA : state.color;

        if (SkillTracker.getInstance().isDownTiming()) {
            color = 0xFF5555;
        }
        
        int currentX = 0;
        
        var hudRenderer = new HudVisuals(mc, context, state);
        // Draw skill label
        hudRenderer.drawText(cache.skillLabel, currentX, yOffset, color);
        currentX += mc.textRenderer.getWidth(cache.skillLabel);
        
        // Draw rate value
        hudRenderer.drawText(cache.rateValue, currentX, yOffset, 0xFFFFFF);
        currentX += mc.textRenderer.getWidth(cache.rateValue);
        
        // Draw XP value
        hudRenderer.drawText(cache.xpValue, currentX, yOffset, 0xAAAAAA);
        currentX += mc.textRenderer.getWidth(cache.xpValue);
        
        // Draw fishing stats if available
        if (cache.hasFishingData) {
            hudRenderer.drawText(cache.catchLabel, currentX, yOffset, color);
            currentX += mc.textRenderer.getWidth(cache.catchLabel);

            hudRenderer.drawText(cache.catchRateText, currentX, yOffset, 0xFFFFFF);
            currentX += mc.textRenderer.getWidth(cache.catchRateText);
            
            hudRenderer.drawText(cache.catchTotal, currentX, yOffset, 0xAAAAAA);
            currentX += mc.textRenderer.getWidth(cache.catchTotal);

            hudRenderer.drawText(cache.mobLabel, currentX, yOffset, color);
            currentX += mc.textRenderer.getWidth(cache.mobLabel);

            hudRenderer.drawText(cache.mobRateText, currentX, yOffset, 0xFFFFFF);
            currentX += mc.textRenderer.getWidth(cache.mobRateText);
            
            hudRenderer.drawText(cache.mobTotal, currentX, yOffset, 0xAAAAAA);
        }
    }

    @Override
    public Rectangle getBounds(MinecraftClient mc) {
        int hudX = getHudX();
        int hudY = getHudY();
        int size = getHudSize();
        float scale = size / 12.0F;
        
        SkillTracker tracker = SkillTracker.getInstance();
        java.util.Set<String> trackedSkills = tracker.getTrackedSkills();
        
        if (trackedSkills.isEmpty()) {
            int width = (int)(120 * scale);
            int height = (int)(size + 4 * scale);
            return new Rectangle(hudX, hudY, width, height);
        }
        
        refreshDisplay(tracker);
        
        int maxWidth = skillCaches.values().stream()
            .mapToInt(SkillDisplayCache::getFullWidth)
            .max().orElse(120);
        
        int width = (int)(maxWidth * scale);
        int lineHeight = (int)(size * 1.2F);
        int height = trackedSkills.size() * lineHeight;
        
        return new Rectangle(hudX, hudY, width, height);
    }   
    
    @Override
    public HudElementState getCachedState() {
        if (cachedState == null) {
            cachedState = new HudElementState(
                FishyConfig.getHudX(HUD_KEY, 5),
                FishyConfig.getHudY(HUD_KEY, 25),
                FishyConfig.getHudSize(HUD_KEY, 12),
                FishyConfig.getHudColor(HUD_KEY, 0xFFFFFF),
                FishyConfig.getHudOutline(HUD_KEY, false),
                FishyConfig.getHudBg(HUD_KEY, true)
            );
        }
        return cachedState;
    }

    @Override
    public void invalidateCache() {
        cachedState = null;
    }

    @Override public int getHudX() { return FishyConfig.getHudX(HUD_KEY, 5); }
    @Override public int getHudY() { return FishyConfig.getHudY(HUD_KEY, 25); }
    @Override public void setHudPosition(int x, int y) { FishyConfig.setHudX(HUD_KEY, x); FishyConfig.setHudY(HUD_KEY, y); }
    @Override public int getHudSize() { return FishyConfig.getHudSize(HUD_KEY, 12); }
    @Override public void setHudSize(int size) { FishyConfig.setHudSize(HUD_KEY, size); }
    @Override public int getHudColor() { return FishyConfig.getHudColor(HUD_KEY, 0xEECAEC); }
    @Override public void setHudColor(int color) { FishyConfig.setHudColor(HUD_KEY, color); }
    @Override public boolean getHudOutline() { return FishyConfig.getHudOutline(HUD_KEY, false); }
    @Override public void setHudOutline(boolean outline) { FishyConfig.setHudOutline(HUD_KEY, outline); }   
    @Override public boolean getHudBg() { return FishyConfig.getHudBg(HUD_KEY, true); }
    @Override public void setHudBg(boolean bg) { FishyConfig.setHudBg(HUD_KEY, bg); }
    @Override public void setEditingMode(boolean editing) { this.editingMode = editing; }
    @Override public String getDisplayName() { return "Skill XP Tracker"; }
}