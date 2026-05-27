package me.valkeea.fishyaddons.vconfig.ui.widget;

import me.valkeea.fishyaddons.tool.FishyMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class FaTextField extends EditBox {
    private static final String THEME = FishyMode.themeName();    
    private static final Identifier BG_TEXTURE = Identifier.fromNamespaceAndPath("fishyaddons", "textures/gui/default/textbg.png");
    private static final Identifier BG_TEXTURE_FOCUS = Identifier.fromNamespaceAndPath("fishyaddons", "textures/gui/" + THEME + "/textbg_highlighted.png");

    public FaTextField(Font textRenderer, int x, int y, int width, int height, Component message) {
        super(textRenderer, x, y, width, height, message);
        this.setMaxLength(54);
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        var texture = this.isFocused() ? BG_TEXTURE_FOCUS : BG_TEXTURE;

        context.blit(
            RenderPipelines.GUI_TEXTURED,
            texture,
            this.getX(), this.getY(),
            0.0F, 0.0F,
            this.width, this.height,
            this.width, this.height
        );

        boolean oldDrawsBackground = this.isBordered();
        this.setBordered(false);

        int originalY = this.getY();
        int adjustedY = this.getY() + (this.height - 8) / 2;
        int originalX = this.getX();
        int adjustedX = this.getX() + 4;

        this.setY(adjustedY);        
        this.setX(adjustedX);      
        super.extractWidgetRenderState(context, mouseX, mouseY, delta);
        this.setY(originalY);
        this.setX(originalX);
        this.setBordered(oldDrawsBackground);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {

        int modifiers = input.modifiers();
        if ((modifiers & 2) != 0 && input.key() == 86) {
            String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
            return writeText(clipboard);
        }
        return super.keyPressed(input);
    }

    private boolean writeText(String text) {
        String currentText = this.getValue();
        int cursorPos = this.getCursorPosition();
        String newText = currentText.substring(
                        0, cursorPos) + text + currentText.substring(cursorPos);
        
        if (newText.length() <= 54) {
            this.setValue(newText);
            this.moveCursorTo(cursorPos + text.length(), false);
            return true;
        }
        return false;
    }    
    
    @Override
    public boolean charTyped(CharacterEvent input) {
        if (this.isFocused() && this.canConsumeInput()) {
            return super.charTyped(input);
        }
        return false;
    }
}
