package me.valkeea.fishyaddons.util;

import me.valkeea.fishyaddons.handler.GuiIcons;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.text.Text;

@SuppressWarnings("squid:S6548")
public class SbGui {
    private static final SbGui INSTANCE = new SbGui();
    private SbGui() {}

    public static SbGui getInstance() {
        return INSTANCE;
    }
    private boolean inGui = false;
    private String current;

    public void onScreen(Text title) {
        if (GuiIcons.hasConfig(title)) {
            inGui = true;
            current = title.getString();
        }
    }

    public boolean inGui() {
        return inGui;
    }

    public void setInGui(String gui) {
        this.inGui = true;
        this.current = gui;
    }

    public String current() {
        return current;
    }

    public void onScreenClose() {
        if (inGui) {
            inGui = false;
            current = null;
        }
    }

    public void onInvUpdate() {
        if (inGui) {
            var screen = MinecraftClient.getInstance().currentScreen;
            if (screen instanceof HandledScreen<?> handledScreen) {
                current = handledScreen.getTitle().getString();
            }
            else {
                current = null;
                inGui = false;
            }
        }
    }
}