package me.valkeea.fishyaddons.util;

import java.util.Arrays;
import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

/**
 * This approach allows for instant area detection based on spawn position.
 * Failure is possible but rare and for critical features a fallback can be implemented.
 **/
public class AreaUtils {
    private AreaUtils() {}
    private static final String CI = "crimson_isles";
    private static final String HUB = "hub";
    private static final String DH = "dungeon_hub";
    private static final String CH = "crystal_hollows";
    private static final String DM = "dwarven_mines";
    private static final String END = "the_end";
    private static final String FI = "farming_islands";
    private static final String GT = "glacite_tunnels";
    private static final String PARK = "park";
    private static final String DEN = "den";
    private static final String DEF = "default";
    private static final String GAL = "galatea";

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static String currentIsland = DEF;
    private static boolean isGalatea = false;    

    private static final List<SpawnData> SPAWN_DATA = Arrays.asList(
        // Crimson Isles
        new SpawnData(CI, -370, -350, 75, 120, -450, -420),
        new SpawnData(CI, -380, -360, 110, 125, -1000, -970),
        // Hub
        new SpawnData(HUB, -45, -43, null, null, 10, 13),
        new SpawnData(HUB, -223, -200, null, null, -16, -14),
        new SpawnData(HUB, -3, -2, null, null, -69, -68),
        new SpawnData(HUB, -160, -158, null, null, -159, -157),
        new SpawnData(HUB, 75, 77, null, null, -183, -180),
        new SpawnData(HUB, -11, -8, null, null, -229, -227),
        new SpawnData(HUB, 90, 92, null, null, 172, 174),
        new SpawnData(HUB, 41, 43, null, null, 68, 70),
        new SpawnData(HUB, -251, -249, null, null, 44, 46),
        new SpawnData(HUB, -162, -160, null, null, -100, -99),
        // Dungeon Hub
        new SpawnData(DH, -32, -29, 120, 122, -2, 2),
        // Crystal Hollows
        new SpawnData(CH, 500, 530, null, null, 500, 550),
        new SpawnData(CH, 200, 230, null, null, 400, 440),
        // Dwarven Mines
        new SpawnData(DM, -60, -30, null, null, -130, -100),
        new SpawnData(DM, 0, 1, null, null, -69, -68),
        // The End
        new SpawnData(END, -503, -501, null, null, -276, -274),
        new SpawnData(END, -570, -569, null, null, -319, -317),
        new SpawnData(END, -607, -605, null, null, -276, -274),
        // Farming Islands
        new SpawnData(FI, 100, 130, null, null, -230, -180),
        new SpawnData(FI, 140, 160, null, null, -320, -290),
        new SpawnData(FI, 150, -170, null, null, -390, -350),
        // Glacite Tunnels
        new SpawnData(GT, -10, 10, null, null, 190, 210),
        // Park
        new SpawnData(PARK, -485, -481, null, null, -43, -40),
        new SpawnData(PARK, -266, -264, null, null, -19, -16),
        new SpawnData(PARK, -468, -465, null, null, -34, -32),
        // Galatea
        new SpawnData(GAL, -549, -546, null, null, -24, -21),
        new SpawnData(GAL, -645, -643, null, null, 1, 3),
        // Spider's Den
        new SpawnData(DEN, -203, -201, null, null, -233, -231),
        new SpawnData(DEN, -190, -188, null, null, -311, -309)
    );

    public static String getIsland() {
        return currentIsland;
    }

    public static void setIsland(String island) {
        currentIsland = island;
        isGalatea = setGalatea(island);
        //RetexHandler.setIsland(island);
    }

    public static void updateIsland() {
        if (mc.world == null || mc.player == null) return;

        BlockPos pos = mc.player.getBlockPos();
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();

        for (SpawnData data : SPAWN_DATA) {
            if (data.matches(x, y, z)) {
                setIsland(data.name);
                return;
            }
        }
        setIsland("default");
    
    }

    public static boolean setGalatea(String island) {
        if (GAL.equalsIgnoreCase(island)) {
            isGalatea = true;
            return true;
        }
        isGalatea = false;
        return false;
    }

    public static boolean isGalatea() {
        return isGalatea;
    }

    public static boolean isDenOrPark() {
        return DEN.equalsIgnoreCase(currentIsland) || PARK.equalsIgnoreCase(currentIsland);
    }

    // Inner class for spawn data
    public static class SpawnData {
        public final String name;
        public final int x1, x2;
        public final Integer y1, y2;
        public final int z1, z2;

        public SpawnData(String name, int x1, int x2, Integer y1, Integer y2, int z1, int z2) {
            this.name = name;
            this.x1 = x1;
            this.x2 = x2;
            this.y1 = y1;
            this.y2 = y2;
            this.z1 = z1;
            this.z2 = z2;
        }

        public boolean matches(int x, int y, int z) {
            boolean xMatch = x >= Math.min(x1, x2) && x <= Math.max(x1, x2);
            boolean zMatch = z >= Math.min(z1, z2) && z <= Math.max(z1, z2);
            boolean yMatch = (y1 == null || y2 == null) || (y >= Math.min(y1, y2) && y <= Math.max(y1, y2));
            return xMatch && yMatch && zMatch;
        }
    }
}