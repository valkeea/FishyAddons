package me.valkeea.fishyaddons.hud.base;

import java.awt.Rectangle;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.hud.core.HudElement;
import me.valkeea.fishyaddons.hud.core.HudElementState;
import me.valkeea.fishyaddons.hud.core.HudDrawer;
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
    public final void render(DrawContext context, int mouseX, int mouseY) {
        if (!editingMode && !shouldRender()) return;

        var mc = MinecraftClient.getInstance();
        var state = getCachedState();

        if (state.bg) {
            Rectangle bounds = calculateContentBounds(mc, state);
            context.fill(bounds.x + 1, bounds.y + 2, 
                        bounds.x + bounds.width + 2, bounds.y + bounds.height - 1, 0x80000000);
        }

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(state.x, state.y);
        context.getMatrices().scale(state.size / 12.0F, state.size / 12.0F);

        HudDrawer drawer = new HudDrawer(mc, context, state);
        renderContent(drawer, mc, state);

        context.getMatrices().popMatrix();
        
        if (editingMode) {
            renderEditingMode(drawer, mc, state);
        }
    }

    protected Rectangle calculateContentBounds(MinecraftClient mc, HudElementState state) {
        Rectangle bounds = getBounds(mc);
        return new Rectangle(state.x, state.y, 
                           (int)(bounds.width / (state.size / 12.0F)), 
                           (int)(bounds.height / (state.size / 12.0F)));
    }

    protected abstract boolean shouldRender();
    protected abstract void renderContent(HudDrawer drawer, MinecraftClient mc, HudElementState state);
    protected abstract int calculateContentWidth(MinecraftClient mc);
    protected abstract int calculateContentHeight(MinecraftClient mc);
    
    protected void renderEditingMode(HudDrawer drawer, MinecraftClient mc, HudElementState state) {
        // Custom editing mode rendering
    }

    @Override
    public Rectangle getBounds(MinecraftClient mc) {
        int hudX = getHudX();
        int hudY = getHudY();
        int size = getHudSize();
        float scale = size / 12.0F;
        
        int width = (int)(calculateContentWidth(mc) * scale);
        int height = (int)(calculateContentHeight(mc) * scale);
        
        return new Rectangle(hudX, hudY, width, height);
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
