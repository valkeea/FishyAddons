package me.valkeea.fishyaddons.gui;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
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
                .map(id -> new SearchEntry(id, null, () -> onSelect.accept(id)))
                .collect(Collectors.toList()),
            x, y, width, entryHeight,
            entry -> onSelect.accept(entry.name),
            screen
        );
        this.onRightClick = onRightClick;
    }

    public SoundSearchMenu(List<String> entries, int x, int y, int width, int entryHeight,
                           Consumer<String> onSelect, Consumer<String> onRightClick, Screen screen, TextFieldWidget externalField) {
        super(
            entries.stream()
                .map(id -> new SearchEntry(id, null, () -> onSelect.accept(id)))
                .collect(Collectors.toList()),
            x, y, width, entryHeight,
            entry -> onSelect.accept(entry.name),
            screen,
            externalField
        );
        this.onRightClick = onRightClick;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isVisible()) return false;
        if (getSearchField().mouseClicked(mouseX, mouseY, button)) return true;
        if (!getSearchField().isFocused()) return false;
        boolean insideDropdown = false;
        int x = getX();
        int width = getWidth();
        for (int i = 0; i < getFilteredEntries().size(); i++) {
            int entryY = getY() + i * getEntryHeight();
            if (mouseX >= getX() && mouseX <= getX() + getWidth() && mouseY >= entryY && mouseY <= entryY + getEntryHeight()) {
                SearchEntry entry = getFilteredEntries().get(i);
                String soundId = entry.name;
                if (button == 1) {
                    onRightClick.accept(soundId);
                } else {
                    getOnSelect().accept(entry);
                    getSearchField().setFocused(false);
                }
                insideDropdown = true;
                break;
            }
        }
        if (!insideDropdown && !(mouseX >= x && mouseX <= x + width && mouseY >= getSearchField().getY() && mouseY <= getSearchField().getY() + getSearchField().getHeight())) {
            getSearchField().setFocused(false);
        }
        return insideDropdown;
    }

    public List<String> getSoundIds() {
        return soundIds;
    }
}