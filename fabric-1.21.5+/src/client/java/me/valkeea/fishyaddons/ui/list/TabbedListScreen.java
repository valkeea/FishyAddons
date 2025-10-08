package me.valkeea.fishyaddons.ui.list;

import java.util.List;
import java.util.Map;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.FishyPresets;
import me.valkeea.fishyaddons.handler.ChatReplacement;
import me.valkeea.fishyaddons.handler.CommandAlias;
import me.valkeea.fishyaddons.handler.KeyShortcut;
import me.valkeea.fishyaddons.ui.GuiUtil;
import me.valkeea.fishyaddons.ui.VCScreen;
import me.valkeea.fishyaddons.ui.widget.FaButton;
import me.valkeea.fishyaddons.ui.widget.FaTextField;
import me.valkeea.fishyaddons.ui.widget.FishyPopup;
import me.valkeea.fishyaddons.ui.widget.dropdown.DropdownMenu;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class TabbedListScreen extends Screen {
    public enum Tab { COMMANDS, KEYBINDS, CHAT }
    private Tab currentTab = Tab.COMMANDS;
    private AliasEntryList commandEntryList;
    private KeybindEntryList keybindEntryList;
    private ChatEntryList chatEntryList;
    private final Screen parent;

    protected boolean addingNewEntry = false;
    protected FishyPopup fishyPopup = null;
    private DropdownMenu presetDropdown;

    private TextFieldWidget presetNameField;

    public TabbedListScreen(Screen parent, Tab tab) {
        super(Text.literal("Aliases, Keybinds, Chat Replacement"));
        this.parent = parent;
        this.currentTab = tab;
    }

    public static void keyTab() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(new TabbedListScreen(client.currentScreen, TabbedListScreen.Tab.KEYBINDS));
    }

    public static void cmdTab() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(new TabbedListScreen(client.currentScreen, TabbedListScreen.Tab.COMMANDS));
    }

    public static void chatTab() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(new TabbedListScreen(client.currentScreen, TabbedListScreen.Tab.CHAT));
    }

    @Override
    protected void init() {
        int listWidth = 700;
        int listHeight = height - 120;
        int listY = 90;

        // Tab buttons
        addDrawableChild(new FaButton(width / 2 - 140, 40, 80, 20,
            Text.literal("Commands"),
            btn -> switchTab(Tab.COMMANDS))
        );
        addDrawableChild(new FaButton(width / 2 - 40, 40, 80, 20,
            Text.literal("Keybinds"),
            btn -> switchTab(Tab.KEYBINDS))
        );
        addDrawableChild(new FaButton(width / 2 + 60, 40, 80, 20,
            Text.literal("Chat"),
            btn -> switchTab(Tab.CHAT))
        );

        // Lists
        commandEntryList = new AliasEntryList(MinecraftClient.getInstance(), listWidth, listHeight, listY, 24, this);
        keybindEntryList = new KeybindEntryList(MinecraftClient.getInstance(), listWidth, listHeight, listY, 24, this);
        chatEntryList = new ChatEntryList(MinecraftClient.getInstance(), listWidth, listHeight, listY, 24, this);

        commandEntryList.setPosition((width - listWidth) / 2, listY);
        keybindEntryList.setPosition((width - listWidth) / 2, listY);        
        chatEntryList.setPosition((width - listWidth) / 2, listY);

        commandEntryList.refreshWithAdd();
        keybindEntryList.refreshWithAdd();
        chatEntryList.refreshWithAdd();
            
        addDrawableChild(commandEntryList);
        addDrawableChild(keybindEntryList);
        addDrawableChild(chatEntryList);
        
        updateTabVisibility();

        // Bottom buttons
        addDrawableChild(new FaButton(width / 2 - 80, height - 30, 80, 20,
            Text.literal("Back").styled(style -> style.withColor(0xFFB0B0B0)),
            b -> this.client.setScreen(new VCScreen())
        ));
        addDrawableChild(new FaButton(width / 2, height - 30, 80, 20,
            Text.literal("Close").styled(style -> style.withColor(0xFFB0B0B0)),
            b -> this.client.setScreen(null)
        ));
        addDrawableChild(new FaButton(width / 2 + 205, height - 30, 100, 20,
            Text.literal("Load From Preset").styled(style -> style.withColor(0xE2CAE9)),
            b -> showPresetDropdown()
        ));
        addDrawableChild(new FaButton(width / 2 + 105, height - 30, 100, 20,
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
    }

    protected void refreshEntryList() {
        switch (currentTab) {
            case COMMANDS -> commandEntryList.refreshWithAdd();
            case KEYBINDS -> keybindEntryList.refreshWithAdd();
            case CHAT -> chatEntryList.refreshWithAdd();
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

        if (fishyPopup == null) {
            if (currentTab == Tab.COMMANDS) {
                commandEntryList.getGuideText(context, this.textRenderer, width - 740, 85);
                for (var entry : commandEntryList.children()) {
                    if (entry instanceof GenericEntryList.GenericEntry ge && ge.pendingTooltip != null) {
                        GuiUtil.fishyTooltip(context, this.textRenderer, ge.tooltipLines != null ? ge.tooltipLines : List.of(Text.literal(ge.pendingTooltip)), ge.tooltipX, ge.tooltipY);
                        break;
                    }
                }
            } else if (currentTab == Tab.KEYBINDS) {
                keybindEntryList.getGuideText(context, this.textRenderer, width - 740, 85);                
                for (var entry : keybindEntryList.children()) {
                    if (entry instanceof GenericEntryList.GenericEntry ge && ge.pendingTooltip != null) {
                        GuiUtil.fishyTooltip(context, this.textRenderer, ge.tooltipLines != null ? ge.tooltipLines : List.of(Text.literal(ge.pendingTooltip)), ge.tooltipX, ge.tooltipY);
                        break;
                    }
                }
            } else if (currentTab == Tab.CHAT) {
                chatEntryList.getGuideText(context, this.textRenderer, width - 740, 85);
                for (var entry : chatEntryList.children()) {
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
        if (fishyPopup != null && presetNameField != null && presetNameField.mouseClicked(mouseX, mouseY, button)) {
            presetNameField.setFocused(true);
            this.setFocused(presetNameField);
            return true;
        }
        if (presetDropdown != null && presetDropdown.isVisible()) {
            if (presetDropdown.mouseClicked(mouseX, mouseY)) return true;
            
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
            return fishyPopup.mouseClicked(mouseX, mouseY, button);
        }
        if (currentTab == Tab.COMMANDS) {
            if (commandEntryList.handleMouseClicked(mouseX, mouseY, button, this)) return true;
        } else if (currentTab == Tab.KEYBINDS) {
            if (keybindEntryList.handleMouseClicked(mouseX, mouseY, button, this)) return true;
        } else if (currentTab == Tab.CHAT && chatEntryList.handleMouseClicked(mouseX, mouseY, button, this)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (fishyPopup != null && presetNameField != null) {
            return presetNameField.keyPressed(keyCode, scanCode, modifiers);
        }
        GenericEntryList.GenericEntry entry = null;
        if (currentTab == Tab.COMMANDS) {
            entry = commandEntryList.getFocused();
        } else if (currentTab == Tab.KEYBINDS) {
            entry = keybindEntryList.getFocused();
        } else if (currentTab == Tab.CHAT) {
            entry = chatEntryList.getFocused();
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
            return presetNameField.charTyped(chr, modifiers);
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
        } else if (currentTab == Tab.CHAT && chatEntryList.getFocused() instanceof GenericEntryList.GenericEntry entry) {
            if (entry.outputField.isFocused() && entry.outputField.charTyped(chr, modifiers)) return true;
            if (entry.inputWidget instanceof TextFieldWidget field && field.isFocused() && field.charTyped(chr, modifiers)) return true;
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
        };
        
        List<String> suffixes = FishyPresets.listPresetSuffixes(type);

        if (suffixes.isEmpty()) {
            showFishyPopup(Text.literal("No presets found for this tab."), Text.literal("OK"), () -> fishyPopup = null, Text.literal(""), () -> {});
            return;
        }

        int dropdownX = width / 2 + 205;
        int dropdownY = height - 40 - (suffixes.size() * 14) / 2;
        presetDropdown = new DropdownMenu(
            suffixes, dropdownX, dropdownY, 100, 14,
            suffix -> {
                loadPresetForCurrentTab(suffix);
                refreshEntryList();
                refreshCache();
                presetDropdown.setVisible(false);
            }
        );
    }

    private void refreshCache() {
        switch (currentTab) {
            case COMMANDS -> CommandAlias.refresh();
            case KEYBINDS -> KeyShortcut.refresh();
            case CHAT -> ChatReplacement.refresh();
        }
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
        }
    }
}