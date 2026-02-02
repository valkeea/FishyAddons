package me.valkeea.fishyaddons.hud.elements.segmented;

import java.awt.Rectangle;
import java.util.List;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.feature.skyblock.timer.EffectTimers;
import me.valkeea.fishyaddons.hud.core.ElementRegistry;
import me.valkeea.fishyaddons.hud.core.HudDrawer;
import me.valkeea.fishyaddons.hud.core.HudElement;
import me.valkeea.fishyaddons.hud.core.HudElementState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class EffectDisplay implements HudElement {
    private boolean editingMode = false;
    private static final String HUD_KEY = Key.HUD_EFFECTS_ENABLED;
    private HudElementState cachedState = null;
    private boolean intersects = false;

    @Override
    public void render(DrawContext context, int mouseX, int mouseY) {
        if (!editingMode && !FishyConfig.getState(HUD_KEY, false)) return;

        var mc = MinecraftClient.getInstance();
        var state = getCachedState();
        int hudX = state.x;
        int hudY = state.y;
        int size = state.size;
        int color = state.color;
        boolean showBg = state.bg;

        float scale = Math.max(0.5f, size / 12.0F);

        List<EffectTimers.Entry> entries = EffectTimers.getInstance().listActive();

        boolean empty = entries.isEmpty();

        if (editingMode && empty) {

            context.getMatrices().pushMatrix();
            context.getMatrices().translate(hudX, hudY);
            context.getMatrices().scale(scale, scale); 

            int y = 0;

            context.fill(0, y, 16, y + 16, 0xFF3A3A3A);
            HudDrawer.drawText(context, Text.literal("59m"), 18, y + 4, color, state.outlined);

            y += 20;
            
            context.fill(0, y, 16, y + 16, 0xFF3A3A3A);
            HudDrawer.drawText(context, Text.literal("1h 12m"), 18, y + 4, color, state.outlined);

            context.getMatrices().popMatrix();
            return;
        }

        if (empty) return;

        int maxTextWidth = 0;
        for (var e : entries) {
            String time = EffectTimers.formatTime(e.remainingMs());
            int tw = (int)(mc.textRenderer.getWidth(time) * scale);
            maxTextWidth = Math.max(maxTextWidth, tw);
        }

        int totalW = (int)(16 * scale + 18 + maxTextWidth);
        int totalH = (int)(entries.size() * 20 * scale);

        if (showBg) {
            int shadowX1 = hudX + 1;
            int shadowX2 = hudX + totalW + 2;            
            int shadowY1 = intersects ? hudY - totalH - 2 : hudY + 2;
            int shadowY2 = intersects ? hudY - 1 : hudY + totalH - 1;
            context.fill(shadowX1, shadowY1, shadowX2, shadowY2, 0x80000000);
        }

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(hudX, hudY);
        context.getMatrices().scale(scale, scale); 

        int y = 0;

        for (var e : entries) {

            if (e.textureId != null) {
                context.drawTexture(
                    RenderPipelines.GUI_TEXTURED,
                    e.textureId,
                    0, y, 0, 0, 16, 16, 16, 16
                );
            }

            String time = EffectTimers.formatTime(e.remainingMs());
            HudDrawer.drawText(context, Text.literal(time), 18, y + 4, color, state.outlined);

            y += intersects ? -20 : 20;
        }

        context.getMatrices().popMatrix();
    }

    public void updateSpace() {
        var mc = MinecraftClient.getInstance();
        int totalH = (int)(Math.max(1, EffectTimers.getInstance().listActive().size()) * 20 * Math.max(0.5f, getHudSize() / 12.0F));
        if (getHudY() + totalH > mc.getWindow().getScaledHeight()) {
            intersects = true;
            return;
        }

        for (HudElement other : ElementRegistry.getElements()) {
            if (other == this) continue;
            var otherBounds = other.getBounds(mc);
            var thisBounds = getBounds(mc);
            if (thisBounds.intersects(otherBounds)) {
                intersects = true;
                return;
            }
        }
        
        intersects = false;
    }

    @Override
    public Rectangle getBounds(MinecraftClient mc) {
        int hudX = getHudX();
        int hudY = getHudY();
        int size = getHudSize();
        float scale = Math.max(0.5f, size / 12.0F);
        int entries = Math.max(1, EffectTimers.getInstance().listActive().size());
        int width = (int)(60 * scale); 
        int height = (int)(entries * 20 * scale);
        return new Rectangle(hudX, hudY, width, height);
    }

    @Override
    public HudElementState getCachedState() {
        if (cachedState == null) {
            cachedState = new HudElementState(
                FishyConfig.getHudX(HUD_KEY, 6),
                FishyConfig.getHudY(HUD_KEY, 28),
                FishyConfig.getHudSize(HUD_KEY, 12),
                FishyConfig.getHudColor(HUD_KEY, 0xFFFFFF),
                FishyConfig.getHudOutline(HUD_KEY, false),
                FishyConfig.getHudBg(HUD_KEY, true)
            );
            updateSpace();
        }
        return cachedState;
    }

    @Override public void invalidateCache() { cachedState = null; }
    @Override public int getHudX() { return FishyConfig.getHudX(HUD_KEY, 6); }
    @Override public int getHudY() { return FishyConfig.getHudY(HUD_KEY, 28); }
    @Override public void setHudPosition(int x, int y) { FishyConfig.setHudX(HUD_KEY, x); FishyConfig.setHudY(HUD_KEY, y); }
    @Override public int getHudSize() { return FishyConfig.getHudSize(HUD_KEY, 12); }
    @Override public void setHudSize(int size) { FishyConfig.setHudSize(HUD_KEY, size); }
    @Override public int getHudColor() { return FishyConfig.getHudColor(HUD_KEY, 0xFFFFFF); }
    @Override public void setHudColor(int color) { FishyConfig.setHudColor(HUD_KEY, color); }
    @Override public boolean getHudOutline() { return FishyConfig.getHudOutline(HUD_KEY, true); }
    @Override public void setHudOutline(boolean outline) { FishyConfig.setHudOutline(HUD_KEY, outline); }
    @Override public boolean getHudBg() { return FishyConfig.getHudBg(HUD_KEY, false); }
    @Override public void setHudBg(boolean bg) { FishyConfig.setHudBg(HUD_KEY, bg); }
    @Override public void setEditingMode(boolean editing) { editingMode = editing; }
    @Override public String getDisplayName() { return HUD_KEY; }
}
