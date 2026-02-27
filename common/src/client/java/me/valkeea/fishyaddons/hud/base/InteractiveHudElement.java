package me.valkeea.fishyaddons.hud.base;

import java.util.ArrayList;
import java.util.List;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.hud.core.ClickableRegionManager;
import me.valkeea.fishyaddons.hud.core.HudButtonManager;
import me.valkeea.fishyaddons.hud.core.HudDrawer;
import me.valkeea.fishyaddons.hud.core.HudElementState;
import me.valkeea.fishyaddons.hud.core.HudUtils;
import me.valkeea.fishyaddons.ui.widget.dropdown.VCToggleMenu;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Elements with interactive features:
 * - Inventory buttons
 * - Clickable regions (lines)
 * - Tooltips
 * - Scrolling
 * - Toggle menus
 * - Line count control
 */
public abstract class InteractiveHudElement extends BaseHudElement {
    
    protected static final int MENU_PADDING = 50;
    
    private HudButtonManager buttonManager;
    private ClickableRegionManager regionManager;
    
    protected int visibleLineIdx = 1;
    protected int maxVisibleLines = 10;
    
    private final List<VCToggleMenu> toggleMenus = new ArrayList<>();
    
    private ClickableRegionManager lineCountRegionManager;
    
    protected InteractiveHudElement(String hudKey, String displayName,
                                   int defaultX, int defaultY, int defaultSize, int defaultColor,
                                   boolean defaultOutline, boolean defaultBg) {
        super(hudKey, displayName, defaultX, defaultY, defaultSize, defaultColor, defaultOutline, defaultBg);
        this.regionManager = new ClickableRegionManager();
        this.lineCountRegionManager = new ClickableRegionManager();
        loadMaxVisibleLines();
    }
    
    /** 
     * Set up buttons when inventory is open
     */
    protected void setupButtons(HudButtonManager manager, HudElementState state) {}
    
    /**
     * Set up clickable regions during render
     */
    protected void setupClickableRegions(ClickableRegionManager manager, HudElementState state) {}
    
    /**
     * Get config key for max visible lines setting.
     * Return null to disable line count control.
     */
    protected abstract String getMaxLinesConfigKey();
    
    /**
     * Get total number of lines available for display.
     * Default implementation returns the display lines count.
     */
    protected int getTotalLineCount() {
        var state = getCachedState();
        List<Text> lines = getDisplayLines(state);
        return lines != null ? lines.size() : 0;
    }
    
    /**
     * Load max visible lines from config
     */
    protected void loadMaxVisibleLines() {
        String key = getMaxLinesConfigKey();
        if (key != null) {
            maxVisibleLines = FishyConfig.getInt(key, 10);
        }
    }
    
    /**
     * Save max visible lines to config
     */
    protected void saveMaxVisibleLines() {
        String key = getMaxLinesConfigKey();
        if (key != null) {
            FishyConfig.setInt(key, maxVisibleLines);
        }
    }
    
    /**
     * Register a toggle menu to be managed by this element
     */
    protected void registerToggleMenu(VCToggleMenu menu) {
        toggleMenus.add(menu);
    }
    
    /**
     * Clear all toggle menus
     */
    protected void clearToggleMenus() {
        toggleMenus.clear();
    }
    
    /**
     * Update menu positions on cache refresh
     */
    protected void updateMenuPositions() {
        int height = MinecraftClient.getInstance().getWindow().getScaledHeight();
        float scale = getHudSize() / 12.0F;
        int verticalSpace = height - getHudY() - MENU_PADDING;
        
        for (VCToggleMenu menu : toggleMenus) {
            menu.setMaxVisibleEntries(scale, verticalSpace);
        }
    }
    
    /**
     * Check if any toggle menu is visible
     */
    protected boolean isAnyMenuVisible() {
        return toggleMenus.stream().anyMatch(VCToggleMenu::isVisible);
    }
    
    /**
     * Check if mouse is over any menu
     */
    protected boolean isMouseOverAnyMenu(double mouseX, double mouseY) {
        return toggleMenus.stream().anyMatch(menu -> menu.isMouseOver(mouseX, mouseY));
    }
    
