package me.valkeea.fishyaddons.ui.screen;

import me.valkeea.fishyaddons.compat.McApi;
import me.valkeea.fishyaddons.vconfig.ui.widget.VCPopup;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

// Overlay for screens not initialized by the mod
public class Overlay extends Screen {
    private final Screen parent;
    private final VCPopup popup;

    public Overlay(Screen parent, VCPopup popup) {
        super(Component.literal("Popup"));
        this.parent = parent;
        this.popup = popup;
    }

    @Override
    protected void init() {
        popup.init(this.minecraft.font, this.width, this.height);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        parent.extractRenderState(context, mouseX, mouseY, delta);
        context.pose().pushMatrix();
        context.pose().identity();
        popup.render(context, this.minecraft.font, mouseX, mouseY, delta);
        context.pose().popMatrix();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        return popup.mouseClicked(click);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (input.key() == 256) {
            McApi.setScreen(this.parent);
            return true;
        }
        return popup.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent chr) {
        return popup.charTyped(chr);
    }
}
