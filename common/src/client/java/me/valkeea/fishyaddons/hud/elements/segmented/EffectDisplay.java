package me.valkeea.fishyaddons.hud.elements.segmented;

import java.awt.Rectangle;
import java.util.List;

import me.valkeea.fishyaddons.feature.skyblock.timer.EffectTimers;
import me.valkeea.fishyaddons.hud.core.ElementRegistry;
import me.valkeea.fishyaddons.hud.core.HudDrawer;
import me.valkeea.fishyaddons.hud.core.HudElement;
import me.valkeea.fishyaddons.hud.core.HudElementState;
import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.config.impl.HudConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class EffectDisplay implements HudElement {
    private boolean editingMode = false;
    private static final String HUD_KEY = BooleanKey.HUD_EFFECTS_ENABLED.getString();
    private HudElementState cachedState = null;
    private boolean intersects = false;

    @Override
    public void render(DrawContext context, MinecraftClient mc, int mouseX, int mouseY) {
        if (!editingMode && !Config.get(BooleanKey.HUD_EFFECTS_ENABLED)) return;

        var state = getCachedState();
        int hudX = state.x;
        int hudY = state.y;
        int size = state.size;
        int color = state.color;
        boolean showBg = state.bg;

        float scale = Math.max(0.5f, size / 12.0F);

        List<EffectTimers.Entry> entries = EffectTimers.getInstance().listActive();

        boolean empty = entries.isEmpty();

        if (editingOrEmpty(empty, context, hudX, hudY, scale, color, state)) return;

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

    private boolean editingOrEmpty(boolean empty, DrawContext context, int hudX, int hudY, float scale, int color, HudElementState state) {
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
            return true;

        } else return empty;
    }

    public void updateSpace() {
        var mc = MinecraftClient.getInstance();
        int totalH = (int)(Math.max(1, EffectTimers.getInstance().listActive().size()) * 20 * Math.max(0.5f, getHudSize() / 12.0F));
        var window = mc.getWindow();
        int screenHeight = window.getFramebufferHeight() / window.getScaleFactor();
        
        if (getHudY() + totalH > screenHeight) {
            intersects = true;
            return;
        }

        for (var other : ElementRegistry.getElements()) {
            if (other == this || other == null) continue;
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
                getHudX(),
                getHudY(),
                getHudSize(),
                getHudColor(),
                getHudOutline(),
                getHudBg()
            );
            updateSpace();
        }
        return cachedState;
    }

    @Override
    public void resetAll() {
        setHudPosition(6, 28);
        setHudSize(12);
        setHudColor(0xFFFFFFFF);
        setHudOutline(true);
        setHudBg(false);
        invalidateCache();
    }

    @Override public void invalidateCache() { cachedState = null; }
    @Override public int getHudX() { return HudConfig.getHudX(HUD_KEY, 6); }
    @Override public int getHudY() { return HudConfig.getHudY(HUD_KEY, 28); }
    @Override public void setHudPosition(int x, int y) { HudConfig.setHudX(HUD_KEY, x); HudConfig.setHudY(HUD_KEY, y); }
    @Override public int getHudSize() { return HudConfig.getHudSize(HUD_KEY, 12); }
    @Override public void setHudSize(int size) { HudConfig.setHudSize(HUD_KEY, size); }
    @Override public int getHudColor() { return HudConfig.getHudColor(HUD_KEY, 0xFFFFFFFF); }
    @Override public void setHudColor(int color) { HudConfig.setHudColor(HUD_KEY, color); }
    @Override public boolean getHudOutline() { return HudConfig.getHudOutline(HUD_KEY, true); }
    @Override public void setHudOutline(boolean outline) { HudConfig.setHudOutline(HUD_KEY, outline); }
    @Override public boolean getHudBg() { return HudConfig.getHudBg(HUD_KEY, false); }
    @Override public void setHudBg(boolean bg) { HudConfig.setHudBg(HUD_KEY, bg); }
    @Override public void setEditingMode(boolean editing) { editingMode = editing; }
    @Override public String getDisplayName() { return HUD_KEY; }
}
