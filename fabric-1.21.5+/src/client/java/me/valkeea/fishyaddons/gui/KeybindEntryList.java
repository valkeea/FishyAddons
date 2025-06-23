package me.valkeea.fishyaddons.gui;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.handler.KeyShortcut;
import me.valkeea.fishyaddons.util.KeyUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.client.util.InputUtil;

import java.util.Map;

public class KeybindEntryList extends GenericEntryList {
    public KeybindEntryList(MinecraftClient client, int width, int height, int y, int itemHeight, TabbedListScreen parentScreen) {
        super(client, width, height, y, itemHeight, parentScreen);
    }

    @Override
    public String getDefaultInput() {
        return "> Press a Key <";
    }

    @Override
    public Map<String, String> getEntries() {
        return FishyConfig.getKeybinds();
    }

    @Override
    public boolean isEntryToggled(String key) {
        return FishyConfig.isKeybindToggled(key);
    }

    @Override
    public void setEntry(String key, String value) {
        FishyConfig.setKeybind(key, value);;
    }

    @Override
    public void removeEntry(String key) {
        FishyConfig.removeKeybind(key);
    }

    @Override
    public void toggleEntry(String key, boolean toggled) {
        FishyConfig.toggleKeybind(key, toggled);
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

    public GenericEntryList.GenericEntry getHoveredKeybindEntry() {
        return (GenericEntryList.GenericEntry) this.getHoveredEntry();
    }

    @Override
    public void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {}

    @Override
    public boolean isValidEntry(String key, String value) {
        return !key.contains("> Press a Key <") && !key.isEmpty() && !value.isEmpty();
    }


    @Override
    public Object createInputWidget(String input, GenericEntry output) {
        return new KeybindButtonWidget(input, output, this, parentScreen);
    }

    
    public static class KeybindButtonWidget extends ButtonWidget {
        private boolean listening = false;
        private String keyValue;
        private final GenericEntryList.GenericEntry entry;
        private final KeybindEntryList entryList;
        private final TabbedListScreen parentScreen;
        private final PressAction customPress;

        public KeybindButtonWidget(String keyValue, GenericEntryList.GenericEntry entry, KeybindEntryList entryList, TabbedListScreen parentScreen) {
            super(
                - 40, 5, 100, 20,
                Text.literal(keyValue.isEmpty() ? "Set Key" : keyValue),
                b -> {}, // Manual click
                DEFAULT_NARRATION_SUPPLIER
            );
            this.keyValue = keyValue;
            this.entry = entry;
            this.entryList = entryList;
            this.parentScreen = parentScreen;
            this.customPress = b -> {
                this.setFocused(true);
                listening = true;
                this.setMessage(Text.literal("> Press a Key <"));
            };
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (listening) {
                String keyName = KeyUtil.getGlfwKeyName(keyCode);
                if (keyName != null) {
                    keyValue = keyName;
                } else {
                    // fallback to InputUtil for regular keys
                    String translationKey = InputUtil.fromKeyCode(keyCode, scanCode).getTranslationKey();
                    if (translationKey.startsWith("key.keyboard.")) {
                        keyValue = translationKey.substring("key.keyboard.".length()).toUpperCase();
                    } else {
                        keyValue = translationKey.toUpperCase();
                    }
                }
                handleKeyChange();
                listening = false;
                if (keyValue.startsWith("MOUSE")) {
                    int btn = -1;
                    try { btn = Integer.parseInt(keyValue.substring(5)); } catch (Exception ignored) {}
                    this.setMessage(Text.literal("Mouse " + (btn + 1)));
                } else {
                    this.setMessage(Text.literal(String.valueOf(KeyUtil.getKeyCodeFromString(keyValue))));
                }
                if (parentScreen != null) parentScreen.refreshEntryList();
                this.setFocused(false);
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (listening) {
                keyValue = "MOUSE" + button;
                handleKeyChange();
                listening = false;
                return true;
            }
            if (this.isMouseOver(mouseX, mouseY)) {
                customPress.onPress(this);
                return true;
            }
            return false;
        }

        private void handleKeyChange() {
            // Check for duplicate keybind (excluding this entry)
            boolean duplicateExists = entryList.children().stream()
                .anyMatch(e -> e instanceof GenericEntryList.GenericEntry ge
                    && ge != entry
                    && !ge.isNew
                    && ge.input.equals(keyValue));

            if (duplicateExists) {
                parentScreen.showFishyPopup(
                    Text.literal("Keybind '" + keyValue + "' already exists!"),
                    Text.literal("Overwrite Existing"), () -> {
                        // Remove the old entry from config and UI, then update
                        entryList.removeEntry(keyValue);
                        entryList.children().removeIf(e -> e instanceof GenericEntryList.GenericEntry ge && !ge.isNew && ge.input.equals(keyValue));
                        updateEntryAndConfig();
                        parentScreen.fishyPopup = null;
                        // Focus the output field
                        if (entry.outputField != null) {
                            entry.outputField.setFocused(true);
                        }
                    },
                    Text.literal("Discard Change"), () -> {
                        // Revert to previous value and focus this button for another try
                        entryList.children().remove(entry);
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
                FishyConfig.removeKeybind(oldKey);
            }
            entry.input = keyValue;
            FishyConfig.setKeybind(keyValue, entry.output);
            // If this was the "add new" entry, mark it as not new
            if (entry.isNew) {
                entry.isNew = false;
                if (parentScreen != null) parentScreen.addingNewEntry = false;
            }
            if (parentScreen != null) parentScreen.refreshEntryList();
            KeyShortcut.refreshCache();
        }

        public String getKeyValue() {
            return keyValue;
        }
    }
}