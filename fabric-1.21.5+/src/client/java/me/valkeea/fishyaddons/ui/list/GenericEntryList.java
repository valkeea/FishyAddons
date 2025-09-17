package me.valkeea.fishyaddons.ui.list;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import me.valkeea.fishyaddons.ui.GuiUtil;
import me.valkeea.fishyaddons.ui.widget.FaButton;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public abstract class GenericEntryList extends EntryListWidget<GenericEntryList.GenericEntry> {
    protected final TabbedListScreen parentScreen;
    private final Map<String, GenericEntry> entryMap = new LinkedHashMap<>();

    protected GenericEntryList(MinecraftClient client, int width, int height, int y, int itemHeight, TabbedListScreen parentScreen) {
        super(client, width, height, y, itemHeight);
        this.parentScreen = parentScreen;
    }

    @Override
    public int getRowWidth() {
        return 550;
    }

    public abstract Map<String, String> getEntries();
    public abstract boolean isEntryToggled(String key);
    public abstract void setEntry(String key, String value);
    public abstract void removeEntry(String key);
    public abstract void toggleEntry(String key, boolean toggled);

    public abstract void getGuideText(DrawContext context, TextRenderer tr, int x, int y);    
    public abstract String getAddButtonText();
    public abstract String getSaveButtonText();
    public abstract String getDeleteButtonText();
    public abstract String getToggleOnText();
    public abstract String getToggleOffText();

    public abstract boolean isValidEntry(String input, String output);
    public String getDefaultInput() { return "/"; }
    public String getDefaultOutput() { return "/"; }

    public void refreshWithAdd() {
        Map<String, String> entries = getEntries();

        entryMap.keySet().removeIf(key -> !entries.containsKey(key));

        for (Map.Entry<String, String> entry : entries.entrySet()) {

            GenericEntry existing = entryMap.get(entry.getKey());
            if (existing != null) {
                existing.input = entry.getKey();
                existing.output = entry.getValue();

                if (!existing.outputField.getText().equals(entry.getValue())) {
                    existing.outputField.setText(entry.getValue());
                }
                if (existing.inputWidget instanceof TextFieldWidget field && !field.getText().equals(entry.getKey())) {
                    field.setText(entry.getKey());
                }
            } else {
                entryMap.put(entry.getKey(), new GenericEntry(entry.getKey(), entry.getValue(), this, parentScreen));
            }
        }

        this.children().clear();
        for (String key : entries.keySet()) {
            this.children().add(entryMap.get(key));
        }

        if (parentScreen.addingNewEntry) {
            GenericEntry newEntry = new GenericEntry(getDefaultInput(), getDefaultOutput(), this, parentScreen);
            newEntry.isNew = true;
            this.children().add(newEntry);
        }
        this.children().add(new AddEntry(this, parentScreen));
    }

    public boolean doesKeyExist(String key, GenericEntry exclude) {
        return children().stream()
            .anyMatch(e -> e instanceof GenericEntryList.GenericEntry ge
                && ge != exclude
                && !ge.isNew
                && ge.input.equals(key));
    }

    public static class AddEntry extends GenericEntry {
        private final GenericEntryList entryList;
        private final TabbedListScreen parentScreen;
        private final ButtonWidget addButton;

        public AddEntry(GenericEntryList entryList, TabbedListScreen parentScreen) {
            super("", "", entryList, parentScreen);
            this.entryList = entryList;
            this.parentScreen = parentScreen;
            this.addButton = new FaButton(
                0, -20, 100, 20,
                Text.literal(entryList.getAddButtonText()),
                b -> {
                    if (parentScreen.addingNewEntry) {
                        parentScreen.addingNewEntry = false;
                        parentScreen.refreshEntryList();
                        return;
                    }
                    GenericEntry lastEntry = null;
                    for (EntryListWidget.Entry<?> entry : entryList.children()) {
                        if (entry instanceof GenericEntryList.GenericEntry ce && ce.isNew) {
                            lastEntry = ce;
                            break;
                        }
                    }
                    if (lastEntry != null) {
                        String input = lastEntry.inputWidget instanceof TextFieldWidget field ? field.getText().trim() : lastEntry.input;
                        String output = lastEntry.outputField.getText().trim();
                        if (!entryList.isValidEntry(input, output)) {
                            parentScreen.addingNewEntry = false;
                            parentScreen.refreshEntryList();
                            return;
                        }
                    }
                    if (!parentScreen.addingNewEntry) {
                        parentScreen.addingNewEntry = true;
                        parentScreen.refreshEntryList();
                    }
                }
            );
        }

        public boolean isSelectable() {
            return false;
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovered, float delta) {
            if (parentScreen.addingNewEntry) {
                addButton.setMessage(Text.literal("Cancel").styled(style -> style.withColor(0xFFFF8080)));
            } else {
                addButton.setMessage(Text.literal(entryList.getAddButtonText()).styled(style -> style.withColor(0xFFCCFFCC)));
            }
            addButton.setX(x);
            addButton.setY(y);
            addButton.render(context, mouseX, mouseY, delta);

            if (parentScreen.addingNewEntry) {
                if (mouseX >= addButton.getX() && mouseX < addButton.getX() + addButton.getWidth()
                    && mouseY >= addButton.getY() && mouseY < addButton.getY() + addButton.getHeight()) {
                    GuiUtil.fishyTooltip(context, MinecraftClient.getInstance().textRenderer, List.of(
                        Text.literal("This will delete your draft.")
                    ), mouseX, mouseY);
                }
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return addButton.mouseClicked(mouseX, mouseY, button);
        }
    }

    /**
     * Represents a single entry in the GenericEntryList.
     * Contains input, output, and buttons for saving, deleting, and toggling.
     */
    public static class GenericEntry extends EntryListWidget.Entry<GenericEntry> {
        protected String input;
        protected String output;
        protected final ButtonWidget deleteButton;
        protected final ButtonWidget toggleButton;
        protected final ButtonWidget saveButton;
        protected final TextFieldWidget outputField;
        protected boolean isNew = false;
        protected final GenericEntryList entryList;
        protected final TabbedListScreen parentScreen;
        protected final Object inputWidget;
        protected final ButtonWidget extraButton;

        protected String pendingTooltip = null;
        protected int tooltipX = 0, tooltipY = 0;
        protected List<Text> tooltipLines = null;
        protected boolean duplicatePopupShown = false;

        public GenericEntry(String input, String output, GenericEntryList entryList, TabbedListScreen parentScreen) {
            this.input = input;
            this.output = output;
            this.entryList = entryList;
            this.parentScreen = parentScreen;

            this.inputWidget = entryList.createInputWidget(input, this);

            this.outputField = createOutputField(output);

            this.saveButton = createSaveButton();
            this.deleteButton = createDeleteButton();
            this.toggleButton = createToggleButton();
            this.extraButton = entryList.createExtraButton(this);
        }

        private TextFieldWidget createOutputField(String output) {
            TextFieldWidget field = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, 0, 0, 200, 20, Text.literal("Command"));
            field.setText(output);
            field.setMaxLength(256);
            field.setDrawsBackground(false);
            return field;
        }

        private FaButton createSaveButton() {
            return new FaButton(
                0, 0, 60, 20,
                Text.literal(entryList.getSaveButtonText()).styled(style -> style.withColor(0xFF808080)),
                b -> handleSave()
            );
        }

        public void dupePopup() {
            String enteredInput = inputWidget instanceof TextFieldWidget field ? field.getText().trim() : this.input;
            String enteredOutput = outputField.getText().trim();
                parentScreen.showFishyPopup(
                    Text.literal("Entry with key '" + enteredInput + "' already exists!"),
                    Text.literal("Overwrite Existing"), () -> {
                        entryList.setEntry(enteredInput, enteredOutput);
                        entryList.children().remove(this);
                        duplicatePopupShown = false;
                        parentScreen.addingNewEntry = false;
                        parentScreen.refreshEntryList();
                        if (inputWidget instanceof TextFieldWidget field) {
                            field.setFocused(false);
                        }
                        parentScreen.refreshEntryList();
                        parentScreen.fishyPopup = null; },
                    Text.literal("Discard Entry"), () -> {
                        entryList.children().remove(this);
                        parentScreen.addingNewEntry = false;
                        duplicatePopupShown = false;
                        parentScreen.refreshEntryList();
                        parentScreen.fishyPopup = null;
                    }); 
        }

        private void handleSave() {
            String enteredInput = inputWidget instanceof TextFieldWidget field ? field.getText().trim() : this.input;
            String enteredOutput = outputField.getText().trim();
            if (enteredOutput.length() > 255) {
                parentScreen.showFishyPopup(
                    Text.literal("Output too long!"),
                    Text.literal("Continue Editing"), () -> parentScreen.fishyPopup = null,
                    Text.literal("Discard Entry"), () -> { 
                        entryList.children().remove(this);
                        parentScreen.addingNewEntry = false;
                        parentScreen.refreshEntryList();
                        parentScreen.fishyPopup = null;
                    });
                return;
            }

            if (!entryList.isValidEntry(enteredInput, enteredOutput)) {
                parentScreen.showFishyPopup(
                    Text.literal("Invalid entry! Please fix or discard."),
                    Text.literal("Continue Editing"), () -> parentScreen.fishyPopup = null,
                    Text.literal("Discard Entry"), () -> { 
                        entryList.children().remove(this);
                        parentScreen.addingNewEntry = false;
                        parentScreen.refreshEntryList();
                        parentScreen.fishyPopup = null;
                    });
                return;
            }

            boolean keyChanged = !enteredInput.equals(this.input);
            boolean duplicateExists = entryList.children().stream()
                .anyMatch(e -> e instanceof GenericEntryList.GenericEntry ge
                    && ge != this
                    && !ge.isNew
                    && ge.input.equals(enteredInput));

            if (duplicateExists && !duplicatePopupShown) {
                dupePopup();
                return;
            }
            duplicatePopupShown = false;

            if (inputWidget instanceof TextFieldWidget field) {
                field.setFocused(false);
            }
            outputField.setFocused(false);

            if (keyChanged && !this.isNew) {
                entryList.removeEntry(this.input);
            }

            entryList.setEntry(enteredInput, enteredOutput);
            this.input = enteredInput;
            this.output = enteredOutput;

            MinecraftClient.getInstance().currentScreen.setFocused(null);
            parentScreen.addingNewEntry = false;
            parentScreen.refreshEntryList();
        }

        private FaButton createDeleteButton() {
            return new FaButton(
                0, 0, 60, 20,
                Text.literal(entryList.getDeleteButtonText()).styled(style -> style.withColor(0xFF808080)),
                b -> handleDelete()
            );
        }

        public void checkAndSave() {
            handleSave();
        }

        private void handleDelete() {
            if (inputWidget instanceof TextFieldWidget field) {
                field.setFocused(false);
            }

            outputField.setFocused(false);
            if (isNew) {
                parentScreen.addingNewEntry = false;
                parentScreen.refreshEntryList();
            } else {
                entryList.removeEntry(input);
                if (parentScreen != null) parentScreen.refreshEntryList();
            }
        }

        private FaButton createToggleButton() {
            boolean isToggled = entryList.isEntryToggled(input);
            final FaButton[] toggleButtonRef = new FaButton[1];
            FaButton toggleBtn = new FaButton(
                0, 0, 40, 20,
                Text.literal(isToggled ? entryList.getToggleOnText() : entryList.getToggleOffText())
                    .styled(style -> style.withColor(isToggled ? 0xFFCCFFCC : 0xFFFF8080)),
                b -> handleToggle(toggleButtonRef)
            );
            toggleButtonRef[0] = toggleBtn;
            return toggleBtn;
        }

        private void handleToggle(ButtonWidget[] toggleButtonRef) {
            if (inputWidget instanceof TextFieldWidget field) {
                field.setFocused(false);
            }
            outputField.setFocused(false);
            boolean toggled = !entryList.isEntryToggled(input);
            entryList.toggleEntry(input, toggled);
            toggleButtonRef[0].setMessage(
                Text.literal(toggled ? entryList.getToggleOnText() : entryList.getToggleOffText())
                    .styled(style2 -> style2.withColor(toggled ? 0xFFCCFFCC : 0xFFFF8080))
            );
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovered, float delta) {
            this.pendingTooltip = null;

            int ax = x, ay = y, aw = 100, ah = 20;
            int cx = x + 110, cy = y, cw = 200, ch = 20;

            renderInputField(context, ax, ay, aw, ah, mouseX, mouseY, delta);
            renderOutputField(context, cx, cy, cw, ch, mouseX, mouseY, delta);
            renderButtons(context, x, y, mouseX, mouseY, delta);
            updateTooltipState(cx, cy, cw, ch, mouseX, mouseY);
        }

        private void renderInputField(DrawContext context, int ax, int ay, int aw, int ah, int mouseX, int mouseY, float delta) {
            if (inputWidget instanceof TextFieldWidget field) {
                int inputColor = field.isFocused() ? 0xFFE2CAE9 : 0xFF555555;
                GuiUtil.drawBox(context, ax, ay, aw, ah, inputColor);
                field.setX(ax + 2);
                field.setY(ay + 5);
                field.render(context, mouseX, mouseY, delta);
            } else if (inputWidget instanceof ButtonWidget button) {
                button.setX(ax + 2);
                button.setY(ay - 1);
                button.render(context, mouseX, mouseY, delta);
            }
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == 257 || keyCode == 335) {
                handleSave();
                return true;
            }

            if (keyCode == 258) {
                boolean shift = (modifiers & 0x1) != 0;

                java.util.List<net.minecraft.client.gui.widget.ClickableWidget> widgets = new java.util.ArrayList<>();

                if (inputWidget instanceof TextFieldWidget field) widgets.add(field);
                widgets.add(outputField);
                widgets.add(saveButton);
                widgets.add(deleteButton);
                widgets.add(toggleButton);
                if (extraButton != null) widgets.add(extraButton);

                net.minecraft.client.gui.widget.ClickableWidget focused = null;
                for (var w : widgets) {
                    if (w.isFocused()) {
                        focused = w;
                        break;
                    }
                }

                int idx = widgets.indexOf(focused);
                int nextIdx = shift ? (idx - 1 + widgets.size()) % widgets.size() : (idx + 1) % widgets.size();

                for (var w : widgets) w.setFocused(false);
                widgets.get(nextIdx).setFocused(true);
                return true;
            }

            if (inputWidget instanceof TextFieldWidget field && field.isFocused()) {
                return field.keyPressed(keyCode, scanCode, modifiers);
            }
            if (outputField.isFocused()) {
                return outputField.keyPressed(keyCode, scanCode, modifiers);
            }
            if (saveButton.isFocused()) {
                return saveButton.keyPressed(keyCode, scanCode, modifiers);
            }
            if (deleteButton.isFocused()) {
                return deleteButton.keyPressed(keyCode, scanCode, modifiers);
            }
            if (toggleButton.isFocused()) {
                return toggleButton.keyPressed(keyCode, scanCode, modifiers);
            }
            if (extraButton != null && extraButton.isFocused()) {
                return extraButton.keyPressed(keyCode, scanCode, modifiers);
            }

            return false;
        }        

        private void renderOutputField(DrawContext context, int cx, int cy, int cw, int ch, int mouseX, int mouseY, float delta) {
            int outputColor = outputField.isFocused() ? 0xFFE2CAE9 : 0xFF555555;
            GuiUtil.drawBox(context, cx, cy, cw, ch, outputColor);
            outputField.setX(cx + 2);
            outputField.setY(cy + 5);
            outputField.render(context, mouseX, mouseY, delta);
        }

        private void renderButtons(DrawContext context, int x, int y, int mouseX, int mouseY, float delta) {
            saveButton.setX(x + 320);
            saveButton.setY(y);
            saveButton.render(context, mouseX, mouseY, delta);

            deleteButton.setX(x + 385);
            deleteButton.setY(y);
            deleteButton.render(context, mouseX, mouseY, delta);

            toggleButton.setX(x + 450);
            toggleButton.setY(y);
            toggleButton.render(context, mouseX, mouseY, delta);

            if (extraButton != null) {
                extraButton.setX(x + 495);
                extraButton.setY(y);
                extraButton.render(context, mouseX, mouseY, delta);
            }
        }

        private void updateTooltipState(int cx, int cy, int cw, int ch, int mouseX, int mouseY) {
            if (parentScreen.fishyPopup == null) {
                String outputText = outputField.getText();
                boolean outputTooltip = false;
                if (outputText.length() > 31) {
                    int mouseFieldX = cx + 2;
                    int mouseFieldY = cy + 5;
                    int fieldWidth = cw - 4;
                    int fieldHeight = ch - 10 + 10;
                    if (mouseX >= mouseFieldX && mouseX < mouseFieldX + fieldWidth &&
                        mouseY >= mouseFieldY && mouseY < mouseFieldY + fieldHeight) {
                        outputTooltip = true;
                        List<Text> tooltipLines = new java.util.ArrayList<>();
                        for (int i = 0; i < outputText.length(); i += 64) {
                            tooltipLines.add(Text.literal(outputText.substring(i, Math.min(i + 64, outputText.length()))));
                        }
                        this.pendingTooltip = outputText;
                        this.tooltipX = mouseX;
                        this.tooltipY = mouseY;
                        this.tooltipLines = tooltipLines;
                    }
                }

                if (!outputTooltip && inputWidget instanceof TextFieldWidget field) {
                    String inputText = field.getText();
                    if (inputText.length() > 31) {
                        int mouseFieldX = field.getX();
                        int mouseFieldY = field.getY();
                        int fieldWidth = field.getWidth();
                        int fieldHeight = field.getHeight();
                        if (mouseX >= mouseFieldX && mouseX < mouseFieldX + fieldWidth &&
                            mouseY >= mouseFieldY && mouseY < mouseFieldY + fieldHeight) {
                            List<Text> tooltipLines = new java.util.ArrayList<>();
                            for (int i = 0; i < inputText.length(); i += 64) {
                                tooltipLines.add(Text.literal(inputText.substring(i, Math.min(i + 64, inputText.length()))));
                            }
                            this.pendingTooltip = inputText;
                            this.tooltipX = mouseX;
                            this.tooltipY = mouseY;
                            this.tooltipLines = tooltipLines;
                            return;
                        }
                    }
                }
                if (!outputTooltip) {
                    this.pendingTooltip = null;
                    this.tooltipLines = null;
                }
            } else {
                this.pendingTooltip = null;
                this.tooltipLines = null;
            }
        }
    }

    public Object createInputWidget(String input, GenericEntry output) {
        TextFieldWidget field = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, 0, 0, 100, 20, Text.literal("Alias"));
        field.setText(input);
        field.setMaxLength(256);
        field.setDrawsBackground(false);
        return field;
    }

    public ButtonWidget createExtraButton(GenericEntry entry) {
        return null;
    }
}