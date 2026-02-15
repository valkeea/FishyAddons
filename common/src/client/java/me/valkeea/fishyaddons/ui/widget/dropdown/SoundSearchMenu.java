package me.valkeea.fishyaddons.ui.widget.dropdown;

import java.util.List;
import java.util.function.Consumer;

import me.valkeea.fishyaddons.ui.widget.VCTextField;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class SoundSearchMenu extends SearchMenu {
    private final Consumer<String> onRightClick;

    List<String> soundIds = Registries.SOUND_EVENT.stream()
        .map(Registries.SOUND_EVENT::getId)
        .filter(java.util.Objects::nonNull)
        .map(Identifier::toString)
        .sorted()
        .toList();

    public SoundSearchMenu(List<String> entries, int x, int y, int width, int entryHeight,
                           Consumer<String> onSelect, Consumer<String> onRightClick, Screen screen) {
        super(
            entries.stream()
                .map(id -> new SearchEntry(id, null, null, () -> onSelect.accept(id)))
                .toList(),
            x, y, width, entryHeight,
            entry -> onSelect.accept(entry.name),
            screen
        );
        this.onRightClick = onRightClick;
    }

    public SoundSearchMenu(List<String> entries, int x, int y, int width, int entryHeight,
                           Consumer<String> onSelect, Consumer<String> onRightClick, Screen screen, VCTextField externalField) {
        super(
            entries.stream()
                .map(id -> new SearchEntry(id, null, null, () -> onSelect.accept(id)))
                .toList(),
            x, y, width, entryHeight,
            entry -> onSelect.accept(entry.name),
            screen,
            externalField
        );
        this.onRightClick = onRightClick;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (!isVisible()) return false;

        if (click.button() == 1) {
            return handleRightClick(click.x(), click.y());
        }

        return super.mouseClicked(click, doubled);
    }
    
    private boolean handleRightClick(double mouseX, double mouseY) {

        int totalEntries = getFilteredEntries().size();
        int visibleEntries = Math.min(totalEntries, getMaxVisibleEntries());
        int[] entryHeights = calculateEntryHeights(visibleEntries);
        int entryTop = getY();

        for (int i = 0; i < visibleEntries; i++) {
            int entryIndex = i + getScrollOffset();
            if (entryIndex >= getFilteredEntries().size()) break;
            
            int entryHeight = entryHeights[i];
            int entryBottom = entryTop + entryHeight;
            
            if (isClickInsideEntry(mouseX, mouseY, entryTop, entryBottom)) {
                SearchEntry entry = getFilteredEntries().get(entryIndex);
                onRightClick.accept(entry.name);
                return true;
            }
            entryTop = entryBottom;
        }
        return false;
    }
    
    private int[] calculateEntryHeights(int visibleEntries) {

        int[] entryHeights = new int[visibleEntries];

        for (int i = 0; i < visibleEntries; i++) {
            int entryIndex = i + getScrollOffset();
            if (entryIndex >= getFilteredEntries().size()) break;
            var entry = getFilteredEntries().get(entryIndex);
            int descLines = 0;
            if (entry.description != null && !entry.description.isEmpty()) {
                descLines = entry.description.split("\n").length;
            }

            entryHeights[i] = 14 + descLines * 12 + 4;
        }
        return entryHeights;
    }
    
    private boolean isClickInsideEntry(double mouseX, double mouseY, int entryTop, int entryBottom) {
        return mouseX >= getX() && mouseX <= getX() + getWidth() && 
               mouseY >= entryTop && mouseY <= entryBottom;
    }

    public List<String> getSoundIds() {
        return soundIds;
    }
}
