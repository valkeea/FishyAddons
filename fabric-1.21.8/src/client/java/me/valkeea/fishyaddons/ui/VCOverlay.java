package me.valkeea.fishyaddons.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return popup.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.client.setScreen(this.parent);
            return true;
        }
        return popup.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return popup.charTyped(chr, modifiers);
    }
}