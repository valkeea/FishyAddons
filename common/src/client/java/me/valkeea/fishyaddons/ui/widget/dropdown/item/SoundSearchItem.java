package me.valkeea.fishyaddons.ui.widget.dropdown.item;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.tool.PlaySound;
import net.minecraft.text.Text;

public class SoundSearchItem implements ToggleMenuItem {
    private final String configKey;
    private final String id;

    public SoundSearchItem(String configKey, String id) {
        this.configKey = configKey;
        this.id = id;
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public String getDisplayName() {

        var dpn = id
        .replace("minecraft:", "")
        .replace("fishyaddons:fishyaddons.", "fa:");        

        int firstDot = dpn.indexOf('.');
        if (firstDot != -1) return dpn.substring(firstDot + 1);
        
        return dpn;
    }
    
    @Override
    public boolean isEnabled() {
        return FishyConfig.getString(configKey, getDefaultValue()).equals(id);
    }
    
    @Override
    public void toggle() {

        var current = FishyConfig.getString(configKey, getDefaultValue());

        if (current.equals(id)) {
            FishyConfig.setString(configKey, getDefaultValue());
        } else {
            FishyConfig.setString(configKey, id);
        }
    }

    @Override
    public boolean supportsRightClick() {
        return true;
    }

    @Override
    public boolean onRightClick() {

        var volume = getVolume();

        if (usingBypass()) {
            PlaySound.dynamicBypass(id, volume, 1.0f, false);
        } else {
            PlaySound.dynamic(id, volume, 1.0f, false);
        }

        return true;
    }

    @Override
    public Text getDisabledSuffix() {
        return Text.of("");
    }

    @Override
    public boolean useFixedWidth() {
        return true;
    }

    public String getConfigKey() {
        return configKey;
    }
    
    public String getDefaultValue() {
        return "";
    }

    public float getVolume() {
        return 0.5f;
    }

    public boolean usingBypass() {
        return false;
    }
}
