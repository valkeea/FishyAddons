package me.valkeea.fishyaddons.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class VCLabelField extends ClickableWidget {
    private static final Identifier BG_TEXTURE = Identifier.of("fishyaddons", "textures/gui/default/textbg.png");
    private String text;
    private final TextRenderer textRenderer;
    private float uiScale;
    private boolean exists = true;

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

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!exists) return;
        Identifier tex = BG_TEXTURE;

        context.drawTexture(
            RenderLayer::getGuiTextured,
            tex,
            this.getX(), this.getY(),
            0.0F, 0.0F,
            this.width, this.height,
            this.width, this.height
        );

        float textWidth = this.textRenderer.getWidth(text) * uiScale;
        if (textWidth > this.width - 8) {
            text = this.textRenderer.trimToWidth(text, (int) ((this.width - 8) / uiScale));
        }

        context.getMatrices().push();
        context.getMatrices().translate((this.getX() + 4), this.getY() + ((this.height - 8) / 2.0), 0);
        context.getMatrices().scale(uiScale, uiScale, 1.0f);
        context.drawText(this.textRenderer, text, 0, 0, 0xE0E0E0, false);
        context.getMatrices().pop();
    }

    @Override
    protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
        // Access
    }
}