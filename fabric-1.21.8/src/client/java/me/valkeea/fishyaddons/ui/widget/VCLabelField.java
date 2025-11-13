package me.valkeea.fishyaddons.ui.widget;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class VCLabelField extends ClickableWidget {
    private static final Identifier BG_TEXTURE = Identifier.of("fishyaddons", "textures/gui/default/textbg.png");
    private String text;
    private final TextRenderer textRenderer;
    private float uiScale;
    private boolean exists = true;
    private boolean drawsBg = true;

    public VCLabelField(TextRenderer tr, int x, int y, int width, int height, MutableText initialText) {
        super(x, y, width, height, initialText);
        this.textRenderer = tr;
        this.text = initialText.getString();
    }

    public void setText(String text) {
        this.text = text;
        this.setMessage(Text.literal(text));
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
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!exists) return;
        Identifier tex = BG_TEXTURE;

        if (drawsBg) {
            context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                tex,
                this.getX(), this.getY(),
                0.0F, 0.0F,
                this.width, this.height,
                this.width, this.height
            );
        }

        float textWidth = this.textRenderer.getWidth(text) * uiScale;
        if (textWidth > this.width - 8) {
            text = this.textRenderer.trimToWidth(text, (int) ((this.width - 8) / uiScale));
        }
        int textX = this.getX() + 4;
        int textY = this.getY() + (this.height - 8) / 2;

        context.getMatrices().pushMatrix();
        context.getMatrices().scale(uiScale, uiScale);
        
        float scaledX = textX / uiScale;
        float scaledY = textY / uiScale;
        
        context.drawText(this.textRenderer, text, (int)scaledX, (int)scaledY, 0xFFE0E0E0, false);
        context.getMatrices().popMatrix();
    }

    @Override
    protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
        // Access
    }
}
