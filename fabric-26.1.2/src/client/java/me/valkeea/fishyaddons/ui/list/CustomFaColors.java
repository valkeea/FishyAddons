package me.valkeea.fishyaddons.ui.list;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import me.valkeea.fishyaddons.feature.visual.FaColors;
import me.valkeea.fishyaddons.vconfig.config.impl.ColorConfig;
import me.valkeea.fishyaddons.vconfig.ui.manager.ScreenManager;
import me.valkeea.fishyaddons.vconfig.ui.render.VCText;
import me.valkeea.fishyaddons.vconfig.ui.screen.ColorWheel;
import me.valkeea.fishyaddons.vconfig.ui.widget.FaButton;
import me.valkeea.fishyaddons.vconfig.ui.widget.VCTextField;
import me.valkeea.fishyaddons.vconfig.ui.widget.VCVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class CustomFaColors extends Screen {

    private static final float UI_SCALE = 1.0f;  
    private static final int ENTRY_HEIGHT = 28;
    private static final int ENTRY_WIDTH = 300;
    private static final int COLOR_BTN_WIDTH = 40;
    private static final int DEL_BTN_WIDTH = 20;
    private static final int FIELD_WIDTH = 160;
    private static final int FIELD_HEIGHT = 20; 

    private static final String COLOR_TEXT = "Color";

    private final List<Entry> entries = new ArrayList<>();

    private boolean addMode = false;
    private boolean isDraggingScrollbar = false; 

    private int scrollOffset = 0;
    private int maxVisibleEntries = 0;
    private int scrollKnobOffset = 0;

    private AddEntry addEntry = null;    
    private Button addBtn = null;

    public CustomFaColors() {
        super(Component.literal("User Color Overrides"));
    }

    @Override
    protected void init() {
        entries.clear();
        this.clearWidgets();
        for (Map.Entry<String, Integer> entry : ColorConfig.getFaC().entrySet()) {
            Entry e = new Entry(entry.getKey(), entry.getValue());
            entries.add(e);
            this.addRenderableWidget(e.nameField);
            this.addRenderableWidget(e.colorBtn);
            this.addRenderableWidget(e.delBtn);
        }

        int totalEntries = entries.size() + (addMode ? 1 : 0);
        int listTop = 40;
        int listBottom = this.height - 60;
        int listHeight = listBottom - listTop;

        maxVisibleEntries = Math.max(1, listHeight / ENTRY_HEIGHT);
        int maxScroll = Math.max(0, totalEntries - maxVisibleEntries);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        if (scrollOffset < 0) scrollOffset = 0;

        if (!addMode) {
            int addBtnY = 40 + entries.size() * ENTRY_HEIGHT;
            addBtn = new FaButton(
                this.width / 2 - ENTRY_WIDTH / 2, addBtnY, 80, 20,
                Component.literal("Add").withStyle(style -> style.withColor(0xFFCCFFCC)),
                btn -> {
                    addMode = true;
                    addEntry = new AddEntry();
                    this.addRenderableWidget(addEntry.nameField);
                    this.addRenderableWidget(addEntry.colorBtn);
                    this.addRenderableWidget(addEntry.saveBtn);
                    this.addRenderableWidget(addEntry.cancelBtn);
                    this.removeWidget(addBtn);
                }
            );
            this.addRenderableWidget(addBtn);
        }

        this.addRenderableWidget(new FaButton(
            this.width / 2 - ENTRY_WIDTH / 2 + 80, this.height - 40, 80, 20,
            Component.literal("Back").withStyle(style -> style.withColor(0xFF808080)),
            btn -> ScreenManager.openConfigScreen()
        ));

        this.addRenderableWidget(new FaButton(
            this.width / 2 - ENTRY_WIDTH / 2 + 160, this.height - 40, 80, 20,
            Component.literal("Close").withStyle(style -> style.withColor(0xFF808080)),
            btn -> this.onClose()
        ));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);

        context.text(this.font, VCText.header("User Color Overrides", null),
            this.width / 2 - 80, 15, 0xFF55FFFF, false);

        int listTop = 40;
        int listBottom = this.height - 60;
        int listHeight = listBottom - listTop;

        maxVisibleEntries = Math.max(1, listHeight / ENTRY_HEIGHT);

        int totalEntries = entries.size() + (addMode ? 1 : 0);
        int y = listTop;
        int startIdx = scrollOffset;
        int endIdx = Math.min(startIdx + maxVisibleEntries, entries.size());

        for (int i = 0; i < entries.size(); i++) {
            if (i >= startIdx && i < endIdx) {
                entries.get(i).setPosition(this.width / 2 - ENTRY_WIDTH / 2, y);
                entries.get(i).setVisible(true);
                y += ENTRY_HEIGHT;
            } else {
                entries.get(i).setVisible(false);
            }
        }

        if (addMode && addEntry != null) {
            addEntry.updateVisibility();
            if (endIdx == entries.size()) {
                addEntry.setPosition(this.width / 2 - ENTRY_WIDTH / 2, y);
            }
        }

        if (addBtn != null && !addMode) {
            int addBtnY = this.height - 40;
            addBtn.setX(this.width / 2 - ENTRY_WIDTH / 2);
            addBtn.setY(addBtnY);
        }

        if (totalEntries > maxVisibleEntries) {
            renderScrollIndicator(context, this.width / 2 + ENTRY_WIDTH / 2, listTop, listHeight, totalEntries);
        }
    }

    private void renderScrollIndicator(GuiGraphicsExtractor context, int x, int y, int listHeight, int totalEntries) {
        int scrollbarWidth = 3;
        context.fill(x, y, x + scrollbarWidth, y + listHeight, 0x44000000);
        if (totalEntries > maxVisibleEntries) {
            int thumbHeight = Math.max((int)(10 * UI_SCALE), (maxVisibleEntries * listHeight) / totalEntries);
            int thumbY = y + (scrollOffset * (listHeight - thumbHeight)) / (totalEntries - maxVisibleEntries);
            context.fill(x + 1, thumbY, x + scrollbarWidth - 1, thumbY + thumbHeight, VCVisuals.getThemeColor());
            context.fill(x + 1, thumbY + thumbHeight - 1, x + scrollbarWidth - 1, thumbY + thumbHeight, 0xFF000000);
        }
    }  

    private class AddEntry {
        private final VCTextField nameField;
        private int color = 0xFFFFB8E4;
        private Button colorBtn;
        private Button saveBtn;
        private Button cancelBtn;

        public AddEntry() {
            this.nameField = new VCTextField(CustomFaColors.this.font, 0, 0, FIELD_WIDTH, FIELD_HEIGHT, Component.literal("Name"));
            this.nameField.setValue("");
            this.nameField.setEditable(true);
            this.nameField.setMaxLength(32);
            this.nameField.setUIScale(UI_SCALE);
            this.nameField.setFocused(true);
            CustomFaColors.this.setFocused(this.nameField);   

            this.colorBtn = new FaButton(
                0, 0, COLOR_BTN_WIDTH, FIELD_HEIGHT,
                Component.literal(COLOR_TEXT).withStyle(s -> s.withColor(color)),
                btn -> 
                    Minecraft.getInstance().setScreen(new ColorWheel(CustomFaColors.this, this.color, selected -> {
                        this.color = selected;
                        this.colorBtn.setMessage(Component.literal(COLOR_TEXT).withStyle(s -> s.withColor(this.color)));
                        this.nameField.setTextColor(this.color);                        
                        String name = this.nameField.getValue().trim();
                        if (!name.isEmpty()) {
                            FaColors.saveUserEntry(name, this.color & 0xFFFFFFFF);
                            addMode = false;
                            CustomFaColors.this.removeWidget(this.nameField);
                            CustomFaColors.this.removeWidget(this.colorBtn);
                            CustomFaColors.this.removeWidget(this.saveBtn);
                            CustomFaColors.this.removeWidget(this.cancelBtn);
                            CustomFaColors.this.init();
                        }
                    }))
            );

            this.saveBtn = new FaButton(
                0, 0, 20, FIELD_HEIGHT,
                Component.literal("✔").withStyle(s -> s.withColor(0xFFCCFFCC)),
                btn -> {
                    String name = this.nameField.getValue().trim();
                    if (!name.isEmpty()) {
                        FaColors.saveUserEntry(name, color & 0xFFFFFFFF);
                        addMode = false;
                        CustomFaColors.this.removeWidget(this.nameField);
                        CustomFaColors.this.removeWidget(this.colorBtn);
                        CustomFaColors.this.removeWidget(this.saveBtn);
                        CustomFaColors.this.removeWidget(this.cancelBtn);
                        CustomFaColors.this.init();
                    }
                }
            );

            this.cancelBtn = new FaButton(
                0, 0, 20, FIELD_HEIGHT,
                Component.literal("🗑").withStyle(s -> s.withColor(0xFFFF8080)),
                btn -> {
                    addMode = false;
                    CustomFaColors.this.removeWidget(this.nameField);
                    CustomFaColors.this.removeWidget(this.colorBtn);
                    CustomFaColors.this.removeWidget(this.saveBtn);
                    CustomFaColors.this.removeWidget(this.cancelBtn);
                    CustomFaColors.this.init();
                }
            );
        }

        public void setPosition(int x, int y) {
            this.nameField.setX(x);
            this.nameField.setY(y);
            this.colorBtn.setX(x + FIELD_WIDTH);
            this.colorBtn.setY(y);
            this.saveBtn.setX(x + FIELD_WIDTH + COLOR_BTN_WIDTH);
            this.saveBtn.setY(y);
            this.cancelBtn.setX(x + FIELD_WIDTH + COLOR_BTN_WIDTH + 20);
            this.cancelBtn.setY(y);
        }

        public void updateVisibility() {
            if (this.nameField.visible) {
                this.nameField.setVisible(false);
                this.colorBtn.visible = false;
                this.saveBtn.visible = false;
                this.cancelBtn.visible = false;
            }
            setVisible(true);
        }

        public void setVisible(boolean visible) {
            this.nameField.setVisible(visible);
            this.colorBtn.visible = visible;
            this.saveBtn.visible = visible;
            this.cancelBtn.visible = visible;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {

        double mouseX = click.x();
        double mouseY = click.y();

        int listTop = 40;
        int listBottom = this.height - 60;
        int listHeight = listBottom - listTop;
        int scrollbarX = this.width / 2 + ENTRY_WIDTH;
        int scrollbarWidth = Math.max(4, (int)(8 * UI_SCALE));
        int totalEntries = entries.size() + (addMode ? 1 : 0);

        if (totalEntries > maxVisibleEntries && mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth && mouseY >= listTop && mouseY <= listTop + listHeight) {
            int thumbHeight = Math.max((int)(10 * UI_SCALE), (maxVisibleEntries * listHeight) / totalEntries);
            int thumbY = listTop + (scrollOffset * (listHeight - thumbHeight)) / (totalEntries - maxVisibleEntries);

            if (mouseY >= thumbY && mouseY <= thumbY + thumbHeight) {
                isDraggingScrollbar = true;
                scrollKnobOffset = (int)mouseY - thumbY;
            } else {
                isDraggingScrollbar = true;
                scrollKnobOffset = thumbHeight / 2;
                double trackClickY = mouseY - listTop - scrollKnobOffset;
                double scrollPercent = trackClickY / (listHeight - thumbHeight);
                int newScrollOffset = (int)(scrollPercent * (totalEntries - maxVisibleEntries));
                scrollOffset = Math.clamp(newScrollOffset, 0, totalEntries - maxVisibleEntries);
            }
            return true;
        }

        int startIdx = scrollOffset;
        int endIdx = Math.min(startIdx + maxVisibleEntries, entries.size());
        for (int i = startIdx; i < endIdx; i++) {
            if (entries.get(i).mouseClicked()) {
                return true;
            }
        }
        for (Entry entry : entries) {
            entry.saveIfChanged();
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        isDraggingScrollbar = false;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
        if (isDraggingScrollbar) {
            int listTop = 40;
            int listBottom = this.height - 60;
            int listHeight = listBottom - listTop;
            int totalEntries = entries.size() + (addMode ? 1 : 0);
            int thumbHeight = Math.max((int)(10 * UI_SCALE), (maxVisibleEntries * listHeight) / totalEntries);
            int mouseThumbY = (int)click.y() - listTop - scrollKnobOffset;
            double scrollPercent = mouseThumbY / (double)(listHeight - thumbHeight);
            int newScrollOffset = (int)(scrollPercent * (totalEntries - maxVisibleEntries));
            scrollOffset = Math.clamp(newScrollOffset, 0, totalEntries - maxVisibleEntries);
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int totalEntries = entries.size() + (addMode ? 1 : 0);
        if (totalEntries > maxVisibleEntries) {
            scrollOffset -= (int)Math.signum(verticalAmount);
            scrollOffset = Math.clamp(scrollOffset, 0, totalEntries - maxVisibleEntries);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void removed() {
        for (Entry entry : entries) {
            entry.saveIfChanged();
        }
    }

    private class Entry {
        private final VCTextField nameField;
        private int color;
        private String originalName;
        private int originalColor;
        private final Button colorBtn;
        private Button delBtn;
        private boolean changed = false;

        public Entry(String name, int color) {
            this.originalName = name;
            this.originalColor = color;
            this.color = color;
            this.nameField = new VCTextField(CustomFaColors.this.font, 0, 0, FIELD_WIDTH, FIELD_HEIGHT, Component.literal("Name"));
            this.nameField.setValue(name);
            this.nameField.setEditable(true);
            this.nameField.setMaxLength(32);
            this.nameField.setUIScale(UI_SCALE);
            this.nameField.setFocused(false);
            this.colorBtn = new FaButton(
                0, 0, COLOR_BTN_WIDTH, FIELD_HEIGHT,
                Component.literal(COLOR_TEXT).withStyle(s -> s.withColor(color)),
                btn -> 
                    Minecraft.getInstance().setScreen(new ColorWheel(CustomFaColors.this, this.color, selected -> {
                        this.color = selected;
                        btn.setMessage(Component.literal(COLOR_TEXT).withStyle(s -> s.withColor(this.color)));
                        Minecraft.getInstance().setScreen(CustomFaColors.this);
                        this.changed = true;
                        FaColors.saveUserEntry(this.originalName, this.color & 0xFFFFFFFF);
                    }))
            );
            
            this.delBtn = new FaButton(
                0, 0, DEL_BTN_WIDTH, FIELD_HEIGHT,
                Component.literal("❌").withStyle(style -> style.withColor(0xFF808080)),
                btn -> FaColors.deleteUserEntry(this.originalName)
            );
        }

        public void setPosition(int x, int y) {
            this.nameField.setX(x);
            this.nameField.setY(y);
            this.colorBtn.setX(x + FIELD_WIDTH);
            this.colorBtn.setY(y);
            this.delBtn.setX(x + FIELD_WIDTH + COLOR_BTN_WIDTH);
            this.delBtn.setY(y);
        }

        public void setVisible(boolean visible) {
            this.nameField.setVisible(visible);
            this.colorBtn.visible = visible;
            this.delBtn.visible = visible;
        }

        public boolean mouseClicked() {
            return false;
        }

        public void saveIfChanged() {
            String newName = this.nameField.getValue().trim();
            if (!newName.isEmpty() && (changed || !newName.equals(originalName) || color != originalColor)) {
                if (!newName.equals(originalName)) {
                    FaColors.deleteUserEntry(originalName);
                }
                FaColors.saveUserEntry(newName, color & 0xFFFFFFFF);
                this.originalName = newName;
                this.originalColor = color;
                this.changed = false;
            }
        }
    }
}
