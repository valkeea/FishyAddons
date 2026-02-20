package me.valkeea.fishyaddons.hud.elements.simple;

import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.feature.skyblock.PetInfo;
import me.valkeea.fishyaddons.hud.base.SimpleHudElement;
import net.minecraft.text.Text;

public class PetDisplay extends SimpleHudElement {
    
    public PetDisplay() {
        super(
            Key.HUD_PET_ENABLED,
            "Pet Display",
            25, 5,
            12,
            0xFFFFFFFF,
            true,
            false
        );
    }

    @Override
    protected boolean shouldRender() {
        return PetInfo.isOn();
    }

    @Override
    protected Text getText() {
        return PetInfo.getPet();
    }
}
