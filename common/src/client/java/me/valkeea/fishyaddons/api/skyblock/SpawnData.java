package me.valkeea.fishyaddons.api.skyblock;

import java.util.Arrays;
import java.util.List;

import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas.Island;
import net.minecraft.client.MinecraftClient;

public class SpawnData {
    private SpawnData() {}

    private static final List<SpawnZone> SPAWN_DATA = Arrays.asList(
        // Crimson Isles
        new SpawnZone(Island.CI, -370, -350, 75, 120, -450, -420),
        new SpawnZone(Island.CI, -380, -360, 110, 125, -1000, -970),
        new SpawnZone(Island.CI, -376, -373, 113, 115, -1035, -1030),
        new SpawnZone(Island.CI, -293, -287, 120, 126, -1000, -993),
        // Hub
        new SpawnZone(Island.HUB, -45, -43, null, null, 10, 13),
        new SpawnZone(Island.HUB, -223, -200, null, null, -16, -14),
        new SpawnZone(Island.HUB, -4, -1, 68, 71, -71, -68),
        new SpawnZone(Island.HUB, -160, -158, null, null, -159, -157),
        new SpawnZone(Island.HUB, 75, 77, null, null, -183, -180),
        new SpawnZone(Island.HUB, -11, -8, null, null, -229, -227),
        new SpawnZone(Island.HUB, 90, 92, null, null, 172, 174),
        new SpawnZone(Island.HUB, 41, 43, null, null, 68, 70),
        new SpawnZone(Island.HUB, -251, -249, null, null, 44, 46),
        new SpawnZone(Island.HUB, -162, -160, null, null, -100, -99),
        // New Hub
        new SpawnZone(Island.HUB, -2, 1, 70, 80, -2, 2), // hub main
        new SpawnZone(Island.HUB, 28, 31, 70, 80, 0, 3), // museum
        new SpawnZone(Island.HUB, -221, -220, 73, 74, -16, -15), // hub park
        new SpawnZone(Island.HUB, -145, -141, 76, 74, -185, -183), // hub den
        new SpawnZone(Island.HUB, -1, 0, 64, 65, -188, -187), // hub mines
        new SpawnZone(Island.HUB, 83, 86, 70, 75, -151, -149), // hub barn
        // Dungeon Hub
        new SpawnZone(Island.DH, -32, -29, 120, 122, -2, 2),
        // Crystal Hollows
        new SpawnZone(Island.CH, 500, 530, null, null, 500, 550),
        new SpawnZone(Island.CH, 200, 230, null, null, 400, 440),
        // Dwarven Mines
        new SpawnZone(Island.DM, -50, -47, 198, 202, -125, -119),
        new SpawnZone(Island.DM, -1, 2, 147, 149, -70, -67),
        // The End
        new SpawnZone(Island.END, -503, -501, null, null, -276, -274),
        new SpawnZone(Island.END, -570, -569, null, null, -319, -317),
        new SpawnZone(Island.END, -607, -605, null, null, -276, -274),
        // New End: -503 10 -275, -569.9 7 -318, -606.5 22 -275
        new SpawnZone(Island.END, -504, -502, 7, 12, -277, -273),
        new SpawnZone(Island.END, -572, -567, 6, 9, -320, -316),
        new SpawnZone(Island.END, -608, -605, 20, 24, -277, -273),
        // Farming Islands
        new SpawnZone(Island.FI, 100, 130, null, null, -230, -180),
        new SpawnZone(Island.FI, 140, 160, null, null, -320, -290),
        // New Farming Islands: 160.5 77 -370, 160.5 77 -370
        new SpawnZone(Island.FI, 111, 115, 69, 73, -210, -205),
        new SpawnZone(Island.FI, 158, 162, 75, 78, -372, -368),
        // Glacite Tunnels
        new SpawnZone(Island.GT, -1, 1, 127, 129, 198, 202),
        // Park
        new SpawnZone(Island.PARK, -485, -481, null, null, -43, -40),
        new SpawnZone(Island.PARK, -266, -264, null, null, -19, -16),
        new SpawnZone(Island.PARK, -468, -465, null, null, -34, -32),
        // Galatea
        new SpawnZone(Island.GAL, -549, -541, 106, 110, -28, -21),
        new SpawnZone(Island.GAL, -645, -643, null, null, 1, 3),
        // Spider's Den
        new SpawnZone(Island.DEN, -357, -353, 85, 90, -349, -345),
        new SpawnZone(Island.DEN, -203, -200, 82, 84, -235, -231),
        new SpawnZone(Island.DEN, -190, -188, null, null, -311, -309),
        // New den: 378 118.5 -261
        new SpawnZone(Island.DEN, -380, -375, 117, 120, -263, -258),
        // Bayou
        new SpawnZone(Island.BAYOU, -14, -10, 72, 76, -13, -9),
        // Jerry
        new SpawnZone(Island.JERRY, -6, -3, 75, 78, 99, 102),
        // Rift
        new SpawnZone(Island.RIFT, -47, -40, 120, 125, 66, 72)
    );

    public static void updateIsland() {
        var mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        var pos = mc.player.getBlockPos();

        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        SPAWN_DATA.stream().filter(zone -> zone.matches(x, y, z)).findFirst()
            .ifPresent(zone -> SkyblockAreas.setIsland(zone.name));
    }

    private static class SpawnZone {
        private final Island name;
        private final int x1;
        private final int x2;
        private final Integer y1;
        private final Integer y2;
        private final int z1;
        private final int z2;

        private SpawnZone(Island name, int x1, int x2, Integer y1, Integer y2, int z1, int z2) {
            this.name = name;
            this.x1 = x1;
            this.x2 = x2;
            this.y1 = y1;
            this.y2 = y2;
            this.z1 = z1;
            this.z2 = z2;
        }

        private boolean matches(int x, int y, int z) {
            boolean xMatch = x >= Math.min(x1, x2) && x <= Math.max(x1, x2);
            boolean zMatch = z >= Math.min(z1, z2) && z <= Math.max(z1, z2);
            boolean yMatch = (y1 == null || y2 == null) || (y >= Math.min(y1, y2) && y <= Math.max(y1, y2));
            return xMatch && yMatch && zMatch;
        }
    }
}
