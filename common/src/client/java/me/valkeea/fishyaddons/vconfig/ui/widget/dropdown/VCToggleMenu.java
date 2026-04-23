package me.valkeea.fishyaddons.vconfig.ui.widget.dropdown;

import java.awt.Rectangle;
import java.util.List;
import java.util.function.Supplier;

import me.valkeea.fishyaddons.tool.FishyMode;
import me.valkeea.fishyaddons.util.text.Color;
import me.valkeea.fishyaddons.util.text.TextUtils;
import me.valkeea.fishyaddons.vconfig.ui.render.VCText;
import me.valkeea.fishyaddons.vconfig.ui.widget.VCVisuals;
import me.valkeea.fishyaddons.vconfig.ui.widget.dropdown.item.ToggleMenuItem;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

@SuppressWarnings("squid:S107")
public class VCToggleMenu {
    private final Supplier<List<ToggleMenuItem>> itemSupplier;

    private int menuX;
    private int menuY;
    private final int baseWidth;
    private final int baseEntryHeight;
    private final Runnable onRefresh;

    protected boolean visible = true;
    private boolean isDraggingScrollbar = false;
    private double scrollKnobOffset = 0.0;

    private int hoveredIndex = -1;    
    private int scrollOffset = 0;
    private long lastHoverTime = 0;
    private int lastHoveredIndex = -1;
    private int scaledWidth;
    private int scaledEntryHeight;
    private int scaledPadding;
    private float uiScale;
    
    private int maxVisible = 6;

    public VCToggleMenu(Supplier<List<ToggleMenuItem>> itemSupplier, int x, int y, int width, int entryHeight, Runnable onRefresh) {
        this.itemSupplier = itemSupplier;
        this.menuX = x;
        this.menuY = y;
        this.baseWidth = width;
        this.baseEntryHeight = entryHeight;
        this.onRefresh = onRefresh;
    }

    public void setPosition(int x, int y, int screenHeight) {
        this.menuX = x;
        this.menuY = y;
        setMaxVisibleEntries(uiScale, screenHeight - y - (int)(baseEntryHeight * uiScale));
    }

    public void render(DrawContext context, Screen screen, int mouseX, int mouseY, float scale) {
        if (!visible) return;

        uiScale = scale;
        scaledEntryHeight = (int)(baseEntryHeight * uiScale);
        scaledPadding = Math.max(1, (int)(6 * uiScale));
        
        scaledWidth = 0;

        var items = itemSupplier.get();
        if (items.isEmpty()) return;

        var textRenderer = screen.getTextRenderer();
        int totalEntries = items.size();
        int visibleEntries = Math.min(totalEntries, maxVisible);

        if (!getSearchText().isEmpty()) {
            var search = getSearchText().toLowerCase();
            items = items.stream()
                .filter(item -> item.getId().contains(search))
                .toList();
            totalEntries = items.size();
            visibleEntries = Math.min(totalEntries, maxVisible);
        }
        
        if (items.isEmpty()) return;

        for (var item : items) {
            if (!item.useFixedWidth()) {
                String displayName = TextUtils.stripColor(item.getDisplayName() + "[✓]");
                scaledWidth = Math.max(scaledWidth, (int)(textRenderer.getWidth(displayName) * uiScale) + scaledPadding * 2);
            }
        }
        
        if (scaledWidth == 0) scaledWidth = baseWidth;

        int maxOffset = Math.max(0, totalEntries - visibleEntries);
        if (scrollOffset > maxOffset) scrollOffset = maxOffset;
        if (scrollOffset < 0) scrollOffset = 0;

        renderItems(context, textRenderer, mouseX, mouseY, items, visibleEntries);

        if (totalEntries > maxVisible) {
            renderScrollable(context, textRenderer, totalEntries, visibleEntries);
        }
    }

    private void renderItems(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY, List<ToggleMenuItem> items, int visibleEntries) {

        int previousHoveredIndex = hoveredIndex;
        hoveredIndex = -1;
        int currentY = menuY;
        long currentTime = System.currentTimeMillis();

        for (int i = 0; i < visibleEntries; i++) {
            int itemIndex = i + scrollOffset;
            if (itemIndex >= items.size()) break;
            
            var item = items.get(itemIndex);
            boolean hovered = mouseX >= menuX && mouseX <= menuX + scaledWidth && 
                             mouseY >= currentY && mouseY <= currentY + scaledEntryHeight;

            if (hovered) hoveredIndex = itemIndex;
            
            if (itemIndex != previousHoveredIndex) {
                if (itemIndex == hoveredIndex) {
                    lastHoverTime = currentTime;
                    lastHoveredIndex = itemIndex;
                }
            } else if (itemIndex == hoveredIndex) {
                lastHoveredIndex = itemIndex;
            }

            renderItem(context, textRenderer, item, currentY, hovered, itemIndex, currentTime);
            currentY += scaledEntryHeight;
        }

        if (hoveredIndex == -1 && previousHoveredIndex != -1) lastHoveredIndex = -1;
    }

