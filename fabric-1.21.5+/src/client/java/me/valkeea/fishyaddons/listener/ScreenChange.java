package me.valkeea.fishyaddons.listener;

import me.valkeea.fishyaddons.handler.EqDetector;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.text.Text;

public class ScreenChange {
    private ScreenChange() {}

    public static void onInit(HandledScreen<?> screen, Text title) {
        checkEq(screen, title);
        me.valkeea.fishyaddons.util.SbGui.getInstance().onScreen(title);
    }

    public static void onClose(Text title) {
        resetEq(title);
        me.valkeea.fishyaddons.util.SbGui.getInstance().onScreenClose();
    }

    private static void checkEq(HandledScreen<?> screen, Text title) {
        if (title.getString().contains("Your Equipment and Stats")) {
            EqDetector.onScreen(screen);
        }
    }

    private static void resetEq(Text title) {
        if (title.getString().contains("Your Equipment and Stats")) {
            EqDetector.onScreenClosed();
        }
    }
}