package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import me.valkeea.fishyaddons.vconfig.api.Config;
import net.minecraft.client.CameraType;

@Mixin(CameraType.class)
public abstract class MixinPerspective {

    /**
     * Overwrites the next() method to skip THIRD_PERSON_FRONT if enabled in config.
     * @reason Allows skipping the THIRD_PERSON_FRONT perspective when cycling if enabled in config.
     */
    @Overwrite
    public CameraType cycle() {
        if (!Config.get(me.valkeea.fishyaddons.vconfig.api.BooleanKey.SKIP_F5)) {
            var values = CameraType.values();
            return values[(((CameraType)(Object)this).ordinal() + 1) % values.length];
        }

        switch ((CameraType)(Object)this) {
            case FIRST_PERSON:
                return CameraType.THIRD_PERSON_BACK;
            case THIRD_PERSON_BACK, THIRD_PERSON_FRONT:
            default:
                return CameraType.FIRST_PERSON;
        }
    }
}
