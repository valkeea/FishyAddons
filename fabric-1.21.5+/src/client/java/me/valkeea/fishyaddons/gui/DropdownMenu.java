package me.valkeea.fishyaddons.gui;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

public class DropdownMenu {
    private final List<String> entries;
    private final int x, y, width, entryHeight;
    private final Consumer<String> onSelect;
    private int hoveredIndex = -1;
    private boolean visible = true;

    public DropdownMenu(List<String> entries, int x, int y, int width, int entryHeight, Consumer<String> onSelect) {
        this.entries = entries;
        this.x = x;
        this.y = y;
        this.width = width;
        this.entryHeight = entryHeight;
        this.onSelect = onSelect;
    }

    public void render(DrawContext context, Screen screen, int mouseX, int mouseY) {
        if (!visible) return;
        for (int i = 0; i < entries.size(); i++) {
            int entryY = y + i * entryHeight;
            boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= entryY && mouseY <= entryY + entryHeight;
            int bgColor = hovered ? 0xFFE2CAE9 : 0xCC222222;
            context.fill(x, entryY, x + width, entryY + entryHeight, bgColor);
            int textColor = hovered ? 0xFF000000 : 0xFFE2CAE9;
            context.drawText(screen.getTextRenderer(), entries.get(i), x + 6, entryY + (entryHeight - 8) / 2, textColor, false);
            if (hovered) hoveredIndex = i;
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;
        for (int i = 0; i < entries.size(); i++) {
            int entryY = y + i * entryHeight;

            if (mouseX >= x && mouseX < x + width && mouseY >= entryY && mouseY < entryY + entryHeight) {
                onSelect.accept(entries.get(i));
                visible = false;
                return true;
            }
        }
        visible = false;
        return false;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() { return visible; }
    public int getEntryHeight() { return entryHeight; }
    public List<String> getEntries() { return entries; }
    public int getHoveredIndex() { return hoveredIndex; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
}