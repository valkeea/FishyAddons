package me.valkeea.fishyaddons.ui.list;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import me.valkeea.fishyaddons.config.FilterConfig;
import me.valkeea.fishyaddons.config.FilterConfig.Rule;
import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.ui.VCPopup;
import me.valkeea.fishyaddons.ui.VCText;
import me.valkeea.fishyaddons.ui.widget.FaButton;
import me.valkeea.fishyaddons.ui.widget.VCButton;
import me.valkeea.fishyaddons.ui.widget.VCLabelField;
import me.valkeea.fishyaddons.ui.widget.VCTextField;
import me.valkeea.fishyaddons.ui.widget.VCVisuals;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public class FilterRules extends Screen {
    private static final String TITLE_TEXT = "─ α Chat Filter Rules α ─";

    private static float uiScale;
    private static int entryH;
    private static int entryW;
    private static int btnW;
    private static int modeBtnW;
    private static int delBtnW;
    private static int fieldW;
    private static int fieldH;
    private static int btnH;

    private final Screen parent;
    private final List<Entry> entries = new ArrayList<>();

    private boolean isDraggingScrollbar = false;
    private boolean addMode = false;
    private int scrollOffset = 0;
    private int maxVisibleEntries = 0;
    private int scrollbarThumbOffset = 0;

    private AddEntry addEntry = null;
    private FaButton addBtn = null;
    private VCPopup popup = null;

    public FilterRules(Screen parent) {
        super(Text.literal(TITLE_TEXT));
        this.parent = parent;
    }

    @Override
    protected void init() {
    entries.clear();        
    this.clearChildren();
    calcDimensions(FishyConfig.getFloat(Key.MOD_UI_SCALE, 0.4265625f));
        for (Map.Entry<String, Rule> entry : FilterConfig.getUserCreatedRules().entrySet()) {
            Entry e = new Entry(entry.getKey());
            entries.add(e);
            e.addToScreen();
        }
        int totalEntries = entries.size() + (addMode ? 1 : 0);
        int listTop = 40;
        int listBottom = this.height - 60;
        int listHeight = listBottom - listTop;
        maxVisibleEntries = Math.max(1, listHeight / entryH);
        int maxScroll = Math.max(0, totalEntries - maxVisibleEntries);
        scrollOffset = Math.clamp(scrollOffset, 0, maxScroll);
        if (scrollOffset > maxScroll || (addMode && scrollOffset < totalEntries - maxVisibleEntries)) scrollOffset = maxScroll;
        if (scrollOffset < 0) scrollOffset = 0;
        if (!addMode) {
            int addBtnY = this.height - 40;
            addBtn = new FaButton(
                this.width / 2 - entryW / 2, addBtnY, btnW, btnH,
                Text.literal("Add").styled(style -> style.withColor(0xCCFFCC)),
                btn -> {
                    addMode = true;
                    addEntry = new AddEntry();
                    this.addDrawableChild(addEntry.keyField);
                    this.addDrawableChild(addEntry.saveBtn);
                    this.addDrawableChild(addEntry.cancelBtn);
                    this.remove(addBtn);
                }
            );
            addBtn.setUIScale(uiScale);
            this.addDrawableChild(addBtn);
        }      

        FaButton backButton = new FaButton(
            this.width / 2 - entryW / 2 + btnW, this.height - 40, btnW, btnH,
            Text.literal("Back").styled(style -> style.withColor(0xFF808080)),
            btn -> client.setScreen(parent)
        );
        backButton.setUIScale(uiScale);
        this.addDrawableChild(backButton);

        FaButton closeButton = new FaButton(
            this.width / 2 - entryW / 2 + btnW * 2, this.height - 40, btnW, btnH,
            Text.literal("Close").styled(style -> style.withColor(0xFF808080)),
            btn -> this.close()
        );
        closeButton.setUIScale(uiScale);
        this.addDrawableChild(closeButton);
    }

    private static void calcDimensions(float scale) {
        uiScale = Math.clamp(scale, 0.7f, 1.3f);
        entryH = (int) (28 * uiScale);
        entryW = (int) (600 * uiScale);
        btnW = (int) (40 * uiScale);
        modeBtnW = (int) (60 * uiScale);
        delBtnW = (int) (20 * uiScale);
        fieldW = (int) (480 * uiScale);
        fieldH = (int) (20 * uiScale);
        btnH = (int) (20 * uiScale);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        if (entries.isEmpty()) {
            int y = this.height / 2;
            int x = this.width / 2;
            int lineHeight = 15;
            y += lineHeight * 2;
            String[] instructions = {
                TITLE_TEXT,
                " -§7 Add clean strings you want to be filtered. Without editing, the message defaults to being removed.",
                " -§7 If you instead want the message to be replaced, click 'Edit' to configure an override.",
                "§b  • §8'Exact' will only trigger on full matches",
                "§b  • §8'Anywhere' will match any phrase containing the input and fully replace the message.",                
                " -§d If you wish to filter sea creature messages, please use §bCustom Sea Creature Messages §dinstead.",
            };
            for (String instruction : instructions) {
                context.getMatrices().push();
                context.getMatrices().translate(0,0, 300);      
                Text text = Text.literal(instruction);
                VCText.drawScaledCenteredText(context, this.textRenderer, text.getString(),
                    x, y, 0xFF55FFFF, uiScale - 0.1f);
                context.getMatrices().pop();
                y += lineHeight;
            }
        } else {
            VCText.drawScaledCenteredText(
                context, this.textRenderer, TITLE_TEXT, this.width / 2, 15, 0xFF55FFFF, uiScale - 0.1f);
        }
       
        addList(context);

        if (popup != null) {
            this.renderBackground(context, mouseX, mouseY, delta);            
            popup.render(context, this.textRenderer, mouseX, mouseY, delta);
        }
    }

    private void addList(DrawContext context) {
        int listTop = 40;
        int listBottom = this.height - 60;
        int listHeight = listBottom - listTop;
        maxVisibleEntries = Math.max(1, listHeight / entryH);
        int totalEntries = entries.size() + (addMode ? 1 : 0);
        int y = listTop;
        int startIdx = scrollOffset;
        int endIdx = Math.min(startIdx + maxVisibleEntries, entries.size());
        for (int i = 0; i < entries.size(); i++) {
            if (i >= startIdx && i < endIdx) {
                entries.get(i).setPosition(this.width / 2 - entryW / 2, y);
                entries.get(i).setVisible(true);
                y += entryH;
            } else {
                entries.get(i).setVisible(false);
            }
        }

        if (totalEntries > maxVisibleEntries) {
            renderScrollIndicator(context, this.width / 2 + entryW / 2 + 20, listTop, listHeight, totalEntries);
        }

        if (addMode && addEntry != null) {
            addEntry.updateVisibility();
            if (endIdx == entries.size()) {
                addEntry.setPosition(this.width / 2 - entryW / 2, y);
            }
        }
        if (addBtn != null && !addMode) {
            int addBtnY = this.height - 40;
            addBtn.setX(this.width / 2 - entryW / 2);
            addBtn.setY(addBtnY);
        }
    }

    private void renderScrollIndicator(DrawContext context, int x, int y, int listHeight, int totalEntries) {
        int scrollbarWidth = 4;
        context.fill(x, y, x + scrollbarWidth, y + listHeight, 0x44000000);
        if (totalEntries > maxVisibleEntries) {
            int thumbHeight = Math.max((int)(10 * uiScale), (maxVisibleEntries * listHeight) / totalEntries);
            int thumbY = y + (scrollOffset * (listHeight - thumbHeight)) / (totalEntries - maxVisibleEntries);
            context.fill(x + 1, thumbY, x + scrollbarWidth - 1, thumbY + thumbHeight, VCVisuals.getThemeColor());
            context.fill(x + 1, thumbY + thumbHeight - 1, x + scrollbarWidth - 1, thumbY + thumbHeight, 0xFF000000);
        }
    }

    private static boolean isInside(int x, int y, int w, int h, double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }    

    private class AddEntry {
        private final VCTextField keyField;
        private ButtonWidget saveBtn;
        private ButtonWidget cancelBtn;

        public AddEntry() {
            final int offScreenY = -1000;
            
            this.keyField = new VCTextField(FilterRules.this.textRenderer, 0, offScreenY, fieldW, fieldH, Text.literal("Key"));
            this.keyField.setDrawsBackground(false);
            this.keyField.setText("");
            this.keyField.setEditable(true);
            this.keyField.setMaxLength(100);
            this.keyField.setUIScale(uiScale);
            this.keyField.setFocused(true);
            this.keyField.setPlaceholder(Text.literal("Filtered String...").styled(style -> style.withColor(0xFF808080)));
            FilterRules.this.setFocused(this.keyField);

            this.saveBtn = new FaButton(
                0, offScreenY, delBtnW, fieldH,
                Text.literal("✔").styled(s -> s.withColor(0xCCFFCC)),
                btn -> {
                    String key = this.keyField.getText().trim();
                    if (!checkForDupes(key)) {
                        return;
                    }
                    if (!key.isEmpty()) {
                        Rule defaultRule = new Rule();
                        defaultRule.setSearchText(key);
                        FilterConfig.setUserRule(key, defaultRule);
                        abortEntry();
                    }
                }
            );

            this.cancelBtn = new FaButton(
                0, offScreenY, delBtnW, fieldH,
                Text.literal("❌").styled(s -> s.withColor(0xFF8080)),
                btn -> abortEntry()
            );
        }

        public void abortEntry() {
            addMode = false;
            FilterRules.this.remove(this.keyField);
            FilterRules.this.remove(this.saveBtn);
            FilterRules.this.remove(this.cancelBtn);
            FilterRules.this.init();
        }

        public void setPosition(int x, int y) {
            this.keyField.setX(x);
            this.keyField.setY(y);
            this.saveBtn.setX(x + fieldW);
            this.saveBtn.setY(y);
            this.cancelBtn.setX(x + fieldW + delBtnW);
            this.cancelBtn.setY(y);
        }

        public void updateVisibility() {
            if (this.keyField.visible) {
                this.keyField.setVisible(false);
                this.saveBtn.visible = false;
                this.cancelBtn.visible = false;
            }
            setVisible(true);
        }

        public void setVisible(boolean visible) {
            this.keyField.setVisible(visible);
            this.saveBtn.visible = visible;
            this.cancelBtn.visible = visible;
        }

        private boolean checkForDupes(String key) {
            for (Entry entry : entries) {
                if (entry.keyField.getText().trim().equals(key)) {
                    dupePopup(entry.keyField.getText().trim());
                    return false;
                }
            }
            return true;
        }
    }

    private class Entry {
        private final VCLabelField keyField;
        private final ButtonWidget editBtn;
        private ButtonWidget delBtn;
        private ButtonWidget toggleBtn;
        private ButtonWidget modeBtn;

        public Entry(String key) {
            final int offScreenY = -1000;
            
            this.keyField = new VCLabelField(FilterRules.this.textRenderer, 0, offScreenY, fieldW, fieldH, Text.literal("Key"));
            this.keyField.setText(key);
            this.keyField.setUIScale(uiScale);
            this.keyField.setFocused(false);
            Rule rule = FilterConfig.getUserCreatedRules().get(key);
            this.editBtn = VCButton.createNavigationButton(
                0, offScreenY, btnW, fieldH,
                Text.literal("Edit").styled(s -> s.withColor(0xE2CAE9)),
                btn -> 
                    MinecraftClient.getInstance().setScreen(new FilterEditScreen(
                        key, rule != null ? rule : new Rule(), FilterRules.this
                    )),
                uiScale - 0.1f
            );

            boolean mode = rule.requireFullMatch();
            this.modeBtn = VCButton.createNavigationButton(
                0, offScreenY, modeBtnW, fieldH,
                mode ? Text.literal("Exact") : Text.literal("Anywhere"),
                btn -> {
                    Rule currentRule = FilterConfig.getUserCreatedRules().get(key);
                    boolean currentMode = currentRule != null && currentRule.requireFullMatch();
                    FilterConfig.setRequireFullMatch(key, !currentMode);
                    btn.setMessage(!currentMode ? Text.literal("Exact") : Text.literal("Anywhere"));
                },
                uiScale - 0.1f
            );

            this.toggleBtn = VCButton.createMcToggle(
                0, offScreenY, btnW, fieldH,
                rule.isEnabled(),
                btn -> {
                    VCButton.updateButtonState(this.toggleBtn, !rule.isEnabled());                    
                    FilterConfig.toggleUserRule(key);
                },
                uiScale - 0.1f
            );            

            this.delBtn = VCButton.createNavigationButton(
                0, offScreenY, delBtnW, fieldH,
                Text.literal("🗑").setStyle(Style.EMPTY.withColor(0xFF808080)),
                btn -> {
                    FilterConfig.removeUserRule(key);
                    entries.remove(this);
                    FilterRules.this.remove(this.keyField);
                    FilterRules.this.remove(this.editBtn);
                    FilterRules.this.remove(this.modeBtn);
                    FilterRules.this.remove(this.delBtn);
                    FilterRules.this.remove(this.toggleBtn);
                    FilterRules.this.init();
                }, uiScale
            );
        }

        public void setPosition(int x, int y) {
            this.keyField.setX(x);
            this.keyField.setY(y);
            this.editBtn.setX(x + fieldW);
            this.editBtn.setY(y);
            this.modeBtn.setX(x + fieldW + btnW);
            this.modeBtn.setY(y);
            this.toggleBtn.setX(x + fieldW + modeBtnW + btnW);
            this.toggleBtn.setY(y);
            this.delBtn.setX(x + fieldW + modeBtnW + btnW * 2);
            this.delBtn.setY(y);
        }

        public void setVisible(boolean visible) {
            this.keyField.setVisible(visible);
            this.editBtn.visible = visible;
            this.modeBtn.visible = visible;
            this.delBtn.visible = visible;
            this.toggleBtn.visible = visible;
        }

        public void addToScreen() {
            FilterRules.this.addDrawableChild(this.keyField);
            FilterRules.this.addDrawableChild(this.editBtn);
            FilterRules.this.addDrawableChild(this.modeBtn);
            FilterRules.this.addDrawableChild(this.delBtn);
            FilterRules.this.addDrawableChild(this.toggleBtn);
        }

        public boolean mouseClicked(double mouseX, double mouseY) {
            if (isInside(keyField.getX(), keyField.getY(), keyField.getWidth(), keyField.getHeight(), mouseX, mouseY)) {
                return true;
            }
            if (isInside(editBtn.getX(), editBtn.getY(), editBtn.getWidth(), editBtn.getHeight(), mouseX, mouseY)) {
                editBtn.onPress();
                return true;
            }
            if (isInside(modeBtn.getX(), modeBtn.getY(), modeBtn.getWidth(), modeBtn.getHeight(), mouseX, mouseY)) {
                modeBtn.onPress();
                return true;
            }
            if (isInside(toggleBtn.getX(), toggleBtn.getY(), toggleBtn.getWidth(), toggleBtn.getHeight(), mouseX, mouseY)) {
                toggleBtn.onPress();
                return true;
            }            
            if (isInside(delBtn.getX(), delBtn.getY(), delBtn.getWidth(), delBtn.getHeight(), mouseX, mouseY)) {
                delBtn.onPress();
                return true;
            }
            return false;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (popup != null) {
            return popup.mouseClicked(mouseX, mouseY, button);
        }

        if (handleScrollbar(mouseX, mouseY)) return true;

        int startIdx = scrollOffset;
        int endIdx = Math.min(startIdx + maxVisibleEntries, entries.size());
        for (int i = startIdx; i < endIdx; i++) {
            if (entries.get(i).mouseClicked(mouseX, mouseY)) {
                return false;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleScrollbar(double mouseX, double mouseY) {
        int listTop = 40;
        int listBottom = this.height - 60;
        int listHeight = listBottom - listTop;
        int scrollbarX = this.width / 2 + entryW / 2 + 20;
        int scrollbarWidth = Math.max(4, (int)(8 * uiScale));
        int totalEntries = entries.size() + (addMode ? 1 : 0);

        if (totalEntries > maxVisibleEntries
            && mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth
            && mouseY >= listTop && mouseY <= listTop + listHeight) {
            int thumbHeight = Math.max((int)(10 * uiScale), (maxVisibleEntries * listHeight) / totalEntries);
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
        return false;
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
            int thumbHeight = Math.max((int)(10 * uiScale), (maxVisibleEntries * listHeight) / totalEntries);
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

    public void popup(Text title, String continueButtonText, Runnable onContinue, String discardButtonText, Runnable onDiscard) {
        popup = new VCPopup(
			title,
			discardButtonText,
            onDiscard::run,
			continueButtonText,
            onContinue::run,
            uiScale
        );
        this.popup.init(this.textRenderer, this.width, this.height);
    }

    public void dupePopup(String input) {
        String truncated = input.length() > 7 ? input.substring(0, 7) + "..." : input;        
        this.popup = new VCPopup(
            Text.literal("Entry with key '" + truncated + "' already exists!"),
            "Continue Editing", () -> this.popup = null,
            "Discard Entry", () -> {
                if (addEntry != null) {
                    addEntry.abortEntry();
                }
                this.popup = null;
            },
            1.0f
        );
        this.popup.init(this.textRenderer, this.width, this.height);
    } 
}