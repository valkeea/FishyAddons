package me.valkeea.fishyaddons.tool;

import net.minecraft.client.Minecraft;

public class PlayerPosition {
    private PlayerPosition() {}

    public static void giveAwayCoords() {
        var mc = Minecraft.getInstance();
        if (mc.player != null) {
            String coords = getCoordsString(mc);
            mc.player.connection.sendChat(coords);
        }
    }
    
    public static void giveAwayCoordsWithLabel(String label) {
        var mc = Minecraft.getInstance();
        if (mc.player != null) {
            String coords = getCoordsString(mc);
            mc.player.connection.sendChat(coords + " " + label);
        }
    }

    public static void giveAwayFakeCoordsWithLabel(String label, int x, int y, int z) {
        var mc = Minecraft.getInstance();
        if (mc.player != null) {
            String coords = String.format("x: %d, y: %d, z: %d", x, y, z);
            mc.player.connection.sendChat(coords + " " + label);
        }
    }

    public static String getCoordsString(Minecraft mc) {
        int x = (int) mc.player.getX();
        int y = (int) mc.player.getY();
        int z = (int) mc.player.getZ();
        return String.format("x: %d, y: %d, z: %d", x, y, z);
    }
}
