package me.valkeea.fishyaddons.compat;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

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


    // -- Misc --

    public static Item getLimeDye() {
        return Items.DYE.lime();
    }

    private McApi() {}
}
