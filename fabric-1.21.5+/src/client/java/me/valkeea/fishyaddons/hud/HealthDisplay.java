package me.valkeea.fishyaddons.hud;

import java.awt.Rectangle;
import java.util.List;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.tracker.ValuableMobs;
import me.valkeea.fishyaddons.tracker.ValuableMobs.ValuableMobInfo;
import me.valkeea.fishyaddons.util.text.Color;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class HealthDisplay implements HudElement {
    private boolean editingMode = false;
    private static final String HUD_KEY = me.valkeea.fishyaddons.config.Key.HUD_HEALTH_ENABLED;
    private HudElementState cachedState = null;

    public void register() {
        HudLayerRegistrationCallback.EVENT.register(layeredDrawer ->
            layeredDrawer.attachLayerAfter(
                IdentifiedLayer.MISC_OVERLAYS,
                Identifier.of("fishyaddons", "health_hud"),
                (context, tickCounter) -> render(context, 0, 0)
            )
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY) {
        if (!editingMode && !ValuableMobs.displayOn()) return;

        var mc = MinecraftClient.getInstance();
        var state = getCachedState();
        int hudX = state.x;
        int hudY = state.y;
        int size = state.size;
        boolean showBg = state.bg;
        float scale = size / 12.0F;

        List<ValuableMobInfo> valuableMobs = ValuableMobs.getValuableMobs();
        if (!editingMode && valuableMobs.isEmpty()) return;

        int mobCount = editingMode ? 2 : valuableMobs.size();
        if (mobCount == 0) return;

        int lineHeight = (int)(size * 1.2f);
        int healthBarHeight = (int)(3 * scale);
        int mobSpacing = (int)(2 * scale);
        int entryHeight = lineHeight + healthBarHeight + mobSpacing;
        
        int maxWidth = getMaxDisplayWidth(mc, valuableMobs, editingMode, scale);
        int totalHeight = mobCount * entryHeight - mobSpacing;

        if (showBg) {
            drawBackground(context, hudX, hudY, maxWidth, totalHeight);
        }

        context.getMatrices().push();
        context.getMatrices().translate(hudX, hudY, 0);
        context.getMatrices().scale(scale, scale, 1.0F);

        int currentY = 0;

        if (editingMode) {
            renderMobEntry(context, mc, Text.literal("§dLord Jawbus"), 1250, 2000, currentY, state);
            currentY += entryHeight / scale;
            renderMobEntry(context, mc, Text.literal("§dThunder"), 800, 1500, currentY, state);
        } else {

            for (ValuableMobInfo mobInfo : valuableMobs) {
                Text mobName = mobInfo.getDisplayName();
                int currentHealth = mobInfo.getHealth();
                int maxHealth = mobInfo.getMaxHealth();
                
                renderMobEntry(context, mc, mobName, currentHealth, maxHealth, currentY, state);
                currentY += entryHeight / scale;
            }
        }

        context.getMatrices().pop();

        if (editingMode) {
            context.drawBorder(hudX - 1, hudY - 1, maxWidth + 2, totalHeight + 2, 0xFFFFFFFF);
        }
    }

    private void renderMobEntry(DrawContext context, MinecraftClient mc, Text mobName, 
                               int currentHealth, int maxHealth, int yOffset, HudElementState state) {
        int size = state.size;
        int color = state.color;
        float scale = size / 12.0F;

        String healthText = currentHealth > 0 && maxHealth > 0 ? 
            String.format("§c%d§8/§c%d❤", currentHealth, maxHealth) : "Unknown";

        var healthDisplay = Text.literal(healthText);
        var hudRenderer = new HudVisuals(mc, context, state);
        int healthTextWidth = mc.textRenderer.getWidth(healthDisplay);
        int nameWidth = mc.textRenderer.getWidth(mobName);
        int textX = Math.max(nameWidth + (int)(10 * scale), (int)(120 * scale) - healthTextWidth);

        hudRenderer.drawText(mobName, 0, yOffset, color);
        hudRenderer.drawText(healthDisplay, textX, yOffset, color);

        int barY = yOffset + size + (int)(2 * scale);
        int barWidth = Math.max(nameWidth + healthTextWidth + (int)(10 * scale), (int)(120 * scale));
        int height = Math.clamp((int)(3 * scale), 1, 4);

        drawHealthBar(context, 0, barY, barWidth, height, currentHealth, maxHealth);
    }

    private void drawHealthBar(DrawContext context, int x, int y, int width, int height, 
                                int currentHealth, int maxHealth) {

        context.fill(x, y, x + width, y + height, 0x80000000);
        context.drawBorder(x, y, width, height + 1, 0xFF000000);

        if (maxHealth > 0 && currentHealth > 0) {
            float healthPercent = Math.min(1.0f, (float) currentHealth / maxHealth);
            int healthWidth = (int) (width * healthPercent);
            int healthColor;
            int color = Color.ensureOpaque(getHudColor());

            if (healthPercent > 0.6f) {
                healthColor = Color.brighten(color, 0.3f);
            } else if (healthPercent > 0.2f) {
                healthColor = color;
            } else {
                healthColor = 0xFFFF8080;
            }

            context.fill(x, y, x + healthWidth, y + height, healthColor);
        }
    }

    private void drawBackground(DrawContext context, int x, int y, int width, int height) {
        context.fill(x - 2, y - 2, x + width + 2, y + height + 2, 0x80000000);
    }

    private int getMaxDisplayWidth(MinecraftClient mc, List<ValuableMobInfo> mobs, 
                                  boolean editingMode, float scale) {
        int maxWidth = (int)(120 * scale); // Minimum width
        
        if (editingMode) {
            int exampleWidth1 = mc.textRenderer.getWidth("Lord Jawbus 1250/2000❤");
            int exampleWidth2 = mc.textRenderer.getWidth("Thunder 800/1500❤");
            maxWidth = Math.max(maxWidth, (int)(Math.max(exampleWidth1, exampleWidth2) * scale));

        } else {

            for (ValuableMobInfo mob : mobs) {
                String healthText = mob.getHealth() > 0 && mob.getMaxHealth() > 0 ? 
                    String.format("%d/%d❤", mob.getHealth(), mob.getMaxHealth()) : "Unknown";
                String fullText = mob.getName() + " " + healthText;
                int textWidth = mc.textRenderer.getWidth(fullText);
                maxWidth = Math.max(maxWidth, (int)(textWidth * scale));
            }
        }
        
        return maxWidth + (int)(4 * scale);
    }

    @Override
    public Rectangle getBounds(MinecraftClient mc) {
        int hudX = getHudX();
        int hudY = getHudY();
        int size = getHudSize();
        float scale = size / 12.0F;
        
        List<ValuableMobInfo> valuableMobs = ValuableMobs.getValuableMobs() == null ? 
            List.of() : ValuableMobs.getValuableMobs();
        int mobCount = editingMode ? 2 : valuableMobs.size();
        if (mobCount == 0) mobCount = 1;
        
        int lineHeight = (int)(size * 1.2f);
        int healthBarHeight = (int)(3 * scale);
        int mobSpacing = (int)(2 * scale);
        int entryHeight = lineHeight + healthBarHeight + mobSpacing;
        
        int width = getMaxDisplayWidth(mc, valuableMobs, editingMode, scale);
        int height = mobCount * entryHeight - mobSpacing;
        
        return new Rectangle(hudX, hudY, width, height);
    }   
    
    @Override
    public HudElementState getCachedState() {
        if (cachedState == null) {
            cachedState = new HudElementState(
                FishyConfig.getHudX(HUD_KEY, 5),
                FishyConfig.getHudY(HUD_KEY, 5),
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
    @Override public int getHudY() { return FishyConfig.getHudY(HUD_KEY, 5); }
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
    @Override public String getDisplayName() { return "Health Display"; }
}