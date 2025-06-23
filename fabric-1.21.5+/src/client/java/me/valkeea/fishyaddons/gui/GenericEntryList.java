package me.valkeea.fishyaddons.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class GenericEntryList extends EntryListWidget<GenericEntryList.GenericEntry> {
    protected final TabbedListScreen parentScreen;

    // Persist full entries and widgets by input key
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

        // Remove entries from entryMap that are no longer in config
        entryMap.keySet().removeIf(key -> !entries.containsKey(key));

        // Update existing entries and add new ones as needed
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            GenericEntry existing = entryMap.get(entry.getKey());
            if (existing != null) {
                // Update value, but keep widget state
                existing.input = entry.getKey();
                existing.output = entry.getValue();
                if (!existing.outputField.getText().equals(entry.getValue())) {
                    existing.outputField.setText(entry.getValue());
                }
                if (existing.inputWidget instanceof TextFieldWidget field) {
                    if (!field.getText().equals(entry.getKey())) {
                        field.setText(entry.getKey());
                    }
                }
            } else {
                // New entry
                entryMap.put(entry.getKey(), new GenericEntry(entry.getKey(), entry.getValue(), this, parentScreen));
            }
        }

        // Rebuild children from entryMap (preserving widget state)
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
            this.addButton = ButtonWidget.builder(Text.literal(entryList.getAddButtonText()), b -> {
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
            }).dimensions(0, -20, 100, 20).build();
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
        }

        private TextFieldWidget createOutputField(String output) {
            TextFieldWidget field = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, 0, 0, 200, 20, Text.literal("Command"));
            field.setText(output);
            field.setMaxLength(256);
            field.setDrawsBackground(false);
            return field;
        }

        private ButtonWidget createSaveButton() {
            return ButtonWidget.builder(Text.literal(entryList.getSaveButtonText())
                .styled(style -> style.withColor(0xFF808080)), b -> handleSave())
                .dimensions(0, 0, 60, 20).build();
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
            // Only show dupePopup if another entry (not this one) already exists with this key
            boolean duplicateExists = entryList.children().stream()
                .anyMatch(e -> e instanceof GenericEntryList.GenericEntry ge
                    && ge != this
                    && !ge.isNew
                    && ge.input.equals(enteredInput));
            if (duplicateExists && !duplicatePopupShown) {
                dupePopup();
                return;
            }
            duplicatePopupShown = false; // reset after successful save
            if (inputWidget instanceof TextFieldWidget field) {
                field.setFocused(false);
            }
            if (inputWidget instanceof TextFieldWidget field) {
                field.setFocused(false);
            }
            outputField.setFocused(false);
            entryList.setEntry(enteredInput, enteredOutput);
            this.input = enteredInput;
            this.output = enteredOutput;

            MinecraftClient.getInstance().currentScreen.setFocused(null);
            parentScreen.addingNewEntry = false;
            parentScreen.refreshEntryList();
        }

        private ButtonWidget createDeleteButton() {
            return ButtonWidget.builder(Text.literal(entryList.getDeleteButtonText())
                .styled(style -> style.withColor(0xFF808080)), b -> handleDelete())
                .dimensions(0, 0, 60, 20).build();
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

        private ButtonWidget createToggleButton() {
            boolean isToggled = entryList.isEntryToggled(input);
            final ButtonWidget[] toggleButtonRef = new ButtonWidget[1];
            ButtonWidget toggleBtn = ButtonWidget.builder(
                Text.literal(isToggled ? entryList.getToggleOnText() : entryList.getToggleOffText())
                    .styled(style -> style.withColor(isToggled ? 0xFFCCFFCC : 0xFFFF8080)),
                b -> handleToggle(toggleButtonRef)
            ).dimensions(0, 0, 40, 20).build();
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
            this.pendingTooltip = null; // Clear at start of render

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
        }

        private void updateTooltipState(int cx, int cy, int cw, int ch, int mouseX, int mouseY) {
            if (parentScreen.fishyPopup == null) {
                String outputText = outputField.getText();
                if (outputText.length() > 31) {
                    int mouseFieldX = cx + 2;
                    int mouseFieldY = cy + 5;
                    int fieldWidth = cw - 4;
                    int fieldHeight = ch - 10 + 10;
                    if (mouseX >= mouseFieldX && mouseX < mouseFieldX + fieldWidth &&
                        mouseY >= mouseFieldY && mouseY < mouseFieldY + fieldHeight) {
                        List<Text> tooltipLines = new java.util.ArrayList<>();
                        for (int i = 0; i < outputText.length(); i += 64) {
                            tooltipLines.add(Text.literal(outputText.substring(i, Math.min(i + 64, outputText.length()))));
                        }
                        this.pendingTooltip = outputText;
                        this.tooltipX = mouseX;
                        this.tooltipY = mouseY;
                        this.tooltipLines = tooltipLines;
                    } else {
                        this.pendingTooltip = null;
                        this.tooltipLines = null;
                    }
                } else {
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
        // Default: TextFieldWidget for input
        TextFieldWidget field = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, 0, 0, 100, 20, Text.literal("Alias"));
        field.setText(input);
        field.setMaxLength(256);
        field.setDrawsBackground(false);
        return field;
    }
}