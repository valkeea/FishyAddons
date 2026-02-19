package me.valkeea.fishyaddons.ui.widget;

import me.valkeea.fishyaddons.tool.FishyMode;
import me.valkeea.fishyaddons.util.SpriteUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class VCTextField extends TextFieldWidget {
    private static final Identifier BG_TEXTURE = SpriteUtil.createModSprite("gui/default/textbg");

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
        
        var focusedBg = SpriteUtil.createModSprite("gui/" + FishyMode.getTheme() + "/textbg_highlighted");
        var texture = this.isFocused() ? focusedBg : BG_TEXTURE;

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
        
        this.setX(adjustedX + MinecraftClient.getInstance().textRenderer.fontHeight / 4);           
        renderScaledText(context, mouseX, mouseY, delta);
        
        this.setDrawsBackground(oldDrawsBackground);        
        this.setY(originalY);
        this.setX(originalX);
    }
    
    private void renderScaledText(DrawContext context, int mouseX, int mouseY, float delta) {
        int scissorX = this.getX() - 1;
        int scissorY = this.getY() - 1;
        int scissorWidth = this.width - (int)(4 * uiScale) + 2;
        int scissorHeight = this.height + 2;
        
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
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        isDragging = true;
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {

        double mouseX = click.x();
        double mouseY = click.y();
        boolean inside = this.isMouseOver(mouseX, mouseY);
        
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

                var adjustedClick = new Click(adjustedMouseX, adjustedMouseY, click.buttonInfo());

                this.width = (int)(this.width / uiScale) + (int)horizontalOffset;
                boolean result = super.mouseClicked(adjustedClick, doubled);
                this.width = originalWidth;
                selectionStart = (int)adjustedMouseX;
                return result;

            } else {
                selectionStart = (int)mouseX;
                return super.mouseClicked(click, doubled);
            }

        } else if (this.isFocused()) {
            this.setFocused(false);
        }
        return inside;
    }


    @Override
    public boolean mouseReleased(Click click) {
        if (isDragging) {
            int selectionEnd;
            if (uiScale != 1.0f) {
                double horizontalOffset = Math.max(2, (int)(4 * uiScale)) +
                                        MinecraftClient.getInstance().textRenderer.fontHeight / 3.0;
                selectionEnd = (int)((click.x() - this.getX() - horizontalOffset) / uiScale + this.getX() - horizontalOffset);
            } else {
                selectionEnd = (int)click.x();
            }
            isDragging = false;
            if (selectionStart != selectionEnd) {
                this.setSelectionStart(this.getCharacterIndex(selectionStart));
                this.setSelectionEnd(this.getCharacterIndex(selectionEnd));
            }
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int modifiers = input.modifiers();
        if ((modifiers & 2) != 0 && input.key() == 86) {
            String clipboard = MinecraftClient.getInstance().keyboard.getClipboard();
            return writeText(clipboard);
        }
        return super.keyPressed(input);
    }
    
    @Override
    public boolean charTyped(CharInput input) {
        if (this.isFocused() && this.isActive()) {
            if (input.asString().equals("§") && allowSection) {
                return writeText(input.asString());
            }
            return super.charTyped(input);
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

    /**
     * Converts a pixel X position to a character index in the text.
     */
    private int getCharacterIndex(int pixelX) {
        var textRenderer = MinecraftClient.getInstance().textRenderer;
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
