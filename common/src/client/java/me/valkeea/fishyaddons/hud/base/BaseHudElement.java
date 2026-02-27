package me.valkeea.fishyaddons.hud.base;

import java.awt.Rectangle;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.hud.core.HudDrawer;
import me.valkeea.fishyaddons.hud.core.HudElement;
import me.valkeea.fishyaddons.hud.core.HudElementState;
import me.valkeea.fishyaddons.hud.core.HudUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public abstract class BaseHudElement implements HudElement {
    private final String hudKey;
    private final String displayName;
    private final int defaultX;
    private final int defaultY;
    private final int defaultSize;
    private final int defaultColor;
    private final boolean defaultOutline;
    private final boolean defaultBg;
    
    private boolean editingMode = false;
    private HudElementState cachedState = null;

    @SuppressWarnings("java:S107")
    protected BaseHudElement(String hudKey, String displayName, 
                           int defaultX, int defaultY, int defaultSize, int defaultColor,
                           boolean defaultOutline, boolean defaultBg) {
        this.hudKey = hudKey;
        this.displayName = displayName;
        this.defaultX = defaultX;
        this.defaultY = defaultY;
        this.defaultSize = defaultSize;
        this.defaultColor = defaultColor;
        this.defaultOutline = defaultOutline;
        this.defaultBg = defaultBg;
    }

    @Override
    public final void render(DrawContext context, MinecraftClient mc, int mouseX, int mouseY) {
        if (!editingMode && !shouldRender()) return;

        var state = getCachedState();
        float scale = state.size / 12.0F;

        if (state.bg) drawBackGround(context, mc, state);

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(state.x, state.y);
        context.getMatrices().scale(scale, scale);

        var drawer = new HudDrawer(mc, context, state);
        renderContent(drawer, mc, state);

        context.getMatrices().popMatrix();

        postRender(context, mc, state, mouseX, mouseY);
        
        if (editingMode) {
            renderEditingMode(drawer, mc, state);
        }
    }

    protected final boolean isMouseOver(double mouseX, double mouseY) {
        var bounds = getBounds(MinecraftClient.getInstance());
        return bounds.contains(mouseX, mouseY);

    }

    protected abstract boolean shouldRender();
    protected abstract void renderContent(HudDrawer drawer, MinecraftClient mc, HudElementState state);
    protected abstract int calculateContentWidth(MinecraftClient mc);
    protected abstract int calculateContentHeight(MinecraftClient mc);

    /**
     * Draw background based on content size.
     * Default implementation draws a simple rectangle, can be overridden for custom backgrounds.
     */
    protected void drawBackGround(DrawContext context, MinecraftClient mc, HudElementState state) {
        float scale = state.size / 12.0F;
        int bgWidth = (int)(calculateContentWidth(mc) * scale);
        int bgHeight = (int)(calculateContentHeight(mc) * scale);
        HudUtils.drawBackground(context, state.x, state.y, bgWidth, bgHeight);
    }    
    
    /**
     * Special rendering in editing mode
     */
    protected void renderEditingMode(HudDrawer drawer, MinecraftClient mc, HudElementState state) {}

    /**
     * Rendering after matrix pop
     */
    protected void postRender(DrawContext context, MinecraftClient mc, HudElementState state, int mouseX, int mouseY) {}

    /**
     * Perform actions after cache refresh
     */
    protected void onCacheRefresh() {}

    @Override
    public Rectangle getBounds(MinecraftClient mc) {
        var state = getCachedState();
        float scale = state.size / 12.0F;
        
        int width = (int)(calculateContentWidth(mc) * scale);
        int height = (int)(calculateContentHeight(mc) * scale);
        
        return new Rectangle(state.x, state.y, width, height);
    }

    @Override
    public final HudElementState getCachedState() {
        if (cachedState == null) {
            cachedState = new HudElementState(
                FishyConfig.getHudX(hudKey, defaultX),
                FishyConfig.getHudY(hudKey, defaultY),
                FishyConfig.getHudSize(hudKey, defaultSize),
                FishyConfig.getHudColor(hudKey, defaultColor),
                FishyConfig.getHudOutline(hudKey, defaultOutline),
                FishyConfig.getHudBg(hudKey, defaultBg)
            );
            onCacheRefresh();
        }
        return cachedState;
    }

    @Override
    public final void invalidateCache() {
        cachedState = null;
    }

    @Override public final int getHudX() { return FishyConfig.getHudX(hudKey, defaultX); }
    @Override public final int getHudY() { return FishyConfig.getHudY(hudKey, defaultY); }
    @Override public final void setHudPosition(int x, int y) { FishyConfig.setHudX(hudKey, x); FishyConfig.setHudY(hudKey, y); }
    @Override public final int getHudSize() { return FishyConfig.getHudSize(hudKey, defaultSize); }
    @Override public final void setHudSize(int size) { FishyConfig.setHudSize(hudKey, size); }
    @Override public final int getHudColor() { return FishyConfig.getHudColor(hudKey, defaultColor); }
    @Override public final void setHudColor(int color) { FishyConfig.setHudColor(hudKey, color); }
    @Override public final boolean getHudOutline() { return FishyConfig.getHudOutline(hudKey, defaultOutline); }
    @Override public final void setHudOutline(boolean outline) { FishyConfig.setHudOutline(hudKey, outline); }
    @Override public final boolean getHudBg() { return FishyConfig.getHudBg(hudKey, defaultBg); }
    @Override public final void setHudBg(boolean bg) { FishyConfig.setHudBg(hudKey, bg); }
    @Override public final void setEditingMode(boolean editing) { this.editingMode = editing; }
    @Override public final String getDisplayName() { return displayName; }
    
    protected final boolean isEditingMode() { return editingMode; }
}
