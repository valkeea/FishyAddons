package me.valkeea.fishyaddons.ui.element;

import java.util.List;
import java.util.function.Consumer;

import me.valkeea.fishyaddons.tool.FishyMode;
import me.valkeea.fishyaddons.util.text.Color;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;

public class DropdownMenu {
    private final List<String> entries;
    private final Consumer<String> onSelect;

    private final int x;
    private final int y;
    private final int width;
    private final int entryHeight;

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

    public void render(GuiGraphicsExtractor gge, Screen screen, int mouseX, int mouseY) {
        if (!visible) return;

        int theme = Color.mulRGB(FishyMode.getThemeColor(), 0.3f);
        var tr = screen.getFont();
        gge.nextStratum();

        for (int i = 0; i < entries.size(); i++) {

            int entryY = y + i * entryHeight;
            boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= entryY && mouseY <= entryY + entryHeight;

            int bgColor = hovered ? theme : 0xEE121212;
            int textColor = hovered ? 0xFF000000 : theme;

            var l = entries.get(i);
            int renderW = width;
            if (hovered) {
                hoveredIndex = i;
                int w = tr.width(l);
                if (w > renderW) renderW = w;
            }            

            gge.pose().pushMatrix();      
            gge.fill(x, entryY, x + renderW, entryY + entryHeight, bgColor);
            gge.text(tr, l, x + 6, entryY + (entryHeight - 8) / 2, textColor, false);
            gge.pose().popMatrix();

        }
    }

    public boolean mouseClicked(MouseButtonEvent click) {
        if (!visible) return false;

        for (int i = 0; i < entries.size(); i++) {
            int entryY = y + i * entryHeight;

            if (click.x() >= x && click.x() < x + width && click.y() >= entryY && click.y() < entryY + entryHeight) {
                onSelect.accept(entries.get(i));
                visible = false;
                return true;
            }
        }

        visible = false;
        return false;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        int h = y + entries.size() * entryHeight;
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= h;
    }

    public void setVisible(boolean visible) { this.visible = visible; }
    public boolean isVisible() { return visible; }
    public int getEntryHeight() { return entryHeight; }
    public List<String> getEntries() { return entries; }
    public int getHoveredIndex() { return hoveredIndex; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
}
