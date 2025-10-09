package me.valkeea.fishyaddons.ui.widget.dropdown;

import java.util.List;
import java.util.function.Supplier;

import me.valkeea.fishyaddons.tool.FishyMode;
import me.valkeea.fishyaddons.ui.VCText;
import me.valkeea.fishyaddons.ui.widget.VCVisuals;
import me.valkeea.fishyaddons.util.text.Color;
import me.valkeea.fishyaddons.util.text.TextUtils;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class VCToggleMenu {
    private final Supplier<List<ToggleMenuItem>> itemSupplier;

    private final int baseX;
    private final int baseY;
    private final int baseWidth;
    private final int baseEntryHeight;
    private final Runnable onRefresh;

    private boolean visible = true;
    private boolean isDraggingScrollbar = false;
    private double scrollbarThumbOffset = 0.0;

    private int hoveredIndex = -1;    
    private int scrollOffset = 0;
    private int scaledX;
    private int scaledY;
    private int scaledWidth;
    private int scaledEntryHeight;
    
    private static final int MAX_VISIBLE_ENTRIES = 6;

    public VCToggleMenu(Supplier<List<ToggleMenuItem>> itemSupplier, int x, int y, int width, int entryHeight, Runnable onRefresh) {
        this.itemSupplier = itemSupplier;
        this.baseX = x;
        this.baseY = y;
        this.baseWidth = width;
        this.baseEntryHeight = entryHeight;
        this.onRefresh = onRefresh;
    }

    public void render(DrawContext context, Screen screen, int mouseX, int mouseY, float uiScale) {
        if (!visible) return;

        scaledX = baseX;
        scaledY = baseY;
        scaledEntryHeight = (int)(baseEntryHeight * uiScale);
        int scaledPadding = Math.max(1, (int)(6 * uiScale));

        List<ToggleMenuItem> items = itemSupplier.get();
        if (items.isEmpty()) return;

        TextRenderer textRenderer = screen.getTextRenderer();
        int totalEntries = items.size();
        int visibleEntries = Math.min(totalEntries, MAX_VISIBLE_ENTRIES);

        for (ToggleMenuItem item : items) {
            String displayName = TextUtils.stripColor(item.getDisplayName() + "[✓]");
            scaledWidth = Math.max(scaledWidth, (int)(textRenderer.getWidth(displayName) * uiScale) + scaledPadding * 2);
        }

        int maxOffset = Math.max(0, totalEntries - visibleEntries);
        if (scrollOffset > maxOffset) scrollOffset = maxOffset;
        if (scrollOffset < 0) scrollOffset = 0;

        hoveredIndex = -1;
        int currentY = scaledY;

        for (int i = 0; i < visibleEntries; i++) {
            int itemIndex = i + scrollOffset;
            if (itemIndex >= items.size()) break;
            
            ToggleMenuItem item = items.get(itemIndex);
            boolean hovered = mouseX >= scaledX && mouseX <= scaledX + scaledWidth && 
                             mouseY >= currentY && mouseY <= currentY + scaledEntryHeight;

            renderItem(context, textRenderer, item, currentY, hovered, uiScale);

            if (hovered) hoveredIndex = itemIndex;
            currentY += scaledEntryHeight;
        }

        if (totalEntries > MAX_VISIBLE_ENTRIES) {
            renderScrollable(context, textRenderer, uiScale, totalEntries, visibleEntries);
        }
    }

    private void renderItem(DrawContext context, TextRenderer textRenderer, ToggleMenuItem item, int currentY, boolean hovered, float uiScale) {
        int scaledPadding = Math.max(1, (int)(6 * uiScale));
        int textPadding = Math.max(1, (int)(2 * uiScale));        
        int themeColor = FishyMode.getThemeColor();
        int hoverColor = Color.brighten(themeColor, 0.3f);
        int bgColor = hovered ? hoverColor : 0xEE121212;
        int textColor = hovered ? 0xFFFFFFFF : themeColor;
        var name = Text.literal(item.getDisplayName());
        var displayText = name.append(item.isEnabled() ? item.getEnabledSuffix() : item.getDisabledSuffix());            

        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 500);

        context.fill(scaledX, currentY, scaledX + scaledWidth, currentY + scaledEntryHeight, bgColor);

        VCText.drawScaledText(
            context, textRenderer, displayText, 
            scaledX + scaledPadding, currentY + textPadding, textColor, uiScale
        );

        context.getMatrices().pop();
    }

    private void renderScrollable(DrawContext context, TextRenderer textRenderer, float uiScale, int totalEntries, int visibleEntries) {
        int scaledPadding = Math.max(1, (int)(6 * uiScale));
        int textPadding = Math.max(1, (int)(2 * uiScale));        
        int menuHeight = visibleEntries * scaledEntryHeight;
        int scrollbarX = scaledX + scaledWidth - (int)(6 * uiScale);
        int scrollbarWidth = (int)(6 * uiScale);
        int thumbHeight = Math.max((int)(8 * uiScale), (visibleEntries * menuHeight) / totalEntries);
        int thumbY = scaledY + (scrollOffset * (menuHeight - thumbHeight)) / (totalEntries - visibleEntries);
            
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 600);
        context.fill(scrollbarX, scaledY, scrollbarX + scrollbarWidth, scaledY + menuHeight, 0x44000000);
            
        context.fill(scrollbarX + 1, thumbY, scrollbarX + scrollbarWidth - 1, thumbY + thumbHeight, VCVisuals.getThemeColor());
            
        context.fill(scaledX, scaledY + menuHeight, scaledX + scaledWidth, scaledY + menuHeight + scaledEntryHeight, 0xEE121212);
        VCText.drawScaledText(
            context, textRenderer, Text.literal(". . ."), 
            scaledX + scaledPadding, scaledY + menuHeight + textPadding, 0xFFBBAACC, uiScale
        );
        context.getMatrices().pop();
    }

    public boolean mouseClicked(double mouseX, double mouseY, float uiScale) {
        if (!visible) return false;

        List<ToggleMenuItem> items = itemSupplier.get();
        int totalEntries = items.size();
        int visibleEntries = Math.min(totalEntries, MAX_VISIBLE_ENTRIES);

        if (totalEntries > visibleEntries) {
            int menuHeight = visibleEntries * scaledEntryHeight;
            int scrollbarX = scaledX + scaledWidth - (int)(6 * uiScale);
            int scrollbarWidth = (int)(6 * uiScale);
            if (mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth && 
                mouseY >= scaledY && mouseY <= scaledY + menuHeight) {
                
                // Calculate where within the thumb the user clicked
                int thumbHeight = Math.max((int)(8 * uiScale), (visibleEntries * menuHeight) / totalEntries);
                int thumbY = scaledY + (scrollOffset * (menuHeight - thumbHeight)) / (totalEntries - visibleEntries);
                scrollbarThumbOffset = mouseY - thumbY;
                
                isDraggingScrollbar = true;
                return true;
            }
        }

        int entryTop = scaledY;
        for (int i = 0; i < visibleEntries; i++) {
            int itemIndex = i + scrollOffset;
            if (itemIndex >= items.size()) break;
            
            int entryBottom = entryTop + scaledEntryHeight;
            if (mouseX >= scaledX && mouseX <= scaledX + scaledWidth && mouseY >= entryTop && mouseY <= entryBottom) {
                ToggleMenuItem item = items.get(itemIndex);
                item.toggle();
                
                if (onRefresh != null) {
                    onRefresh.run();
                }
                return true;
            }
            entryTop = entryBottom;
        }

        return false;
    }

    public boolean mouseScrolled(double amount) {
        if (!visible) return false;
        
        List<ToggleMenuItem> items = itemSupplier.get();
        int totalEntries = items.size();
        int visibleEntries = Math.min(totalEntries, MAX_VISIBLE_ENTRIES);
        
        if (totalEntries > visibleEntries) {
            scrollOffset -= (int) amount;
            int maxOffset = totalEntries - visibleEntries;
            if (scrollOffset < 0) scrollOffset = 0;
            if (scrollOffset > maxOffset) scrollOffset = maxOffset;
            return true;
        }
        return false;
    }

    public boolean mouseDragged(double mouseY, float uiScale) {
        if (!visible || !isDraggingScrollbar) return false;

        List<ToggleMenuItem> items = itemSupplier.get();
        int totalEntries = items.size();
        int visibleEntries = Math.min(totalEntries, MAX_VISIBLE_ENTRIES);
        int menuHeight = visibleEntries * scaledEntryHeight;
        int maxScroll = Math.max(0, totalEntries - visibleEntries);
        int thumbHeight = Math.max((int)(8 * uiScale), (visibleEntries * menuHeight) / totalEntries);

        double thumbTopY = mouseY - scaledY - scrollbarThumbOffset;
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

    public void setVisible(boolean visible) { 
        this.visible = visible; 
    }

    public int getX() { return baseX; }
    public int getY() { return baseY; }
    public int getWidth() { return baseWidth; }
    public int getEntryHeight() { return baseEntryHeight;  }
    public int getMaxVisibleEntries() { return MAX_VISIBLE_ENTRIES; }
    public int getScrollOffset() { return scrollOffset; }
    public int getHoveredIndex() { return hoveredIndex;  }
    public boolean isVisible() {  return visible; }    
}