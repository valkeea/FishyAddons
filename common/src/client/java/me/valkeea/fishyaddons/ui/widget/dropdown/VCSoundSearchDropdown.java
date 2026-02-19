package me.valkeea.fishyaddons.ui.widget.dropdown;

import java.util.List;
import java.util.function.Supplier;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.ui.widget.VCTextField;
import me.valkeea.fishyaddons.ui.widget.dropdown.item.SoundSearchItem;
import me.valkeea.fishyaddons.ui.widget.dropdown.item.ToggleMenuItem;
import net.minecraft.client.gui.Click;

public class VCSoundSearchDropdown extends VCToggleMenu {
    private VCTextField tf;

    public static VCSoundSearchDropdown create(Supplier<List<ToggleMenuItem>> itemSupplier, int x, int y, int w, int h, Runnable onRefresh, VCTextField tf) {
        var dropdown = new VCSoundSearchDropdown(itemSupplier, x, y, w, h, onRefresh);
        dropdown.tf = tf;
        return dropdown;
    }

    public VCSoundSearchDropdown(Supplier<List<ToggleMenuItem>> itemSupplier, int x, int y, int w, int h, Runnable onRefresh) {
        super(itemSupplier, x, y, w, h, onRefresh);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled, float uiScale) {

        if (tf.mouseClicked(click, doubled) && doubled) {

            var item = getItem(1);
            if (item instanceof SoundSearchItem ssi) {
                
                String defaultValue = ssi.getDefaultValue();
                tf.setText(defaultValue);
                FishyConfig.setString(ssi.getConfigKey(), defaultValue);
                onRefresh();
            }
            return true;
        }

        return super.mouseClicked(click, doubled, uiScale);
    }

    @Override
    public void clickAction(ToggleMenuItem item, boolean rightClick) {
        if (!rightClick) {
            tf.setFocused(false);
            tf.setText(item.getId());            
        }
        super.clickAction(item, rightClick);
    }

    @Override
    public String getSearchText() {
        return tf.getText();
    }
    
    public VCTextField getSearchField() {
        return tf;
    }
}
