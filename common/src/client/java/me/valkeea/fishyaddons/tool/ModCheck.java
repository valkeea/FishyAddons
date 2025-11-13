package me.valkeea.fishyaddons.tool;

import net.fabricmc.loader.api.FabricLoader;

public class ModCheck {
    private ModCheck() {}
    private static boolean hasSh;

    public static void init() {
        hasSh = FabricLoader.getInstance().isModLoaded("skyhanni");
    }

    public static boolean hasSh() {
        return hasSh;
    }
}
