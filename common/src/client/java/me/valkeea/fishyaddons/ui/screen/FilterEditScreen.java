package me.valkeea.fishyaddons.ui.screen;

import com.mojang.blaze3d.platform.InputConstants;

import me.valkeea.fishyaddons.compat.McApi;
import me.valkeea.fishyaddons.feature.filter.FilterConfig;
import me.valkeea.fishyaddons.feature.filter.FilterConfig.Rule;
import me.valkeea.fishyaddons.ui.element.TextFormatMenu;
import me.valkeea.fishyaddons.util.text.Enhancer;
import me.valkeea.fishyaddons.vconfig.ui.layout.Colors;
import me.valkeea.fishyaddons.vconfig.ui.render.VCText;
import me.valkeea.fishyaddons.vconfig.ui.screen.AdjustedScreen;
import me.valkeea.fishyaddons.vconfig.ui.widget.FaButton;
import me.valkeea.fishyaddons.vconfig.ui.widget.VCPopup;
import me.valkeea.fishyaddons.vconfig.ui.widget.VCTextField;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public class FilterEditScreen extends AdjustedScreen {
    private static final int FIELDW = 300;
    private static final int ELH = 20;
    private static final int CONTENTW = FIELDW + FIELDW / 2;
    private final Screen parent;
    private final Rule initialData;
    private final String filterKey;

    private TextFormatMenu searchMenu;
    private VCTextField keyField;
    private VCTextField overrideField;
    private VCTextField formatField;

    private boolean menuInteractionActive = false;    

    public FilterEditScreen(String key, Rule data, Screen parent) {
        super(VCText.header(
            "FishyAddons Chat Filters and Overrides",
            Style.EMPTY.withBold(true))
        );
        this.parent = parent;
        this.filterKey = key;
        this.initialData = data != null ? data : new FilterConfig.Rule(
            "", "", 40, true, true
        );
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int centerY = this.height / 2 - 10;

        int x = centerX - CONTENTW / 2;
        int y = centerY + 50;        

        keyField = new VCTextField(this.font, x, y, FIELDW, ELH, Component.literal("Key"));
        keyField.setMaxLength(100);
        keyField.setValue(filterKey);
        this.addRenderableWidget(keyField);

        overrideField = new VCTextField(this.font, x, y + 40, FIELDW, ELH, Component.literal("Chat message"));
        overrideField.setMaxLength(100);
        overrideField.setValue(initialData.getReplacement());
        this.addRenderableWidget(overrideField);

        formatField = new VCTextField(this.font, x + FIELDW, y + 40, FIELDW / 2, ELH, Component.literal("Text Formats"));
        this.addRenderableWidget(formatField);

        int sx = formatField.getX();
        int sy = formatField.getY();
        int width = formatField.getWidth();
        int actualEntryHeight = formatField.getHeight();

        searchMenu = new TextFormatMenu(
            sx, sy, width,
            this::insertFormatIntoFocusedField,
            formatField,
            1.0f
        );
        int menuHeight = this.height - (sy + actualEntryHeight) - 20;
        searchMenu.setMaxEntries(menuHeight / actualEntryHeight);

        this.addRenderableWidget(new FaButton(x + FIELDW / 2, formatField.getY() + 80, 80, 20,
            Component.literal("Save").withStyle(style -> style.withColor(0xFFE2CAE9)),
            btn -> {
            save();
            onClose();
        }));

        this.addRenderableWidget(new FaButton(x + FIELDW / 2 - 80, formatField.getY() + 80, 80, 20,
            Component.literal("Cancel").withStyle(style -> style.withColor(0xFFE2CAE9)),
            btn -> onClose()
        ));
    }

    private void insertFormatIntoFocusedField(String format) {
            String currentText = overrideField.getValue();
            int caretPos = overrideField.getCursorPosition();

            String newText = currentText.substring(0, caretPos) + format + currentText.substring(caretPos);
            overrideField.setValue(newText);
            overrideField.moveCursorTo(caretPos + format.length(), false);
            this.setFocused(overrideField);

        if (searchMenu != null) {
            searchMenu.setVisible(false);
            formatField.setFocused(false);
            menuInteractionActive = false;
        }
    }

    private void save() {
         String newKey = keyField.getValue().trim();
        if (newKey.isEmpty()) {
            warn();
            return;
        }

        var newData = new FilterConfig.Rule(
            newKey,
            overrideField.getValue().trim(),
            40,
            true,
            initialData.requireFullMatch()
        );

        if (!newKey.equals(filterKey)) {
            FilterConfig.removeUserRule(filterKey);
        }
        FilterConfig.setUserRule(newKey, newData);
    }

	public void warn() {      
        var popup = new VCPopup(
            Component.literal("Empty field detected! Would you like to restore it?"),
            "No", this::onClose,
            "Yes", () -> keyField.setValue(filterKey),
            1.0f
            );
        McApi.setScreen(new Overlay(McApi.screen(), popup));
	}    

    @Override
    public void extractContent(GuiGraphicsExtractor gge, int mX, int mY, float delta) {
        super.extractContent(gge, mX, mY, delta);
        gge.blurBeforeThisStratum();

        int centerX = this.width / 2;
        int x = centerX - CONTENTW / 2;        
        renderGuideText(gge, x);

        if (searchMenu != null) {
            boolean fieldActive = formatField.isFocused();
            if (fieldActive) menuInteractionActive = true;
            searchMenu.setVisible(fieldActive || menuInteractionActive);
            if (searchMenu.isVisible()) {
                searchMenu.render(gge, parent, mX, mY);
            }
        }

        int centerY = this.height / 2 - 10;
        int y = centerY + 40;
        int color = Colors.CLOSE_BUTTON_TEXT;

        gge.text(this.font, "Filtered Message (Clean String)", x, y, color, false);
        gge.text(this.font, "Override (Formatted)", x, y + 40, color, false);
        gge.text(this.font, "Text Format (Click)", x + FIELDW, y + 40, color, false);

        checkTooltip(gge, mX, mY);
    }

    private void checkTooltip(GuiGraphicsExtractor gge, int mX, int mY) {
        if (overrideField != null && overrideField.isMouseOver(mX, mY)) {
            
            String previewText = overrideField.getValue().trim();
            if (!previewText.isEmpty()) {

                try {
                    var tt = Enhancer.parseFormattedText(previewText);
                    int ttW = Math.min(400, this.font.width(tt) + 20);
                    int ttH = overrideField.getHeight();
                    int ttX = overrideField.getX();
                    int ttY = overrideField.getY() + ttH + 5;
                    
                    if (ttX + ttW > this.width) ttX = mX - ttW - 10;
                    if (ttY < 0) ttY = mY + 20;
                    gge.fill(ttX, ttY, ttX + ttW + 6, ttY + ttH + 4, 0xFF171717);
                    gge.text(this.font, tt, ttX + 3, ttY + ttH / 3, 0xFFFFFFFF, true);
                                   
                } catch (Exception e) {
                    System.err.println("[FishyAddons] Error rendering tooltip: " + e.getMessage());
                    renderFallback(mX, mY, gge);
                }
            }
        }
    }

    private void renderFallback(int mX, int mY, GuiGraphicsExtractor gge) {
        String errorText = "Error rendering preview";
        int ttW = Math.min(400, this.font.width(errorText) + 20);
        int ttH = 20;
        int ttX = mX + 10;
        int ttY = mY - ttH - 10;
        if (ttX + ttW > this.width) ttX = mX - ttW - 10;
        if (ttY < 0) ttY = mY + 20;
                    
        gge.fill(ttX - 5, ttY - 5, ttX + ttW + 5, ttY + ttH + 5,0xFF171717);
        gge.text(this.font, Component.literal(errorText),
            ttX + 5, ttY + 10, 0xFFFF8080, true);
    }

    public void renderGuideText(GuiGraphicsExtractor gge, int x) {
        int y = this.height / 2 - 175;
        int lineH = 15;

        gge.text(this.font, getTitle(), x, y, 0xFFFFFFFF);            

        y += lineH * 2; 
        String[] instructions = {
            " The First field is required to successfully create a rule!",
            " Leaving the override field empty results in the message being removed.",
            "",
            "- Text Formats -",
            " • §7Search for formats and click to insert at caret position.",
            "   §7You can also type them manually.",
            " • §7For all codes, '&' and the section symbol can be used interchangeably.",
            " • §7All legacy formatting is available (&l, &6..).",
            "- Custom Formats -",
            " • §7&[hexcode>hexcode...] §8for gradients",
            " • §7&{hexcode} §8for solid RGB colors",
        };

        for (var line : instructions) {
            if (line.isEmpty()) {
                y += lineH / 2;
                continue;
            }

            var format = ChatFormatting.GRAY;
            if (line.startsWith(" •") || line.matches("\\d+\\..*")) {
                format = ChatFormatting.AQUA;
            } else if (line.contains("-")) {
                format = ChatFormatting.DARK_AQUA;
            } else if (line.startsWith(" The")) {
                format = ChatFormatting.DARK_GRAY;
            }
     
            var text = Component.literal(line).withStyle(format);
            gge.text(this.font, text, x, y, 0xFFFFFFFF);
            y += lineH;
        }              
    }   

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        var adjusted = adjustClick(click);
        if (searchMenu != null && searchMenu.isVisible() && searchMenu.mouseClicked(adjusted)) {
            return true;
        }

        double mX = adjusted.x();
        double mY = adjusted.y();

        if (click.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            if (formatField.isFocused() && !formatField.isMouseOver(mX, mY)) formatField.setFocused(false);
            if (overrideField.isFocused() && !overrideField.isMouseOver(mX, mY)) overrideField.setFocused(false);
        }
        
        checkMenuInteraction(mX, mY);
        
        boolean r = super.mouseClicked(click, doubled);
        if (!r && !menuInteractionActive) this.setFocused(null);
        return r;
    }

    private void checkMenuInteraction(double mX, double mY) {
        if (searchMenu != null && searchMenu.isVisible()) {
            boolean clickedOnField = formatField.isMouseOver(mX, mY) || 
                                   overrideField.isMouseOver(mX, mY) || 
                                   keyField.isMouseOver(mX, mY);
            boolean clickedOnMenu = searchMenu.isMouseOver((int)mX, (int)mY);
            
            if (!clickedOnField && !clickedOnMenu) {
                menuInteractionActive = false;
            }
        }
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (input.isEscape()) menuInteractionActive = false;
        if (searchMenu != null && formatField.isFocused() && searchMenu.keyPressed(input)) {
            return true;
        }
        if (overrideField.isFocused()) return overrideField.keyPressed(input);
        if (formatField.isFocused() && formatField.keyPressed(input)) return true;
        return super.keyPressed(input);
    }

    @Override
    public boolean mouseScrolled(double mX, double mY, double horizontalAmount, double verticalAmount) {
        if (searchMenu != null && searchMenu.isVisible()) {
            return searchMenu.mouseScrolled(verticalAmount);
        }
        return super.mouseScrolled(mX, mY, horizontalAmount, verticalAmount);
    }

    @Override 
    public boolean mouseDragged(MouseButtonEvent click, double h, double v) {
        if (searchMenu != null && searchMenu.isVisible() && searchMenu.mouseDragged(adjustClick(click))) {
            return true;
        }
        return super.mouseDragged(click, h, v);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (searchMenu != null && searchMenu.isVisible()) {
            searchMenu.mouseReleased();
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        McApi.setScreen(parent);
    }    
}
