package me.valkeea.fishyaddons.ui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

// Overlay for screens not initialized by the mod
public class VCOverlay extends Screen {
    private final Screen parent;
    private final VCPopup popup;

    public VCOverlay(Screen parent, VCPopup popup) {
        super(Text.literal("Popup"));
        this.parent = parent;
        this.popup = popup;
    }

    @Override
    protected void init() {
        popup.init(this.client.textRenderer, this.width, this.height);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        parent.render(context, mouseX, mouseY, delta);
        context.getMatrices().pushMatrix();
        context.getMatrices().identity();
        popup.render(context, this.client.textRenderer, mouseX, mouseY, delta);
        context.getMatrices().popMatrix();
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        return popup.mouseClicked(click);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == 256) {
            this.client.setScreen(this.parent);
            return true;
        }
        return popup.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput chr) {
        return popup.charTyped(chr);
    }
}
