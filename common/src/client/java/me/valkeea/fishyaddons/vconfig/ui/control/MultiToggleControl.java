package me.valkeea.fishyaddons.vconfig.ui.control;

import java.util.List;
import java.util.function.Supplier;

import me.valkeea.fishyaddons.vconfig.ui.layout.Dimensions;
import me.valkeea.fishyaddons.vconfig.ui.model.Bounds;
import me.valkeea.fishyaddons.vconfig.ui.model.ClickContext;
import me.valkeea.fishyaddons.vconfig.ui.model.DragContext;
import me.valkeea.fishyaddons.vconfig.ui.render.VCRenderContext;
import me.valkeea.fishyaddons.vconfig.ui.render.VCText;
import me.valkeea.fishyaddons.vconfig.ui.widget.VCButton;
import me.valkeea.fishyaddons.vconfig.ui.widget.dropdown.VCToggleMenu;
import me.valkeea.fishyaddons.vconfig.ui.widget.dropdown.item.ToggleMenuItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyInput;

public class MultiToggleControl extends AbstractUIControl {
    private static final int BASE_WIDTH = 60;
    private static final int BASE_HEIGHT = Dimensions.BUTTON_H;
    
    private final Supplier<List<ToggleMenuItem>> menuProvider;
    private final String buttonText;
    
    private VCToggleMenu activeDropdown = null;
    
    /**
     * Create a dropdown with toggleable items.
     * 
     * @param menuProvider Supplier to provide menu items when dropdown is clicked
     * @param buttonText Text to display on the button
     * @param onRefresh Callback when dropdown items are selected
     */
    public MultiToggleControl(
        Supplier<List<ToggleMenuItem>> menuProvider,
        String buttonText,
        String[] tooltip
    ) {
        this.menuProvider = menuProvider;
        this.buttonText = buttonText;
        this.tooltip = tooltip;
    }
    
    @Override
    public void render(VCRenderContext ctx, int x, int y) {

        int w = calculateWidth(ctx);
        int h = getPreferredHeight();
        
        this.lastBounds = new Bounds(x, y, w, h);
        
        VCButton.render(
            ctx.context,
            ctx.textRenderer,
            new VCButton.ButtonConfig(x, y, w, h, buttonText)
                .withHovered(hovered)
        );
    }
    
    private void openDropdown() {
        if (lastBounds == null) return;
        
        List<ToggleMenuItem> items = menuProvider.get();
        if (items.isEmpty()) return;
        
        int dropdownX = lastBounds.x;
        int dropdownY = lastBounds.y + lastBounds.height;
        
        activeDropdown = new VCToggleMenu(
            menuProvider,
            dropdownX,
            dropdownY,
            BASE_WIDTH,
            BASE_HEIGHT,
            null
        );
        
        var mc = MinecraftClient.getInstance();
        int screenH = mc.getWindow().getHeight();
        int available = screenH - lastBounds.y - BASE_HEIGHT * 2;      
        activeDropdown.setMaxVisibleEntries(Dimensions.SCALE, available);
    }
    
    @Override
    public boolean renderOverlay(VCRenderContext ctx, int scrollX, int endY) {
        if (activeDropdown != null && activeDropdown.isVisible()) {
            var screen = MinecraftClient.getInstance().currentScreen;
            if (screen != null) {
                activeDropdown.render(ctx.context, screen, ctx.mouseX, ctx.mouseY, Dimensions.SCALE);
                return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean hasActiveOverlay() {
        return activeDropdown != null && activeDropdown.isVisible();
    }

    @Override
    public boolean handleClick(ClickContext ctx, int x, int y) {

        if (activeDropdown != null) {
            var handled = activeDropdown.mouseClicked(ctx.click, ctx.doubled, ctx.uiScale);
            if (!handled || !activeDropdown.isVisible()) {
                activeDropdown = null;
            }
            return true;
        }
        
        if (lastBounds == null || !ctx.isLeftClick()) return false;
        if (!lastBounds.contains(ctx.mouseX, ctx.mouseY)) return false;
        
        openDropdown();
        return true;
    }    
    
    @Override
    public boolean handleOverlayScroll(double amount) {
        if (activeDropdown != null && activeDropdown.isVisible()) {
            return activeDropdown.mouseScrolled(amount);
        }
        return false;
    }
    
    @Override
    public boolean handleKeyPress(KeyInput input) {
        if (activeDropdown != null && activeDropdown.isVisible() && input.getKeycode() == 256) {
            activeDropdown.setVisible(false);
            activeDropdown = null;
            return true;
        }
        return false;
    }

    @Override
    public boolean handleDrag(DragContext ctx, int x, int y) {
        if (activeDropdown != null) {
            return activeDropdown.mouseDragged(ctx.click, ctx.uiScale);
        }
        return false;
    }

    @Override
    public boolean handleMouseRelease(ClickContext ctx) {
        if (activeDropdown != null) {
            return activeDropdown.mouseReleased();
        }
        return false;
    }

    private int calculateWidth(VCRenderContext ctx) {
        return Dimensions.getCustomButtonW(
            VCText.getWidthWithPadding(
                ctx.textRenderer,
                buttonText
            )
        );
    }    

    @Override
    public Bounds getBounds() {
        return lastBounds;
    }

    @Override
    public boolean isHovered(double mouseX, double mouseY) {
        boolean anyHovered = hovered;
        if (hasActiveOverlay()) {
            anyHovered = activeDropdown.isMouseOver(mouseX, mouseY);
        }
        return anyHovered;
    }
}
