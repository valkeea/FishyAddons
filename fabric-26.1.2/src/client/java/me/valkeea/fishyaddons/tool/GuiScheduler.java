package me.valkeea.fishyaddons.tool;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screens.Screen;

public final class GuiScheduler {
    private static Screen nextScreen = null;

    private GuiScheduler() {}

    public static void scheduleGui(Screen screen) {
        nextScreen = screen;
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (nextScreen != null) {

                var current = mc.screen;
                if (current != null && current.equals(nextScreen)) {
                    current.onClose();
                }
                
                mc.setScreen(nextScreen);
                nextScreen = null;
            }
        });
    }
}
