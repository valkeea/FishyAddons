package me.valkeea.fishyaddons.ui.list;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.mojang.blaze3d.platform.InputConstants;

import me.valkeea.fishyaddons.feature.filter.FilterConfig;
import me.valkeea.fishyaddons.feature.filter.FilterConfig.Rule;
import me.valkeea.fishyaddons.feature.filter.RuleFactory;
import me.valkeea.fishyaddons.feature.filter.RuleFactory.SeaCreatureData.CreatureConfig;
import me.valkeea.fishyaddons.ui.element.TextFormatMenu;
import me.valkeea.fishyaddons.util.text.Enhancer;
import me.valkeea.fishyaddons.vconfig.ui.manager.ScreenManager;
import me.valkeea.fishyaddons.vconfig.ui.render.VCText;
import me.valkeea.fishyaddons.vconfig.ui.screen.AdjustedScreen;
import me.valkeea.fishyaddons.vconfig.ui.widget.FaButton;
import me.valkeea.fishyaddons.vconfig.ui.widget.VCButton;
import me.valkeea.fishyaddons.vconfig.ui.widget.VCLabelField;
import me.valkeea.fishyaddons.vconfig.ui.widget.VCTextField;
import me.valkeea.fishyaddons.vconfig.ui.widget.VCVisuals;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public final class ScRules extends AdjustedScreen {
    private static final String F3_TEXT = "§7Click a field and press F3 to apply formats...";

    private static final int NAMEW = 120;
    private static final int PREFIXW = 150;
    private static final int BTNW = 40;
    private static final int BTNH = 20;    
    private static final int FIELDW = 330;
    private static final int FIELDH = 20;
    private static final int ENTRYH = 28;
    private static final int ENTRYW = NAMEW + PREFIXW + FIELDW + 3 * BTNW;

    private final List<Entry> entries = new ArrayList<>();
    
    private TextFormatMenu formatMenu;
    private VCLabelField formatTip;
    private VCTextField lastFocusedField;

    private boolean isDraggingScrollbar = false;
    private int scrollOffset = 0;
    private int maxVisibleEntries = 0;
    private int scrollKnobOffset = 0;

    private Entry hoveredEntry = null;
    private long hoverStartTime = 0;
    private static final long TOOLTIP_DELAY = 250;

    public ScRules() {
        super(VCText.header("Sea Creature Messages", Style.EMPTY.withBold(true)));
    } 

    @Override
    protected void init() {
        super.init();

        if (!FilterConfig.areScRulesLoaded()) {
            FilterConfig.refreshScRules();
        }
        
        entries.clear();        
        this.clearWidgets();

        var allCreatures = FilterConfig.getSeaCreatureData();
        var categories = FilterConfig.getSeaCreatureCategories();
        
        sort(allCreatures);
        String currentCategory = null;
        
        for (var creature : allCreatures) {

            String ruleName = "sc_" + creature.id();
            if (!creature.category().equals(currentCategory)) {
                currentCategory = creature.category();
                String categoryDisplay = currentCategory.substring(0, 1).toUpperCase() + currentCategory.substring(1);
                
                Entry categoryHeader = createCategoryHeader("── " + categoryDisplay + " ──");
                entries.add(categoryHeader);
                categoryHeader.addToScreen();
            }
            
            var existingRule = FilterConfig.getAllRules().get(ruleName);
            var category = categories.get(creature.category());
            var displayName = creature.displayName();
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
        maxVisibleEntries = Math.max(1, listHeight / ENTRYH);
        int maxScroll = Math.max(0, totalEntries - maxVisibleEntries);
        scrollOffset = Math.clamp(scrollOffset, 0, maxScroll);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        if (scrollOffset < 0) scrollOffset = 0;  

        var backButton = new FaButton(
            this.width / 2 - ENTRYW / 2 + BTNW, this.height - 40, BTNW, BTNH,
            Component.literal("Back").withStyle(style -> style.withColor(0xFF808080)),
            btn -> ScreenManager.openConfigScreen()
        );
        this.addRenderableWidget(backButton);

        var closeButton = new FaButton(
            this.width / 2 - ENTRYW / 2 + BTNW * 2, this.height - 40, BTNW, BTNH,
            Component.literal("Close").withStyle(style -> style.withColor(0xFF808080)),
            btn -> this.onClose()
        );
        this.addRenderableWidget(closeButton);
        
        formatTip = new VCLabelField(
            this.font, this.width / 2 - FIELDW / 2, this.height - 40, NAMEW * 2, FIELDH, Component.literal(F3_TEXT));
        formatTip.setUIScale(1.0f);
        formatTip.setBg(false);
        this.addRenderableWidget(formatTip);

        formatMenu = new TextFormatMenu(
            Math.max(0, this.width / 2 - ENTRYW / 2 - NAMEW - BTNW), listTop, NAMEW,
            this::insertAtCaret,
            1.0f
        );

        int availableHeight = listBottom - listTop;
        int maxEntriesByHeight = availableHeight / FIELDH;
        formatMenu.setMaxEntries(maxEntriesByHeight);
    }

    private void sort(List<RuleFactory.SeaCreatureData.CreatureConfig> creatures) {
        Map<String, Integer> categoryOrder = new LinkedHashMap<>();
        int order = 0;
        for (var creature : creatures) {
            if (!categoryOrder.containsKey(creature.category())) {
                categoryOrder.put(creature.category(), order++);
            }
        }
        
        creatures.sort((a, b) -> {
            int categoryComparison = Integer.compare(
                categoryOrder.get(a.category()), 
                categoryOrder.get(b.category())
            );

            if (categoryComparison == 0) {
                return a.displayName().compareTo(b.displayName());
            }
            return categoryComparison;
        });
    }

    private void insertAtCaret(String format) {
        VCTextField focusedField = null;
        for (var entry : entries) {
            if (entry.overrideField != null && entry.overrideField.isFocused()) {
                focusedField = entry.overrideField;
                lastFocusedField = focusedField;
            } else if (entry.prefixField != null && entry.prefixField.isFocused()) {
                focusedField = entry.prefixField;
                lastFocusedField = focusedField;
            }
            if (focusedField != null) break;
        }
        
        if (focusedField == null && lastFocusedField != null) {
            focusedField = lastFocusedField;
            focusedField.setFocused(true);
        }
        
        if (focusedField != null) apply(focusedField, format);
        formatMenu.setVisible(false);
    }
    
    @Override
    public boolean keyPressed(KeyEvent input) {
        if (formatMenu != null && formatMenu.isVisible() && formatMenu.keyPressed(input)) {
            return true;
        }
        
        if (input.key() == InputConstants.KEY_F3) {
            toggleFormatMenu();
            return true;
        }
        
        return super.keyPressed(input);
    }

    private void apply(VCTextField field, String format) {
        String currentText = field.getValue();
        int caretPos = field.getCursorPosition();    
        String newText = currentText.substring(0, caretPos) + format + currentText.substring(caretPos);
        field.setValue(newText);
        field.moveCursorTo(caretPos + format.length(), false);
            
        for (Entry e : entries) {
            if (e.overrideField == field) {
                e.replacementText = newText;
                e.modified = true;
                return;
            } else if (e.prefixField == field) {
                e.prefixText = newText;
                e.modified = true;
                return;
            }
        }
        field.setFocused(true);
    }
    
    @Override
    public void tick() {
        super.tick();
        
        for (Entry e : entries) {
            if (e.overrideField != null && e.overrideField.isFocused()) {
                lastFocusedField = e.overrideField;
            } else if (e.prefixField != null && e.prefixField.isFocused()) {
                lastFocusedField = e.prefixField;
            }
            if (lastFocusedField != null) break;
        }
    }
    
    private void toggleFormatMenu() {
        VCTextField focusedField = null;
        for (Entry e : entries) {
            if (e.overrideField != null && e.overrideField.isFocused()) {
                focusedField = e.overrideField;
            } else if (e.prefixField != null && e.prefixField.isFocused()) {
                focusedField = e.prefixField;
            }
            if (focusedField != null) break;
        }

        if (formatMenu != null) {
            boolean shouldShow = !formatMenu.isVisible();
            formatMenu.setVisible(shouldShow);
            if (shouldShow) formatTip.setText("§7Press F3 to apply formats...");
            else formatTip.setText(F3_TEXT);
        }
    }

    @Override
    public void extractContent(GuiGraphicsExtractor gge, int mX, int mY, float delta) {
        super.extractContent(gge, mX, mY, delta);
        gge.blurBeforeThisStratum();
        gge.centeredText(this.font,getTitle(), this.width / 2, 15, 0xFF55FFFF);
          
        addList(gge);
        if (formatMenu != null && formatMenu.isVisible()) {
            formatMenu.render(gge, this, mX, mY);
        }

        tryTooltip(gge, mX, mY);
    }

    private void tryTooltip(GuiGraphicsExtractor gge, int mX, int mY) {
        if (shouldShowTooltip()) {
            String previewText = buildPreview();
            if (!previewText.isEmpty()) {
                try {
                    var tt = Enhancer.parseFormattedText(previewText);
                    renderTooltip(gge, mX, mY, tt, true);
                } catch (Exception e) {
                    System.err.println("Error rendering preview: " + e.getMessage());
                    renderTooltip(gge, mX, mY, Component.literal("Error rendering preview"), false);
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
        if (hoveredEntry.overrideField != null && !hoveredEntry.overrideField.getValue().isEmpty()) {
            previewText = hoveredEntry.overrideField.getValue();
        } else if (hoveredEntry.replacementText != null && !hoveredEntry.replacementText.isEmpty()) {
            previewText = hoveredEntry.replacementText;
        }

        if (hoveredEntry.prefixField != null && !hoveredEntry.prefixField.getValue().isEmpty()) {
            previewText = hoveredEntry.prefixField.getValue() + " " + previewText;
        } else if (hoveredEntry.prefixText != null && !hoveredEntry.prefixText.isEmpty()) {
            previewText = hoveredEntry.prefixText + previewText;
        }
        return previewText;
    }

    private void renderTooltip(GuiGraphicsExtractor gge, int mX, int mY, Component text, boolean success) {
        if (hoveredEntry.overrideField == null || hoveredEntry.prefixField == null) { return; }
        int ttW = Math.min(400, this.font.width(text) + 20);
        int ttH = FIELDH;
        int ttX = hoveredEntry.prefixField.getX();
        int ttY = hoveredEntry.overrideField.getY() - FIELDH - 5;

        if (ttX + ttW > this.width) ttX = mX - ttW - 10;
        if (ttY < 0) ttY = mY + 20;

        int gap = 6;
        gge.fill(ttX, ttY, ttX + ttW, ttY + ttH + gap, 0xFF171717);
        gge.text(this.font, text, ttX, ttY + ttH / 3, success ? 0xFFFFFFFF : 0xFFFF8080, true);
    }


    @Override
    public void mouseMoved(double mX, double mY) {
        super.mouseMoved(mX, mY);
        var adjusted = adjustClick(
            new MouseButtonEvent(mX, mY,
                new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)
        ));
        
        Entry newHoveredEntry = null;
        int startIdx = scrollOffset;
        int endIdx = Math.min(startIdx + maxVisibleEntries, entries.size());
        
        for (int i = startIdx; i < endIdx; i++) {
            Entry e = entries.get(i);
            if (!e.isHeader && e.isPreviewEntry(adjusted.x(), adjusted.y())) {
                newHoveredEntry = e;
                break;
            }
        }
        
        if (newHoveredEntry != hoveredEntry) {
            hoveredEntry = newHoveredEntry;
            hoverStartTime = System.currentTimeMillis();
        }
    }

    private void addList(GuiGraphicsExtractor gge) {
        int listTop = 40;
        int listBottom = this.height - 60;
        int listHeight = listBottom - listTop;
        maxVisibleEntries = Math.max(1, listHeight / ENTRYH);
        int totalEntries = entries.size();
        int y = listTop;
        int startIdx = scrollOffset;
        int endIdx = Math.min(startIdx + maxVisibleEntries, entries.size());
        
        for (int i = 0; i < entries.size(); i++) {
            if (i >= startIdx && i < endIdx) {
                Entry entry = entries.get(i);
                entry.setPosition(this.width / 2 - ENTRYW / 2, y);
                entry.setVisible(true);
                y += ENTRYH;
            } else {
                entries.get(i).setVisible(false);
            }
        }

        if (totalEntries > maxVisibleEntries) {
            int scrollX = Math.clamp(this.width / 2 + ENTRYW / 2 + (long)20, 0, this.width - 10);
            renderScrollIndicator(gge, scrollX, listTop, listHeight, totalEntries);
        }
    }

    private void renderScrollIndicator(GuiGraphicsExtractor gge, int x, int y, int listHeight, int totalEntries) {
        int scrollbarWidth = 4;
        gge.fill(x, y, x + scrollbarWidth, y + listHeight, 0x44000000);
        if (totalEntries > maxVisibleEntries) {
            int thumbHeight = Math.max(10, (maxVisibleEntries * listHeight) / totalEntries);
            int thumbY = y + (scrollOffset * (listHeight - thumbHeight)) / (totalEntries - maxVisibleEntries);
            gge.fill(x + 1, thumbY, x + scrollbarWidth - 1, thumbY + thumbHeight, VCVisuals.getThemeColor());
            gge.fill(x + 1, thumbY + thumbHeight - 1, x + scrollbarWidth - 1, thumbY + thumbHeight, 0xFF000000);
        }
    }

    private String buildPreview(RuleFactory.SeaCreatureData.CreatureConfig creature, 
                                     RuleFactory.SeaCreatureData.CategoryConfig category) {
        if (category == null) {
            return creature.displayName() + " " + creature.emoji();
        }

        String message = creature.customMessage();
        message = message.replace("{name}", creature.displayName());
        message = message.replace("{emoji}", creature.emoji());
        message = message.replace("{id}", creature.id());

        return category.prefix() + message;
    }

    private Entry createCategoryHeader(String categoryName) {
        return new Entry(categoryName, "", null, null, false, true);
    }

    private static boolean isInside(int x, int y, int w, int h, double mX, double mY) {
        return mX >= x && mX < x + w && mY >= y && mY < y + h;
    }

    private class Entry {
        private final VCTextField overrideField;
        private final VCTextField prefixField;
        private final VCLabelField nameField;
        private Button toggleBtn;
        private final Button saveBtn;

        String replacementText;
        String prefixText;
        Rule rule;
        CreatureConfig creatureData;
        boolean modified = false;
        boolean isEnabled;
        boolean isHeader;

        public Entry(String creatureName, String currentReplacement, 
                    CreatureConfig creatureData, Rule existingRule, boolean isEnabled) {
            this(creatureName, currentReplacement, creatureData, existingRule, isEnabled, false);
        }

        public Entry(String creatureName, String currentReplacement, 
                    CreatureConfig creatureData, Rule existingRule, boolean isEnabled, boolean isHeader) {
            final int offScreenY = -1000;
            
            this.creatureData = creatureData;
            this.rule = existingRule;
            this.isEnabled = isEnabled;
            this.isHeader = isHeader;
            
            this.nameField = new VCLabelField(ScRules.this.font, 0, offScreenY, NAMEW, FIELDH, Component.literal("Mob"));
            this.nameField.setText(creatureName);
            this.nameField.setUIScale(1.0f);
            
            if (isHeader) {
                this.overrideField = null;
                this.prefixField = null;
                this.toggleBtn = null;
                this.saveBtn = null;
                this.nameField.setText("§b§l" + creatureName);
                this.nameField.setBg(false);
                this.nameField.setWidth(ENTRYW / 2);
            } else {
                this.overrideField = new VCTextField(ScRules.this.font, 0, offScreenY, FIELDW, FIELDH, Component.literal("Override text"));   
                this.overrideField.setValue(currentReplacement);
                this.overrideField.setEditable(true);
                this.overrideField.setMaxLength(200);        
                this.overrideField.setFocused(false);

                this.overrideField.setResponder(text -> {
                    this.replacementText = text;
                    this.modified = true;
                });

                String initialPrefixText = existingRule != null ? existingRule.getDhPrefix() : "";
                if (initialPrefixText == null) initialPrefixText = "";
                this.prefixText = initialPrefixText;
                this.prefixField = new VCTextField(ScRules.this.font, 0, offScreenY, PREFIXW, FIELDH, Component.literal("Prefix"));
                this.prefixField.setValue(initialPrefixText);
                this.prefixField.setEditable(true);
                this.prefixField.setMaxLength(100);
                this.prefixField.moveCursorTo(0, false);
                this.prefixField.setFocused(false);

                this.prefixField.setResponder(text -> {
                    this.prefixText = text;
                    this.modified = true;
                });

                this.saveBtn = VCButton.createNavigationButton(
                    0, offScreenY, BTNW, FIELDH,
                    Component.literal("Save"),
                    btn -> saveChanges(),
                    1.0f
                );

                this.toggleBtn = VCButton.createMcToggle(
                    0, offScreenY, BTNW, FIELDH,
                    this.isEnabled,
                    btn -> {
                        this.isEnabled = !this.isEnabled;
                        this.modified = true;
                        VCButton.updateButtonState(btn, this.isEnabled);
                        saveChanges();
                    },
                    1.0f
                );
            }
        }

        private void visitWidgets(Consumer<AbstractWidget> visitor) {
            if (prefixField != null) visitor.accept(prefixField);
            if (overrideField != null) visitor.accept(overrideField);
            if (saveBtn != null) visitor.accept(saveBtn);
            if (toggleBtn != null) visitor.accept(toggleBtn);                      
        }  

        public void setPosition(int x, int y) {
            this.nameField.setX(x);
            this.nameField.setY(y);
            
            if (!isHeader) {
                x += NAMEW;
                this.prefixField.setX(x);
                x += PREFIXW;
                this.overrideField.setX(x);
                x += FIELDW;
                this.saveBtn.setX(x);
                x += BTNW;
                this.toggleBtn.setX(x);
                visitWidgets(wg -> wg.setY(y));
            }
        }

        public void setVisible(boolean visible) {
            this.nameField.setVisible(visible);
            if (!isHeader) visitWidgets(wg -> wg.visible = visible);
        }

        public void addToScreen() {
            ScRules.this.addRenderableWidget(this.nameField);
            if (!isHeader) visitWidgets(wg -> ScRules.this.addRenderableWidget(wg));
        }

        public boolean mouseClicked(MouseButtonEvent click) {
            if (isHeader)  return false;

            double mX = click.x();
            double mY = click.y();

            if (!overrideField.isMouseOver(mX, mY)) overrideField.setFocused(false);
            if (!prefixField.isMouseOver(mX, mY)) prefixField.setFocused(false);

            if (saveBtn.isMouseOver(mX, mY)) {
                saveBtn.onPress(click);
                return true;
            }

            if (toggleBtn.isMouseOver(mX, mY)) {
                toggleBtn.onPress(click);
                return true;
            }
            return false;
        }

        public boolean isPreviewEntry(double mX, double mY) {
            return (overrideField != null && overrideField.isMouseOver(mX, mY) && overrideField.isVisible()) ||
                (prefixField != null && prefixField.isMouseOver(mX, mY) && prefixField.isVisible());
        }

        private void saveChanges() {
            if (isHeader) return;
            
            try {
                String ruleName = "sc_" + this.creatureData.id();
                
                if (this.modified) {
                    String currentOverrideText = this.overrideField != null ? this.overrideField.getValue() : "";
                    String currentPrefixText = this.prefixField != null ? this.prefixField.getValue() : "";
                    
                    updateRule(ruleName);
                    
                    if (this.overrideField != null) {
                        this.overrideField.setValue(currentOverrideText);
                        this.replacementText = currentOverrideText;
                    }
                    if (this.prefixField != null) {
                        this.prefixField.setValue(currentPrefixText);
                        this.prefixText = currentPrefixText;
                    }
                }
                
                this.modified = false;
                this.rule = FilterConfig.getAllRules().get(ruleName);
                
            } catch (Exception e) {
                System.err.println("[FishyAddons] Failed to save sea creature configuration: " + e.getMessage());
            }
        }
        
        private void updateRule(String ruleName) {
            if (this.rule == null) return;
            
            String currentReplacementText = this.overrideField != null ? this.overrideField.getValue() : this.replacementText;
            String currentPrefixText = this.prefixField != null ? this.prefixField.getValue() : this.prefixText;
            
            if (FilterConfig.getDefaultRules().containsKey(ruleName)) {
                var modifiedRule = new Rule(
                    this.rule.getSearchText(),
                    currentReplacementText != null ? currentReplacementText : this.rule.getReplacement(),
                    currentPrefixText != null ? currentPrefixText : this.rule.getDhPrefix(),
                    this.rule.getTriggerMessages(),
                    this.rule.getPriority(),
                    this.isEnabled
                );
                FilterConfig.setUserRule(ruleName, modifiedRule);
            }
        }   
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        var adjusted = adjustClick(click);
        if (handleMenu(adjusted)) return true;
        if (handleScrollbar(adjusted.x(), adjusted.y())) return true;

        int startIdx = scrollOffset;
        int endIdx = Math.min(startIdx + maxVisibleEntries, entries.size());

        for (int i = startIdx; i < endIdx; i++) {
            Entry entry = entries.get(i);
            if (entry.mouseClicked(adjusted)) {
                handleEntryFocus(entry, adjusted.x(), adjusted.y());
                return false;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    private void handleEntryFocus(Entry entry, double mX, double mY) {
        if (entry.overrideField != null && entry.overrideField.isFocused()) {
            lastFocusedField = entry.overrideField;
            unfocusIfOutside(entry.overrideField, mX, mY);
        }
        if (entry.prefixField != null && entry.prefixField.isFocused()) {
            lastFocusedField = entry.prefixField;
            unfocusIfOutside(entry.prefixField, mX, mY);
        }
    }

    private void unfocusIfOutside(VCTextField field, double mX, double mY) {
        if (!isInside(field.getX(), field.getY(), field.getWidth(), field.getHeight(), mX, mY)) {
            field.setFocused(false);
        }
    }

    private boolean handleMenu(MouseButtonEvent click) {
        if (formatMenu != null && formatMenu.isVisible()) {
            if (formatMenu.mouseClicked(click)) return true;
            formatMenu.setVisible(false);
            formatTip.setText(F3_TEXT);
            return false;
        }
        return false;
    }

    private boolean handleScrollbar(double mX, double mY) {
        int listTop = 40;
        int listBottom = this.height - 60;
        int listHeight = listBottom - listTop;
        int scrollbarX = this.width / 2 + ENTRYW / 2 + 20;
        int scrollbarWidth = 8;
        int totalEntries = entries.size();

        if (totalEntries > maxVisibleEntries
            && mX >= scrollbarX && mX <= scrollbarX + scrollbarWidth
            && mY >= listTop && mY <= listTop + listHeight) {
            int thumbHeight = Math.max(10, (maxVisibleEntries * listHeight) / totalEntries);
            int thumbY = listTop + (scrollOffset * (listHeight - thumbHeight)) / (totalEntries - maxVisibleEntries);
            if (mY >= thumbY && mY <= thumbY + thumbHeight) {
                isDraggingScrollbar = true;
                scrollKnobOffset = (int)mY - thumbY;
            } else {
                isDraggingScrollbar = true;
                scrollKnobOffset = thumbHeight / 2;
                double trackClickY = mY - listTop - scrollKnobOffset;
                double scrollPercent = trackClickY / (listHeight - thumbHeight);
                int newScrollOffset = (int)(scrollPercent * (totalEntries - maxVisibleEntries));
                scrollOffset = Math.clamp(newScrollOffset, 0, totalEntries - maxVisibleEntries);
            }
            return true;
        }
        return false;
    }        

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        isDraggingScrollbar = false;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double h, double v) {
        var adjusted = adjustClick(click);
        if (formatMenu != null && formatMenu.isVisible() && formatMenu.mouseDragged(adjusted)) {
            return true;
        }
        if (isDraggingScrollbar) {
            int listTop = 40;
            int listBottom = this.height - 60;
            int listHeight = listBottom - listTop;
            int totalEntries = entries.size();
            int thumbHeight = Math.max(10, (maxVisibleEntries * listHeight) / totalEntries);
            int mouseThumbY = (int)adjusted.y() - listTop - scrollKnobOffset;
            double scrollPercent = mouseThumbY / (double)(listHeight - thumbHeight);
            int newScrollOffset = (int)(scrollPercent * (totalEntries - maxVisibleEntries));
            scrollOffset = Math.clamp(newScrollOffset, 0, totalEntries - maxVisibleEntries);
            return true;
        }
        return super.mouseDragged(click, h, v);
    }

    @Override
    public boolean mouseScrolled(double mX, double mY, double h, double v) {
        if (formatMenu != null && formatMenu.isVisible()) {
            return formatMenu.mouseScrolled(v);
        }
        int totalEntries = entries.size();
        if (totalEntries > maxVisibleEntries) {
            scrollOffset -= (int)Math.signum(v);
            scrollOffset = Math.clamp(scrollOffset, 0, totalEntries - maxVisibleEntries);
            return true;
        }
        return super.mouseScrolled(mX, mY, h, v);
    }
}
