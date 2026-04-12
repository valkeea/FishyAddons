package me.valkeea.fishyaddons.vconfig.ui.widget.dropdown.item;

import me.valkeea.fishyaddons.tool.PlaySound;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.api.DoubleKey;
import me.valkeea.fishyaddons.vconfig.api.StringKey;
import net.minecraft.text.Text;

public class SoundSearchItem implements ToggleMenuItem {
    private final StringKey key;
    private final DoubleKey volKey;
    private final BooleanKey bypassKey;
    private final String id;

    public SoundSearchItem(StringKey key, DoubleKey volKey, BooleanKey bypassKey, String id) {
        this.key = key;
        this.volKey = volKey;
        this.bypassKey = bypassKey;
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
        return Config.get(key).equals(id);
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
    public boolean useFixedWidth() {
        return true;
    }

    public StringKey getConfigKey() {
        return key;
    }
    
    public String getDefaultValue() {
        return key.getDefault();
    }

    public float getVolume() {
        return (float) Config.get(volKey);
    }

    public boolean usingBypass() {
        return Config.get(bypassKey);
    }

    @Override
    public Text getEnabledSuffix() {
        return Text.of("");
    }

    @Override
    public Text getDisabledSuffix() {
        return Text.of("");
    }
}
