package me.valkeea.fishyaddons.ui.widget.dropdown.item;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;

public class ReelItem extends SoundSearchItem {
    public ReelItem(String configKey, String id) {
        super(configKey, id);
    }

    @Override
    public String getDefaultValue() {
        return "minecraft:block.note_block.pling";
    }

    @Override
    public float getVolume() {
        return FishyConfig.getFloat(Key.REEL_ALERT, 0.5f);
    }

    @Override
    public boolean usingBypass() {
        return FishyConfig.getState(Key.REEL_TRUE_VOLUME, false);
    }
}
