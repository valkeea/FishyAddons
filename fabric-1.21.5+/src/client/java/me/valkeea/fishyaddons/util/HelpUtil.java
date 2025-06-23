package me.valkeea.fishyaddons.util;

import net.minecraft.client.MinecraftClient;

public class HelpUtil {
    private HelpUtil() {}

    // Strip color codes from a string
    public static String stripColor(String text) {
        return text == null ? "" : text.replaceAll("(?i)§[0-9a-fk-or]", "");
    }
    
    public static boolean isPlayerInventory() {
        // Check if the current screen is the player inventory or server gui
        return MinecraftClient.getInstance().currentScreen != null &&
               (MinecraftClient.getInstance().currentScreen instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen ||
                MinecraftClient.getInstance().currentScreen instanceof net.minecraft.client.gui.screen.ingame.GenericContainerScreen);
    }
}
