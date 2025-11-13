package me.valkeea.fishyaddons.tool;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screen.Screen;

public final class GuiScheduler {
    private static Screen nextScreen = null;

    private GuiScheduler() {}

    public static void scheduleGui(Screen screen) {
        nextScreen = screen;
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (nextScreen != null) {
                client.setScreen(nextScreen);
                nextScreen = null;
            }
        });
    }
}
