package me.valkeea.fishyaddons.ui.list;

import java.util.Map;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.feature.qol.KeyShortcut;
import me.valkeea.fishyaddons.ui.widget.FaButton;
import me.valkeea.fishyaddons.util.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

public class KeybindEntryList extends GenericEntryList {
    private static final String PROMPT = "> Press a Key <";

    public KeybindEntryList(MinecraftClient client, int width, int height, int y, int itemHeight, TabbedListScreen parentScreen) {
        super(client, width, height, y, itemHeight, parentScreen);
    }

    @Override
    public String getDefaultInput() {
        return PROMPT;
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
    public void getGuideText(DrawContext context, TextRenderer tr, int x, int y) {
        context.drawTextWithShadow(tr, Text.literal("Keybind"), x - 5, y - 10, 0xFFAAAAAA);
        context.drawTextWithShadow(tr, Text.literal("Executed Command"), x + 110, y - 10, 0xFFAAAAAA);
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
        return this.getHoveredEntry();
    }

    @Override
    public void appendClickableNarrations(NarrationMessageBuilder builder) {
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
        private final GenericEntryList.GenericEntry entry;
        private final KeybindEntryList entryList;
        private final TabbedListScreen parentScreen;
        private final PressAction customPress;

        public KeybindButtonWidget(String keyValue, GenericEntryList.GenericEntry entry, KeybindEntryList entryList, TabbedListScreen parentScreen) {
            super(
                - 40, 5, 100, 20,
                net.minecraft.text.Text.literal(keyValue.isEmpty() ? "Set Key" : Keyboard.getDisplayNameFor(keyValue)),
                b -> {}
            );
            this.keyValue = keyValue;
            this.entry = entry;
            this.entryList = entryList;
            this.parentScreen = parentScreen;
            this.customPress = b -> {
                this.setFocused(true);
                listening = true;
                this.setMessage(net.minecraft.text.Text.literal(PROMPT));
            };
        }

        @Override
        public boolean keyPressed(KeyInput input) {

            if (listening) {
                String keyName = Keyboard.getGlfwKeyName(input.key());
                if (keyName != null) {
                    keyValue = keyName;
                } else {

                    String translationKey = InputUtil.fromKeyCode(input).getTranslationKey();
                    if (translationKey.startsWith("key.keyboard.")) {
                        keyValue = translationKey.substring("key.keyboard.".length()).toUpperCase();
                    } else {
                        keyValue = translationKey.toUpperCase();
                    }
                }
                handleKeyChange();
                listening = false;
                this.setMessage(net.minecraft.text.Text.literal(Keyboard.getDisplayNameFor(keyValue)));
                if (parentScreen != null) parentScreen.refreshEntryList();
                this.setFocused(false);
                return true;
            }
            return super.keyPressed(input);
        }

        @Override
        public boolean mouseClicked(Click click, boolean doubled) {
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
                .anyMatch(e -> e instanceof GenericEntryList.GenericEntry ge
                    && ge != entry
                    && !ge.isNew
                    && ge.input.equals(keyValue));

            if (duplicateExists) {
                parentScreen.showFishyPopup(
                    net.minecraft.text.Text.literal("Keybind '" + keyValue + "' already exists!"),
                    net.minecraft.text.Text.literal("Overwrite Existing"), () -> {

                        entryList.removeEntry(keyValue);
                        entryList.children().stream()
                            .filter(e -> e instanceof GenericEntryList.GenericEntry ge && !ge.isNew && ge.input.equals(keyValue))
                            .findFirst()
                            .ifPresent(entryList::removeEntry);
                        updateEntryAndConfig();
                        parentScreen.fishyPopup = null;

                        if (entry.outputField != null) {
                            entry.outputField.setFocused(true);
                        }
                    },
                    net.minecraft.text.Text.literal("Discard Change"), () -> {
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
                FishyConfig.removeKeybind(oldKey);
            }
            entry.input = keyValue;
            FishyConfig.setKeybind(keyValue, entry.output);

            if (entry.isNew) {
                entry.isNew = false;
                if (parentScreen != null) parentScreen.addingNewEntry = false;
            }
            if (parentScreen != null) parentScreen.refreshEntryList();
            KeyShortcut.refresh();
        }

        public String getKeyValue() {
            return keyValue;
        }
    }

    public boolean handleMouseClicked(Click click, TabbedListScreen screen) {
        GenericEntryList.GenericEntry entry = getHoveredKeybindEntry();

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
