package me.wait.fishyaddons.util;

public class SpawnData {
    public final String name;
    public final int minX, maxX;
    public final Integer minY, maxY;
    public final int minZ, maxZ;

    public SpawnData(String name, int minX, int maxX, Integer minY, Integer maxY, int minZ, int maxZ) {
        this.name = name;
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.minZ = minZ;
        this.maxZ = maxZ;
    }

    public boolean matches(int x, int y, int z) {
        boolean xMatch = x >= minX && x <= maxX;
        boolean zMatch = z >= minZ && z <= maxZ;
        boolean yMatch = (minY == null || maxY == null) || (y >= minY && y <= maxY);
        return xMatch && yMatch && zMatch;
    }
}