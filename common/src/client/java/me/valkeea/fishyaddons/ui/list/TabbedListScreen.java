package me.valkeea.fishyaddons.ui.list;

import java.util.List;
import java.util.Map;

import me.valkeea.fishyaddons.feature.qol.ChatReplacement;
import me.valkeea.fishyaddons.feature.qol.CommandAlias;
import me.valkeea.fishyaddons.feature.qol.FishyPresets;
import me.valkeea.fishyaddons.feature.qol.KeyShortcut;
import me.valkeea.fishyaddons.ui.GuiUtil;
import me.valkeea.fishyaddons.ui.element.DropdownMenu;
import me.valkeea.fishyaddons.ui.element.FishyPopup;
import me.valkeea.fishyaddons.vconfig.config.impl.ShortcutsConfig;
import me.valkeea.fishyaddons.vconfig.ui.manager.ScreenManager;
import me.valkeea.fishyaddons.vconfig.ui.render.VCText;
import me.valkeea.fishyaddons.vconfig.ui.widget.FaButton;
import me.valkeea.fishyaddons.vconfig.ui.widget.FaTextField;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

public class TabbedListScreen extends Screen {
    public enum Tab { COMMANDS, KEYBINDS, CHAT }
    private Tab currentTab = Tab.COMMANDS;
    private AliasEntryList commandEntryList;
    private KeybindEntryList keybindEntryList;
    private ChatEntryList chatEntryList;
    private static final String HEADER = "Aliases, Keybinds, Chat Replacement";

    protected boolean addingNewEntry = false;
    protected FishyPopup fishyPopup = null;
    private DropdownMenu presetDropdown;

    private TextFieldWidget presetNameField;

    public TabbedListScreen(Tab tab) {
        super(Text.literal(HEADER));
        this.currentTab = tab;
    }

