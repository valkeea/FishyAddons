package me.valkeea.fishyaddons.ui.widget;

import me.valkeea.fishyaddons.tool.FishyMode;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class FaTextField extends TextFieldWidget {
    private static final String THEME = FishyMode.getTheme();    
    private static final Identifier BG_TEXTURE = Identifier.of("fishyaddons", "textures/gui/default/textbg.png");
    private static final Identifier BG_TEXTURE_FOCUS = Identifier.of("fishyaddons", "textures/gui/" + THEME + "/textbg_highlighted.png");
    
    private boolean useCustomCharacterHandling = false;

    public FaTextField(TextRenderer textRenderer, int x, int y, int width, int height, Text message) {
        super(textRenderer, x, y, width, height, message);
        this.setMaxLength(54);
    }
    
    public FaTextField(TextRenderer textRenderer, int x, int y, int width, int height, Text message, boolean useCustomCharacterHandling) {
        super(textRenderer, x, y, width, height, message);
        this.setMaxLength(54);
        this.useCustomCharacterHandling = useCustomCharacterHandling;
    }
    
    public void setUseCustomCharacterHandling(boolean useCustomCharacterHandling) {
        this.useCustomCharacterHandling = useCustomCharacterHandling;
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        var texture = this.isFocused() ? BG_TEXTURE_FOCUS : BG_TEXTURE;

        context.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            texture,
            this.getX(), this.getY(),
            0.0F, 0.0F,
            this.width, this.height,
            this.width, this.height
        );

        boolean oldDrawsBackground = this.drawsBackground();
        this.setDrawsBackground(false);

        int originalY = this.getY();
        int adjustedY = this.getY() + (this.height - 8) / 2;
        int originalX = this.getX();
        int adjustedX = this.getX() + 4;

        this.setY(adjustedY);        
        this.setX(adjustedX);      
        super.renderWidget(context, mouseX, mouseY, delta);
        this.setY(originalY);
        this.setX(originalX);
        this.setDrawsBackground(oldDrawsBackground);
    }

    @Override
    public boolean keyPressed(KeyInput input) {

        if (this.isFocused() && isStandardTextInputKey(input.key())) {
            boolean handled = super.keyPressed(input);
            if (handled) {
                return true;
            }
        }

        boolean handled = super.keyPressed(input);

        if (useCustomCharacterHandling && !handled && this.isFocused() && isPrintableCharacter(input.key())) {
            char character = (char) input.key();

            if ((input.modifiers() & 1) != 0) {
                character = getShiftedCharacter(character);
            } else {
                character = Character.toLowerCase(character);
            }

            int codePoint = Character.codePointAt(new char[]{character}, 0);
            var charInput = new CharInput(character, codePoint);
            return this.charTyped(charInput);
        }
        
        return handled;
    }
    
    @Override
    public boolean charTyped(CharInput input) {
        if (this.isFocused() && this.isActive()) {
            return super.charTyped(input);
        }
        return false;
    }
    
    private boolean isStandardTextInputKey(int keyCode) {
        return keyCode == 259 ||
               keyCode == 261 ||
               keyCode == 262 ||
               keyCode == 263 ||
               keyCode == 268 ||
               keyCode == 269 ||
               keyCode == 257 ||
               keyCode == 335 ||
               keyCode == 341 ||
               keyCode == 342 ||
               keyCode == 345;
    }
    
    private boolean isPrintableCharacter(int keyCode) {
        return keyCode >= 32 && keyCode <= 126;
    }
    
    private char getShiftedCharacter(char c) {
        if (c >= 'a' && c <= 'z') {
            return Character.toUpperCase(c);
        }
        
        switch (c) {
            case '1': return '!';
            case '2': return '@';
            case '3': return '#';
            case '4': return '$';
            case '5': return '%';
            case '6': return '^';
            case '7': return '&';
            case '8': return '*';
            case '9': return '(';
            case '0': return ')';
            case '-': return '_';
            case '=': return '+';
            case '[': return '{';
            case ']': return '}';
            case '\\': return '|';
            case ';': return ':';
            case '\'': return '"';
            case ',': return ';';
            case '.': return ':';
            case '/': return '?';
            case '`': return '~';
            case '<': return '>';
            default: return c;
        }
    }
}
