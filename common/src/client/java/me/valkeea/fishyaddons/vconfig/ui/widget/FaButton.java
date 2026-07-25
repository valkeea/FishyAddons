package me.valkeea.fishyaddons.vconfig.ui.widget;

import me.valkeea.fishyaddons.tool.FishyMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public class FaButton extends Button {
    private static final String FA = "fishyaddons";
    private static final Identifier BUTTON_TEXTURE = Identifier.fromNamespaceAndPath(FA, "textures/gui/default/button.png");
    private static final Identifier BUTTON_DISABLED = Identifier.fromNamespaceAndPath(FA, "textures/gui/defaul/button_disabled.png");
    private float uiScale = 1.0f;

    public FaButton(int x, int y, int width, int height, net.minecraft.network.chat.Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    public void setUIScale(float scale) {
        this.uiScale = scale;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        Identifier texture;
        if (!this.active) {
            texture = BUTTON_DISABLED;
        } else if (this.isHovered()) {
            String mode = FishyMode.themeName();
            texture = Identifier.fromNamespaceAndPath(FA, "textures/gui/" + mode + "/button_highlighted.png");
        } else {
            texture = BUTTON_TEXTURE;
        }

        context.blit(
            RenderPipelines.GUI_TEXTURED,
            texture,
            this.getX(), this.getY(),
            0.0F, 0.0F,
            this.width, this.height,
            this.width, this.height
        );


        int color = this.active ? 0xFFFFFFFF : 0xFFA0A0A0;
        if (uiScale != 1.0f) {
            context.pose().pushMatrix();
            context.pose().scale(uiScale, uiScale);
            context.centeredText(
                Minecraft.getInstance().font,
                this.getMessage(),
                (int) ((this.getX() + ((double)this.width / 2)) / uiScale),
                (int) ((this.getY() + ((double)this.height / 2) - 3) / uiScale) + 1,
                color
            );
            context.pose().popMatrix();
        } else {
            context.centeredText(
                Minecraft.getInstance().font,
                this.getMessage(),
                this.getX() + this.width / 2,
                this.getY() + (this.height - 8) / 2 + 1,
                color
            );
        }
    }
}
