package me.valkeea.fishyaddons.util;

import net.minecraft.client.MinecraftClient;

public class PlayerPosition {
    private PlayerPosition() {}

    public static void giveAwayCoords() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            int x = (int) mc.player.getX();
            int y = (int) mc.player.getY();
            int z = (int) mc.player.getZ();
            String coords = String.format("x: %d, y: %d, z: %d", x, y, z);
            mc.player.networkHandler.sendChatMessage(coords);
        }
    }
}
