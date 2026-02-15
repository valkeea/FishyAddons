package me.valkeea.fishyaddons.ui.list;

import java.util.Map;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.feature.qol.ChatReplacement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

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
        ChatReplacement.refresh();
    }

    @Override
    public void getGuideText(DrawContext context, TextRenderer tr, int x, int y) {
        context.drawTextWithShadow(tr, Text.literal("Detected String"), x - 5, y - 10, 0xFFAAAAAA);
        context.drawTextWithShadow(tr, Text.literal("Replaced in chat with:"), x + 110, y - 10, 0xFFAAAAAA);
    }     

    public GenericEntryList.GenericEntry getHoveredChatEntry() {
        return this.getHoveredEntry();
    }

    @Override
    public void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
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

    public boolean handleMouseClicked(Click click, TabbedListScreen screen) {

        var entry = getHoveredChatEntry();

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