    private void renderItem(DrawContext context, TextRenderer textRenderer, ToggleMenuItem item,
        int currentY, boolean hovered, int itemIndex, long currentTime) {
       
        int themeColor = FishyMode.getThemeColor();
        int hoverColor = Color.mulRGB(themeColor, 0.3f);
        int bgColor = hovered ? hoverColor : 0xEE121212;
        int textColor = hovered ? 0xFFFFFFFF : themeColor;
        var name = Text.literal(item.getDisplayName());
        var displayText = name.append(item.isEnabled() ? item.getEnabledSuffix() : item.getDisabledSuffix());            

        context.getMatrices().pushMatrix();
        context.fill(menuX, currentY, menuX + scaledWidth, currentY + scaledEntryHeight, bgColor);

        if (item.useFixedWidth()) {
            animateOverflow(context, textRenderer, item, displayText, textColor, currentY, hovered, itemIndex, currentTime);
        } else {
            VCText.flatText(
                context, textRenderer, displayText, 
                menuX + scaledPadding, currentY + Math.max(1, (int)(2 * uiScale)), textColor
            );
        }

        context.getMatrices().popMatrix();
    }

    private void animateOverflow(DrawContext context, TextRenderer textRenderer, ToggleMenuItem item, Text displayText, int textColor, int currentY, boolean hovered, int itemIndex, long currentTime) {
        int availableWidth = scaledWidth - (scaledPadding * 2);
        String fullText = TextUtils.stripColor(item.getDisplayName() + (item.isEnabled() ? "[✓]" : "[✗]"));
        int textWidth = (int)(textRenderer.getWidth(fullText) * uiScale);
        
        int offset = 0;
        if (textWidth > availableWidth && hovered && itemIndex == lastHoveredIndex) {
            // Animate horizontal scroll after 350ms hover
            long hoverDuration = currentTime - lastHoverTime;
            if (hoverDuration > 350) {
                int maxScroll = textWidth - availableWidth;
                // Oscillate scroll back and forth every 2 seconds
                long cycleTime = 2000;
                long animTime = (hoverDuration - 350) % (cycleTime * 2);
                
                if (animTime < cycleTime) {
                    offset = (int)((animTime * maxScroll) / cycleTime);
                } else {
                    offset = (int)(((cycleTime * 2 - animTime) * maxScroll) / cycleTime);
                }
            }
        }
        
        context.enableScissor(menuX + scaledPadding, currentY, menuX + scaledWidth - scaledPadding, currentY + scaledEntryHeight);
        VCText.flatText(
            context, textRenderer, displayText, 
            menuX + scaledPadding - offset, currentY + Math.max(1, (int)(2 * uiScale)), textColor
        );
        context.disableScissor();
    }

    private void renderScrollable(DrawContext context, TextRenderer textRenderer, int totalEntries, int visibleEntries) {

        int textPadding = Math.max(1, (int)(2 * uiScale));        
        int menuHeight = visibleEntries * scaledEntryHeight;
        int scrollbarX = menuX + scaledWidth - (int)(6 * uiScale);
        int scrollbarWidth = (int)(6 * uiScale);
        int thumbHeight = Math.max((int)(8 * uiScale), (visibleEntries * menuHeight) / totalEntries);
        int thumbY = menuY + (scrollOffset * (menuHeight - thumbHeight)) / (totalEntries - visibleEntries);
            
        context.getMatrices().pushMatrix();
        context.fill(scrollbarX, menuY, scrollbarX + scrollbarWidth, menuY + menuHeight, 0x44000000);
            
        context.fill(scrollbarX + 1, thumbY, scrollbarX + scrollbarWidth - 1, thumbY + thumbHeight, VCVisuals.getThemeColor());
            
        context.fill(menuX, menuY + menuHeight, menuX + scaledWidth, menuY + menuHeight + scaledEntryHeight, 0xEE121212);
        VCText.flatText(
            context, textRenderer, Text.literal(". . ."), 
            menuX + scaledPadding, menuY + menuHeight + textPadding, 0xFFBBAACC
        );
        context.getMatrices().popMatrix();
    }

