package me.valkeea.fishyaddons.ui.widget;

import me.valkeea.fishyaddons.tool.FishyMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.util.Identifier;

public class FaButton extends ButtonWidget {
    private static final String FA = "fishyaddons";
    private static final Identifier BUTTON_TEXTURE = Identifier.of(FA, "textures/gui/default/button.png");
    private static final Identifier BUTTON_DISABLED = Identifier.of(FA, "textures/gui/defaul/button_disabled.png");
    private float uiScale = 1.0f;

    public FaButton(int x, int y, int width, int height, net.minecraft.text.Text message, PressAction onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
    }

    public void setUIScale(float scale) {
        this.uiScale = scale;
    }

    @Override
    protected void drawIcon(DrawContext context, int mouseX, int mouseY, float delta) {
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
            RenderPipelines.GUI_TEXTURED,
            texture,
            this.getX(), this.getY(),
            0.0F, 0.0F,
            this.width, this.height,
            this.width, this.height
        );


        int color = this.active ? 0xFFFFFFFF : 0xFFA0A0A0;
        if (uiScale != 1.0f) {
            context.getMatrices().pushMatrix();
            context.getMatrices().scale(uiScale, uiScale);
            context.drawCenteredTextWithShadow(
                MinecraftClient.getInstance().textRenderer,
                this.getMessage(),
                (int) ((this.getX() + ((double)this.width / 2)) / uiScale),
                (int) ((this.getY() + ((double)this.height / 2) - 3) / uiScale) + 1,
                color
            );
            context.getMatrices().popMatrix();
        } else {
            context.drawCenteredTextWithShadow(
                MinecraftClient.getInstance().textRenderer,
                this.getMessage(),
                this.getX() + this.width / 2,
                this.getY() + (this.height - 8) / 2 + 1,
                color
            );
        }
    }
}
