package me.valkeea.fishyaddons.ui.widget.dropdown.item;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;

public class FeroItem extends SoundSearchItem {
    public FeroItem(String configKey, String id) {
        super(configKey, id);
    }

    @Override
    public String getDefaultValue() {
        return "entity.zombie.break_wooden_door";
    }

    @Override
    public float getVolume() {
        return FishyConfig.getFloat(Key.CUSTOM_FERO, 0.5f);
    }

    @Override
    public boolean usingBypass() {
        return FishyConfig.getState(Key.FERO_TRUE_VOLUME, false);
    }
}