    @Override
    protected void init() {
        int listWidth = 550;
        int listHeight = height - 120;
        int listY = 90;

        int w = 80;
        int h = 20;
        int tabBtnY = 40;

        // Tab buttons
        int tx = width / 2 - w - w / 2 - h;

        addDrawableChild(new FaButton(tx, tabBtnY, w, h,
            Text.literal("Commands"),
            btn -> switchTab(Tab.COMMANDS))
        );
        tx += w + h;

        addDrawableChild(new FaButton(tx, tabBtnY, w, h,
            Text.literal("Keybinds"),
            btn -> switchTab(Tab.KEYBINDS))
        );
        tx += w + h;

        addDrawableChild(new FaButton(tx, tabBtnY, w, h,
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

        int x = width / 2 - w;
        int y = height - 30;
        
        addDrawableChild(new FaButton(x, y, w, h,
            Text.literal("Back").styled(style -> style.withColor(0xFFB0B0B0)),
            b -> ScreenManager.openConfigScreen()
        ));
        x += w;

        addDrawableChild(new FaButton(x, y, w, h,
            Text.literal("Close").styled(style -> style.withColor(0xFFB0B0B0)),
            b -> this.close()
        ));
        x += w;

        addDrawableChild(new FaButton(x, y, w, h,
            Text.literal("Load Preset").styled(style -> style.withColor(0xFFE2CAE9)),
            b -> showPresetDropdown()
        ));
        x += w;

        addDrawableChild(new FaButton(x, y, w, h,
            Text.literal("Save Preset").styled(style -> style.withColor(0xFFB0FFB0)),
            b -> showSavePresetPopup()
        ));

        this.client.execute(this::refreshEntryList);
    }

    private void switchTab(Tab tab) {
        this.currentTab = tab;
        if (fishyPopup != null) {
            fishyPopup = null;
        }
        updateTabVisibility();
        this.client.execute(this::refreshEntryList);
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
        super.render(context, mouseX, mouseY, delta);

        var title = VCText.header(HEADER, null);
        GuiUtil.drawScaledCenteredText(context, textRenderer, title, width / 2, 20, 0xFFFFFFFF, 1.0f);

        if (presetDropdown != null && presetDropdown.isVisible()) {
            presetDropdown.render(context, this, mouseX, mouseY);
        }

        if (fishyPopup == null) {
            findAndDrawTooltip(context);
        } else {
            fishyPopup.render(context, this.textRenderer, mouseX, mouseY, delta);
            if (presetNameField != null) {
                presetNameField.setX(fishyPopup.getX() + (fishyPopup.getWidth() - presetNameField.getWidth()) / 2);
                presetNameField.setY(fishyPopup.getY() + 35);
                presetNameField.render(context, mouseX, mouseY, delta);
            }            
        }
    }

    private void findAndDrawTooltip(DrawContext context) {
        var activeList = getActiveList();
        activeList.getGuideText(context, this.textRenderer, width - 710, 85);
        for (var entry : activeList.children()) {
            if (entry instanceof GenericEntryList.GenericEntry ge && ge.pendingTooltip != null) {
                GuiUtil.fishyTooltip(context, this.textRenderer, ge.tooltipLines != null ? ge.tooltipLines : List.of(Text.literal(ge.pendingTooltip)), ge.tooltipX, ge.tooltipY);
                break;
            }
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (fishyPopup != null && presetNameField != null && presetNameField.mouseClicked(click, doubled)) {
            presetNameField.setFocused(true);
            this.setFocused(presetNameField);
            return true;
        }

        if (presetDropdown != null && presetDropdown.isVisible()) {
            return handlePresetClicks(click);
        }

        if (fishyPopup != null) {
            return fishyPopup.mouseClicked(click, doubled);
        }

        if (currentTab == Tab.COMMANDS) {
            if (commandEntryList.handleMouseClicked(click, this)) return true;
        } else if (currentTab == Tab.KEYBINDS) {
            if (keybindEntryList.handleMouseClicked(click, this)) return true;
        } else if (currentTab == Tab.CHAT && chatEntryList.handleMouseClicked(click, this)) {
            return true;
        }
        
        return super.mouseClicked(click, doubled);
    }

    private boolean handlePresetClicks(Click click) {
        if (presetDropdown.mouseClicked(click)) return true;
        
        int x = presetDropdown.getX(); 
        int y = presetDropdown.getY(); 
        int w = presetDropdown.getWidth();
        int h = presetDropdown.getEntryHeight() * presetDropdown.getEntries().size();

        if (!(click.x() >= x && click.x() <= x + w && click.y() >= y && click.y() <= y + h)) {
            presetDropdown.setVisible(false);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (fishyPopup != null && presetNameField != null) {
            return presetNameField.keyPressed(input);
        }
        var entry = getActiveList().getFocused();
        if (entry != null) {
            if (entry.outputField.isFocused() && entry.outputField.keyPressed(input)) return true;
            if (entry.inputWidget instanceof TextFieldWidget field && field.isFocused() && field.keyPressed(input)) return true;
            if (entry.inputWidget instanceof ButtonWidget btn && btn.isFocused() && btn.keyPressed(input)) return true;
            if (entry.keyPressed(input)) return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput chr) {
        if (fishyPopup != null && presetNameField != null) {
            return presetNameField.charTyped(chr);
        }
        var entry = getActiveList().getFocused();
        if (entry != null) {
            if (entry.outputField.isFocused() && entry.outputField.charTyped(chr)) return true;
            if (entry.inputWidget instanceof TextFieldWidget field && field.isFocused() && field.charTyped(chr)) return true;
            if (entry.inputWidget instanceof ButtonWidget btn && btn.isFocused() && btn.charTyped(chr)) return true;
        }
        return super.charTyped(chr);
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

    private GenericEntryList getActiveList() {
        return switch (currentTab) {
            case COMMANDS -> commandEntryList;
            case KEYBINDS -> keybindEntryList;
            case CHAT -> chatEntryList;
        };
    }

    private void loadPresetForCurrentTab(String suffix) {
        switch (currentTab) {
            case COMMANDS -> {
                Map<String, String> map = FishyPresets.loadStringPreset(FishyPresets.PresetType.COMMANDS, suffix);
                if (map != null) {
                    ShortcutsConfig.getAliases().putAll(map);
                    commandEntryList.refreshWithAdd();
                }
            }
            case KEYBINDS -> {
                Map<String, String> map = FishyPresets.loadStringPreset(FishyPresets.PresetType.KEYBINDS, suffix);
                if (map != null) {
                    ShortcutsConfig.getKeybinds().putAll(map);
                    keybindEntryList.refreshWithAdd();
                }
            }
            case CHAT -> {
                Map<String, String> map = FishyPresets.loadStringPreset(FishyPresets.PresetType.CHAT, suffix);
                if (map != null) {
                    ShortcutsConfig.getChat().putAll(map);
                    chatEntryList.refreshWithAdd();
                }
            }
        }
        ShortcutsConfig.save();
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
                FishyPresets.PresetType.COMMANDS, suffix, ShortcutsConfig.getAliases());
            case KEYBINDS -> FishyPresets.saveStringPreset(
                FishyPresets.PresetType.KEYBINDS, suffix, ShortcutsConfig.getKeybinds());
            case CHAT -> FishyPresets.saveStringPreset(
                FishyPresets.PresetType.CHAT, suffix, ShortcutsConfig.getChat());
        }
    }
}
