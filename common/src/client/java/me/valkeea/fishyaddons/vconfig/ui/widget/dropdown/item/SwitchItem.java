package me.valkeea.fishyaddons.vconfig.ui.widget.dropdown.item;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.api.IntKey;
import net.minecraft.network.chat.Component;

public class SwitchItem implements ToggleMenuItem {
    private final IntKey source;
    private final String label;
    private final int index;
    
    public SwitchItem(IntKey source, String label, int index) {
        this.source = source;
        this.label = label;
        this.index = index;
    }

    @Override
    public String getId() {
        return source.toString() + "_" + index;
    }

    @Override
    public String getDisplayName() {
        return label;
    }

    @Override
    public boolean isEnabled() {
        return Config.get(source) == index;
    }

    @Override
    public void toggle() {
        if (!isEnabled()) {
            Config.set(source, index);
        }
    }

    @Override
    public Component getDisabledSuffix() {
        return Component.empty();
    }

    /**
     * Create a list of SwitchItem instances starting with this item.
     *
     * @param labels the display labels for the subsequent SwitchItem instances
     * @return a list of SwitchItem instances with the same source
     */
    public static List<ToggleMenuItem> createList(List<String> labels, IntKey source, int startIndex) {
        return IntStream.range(startIndex, labels.size())
                        .mapToObj(i -> new SwitchItem(source, labels.get(i), i))
                        .collect(Collectors.toList());
    }
}
