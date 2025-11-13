package me.valkeea.fishyaddons.ui.list;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import me.valkeea.fishyaddons.config.FilterConfig;
import me.valkeea.fishyaddons.config.FilterConfig.Rule;
import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.config.RuleFactory;
import me.valkeea.fishyaddons.ui.VCText;
import me.valkeea.fishyaddons.ui.widget.FaButton;
import me.valkeea.fishyaddons.ui.widget.VCButton;
import me.valkeea.fishyaddons.ui.widget.VCLabelField;
import me.valkeea.fishyaddons.ui.widget.VCTextField;
import me.valkeea.fishyaddons.ui.widget.VCVisuals;
import me.valkeea.fishyaddons.ui.widget.dropdown.TextFormatMenu;
import me.valkeea.fishyaddons.util.text.Enhancer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public class ScRules extends Screen {
    private static final String TITLE_TEXT = "Sea Creature Messages";
    private static final String F3_TEXT = "§7Click a field and press F3 to apply formats...";

    private static float uiScale;
    private static int entryH;
    private static int entryW;
    private static int btnW;
    private static int fieldW;
    private static int nameW;
    private static int prefixW;
    private static int fieldH;
    private static int btnH;

    private final Screen parent;
    private final List<Entry> entries = new ArrayList<>();
    
    private TextFormatMenu formatMenu;
    private VCLabelField formatTip;
    private VCTextField lastFocusedField;

    private boolean isDraggingScrollbar = false;
    private int scrollOffset = 0;
    private int maxVisibleEntries = 0;
    private int scrollbarThumbOffset = 0;

    private Entry hoveredEntry = null;
    private long hoverStartTime = 0;
    private static final long TOOLTIP_DELAY = 250;

    public ScRules(Screen parent) {
        super(Text.literal(TITLE_TEXT));
        this.parent = parent;
    }  

    @Override
    protected void init() {
        if (!FilterConfig.areScRulesLoaded()) {
            FilterConfig.refreshScRules();
        }
        
        entries.clear();        
        this.clearChildren();
        calcDimensions(FishyConfig.getFloat(Key.MOD_UI_SCALE, 0.4265625f), this.width);

        List<RuleFactory.SeaCreatureData.CreatureConfig> allCreatures = 
            FilterConfig.getSeaCreatureData();
        Map<String, RuleFactory.SeaCreatureData.CategoryConfig> categories = 
            FilterConfig.getSeaCreatureCategories();
        
        sort(allCreatures);
        String currentCategory = null;
        
        for (RuleFactory.SeaCreatureData.CreatureConfig creature : allCreatures) {
            String ruleName = "sc_" + creature.getId();
            
            if (!creature.getCategory().equals(currentCategory)) {
                currentCategory = creature.getCategory();
                String categoryDisplay = currentCategory.substring(0, 1).toUpperCase() + currentCategory.substring(1);
                
                Entry categoryHeader = createCategoryHeader("── " + categoryDisplay + " ──");
                entries.add(categoryHeader);
                categoryHeader.addToScreen();
            }
            
            Rule existingRule = FilterConfig.getAllRules().get(ruleName);
            
            RuleFactory.SeaCreatureData.CategoryConfig category = categories.get(creature.getCategory());
            
            String displayName = creature.getDisplayName();
            String currentReplacement;
            boolean isEnabled;
            
            if (existingRule != null) {
                currentReplacement = existingRule.getReplacement();
                isEnabled = existingRule.isEnabled();
            } else {
                currentReplacement = buildPreview(creature, category);
                isEnabled = false;
            }

            Entry e = new Entry(displayName, currentReplacement, creature, existingRule, isEnabled);
            entries.add(e);
            e.addToScreen();                       
        }    

        int totalEntries = entries.size();
        int listTop = 40;
        int listBottom = this.height - 60;
        int listHeight = listBottom - listTop;
        maxVisibleEntries = Math.max(1, listHeight / entryH);
        int maxScroll = Math.max(0, totalEntries - maxVisibleEntries);
        scrollOffset = Math.clamp(scrollOffset, 0, maxScroll);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        if (scrollOffset < 0) scrollOffset = 0;  

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
        
        formatTip = new VCLabelField(
            this.textRenderer, this.width / 2 - fieldW / 2, this.height - 40, nameW * 2, fieldH, Text.literal(F3_TEXT));
        formatTip.setUIScale(uiScale);
        formatTip.setBg(false);
        this.addDrawableChild(formatTip);

        formatMenu = new TextFormatMenu(
            Math.max(0, this.width / 2 - entryW / 2 - nameW - btnW), listTop, nameW,
            this::insertAtCaret,
            Math.min(uiScale, 1.0f)
        );

        int availableHeight = listBottom - listTop;
        float menuUiScale = Math.min(uiScale, 1.0f);
        int scaledEntryHeight = (int)(fieldH * menuUiScale);
        int maxEntriesByHeight = availableHeight / scaledEntryHeight;
        formatMenu.setMaxEntries(maxEntriesByHeight);
    }

    private void sort(List<RuleFactory.SeaCreatureData.CreatureConfig> creatures) {
        Map<String, Integer> categoryOrder = new LinkedHashMap<>();
        int order = 0;
        for (RuleFactory.SeaCreatureData.CreatureConfig creature : creatures) {
            if (!categoryOrder.containsKey(creature.getCategory())) {
                categoryOrder.put(creature.getCategory(), order++);
            }
        }
        
        creatures.sort((a, b) -> {
            int categoryComparison = Integer.compare(
                categoryOrder.get(a.getCategory()), 
                categoryOrder.get(b.getCategory())
            );
            if (categoryComparison == 0) {
                return a.getDisplayName().compareTo(b.getDisplayName());
            }
            return categoryComparison;
        });
    }

    private void insertAtCaret(String format) {
        VCTextField focusedField = null;
        for (Entry entry : entries) {
            if (entry.overrideField != null && entry.overrideField.isFocused()) {
                focusedField = entry.overrideField;
                lastFocusedField = focusedField;
            } else if (entry.prefixField != null && entry.prefixField.isFocused()) {
                focusedField = entry.prefixField;
                lastFocusedField = focusedField;
            }
            if (focusedField != null) {
                break;
            }
        }
        
        if (focusedField == null && lastFocusedField != null) {
            focusedField = lastFocusedField;
            focusedField.setFocused(true);
        }
        
        if (focusedField != null) {
            apply(focusedField, format);
        }
        formatMenu.setVisible(false);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (formatMenu != null && formatMenu.isVisible() && formatMenu.keyPressed(keyCode)) {
            return true;
        }
        
        if (keyCode == 292) {
            toggleFormatMenu();
            return true;
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void apply(VCTextField field, String format) {
        String currentText = field.getText();
        int caretPos = field.getCursor();    
        String newText = currentText.substring(0, caretPos) + format + currentText.substring(caretPos);
        field.setText(newText);
        field.setCursor(caretPos + format.length(), false);
            
        for (Entry entry : entries) {
            if (entry.overrideField == field) {
                entry.replacementText = newText;
                entry.modified = true;
                return;
            } else if (entry.prefixField == field) {
                entry.prefixText = newText;
                entry.modified = true;
                return;
            }
        }
        field.setFocused(true);
    }
    
    @Override
    public void tick() {
        super.tick();
        
        for (Entry entry : entries) {
            if (entry.overrideField != null && entry.overrideField.isFocused()) {
                lastFocusedField = entry.overrideField;
            } else if (entry.prefixField != null && entry.prefixField.isFocused()) {
                lastFocusedField = entry.prefixField;
            }
            if (lastFocusedField != null) {
                break;
            }
        }
    }
    
    private void toggleFormatMenu() {
        VCTextField focusedField = null;
        for (Entry entry : entries) {
            if (entry.overrideField != null && entry.overrideField.isFocused()) {
                focusedField = entry.overrideField;
            } else if (entry.prefixField != null && entry.prefixField.isFocused()) {
                focusedField = entry.prefixField;
            }
            if (focusedField != null) {
                break;
            }
        }

        if (formatMenu != null) {
            boolean shouldShow = !formatMenu.isVisible();
            formatMenu.setVisible(shouldShow);
            if (shouldShow) {
                formatTip.setText("§7Press F3 to apply formats...");
            } else {
                formatTip.setText(F3_TEXT);
            }
        }
    }

    private static void calcDimensions(float scale, int width) {
        uiScale = Math.clamp(scale, 0.7f, 1.2f);
        nameW = (int) (120 * uiScale);
        prefixW = (int) (150 * uiScale);
        btnW = (int) (40 * uiScale);
        fieldW = (int) (330 * uiScale);
        fieldH = (int) (20 * uiScale);
        btnH = (int) (20 * uiScale);
        entryH = (int) (28 * uiScale);
        entryW = Math.clamp(nameW + prefixW + fieldW + (long)(3 * btnW), 0, width - btnW * 2);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        var title = VCText.header(TITLE_TEXT, Style.EMPTY.withBold(true));

        VCText.drawScaledCenteredText(
        context, this.textRenderer, title, this.width / 2, 15, 0xFF55FFFF, uiScale - 0.1f);
          
        addList(context);

        if (formatMenu != null && formatMenu.isVisible()) {
            formatMenu.render(context, this, mouseX, mouseY);
        }

        tryTooltip(context, mouseX, mouseY);
    }

    private void tryTooltip(DrawContext context, int mouseX, int mouseY) {
        if (shouldShowTooltip()) {
            String previewText = buildPreview();
            if (!previewText.isEmpty()) {
                try {
                    Text formattedPreview = Enhancer.parseFormattedText(previewText);
                    renderTooltip(context, mouseX, mouseY, formattedPreview, true);
                } catch (Exception e) {
                    System.err.println("[FishyAddons] Error rendering tooltip: " + e.getMessage());
                    e.printStackTrace();
                    renderTooltip(context, mouseX, mouseY, Text.literal("Error rendering preview"), false);
                }
            }
        }
    }

    private boolean shouldShowTooltip() {
        return hoveredEntry != null && !hoveredEntry.isHeader &&
                System.currentTimeMillis() - hoverStartTime > TOOLTIP_DELAY;
    }

    private String buildPreview() {
        String previewText = "";
        if (hoveredEntry.overrideField != null && !hoveredEntry.overrideField.getText().isEmpty()) {
            previewText = hoveredEntry.overrideField.getText();
        } else if (hoveredEntry.replacementText != null && !hoveredEntry.replacementText.isEmpty()) {
            previewText = hoveredEntry.replacementText;
        }

        if (hoveredEntry.prefixField != null && !hoveredEntry.prefixField.getText().isEmpty()) {
            previewText = hoveredEntry.prefixField.getText() + " " + previewText;
        } else if (hoveredEntry.prefixText != null && !hoveredEntry.prefixText.isEmpty()) {
            previewText = hoveredEntry.prefixText + previewText;
        }
        return previewText;
    }

    private void renderTooltip(DrawContext context, int mouseX, int mouseY, Text text, boolean success) {
        if (hoveredEntry.overrideField == null || hoveredEntry.prefixField == null) { return; }
        int tooltipWidth = Math.min(400, this.textRenderer.getWidth(text) + 20);
        int tooltipHeight = fieldH;
        int tooltipX = hoveredEntry.prefixField.getX();
        int tooltipY = hoveredEntry.overrideField.getY() - fieldH - 5;

        if (tooltipX + tooltipWidth > this.width) {
            tooltipX = mouseX - tooltipWidth - 10;
        }
        
        if (tooltipY < 0) {
            tooltipY = mouseY + 20;
        }

        int gap = 6 * (int)uiScale;

        context.fill(tooltipX, tooltipY,
                tooltipX + tooltipWidth, tooltipY + tooltipHeight + gap,
                0xFF171717);

        context.drawText(this.textRenderer, text,
                tooltipX, tooltipY + tooltipHeight / 3, success ? 0xFFFFFFFF : 0xFFFF8080, true);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX, mouseY);
        
        Entry newHoveredEntry = null;
        int startIdx = scrollOffset;
        int endIdx = Math.min(startIdx + maxVisibleEntries, entries.size());
        
        for (int i = startIdx; i < endIdx; i++) {
            Entry entry = entries.get(i);
            if (!entry.isHeader && entry.overrideField != null && entry.overrideField.isVisible() &&
                isInside(entry.overrideField.getX(), entry.overrideField.getY(),
                         entry.overrideField.getWidth(), entry.overrideField.getHeight(), 
                         mouseX, mouseY) || (entry.prefixField != null && entry.prefixField.isVisible() &&
                isInside(entry.prefixField.getX(), entry.prefixField.getY(),
                         entry.prefixField.getWidth(), entry.prefixField.getHeight(), 
                         mouseX, mouseY))) {
                newHoveredEntry = entry;
                break;
            }
        }
        
        if (newHoveredEntry != hoveredEntry) {
            hoveredEntry = newHoveredEntry;
            hoverStartTime = System.currentTimeMillis();
        }
    }

    private void addList(DrawContext context) {
        int listTop = 40;
        int listBottom = this.height - 60;
        int listHeight = listBottom - listTop;
        maxVisibleEntries = Math.max(1, listHeight / entryH);
        int totalEntries = entries.size();
        int y = listTop;
        int startIdx = scrollOffset;
        int endIdx = Math.min(startIdx + maxVisibleEntries, entries.size());
        
        for (int i = 0; i < entries.size(); i++) {
            if (i >= startIdx && i < endIdx) {
                Entry entry = entries.get(i);
                entry.setPosition(this.width / 2 - entryW / 2, y);
                entry.setVisible(true);
                y += entryH;
            } else {
                entries.get(i).setVisible(false);
            }
        }

        if (totalEntries > maxVisibleEntries) {
            int scrollX = Math.clamp(this.width / 2 + entryW / 2 + (long)20, 0, this.width - 10);
            renderScrollIndicator(context, scrollX, listTop, listHeight, totalEntries);
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

    private String buildPreview(RuleFactory.SeaCreatureData.CreatureConfig creature, 
                                     RuleFactory.SeaCreatureData.CategoryConfig category) {
        if (category == null) {
            return creature.getDisplayName() + " " + creature.getEmoji();
        }

        String message = creature.getCustomMessage();
        message = message.replace("{name}", creature.getDisplayName());
        message = message.replace("{emoji}", creature.getEmoji());
        message = message.replace("{id}", creature.getId());

        return category.getPrefix() + message;
    }

    private Entry createCategoryHeader(String categoryName) {
        return new Entry(categoryName, "", null, null, false, true);
    }

    private static boolean isInside(int x, int y, int w, int h, double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private class Entry {
        private final VCTextField overrideField;
        private final VCTextField prefixField;
        private final VCLabelField nameField;
        private ButtonWidget toggleBtn;
        private final ButtonWidget saveBtn;
        String replacementText;
        String prefixText;
        Rule rule;
        RuleFactory.SeaCreatureData.CreatureConfig creatureData;
        boolean modified = false;
        boolean isEnabled;
        boolean isHeader;

        public Entry(String creatureName, String currentReplacement, 
                    RuleFactory.SeaCreatureData.CreatureConfig creatureData,
                    Rule existingRule, boolean isEnabled) {
            this(creatureName, currentReplacement, creatureData, existingRule, isEnabled, false);
        }

        public Entry(String creatureName, String currentReplacement, 
                    RuleFactory.SeaCreatureData.CreatureConfig creatureData,
                    Rule existingRule, boolean isEnabled, boolean isHeader) {
            final int offScreenY = -1000;
            
            this.creatureData = creatureData;
            this.rule = existingRule;
            this.isEnabled = isEnabled;
            this.isHeader = isHeader;
            
            this.nameField = new VCLabelField(ScRules.this.textRenderer, 0, offScreenY, nameW, fieldH, Text.literal("Mob"));
            this.nameField.setText(creatureName);
            this.nameField.setUIScale(uiScale);
            
            if (isHeader) {
                this.overrideField = null;
                this.prefixField = null;
                this.toggleBtn = null;
                this.saveBtn = null;
                this.nameField.setText("§b§l" + creatureName);
                this.nameField.setBg(false);
                this.nameField.setWidth(entryW / 2);
            } else {
                this.overrideField = new VCTextField(ScRules.this.textRenderer, 0, offScreenY, fieldW, fieldH, Text.literal("Override text"));   
                this.overrideField.setText(currentReplacement);
                this.overrideField.setEditable(true);
                this.overrideField.setMaxLength(200);        
                this.overrideField.setUIScale(uiScale);
                this.overrideField.setFocused(false);

                this.overrideField.setChangedListener(text -> {
                    this.replacementText = text;
                    this.modified = true;
                });

                String initialPrefixText = existingRule != null ? existingRule.getDhPrefix() : "";
                if (initialPrefixText == null) initialPrefixText = "";
                this.prefixText = initialPrefixText;
                this.prefixField = new VCTextField(ScRules.this.textRenderer, 0, offScreenY, prefixW, fieldH, Text.literal("Prefix"));
                this.prefixField.setText(initialPrefixText);
                this.prefixField.setEditable(true);
                this.prefixField.setMaxLength(100);
                this.prefixField.setCursor(0, false);
                this.prefixField.setUIScale(uiScale);
                this.prefixField.setFocused(false);

                this.prefixField.setChangedListener(text -> {
                    this.prefixText = text;
                    this.modified = true;
                });

                this.saveBtn = VCButton.createNavigationButton(
                    0, offScreenY, btnW, fieldH,
                    Text.literal("Save"),
                    btn -> saveChanges(),
                    uiScale - 0.1f
                );

                this.toggleBtn = VCButton.createMcToggle(
                    0, offScreenY, btnW, fieldH,
                    this.isEnabled,
                    btn -> {
                        this.isEnabled = !this.isEnabled;
                        this.modified = true;
                        VCButton.updateButtonState(btn, this.isEnabled);
                        saveChanges();
                    },
                    uiScale - 0.1f
                );
            }
        }

        public void setPosition(int x, int y) {
            this.nameField.setX(x);
            this.nameField.setY(y);
            
            if (!isHeader) {
                this.prefixField.setX(x + nameW);
                this.prefixField.setY(y);
                this.overrideField.setX(x + nameW + prefixW);
                this.overrideField.setY(y);
                this.saveBtn.setX(x + fieldW + nameW + prefixW);
                this.saveBtn.setY(y);
                this.toggleBtn.setX(x + fieldW + btnW + nameW + prefixW);
                this.toggleBtn.setY(y);
            }
        }

        public void setVisible(boolean visible) {
            this.nameField.setVisible(visible);
            
            if (!isHeader) {
                this.overrideField.setVisible(visible);
                this.prefixField.setVisible(visible);
                this.saveBtn.visible = visible;
                this.toggleBtn.visible = visible;
            }
        }

        public void addToScreen() {
            ScRules.this.addDrawableChild(this.nameField);
            
            if (!isHeader) {
                if (this.overrideField != null) ScRules.this.addDrawableChild(this.overrideField);
                if (this.prefixField != null) ScRules.this.addDrawableChild(this.prefixField);
                if (this.toggleBtn != null) ScRules.this.addDrawableChild(this.toggleBtn);
                if (this.saveBtn != null) ScRules.this.addDrawableChild(this.saveBtn);
            }
        }

        public boolean mouseClicked(double mouseX, double mouseY) {
            if (isHeader) {
                return false;
            }
            if (isInside(saveBtn.getX(), saveBtn.getY(), saveBtn.getWidth(), saveBtn.getHeight(), mouseX, mouseY)) {
                saveBtn.onPress();
                return true;
            }
            if (isInside(toggleBtn.getX(), toggleBtn.getY(), toggleBtn.getWidth(), toggleBtn.getHeight(), mouseX, mouseY)) {
                toggleBtn.onPress();
                return true;
            }
            if (!isInside(overrideField.getX(), overrideField.getY(), overrideField.getWidth(), overrideField.getHeight(), mouseX, mouseY)) {
                overrideField.setFocused(false);
            }
            if (!isInside(prefixField.getX(), prefixField.getY(), prefixField.getWidth(), prefixField.getHeight(), mouseX, mouseY)) {
                prefixField.setFocused(false);
            }
            return false;
        }

        private void saveChanges() {
            if (isHeader) {
                return;
            }
            
            try {
                String ruleName = "sc_" + this.creatureData.getId();
                
                if (this.modified) {
                    String currentOverrideText = this.overrideField != null ? this.overrideField.getText() : "";
                    String currentPrefixText = this.prefixField != null ? this.prefixField.getText() : "";
                    
                    updateRule(ruleName);
                    
                    if (this.overrideField != null) {
                        this.overrideField.setText(currentOverrideText);
                        this.replacementText = currentOverrideText;
                    }
                    if (this.prefixField != null) {
                        this.prefixField.setText(currentPrefixText);
                        this.prefixText = currentPrefixText;
                    }
                }
                
                MinecraftClient.getInstance().getSoundManager().play(
                    PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F)
                );
                
                this.modified = false;
                this.rule = FilterConfig.getAllRules().get(ruleName);
                
            } catch (Exception e) {
                System.err.println("[FishyAddons] Failed to save sea creature configuration: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        private void updateRule(String ruleName) {
            if (this.rule == null) { return; }
            
            String currentReplacementText = this.overrideField != null ? this.overrideField.getText() : this.replacementText;
            String currentPrefixText = this.prefixField != null ? this.prefixField.getText() : this.prefixText;
            boolean isDefaultRule = FilterConfig.getDefaultRules().containsKey(ruleName) && 
                                   !FilterConfig.getUserRules().containsKey(ruleName);
            
            if (isDefaultRule) {
                FilterConfig.Rule modifiedRule = new FilterConfig.Rule(
                    this.rule.getSearchText(),
                    currentReplacementText != null ? currentReplacementText : this.rule.getReplacement(),
                    currentPrefixText != null ? currentPrefixText : this.rule.getDhPrefix(),
                    this.rule.getTriggerMessages(),
                    this.rule.getPriority(),
                    this.isEnabled
                );
                FilterConfig.setUserRule(ruleName, modifiedRule);
            } else {
                if (currentReplacementText != null) {
                    this.rule.setReplacement(currentReplacementText);
                }
                if (currentPrefixText != null) {
                    this.rule.setDhPrefix(currentPrefixText);
                }
                this.rule.setEnabled(this.isEnabled);
            }
        }   
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (handleMenu(mouseX, mouseY)) {
            return true;
        }
        if (handleScrollbar(mouseX, mouseY)) {
            return true;
        }

        int startIdx = scrollOffset;
        int endIdx = Math.min(startIdx + maxVisibleEntries, entries.size());

        for (int i = startIdx; i < endIdx; i++) {
            Entry entry = entries.get(i);
            if (entry.mouseClicked(mouseX, mouseY)) {
                handleEntryFocus(entry, mouseX, mouseY);
                return false;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handleEntryFocus(Entry entry, double mouseX, double mouseY) {
        if (entry.overrideField != null && entry.overrideField.isFocused()) {
            lastFocusedField = entry.overrideField;
            unfocusIfOutside(entry.overrideField, mouseX, mouseY);
        }
        if (entry.prefixField != null && entry.prefixField.isFocused()) {
            lastFocusedField = entry.prefixField;
            unfocusIfOutside(entry.prefixField, mouseX, mouseY);
        }
    }

    private void unfocusIfOutside(VCTextField field, double mouseX, double mouseY) {
        if (!isInside(field.getX(), field.getY(), field.getWidth(), field.getHeight(), mouseX, mouseY)) {
            field.setFocused(false);
        }
    }

    private boolean handleMenu(double mouseX, double mouseY) {
        if (formatMenu != null && formatMenu.isVisible()) {
            if (formatMenu.mouseClicked(mouseX, mouseY)) {
                return true;
            }
            formatMenu.setVisible(false);
            formatTip.setText(F3_TEXT);
            return false;
        }
        return false;
    }

    private boolean handleScrollbar(double mouseX, double mouseY) {
        int listTop = 40;
        int listBottom = this.height - 60;
        int listHeight = listBottom - listTop;
        int scrollbarX = this.width / 2 + entryW / 2 + 20;
        int scrollbarWidth = Math.max(4, (int)(8 * uiScale));
        int totalEntries = entries.size();

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
        if (formatMenu != null && formatMenu.isVisible() && formatMenu.mouseDragged(mouseY)) {
            return true;
        }
        if (isDraggingScrollbar) {
            int listTop = 40;
            int listBottom = this.height - 60;
            int listHeight = listBottom - listTop;
            int totalEntries = entries.size();
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
        if (formatMenu != null && formatMenu.isVisible()) {
            return formatMenu.mouseScrolled(verticalAmount);
        }
        int totalEntries = entries.size();
        if (totalEntries > maxVisibleEntries) {
            scrollOffset -= (int)Math.signum(verticalAmount);
            scrollOffset = Math.clamp(scrollOffset, 0, totalEntries - maxVisibleEntries);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
}