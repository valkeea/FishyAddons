package me.valkeea.fishyaddons.ui.screen;

import me.valkeea.fishyaddons.feature.filter.FilterConfig;
import me.valkeea.fishyaddons.feature.filter.FilterConfig.Rule;
import me.valkeea.fishyaddons.ui.element.TextFormatMenu;
import me.valkeea.fishyaddons.util.text.Enhancer;
import me.valkeea.fishyaddons.vconfig.ui.render.VCText;
import me.valkeea.fishyaddons.vconfig.ui.widget.FaButton;
import me.valkeea.fishyaddons.vconfig.ui.widget.VCPopup;
import me.valkeea.fishyaddons.vconfig.ui.widget.VCTextField;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public class FilterEditScreen extends Screen {
    private final Screen parent;
    private final Rule initialData;
    private String filterKey;

    private TextFormatMenu searchMenu;
    private boolean menuInteractionActive = false;
    private VCTextField keyField;
    private VCTextField overrideField;
    private VCTextField formatField;

    public FilterEditScreen(String key, Rule data, Screen parent) {
        super(Component.literal("Configure Chat Overrides"));
        this.parent = parent;
        this.filterKey = key;
        this.initialData = data != null ? data : new FilterConfig.Rule(
            "", "", 40, true, true
        );
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2 - 10;

        int w = 300;
        int h = 20;

        int x = centerX - w / 2;
        int y = centerY + 50;        

        keyField = new VCTextField(this.font, x, y, w, h, Component.literal("Key"));
        keyField.setMaxLength(100);
        keyField.setValue(filterKey);
        this.addRenderableWidget(keyField);

        overrideField = new VCTextField(this.font, x, y + 40, w, h, Component.literal("Chat message"));
        overrideField.setMaxLength(100);
        overrideField.setValue(initialData.getReplacement());
        this.addRenderableWidget(overrideField);

        formatField = new VCTextField(this.font, x + w, y + 40, w / 2, h, Component.literal("Text Format Autofill"));
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

        this.addRenderableWidget(new FaButton(x + w / 2, formatField.getY() + 80, 80, 20,
            Component.literal("Save").withStyle(style -> style.withColor(0xFFE2CAE9)),
            btn -> {
            save();
            onClose();
        }));

        this.addRenderableWidget(new FaButton(x + w / 2 - 80, formatField.getY() + 80, 80, 20,
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
            overrideField.setFocused(true);

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
        var client = Minecraft.getInstance();        
        var popup = new VCPopup(
            Component.literal("Empty field detected! Would you like to restore it?"),
            "No", this::onClose,
            "Yes", () -> keyField.setValue(filterKey),
            1.0f
            );
        client.setScreen(new Overlay(client.screen, popup));
	}    

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        renderGuideText(context);

        if (searchMenu != null) {
            boolean fieldActive = formatField.isFocused();
            if (fieldActive) {
                menuInteractionActive = true;
            }
            searchMenu.setVisible(fieldActive || menuInteractionActive);
            if (searchMenu.isVisible()) {
                searchMenu.render(context, parent, mouseX, mouseY);
            }
        }

        int centerX = this.width / 2;
        int centerY = this.height / 2 - 10;
        int w = 300; 
        int x = centerX - w / 2;
        int y = centerY + 40;    

        context.text(this.font, "Filtered Message (Clean String)", x, y, 0xFF808080, false);
        context.text(this.font, "Override (Formatted)", x, y + 40, 0xFF808080, false);
        context.text(this.font, "Text Format (Autofill)", x + w, y + 40, 0xFF808080, false);

        checkTooltip(context, mouseX, mouseY);
    }

    private void checkTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        if (overrideField != null && overrideField.isMouseOver(mouseX, mouseY)) {
            
            String previewText = overrideField.getValue().trim();
            if (!previewText.isEmpty()) {

                try {
                    Component formattedPreview = Enhancer.parseFormattedText(previewText);
                    int tooltipWidth = Math.min(400, this.font.width(formattedPreview) + 20);
                    int tooltipHeight = overrideField.getHeight();
                    int tooltipX = overrideField.getX();
                    int tooltipY = overrideField.getY() + tooltipHeight + 5;
                    
                    if (tooltipX + tooltipWidth > this.width) {
                        tooltipX = mouseX - tooltipWidth - 10;
                    }
                    if (tooltipY < 0) {
                        tooltipY = mouseY + 20;
                    }
                    
                    context.fill(tooltipX, tooltipY, 
                               tooltipX + tooltipWidth + 6, tooltipY + tooltipHeight + 4, 
                               0xFF171717);
                    
                    context.text(this.font, formattedPreview, 
                                   tooltipX + 3, tooltipY + tooltipHeight / 3, 0xFFFFFFFF, true);
                                   
                } catch (Exception e) {
                    System.err.println("[FishyAddons] Error rendering tooltip: " + e.getMessage());
                    e.printStackTrace();
                    renderFallback(mouseX, mouseY, context);
                }
            }
        }
    }

    private void renderFallback(int mouseX, int mouseY, GuiGraphicsExtractor context) {
        String errorText = "Error rendering preview";
        int tooltipWidth = Math.min(400, this.font.width(errorText) + 20);
        int tooltipHeight = 20;
        int tooltipX = mouseX + 10;
        int tooltipY = mouseY - tooltipHeight - 10;
        if (tooltipX + tooltipWidth > this.width) {
            tooltipX = mouseX - tooltipWidth - 10;
        }
        if (tooltipY < 0) {
            tooltipY = mouseY + 20;
        }
                    
        context.fill(tooltipX - 5, tooltipY - 5, 
                    tooltipX + tooltipWidth + 5, tooltipY + tooltipHeight + 5, 
                    0xFF171717);
        context.text(this.font, Component.literal(errorText), 
                    tooltipX + 5, tooltipY + 10, 0xFFFF8080, true);
    }

    public void renderGuideText(GuiGraphicsExtractor context) {
        int x = this.width / 2 - 150;
        int y = this.height / 2 - 175;
        int lineHeight = 15;

        Component title = VCText.header("FishyAddons Chat Filters and Overrides", Style.EMPTY.withBold(true));

        context.text(this.font, title, x, y, 0xFFFFFFFF);            

        y += lineHeight * 2;
                
        String[] instructions = {
            " The First field is required to successfully create a rule!",
            " Leaving the override field empty results in the message being removed.",
            "",
            "- Text Formats -",
            " • §7Search for formats and click to insert at caret position.",
            "   §7You can also type them manually.",
            " • §7For all codes, custom or legacy, '&' and '§' can be used interchangeably.",
            " • §7All legacy formatting is available but INCOMPATIBLE with RGB/Gradients.",
            "- Custom Formats -",
            " • §7&[hexcode>hexcode...] §8for gradients",
            " • §7&{hexcode} §8for solid RGB colors",
        };

        for (String instruction : instructions) {
            if (instruction.isEmpty()) {
                y += lineHeight / 2;
                continue;
            }

            var format = ChatFormatting.GRAY;
            if (instruction.startsWith(" •") || instruction.matches("\\d+\\..*")) {
                format = ChatFormatting.AQUA;
            } else if (instruction.contains("-")) {
                format = ChatFormatting.DARK_AQUA;
            } else if (instruction.startsWith(" The")) {
                format = ChatFormatting.DARK_GRAY;
            }
     
            Component text = Component.literal(instruction).withStyle(format);
            context.text(this.font, text, x, y, 0xFFFFFFFF);
            y += lineHeight;
        }              
    }   

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (searchMenu != null && searchMenu.isVisible() && searchMenu.mouseClicked(click)) {
            return true;
        }

        double mouseX = click.x();
        double mouseY = click.y();

        if (formatField.isFocused() && (!formatField.isMouseOver(mouseX, mouseY) || click.button() != 0)) {
            formatField.setFocused(false);
        }
        if (overrideField.isFocused() && (!overrideField.isMouseOver(mouseX, mouseY) || click.button() != 0)) {
            overrideField.setFocused(false);
        }

        if (searchMenu != null && searchMenu.isVisible()) {
            boolean clickedOnField = formatField.isMouseOver(mouseX, mouseY) || 
                                   overrideField.isMouseOver(mouseX, mouseY) || 
                                   keyField.isMouseOver(mouseX, mouseY);
            boolean clickedOnMenu = searchMenu.isMouseOver((int)mouseX, (int)mouseY);
            
            if (!clickedOnField && !clickedOnMenu) {
                menuInteractionActive = false;
            }
        }
        
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (input.key() == 256) {
            menuInteractionActive = false;
        }
        if (searchMenu != null && formatField.isFocused() && searchMenu.keyPressed(input)) {
            return true;
        }
        if (overrideField.isFocused()) {
            return overrideField.keyPressed(input);
        }        
        if (formatField.isFocused() && formatField.keyPressed(input)) {
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (searchMenu != null && searchMenu.isVisible()) {
            return searchMenu.mouseScrolled(verticalAmount);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override 
    public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
        if (searchMenu != null && searchMenu.isVisible() && searchMenu.mouseDragged(click)) {
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
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
        this.minecraft.setScreen(parent);
    }    
}
