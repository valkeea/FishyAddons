package me.valkeea.fishyaddons.hud.base;

import java.awt.Rectangle;

import me.valkeea.fishyaddons.hud.core.HudDrawer;
import me.valkeea.fishyaddons.hud.core.HudElement;
import me.valkeea.fishyaddons.hud.core.HudElementState;
import me.valkeea.fishyaddons.hud.core.HudUtils;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.config.impl.HudConfig;
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
    protected BaseHudElement(BooleanKey hudKey, String displayName, 
                           int defaultX, int defaultY, int defaultSize, int defaultColor,
                           boolean defaultOutline, boolean defaultBg) {
        this.hudKey = hudKey.getString();
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
                getHudX(),
                getHudY(),
                getHudSize(),
                getHudColor(),
                getHudOutline(),
                getHudBg()
            );
            onCacheRefresh();
        }
        return cachedState;
    }

    @Override
    public final void invalidateCache() {
        cachedState = null;
    }

    /** The key used for state getters/setters */
    protected String getHudKey() {
        return hudKey;
    }

    @Override
    public void resetAll() {
        setHudPosition(defaultX, defaultY);
        setHudSize(defaultSize);
        setHudColor(defaultColor);
        setHudOutline(defaultOutline);
        setHudBg(defaultBg);
        invalidateCache();
    }

    @Override public final int getHudX() { return HudConfig.getHudX(getHudKey(), defaultX); }
    @Override public final int getHudY() { return HudConfig.getHudY(getHudKey(), defaultY); }
    @Override public final void setHudPosition(int x, int y) { HudConfig.setHudX(getHudKey(), x); HudConfig.setHudY(getHudKey(), y); }
    @Override public final int getHudSize() { return HudConfig.getHudSize(getHudKey(), defaultSize); }
    @Override public final void setHudSize(int size) { HudConfig.setHudSize(getHudKey(), size); }
    @Override public final int getHudColor() { return HudConfig.getHudColor(getHudKey(), defaultColor); }
    @Override public final void setHudColor(int color) { HudConfig.setHudColor(getHudKey(), color); }
    @Override public final boolean getHudOutline() { return HudConfig.getHudOutline(getHudKey(), defaultOutline); }
    @Override public final void setHudOutline(boolean outline) { HudConfig.setHudOutline(getHudKey(), outline); }
    @Override public final boolean getHudBg() { return HudConfig.getHudBg(getHudKey(), defaultBg); }
    @Override public final void setHudBg(boolean bg) { HudConfig.setHudBg(getHudKey(), bg); }
    @Override public final void setEditingMode(boolean editing) { this.editingMode = editing; }
    @Override public final String getDisplayName() { return displayName; }
    
    protected final boolean isEditingMode() { return editingMode; }
}
