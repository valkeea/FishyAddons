package me.valkeea.fishyaddons.util;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public class CustomSounds {
    private CustomSounds() {}
    public static final SoundEvent FISHYADDONS_1 = registerSoundEvent("fishyaddons_1");
    public static final SoundEvent FISHYADDONS_2 = registerSoundEvent("fishyaddons_2");
    public static final SoundEvent FISHYADDONS_3 = registerSoundEvent("fishyaddons_3");
    
    private static SoundEvent registerSoundEvent(String name) {
        Identifier id = Identifier.fromNamespaceAndPath("fishyaddons", name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }
    
    public static void init() {
        // load
    }
}
