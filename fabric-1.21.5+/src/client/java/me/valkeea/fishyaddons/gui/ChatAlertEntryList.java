package me.valkeea.fishyaddons.gui;

import java.util.LinkedHashMap;
import java.util.Map;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.FishyConfig.AlertData;
import me.valkeea.fishyaddons.handler.ChatAlert;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class ChatAlertEntryList extends GenericEntryList {
    public ChatAlertEntryList(MinecraftClient client, int width, int height, int y, int itemHeight, TabbedListScreen parentScreen) {
        super(client, width, height, y, itemHeight, parentScreen);
    }

    @Override
    public Map<String, String> getEntries() {
        Map<String, AlertData> alerts = FishyConfig.getChatAlerts();
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, AlertData> e : alerts.entrySet()) {
            result.put(e.getKey(), e.getValue() != null && e.getValue().getMsg() != null ? e.getValue().getMsg() : "");
        }
        return result;
    }

    @Override
    public boolean isEntryToggled(String key) {
        return FishyConfig.isChatAlertToggled(key);
    }

    public GenericEntryList.GenericEntry getHoveredAlertEntry() {
        return (GenericEntryList.GenericEntry) this.getHoveredEntry();
    }    

    @Override
    public void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
    }    

    @Override
    public void setEntry(String key, String value) {
        AlertData data = FishyConfig.getChatAlerts().getOrDefault(key, new AlertData());
        data.setMsg(value);
        FishyConfig.setChatAlert(key, data);
        ChatAlert.refresh();
    }

    @Override
    public void removeEntry(String key) {
        FishyConfig.removeChatAlert(key);
        ChatAlert.refresh();
    }

    @Override
    public void toggleEntry(String key, boolean toggled) {
        FishyConfig.toggleChatAlert(key, toggled);
        ChatAlert.refresh();
    }

    @Override
    public void getGuideText(DrawContext context, TextRenderer tr, int x, int y) {
        context.drawTextWithShadow(tr, Text.literal("Detected String"), x - 5, y - 10, 0xFFAAAAAA);
        context.drawTextWithShadow(tr, Text.literal("Auto Chat"), x + 110, y - 10, 0xFFAAAAAA);
    }

    @Override public String getAddButtonText() { return "+ Add Alert"; }
    @Override public String getSaveButtonText() { return "Save"; }
    @Override public String getDeleteButtonText() { return "Delete"; }
    @Override public String getToggleOnText() { return "ON"; }
    @Override public String getToggleOffText() { return "OFF"; }
    @Override public String getDefaultInput() { return ""; }
    @Override public String getDefaultOutput() { return ""; }
    @Override public boolean isValidEntry(String input, String output) {
        return !input.isBlank();
    }

    @Override
    public ButtonWidget createExtraButton(GenericEntryList.GenericEntry entry) {
        return new FaButton(0, 0, 40, 20,
            net.minecraft.text.Text.literal("More").styled(style -> style.withColor(0xE2CAE9)),
            b -> {
                // Save current edits before opening AlertEditScreen
                if (entry != null) {
                    entry.checkAndSave();
                }
                AlertData data = FishyConfig.getChatAlerts().get(entry.input);
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null) {
                    client.setScreen(new AlertEditScreen(entry.input, data, this.parentScreen));
                }
            }
        );
    }

    public boolean handleMouseClicked(double mouseX, double mouseY, int button, TabbedListScreen screen) {
        GenericEntryList.GenericEntry entry = getHoveredAlertEntry();
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
        if (entry.extraButton != null && entry.extraButton.mouseClicked(mouseX, mouseY, button)) {
            entry.extraButton.setFocused(true);
            screen.setFocused(entry.extraButton);
            return true;
        }
        return false;
    }
}