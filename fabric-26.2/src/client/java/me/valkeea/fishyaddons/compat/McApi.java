package me.valkeea.fishyaddons.compat;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class McApi {

    private static Minecraft mc() {
        return Minecraft.getInstance();
    }

    // -- Client --

    public static @Nullable Screen screen() {
        return mc().gui.screen();
    }
    
    public static void setScreen(@Nullable Screen screen) {
        mc().gui.setScreen(screen);
    }

    public static boolean isScreen(Class<? extends Screen> screenClass) {
        var screen = screen();
        return screen != null && screenClass.isInstance(screen);
    }

    public static boolean screenIsActive() {
        return screen() != null;
    }

    private McApi() {}
}
