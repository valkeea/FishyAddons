package me.valkeea.fishyaddons.feature.waypoints;

import net.minecraft.util.math.BlockPos;

public class Waypoint {
    public final BlockPos position;
    private String label;
    private boolean visited;

    public Waypoint(BlockPos position, String label, boolean visited) {
        this.position = position;
        this.label = label;
        this.visited = visited;
    }

    public boolean visited() {
        return visited;
    }

    public void setVisited(boolean visited) {
        this.visited = visited;
    }

    public String label() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
