package me.valkeea.fishyaddons.ui.widget;

import me.valkeea.fishyaddons.tool.FishyMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class VCTextField extends TextFieldWidget {
    private static final Identifier BG_TEXTURE = Identifier.of("fishyaddons", "textures/gui/default/textbg.png");

    private boolean useCustomCharacterHandling = false;
    private boolean isDragging = false;    
    private boolean drawsBg = true;
    private boolean allowSection = true;
    private float uiScale = 1.0f;
    private int maxLength = 256;
    private int selectionStart = 0;

    public VCTextField(TextRenderer textRenderer, int x, int y, int width, int height, Text message) {
        super(textRenderer, x, y, width, height, message);
        this.setMaxLength(maxLength);
    }
    
    public VCTextField(TextRenderer textRenderer, int x, int y, int width, int height, Text message, boolean useCustomCharacterHandling) {
        super(textRenderer, x, y, width, height, message);
        this.setMaxLength(maxLength);
        this.useCustomCharacterHandling = useCustomCharacterHandling;
    }
    
    
    public void setUseCustomCharacterHandling(boolean useCustomCharacterHandling) {
        this.useCustomCharacterHandling = useCustomCharacterHandling;
    }
    
    public void setUIScale(float scale) {
        this.uiScale = scale;
    }

    public void setDrawsCustomBg(boolean shouldDraw) {
        this.drawsBg = shouldDraw;
    }

    public void setSectionSymbol(boolean allow) {
        this.allowSection = allow;
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        Identifier focusedBg = Identifier.of("fishyaddons", "textures/gui/" + FishyMode.getTheme() + "/textbg_highlighted.png");        
        Identifier texture = this.isFocused() ? focusedBg : BG_TEXTURE;
        if (drawsBg) {
            context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                texture,
                this.getX(), this.getY(),
                0.0F, 0.0F,
                this.width, this.height,
                this.width, this.height
            );
        }

        boolean oldDrawsBackground = this.drawsBackground();
        this.setDrawsBackground(false);

        int originalY = this.getY();
        int originalX = this.getX();
        
        int scaledVerticalOffset = (this.height - 8) / 2;
        int scaledHorizontalOffset = Math.max(2, (int)(4 * uiScale));
        
        int adjustedY = this.getY() + scaledVerticalOffset;
        int adjustedX = this.getX() + scaledHorizontalOffset;

        this.setY(adjustedY);
        this.setX(adjustedX);
        
        if (uiScale != 1.0f) {
            this.setX(adjustedX + MinecraftClient.getInstance().textRenderer.fontHeight / 4);           
            renderScaledText(context, mouseX, mouseY, delta);
        } else {
            super.renderWidget(context, mouseX, mouseY, delta);
        }
        
        this.setDrawsBackground(oldDrawsBackground);        
        this.setY(originalY);
        this.setX(originalX);
    }

    private void renderScaledText(DrawContext context, int mouseX, int mouseY, float delta) {
        int scissorX = this.getX() - 1;
        int scissorY = this.getY() - 1;
        int scissorWidth = this.width - (int)(4 * uiScale) + 2;
        int scissorHeight = this.height + 2;

        context.createNewRootLayer();        
        context.enableScissor(scissorX, scissorY, scissorX + scissorWidth, scissorY + scissorHeight);
        context.getMatrices().pushMatrix();
        context.getMatrices().scale(uiScale, uiScale);
        
        float scaledX = this.getX() / uiScale;
        float scaledY = this.getY() / uiScale;
        float scaledMouseX = mouseX / uiScale;
        float scaledMouseY = mouseY / uiScale;
        
        int origX = this.getX();
        int origY = this.getY();
        this.setX((int)scaledX);
        this.setY((int)scaledY);
        
        int origWidth = this.width;
        int origHeight = this.height;
        this.width = (int)(this.width / uiScale);
        this.height = (int)(this.height / uiScale);
        
        super.renderWidget(context, (int)scaledMouseX, (int)scaledMouseY, delta);
        
        this.setX(origX);
        this.setY(origY);
        this.width = origWidth;
        this.height = origHeight;
        
        context.getMatrices().popMatrix();
        context.disableScissor();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        isDragging = true;
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean inside = mouseX >= this.getX() && mouseX < this.getX() + this.width &&
                         mouseY >= this.getY() && mouseY < this.getY() + this.height;
        
        if (inside) {
            if (!this.isFocused()) {
                this.setFocused(true);
            }
            if (uiScale != 1.0f) {
                double adjustedMouseY = mouseY;
                double horizontalOffset = Math.max(2, (int)(4 * uiScale)) +
                                        MinecraftClient.getInstance().textRenderer.fontHeight / 3.0;
                
                double adjustedMouseX = (mouseX - this.getX() - horizontalOffset) / uiScale + this.getX() - horizontalOffset;
                int originalWidth = this.width;

                this.width = (int)(this.width / uiScale) + (int)horizontalOffset;
                boolean result = super.mouseClicked(adjustedMouseX, adjustedMouseY, button);
                this.width = originalWidth;
                selectionStart = (int)adjustedMouseX;
                return result;
            } else {
                selectionStart = (int)mouseX;
                return super.mouseClicked(mouseX, mouseY, button);
            }
        } else if (this.isFocused()) {
            this.setFocused(false);
        }
        return inside;
    }


    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isDragging) {
            int selectionEnd;
            if (uiScale != 1.0f) {
                double horizontalOffset = Math.max(2, (int)(4 * uiScale)) +
                                        MinecraftClient.getInstance().textRenderer.fontHeight / 3.0;
                selectionEnd = (int)((mouseX - this.getX() - horizontalOffset) / uiScale + this.getX() - horizontalOffset);
            } else {
                selectionEnd = (int)mouseX;
            }
            isDragging = false;
            if (selectionStart != selectionEnd) {
                this.setSelectionStart(this.getCharacterIndex(selectionStart));
                this.setSelectionEnd(this.getCharacterIndex(selectionEnd));
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((modifiers & 2) != 0 && keyCode == 86) {
            String clipboard = MinecraftClient.getInstance().keyboard.getClipboard();
            return writeText(clipboard);
        }

        if (this.isFocused() && isStandardTextInputKey(keyCode)) {
            boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
            if (handled) {
                return true;
            }
        }
        
        boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
        
        if (useCustomCharacterHandling && !handled && this.isFocused() && isPrintableCharacter(keyCode)) {
            char character = (char) keyCode;
            
            if ((modifiers & 1) != 0) {
                character = getShiftedCharacter(character);
            } else {
                character = Character.toLowerCase(character);
            }
            
            return this.charTyped(character, modifiers);
        }
        
        return handled;
    }
    
    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.isFocused() && this.isActive()) {
            if (chr == '§' && allowSection) {
                return writeText(String.valueOf(chr));
            }
            return super.charTyped(chr, modifiers);
        }
        return false;
    }

    private boolean writeText(String text) {
        String currentText = this.getText();
        int cursorPos = this.getCursor();
        String newText = currentText.substring(
                        0, cursorPos) + text + currentText.substring(cursorPos);
        
        if (newText.length() <= this.maxLength) {
            this.setText(newText);
            this.setCursor(cursorPos + text.length(), false);
            return true;
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
            case '4': return '¤';
            case '5': return '%';
            case '6': return '&';
            case '7': return '/';
            case '8': return '(';
            case '9': return ')';
            case '0': return '=';
            case '-': return '_';
            case '+': return '?';
            case '´': return '`';
            case '§': return '½';
            case ',': return ';';
            case '.': return ':';
            case '¨': return '^';
            default: return c;
        }
    }

    /**
     * Converts a pixel X position to a character index in the text.
     */
    private int getCharacterIndex(int pixelX) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        String text = this.getText();
        int x = this.getX();
        int relativeX = pixelX - x;
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            int charWidth = textRenderer.getWidth(String.valueOf(text.charAt(i)));
            if (uiScale != 1.0f) {
                charWidth = (int)(charWidth * uiScale);
            }
            width += charWidth;
            if (relativeX < width) {
                return i;
            }
        }
        return text.length();
    }
}
