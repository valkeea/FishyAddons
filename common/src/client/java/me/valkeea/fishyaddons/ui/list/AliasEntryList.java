package me.valkeea.fishyaddons.ui.list;

import java.util.Map;

import me.valkeea.fishyaddons.vconfig.config.impl.ShortcutsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class AliasEntryList extends GenericEntryList {
    public AliasEntryList(Minecraft client, int width, int height, int y, int itemHeight, TabbedListScreen parentScreen) {
        super(client, width, height, y, itemHeight, parentScreen);
    }

    @Override
    public Map<String, String> getEntries() {
        return ShortcutsConfig.getAliases();
    }

    @Override
    public boolean isEntryToggled(String key) {
        return ShortcutsConfig.isAliasToggled(key);
    }

    @Override
    public void setEntry(String key, String value) {
        ShortcutsConfig.setAlias(key, value);
    }

    @Override
    public void removeEntry(String key) {
        ShortcutsConfig.removeAlias(key);
    }

    @Override
    public void toggleEntry(String key, boolean toggled) {
        ShortcutsConfig.toggleAlias(key, toggled);
    }

    @Override
    public void getGuideText(GuiGraphicsExtractor context, Font tr, int x, int y) {
        context.text(tr, Component.literal("Alias"), x - 5, y - 10, 0xFFAAAAAA);
        context.text(tr, Component.literal("Executed Command"), x + 110, y - 10, 0xFFAAAAAA);
    }

    @Override
    public String getAddButtonText() {
        return "+ Add Command";
    }

    @Override
    public String getSaveButtonText() {
        return "Save";
    }

    @Override
    public String getDeleteButtonText() {
        return "Delete";
    }

    @Override
    public String getToggleOnText() {
        return "ON";
    }

    @Override
    public String getToggleOffText() {
        return "OFF";
    }

    public GenericEntry getHoveredCommandEntry() {
        return this.getHovered();
    }

    @Override
    public void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput builder) {
        // Access
    }

    @Override
    public boolean isValidEntry(String key, String value) {
        return !key.isEmpty() && !value.isEmpty() && key.startsWith("/") && value.startsWith("/");
    }

    public boolean handleMouseClicked(MouseButtonEvent click, TabbedListScreen screen) {
        
        var entry = getHoveredCommandEntry();

        if (entry == null) return false;
        if (entry.inputWidget instanceof EditBox field) {
            if (field.mouseClicked(click, false)) {
                field.setFocused(true);
                screen.setFocused(field);
                return true;
            }

        } else if (entry.inputWidget instanceof Button btn && btn.mouseClicked(click, false)) {
            btn.setFocused(true);
            screen.setFocused(btn);
            return true;
        }

        if (entry.outputField.mouseClicked(click, false)) {
            entry.outputField.setFocused(true);
            screen.setFocused(entry.outputField);
            return true;
        }

        if (entry.saveButton.mouseClicked(click, false)) return true;
        if (entry.deleteButton.mouseClicked(click, false)) return true;
        return entry.toggleButton.mouseClicked(click, false);
    }
}
