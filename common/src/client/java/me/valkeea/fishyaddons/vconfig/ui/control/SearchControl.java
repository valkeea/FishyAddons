package me.valkeea.fishyaddons.vconfig.ui.control;

import java.util.List;
import java.util.function.Supplier;

import me.valkeea.fishyaddons.vconfig.binding.ConfigBinding.StringBinding;
import me.valkeea.fishyaddons.vconfig.ui.layout.Dimensions;
import me.valkeea.fishyaddons.vconfig.ui.model.Bounds;
import me.valkeea.fishyaddons.vconfig.ui.model.ClickContext;
import me.valkeea.fishyaddons.vconfig.ui.model.DragContext;
import me.valkeea.fishyaddons.vconfig.ui.render.RenderUtils;
import me.valkeea.fishyaddons.vconfig.ui.render.VCRenderContext;
import me.valkeea.fishyaddons.vconfig.ui.widget.VCTextField;
import me.valkeea.fishyaddons.vconfig.ui.widget.dropdown.VCSearchDropdown;
import me.valkeea.fishyaddons.vconfig.ui.widget.dropdown.item.ToggleMenuItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

public class SearchControl extends AbstractUIControl {
    private static final int FIELD_WIDTH = Dimensions.FIELD_W;
    private static final int BASE_HEIGHT = Dimensions.CONTROL_H;
    
    private final StringBinding fieldBinding;
    private final Supplier<List<ToggleMenuItem>> itemSupplier;
    private String cachedFieldValue;
    
    private VCTextField field;
    private Bounds fieldBounds;
    private VCSearchDropdown activeDropdown = null;
    
    /**
     * Create a dropdown of value options that filters based on a search field.
     * 
     * @param fieldBinding {@link StringBinding} for the config value
     * @param itemSupplier Supplier for dropdown menu items
     * @param tooltip Tooltip text for the control
     */
    public SearchControl(
        StringBinding fieldBinding,
        Supplier<List<ToggleMenuItem>> itemSupplier,
        String[] tooltip
    ) {
        this.fieldBinding = fieldBinding;
        this.itemSupplier = itemSupplier;
        this.tooltip = tooltip;
    }
    
    @Override
    public void render(VCRenderContext context, int x, int y) {
        if (field == null) createField(context, x, y);

        field.setPosition(x, y);
        field.setWidth(FIELD_WIDTH);
        field.setHeight(BASE_HEIGHT);

        if (!field.isFocused() && cachedValueDirty) {
            cachedFieldValue = fieldBinding.get();
            markCacheFresh();
            if (!field.getText().equals(cachedFieldValue)) {
                field.setText(cachedFieldValue);
            }
        }
        
        field.render(context.context, context.mouseX, context.mouseY, 0);
        
        fieldBounds = new Bounds(x, y, FIELD_WIDTH, BASE_HEIGHT);
        lastBounds = fieldBounds;
    }
    
    @Override
    public boolean handleClick(ClickContext ctx, int x, int y) {

        if (activeDropdown != null) {

            var handled = activeDropdown.mouseClicked(ctx.click, ctx.doubled, ctx.uiScale);   
            if (!handled || !activeDropdown.isVisible()) {
                activeDropdown = null;
                field.setFocused(false);
            }
            return true;
        }
        
        if (fieldBounds != null && fieldBounds.contains(ctx.click.x(), ctx.click.y())) {

            var handled = field.mouseClicked(ctx.click, false);
            if (handled && field.isFocused()) {
                openDropdown(ctx);
            }
            return handled;
        }
        
        return false;
    }
    
    private void openDropdown(ClickContext ctx) {
        if (fieldBounds == null) return;
        
        List<ToggleMenuItem> items = itemSupplier.get();
        if (items.isEmpty()) return;
        
        int dropdownX = fieldBounds.x;
        int dropdownY = fieldBounds.y + fieldBounds.height;
        
        activeDropdown = VCSearchDropdown.create(
            itemSupplier,
            dropdownX,
            dropdownY,
            (int)(FIELD_WIDTH * (fieldBounds.width / (float)FIELD_WIDTH)),
            BASE_HEIGHT,
            field
        );
        
        var mc = MinecraftClient.getInstance();
        int screenH = (int) Math.floor(mc.getWindow().getHeight() / ctx.uiScale);
        int available = screenH - dropdownY - BASE_HEIGHT * 2;
        activeDropdown.setMaxVisibleEntries(Dimensions.SCALE, available);
    }

    @Override
    public boolean handleKeyPress(KeyInput input) {
        if (field != null && field.isFocused()) {
            boolean handled = field.keyPressed(input);
            if (handled) {
                cachedFieldValue = field.getText();
                fieldBinding.set(cachedFieldValue);
            }
            return handled;
        }
        return false;
    }

    @Override
    public boolean handleCharInput(CharInput input) {
        if (field != null && field.isFocused()) {
            boolean handled = field.charTyped(input);
            if (handled) {
                cachedFieldValue = field.getText();
                fieldBinding.set(cachedFieldValue);
            }
            return handled;
        }
        return false;
    }

    @Override
    public boolean handleDrag(DragContext ctx, int x, int y) {
        if (field != null && field.isFocused()) {
            return field.mouseDragged(ctx.click, ctx.offsetX, ctx.offsetY);
        }
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
    
    @Override
    public boolean renderOverlay(VCRenderContext ctx, int scrollX, int endY) {
        
        if (activeDropdown != null && activeDropdown.isVisible()) {
            var screen = MinecraftClient.getInstance().currentScreen;
            if (screen != null) {
                activeDropdown.render(ctx.context, screen, ctx.mouseX, ctx.mouseY, Dimensions.SCALE);
                return true;
            }

        } else if (field != null && field.isHovered()) {

            var text = field.getText();
            if (text != null && !text.isEmpty()) {

                RenderUtils.preview(
                    ctx.context, ctx.textRenderer,
                    List.of(Text.literal(text)
                    .styled(s -> s.withColor(ctx.themeColor))),
                    ctx.mouseX, ctx.mouseY,
                    ctx.themeColor,
                    Dimensions.SCALE
                );
            }
            return true;
        }

        return false;
    }
    
    @Override
    public boolean hasActiveOverlay() {
        return activeDropdown != null && activeDropdown.isVisible();
    }
    
    @Override
    public boolean handleOverlayScroll(double amount) {
        if (activeDropdown != null && activeDropdown.isVisible()) {
            return activeDropdown.mouseScrolled(amount);
        }
        return false;
    }
    
    @Override
    public int getPreferredWidth() {
        return FIELD_WIDTH;
    }
    
    @Override
    public int getPreferredHeight() {
        return BASE_HEIGHT;
    }

    @Override
    public boolean isHovered(double mouseX, double mouseY) {
        boolean anyHovered = hovered;
        if (hasActiveOverlay()) {
            anyHovered = activeDropdown.isMouseOver(mouseX, mouseY);
        }
        return anyHovered;
    }    
    
    private void createField(VCRenderContext context, int x, int y) {
        if (cachedValueDirty) {
            cachedFieldValue = fieldBinding.get();
            markCacheFresh();
        }
        
        field = new VCTextField(
            context.textRenderer,
            x, y, FIELD_WIDTH, BASE_HEIGHT,
            Text.literal(cachedFieldValue)
        );
        
        field.setUIScale(Dimensions.SCALE);
        field.setText(cachedFieldValue);
        field.setChangedListener(text -> {
            cachedFieldValue = text;
            fieldBinding.set(text);
        });
    }
}
