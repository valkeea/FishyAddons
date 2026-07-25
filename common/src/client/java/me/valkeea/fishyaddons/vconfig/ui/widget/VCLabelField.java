package me.valkeea.fishyaddons.vconfig.ui.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

public class VCLabelField extends AbstractWidget {
    private static final Identifier BG_TEXTURE = Identifier.fromNamespaceAndPath("fishyaddons", "textures/gui/default/textbg.png");
    private final Font textRenderer;    
    private String text;
    private float uiScale;
    private boolean exists = true;
    private boolean drawsBg = true;

    public VCLabelField(Font tr, int x, int y, int width, int height, MutableComponent initialText) {
        super(x, y, width, height, initialText);
        this.textRenderer = tr;
        this.text = initialText.getString();
    }

    public void setText(String text) {
        this.text = text;
        this.setMessage(Component.literal(text));
    }

    public String getText() {
        return text;
    }

    public void setUIScale(float scale) {
        this.uiScale = scale;
    }

    public void setVisible(boolean exists) {
        this.exists = exists;
    }

    public void setBg(boolean shouldDraw) {
        this.drawsBg = shouldDraw;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (!exists) return;

        if (drawsBg) {
            context.blit(
                RenderPipelines.GUI_TEXTURED,
                BG_TEXTURE,
                this.getX(), this.getY(),
                0.0F, 0.0F,
                this.width, this.height,
                this.width, this.height
            );
        }

        float textWidth = this.textRenderer.width(text) * uiScale;
        if (textWidth > this.width - 8) {
            text = this.textRenderer.plainSubstrByWidth(text, (int) ((this.width - 8) / uiScale));
        }
        int textX = this.getX() + 4;
        int textY = this.getY() + (this.height - 8) / 2;

        context.pose().pushMatrix();
        context.pose().scale(uiScale, uiScale);

        float scaledX = textX / uiScale;
        float scaledY = textY / uiScale;
        
        context.text(this.textRenderer, text, (int)scaledX, (int)scaledY, 0xFFE0E0E0, false);
        context.pose().popMatrix();
    }

    @Override
    protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput builder) {
        // Access
    }
}
