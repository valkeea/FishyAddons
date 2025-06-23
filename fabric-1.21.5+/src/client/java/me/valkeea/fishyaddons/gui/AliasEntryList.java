package me.valkeea.fishyaddons.gui;

import me.valkeea.fishyaddons.config.FishyConfig;
import net.minecraft.client.MinecraftClient;

import java.util.Map;

public class AliasEntryList extends GenericEntryList {
    public AliasEntryList(MinecraftClient client, int width, int height, int y, int itemHeight, TabbedListScreen parentScreen) {
        super(client, width, height, y, itemHeight, parentScreen);
    }

    @Override
    public Map<String, String> getEntries() {
        return FishyConfig.getCommandAliases();
    }

    @Override
    public boolean isEntryToggled(String key) {
        return FishyConfig.isCommandToggled(key);
    }

    @Override
    public void setEntry(String key, String value) {
        FishyConfig.setCommandAlias(key, value);
    }

    @Override
    public void removeEntry(String key) {
        FishyConfig.removeCommandAlias(key);
    }

    @Override
    public void toggleEntry(String key, boolean toggled) {
        FishyConfig.toggleCommand(key, toggled);
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

    public GenericEntryList.GenericEntry getHoveredCommandEntry() {
        return (GenericEntryList.GenericEntry) this.getHoveredEntry();
    }

    @Override
    public void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
    }

    @Override
    public boolean isValidEntry(String key, String value) {
        return !key.isEmpty() && !value.isEmpty() && key.startsWith("/") && value.startsWith("/");
    }
}