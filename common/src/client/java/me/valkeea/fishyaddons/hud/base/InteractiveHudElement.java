package me.valkeea.fishyaddons.hud.base;

import java.util.List;

import me.valkeea.fishyaddons.hud.core.ClickableRegionManager;
import me.valkeea.fishyaddons.hud.core.HudButtonManager;
import me.valkeea.fishyaddons.hud.core.HudDrawer;
import me.valkeea.fishyaddons.hud.core.HudElementState;
import me.valkeea.fishyaddons.hud.core.HudUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Elements with interactive features:
 * - Inventory buttons
 * - Clickable regions (lines, menu items)
 * - Tooltips for clickable regions
 */
public abstract class InteractiveHudElement extends BaseHudElement {
    
    private HudButtonManager buttonManager;
    private ClickableRegionManager regionManager;
    
    protected InteractiveHudElement(String hudKey, String displayName,
                                   int defaultX, int defaultY, int defaultSize, int defaultColor,
                                   boolean defaultOutline, boolean defaultBg) {
        super(hudKey, displayName, defaultX, defaultY, defaultSize, defaultColor, defaultOutline, defaultBg);
        this.regionManager = new ClickableRegionManager();
    }
    
    /** 
     * Set up buttons when inventory is open
     */
    protected void setupButtons(HudButtonManager manager, HudElementState state) {}
    
    /**
     * Set up clickable regions during render
     */
    protected void setupClickableRegions(ClickableRegionManager manager, HudElementState state) {}

    protected void renderButtons(DrawContext context, MinecraftClient mc, HudElementState state) {
        float scale = state.size / 12.0F;
        
        if (buttonManager == null || buttonManager.size() == 0) {
            buttonManager = new HudButtonManager(state.x, state.y, scale);
            setupButtons(buttonManager, state);
        } else {
            buttonManager.updatePositions(state.x, state.y, scale);
        }

        var window = mc.getWindow();
        double actualMouseX = mc.mouse.getX() * window.getScaledWidth() / window.getWidth();
        double actualMouseY = mc.mouse.getY() * window.getScaledHeight() / window.getHeight();
        
        var drawer = new HudDrawer(mc, context, state);
        buttonManager.render(drawer, actualMouseX, actualMouseY);
    }
    
    protected void drawTextLines(HudDrawer drawer, MinecraftClient mc, HudElementState state) {
        List<Text> lines = getDisplayLines(state);
        if (lines == null) return;
        
        int size = state.size;
        float scale = size / 12.0F;
        
        regionManager.clear();
        
        for (int i = 0; i < lines.size(); i++) {
            Text line = lines.get(i);
            if (line == null) continue;
            
            int y = i * (size);
            drawer.drawText(line, 0, y, state.color);
            
            if (isLineClickable(i, line)) {
                int lineWidth = mc.textRenderer.getWidth(line);
                int scaledY = (int)(state.y + i * size * scale);
                var region = regionManager.addLine(state.x, scaledY, (int)(lineWidth * scale), (int)(size * scale), 
                                    line, this::handleLineClick);
                
                List<Text> tooltip = getLineTooltip(i, line);
                if (tooltip != null && !tooltip.isEmpty()) {
                    region.withTooltip(tooltip);
                }
            }
        }
        
        setupClickableRegions(regionManager, state);
    }
    
    /**
     * Check if inventory screen is open
     */
    protected boolean isInventoryOpen(MinecraftClient mc) {
        return HudUtils.isInventoryOpen(mc);
    }
    
    public boolean handleMouseClick(Click click) {
        if (isEditingMode()) return false;

        if (shouldShowButtons(MinecraftClient.getInstance()) && buttonManager != null
            && buttonManager.handleClick(click.x(), click.y(), click.button())) {
            return true;
        }
        
        return regionManager.handleClick(click.x(), click.y(), click.button());
    }
    
    /** Get lines to display in the HUD */
    protected abstract List<Text> getDisplayLines(HudElementState state);

    /** Draw custom content inside matrix */
    protected void drawCustomContent(HudDrawer drawer, MinecraftClient mc, HudElementState state) {}    
    
    /** Whether to show buttons (default: when inventory is open) */
    protected boolean shouldShowButtons(MinecraftClient mc) {
        return isInventoryOpen(mc);
    }
    
    /** Whether a specific line should be clickable, default: false */
    @SuppressWarnings("squid:S1172")
    protected boolean isLineClickable(int lineIndex, Text line) {
        return false;
    }
    
    /** Get tooltip for a clickable line, default: empty */
    @SuppressWarnings("squid:S1172")
    protected List<Text> getLineTooltip(int lineIndex, Text line) {
        return List.of();
    }
    
    /** Handle click on a line */
    protected void handleLineClick(Text line) {}
    
    /** Get clickable region manager for custom region setup */
    protected ClickableRegionManager getRegionManager() {
        return regionManager;
    }
    
    /** Get button manager for custom button manipulation */
    protected HudButtonManager getButtonManager() {
        return buttonManager;
    }
    
    @Override
    protected final void postRender(DrawContext context, MinecraftClient mc, HudElementState state, int mouseX, int mouseY) {

        if (!isEditingMode() && shouldShowButtons(mc)) {
            renderButtons(context, mc, state);
        }
        
        if (!isEditingMode() && isInventoryOpen(mc)) {
            var window = mc.getWindow();
            double actualMouseX = mc.mouse.getX() * window.getScaledWidth() / window.getWidth();
            double actualMouseY = mc.mouse.getY() * window.getScaledHeight() / window.getHeight();
            var drawer = new HudDrawer(mc, context, state);
            regionManager.renderTooltips(context, drawer, actualMouseX, actualMouseY);
        }

        postRenderCustom(context, mc, state, mouseX, mouseY);
    }
    
    /**
     * Custom rendering after matrix pop
     */
    protected void postRenderCustom(DrawContext context, MinecraftClient mc, HudElementState state, int mouseX, int mouseY) {}
    
    @Override
    protected void renderContent(HudDrawer drawer, MinecraftClient mc, HudElementState state) {
        drawTextLines(drawer, mc, state);
        drawCustomContent(drawer, mc, state);
    }
    
    @Override
    protected int calculateContentWidth(MinecraftClient mc) {
        var state = getCachedState();
        List<Text> lines = getDisplayLines(state);
        if (lines == null) return 12;
        return HudUtils.getMaxLineWidth(mc, lines, state.size / 12.0F);
    }
    
    @Override
    protected int calculateContentHeight(MinecraftClient mc) {
        var state = getCachedState();
        List<Text> lines = getDisplayLines(state);
        if (lines == null) return 12;
        return (int)(lines.size() * 12 * (state.size / 12.0F));
    }
}
