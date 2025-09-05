package me.valkeea.fishyaddons.gui;

import me.valkeea.fishyaddons.tool.FishyMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class FaButton extends ButtonWidget {
    private static final String FA = "fishyaddons";
    private static final Identifier BUTTON_TEXTURE = Identifier.of(FA, "textures/gui/default/button.png");
    private static final Identifier BUTTON_DISABLED = Identifier.of(FA, "textures/gui/defaul/button_disabled.png");
    private float uiScale = 1.0f;

    public FaButton(int x, int y, int width, int height, Text message, PressAction onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
    }

    public void setUIScale(float scale) {
        this.uiScale = scale;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        Identifier texture;
        if (!this.active) {
            texture = BUTTON_DISABLED;
        } else if (this.isHovered()) {
            String mode = FishyMode.getTheme();
            texture = Identifier.of(FA, "textures/gui/" + mode + "/button_highlighted.png");
        } else {
            texture = BUTTON_TEXTURE;
        }

        context.drawTexture(
            RenderLayer::getGuiTextured,
            texture,
            this.getX(), this.getY(),
            0.0F, 0.0F,
            this.width, this.height,
            this.width, this.height
        );


        int color = this.active ? 0xFFFFFF : 0xA0A0A0;
        if (uiScale != 1.0f) {
            context.getMatrices().push();
            context.getMatrices().scale(uiScale, uiScale, 1.0f);
            context.drawCenteredTextWithShadow(
                MinecraftClient.getInstance().textRenderer,
                this.getMessage(),
                (int) ((this.getX() + ((double)this.width / 2)) / uiScale),
                (int) ((this.getY() + ((double)this.height / 2) - 3) / uiScale),
                color
            );
            context.getMatrices().pop();
        } else {
            context.drawCenteredTextWithShadow(
                MinecraftClient.getInstance().textRenderer,
                this.getMessage(),
                this.getX() + this.width / 2,
                this.getY() + (this.height - 8) / 2,
                color
            );
        }
    }
}