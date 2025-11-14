package me.valkeea.fishyaddons.ui.list;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.FishyConfig.AlertData;
import me.valkeea.fishyaddons.config.FishyPresets;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.feature.qol.ChatAlert;
import me.valkeea.fishyaddons.ui.VCPopup;
import me.valkeea.fishyaddons.ui.VCRenderUtils;
import me.valkeea.fishyaddons.ui.VCScreen;
import me.valkeea.fishyaddons.ui.VCText;
import me.valkeea.fishyaddons.ui.widget.FaButton;
import me.valkeea.fishyaddons.ui.widget.VCButton;
import me.valkeea.fishyaddons.ui.widget.VCLabelField;
import me.valkeea.fishyaddons.ui.widget.VCTextField;
import me.valkeea.fishyaddons.ui.widget.VCVisuals;
import me.valkeea.fishyaddons.ui.widget.dropdown.DropdownMenu;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public class ChatAlerts extends Screen {
    private static final String TITLE_TEXT = "Chat Alerts";

    private static float uiScale;
    private static int entryH;
    private static int entryW;
    private static int btnW;
    private static int delBtnW;
    private static int fieldW;
    private static int fieldH;
    private static int btnH;

    private final List<Entry> entries = new ArrayList<>();

    private boolean isDraggingScrollbar = false;
    private boolean addMode = false;
    private int scrollOffset = 0;
    private int maxVisibleEntries = 0;
    private int uploadBtnX = 0;
    private int uploadBtnY = 0;
    private int downloadBtnX = 0;
    private int downloadBtnY = 0;
    private int scrollbarThumbOffset = 0;
    
    private Screen parent = null;  
    private AddEntry addEntry = null;
    private FaButton addBtn = null;
    private DropdownMenu presetDropdown;
    private VCPopup popup = null;
    private VCTextField presetNameField = null;  

    public ChatAlerts(Screen parent) {
        super(Text.literal(TITLE_TEXT));
        this.parent = parent;
    }

    @Override
    protected void init() {

        entries.clear();        
        this.clearChildren();
        calcDimensions(FishyConfig.getFloat(Key.MOD_UI_SCALE, 0.4265625f));

        for (Map.Entry<String, AlertData> entry : FishyConfig.getChatAlerts().entrySet()) {
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
                Text.literal("Add").styled(style -> style.withColor(0xFFCCFFCC)),
                btn -> {
                    if (entries.size() >= maxVisibleEntries) {
                        scrollOffset = entries.size() - maxVisibleEntries + 1;
                    }
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

        var backButton = new FaButton(
            this.width / 2 - entryW / 2 + btnW, this.height - 40, btnW, btnH,
            Text.literal("Back").styled(style -> style.withColor(0xFF808080)),
            btn -> {
                if (parent != null) {
                    MinecraftClient.getInstance().setScreen(parent);
                } else {
                    client.setScreen(new VCScreen());
                }
            }
        );
        backButton.setUIScale(uiScale);
        this.addDrawableChild(backButton);

        var closeButton = new FaButton(
            this.width / 2 - entryW / 2 + btnW * 2, this.height - 40, btnW, btnH,
            Text.literal("Close").styled(style -> style.withColor(0xFF808080)),
            btn -> this.close()
        );
        closeButton.setUIScale(uiScale);
        this.addDrawableChild(closeButton);

        downloadBtnX = this.width / 2 + entryW / 2 - btnW * 2;
        downloadBtnY = this.height - 40;
        this.addDrawableChild(new FaButton(
            downloadBtnX, downloadBtnY, btnW, btnH,
            Text.literal("📁⤒").styled(style -> style.withBold(true).withColor(0xFFE2CAE9)),
            b -> showPresetDropdown(downloadBtnX, downloadBtnY)
        ));

        uploadBtnX = this.width / 2 + entryW / 2 - btnW;
        uploadBtnY = this.height - 40;
        this.addDrawableChild(new FaButton(
            uploadBtnX, uploadBtnY, btnW, btnH,
            Text.literal("📁⤓").styled(style -> style.withBold(true).withColor(0xFFB0FFB0)),
            b -> presetPopup()
        ));
    }

    private static void calcDimensions(float scale) {
        uiScale = Math.clamp(scale, 0.7f, 1.3f);
        entryH = (int) (28 * uiScale);
        entryW = (int) (600 * uiScale);
        btnW = (int) (40 * uiScale);
        delBtnW = (int) (20 * uiScale);
        fieldW = (int) (480 * uiScale);
        fieldH = (int) (20 * uiScale);
        btnH = (int) (20 * uiScale);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        if (entries.isEmpty()) {
            int y = this.height / 2;
            int x = this.width / 2;
            int lineHeight = 15;
            y += lineHeight * 2;
            String[] instructions = {
                TITLE_TEXT,
                " -§7Add strings you want to be alerted on.",
                " -§7After adding, click 'Edit' to configure optional title, chat msg and sound alarms.",
                " -§7You can share configs by dropping files in fishyaddons/preset.",
                " -§dIf you add a chat filter, the original message should be used as a trigger!",
            };
            for (String instruction : instructions) {
                Text text = Text.literal(instruction);
                VCText.drawScaledCenteredText(context, this.textRenderer, text.getString(),
                    x, y, 0xFF55FFFF, uiScale - 0.1f);
                y += lineHeight;
            }
        } else {
            VCText.drawScaledCenteredText(
                context, this.textRenderer, VCText.header(TITLE_TEXT, Style.EMPTY.withBold(true)), this.width / 2, 15, 0xFF55FFFF, uiScale - 0.1f);
        }

        if (presetDropdown != null && presetDropdown.isVisible()) {
            presetDropdown.render(context, this, mouseX, mouseY);
        }        
        addList(context);

        if (popup != null) {          
            popup.render(context, this.textRenderer, mouseX, mouseY, delta);
        }

        if (popup != null && presetNameField != null) {
            presetNameField.setY(popup.getY() + 30);
            presetNameField.render(context, mouseX, mouseY, delta);
        }

        if (isInside(downloadBtnX, downloadBtnY, btnW, btnH, mouseX, mouseY)) {
            VCRenderUtils.preview(context, this.textRenderer, Arrays.asList(
                Text.literal("Download From File:"),
                Text.literal("- §8Format: preset.alert.<name>.json"),
                Text.literal("- §8All presets are stored in config/fishyaddons/preset"),
                Text.literal("  §8and can be re-downloaded"),
                Text.literal("  §8'example' contains the proper json structure")
            ), downloadBtnX - 60, downloadBtnY - 60, VCVisuals.getThemeColor(), uiScale);
        }
        if (isInside(uploadBtnX, uploadBtnY, btnW, btnH, mouseX, mouseY)) {
            VCRenderUtils.preview(context, this.textRenderer, Arrays.asList(
                Text.literal("Save as Preset:"),
                Text.literal("- §8Creates a working preset with all current alerts."),
                Text.literal("- §8Can be shared with others or saved for later use")
            ), uploadBtnX - 60, uploadBtnY - 60, VCVisuals.getThemeColor(), uiScale);
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
            
            this.keyField = new VCTextField(ChatAlerts.this.textRenderer, 0, offScreenY, fieldW, fieldH, Text.literal("Key"));
            this.keyField.setDrawsBackground(false);
            this.keyField.setText("");
            this.keyField.setEditable(true);
            this.keyField.setMaxLength(100);
            this.keyField.setUIScale(uiScale);
            this.keyField.setFocused(true);
            this.keyField.setPlaceholder(Text.literal("Detected String...").styled(style -> style.withColor(0xFF808080)));
            ChatAlerts.this.setFocused(this.keyField);

            this.saveBtn = new FaButton(
                0, offScreenY, delBtnW, fieldH,
                Text.literal("✔").styled(s -> s.withColor(0xCCFFCC)),
                btn -> {
                    String key = this.keyField.getText().trim();
                    if (!checkForDupes(key)) {
                        return;
                    }
                    if (!key.isEmpty()) {
                        FishyConfig.setChatAlert(key, new AlertData());
                        abortEntry();
                    }
                }
            );

            this.cancelBtn = new FaButton(
                0, offScreenY, delBtnW, fieldH,
                Text.literal("❌").styled(s -> s.withColor(0xFFFF8080)),
                btn -> abortEntry()
            );
        }

        public void abortEntry() {
            addMode = false;
            ChatAlerts.this.remove(this.keyField);
            ChatAlerts.this.remove(this.saveBtn);
            ChatAlerts.this.remove(this.cancelBtn);
            ChatAlerts.this.init();
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

        public Entry(String key) {
            final int offScreenY = -1000;
            
            this.keyField = new VCLabelField(ChatAlerts.this.textRenderer, 0, offScreenY, fieldW, fieldH, Text.literal("Key"));
            this.keyField.setText(key);
            this.keyField.setUIScale(uiScale);
            this.keyField.setFocused(false);
            this.editBtn = VCButton.createNavigationButton(
                0, offScreenY, btnW, fieldH,
                Text.literal("Edit").styled(s -> s.withColor(0xFFE2CAE9)),
                btn -> {
                    AlertData data = FishyConfig.getChatAlerts().get(key);
                    MinecraftClient.getInstance().setScreen(new AlertEditScreen(key, data, ChatAlerts.this));
                },
                uiScale - 0.1f
            );

            this.delBtn = VCButton.createNavigationButton(
                0, offScreenY, delBtnW, fieldH,
                Text.literal("🗑").setStyle(Style.EMPTY.withColor(0xFF808080)),
                btn -> {
                    FishyConfig.removeChatAlert(key);
                    ChatAlert.refresh();
                    entries.remove(this);
                    ChatAlerts.this.remove(this.keyField);
                    ChatAlerts.this.remove(this.editBtn);
                    ChatAlerts.this.remove(this.delBtn);
                    ChatAlerts.this.remove(this.toggleBtn);
                    ChatAlerts.this.init();
                }, uiScale
            );

            boolean state = FishyConfig.isChatAlertToggled(key);
            this.toggleBtn = VCButton.createMcToggle(
                0, offScreenY, btnW, fieldH,
                state,
                btn -> {
                    FishyConfig.toggleChatAlert(key, !state);
                    ChatAlert.refresh();
                    ChatAlerts.this.init();
                },
                uiScale - 0.1f
            );
        }

        public void setPosition(int x, int y) {
            this.keyField.setX(x);
            this.keyField.setY(y);
            this.editBtn.setX(x + fieldW);
            this.editBtn.setY(y);
            this.toggleBtn.setX(x + fieldW + btnW);
            this.toggleBtn.setY(y);
            this.delBtn.setX(x + fieldW + btnW * 2);
            this.delBtn.setY(y);
        }

        public void setVisible(boolean visible) {
            this.keyField.setVisible(visible);
            this.editBtn.visible = visible;
            this.delBtn.visible = visible;
            this.toggleBtn.visible = visible;
        }

        public void addToScreen() {
            ChatAlerts.this.addDrawableChild(this.keyField);
            ChatAlerts.this.addDrawableChild(this.editBtn);
            ChatAlerts.this.addDrawableChild(this.delBtn);
            ChatAlerts.this.addDrawableChild(this.toggleBtn);
        }

        public boolean mouseClicked(Click click) {

            double mouseX = click.x();
            double mouseY = click.y();
            
            if (isInside(keyField.getX(), keyField.getY(), keyField.getWidth(), keyField.getHeight(), mouseX, mouseY)) {
                return true;
            }
            if (isInside(editBtn.getX(), editBtn.getY(), editBtn.getWidth(), editBtn.getHeight(), mouseX, mouseY)) {
                editBtn.onPress(click);
                return true;
            }
            if (isInside(delBtn.getX(), delBtn.getY(), delBtn.getWidth(), delBtn.getHeight(), mouseX, mouseY)) {
                delBtn.onPress(click);
                return true;
            }
            if (isInside(toggleBtn.getX(), toggleBtn.getY(), toggleBtn.getWidth(), toggleBtn.getHeight(), mouseX, mouseY)) {
                toggleBtn.onPress(click);
                return true;
            }
            return false;
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (popup != null && presetNameField != null && presetNameField.mouseClicked(click, doubled)) {
            presetNameField.setFocused(true);
            this.setFocused(presetNameField);
            return true;
        }
        if (presetDropdown != null && presetDropdown.isVisible()) {
            if (presetDropdown.mouseClicked(click)) return true;
            int x = presetDropdown.getX(); 
            int y = presetDropdown.getY(); 
            int w = presetDropdown.getWidth();
            int h = presetDropdown.getEntryHeight() * presetDropdown.getEntries().size();
            if (!(click.x() >= x && click.x() <= x + w && click.y() >= y && click.y() <= y + h)) {
                presetDropdown.setVisible(false);
                return true;
            }
        }

        if (popup != null) {
            return popup.mouseClicked(click);
        }

        if (handleScrollbar(click.x(), click.y())) {
            return true;
        }

        int startIdx = scrollOffset;
        int endIdx = Math.min(startIdx + maxVisibleEntries, entries.size());
        for (int i = startIdx; i < endIdx; i++) {
            if (entries.get(i).mouseClicked(click)) {
                return false;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    private boolean handleScrollbar(double mouseX, double mouseY) {

        int listTop = 40;
        int listBottom = this.height - 60;
        int listHeight = listBottom - listTop;
        int scrollbarX = this.width / 2 + entryW / 2 + 20;
        int scrollbarWidth = Math.max(4, (int)(8 * uiScale));
        int totalEntries = entries.size() + (addMode ? 1 : 0);

        if (totalEntries > maxVisibleEntries && mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth
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
    public boolean mouseReleased(Click click) {
        isDraggingScrollbar = false;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (isDraggingScrollbar) {
            int listTop = 40;
            int listBottom = this.height - 60;
            int listHeight = listBottom - listTop;
            int totalEntries = entries.size() + (addMode ? 1 : 0);
            int thumbHeight = Math.max((int)(10 * uiScale), (maxVisibleEntries * listHeight) / totalEntries);
            int mouseThumbY = (int)click.y() - listTop - scrollbarThumbOffset;
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

    private void showPresetDropdown(int btnX, int btnY) {
        List<String> suffixes = FishyPresets.listPresetSuffixes(FishyPresets.PresetType.ALERT);

        if (suffixes.isEmpty()) {
            popup(Text.literal("No presets found for this tab."), "OK", () -> popup = null, "", () -> {});
            return;
        }
        
        int entryHeight = (int)(uiScale * 18);
        int dropdownX = btnX;
        int dropdownY = btnY - btnH - (suffixes.size() * entryHeight) / 2;

        presetDropdown = new DropdownMenu(
            suffixes, dropdownX, dropdownY, 100, entryHeight,
            suffix -> {
                loadPreset(suffix);
                init();
                presetDropdown.setVisible(false);
            }
        );
    }

    private void loadPreset(String suffix) {
        Map<String, FishyConfig.AlertData> map = FishyPresets.loadAlertPreset(suffix);
        if (map != null) {
            FishyConfig.chatAlerts.getValues().putAll(map);
            FishyConfig.save();
            init();
        }
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

    private void presetPopup() {
        presetNameField = new VCTextField(this.textRenderer, this.width / 2 - 60,
            this.height / 2 - 35, 120, 20, Text.literal("Preset Name"));

        presetNameField.setMaxLength(15);
        presetNameField.setText("");
        this.setFocused(presetNameField);

        this.popup = new VCPopup(
            Text.literal("Enter preset name:"),
            "Cancel", () -> {
                this.remove(presetNameField);
                presetNameField = null;
                this.popup = null;
            },
            "Save", () -> {
            String suffix = presetNameField.getText().trim();
            if (!suffix.isEmpty()) {
                savePreset(suffix);
            }
            popup = null;
            this.remove(presetNameField);
            presetNameField = null;
        },
        1.0f
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

    private void savePreset(String suffix) {
        FishyPresets.saveAlertPreset(
                suffix, FishyConfig.chatAlerts.getValues());
    }    
}
