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

public class ChatEntryList extends GenericEntryList {
    public ChatEntryList(Minecraft client, int width, int height, int y, int itemHeight, TabbedListScreen parentScreen) {
        super(client, width, height, y, itemHeight, parentScreen);
    }

    @Override
    public Map<String, String> getEntries() {
        return ShortcutsConfig.getChat();
    }

    @Override
    public boolean isEntryToggled(String key) {
        return ShortcutsConfig.isChatToggled(key);
    }

    @Override
    public void setEntry(String key, String value) {
        ShortcutsConfig.setChat(key, value);
    }

    @Override
    public void removeEntry(String key) {
        ShortcutsConfig.removeChat(key);
    }

    @Override
    public void toggleEntry(String key, boolean toggled) {
        ShortcutsConfig.toggleChat(key, toggled);
    }

    @Override
    public void getGuideText(GuiGraphicsExtractor context, Font tr, int x, int y) {
        context.text(tr, Component.literal("Detected String"), x - 5, y - 10, 0xFFAAAAAA);
        context.text(tr, Component.literal("Replaced in chat with:"), x + 110, y - 10, 0xFFAAAAAA);
    }     

    public GenericEntry getHoveredChatEntry() {
        return this.getHovered();
    }

    @Override
    public void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput builder) {
        // Access
    }

    @Override
    public boolean isValidEntry(String key, String value) {
        return !key.isEmpty() && !value.isEmpty();
    }

    @Override public String getAddButtonText() { return "+ New Replacement";}
    @Override public String getSaveButtonText() { return "Save";}
    @Override public String getDeleteButtonText() { return "Delete";}
    @Override public String getToggleOnText() { return "ON";}
    @Override public String getToggleOffText() { return "OFF";}
    @Override public String getDefaultInput() { return ""; }
    @Override public String getDefaultOutput() { return ""; }

    public boolean handleMouseClicked(MouseButtonEvent click, TabbedListScreen screen) {

        var entry = getHoveredChatEntry();

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
