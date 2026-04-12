package me.valkeea.fishyaddons.vconfig.ui.widget.dropdown;

import java.util.List;
import java.util.function.Supplier;

import me.valkeea.fishyaddons.vconfig.ui.widget.VCTextField;
import me.valkeea.fishyaddons.vconfig.ui.widget.dropdown.item.SoundSearchItem;
import me.valkeea.fishyaddons.vconfig.ui.widget.dropdown.item.ToggleMenuItem;
import net.minecraft.client.gui.Click;

public class VCSearchDropdown extends VCToggleMenu {
    private VCTextField tf;

    public static VCSearchDropdown create(Supplier<List<ToggleMenuItem>> itemSupplier, int x, int y, int w, int h, VCTextField tf) {
        var dropdown = new VCSearchDropdown(itemSupplier, x, y, w, h);
        dropdown.tf = tf;
        return dropdown;
    }

    public VCSearchDropdown(Supplier<List<ToggleMenuItem>> itemSupplier, int x, int y, int w, int h) {
        super(itemSupplier, x, y, w, h, null);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled, float uiScale) {

        if (tf.mouseClicked(click, doubled) && doubled) {

            var item = getItem(1);
            if (item instanceof SoundSearchItem ssi) {
                String defaultValue = ssi.getDefaultValue();
                tf.setText(defaultValue);
                visible = false;
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
