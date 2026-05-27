package me.valkeea.fishyaddons.vconfig.ui.widget;

import me.valkeea.fishyaddons.tool.FishyMode;
import me.valkeea.fishyaddons.util.SpriteUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class VCTextField extends EditBox {
    private static final Identifier BG_TEXTURE = SpriteUtil.createModSprite("gui/default/textbg");

    private boolean interceptInventory = false;
    private boolean isDragging = false;    
    private boolean drawsBg = true;
    private boolean allowSection = true;
    private float uiScale = 1.0f;
    private int maxLength = 256;
    private int selectionStart = 0;

    public VCTextField(Font textRenderer, int x, int y, int width, int height, Component message) {
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

    public void interceptInventory(boolean intercept) {
        this.interceptInventory = intercept;
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        
        var focusedBg = SpriteUtil.createModSprite("gui/" + FishyMode.themeName() + "/textbg_highlighted");
        var texture = this.isFocused() ? focusedBg : BG_TEXTURE;

        if (drawsBg) {
            context.blit(
                RenderPipelines.GUI_TEXTURED,
                texture,
                this.getX(), this.getY(),
                0.0F, 0.0F,
                this.width, this.height,
                this.width, this.height
            );
        }

        boolean oldDrawsBackground = this.isBordered();
        this.setBordered(false);

        int originalY = this.getY();
        int originalX = this.getX();
        
        int scaledVerticalOffset = (this.height - 8) / 2;
        int scaledHorizontalOffset = Math.max(2, (int)(4 * uiScale));
        
        int adjustedY = this.getY() + scaledVerticalOffset;
        int adjustedX = this.getX() + scaledHorizontalOffset;

        this.setY(adjustedY);
        this.setX(adjustedX);
        
        this.setX(adjustedX + Minecraft.getInstance().font.lineHeight / 4);           
        renderScaledText(context, mouseX, mouseY, delta);
        
        this.setBordered(oldDrawsBackground);        
        this.setY(originalY);
        this.setX(originalX);
    }
    
    private void renderScaledText(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        int scissorX = this.getX() - 1;
        int scissorY = this.getY() - 1;
        int scissorWidth = this.width - (int)(4 * uiScale) + 2;
        int scissorHeight = this.height + 2;
        
        context.enableScissor(scissorX, scissorY, scissorX + scissorWidth, scissorY + scissorHeight);
        context.pose().pushMatrix();
        context.pose().scale(uiScale, uiScale);
        
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
        
        super.extractWidgetRenderState(context, (int)scaledMouseX, (int)scaledMouseY, delta);
        
        this.setX(origX);
        this.setY(origY);
        this.width = origWidth;
        this.height = origHeight;
        
        context.pose().popMatrix();
        context.disableScissor();
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
        isDragging = true;
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {

        double mouseX = click.x();
        double mouseY = click.y();
        boolean inside = this.isMouseOver(mouseX, mouseY);
        
        if (inside) {
            if (!this.isFocused()) {
                this.setFocused(true);
            }
            double adjustedMouseY = mouseY;
            double horizontalOffset = Math.max(2, (int)(4 * uiScale)) +
                                    Minecraft.getInstance().font.lineHeight / 3.0;
            
            double adjustedMouseX = (mouseX - this.getX() - horizontalOffset) / uiScale + this.getX() - horizontalOffset;
            int originalWidth = this.width;

            var adjustedClick = new MouseButtonEvent(adjustedMouseX, adjustedMouseY, click.buttonInfo());

            this.width = (int)(this.width / uiScale) + (int)horizontalOffset;
            boolean result = super.mouseClicked(adjustedClick, doubled);
            this.width = originalWidth;
            selectionStart = (int)adjustedMouseX;
            return result;

        } else if (this.isFocused()) {
            this.setFocused(false);
        }
        return inside;
    }


    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (isDragging) {
            int selectionEnd;
            if (uiScale != 1.0f) {
                double horizontalOffset = Math.max(2, (int)(4 * uiScale)) +
                                        Minecraft.getInstance().font.lineHeight / 3.0;
                selectionEnd = (int)((click.x() - this.getX() - horizontalOffset) / uiScale + this.getX() - horizontalOffset);
            } else {
                selectionEnd = (int)click.x();
            }
            isDragging = false;
            if (selectionStart != selectionEnd) {
                this.setCursorPosition(this.getCharacterIndex(selectionStart));
                this.setHighlightPos(this.getCharacterIndex(selectionEnd));
            }
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        int modifiers = input.modifiers();
        if ((modifiers & 2) != 0 && input.key() == 86) {
            String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
            return writeText(clipboard);
        }
        if (this.isFocused() && interceptInventory &&
            Minecraft.getInstance().options.keyInventory.matches(input)) {
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (this.isFocused()) {
            if (input.codepointAsString().equals("§") && allowSection) {
                return writeText(input.codepointAsString());
            }
            return super.charTyped(input);
        }
        return false;
    }

    private boolean writeText(String text) {
        String currentText = this.getValue();
        int cursorPos = this.getCursorPosition();
        String newText = currentText.substring(
                        0, cursorPos) + text + currentText.substring(cursorPos);
        
        if (newText.length() <= this.maxLength) {
            this.setValue(newText);
            this.moveCursorTo(cursorPos + text.length(), false);
            return true;
        }
        return false;
    }

    /**
     * Converts a pixel X position to a character index in the text.
     */
    private int getCharacterIndex(int pixelX) {
        var textRenderer = Minecraft.getInstance().font;
        String text = this.getValue();
        int x = this.getX();
        int relativeX = pixelX - x;
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            int charWidth = textRenderer.width(String.valueOf(text.charAt(i)));
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
