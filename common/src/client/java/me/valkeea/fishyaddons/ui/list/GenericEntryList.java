package me.valkeea.fishyaddons.ui.list;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import me.valkeea.fishyaddons.compat.McApi;
import me.valkeea.fishyaddons.ui.GuiUtil;
import me.valkeea.fishyaddons.vconfig.ui.render.RenderUtils;
import me.valkeea.fishyaddons.vconfig.ui.widget.FaButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Generic EntryListWidget for creating UI lists with configurable entries.
 * 
 * Note: This works correctly at runtime despite IDE warnings about generic bounds.
 * The EntryListWidget.Entry class has package-private visibility and complex generic constraints
 * that can cause IDE warnings but don't prevent proper functionality.
 */
public abstract class GenericEntryList extends AbstractSelectionList<GenericEntryList.GenericEntry> {
    protected final TabbedListScreen parentScreen;
    private final Map<String, GenericEntry> entryMap = new LinkedHashMap<>();

    protected GenericEntryList(Minecraft client, int width, int height, int y, int itemHeight, TabbedListScreen parentScreen) {
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

    public abstract void getGuideText(GuiGraphicsExtractor context, Font tr, int x, int y);    
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

                if (!existing.outputField.getValue().equals(entry.getValue())) {
                    existing.outputField.setValue(entry.getValue());
                }
                if (existing.inputWidget instanceof EditBox field && !field.getValue().equals(entry.getKey())) {
                    field.setValue(entry.getKey());
                }
            } else {
                entryMap.put(entry.getKey(), new GenericEntry(entry.getKey(), entry.getValue(), this, parentScreen));
            }
        }

        this.clearEntries();
        for (String key : entries.keySet()) {
            this.addEntry(entryMap.get(key));
        }

        if (parentScreen.addingNewEntry) {
            GenericEntry newEntry = new GenericEntry(getDefaultInput(), getDefaultOutput(), this, parentScreen);
            newEntry.isNew = true;
            this.addEntry(newEntry);
        }
        this.addEntry(new AddEntry(this, parentScreen));
    }

    public boolean doesKeyExist(String key, GenericEntry exclude) {
        return children().stream()
            .anyMatch(e -> e instanceof GenericEntryList.GenericEntry ge
                && ge != exclude
                && !ge.isNew
                && ge.input.equals(key));
    }

    private static final int edgeOffset = 30;

    public static class AddEntry extends GenericEntry {
        private final GenericEntryList entryList;
        private final TabbedListScreen parentScreen;
        private final Button addButton;

        public AddEntry(GenericEntryList entryList, TabbedListScreen parentScreen) {
            super("", "", entryList, parentScreen);
            this.entryList = entryList;
            this.parentScreen = parentScreen;
            this.addButton = new FaButton(
                edgeOffset, -20, 100, 20,
                Component.literal(entryList.getAddButtonText()),
                b -> {
                    if (parentScreen.addingNewEntry) {
                        parentScreen.addingNewEntry = false;
                        parentScreen.refreshEntryList();
                        return;
                    }
                    GenericEntry lastEntry = null;
                    for (AbstractSelectionList.Entry<?> entry : entryList.children()) {
                        if (entry instanceof GenericEntryList.GenericEntry ge && ge.isNew) {
                            lastEntry = ge;
                            break;
                        }
                    }
                    if (lastEntry != null) {
                        String input = lastEntry.inputWidget instanceof EditBox field ? field.getValue().trim() : lastEntry.input;
                        String output = lastEntry.outputField.getValue().trim();
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
        public void extractContent(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float delta) {
            if (parentScreen.addingNewEntry) {
                addButton.setMessage(Component.literal("Cancel").withStyle(style -> style.withColor(0xFFFF8080)));
            } else {
                addButton.setMessage(Component.literal(entryList.getAddButtonText()).withStyle(style -> style.withColor(0xFFCCFFCC)));
            }
            addButton.setX(this.getX());
            addButton.setY(this.getY());
            addButton.extractRenderState(context, mouseX, mouseY, delta);

            if (parentScreen.addingNewEntry && mouseX >= addButton.getX() && mouseX < addButton.getX() + addButton.getWidth()
                && mouseY >= addButton.getY() && mouseY < addButton.getY() + addButton.getHeight()) {
                GuiUtil.fishyTooltip(context, Minecraft.getInstance().font, List.of(
                    Component.literal("This will delete your draft.")
                ), mouseX, mouseY);
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
            return addButton.mouseClicked(click, doubled);
        }
    }

    /**
     * Represents a single entry in the GenericEntryList.
     * Contains input, output, and buttons for saving, deleting, and toggling.
     */
    public static class GenericEntry extends AbstractSelectionList.Entry<GenericEntry> {
        protected String input;
        protected String output;
        protected final Button deleteButton;
        protected final Button toggleButton;
        protected final Button saveButton;
        protected final EditBox outputField;
        protected boolean isNew = false;
        protected final GenericEntryList entryList;
        protected final TabbedListScreen parentScreen;
        protected final Object inputWidget;

        protected String pendingTooltip = null;
        protected int tooltipX = 0;
        protected int tooltipY = 0;
        protected List<Component> tooltipLines = null;
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

        private EditBox createOutputField(String output) {
            EditBox field = new EditBox(Minecraft.getInstance().font, 0, 0, 200, 20, Component.literal("Command"));
            field.setValue(output);
            field.setMaxLength(256);
            field.setBordered(false);
            return field;
        }

        private FaButton createSaveButton() {
            return new FaButton(
                0, 0, 60, 20,
                Component.literal(entryList.getSaveButtonText()).withStyle(style -> style.withColor(0xFF808080)),
                b -> handleSave()
            );
        }

        public void dupePopup() {
            String enteredInput = inputWidget instanceof EditBox field ? field.getValue().trim() : this.input;
            String enteredOutput = outputField.getValue().trim();
                parentScreen.showFishyPopup(
                    Component.literal("Entry with key '" + enteredInput + "' already exists!"),
                    Component.literal("Overwrite Existing"), () -> {
                        entryList.setEntry(enteredInput, enteredOutput);
                        entryList.removeEntry(this);
                        duplicatePopupShown = false;
                        parentScreen.addingNewEntry = false;
                        parentScreen.refreshEntryList();
                        if (inputWidget instanceof EditBox field) {
                            field.setFocused(false);
                        }
                        parentScreen.refreshEntryList();
                        parentScreen.fishyPopup = null; },
                    Component.literal("Discard Entry"), () -> {
                        entryList.removeEntry(this);
                        parentScreen.addingNewEntry = false;
                        duplicatePopupShown = false;
                        parentScreen.refreshEntryList();
                        parentScreen.fishyPopup = null;
                    }); 
        }

        private void handleSave() {
            String enteredInput = inputWidget instanceof EditBox field ? field.getValue().trim() : this.input;
            String enteredOutput = outputField.getValue().trim();
            if (enteredOutput.length() > 255) {
                parentScreen.showFishyPopup(
                    Component.literal("Output too long!"),
                    Component.literal("Continue Editing"), () -> parentScreen.fishyPopup = null,
                    Component.literal("Discard Entry"), () -> { 
                        entryList.removeEntry(this);
                        parentScreen.addingNewEntry = false;
                        parentScreen.refreshEntryList();
                        parentScreen.fishyPopup = null;
                    });
                return;
            }

            if (!entryList.isValidEntry(enteredInput, enteredOutput)) {
                parentScreen.showFishyPopup(
                    Component.literal("Invalid entry! Please fix or discard."),
                    Component.literal("Continue Editing"), () -> parentScreen.fishyPopup = null,
                    Component.literal("Discard Entry"), () -> { 
                        entryList.removeEntry(this);
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

            if (inputWidget instanceof EditBox field) {
                field.setFocused(false);
            }
            outputField.setFocused(false);

            if (keyChanged && !this.isNew) {
                entryList.removeEntry(this.input);
            }

            entryList.setEntry(enteredInput, enteredOutput);
            this.input = enteredInput;
            this.output = enteredOutput;

            McApi.screen().setFocused(null);
            parentScreen.addingNewEntry = false;
            parentScreen.refreshEntryList();
        }

        private FaButton createDeleteButton() {
            return new FaButton(
                0, 0, 60, 20,
                Component.literal(entryList.getDeleteButtonText()).withStyle(style -> style.withColor(0xFF808080)),
                b -> handleDelete()
            );
        }

        public void checkAndSave() {
            handleSave();
        }

        private void handleDelete() {
            if (inputWidget instanceof EditBox field) {
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
            var toggleBtn = new FaButton(
                0, 0, 40, 20,
                Component.literal(isToggled ? entryList.getToggleOnText() : entryList.getToggleOffText())
                    .withStyle(style -> style.withColor(isToggled ? 0xFFCCFFCC : 0xFFFF8080)),
                b -> handleToggle(toggleButtonRef)
            );
            toggleButtonRef[0] = toggleBtn;
            return toggleBtn;
        }

        private void handleToggle(Button[] toggleButtonRef) {
            if (inputWidget instanceof EditBox field) {
                field.setFocused(false);
            }
            outputField.setFocused(false);
            boolean toggled = !entryList.isEntryToggled(input);
            entryList.toggleEntry(input, toggled);
            toggleButtonRef[0].setMessage(
                Component.literal(toggled ? entryList.getToggleOnText() : entryList.getToggleOffText())
                    .withStyle(style2 -> style2.withColor(toggled ? 0xFFCCFFCC : 0xFFFF8080))
            );
        }

        @Override
        public void extractContent(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float delta) {
            int aw = 100, ah = 20;
            int x = this.getX() + edgeOffset;
            int y = this.getY();
            this.pendingTooltip = null;

            int ax = x, ay = y;
            int cx = x + 110, cy = y, cw = 2 * aw, ch = 20;

            renderInputField(context, ax, ay, aw, ah, mouseX, mouseY, delta);
            renderOutputField(context, cx, cy, cw, ch, mouseX, mouseY, delta);
            renderButtons(context, x, y, mouseX, mouseY, delta);
            updateTooltipState(cx, cy, cw, ch, mouseX, mouseY);
        }

        private void renderInputField(GuiGraphicsExtractor context, int ax, int ay, int aw, int ah, int mouseX, int mouseY, float delta) {
            if (inputWidget instanceof EditBox field) {
                int inputColor = field.isFocused() ? 0xFFE2CAE9 : 0xFF555555;
                RenderUtils.border(context, ax, ay, aw, ah, inputColor);
                field.setX(ax + 2);
                field.setY(ay + 5);
                field.extractRenderState(context, mouseX, mouseY, delta);
            } else if (inputWidget instanceof Button button) {
                button.setX(ax + 2);
                button.setY(ay - 1);
                button.extractRenderState(context, mouseX, mouseY, delta);
            }
        }

        @Override
        public boolean keyPressed(KeyEvent input) {
            int keyCode = input.key();
            int modifiers = input.modifiers();
            if (keyCode == 257 || keyCode == 335) {
                handleSave();
                return true;
            }

            if (keyCode == 258) {
                boolean shift = (modifiers & 0x1) != 0;

                List<AbstractWidget> widgets = new java.util.ArrayList<>();

                if (inputWidget instanceof EditBox field) widgets.add(field);
                widgets.add(outputField);
                widgets.add(saveButton);
                widgets.add(deleteButton);
                widgets.add(toggleButton);

                AbstractWidget focused = null;
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

            if (inputWidget instanceof EditBox field && field.isFocused()) {
                return field.keyPressed(input);
            }
            if (outputField.isFocused()) {
                return outputField.keyPressed(input);
            }
            if (saveButton.isFocused()) {
                return saveButton.keyPressed(input);
            }
            if (deleteButton.isFocused()) {
                return deleteButton.keyPressed(input);
            }
            if (toggleButton.isFocused()) {
                return toggleButton.keyPressed(input);
            }

            return false;
        }        

        private void renderOutputField(GuiGraphicsExtractor context, int cx, int cy, int cw, int ch, int mouseX, int mouseY, float delta) {
            int outputColor = outputField.isFocused() ? 0xFFE2CAE9 : 0xFF555555;
            RenderUtils.border(context, cx, cy, cw, ch, outputColor);
            outputField.setX(cx + 2);
            outputField.setY(cy + 5);
            outputField.extractRenderState(context, mouseX, mouseY, delta);
        }

        private void renderButtons(GuiGraphicsExtractor context, int x, int y, int mouseX, int mouseY, float delta) {
            saveButton.setX(x + 320);
            saveButton.setY(y);
            saveButton.extractRenderState(context, mouseX, mouseY, delta);

            deleteButton.setX(x + 385);
            deleteButton.setY(y);
            deleteButton.extractRenderState(context, mouseX, mouseY, delta);

            toggleButton.setX(x + 450);
            toggleButton.setY(y);
            toggleButton.extractRenderState(context, mouseX, mouseY, delta);
        }

        private void updateTooltipState(int cx, int cy, int cw, int ch, int mouseX, int mouseY) {
            if (parentScreen.fishyPopup == null) {
                String outputText = outputField.getValue();
                boolean outputTooltip = false;
                if (outputText.length() > 31) {
                    int mouseFieldX = cx + 2;
                    int mouseFieldY = cy + 5;
                    int fieldWidth = cw - 4;
                    int fieldHeight = ch - 10 + 10;
                    if (mouseX >= mouseFieldX && mouseX < mouseFieldX + fieldWidth &&
                        mouseY >= mouseFieldY && mouseY < mouseFieldY + fieldHeight) {
                        outputTooltip = true;
                        List<Component> lines = new java.util.ArrayList<>();
                        for (int i = 0; i < outputText.length(); i += 64) {
                            lines.add(Component.literal(outputText.substring(i, Math.min(i + 64, outputText.length()))));
                        }
                        this.pendingTooltip = outputText;
                        this.tooltipX = mouseX;
                        this.tooltipY = mouseY;
                        this.tooltipLines = lines;
                    }
                }

                if (!outputTooltip && inputWidget instanceof EditBox field) {
                    String inputText = field.getValue();
                    if (inputText.length() > 31) {
                        int mouseFieldX = field.getX();
                        int mouseFieldY = field.getY();
                        int fieldWidth = field.getWidth();
                        int fieldHeight = field.getHeight();
                        if (mouseX >= mouseFieldX && mouseX < mouseFieldX + fieldWidth &&
                            mouseY >= mouseFieldY && mouseY < mouseFieldY + fieldHeight) {
                            List<Component> lines = new java.util.ArrayList<>();
                            for (int i = 0; i < inputText.length(); i += 64) {
                                lines.add(Component.literal(inputText.substring(i, Math.min(i + 64, inputText.length()))));
                            }
                            this.pendingTooltip = inputText;
                            this.tooltipX = mouseX;
                            this.tooltipY = mouseY;
                            this.tooltipLines = lines;
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
        var field = new EditBox(Minecraft.getInstance().font, 0, 0, 100, 20, Component.literal("Alias"));
        field.setValue(input);
        field.setMaxLength(256);
        field.setBordered(false);
        return field;
    }
}
