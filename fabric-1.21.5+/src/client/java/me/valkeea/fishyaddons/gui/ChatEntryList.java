package me.valkeea.fishyaddons.gui;

import java.util.Map;

import me.valkeea.fishyaddons.config.FishyConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;

public class ChatEntryList extends GenericEntryList {
    public ChatEntryList(MinecraftClient client, int width, int height, int y, int itemHeight, TabbedListScreen parentScreen) {
        super(client, width, height, y, itemHeight, parentScreen);
    }

    @Override
    public Map<String, String> getEntries() {
        return FishyConfig.getChatReplacements();
    }

    @Override
    public boolean isEntryToggled(String key) {
        return FishyConfig.isChatReplacementToggled(key);
    }

    @Override
    public void setEntry(String key, String value) {
        FishyConfig.setChatReplacement(key, value);
    }

    @Override
    public void removeEntry(String key) {
        FishyConfig.removeChatReplacement(key);
    }

    @Override
    public void toggleEntry(String key, boolean toggled) {
        FishyConfig.toggleChatReplacement(key, toggled);
    }

    public GenericEntryList.GenericEntry getHoveredChatEntry() {
        return (GenericEntryList.GenericEntry) this.getHoveredEntry();
    }

    @Override
    public void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
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

    public boolean handleMouseClicked(double mouseX, double mouseY, int button, TabbedListScreen screen) {
        GenericEntryList.GenericEntry entry = getHoveredChatEntry();
        if (entry == null) return false;
        if (entry.inputWidget instanceof TextFieldWidget field) {
            if (field.mouseClicked(mouseX, mouseY, button)) {
                field.setFocused(true);
                screen.setFocused(field);
                return true;
            }
        } else if (entry.inputWidget instanceof ButtonWidget btn) {
            if (btn.mouseClicked(mouseX, mouseY, button)) {
                btn.setFocused(true);
                screen.setFocused(btn);
                return true;
            }
        }
        if (entry.outputField.mouseClicked(mouseX, mouseY, button)) {
            entry.outputField.setFocused(true);
            screen.setFocused(entry.outputField);
            return true;
        }
        if (entry.saveButton.mouseClicked(mouseX, mouseY, button)) return true;
        if (entry.deleteButton.mouseClicked(mouseX, mouseY, button)) return true;
        if (entry.toggleButton.mouseClicked(mouseX, mouseY, button)) return true;
        return false;
    }
}