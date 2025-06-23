package me.valkeea.fishyaddons.gui;

import me.valkeea.fishyaddons.config.FishyConfig;
import net.minecraft.client.MinecraftClient;

import java.util.Map;

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
}