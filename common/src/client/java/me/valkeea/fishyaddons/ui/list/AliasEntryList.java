package me.valkeea.fishyaddons.ui.list;

import java.util.Map;

import me.valkeea.fishyaddons.config.FishyConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

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
    public void getGuideText(DrawContext context, TextRenderer tr, int x, int y) {
        context.drawTextWithShadow(tr, Text.literal("Alias"), x - 5, y - 10, 0xFFAAAAAA);
        context.drawTextWithShadow(tr, Text.literal("Executed Command"), x + 110, y - 10, 0xFFAAAAAA);
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
        return this.getHoveredEntry();
    }

    @Override
    public void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
        // Access
    }

    @Override
    public boolean isValidEntry(String key, String value) {
        return !key.isEmpty() && !value.isEmpty() && key.startsWith("/") && value.startsWith("/");
    }

    public boolean handleMouseClicked(Click click, TabbedListScreen screen) {
        
        var entry = getHoveredCommandEntry();

        if (entry == null) return false;
        if (entry.inputWidget instanceof TextFieldWidget field) {
            if (field.mouseClicked(click, false)) {
                field.setFocused(true);
                screen.setFocused(field);
                return true;
            }

        } else if (entry.inputWidget instanceof ButtonWidget btn && btn.mouseClicked(click, false)) {
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
