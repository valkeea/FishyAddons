package me.valkeea.fishyaddons.ui.widget.dropdown;

import java.util.List;
import java.util.function.Consumer;

import me.valkeea.fishyaddons.tool.FishyMode;
import me.valkeea.fishyaddons.ui.VCRenderUtils;
import me.valkeea.fishyaddons.ui.widget.VCTextField;
import me.valkeea.fishyaddons.util.text.Color;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public class SearchMenu {
    private final List<SearchEntry> allEntries;
    private List<SearchEntry> filteredEntries;
    private final int x, y, width, entryHeight;
    private final Consumer<SearchEntry> onSelect;
    private int hoveredIndex = -1;
    private boolean visible = true;
    private final TextFieldWidget searchField;
    private final boolean usesExternalField;
    private int scrollOffset = 0;
    private static final int MAX_VISIBLE_ENTRIES = 8;

    public SearchMenu(List<SearchEntry> entries, int x, int y, int width, int entryHeight,
        Consumer<SearchEntry> onSelect, Screen screen) {
        this(entries, x, y, width, entryHeight, onSelect, screen, null);
    }

    public SearchMenu(List<SearchEntry> entries, int x, int y, int width, int entryHeight,
        Consumer<SearchEntry> onSelect, Screen screen, VCTextField externalField) {
        this.allEntries = entries;
        this.filteredEntries = entries;
        this.x = x;
        this.y = y + 24;
        this.width = width;
        this.entryHeight = entryHeight;
        this.onSelect = onSelect;
        if (externalField != null) {
            this.searchField = externalField;
            this.usesExternalField = true;
        } else {
            this.searchField = new VCTextField(screen.getTextRenderer(), x, y, width, 15, Text.literal("Search..."));
            this.searchField.setEditableColor(0xFF808080);
            this.searchField.setChangedListener(this::updateFilter);
            this.usesExternalField = false;
        }
    }

    private void updateFilter(String query) {
        filteredEntries = allEntries.stream()
            .filter(e -> e.name.toLowerCase().contains(query.toLowerCase()) || 
                         (e.description != null && e.description.toLowerCase().contains(query.toLowerCase())))
            .toList();
    }

    public void render(DrawContext context, Screen screen, int mouseX, int mouseY, float delta) {
        if (!visible) return;

        if (!searchField.isFocused() && searchField.getText().isEmpty()) {
            searchField.setText(Text.literal("search...")
            .setStyle(Style.EMPTY.withItalic(true).withColor(0xFF808080)).getString());
        
        } else if (searchField.isFocused() && searchField.getText().equals("search...")) {
            searchField.setText("");
        }

        if (!usesExternalField) {
            searchField.render(context, mouseX, mouseY, delta); 
        } 
        if (usesExternalField) {
            updateFilter(searchField.getText());
        }

        if (!searchField.isFocused() && !usesExternalField) return;

        TextRenderer textRenderer = screen.getTextRenderer();

        int totalEntries = filteredEntries.size();
        int visibleEntries = Math.min(totalEntries, MAX_VISIBLE_ENTRIES);

        // Clamp scrollOffset to valid range
        int maxOffset = Math.max(0, totalEntries - visibleEntries);
        if (scrollOffset > maxOffset) scrollOffset = maxOffset;
        if (scrollOffset < 0) scrollOffset = 0;

        // Calculate dynamic entry heights
        int[] entryHeights = new int[visibleEntries];
        int[][] lineCounts = new int[visibleEntries][2]; // [nameLines, descLines]
        for (int i = 0; i < visibleEntries; i++) {
            int entryIndex = i + scrollOffset;
            if (entryIndex >= filteredEntries.size()) break;
            SearchEntry entry = filteredEntries.get(entryIndex);
            int descLines = 0;
            if (entry.description != null && !entry.description.isEmpty()) {
                descLines = entry.description.split("\n").length;
            }
            entryHeights[i] = 14 + descLines * 12 + 4;
            lineCounts[i][0] = 1;
            lineCounts[i][1] = descLines;
        }

        hoveredIndex = -1;
        int currentY = y;

        for (int i = 0; i < visibleEntries; i++) {
            int entryIndex = i + scrollOffset;
            SearchEntry entry = filteredEntries.get(entryIndex);
            int entryH = entryHeights[i];

            boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= currentY && mouseY <= currentY + entryHeight;
            int themeColor = FishyMode.getThemeColor();
            int hoverColor = Color.brighten(themeColor, 0.3f);
            int bgColor = hovered ? hoverColor : 0xEE121212;
            int textColor = hovered ? 0xFFFFFFFF : themeColor;
            String text = entry.displayName != null ? entry.displayName : entry.name;
            
            VCRenderUtils.opaqueGradient(context, x, currentY, width, entryH, bgColor);
            context.drawText(textRenderer, text, x + 6, currentY + 2, textColor, false);

            if (entry.description != null && !entry.description.isEmpty()) {
                String[] lines = entry.description.split("\n");
                int descColor = hovered ? 0xFFEEEEEE : 0xFFCCCCCC;
                for (int l = 0; l < lines.length; l++) {
                    context.drawText(textRenderer, lines[l], x + 8, currentY + 16 + l * 12, descColor, false);
                }
            }

            if (hovered) hoveredIndex = entryIndex;
            currentY += entryH;
        }

        if (totalEntries > MAX_VISIBLE_ENTRIES) {
            context.fill(x, currentY, x + width, currentY + 18, 0xCC222222);
            context.drawText(textRenderer, ". . .", x + 10, currentY + 4, 0xFFBBAACC, false);
        }
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            if (searchField.isFocused()) {
                searchField.setFocused(false);
                return true;
            }
            return false;
        }

        int totalEntries = filteredEntries.size();
        int visibleEntries = Math.min(totalEntries, MAX_VISIBLE_ENTRIES);

        // Clamp to valid range
        int maxOffset = Math.max(0, totalEntries - visibleEntries);
        if (scrollOffset > maxOffset) scrollOffset = maxOffset;
        if (scrollOffset < 0) scrollOffset = 0;

        if (keyCode == 264) {
            if (scrollOffset < maxOffset) {
                scrollOffset++;
                return true;
            }
        } else if (keyCode == 265 && scrollOffset > 0) {
            scrollOffset--;
            return true;
        }
        return searchField.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;

        int totalEntries = filteredEntries.size();
        int visibleEntries = Math.min(totalEntries, MAX_VISIBLE_ENTRIES);
        int[] entryHeights = new int[visibleEntries];
        for (int i = 0; i < visibleEntries; i++) {
            int entryIndex = i + scrollOffset;
            if (entryIndex >= filteredEntries.size()) break;
            SearchEntry entry = filteredEntries.get(entryIndex);
            int descLines = 0;
            if (entry.description != null && !entry.description.isEmpty()) {
                descLines = entry.description.split("\n").length;
            }
            entryHeights[i] = 14 + descLines * 12 + 4;
        }

        int entryTop = y;
        for (int i = 0; i < visibleEntries; i++) {
            int entryIndex = i + scrollOffset;
            int entryH = entryHeights[i];
            int entryBottom = entryTop + entryH;
            if (mouseX >= x && mouseX <= x + width && mouseY >= entryTop && mouseY <= entryBottom) {
                onSelect.accept(filteredEntries.get(entryIndex));
                searchField.setFocused(false);
                return true;
            }
            entryTop = entryBottom;
        }

        if (!(mouseX >= x && mouseX <= x + width && mouseY >= searchField.getY() &&
            mouseY <= searchField.getY() + searchField.getHeight())) {
            searchField.setFocused(false);
        }
        return false;
    }

    public boolean mouseScrolled(double amount) {
        if (!visible) return false;
        int totalEntries = filteredEntries.size();
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

    public TextFieldWidget getSearchField() { return searchField; }
    public void setVisible(boolean visible) { this.visible = visible; }
    public boolean isVisible() { return visible; }
    public List<SearchEntry> getFilteredEntries() { return filteredEntries; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getEntryHeight() { return entryHeight; }
    public Consumer<SearchEntry> getOnSelect() { return onSelect; }
    public int getHoveredIndex() { return hoveredIndex; }
    public int getScrollOffset() { return scrollOffset; }
    public int getMaxVisibleEntries() { return MAX_VISIBLE_ENTRIES; }
    
}