    @SuppressWarnings("unused:parameter")
    public boolean mouseClicked(Click click, boolean doubled, float uiScale) {
        if (!visible) return false;

        var items = itemSupplier.get();
        int totalEntries = items.size();
        int visibleEntries = Math.min(totalEntries, maxVisible);

        if (!getSearchText().isEmpty()) {
            var search = getSearchText().toLowerCase();
            items = items.stream()
                .filter(item -> item.getId().contains(search))
                .toList();
            totalEntries = items.size();
            visibleEntries = Math.min(totalEntries, maxVisible);
        }

        if (totalEntries > visibleEntries) {
            int menuHeight = visibleEntries * scaledEntryHeight;
            int scrollbarX = menuX + scaledWidth - (int)(6 * uiScale);
            int scrollbarWidth = (int)(6 * uiScale);
            if (click.x() >= scrollbarX && click.x() <= scrollbarX + scrollbarWidth && 
                click.y() >= menuY && click.y() <= menuY + menuHeight) {

                int thumbHeight = Math.max((int)(8 * uiScale), (visibleEntries * menuHeight) / totalEntries);
                int thumbY = menuY + (scrollOffset * (menuHeight - thumbHeight)) / (totalEntries - visibleEntries);
                scrollKnobOffset = click.y() - thumbY;
                
                isDraggingScrollbar = true;
                return true;
            }
        }

        int entryTop = menuY;
        for (int i = 0; i < visibleEntries; i++) {
            int itemIndex = i + scrollOffset;
            if (itemIndex >= items.size()) break;
            
            int entryBottom = entryTop + scaledEntryHeight;
            if (click.x() >= menuX && click.x() <= menuX + scaledWidth && click.y() >= entryTop && click.y() <= entryBottom) {

                var item = items.get(itemIndex);
                clickAction(item, click.button() == 1);
                return true;
            }
            entryTop = entryBottom;
        }

        visible = false;
        return false;
    }

    public boolean keyPressed(KeyInput input) {
        if (!visible) return false;

        if (input.key() == 256) {
            visible = false;
            return true;
        }

        return false;
    }

    public boolean mouseScrolled(double amount) {
        if (!visible) return false;
        
        if (amount > 0) scrollOffset--;
        else if (amount < 0) scrollOffset++;

        int totalEntries = itemSupplier.get().size();
        int visibleEntries = Math.min(totalEntries, maxVisible);
        int maxOffset = Math.max(0, totalEntries - visibleEntries);

        if (scrollOffset < 0) scrollOffset = 0;
        if (scrollOffset > maxOffset) scrollOffset = maxOffset;

        return true;
    }

    public boolean mouseDragged(Click click, float uiScale) {
        if (!visible || !isDraggingScrollbar) return false;

        var items = itemSupplier.get();
        int totalEntries = items.size();
        int visibleEntries = Math.min(totalEntries, maxVisible);
        int menuHeight = visibleEntries * scaledEntryHeight;
        int maxScroll = Math.max(0, totalEntries - visibleEntries);
        int thumbHeight = Math.max((int)(8 * uiScale), (visibleEntries * menuHeight) / totalEntries);

        double thumbTopY = click.y() - menuY - scrollKnobOffset;
        thumbTopY = Math.clamp(thumbTopY, 0.0, (double)menuHeight - thumbHeight);

        double scrollPercent = maxScroll > 0 ? thumbTopY / (menuHeight - thumbHeight) : 0;
        int newScrollOffset = (int)(scrollPercent * maxScroll);
        scrollOffset = Math.clamp(newScrollOffset, 0, maxScroll);
        return true;

    }

    public boolean mouseReleased() {
        if (isDraggingScrollbar) {
            isDraggingScrollbar = false;
            return true;
        }
        return false;
    }

    protected void clickAction(ToggleMenuItem item, boolean rightClick) {
        if (rightClick && item.supportsRightClick()) {
            item.onRightClick();
        } else {
            item.toggle();
        }
        onRefresh();
    }

    protected void onRefresh() {
        if (onRefresh != null) onRefresh.run();
    }

    public void setVisible(boolean visible) { 
        this.visible = visible; 
    }

    public void setMaxVisibleEntries(float scale, int verticalSpace) {
        float s = scale > 0 ? scale : 1.0f;
        this.maxVisible = verticalSpace / (int)(baseEntryHeight * s);
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!visible) return false;
        return mouseX >= menuX && mouseX <= menuX + scaledWidth && 
               mouseY >= menuY && mouseY <= menuY + maxVisible * scaledEntryHeight;
    }

    public Rectangle getBounds() {
        return new Rectangle(menuX, menuY, scaledWidth, maxVisible * scaledEntryHeight);
    }

    public int getX() { return menuX; }
    public int getY() { return menuY; }
    public int getWidth() { return scaledWidth; }
    public int getEntryHeight() { return scaledEntryHeight;  }
    public int getAllowedEntries() { return maxVisible; }
    public int getScrollOffset() { return scrollOffset; }
    public int getHoveredIndex() { return hoveredIndex;  }
    public String getSearchText() { return ""; }
    public boolean isVisible() {  return visible; }
    
    protected ToggleMenuItem getItem(int index) {
        return itemSupplier.get().get(index);
    }
}
