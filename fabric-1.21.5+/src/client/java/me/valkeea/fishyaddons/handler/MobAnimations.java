package me.valkeea.fishyaddons.handler;

import me.valkeea.fishyaddons.config.FishyConfig;

public class MobAnimations {
    private MobAnimations() {}
    private static boolean fireAni = false;
    private static boolean deathAni = false;

    public static void refresh() {
        deathAni = FishyConfig.getState("deathAni", false);
        fireAni = FishyConfig.getState("fireAni", false);
    }

    public static boolean isFireAni() { return fireAni;}
    public static boolean isDeathAni() { return deathAni;}    

}
