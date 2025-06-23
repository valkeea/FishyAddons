package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import me.valkeea.fishyaddons.config.FishyConfig;
import net.minecraft.client.option.Perspective;

@Mixin(Perspective.class)
public abstract class MixinPerspective {
    /**
     * Overwrites the next() method to skip THIRD_PERSON_FRONT if enabled in config.
     */
    @Overwrite
    public Perspective next() {
        if (!FishyConfig.getState("skipPerspective", false)) {
            // Default vanilla cycling
            Perspective[] values = Perspective.values();
            return values[(((Perspective)(Object)this).ordinal() + 1) % values.length];
        }
        switch ((Perspective)(Object)this) {
            case FIRST_PERSON:
                return Perspective.THIRD_PERSON_BACK;
            case THIRD_PERSON_BACK, THIRD_PERSON_FRONT:
            default:
                return Perspective.FIRST_PERSON;
        }
    }
}