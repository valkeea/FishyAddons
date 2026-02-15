package me.valkeea.fishyaddons.ui.widget;

import me.valkeea.fishyaddons.tool.FishyMode;
import net.minecraft.client.MinecraftClient;
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

    public FaTextField(TextRenderer textRenderer, int x, int y, int width, int height, Text message) {
        super(textRenderer, x, y, width, height, message);
        this.setMaxLength(54);
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

        int modifiers = input.modifiers();
        if ((modifiers & 2) != 0 && input.key() == 86) {
            String clipboard = MinecraftClient.getInstance().keyboard.getClipboard();
            return writeText(clipboard);
        }
        return super.keyPressed(input);
    }

    private boolean writeText(String text) {
        String currentText = this.getText();
        int cursorPos = this.getCursor();
        String newText = currentText.substring(
                        0, cursorPos) + text + currentText.substring(cursorPos);
        
        if (newText.length() <= 54) {
            this.setText(newText);
            this.setCursor(cursorPos + text.length(), false);
            return true;
        }
        return false;
    }    
    
    @Override
    public boolean charTyped(CharInput input) {
        if (this.isFocused() && this.isActive()) {
            return super.charTyped(input);
        }
        return false;
    }
}
