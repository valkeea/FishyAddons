package me.valkeea.fishyaddons.ui.list;

import java.util.Map;

import com.mojang.blaze3d.platform.InputConstants;

import me.valkeea.fishyaddons.util.Keyboard;
import me.valkeea.fishyaddons.vconfig.config.impl.ShortcutsConfig;
import me.valkeea.fishyaddons.vconfig.ui.widget.FaButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class KeybindEntryList extends GenericEntryList {
    private static final String PROMPT = "> Press a Key <";

    public KeybindEntryList(Minecraft client, int width, int height, int y, int itemHeight, TabbedListScreen parentScreen) {
        super(client, width, height, y, itemHeight, parentScreen);
    }

    @Override
    public String getDefaultInput() {
        return PROMPT;
    }

    @Override
    public Map<String, String> getEntries() {
        return ShortcutsConfig.getKeybinds();
    }

    @Override
    public boolean isEntryToggled(String key) {
        return ShortcutsConfig.isKeybindToggled(key);
    }

    @Override
    public void setEntry(String key, String value) {
        ShortcutsConfig.setKeybind(key, value);
    }

    @Override
    public void removeEntry(String key) {
        ShortcutsConfig.removeKeybind(key);
    }

    @Override
    public void toggleEntry(String key, boolean toggled) {
        ShortcutsConfig.toggleKeybind(key, toggled);
    }

    @Override
    public void getGuideText(GuiGraphicsExtractor context, Font tr, int x, int y) {
        context.text(tr, Component.literal("Keybind"), x - 5, y - 10, 0xFFAAAAAA);
        context.text(tr, Component.literal("Executed Command"), x + 110, y - 10, 0xFFAAAAAA);
    }

    @Override
    public String getAddButtonText() {
        return "+ Add Keybind";
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

    public GenericEntry getHoveredKeybindEntry() {
        return this.getHovered();
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput builder) {
        // Access
    }

    @Override
    public boolean isValidEntry(String key, String value) {
        return !key.contains(PROMPT) && !key.isEmpty() && !value.isEmpty();
    }


    @Override
    public Object createInputWidget(String input, GenericEntry output) {
        return new KeybindButtonWidget(input, output, this, parentScreen);
    }

    
    public static class KeybindButtonWidget extends FaButton {
        private boolean listening = false;
        private String keyValue;
        private final GenericEntry entry;
        private final KeybindEntryList entryList;
        private final TabbedListScreen parentScreen;
        private final OnPress customPress;

        public KeybindButtonWidget(String keyValue, GenericEntry entry, KeybindEntryList entryList, TabbedListScreen parentScreen) {
            super(
                - 40, 5, 100, 20,
                net.minecraft.network.chat.Component.literal(keyValue.isEmpty() ? "Set Key" : Keyboard.getDisplayNameFor(keyValue)),
                b -> {}
            );
            this.keyValue = keyValue;
            this.entry = entry;
            this.entryList = entryList;
            this.parentScreen = parentScreen;
            this.customPress = b -> {
                this.setFocused(true);
                listening = true;
                this.setMessage(net.minecraft.network.chat.Component.literal(PROMPT));
            };
        }

        @Override
        public boolean keyPressed(KeyEvent input) {

            if (listening) {
                String keyName = Keyboard.getGlfwKeyName(input.key());
                if (keyName != null) {
                    keyValue = keyName;
                } else {

                    String translationKey = InputConstants.getKey(input).getName();
                    if (translationKey.startsWith("key.keyboard.")) {
                        keyValue = translationKey.substring("key.keyboard.".length()).toUpperCase();
                    } else {
                        keyValue = translationKey.toUpperCase();
                    }
                }
                handleKeyChange();
                listening = false;
                this.setMessage(net.minecraft.network.chat.Component.literal(Keyboard.getDisplayNameFor(keyValue)));
                if (parentScreen != null) parentScreen.refreshEntryList();
                this.setFocused(false);
                return true;
            }
            return super.keyPressed(input);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
            if (listening) {
                keyValue = "MOUSE" + click.button();
                handleKeyChange();
                listening = false;
                return true;
            }
            if (this.isMouseOver(click.x(), click.y())) {
                customPress.onPress(this);
                return true;
            }
            return false;
        }

        private void handleKeyChange() {

            boolean duplicateExists = entryList.children().stream()
                .anyMatch(e -> e instanceof GenericEntry ge
                    && ge != entry
                    && !ge.isNew
                    && ge.input.equals(keyValue));

            if (duplicateExists) {
                parentScreen.showFishyPopup(
                    net.minecraft.network.chat.Component.literal("Keybind '" + keyValue + "' already exists!"),
                    net.minecraft.network.chat.Component.literal("Overwrite Existing"), () -> {

                        entryList.removeEntry(keyValue);
                        entryList.children().stream()
                            .filter(e -> e instanceof GenericEntry ge && !ge.isNew && ge.input.equals(keyValue))
                            .findFirst()
                            .ifPresent(entryList::removeEntry);
                        updateEntryAndConfig();
                        parentScreen.fishyPopup = null;

                        if (entry.outputField != null) {
                            entry.outputField.setFocused(true);
                        }
                    },
                    net.minecraft.network.chat.Component.literal("Discard Change"), () -> {
                        entryList.removeEntry(entry);
                        parentScreen.addingNewEntry = false;
                        parentScreen.fishyPopup = null;
                        parentScreen.refreshEntryList();
                    }
                );
                entry.duplicatePopupShown = true; 
            } else {
                updateEntryAndConfig();
            }
        }

        private void updateEntryAndConfig() {
            String oldKey = entry.input;
            if (!oldKey.equals(keyValue) && !oldKey.isEmpty()) {
                ShortcutsConfig.removeKeybind(oldKey);
            }
            entry.input = keyValue;
            ShortcutsConfig.setKeybind(keyValue, entry.output);

            if (entry.isNew) {
                entry.isNew = false;
                if (parentScreen != null) parentScreen.addingNewEntry = false;
            }
            if (parentScreen != null) parentScreen.refreshEntryList();
        }

        public String getKeyValue() {
            return keyValue;
        }
    }

    public boolean handleMouseClicked(MouseButtonEvent click, TabbedListScreen screen) {
        GenericEntry entry = getHoveredKeybindEntry();

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
