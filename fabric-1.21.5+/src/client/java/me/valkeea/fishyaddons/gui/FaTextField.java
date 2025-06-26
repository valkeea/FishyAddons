package me.valkeea.fishyaddons.gui;

import me.valkeea.fishyaddons.tool.FishyMode;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class FaTextField extends TextFieldWidget {
    private static final Identifier BG_TEXTURE = Identifier.of("fishyaddons", "textures/gui/default/textbg.png");
    private static final String theme = FishyMode.getTheme();
    private static final Identifier BG_TEXTURE_FOCUS = Identifier.of("fishyaddons", "textures/gui/" + theme + "/textbg_highlighted.png");

    public FaTextField(TextRenderer textRenderer, int x, int y, int width, int height, Text message) {
        super(textRenderer, x, y, width, height, message);
        this.setMaxLength(54);
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        Identifier texture = this.isFocused() ? BG_TEXTURE_FOCUS : BG_TEXTURE;
        context.drawTexture(
            RenderLayer::getGuiTextured,
            texture,
            this.getX(), this.getY(),
            0.0F, 0.0F,
            this.width, this.height,
            this.width, this.height
        );

        boolean oldDrawsBackground = this.drawsBackground();
        this.setDrawsBackground(false);

        // Offset the text to match vanilla centering
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

}