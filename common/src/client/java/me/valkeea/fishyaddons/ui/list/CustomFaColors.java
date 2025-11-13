package me.valkeea.fishyaddons.ui.list;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import me.valkeea.fishyaddons.feature.visual.FaColors;
import me.valkeea.fishyaddons.ui.ColorWheel;
import me.valkeea.fishyaddons.ui.VCText;
import me.valkeea.fishyaddons.ui.widget.FaButton;
import me.valkeea.fishyaddons.ui.widget.VCTextField;
import me.valkeea.fishyaddons.ui.widget.VCVisuals;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class CustomFaColors extends Screen {
    private static final float UI_SCALE = 1.0f;  
    private static final int ENTRY_HEIGHT = 28;
    private static final int ENTRY_WIDTH = 300;
    private static final int COLOR_BTN_WIDTH = 40;
    private static final int DEL_BTN_WIDTH = 20;
    private static final int FIELD_WIDTH = 160;
    private static final int FIELD_HEIGHT = 20; 

    private static final String COLOR_TEXT = "Color";

    private final Screen parent;
    private final List<Entry> entries = new ArrayList<>();

    private boolean addMode = false;
    private boolean isDraggingScrollbar = false; 

    private int scrollOffset = 0;
    private int maxVisibleEntries = 0;
    private int scrollbarThumbOffset = 0;

    private AddEntry addEntry = null;    
    private ButtonWidget addBtn = null;

    public CustomFaColors(Screen parent) {
        super(Text.literal("User Color Overrides"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        entries.clear();
        this.clearChildren();
        for (Map.Entry<String, Integer> entry : me.valkeea.fishyaddons.config.FishyConfig.getFaC().entrySet()) {
            Entry e = new Entry(entry.getKey(), entry.getValue());
            entries.add(e);
            this.addDrawableChild(e.nameField);
            this.addDrawableChild(e.colorBtn);
            this.addDrawableChild(e.delBtn);
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
                Text.literal("Add").styled(style -> style.withColor(0xFFCCFFCC)),
                btn -> {
                    addMode = true;
                    addEntry = new AddEntry();
                    this.addDrawableChild(addEntry.nameField);
                    this.addDrawableChild(addEntry.colorBtn);
                    this.addDrawableChild(addEntry.saveBtn);
                    this.addDrawableChild(addEntry.cancelBtn);
                    this.remove(addBtn);
                }
            );
            this.addDrawableChild(addBtn);
        }

        this.addDrawableChild(new FaButton(
            this.width / 2 - ENTRY_WIDTH / 2 + 80, this.height - 40, 80, 20,
            Text.literal("Back").styled(style -> style.withColor(0xFF808080)),
            btn -> client.setScreen(parent)
        ));

        this.addDrawableChild(new FaButton(
            this.width / 2 - ENTRY_WIDTH / 2 + 160, this.height - 40, 80, 20,
            Text.literal("Close").styled(style -> style.withColor(0xFF808080)),
            btn -> this.close()
        ));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        context.drawText(this.textRenderer, VCText.header("User Color Overrides", null),
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

    private void renderScrollIndicator(DrawContext context, int x, int y, int listHeight, int totalEntries) {
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
        private ButtonWidget colorBtn;
        private ButtonWidget saveBtn;
        private ButtonWidget cancelBtn;

        public AddEntry() {
            this.nameField = new VCTextField(CustomFaColors.this.textRenderer, 0, 0, FIELD_WIDTH, FIELD_HEIGHT, Text.literal("Name"));
            this.nameField.setText("");
            this.nameField.setEditable(true);
            this.nameField.setMaxLength(32);
            this.nameField.setUIScale(UI_SCALE);
            this.nameField.setFocused(true);
            CustomFaColors.this.setFocused(this.nameField);   

            this.colorBtn = new FaButton(
                0, 0, COLOR_BTN_WIDTH, FIELD_HEIGHT,
                Text.literal(COLOR_TEXT).styled(s -> s.withColor(color)),
                btn -> 
                    MinecraftClient.getInstance().setScreen(new ColorWheel(CustomFaColors.this, this.color, selected -> {
                        this.color = selected;
                        this.colorBtn.setMessage(Text.literal(COLOR_TEXT).styled(s -> s.withColor(this.color)));
                        this.nameField.setEditableColor(this.color);                        
                        String name = this.nameField.getText().trim();
                        if (!name.isEmpty()) {
                            FaColors.saveUserEntry(name, this.color & 0xFFFFFFFF);
                            addMode = false;
                            CustomFaColors.this.remove(this.nameField);
                            CustomFaColors.this.remove(this.colorBtn);
                            CustomFaColors.this.remove(this.saveBtn);
                            CustomFaColors.this.remove(this.cancelBtn);
                            CustomFaColors.this.init();
                        } else {
                            MinecraftClient.getInstance().setScreen(CustomFaColors.this);
                        }
                    }))
            );

            this.saveBtn = new FaButton(
                0, 0, 20, FIELD_HEIGHT,
                Text.literal("✔").styled(s -> s.withColor(0xFFCCFFCC)),
                btn -> {
                    String name = this.nameField.getText().trim();
                    if (!name.isEmpty()) {
                        FaColors.saveUserEntry(name, color & 0xFFFFFFFF);
                        addMode = false;
                        CustomFaColors.this.remove(this.nameField);
                        CustomFaColors.this.remove(this.colorBtn);
                        CustomFaColors.this.remove(this.saveBtn);
                        CustomFaColors.this.remove(this.cancelBtn);
                        CustomFaColors.this.init();
                    }
                }
            );

            this.cancelBtn = new FaButton(
                0, 0, 20, FIELD_HEIGHT,
                Text.literal("🗑").styled(s -> s.withColor(0xFFFF8080)),
                btn -> {
                    addMode = false;
                    CustomFaColors.this.remove(this.nameField);
                    CustomFaColors.this.remove(this.colorBtn);
                    CustomFaColors.this.remove(this.saveBtn);
                    CustomFaColors.this.remove(this.cancelBtn);
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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
                scrollbarThumbOffset = (int)mouseY - thumbY;
            } else {
                isDraggingScrollbar = true;
                scrollbarThumbOffset = thumbHeight / 2;
                double trackClickY = mouseY - listTop - scrollbarThumbOffset;
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
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDraggingScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDraggingScrollbar) {
            int listTop = 40;
            int listBottom = this.height - 60;
            int listHeight = listBottom - listTop;
            int totalEntries = entries.size() + (addMode ? 1 : 0);
            int thumbHeight = Math.max((int)(10 * UI_SCALE), (maxVisibleEntries * listHeight) / totalEntries);
            int mouseThumbY = (int)mouseY - listTop - scrollbarThumbOffset;
            double scrollPercent = mouseThumbY / (double)(listHeight - thumbHeight);
            int newScrollOffset = (int)(scrollPercent * (totalEntries - maxVisibleEntries));
            scrollOffset = Math.clamp(newScrollOffset, 0, totalEntries - maxVisibleEntries);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
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
        private final ButtonWidget colorBtn;
        private ButtonWidget delBtn;
        private boolean changed = false;

        public Entry(String name, int color) {
            this.originalName = name;
            this.originalColor = color;
            this.color = color;
            this.nameField = new VCTextField(CustomFaColors.this.textRenderer, 0, 0, FIELD_WIDTH, FIELD_HEIGHT, Text.literal("Name"));
            this.nameField.setText(name);
            this.nameField.setEditable(true);
            this.nameField.setMaxLength(32);
            this.nameField.setUIScale(UI_SCALE);
            this.nameField.setFocused(false);
            this.colorBtn = new FaButton(
                0, 0, COLOR_BTN_WIDTH, FIELD_HEIGHT,
                Text.literal(COLOR_TEXT).styled(s -> s.withColor(color)),
                btn -> 
                    MinecraftClient.getInstance().setScreen(new ColorWheel(CustomFaColors.this, this.color, selected -> {
                        this.color = selected;
                        btn.setMessage(Text.literal(COLOR_TEXT).styled(s -> s.withColor(this.color)));
                        MinecraftClient.getInstance().setScreen(CustomFaColors.this);
                        this.changed = true;
                        FaColors.saveUserEntry(this.originalName, this.color & 0xFFFFFFFF);
                    }))
            );
            
            this.delBtn = new FaButton(
                0, 0, DEL_BTN_WIDTH, FIELD_HEIGHT,
                Text.literal("❌").styled(style -> style.withColor(0xFF808080)),
                btn -> {
                    FaColors.deleteUserEntry(this.originalName);
                    entries.remove(this);
                    CustomFaColors.this.remove(this.nameField);
                    CustomFaColors.this.remove(this.colorBtn);
                    CustomFaColors.this.remove(this.delBtn);
                    CustomFaColors.this.init();
                }
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
            String newName = this.nameField.getText().trim();
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
