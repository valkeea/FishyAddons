package me.valkeea.fishyaddons.feature.visual;

import me.valkeea.fishyaddons.vconfig.annotation.UIToggle;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.core.UICategory;

@VCModule(UICategory.RENDERING)
public class MobAnimations {
    private MobAnimations() {}
    
    @UIToggle(
        key = BooleanKey.FIRE_ANI,
        name = "Skip Entity *Fire* Animation",
        description = {
            "Prevent fire from rendering on entities.",
            "This will not remove the FOV effect."
        },
        autoSync = true
    )
    private static boolean fireAni;
    
    @UIToggle(
        key = BooleanKey.DEATH_ANI,
        name = "Skip Entity *Death* Animation", 
        description = {
            "Prevent death animation from rendering on entities.",
            "Useful for reducing visual clutter caused by already-dead entities.",
        },
        autoSync = true
    )
    private static boolean deathAni;
    
    public static boolean isFireAni() { 
        return fireAni;
    }
    
    public static boolean isDeathAni() { 
        return deathAni;
    }
}
