package me.valkeea.fishyaddons.ui.element;

import me.valkeea.fishyaddons.tool.FishyMode;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class FishyPopup {
    private final Runnable onDiscard;
    private final Runnable onContinue;
    private final Component title;
    private final Component continueButtonText;
    private final Component discardButtonText;
    private Button continueButton;
    private Button discardButton;
    private int x;
    private int y;
    private int width;
    private int height;

    public FishyPopup(Component title, Component continueButtonText, Runnable onContinue, Component discardButtonText, Runnable onDiscard) {
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
        continueButton = Button.builder(continueButtonText, b -> onContinue.run())
            .bounds(x + 15, y + 70, 90, 20).build();
        discardButton = Button.builder(discardButtonText, b -> onDiscard.run())
            .bounds(x + 115, y + 70, 90, 20).build();
    }

    public void render(GuiGraphicsExtractor context, net.minecraft.client.gui.Font textRenderer, int mouseX, int mouseY, float delta) {
        int color = 0xFFE2CAE9;

        context.fill(x, y, x + width, y + height, FishyMode.getThemeColor());
        context.fill(x - 1, y - 1, x + width + 1, y, color);
        context.fill(x - 1, y + height, x + width + 1, y + height + 1, color);
        context.fill(x - 1, y, x, y + height, color);
        context.fill(x + width, y, x + width + 1, y + height, color);

        context.centeredText(
            textRenderer,
            title,
            x + width / 2, y + 15, 0xFFE2CAE9
        );
        continueButton.extractRenderState(context, mouseX, mouseY, delta);
        discardButton.extractRenderState(context, mouseX, mouseY, delta);
    }

    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        return continueButton.mouseClicked(click, doubled) ||
               discardButton.mouseClicked(click, doubled);
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
}
