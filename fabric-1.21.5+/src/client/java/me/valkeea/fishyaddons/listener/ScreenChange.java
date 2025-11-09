package me.valkeea.fishyaddons.listener;

import me.valkeea.fishyaddons.feature.skyblock.EqDetector;
import me.valkeea.fishyaddons.util.SbGui;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.text.Text;

public class ScreenChange {
    private ScreenChange() {}

    public static void onInit(HandledScreen<?> screen, Text title) {
        checkEq(screen, title);
        SbGui.getInstance().onScreen(title);
    }

    public static void onClose(Text title) {
        resetEq(title);
        SbGui.getInstance().onScreenClose();
    }

    private static void checkEq(HandledScreen<?> screen, Text title) {
        if (SbGui.isPlayerInventory() && title.getString().contains("Your Equipment and Stats")) {
            EqDetector.onScreen(screen);
        }
    }

    private static void resetEq(Text title) {
        if (SbGui.isPlayerInventory() && title.getString().contains("Your Equipment and Stats")) {
            EqDetector.onScreenClosed();
        }
    }
}
