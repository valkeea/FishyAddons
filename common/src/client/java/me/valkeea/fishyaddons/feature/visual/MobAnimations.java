package me.valkeea.fishyaddons.feature.visual;

import me.valkeea.fishyaddons.config.FishyConfig;

public class MobAnimations {
    private MobAnimations() {}
    private static boolean fireAni = false;
    private static boolean deathAni = false;

    public static void refresh() {
        deathAni = FishyConfig.getState(me.valkeea.fishyaddons.config.Key.DEATH_ANI, false);
        fireAni = FishyConfig.getState(me.valkeea.fishyaddons.config.Key.FIRE_ANI, false);
    }

    public static boolean isFireAni() { return fireAni;}
    public static boolean isDeathAni() { return deathAni;}    
}
