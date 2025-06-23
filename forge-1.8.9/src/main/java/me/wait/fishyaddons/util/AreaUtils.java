package me.wait.fishyaddons.util;

import me.wait.fishyaddons.handlers.RetexHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Arrays;
import java.util.List;

/*
 * FAretex requires instantaneous area detection.
 * This class allows for area detection based on spawn position
 * instead of relying on scoreboard or other methods.
 */

@SideOnly(Side.CLIENT)
public class AreaUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static String currentIsland = "default";

    private static final List<SpawnData> SPAWN_DATA = Arrays.asList(
        // Crimson Isles
        new SpawnData("crimson_isles", -370, -350, 75, 120, -450, -420),
        new SpawnData("crimson_isles", -380, -360, 110, 125, -1000, -970),
        // Hub
        new SpawnData("hub", -45, -43, null, null, 10, 13),
        new SpawnData("hub", -223, -200, null, null, -16, -14),
        new SpawnData("hub", -3, -2, null, null, -69, -68),
        new SpawnData("hub", -160, -158, null, null, -159, -157),
        new SpawnData("hub", 75, 77, null, null, -183, -180),
        new SpawnData("hub", -11, -8, null, null, -229, -227),
        new SpawnData("hub", 90, 92, null, null, 172, 174),
        new SpawnData("hub", 41, 43, null, null, 68, 70),
        new SpawnData("hub", -251, -249, null, null, 44, 46),
        new SpawnData("hub", -162, -160, null, null, -100, -99),
        // Dungeon Hub
        new SpawnData("dungeon_hub", -30, -27, 120, 122, -2, 2),
        // Crystal Hollows
        new SpawnData("crystal_hollows", 500, 530, null, null, 500, 550),
        new SpawnData("crystal_hollows", 200, 230, null, null, 400, 440),
        // Dwarven Mines
        new SpawnData("dwarven_mines", -60, -30, null, null, -130, -100),
        new SpawnData("dwarven_mines", 0, 1, null, null, -69, -68),
        // The End
        new SpawnData("the_end", -503, -501, null, null, -276, -274),
        new SpawnData("the_end", -570, -569, null, null, -319, -317),
        new SpawnData("the_end", -607, -605, null, null, -276, -274),
        // Farming Islands
        new SpawnData("farming_islands", 100, 130, null, null, -230, -180),
        new SpawnData("farming_islands", 140, 160, null, null, -320, -290),
        new SpawnData("farming_islands", 150, -170, null, null, -390, -350),
        // Glacite Tunnels
        new SpawnData("glacite_tunnels", -10, 10, null, null, 190, 210),
        // Park
        new SpawnData("park", -278, -276, null, null, -14, -13),
        new SpawnData("park", -463, -461, null, null, -126, -124)
    );

    public static String getIsland() {
        return currentIsland;
    }

    public static void setIsland(String island) {
        currentIsland = island;
        RetexHandler.setIsland(island);
    }

    public static void updateIsland() {
        if (mc.theWorld == null || mc.thePlayer == null) return;

        BlockPos pos = mc.thePlayer.getPosition();
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();

        for (SpawnData data : SPAWN_DATA) {
            if (data.matches(x, y, z)) {
                setIsland(data.name);
                return;
            }
        }
        setIsland("default");
    }
}