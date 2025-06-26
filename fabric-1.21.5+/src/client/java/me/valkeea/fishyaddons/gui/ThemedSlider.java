package me.valkeea.fishyaddons.gui;

import me.valkeea.fishyaddons.tool.FishyMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

public abstract class ThemedSlider extends SliderWidget {
    private static final Identifier SLIDER_TEXTURE = Identifier.of("fishyaddons", "textures/gui/default/button_disabled.png");
    private static final Identifier SLIDER_DISABLED = Identifier.of("fishyaddons", "textures/gui/default/button_disabled.png");
    private static final Identifier SLIDER_KNOB = Identifier.of("fishyaddons", "textures/gui/default/knob.png");

    public ThemedSlider(int x, int y, int width, int height, net.minecraft.text.Text message, double value) {
        super(x, y, width, height, message, value);
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        context.drawTexture(
            RenderLayer::getGuiTextured,
            SLIDER_TEXTURE,
            this.getX(), this.getY(),
            0.0F, 0.0F,
            this.width, this.height,
            this.width, this.height
        );

        // Calculate knob position
        int knobWidth = 8;
        int knobHeight = this.height;
        int knobX = this.getX() + (int) (this.value * (this.width - knobWidth));
        int knobY = this.getY();

        Identifier knobTexture;
        if (!this.active) {
            knobTexture = SLIDER_DISABLED;
        } else if (this.isHovered() &&
                   mouseX >= knobX && mouseX <= knobX + knobWidth &&
                   mouseY >= knobY && mouseY <= knobY + knobHeight) {
            String mode = FishyMode.getTheme();
            knobTexture = Identifier.of("fishyaddons", "textures/gui/" + mode + "/knob_highlighted.png");
        } else {
            knobTexture = SLIDER_KNOB;
        }

        context.drawTexture(
            RenderLayer::getGuiTextured,
            knobTexture,
            knobX, knobY,
            0.0F, 0.0F,
            knobWidth, knobHeight,
            knobWidth, knobHeight
        );

        int color = this.active ? 0xFFFFFF : 0xA0A0A0;
        context.drawCenteredTextWithShadow(
            MinecraftClient.getInstance().textRenderer,
            this.getMessage(),
            this.getX() + this.width / 2,
            this.getY() + (this.height - 8) / 2,
            color
        );
    }
}
