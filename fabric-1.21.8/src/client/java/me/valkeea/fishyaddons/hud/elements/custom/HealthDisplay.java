package me.valkeea.fishyaddons.hud.elements.custom;

import java.awt.Rectangle;
import java.util.List;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.hud.core.HudDrawer;
import me.valkeea.fishyaddons.hud.core.HudElement;
import me.valkeea.fishyaddons.hud.core.HudElementState;
import me.valkeea.fishyaddons.tracker.profit.ValuableMobs;
import me.valkeea.fishyaddons.tracker.profit.ValuableMobs.ValuableMobInfo;
import me.valkeea.fishyaddons.ui.VCRenderUtils;
import me.valkeea.fishyaddons.util.text.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class HealthDisplay implements HudElement {
    private boolean editingMode = false;
    private static final String HUD_KEY = me.valkeea.fishyaddons.config.Key.HUD_HEALTH_ENABLED;
    private HudElementState cachedState = null;

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

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(hudX, hudY);
        context.getMatrices().scale(scale, scale);

        int currentY = 0;

        if (editingMode) {
            renderMobEntry(context, mc, Text.literal("§dLord Jawbus"), 1250, 2000, currentY, state);
            currentY += entryHeight / scale;
            renderMobEntry(context, mc, Text.literal("§dThunder"), 800, 1500, currentY, state);
        } else {

            for (ValuableMobInfo mobInfo : valuableMobs) {

                renderMobEntry(
                    context, mc,
                    mobInfo.getDisplayName(),
                    mobInfo.getHealth(),
                    mobInfo.getMaxHealth(),
                    currentY, state
                );

                currentY += entryHeight / scale;
            }
        }

        context.getMatrices().popMatrix();
    }

    private void renderMobEntry(DrawContext context, MinecraftClient mc, Text mobName, 
                               int currentHealth, int maxHealth, int yOffset, HudElementState state) {

        if (maxHealth <= 0 || currentHealth < 0) return;

        int size = state.size;
        int color = state.color;
        float scale = size / 12.0F;

        short healthPercent = maxHealth > 0 ? (short)((currentHealth * 100) / maxHealth) : 0;
        String healthFormat = healthPercent > 60 ? "§a" : (healthPercent > 15 ? "§e" : "§c");
        String health = healthFormat + String.format("%d/%d§c❤", currentHealth, maxHealth);

        var healthDisplay = Text.literal(health);
        int healthWidth = mc.textRenderer.getWidth(health);
        int nameWidth = mc.textRenderer.getWidth(mobName);
        int textX = nameWidth + (int)(10 * scale);
        var drawer = new HudDrawer(mc, context, state);

        drawer.drawText(mobName, 0, yOffset, color);
        drawer.drawText(healthDisplay, textX, yOffset, color);

        int barY = yOffset + size + (int)(2 * scale);
        int barWidth = Math.max(nameWidth + healthWidth + (int)(10 * scale), (int)(120 * scale));
        int height = Math.clamp((int)(3 * scale), 1, 4);

        drawHealthBar(context, 0, barY, barWidth, height, healthPercent);
    }

    private void drawHealthBar(DrawContext context, int x, int y, int width, int height, short healthPercent) {

        context.fill(x, y, x + width, y + height, 0x80000000);
        VCRenderUtils.border(context, x, y, width, height + 1, 0xFF000000);

        if (healthPercent > 0) {
            int healthWidth = width * healthPercent / 100;
            int healthColor;
            int color = Color.ensureOpaque(getHudColor());

            if (healthPercent > 60) {
                healthColor = color;
            } else if (healthPercent > 15) {
                healthColor = 0xFFFFFF00;
            } else {
                healthColor = 0xFFFF0000;
            }

            context.fill(x, y, x + healthWidth, y + height, healthColor);
        }
    }

    private void drawBackground(DrawContext context, int x, int y, int width, int height) {
        context.fill(x - 2, y - 2, x + width + 2, y + height + 2, 0x80000000);
    }

    private int getMaxDisplayWidth(MinecraftClient mc, List<ValuableMobInfo> mobs, 
                                  boolean editingMode, float scale) {
        int maxWidth = (int)(120 * scale);
        
        if (editingMode) {
            int exampleWidth1 = mc.textRenderer.getWidth("Lord Jawbus 1250/2000❤");
            int exampleWidth2 = mc.textRenderer.getWidth("Thunder 800/1500❤");
            maxWidth = Math.max(maxWidth, (int)(Math.max(exampleWidth1, exampleWidth2) * scale));

        } else {

            for (ValuableMobInfo mob : mobs) {
                String health = mob.getHealth() > 0 && mob.getMaxHealth() > 0 ? 
                    String.format("%d/%d❤", mob.getHealth(), mob.getMaxHealth()) : "Unknown";
                String fullText = mob.getName() + " " + health;
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
                FishyConfig.getHudColor(HUD_KEY, 0xFFFFFFFF),
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
    @Override public int getHudColor() { return FishyConfig.getHudColor(HUD_KEY, 0xFFEECAEC); }
    @Override public void setHudColor(int color) { FishyConfig.setHudColor(HUD_KEY, color); }
    @Override public boolean getHudOutline() { return FishyConfig.getHudOutline(HUD_KEY, false); }
    @Override public void setHudOutline(boolean outline) { FishyConfig.setHudOutline(HUD_KEY, outline); }   
    @Override public boolean getHudBg() { return FishyConfig.getHudBg(HUD_KEY, true); }
    @Override public void setHudBg(boolean bg) { FishyConfig.setHudBg(HUD_KEY, bg); }
    @Override public void setEditingMode(boolean editing) { this.editingMode = editing; }
    @Override public String getDisplayName() { return "Health Display"; }
}
