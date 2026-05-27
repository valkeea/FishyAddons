package me.valkeea.fishyaddons.vconfig.core;

public enum UICategory {
    
    INTERFACE("Interface"),
    RENDERING("Rendering Tweaks", "Rendering"),
    QOL("General QoL", "QoL"),
    WAYPOINTS("Waypoints"),
    SKYBLOCK("Skyblock Features", "Skyblock"),
    AUDIO("Audio"),
    FISHING("Fishing"),
    FILTER("Chat Filter"),
    ITEMS("Items"),
    NONE("Miscellaneous", "Misc")
    ;
    
    private final String name;
    private final String shortName;

    UICategory(String name, String shortName) {
        this.name = name;
        this.shortName = shortName;
    }
    
    UICategory(String name) {
        this.name = name;
        this.shortName = name;
    }

    @Override
    public String toString() { return name; }
    public String shortName() { return shortName; }
}
