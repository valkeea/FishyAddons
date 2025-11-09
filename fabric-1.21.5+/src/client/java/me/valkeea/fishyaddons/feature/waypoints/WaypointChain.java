package me.valkeea.fishyaddons.feature.waypoints;

import java.util.List;

public class WaypointChain {
    public final String area;
    public final List<Waypoint> waypoints;
    public final ChainType type;
    private String name;

    public WaypointChain(String area, String name, List<Waypoint> waypoints, ChainType type) {
        this.area = area;
        this.name = name;
        this.waypoints = waypoints;
        this.type = type;
    }

    public String name() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
}
