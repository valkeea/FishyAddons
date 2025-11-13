package me.valkeea.fishyaddons.util;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class CustomSounds {
    private CustomSounds() {}
    public static final SoundEvent FISHYADDONS_1 = registerSoundEvent("fishyaddons_1");
    public static final SoundEvent FISHYADDONS_2 = registerSoundEvent("fishyaddons_2");
    public static final SoundEvent FISHYADDONS_3 = registerSoundEvent("fishyaddons_3");
    
    private static SoundEvent registerSoundEvent(String name) {
        Identifier id = Identifier.of("fishyaddons", name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }
    
    public static void init() {
        // Sound events are registered during class loading
        // This method exists to ensure the class is loaded
    }
}
