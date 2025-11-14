package me.valkeea.fishyaddons.ui.widget;

import me.valkeea.fishyaddons.tool.FishyMode;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class FishyPopup {
    private final Runnable onDiscard;
    private final Runnable onContinue;
    private final Text title;
    private final Text continueButtonText;
    private final Text discardButtonText;
    private ButtonWidget continueButton;
    private ButtonWidget discardButton;
    private int x;
    private int y;
    private int width;
    private int height;

    public FishyPopup(Text title, Text continueButtonText, Runnable onContinue, Text discardButtonText, Runnable onDiscard) {
        this.title = title;
        this.continueButtonText = continueButtonText;
        this.onContinue = onContinue;
        this.discardButtonText = discardButtonText;
        this.onDiscard = onDiscard;
    }

    public void init(int screenWidth, int screenHeight) {
        width = 220;
        height = 110;
        x = (screenWidth - width) / 2;
        y = (screenHeight - height) / 2;
        continueButton = ButtonWidget.builder(continueButtonText, b -> onContinue.run())
            .dimensions(x + 15, y + 70, 90, 20).build();
        discardButton = ButtonWidget.builder(discardButtonText, b -> onDiscard.run())
            .dimensions(x + 115, y + 70, 90, 20).build();
    }

    public void render(DrawContext context, net.minecraft.client.font.TextRenderer textRenderer, int mouseX, int mouseY, float delta) {
        int color = 0xFFE2CAE9;

        context.fill(x, y, x + width, y + height, FishyMode.getThemeColor());
        context.fill(x - 1, y - 1, x + width + 1, y, color);
        context.fill(x - 1, y + height, x + width + 1, y + height + 1, color);
        context.fill(x - 1, y, x, y + height, color);
        context.fill(x + width, y, x + width + 1, y + height, color);

        context.drawCenteredTextWithShadow(
            textRenderer,
            title,
            x + width / 2, y + 15, 0xFFE2CAE9
        );
        continueButton.render(context, mouseX, mouseY, delta);
        discardButton.render(context, mouseX, mouseY, delta);
    }

    public boolean mouseClicked(Click click, boolean doubled) {
        return continueButton.mouseClicked(click, doubled) ||
               discardButton.mouseClicked(click, doubled);
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
}