    /**
     * Handle mouse click on menus
     */
    protected boolean handleMenuClick(Click click, float scale) {
        for (VCToggleMenu menu : toggleMenus) {
            if (menu.isVisible() && menu.mouseClicked(click, false, scale)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Handle mouse scroll on menus
     */
    protected boolean handleMenuScroll(double mouseX, double mouseY, double amount) {
        for (VCToggleMenu menu : toggleMenus) {
            if (menu.isMouseOver(mouseX, mouseY)) {
                return menu.mouseScrolled(amount);
            }
        }
        return false;
    }
    
    /**
     * Handle scrolling of display lines
     */
    protected boolean handleLineScroll(double mouseX, double mouseY, double scrollAmount) {
        if (!isMouseOver((int)mouseX, (int)mouseY)) {
            return false;
        }
        
        int totalLines = getTotalLineCount();
        int maxStartIdx = Math.max(1, totalLines - maxVisibleLines + 1);
        
        if (scrollAmount > 0) {
            visibleLineIdx = Math.max(1, visibleLineIdx - 1);
            return true;
        } else if (scrollAmount < 0) {
            visibleLineIdx = Math.min(maxStartIdx, visibleLineIdx + 1);
            return true;
        }
        return false;
    }
    
    /**
     * Override to provide tooltips for any line.
     * Default implementation returns line tooltip if line is clickable, otherwise empty.
     */
    protected List<Text> getTooltipForLine(int lineIndex, Text line) {
        // Default: use clickable line tooltips
        if (isLineClickable(lineIndex, line)) {
            return getLineTooltip(lineIndex, line);
        }
        return List.of();
    }


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
            
            boolean clickable = isLineClickable(i, line);
            List<Text> tooltip = getTooltipForLine(i, line);
            boolean hasTooltip = tooltip != null && !tooltip.isEmpty();
            
            if (clickable || hasTooltip) {
                int lineWidth = mc.textRenderer.getWidth(line);
                int scaledY = (int)(state.y + i * size * scale);
                
                var region = regionManager.addLine(
                    state.x, scaledY, 
                    (int)(lineWidth * scale), 
                    (int)(size * scale), 
                    line, 
                    clickable ? this::handleLineClick : null
                );
                
                if (hasTooltip) {
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
        
        var mc = MinecraftClient.getInstance();
        var state = getCachedState();
        float scale = state.size / 12.0F;

        if (shouldShowButtons(mc) && getMaxLinesConfigKey() != null
            && lineCountRegionManager.handleClick(click.x(), click.y(), click.button())) {
            return true;
        }
        
        if (handleMenuClick(click, scale)) return true;
        
        if (shouldShowButtons(mc) && buttonManager != null
            && buttonManager.handleClick(click.x(), click.y(), click.button())) {
            return true;
        }
        
        return regionManager.handleClick(click.x(), click.y(), click.button());
    }
    
    /**
     * Render line count control
     */
    protected void renderLineCountControl(DrawContext context, MinecraftClient mc, HudElementState state, int mouseX, int mouseY) {
        if (getMaxLinesConfigKey() == null || buttonManager == null) return;
        
        var drawer = new HudDrawer(mc, context, state);
        var regions = buttonManager.renderLineCountControl(
            mc, drawer, calculateContentWidth(mc), state.color, 
            maxVisibleLines, mouseX, mouseY
        );
        
        if (regions != null) {

            lineCountRegionManager.clear();
            lineCountRegionManager.addRegion(
                regions.minusX, regions.minusY, regions.minusWidth, regions.minusHeight,
                -1, this::handleLineCountChange
            );
            lineCountRegionManager.addRegion(
                regions.plusX, regions.plusY, regions.plusWidth, regions.plusHeight,
                1, this::handleLineCountChange
            );
        }
    }
    
    /**
     * Handle line count change
     */
    private void handleLineCountChange(Integer delta) {
        maxVisibleLines = Math.clamp(maxVisibleLines + (long)delta, 1, 20);
        saveMaxVisibleLines();
        visibleLineIdx = 1; // Reset scroll position
        invalidateCache();
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
    
    /**
     * Check if element or its interactive components are currently hovered.
     */
    public boolean isHovered(double mouseX, double mouseY) {

        if (isAnyMenuVisible() && isMouseOverAnyMenu(mouseX, mouseY)) {
            return true;
        }
        
        var mc = MinecraftClient.getInstance();
        var state = getCachedState();
        
        int padding = 5;
        var bounds = getBounds(mc);
        
        int minX = bounds.x - padding;
        int maxX = bounds.x + bounds.width + padding;
        int minY = bounds.y;
        int maxY = bounds.y + bounds.height + padding;
        
        if (shouldShowButtons(mc) && buttonManager != null && buttonManager.size() > 0) {
            float scale = state.size / 12.0F;
            int buttonAreaY = state.y - (int)(21 * scale);
            int buttonAreaWidth = buttonManager.getTotalWidth();
            
            minY = Math.min(minY, buttonAreaY - padding);
            maxX = Math.max(maxX, state.x + buttonAreaWidth + padding);
            
            if (getMaxLinesConfigKey() != null) {
                int buttonSpacing = (int)(2 * scale);
                int extraWidth = (int)(80 * scale); // Extra width
                maxX = Math.max(maxX, state.x + buttonAreaWidth + buttonSpacing * 2 + extraWidth);
            }
        }
        
        return mouseX > minX && mouseX < maxX && mouseY > minY && mouseY < maxY;
    }
    
    @Override
    public void onCacheRefresh() {
        super.onCacheRefresh();
        updateMenuPositions();
    }
    
    @Override
    protected final void postRender(DrawContext context, MinecraftClient mc, HudElementState state, int mouseX, int mouseY) {

        if (!isEditingMode() && shouldShowButtons(mc)) {
            renderButtons(context, mc, state);
            renderLineCountControl(context, mc, state, mouseX, mouseY);
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
