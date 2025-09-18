package me.valkeea.fishyaddons.ui.list;

import me.valkeea.fishyaddons.config.FilterConfig.Rule;
import me.valkeea.fishyaddons.config.FilterConfig;
import me.valkeea.fishyaddons.ui.VCOverlay;
import me.valkeea.fishyaddons.ui.VCPopup;
import me.valkeea.fishyaddons.ui.widget.FaButton;
import me.valkeea.fishyaddons.ui.widget.VCTextField;
import me.valkeea.fishyaddons.ui.widget.dropdown.TextFormatMenu;
import me.valkeea.fishyaddons.util.text.Enhancer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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
        super(Text.literal("Configure Chat Overrides"));
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

        keyField = new VCTextField(this.textRenderer, x, y, w, h, Text.literal("Key"));
        keyField.setMaxLength(100);
        keyField.setText(filterKey);
        this.addDrawableChild(keyField);

        overrideField = new VCTextField(this.textRenderer, x, y + 40, w, h, Text.literal("Chat message"));
        overrideField.setMaxLength(100);
        overrideField.setText(initialData.getReplacement());
        this.addDrawableChild(overrideField);

        formatField = new VCTextField(this.textRenderer, x + w, y + 40, w / 2, h, Text.literal("Text Format Autofill"));
        this.addDrawableChild(formatField);

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

        this.addDrawableChild(new FaButton(x + w / 2, formatField.getY() + 80, 80, 20,
            Text.literal("Save").styled(style -> style.withColor(0xE2CAE9)),
            btn -> {
            save();
            this.client.setScreen(parent);
        }));

        this.addDrawableChild(new FaButton(x + w / 2 - 80, formatField.getY() + 80, 80, 20,
            Text.literal("Cancel").styled(style -> style.withColor(0xE2CAE9)),
            btn -> this.client.setScreen(parent)
        ));
    }

    private void insertFormatIntoFocusedField(String format) {
            String currentText = overrideField.getText();
            int caretPos = overrideField.getCursor();

            String newText = currentText.substring(0, caretPos) + format + currentText.substring(caretPos);
            overrideField.setText(newText);
            overrideField.setCursor(caretPos + format.length(), false);
            overrideField.setFocused(true);

        if (searchMenu != null) {
            searchMenu.setVisible(false);
            formatField.setFocused(false);
            menuInteractionActive = false;
        }
    }

    private void save() {
         String newKey = keyField.getText().trim();
        if (newKey.isEmpty()) {
            warn();
            return;
        }

        var newData = new FilterConfig.Rule(
            newKey,
            overrideField.getText().trim(),
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
        MinecraftClient cl = MinecraftClient.getInstance();        
        VCPopup popup = new VCPopup(
            Text.literal("Empty field detected! Would you like to restore it?"),
            "No", () -> cl.setScreen(parent),
            "Yes", () -> keyField.setText(filterKey),
            1.0f
            );
        cl.setScreen(new VCOverlay(cl.currentScreen, popup));
	}    

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
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

        context.drawText(this.textRenderer, "Filtered Message (Clean String)", x, y, 0xFF808080, false);
        context.drawText(this.textRenderer, "Override (Formatted)", x, y + 40, 0xFF808080, false);
        context.drawText(this.textRenderer, "Text Format (Autofill)", x + w, y + 40, 0xFF808080, false);

        checkTooltip(context, mouseX, mouseY);
    }

    private void checkTooltip(DrawContext context, int mouseX, int mouseY) {
        if (overrideField != null && overrideField.isMouseOver(mouseX, mouseY)) {
            
            String previewText = overrideField.getText().trim();
            if (!previewText.isEmpty()) {

                try {
                    Text formattedPreview = Enhancer.parseFormattedText(previewText);
                    int tooltipWidth = Math.min(400, this.textRenderer.getWidth(formattedPreview) + 20);
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
                    
                    context.drawText(this.textRenderer, formattedPreview, 
                                   tooltipX + 3, tooltipY + 2, 0xFFFFFFFF, true);
                                   
                } catch (Exception e) {
                    System.err.println("[FishyAddons] Error rendering tooltip: " + e.getMessage());
                    e.printStackTrace();
                    renderFallback(mouseX, mouseY, context);
                }
            }
        }
    }

    private void renderFallback(int mouseX, int mouseY, DrawContext context) {
        String errorText = "Error rendering preview";
        int tooltipWidth = Math.min(400, this.textRenderer.getWidth(errorText) + 20);
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
        context.drawText(this.textRenderer, Text.literal(errorText), 
                    tooltipX + 5, tooltipY + 10, 0xFF8080, true);
    }

    public void renderGuideText(DrawContext context) {
        int x = this.width / 2 - 150;
        int y = this.height / 2 - 175;
        int lineHeight = 15;

        Text title = Text.literal("FishyAddons Chat Filters and Overrides").formatted(Formatting.BOLD, Formatting.AQUA);

        context.getMatrices().push();
        context.getMatrices().translate(0,0, 300);
        context.drawTextWithShadow(this.textRenderer, title, x, y, 0xFFFFFF);            
        context.getMatrices().pop();

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

            Formatting format = Formatting.GRAY;
            if (instruction.startsWith(" •") || instruction.matches("\\d+\\..*")) {
                format = Formatting.AQUA;
            } else if (instruction.contains("-")) {
                format = Formatting.DARK_AQUA;
            } else if (instruction.startsWith(" The")) {
                format = Formatting.DARK_GRAY;
            }

            context.getMatrices().push();
            context.getMatrices().translate(0,0, 300);      
            Text text = Text.literal(instruction).formatted(format);
            context.drawTextWithShadow(this.textRenderer, text, x, y, 0xFFFFFF);
            context.getMatrices().pop();
            y += lineHeight;
        }              
    }   

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (searchMenu != null && searchMenu.isVisible() && searchMenu.mouseClicked(mouseX, mouseY)) {
            return true;
        }
        if (formatField.isFocused() && (!formatField.isMouseOver(mouseX, mouseY) || button != 0)) {
            formatField.setFocused(false);
        }
        if (overrideField.isFocused() && (!overrideField.isMouseOver(mouseX, mouseY) || button != 0)) {
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
        
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            menuInteractionActive = false;
        }
        if (searchMenu != null && formatField.isFocused() && searchMenu.keyPressed(keyCode)) {
            return true;
        }
        if (overrideField.isFocused()) {
            return overrideField.keyPressed(keyCode, scanCode, modifiers);
        }        
        if (formatField.isFocused() && formatField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }    

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (searchMenu != null && searchMenu.isVisible()) {
            return searchMenu.mouseScrolled(verticalAmount);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override 
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (searchMenu != null && searchMenu.isVisible() && searchMenu.mouseDragged(mouseY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (searchMenu != null && searchMenu.isVisible()) {
            searchMenu.mouseReleased();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}