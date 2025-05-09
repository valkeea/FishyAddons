package me.wait.fishyaddons.util;

import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;

import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Arrays;
import java.util.List;

/*
 * Uses spawn position to determine the current island.
 * Instant area info is required for FAretex,
 * will propably be moved to a data class when the feature is finished.
 */

@SideOnly(Side.CLIENT)
public class AreaUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static String currentIsland = "default";

    public static String getIsland() {
        return currentIsland;
    }

    public static void setIsland(String island) {
        currentIsland = island;
    }

    public static void updateIsland() {
        if (mc.theWorld == null || mc.thePlayer == null) return;

        BlockPos pos = mc.thePlayer.getPosition();
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();

        // Define spawn ranges per island
        if (inRange(x, -370, -350) && inRange(y, 75, 120) && inRange(z, -450, -420)) {
            setIsland("crimson_isles");
        } else if (inRange(x, -380, -360) && inRange(y, 110, 125) && inRange(z, -1000, -970)) {
            setIsland("crimson_isles");
        } else if ((inRange(x, -45, -43) && inRange(z, 10, 13)) ||
                   (inRange(x, -223, -200) && inRange(z, -16, -14)) ||
                   (inRange(x, -3, -2) && inRange(z, -69, -68)) ||
                   (inRange(x, -160, -158) && inRange(z, -159, -157)) ||
                   (inRange(x, 75, 77) && inRange(z, -183, -180)) ||
                   (inRange(x, -11, -8) && inRange(z, -229, -227)) ||
                   (inRange(x, 90, 92) && inRange(z, 172, 174)) ||
                   (inRange(x, 41, 43) && inRange(z, 68, 70)) ||
                   (inRange(x, -251, -249) && inRange(z, 44, 46)) ||
                   (inRange(x, -162, -160) && inRange(z, -100, -99))) {
            setIsland("hub");
        } else if (inRange(x, -30, -27) && inRange(y, 120, 122) && inRange(z, -2, 2)) {
            setIsland("dungeon_hub");
        } else if ((inRange(x, 500, 530) && inRange(z, 500, 550)) ||
                   (inRange(x, 200, 230) && inRange(z, 400, 440))) {
            setIsland("crystal_hollows");
        } else if ((inRange(x, -60, -30) && inRange(z, -130, -100)) ||
                   (inRange(x, 0, 1) && inRange(z, -69, -68))) {
            setIsland("dwarven_mines");
        } else if ((inRange(x, -503, -501) && inRange(z, -276, -274)) ||
                     (inRange(x, -570, -569) && inRange(z, -319, -317)) ||
                   (inRange(x, -607, -605) && inRange(z, -276, -274))) {
            setIsland("the_end");
        } else if ((inRange(x, 100, 130) && inRange(z, -230, -180)) ||
                   (inRange(x, 140, 160) && inRange(z, -320, -290)) ||
                   (inRange(x, 150, -170) && inRange(z, -390, -350))) {
            setIsland("farming_islands");            
        } else if (inRange(x, -10, 10) && inRange(z, 190, 210)) {
            setIsland("glacite_tunnels");
        } else if ((inRange(x, -278, -276) && inRange(z, -14, -13)) ||
            (inRange(x, -463, -461) && inRange(z, -126, -124))) {
            setIsland("park");
        } else {
            setIsland("default");
        }

        System.out.println("Island set via spawn position: " + getIsland());
    }

    private static boolean inRange(int val, int min, int max) {
        return val >= min && val <= max;
    }
}