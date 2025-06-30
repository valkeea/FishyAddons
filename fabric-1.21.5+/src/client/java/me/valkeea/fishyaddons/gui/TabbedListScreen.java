package me.valkeea.fishyaddons.gui;

import java.util.List;
import java.util.Map;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.FishyPresets;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class TabbedListScreen extends Screen {
    public enum Tab { COMMANDS, KEYBINDS, CHAT, ALERT }
    private Tab currentTab = Tab.COMMANDS;
    private AliasEntryList commandEntryList;
    private KeybindEntryList keybindEntryList;
    private ChatEntryList chatEntryList;
    private ChatAlertEntryList alertEntryList;
    private final Screen parent;

    protected boolean addingNewEntry = false;
    protected FishyPopup fishyPopup = null;
    private DropdownMenu presetDropdown;

    private TextFieldWidget presetNameField;

    public TabbedListScreen(Screen parent, Tab tab) {
        super(Text.literal("Aliases, Keybinds, Chat Replacements, Alerts"));
        this.parent = parent;
        this.currentTab = tab;
    }

    @Override
    protected void init() {
        int listWidth = 700;
        int listHeight = height - 120;
        int listY = 70;

        // Tab buttons
        addDrawableChild(new FaButton(width / 2 - 200, 40, 80, 20,
            Text.literal("Commands"),
            b -> switchTab(Tab.COMMANDS))
        );
        addDrawableChild(new FaButton(width / 2 - 95, 40, 80, 20,
            Text.literal("Keybinds"),
            b -> switchTab(Tab.KEYBINDS))
        );
        addDrawableChild(new FaButton(width / 2 + 10, 40, 80, 20,
            Text.literal("Chat"),
            b -> switchTab(Tab.CHAT))
        );
        addDrawableChild(new FaButton(width / 2 + 105, 40, 80, 20,
            Text.literal("Alerts"),
            b -> switchTab(Tab.ALERT))
        );        

        // Lists
        commandEntryList = new AliasEntryList(MinecraftClient.getInstance(), listWidth, listHeight, listY, 24, this);
        keybindEntryList = new KeybindEntryList(MinecraftClient.getInstance(), listWidth, listHeight, listY, 24, this);
        chatEntryList = new ChatEntryList(MinecraftClient.getInstance(), listWidth, listHeight, listY, 24, this);
        alertEntryList = new ChatAlertEntryList(MinecraftClient.getInstance(), listWidth, listHeight, listY, 24, this);

        commandEntryList.setPosition((width - listWidth) / 2, listY);
        keybindEntryList.setPosition((width - listWidth) / 2, listY);        
        chatEntryList.setPosition((width - listWidth) / 2, listY);
        alertEntryList.setPosition((width - listWidth) / 2, listY);

        commandEntryList.refreshWithAdd();
        keybindEntryList.refreshWithAdd();
        chatEntryList.refreshWithAdd();
        alertEntryList.refreshWithAdd();
            
        addDrawableChild(commandEntryList);
        addDrawableChild(keybindEntryList);
        addDrawableChild(chatEntryList);
        addDrawableChild(alertEntryList);
        
        updateTabVisibility();

        // Bottom buttons
        addDrawableChild(new FaButton(width / 2 - 80, height - 30, 80, 20,
            Text.literal("Back").styled(style -> style.withColor(0xFFB0B0B0)),
            b -> this.client.setScreen(new QolScreen())
        ));
        addDrawableChild(new FaButton(width / 2, height - 30, 80, 20,
            Text.literal("Close").styled(style -> style.withColor(0xFFB0B0B0)),
            b -> this.client.setScreen(null)
        ));
        addDrawableChild(new FaButton(width - 210, height - 30, 100, 20,
            Text.literal("Load From Preset").styled(style -> style.withColor(0xE2CAE9)),
            b -> showPresetDropdown()
        ));
        addDrawableChild(new FaButton(width - 310, height - 30, 100, 20,
            Text.literal("Save as Preset").styled(style -> style.withColor(0xB0FFB0)),
            b -> showSavePresetPopup()
        ));
        // Refresh the entry list to ensure it's up-to-date
        this.client.execute(this::refreshEntryList);
    }

    private void switchTab(Tab tab) {
        this.currentTab = tab;
        if (fishyPopup != null) {
            fishyPopup = null; // Close any open popup when switching tabs
        }
        updateTabVisibility();
        this.client.execute(this::refreshEntryList);
        // Reset focused element to avoid issues when switching tabs
        this.setFocused(null);
    }

    private void updateTabVisibility() {
        if (commandEntryList != null) commandEntryList.visible = (currentTab == Tab.COMMANDS);
        if (keybindEntryList != null) keybindEntryList.visible = (currentTab == Tab.KEYBINDS);
        if (chatEntryList != null) chatEntryList.visible = (currentTab == Tab.CHAT);
        if (alertEntryList != null) alertEntryList.visible = (currentTab == Tab.ALERT);        
    }

    protected void refreshEntryList() {
        switch (currentTab) {
            case COMMANDS -> commandEntryList.refreshWithAdd();
            case KEYBINDS -> keybindEntryList.refreshWithAdd();
            case CHAT -> chatEntryList.refreshWithAdd();
            case ALERT -> alertEntryList.refreshWithAdd();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, width / 2, 20, 0xFF55FFFF);
        super.render(context, mouseX, mouseY, delta);

        if (presetDropdown != null && presetDropdown.isVisible()) {
            presetDropdown.render(context, this, mouseX, mouseY);
        }

        // Only show tooltips if popup is NOT open
        if (fishyPopup == null) {
            if (currentTab == Tab.COMMANDS) {
                for (var entry : commandEntryList.children()) {
                    if (entry instanceof GenericEntryList.GenericEntry ge && ge.pendingTooltip != null) {
                        GuiUtil.fishyTooltip(context, this.textRenderer, ge.tooltipLines != null ? ge.tooltipLines : List.of(Text.literal(ge.pendingTooltip)), ge.tooltipX, ge.tooltipY);
                        break;
                    }
                }
            } else if (currentTab == Tab.KEYBINDS) {
                for (var entry : keybindEntryList.children()) {
                    if (entry instanceof GenericEntryList.GenericEntry ge && ge.pendingTooltip != null) {
                        GuiUtil.fishyTooltip(context, this.textRenderer, ge.tooltipLines != null ? ge.tooltipLines : List.of(Text.literal(ge.pendingTooltip)), ge.tooltipX, ge.tooltipY);
                        break;
                    }
                }
            } else if (currentTab == Tab.CHAT) {
                for (var entry : chatEntryList.children()) {
                    if (entry instanceof GenericEntryList.GenericEntry ge && ge.pendingTooltip != null) {
                        GuiUtil.fishyTooltip(context, this.textRenderer, ge.tooltipLines != null ? ge.tooltipLines : List.of(Text.literal(ge.pendingTooltip)), ge.tooltipX, ge.tooltipY);
                        break;
                    }
                }
            } else if (currentTab == Tab.ALERT) {
                for (var entry : alertEntryList.children()) {
                    if (entry instanceof GenericEntryList.GenericEntry ge && ge.pendingTooltip != null) {
                        GuiUtil.fishyTooltip(context, this.textRenderer, ge.tooltipLines != null ? ge.tooltipLines : List.of(Text.literal(ge.pendingTooltip)), ge.tooltipX, ge.tooltipY);
                        break;
                    }
                }
            }
        }

        if (fishyPopup != null) {
            this.renderBackground(context, mouseX, mouseY, delta);
            fishyPopup.render(context, this.textRenderer, mouseX, mouseY, delta);
        }

        if (fishyPopup != null && presetNameField != null) {
            context.getMatrices().push();
            context.getMatrices().translate(0, 0, 500);
            presetNameField.setX(fishyPopup.getX() + (fishyPopup.getWidth() - presetNameField.getWidth()) / 2);
            presetNameField.setY(fishyPopup.getY() + 35);
            presetNameField.render(context, mouseX, mouseY, delta);
            context.getMatrices().pop();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (fishyPopup != null && presetNameField != null) {
            if (presetNameField.mouseClicked(mouseX, mouseY, button)) {
                presetNameField.setFocused(true);
                this.setFocused(presetNameField);
                return true;
            }
        }
        if (presetDropdown != null && presetDropdown.isVisible()) {
            if (presetDropdown.mouseClicked(mouseX, mouseY, button)) return true;
            // If click was outside dropdown, close it
            int x = presetDropdown.getX(); 
            int y = presetDropdown.getY(); 
            int w = presetDropdown.getWidth();
            int h = presetDropdown.getEntryHeight() * presetDropdown.getEntries().size();
            if (!(mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h)) {
                presetDropdown.setVisible(false);
                return true;
            }
        }
        if (fishyPopup != null) {
            if (fishyPopup.mouseClicked(mouseX, mouseY, button)) return true;
            return false; // Block clicks to the rest of the UI
        }
        if (currentTab == Tab.COMMANDS) {
            if (commandEntryList.handleMouseClicked(mouseX, mouseY, button, this)) return true;
        } else if (currentTab == Tab.KEYBINDS) {
            if (keybindEntryList.handleMouseClicked(mouseX, mouseY, button, this)) return true;
        } else if (currentTab == Tab.CHAT) {
            if (chatEntryList.handleMouseClicked(mouseX, mouseY, button, this)) return true;
        } else if (currentTab == Tab.ALERT) {
            if (alertEntryList.handleMouseClicked(mouseX, mouseY, button, this)) return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (fishyPopup != null && presetNameField != null) {
            if (presetNameField.keyPressed(keyCode, scanCode, modifiers)) return true;
            return false;
        }
        GenericEntryList.GenericEntry entry = null;
        if (currentTab == Tab.COMMANDS) {
            entry = (GenericEntryList.GenericEntry) commandEntryList.getFocused();
        } else if (currentTab == Tab.KEYBINDS) {
            entry = (GenericEntryList.GenericEntry) keybindEntryList.getFocused();
        } else if (currentTab == Tab.CHAT) {
            entry = (GenericEntryList.GenericEntry) chatEntryList.getFocused();
        } else if (currentTab == Tab.ALERT) {
            entry = (GenericEntryList.GenericEntry) alertEntryList.getFocused();
        }
        if (entry != null) {
            if (entry.outputField.isFocused() && entry.outputField.keyPressed(keyCode, scanCode, modifiers)) return true;
            if (entry.inputWidget instanceof TextFieldWidget field && field.isFocused() && field.keyPressed(keyCode, scanCode, modifiers)) return true;
            if (entry.inputWidget instanceof ButtonWidget btn && btn.isFocused() && btn.keyPressed(keyCode, scanCode, modifiers)) return true;
            // Forward to the entry itself (for Enter/Tab etc)
            if (entry.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (fishyPopup != null && presetNameField != null) {
            if (presetNameField.charTyped(chr, modifiers)) return true;
            return false;
        }
        if (currentTab == Tab.COMMANDS) {
            if (commandEntryList.getFocused() instanceof GenericEntryList.GenericEntry entry) {
                if (entry.outputField.isFocused() && entry.outputField.charTyped(chr, modifiers)) return true;
                if (entry.inputWidget instanceof TextFieldWidget field && field.isFocused() && field.charTyped(chr, modifiers)) return true;
            }
        } else if (currentTab == Tab.KEYBINDS) {
            if (keybindEntryList.getFocused() instanceof GenericEntryList.GenericEntry entry) {
                if (entry.outputField.isFocused() && entry.outputField.charTyped(chr, modifiers)) return true;
                if (entry.inputWidget instanceof TextFieldWidget field && field.isFocused() && field.charTyped(chr, modifiers)) return true;
            }
        } else if (currentTab == Tab.CHAT) {
            if (chatEntryList.getFocused() instanceof GenericEntryList.GenericEntry entry) {
                if (entry.outputField.isFocused() && entry.outputField.charTyped(chr, modifiers)) return true;
                if (entry.inputWidget instanceof TextFieldWidget field && field.isFocused() && field.charTyped(chr, modifiers)) return true;
            }
        } else if (currentTab == Tab.ALERT) {
            if (alertEntryList.getFocused() instanceof GenericEntryList.GenericEntry entry) {
                if (entry.outputField.isFocused() && entry.outputField.charTyped(chr, modifiers)) return true;
                if (entry.inputWidget instanceof TextFieldWidget field && field.isFocused() && field.charTyped(chr, modifiers)) return true;
            }
        }
        return super.charTyped(chr, modifiers);
    }

    public void showFishyPopup(Text title, Text continueButtonText, Runnable onContinue, Text discardButtonText, Runnable onDiscard) {
        this.fishyPopup = new FishyPopup(title, continueButtonText, onContinue, discardButtonText, onDiscard);
        this.fishyPopup.init(this.width, this.height);
    }

    private void showPresetDropdown() {
        var type = switch (currentTab) {
            case COMMANDS -> FishyPresets.PresetType.COMMANDS;
            case KEYBINDS -> FishyPresets.PresetType.KEYBINDS;
            case CHAT -> FishyPresets.PresetType.CHAT;
            case ALERT -> FishyPresets.PresetType.ALERT;
        };
        List<String> suffixes = FishyPresets.listPresetSuffixes(type);
        if (suffixes.isEmpty()) {
            showFishyPopup(Text.literal("No presets found for this tab."), Text.literal("OK"), () -> fishyPopup = null, Text.literal(""), () -> {});
            return;
        }
        int dropdownX = width - 220;
        int dropdownY = height - 40 - (suffixes.size() * 14) / 2;
        presetDropdown = new DropdownMenu(
            suffixes, dropdownX, dropdownY, 100, 14,
            suffix -> {
                loadPresetForCurrentTab(suffix);
                refreshEntryList();
                presetDropdown.setVisible(false);
            }
        );
    }

    // Overload loadPresetForCurrentTab to accept a suffix:
    private void loadPresetForCurrentTab(String suffix) {
        switch (currentTab) {
            case COMMANDS -> {
                Map<String, String> map = FishyPresets.loadStringPreset(FishyPresets.PresetType.COMMANDS, suffix);
                if (map != null) {
                    FishyConfig.commandAliases.getValues().putAll(map);
                    FishyConfig.save();
                    commandEntryList.refreshWithAdd();
                }
            }
            case KEYBINDS -> {
                Map<String, String> map = FishyPresets.loadStringPreset(FishyPresets.PresetType.KEYBINDS, suffix);
                if (map != null) {
                    FishyConfig.keybinds.getValues().putAll(map);
                    FishyConfig.save();
                    keybindEntryList.refreshWithAdd();
                }
            }
            case CHAT -> {
                Map<String, String> map = FishyPresets.loadStringPreset(FishyPresets.PresetType.CHAT, suffix);
                if (map != null) {
                    FishyConfig.chatReplacements.getValues().putAll(map);
                    FishyConfig.save();
                    chatEntryList.refreshWithAdd();
                }
            }
            case ALERT -> {
                Map<String, FishyConfig.AlertData> map = FishyPresets.loadAlertPreset(suffix);
                if (map != null) {
                    FishyConfig.chatAlerts.getValues().putAll(map);
                    FishyConfig.save();
                    alertEntryList.refreshWithAdd();
                }
            }
        }
    }

    private void showSavePresetPopup() {
        presetNameField = new FaTextField(this.textRenderer, this.width / 2 - 60,
        this.height / 2, 120, 20, Text.literal("Preset Name"));
        presetNameField.setMaxLength(32);
        presetNameField.setText("");
        this.setFocused(presetNameField);

        showFishyPopup(
            Text.literal("Enter preset name:"),
            Text.literal("Save"),
            () -> {
                String suffix = presetNameField.getText().trim();
                if (!suffix.isEmpty()) {
                    saveCurrentTabAsPreset(suffix);
                }
                fishyPopup = null;
                this.remove(presetNameField);
                presetNameField = null;
            },
            Text.literal("Cancel"),
            () -> {
                fishyPopup = null;
                this.remove(presetNameField);
                presetNameField = null;
            }
        );
    }

    private void saveCurrentTabAsPreset(String suffix) {
        switch (currentTab) {
            case COMMANDS -> FishyPresets.saveStringPreset(
                FishyPresets.PresetType.COMMANDS, suffix, FishyConfig.commandAliases.getValues());
            case KEYBINDS -> FishyPresets.saveStringPreset(
                FishyPresets.PresetType.KEYBINDS, suffix, FishyConfig.keybinds.getValues());
            case CHAT -> FishyPresets.saveStringPreset(
                FishyPresets.PresetType.CHAT, suffix, FishyConfig.chatReplacements.getValues());
            case ALERT -> FishyPresets.saveAlertPreset(
                suffix, FishyConfig.chatAlerts.getValues());
        }
    }
}

