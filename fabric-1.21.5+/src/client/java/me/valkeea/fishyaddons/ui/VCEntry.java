package me.valkeea.fishyaddons.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.ItemConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.safeguard.BlacklistManager;
import me.valkeea.fishyaddons.tool.FishyMode;
import me.valkeea.fishyaddons.ui.VCScreen.ExtraControl;
import net.minecraft.text.Text;

/**
 * Represents a configuration entry in VCScreen
 */
@SuppressWarnings("squid:S107")
public class VCEntry {
    public enum EntryType {
        TOGGLE,
        BUTTON,
        EXPANDABLE,
        ITEM_CONFIG_TOGGLE,
        BLACKLIST_TOGGLE,
        KEYBIND,
        HEADER,
        SIMPLE_BUTTON,
        SLIDER,
        TOGGLE_WITH_SLIDER
    }
    
    public final String name;
    public final String description;
    public final EntryType type;
    public final Runnable action;

    // Optional tooltip
    public final List<Text> tooltipText;

    // Toggle entries
    public final String configKey;
    public final boolean defaultValue;
    public final Runnable refreshAction;

    // Sub-entries
    public final List<VCEntry> subEntries;

    // HUD entries
    protected final String hudElementName;
    protected final boolean hasColorControl;
    protected final boolean hasAdd;

    // Keybind entries
    private boolean isListening = false;
    
    // Simple toggle entries
    public final String buttonText;
    public final String simpleButtonConfigKey;
    public final boolean simpleButtonDefault;

    // Slider entries
    public final float minValue;
    public final float maxValue;
    public final String formatString;
    public final Consumer<Float> valueChangeAction;
    public final SliderType sliderType;
    
    // Enum for different slider value types
    public enum SliderType {
        FLOAT,
        STRING,
        PRESET
    }
    
    // Builder pattern
    private VCEntry(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.type = builder.type;
        this.tooltipText = builder.tooltipText != null ? List.copyOf(builder.tooltipText) : null;
        this.action = builder.action;
        this.configKey = builder.configKey;
        this.defaultValue = builder.defaultValue;
        this.refreshAction = builder.refreshAction;
        this.subEntries = builder.subEntries;
        this.hudElementName = builder.hudElementName;
        this.hasColorControl = builder.hasColorControl;
        this.hasAdd = builder.hasAdd;
        this.buttonText = builder.buttonText;
        this.simpleButtonConfigKey = builder.simpleButtonConfigKey;
        this.simpleButtonDefault = builder.simpleButtonDefault;
        this.minValue = builder.minValue;
        this.maxValue = builder.maxValue;
        this.formatString = builder.formatString;
        this.valueChangeAction = builder.valueChangeAction;
        this.sliderType = builder.sliderType;
    }
    
    // Builder class for VCEntry
    public static class Builder {
        // Required fields
        private final String name;
        private final String description;
        private final EntryType type;
        
        // Optional fields
        private List<Text> tooltipText = null;
        private Runnable action = null;
        private String configKey = null;
        private boolean defaultValue = false;
        private Runnable refreshAction = null;
        private List<VCEntry> subEntries = null;
        private String hudElementName = null;
        private boolean hasColorControl = false;
        private boolean hasAdd = false;
        private String buttonText = null;
        private String simpleButtonConfigKey = null;
        private boolean simpleButtonDefault = false;
        private float minValue = 0.0f;
        private float maxValue = 100.0f;
        private String formatString = "%.0f";
        private Consumer<Float> valueChangeAction = null;
        private SliderType sliderType = SliderType.FLOAT;
        
        public Builder(String name, String description, EntryType type) {
            this.name = name;
            this.description = description;
            this.type = type;
        }
        
        public Builder action(Runnable action) {
            this.action = action;
            return this;
        }
        
        public Builder configKey(String configKey) {
            this.configKey = configKey;
            return this;
        }
        
        public Builder defaultValue(boolean defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }
        
        public Builder refreshAction(Runnable refreshAction) {
            this.refreshAction = refreshAction;
            return this;
        }
        
        public Builder subEntries(List<VCEntry> subEntries) {
            this.subEntries = subEntries;
            return this;
        }

        public Builder tooltipText(List<Text> tooltipText) {
            this.tooltipText = tooltipText;
            return this;
        }

        public Builder hudElementName(String hudElementName) {
            this.hudElementName = hudElementName;
            return this;
        }
        
        public Builder hasColorControl(boolean hasColorControl) {
            this.hasColorControl = hasColorControl;
            return this;
        }

        public Builder hasAdd(boolean hasAdd) {
            this.hasAdd = hasAdd;
            return this;
        }

        public Builder extraControl(ExtraControl extraControl) {
            if (extraControl != null) {
                this.hudElementName = extraControl.getElementName();
                this.hasColorControl = extraControl.hasColorControl();
                this.hasAdd = extraControl.hasAdd();
            }
            return this;
        }
        
        public Builder buttonText(String buttonText) {
            this.buttonText = buttonText;
            return this;
        }
        
        public Builder simpleButtonConfigKey(String simpleButtonConfigKey) {
            this.simpleButtonConfigKey = simpleButtonConfigKey;
            return this;
        }
        
        public Builder simpleButtonDefault(boolean simpleButtonDefault) {
            this.simpleButtonDefault = simpleButtonDefault;
            return this;
        }

        public Builder sliderRange(float minValue, float maxValue) {
            this.minValue = minValue;
            this.maxValue = maxValue;
            return this;
        }
        
        public Builder formatString(String formatString) {
            this.formatString = formatString;
            return this;
        }
        
        public Builder valueChangeAction(Consumer<Float> valueChangeAction) {
            this.valueChangeAction = valueChangeAction;
            return this;
        }
        
        public Builder sliderType(SliderType sliderType) {
            this.sliderType = sliderType;
            return this;
        }
        
        // Convenience method for TOGGLE_WITH_SLIDER
        public Builder toggleWithSlider(String toggleKey, boolean defaultToggle, String sliderKey, float min, float max, String format) {
            this.configKey = toggleKey;
            this.defaultValue = defaultToggle;
            this.buttonText = sliderKey;
            this.minValue = min;
            this.maxValue = max;
            this.formatString = format;
            return this;
        }
        
        public VCEntry build() {
            return new VCEntry(this);
        }
    }

    /**
     * Static factory methods for entry types,
     * Naming scheme is based on component reading order
     */
    public static VCEntry toggle(String name, String description, String configKey, boolean defaultValue, Runnable refreshAction) {
        return new Builder(name, description, EntryType.TOGGLE)
            .configKey(configKey)
            .defaultValue(defaultValue)
            .refreshAction(refreshAction)
            .build();
    }

    // Toggle + HUD shortcut
    public static VCEntry toggleColorOrHud(String name, String description, String configKey, boolean defaultValue, Runnable refreshAction, ExtraControl extraControl) {
        return new Builder(name, description, EntryType.TOGGLE)
            .configKey(configKey)
            .defaultValue(defaultValue)
            .refreshAction(refreshAction)
            .extraControl(extraControl)
            .build();
    }

    // Toggle for ItemConfig
    public static VCEntry icToggle(String name, String description, String configKey, boolean defaultValue) {
        return new Builder(name, description, EntryType.ITEM_CONFIG_TOGGLE)
            .configKey(configKey)
            .defaultValue(defaultValue)
            .build();
    }

    // Toggle for BlacklistManager
    public static VCEntry blToggle(String name, String description, String configKey, boolean defaultValue) {
        return new Builder(name, description, EntryType.BLACKLIST_TOGGLE)
            .configKey(configKey)
            .defaultValue(defaultValue)
            .build();
    }

    // 2 toggles + config option with no desc needed (simplebutton)
    public static VCEntry toggle2(String name, String description, String mainConfigKey, boolean defaultValue, 
                                                String buttonText, String buttonConfigKey, Boolean simpleDefault, Runnable mainRefresh) {
        return new Builder(name, description, EntryType.SIMPLE_BUTTON)
            .configKey(mainConfigKey)
            .defaultValue(defaultValue)
            .buttonText(buttonText)
            .simpleButtonConfigKey(buttonConfigKey)
            .simpleButtonDefault(simpleDefault)
            .refreshAction(mainRefresh)
            .build();
    }  
    
    // Toggle + slider
    public static VCEntry toggleSlider(String name, String description, String toggleKey, boolean defaultToggle, String sliderKey, float min, float max, String format, Runnable refreshAction) {
        return new Builder(name, description, EntryType.TOGGLE_WITH_SLIDER)
            .toggleWithSlider(toggleKey, defaultToggle, sliderKey, min, max, format)
            .refreshAction(refreshAction)
            .valueChangeAction(v -> refreshAction.run())
            .build();
    }    

    // Slider
    public static VCEntry slider(String name, String description, String configKey, float min, float max, String format, Consumer<Float> valueChangeAction) {
        return new Builder(name, description, EntryType.SLIDER)
            .configKey(configKey)
            .sliderRange(min, max)
            .formatString(format)
            .valueChangeAction(valueChangeAction)
            .build();
    }

    // Slider with type
    public static VCEntry typedSlider(String name, String description, String type, float min, float max, String format, Consumer<Float> valueChangeAction, SliderType sliderType) {
        return new Builder(name, description, EntryType.SLIDER)
            .configKey(type)
            .sliderRange(min, max)
            .formatString(format)
            .valueChangeAction(valueChangeAction)
            .sliderType(sliderType)
            .build();
    }

    // Slider + colorsquare
    public static VCEntry typedSliderColor(String name, String description, String configKey, float min, float max, String format, Consumer<Float> valueChangeAction, SliderType sliderType, boolean hasColorControl) {
        return new Builder(name, description, EntryType.SLIDER)
            .configKey(configKey)
            .sliderRange(min, max)
            .formatString(format)
            .valueChangeAction(valueChangeAction)
            .sliderType(sliderType)
            .hasColorControl(hasColorControl)
            .build();
    }

    // Header
    public static VCEntry header(String name, String description) {
        return new Builder(name, description, EntryType.HEADER)
            .build();
    }    

    // Keybind button
    public static VCEntry keybind(String name, String description, String configKey, boolean defaultValue, Runnable refreshAction) {
        return new Builder(name, description, EntryType.KEYBIND)
            .configKey(configKey)
            .defaultValue(defaultValue)
            .refreshAction(refreshAction)
            .build();
    }

    // Expandable list
    public static VCEntry expandable(String name, String description, List<VCEntry> subEntries, List<Text> tooltipText) {
        return new Builder(name, description, EntryType.EXPANDABLE)
            .subEntries(subEntries)
            .tooltipText(tooltipText)
            .build();
    }

    // Expandable with toggle
    public static VCEntry toggleExpandable(String name, String description, List<VCEntry> subEntries, List<Text> tooltipText, String configKey, boolean defaultValue, Runnable refreshAction) {
        return new Builder(name, description, EntryType.EXPANDABLE)
            .subEntries(subEntries)
            .tooltipText(tooltipText)
            .configKey(configKey)
            .defaultValue(defaultValue)
            .refreshAction(refreshAction)
            .build();
    }

    // Opens a separate screen
    public static VCEntry redirect(String name, String description, String buttonText, Runnable mainAction) {
        return new Builder(name, description, EntryType.BUTTON)
            .buttonText(buttonText)
            .action(mainAction)
            .build();
    }

    // Slider + colorsquare
    public static VCEntry sliderColor(String name, String description, String configKey, float min, float max, String format, Consumer<Float> valueChangeAction) {
        return new Builder(name, description, EntryType.SLIDER)
            .configKey(configKey)
            .sliderRange(min, max)
            .formatString(format)
            .valueChangeAction(valueChangeAction)
            .hasColorControl(true)
            .build();
    }

    public boolean getToggleState() {
        if (configKey == null) {
            return false;
        }
        switch (type) {
            case TOGGLE, EXPANDABLE, TOGGLE_WITH_SLIDER, SIMPLE_BUTTON:
                return getFishyConfigToggleState();
            case ITEM_CONFIG_TOGGLE:
                return getItemConfigToggleState();
            case BLACKLIST_TOGGLE:
                return getBlacklistToggleState();
            default:
                return false;
        }
    }

    public void toggleSetting() {
        if (configKey != null) {
            switch (type) {
                case TOGGLE, EXPANDABLE, TOGGLE_WITH_SLIDER, SIMPLE_BUTTON:
                    toggleFishyConfig();
                    break;
                case ITEM_CONFIG_TOGGLE:
                    toggleItemConfig();
                    break;
                case BLACKLIST_TOGGLE:
                    toggleBlacklist();
                    break;
                default:
                    break;
            }
        }
        if (refreshAction != null) {
            refreshAction.run();
        }
    }    

    private boolean getFishyConfigToggleState() {
        return FishyConfig.getState(configKey, defaultValue);
    }

    private boolean getItemConfigToggleState() {
        return ItemConfig.getState(configKey, defaultValue);
    }

    private boolean getBlacklistToggleState() {
        for (BlacklistManager.GuiBlacklistEntry entry : BlacklistManager.getMergedBlacklist()) {
            if (entry.identifiers != null && !entry.identifiers.isEmpty() && entry.identifiers.contains(configKey)) {
                return entry.isEnabled();
            }
        }
        return defaultValue;
    }
    
    public boolean getSimpleButtonState() {
        if (type == EntryType.SIMPLE_BUTTON && simpleButtonConfigKey != null) {
            return FishyConfig.getState(simpleButtonConfigKey, simpleButtonDefault);
        }
        return false;
    }

    private void toggleFishyConfig() {
        FishyConfig.toggle(configKey, defaultValue);
    }

    private void toggleItemConfig() {
        ItemConfig.toggle(configKey, defaultValue);
    }

    private void toggleBlacklist() {
        for (BlacklistManager.GuiBlacklistEntry entry : BlacklistManager.getMergedBlacklist()) {
            if (entry.identifiers != null && !entry.identifiers.isEmpty() && entry.identifiers.contains(configKey)) {
                entry.setEnabled(!entry.isEnabled());
                BlacklistManager.updateBlacklistEntry(entry.identifiers.get(0), entry.isEnabled());
                break;
            }
        }
    }
    
    public void toggleSimpleButton() {
        if (type == EntryType.SIMPLE_BUTTON && simpleButtonConfigKey != null) {
            FishyConfig.toggle(simpleButtonConfigKey, simpleButtonDefault);
        }
        
        if (refreshAction != null) {
            refreshAction.run();
        }
    }
    
    // Helper methods for expandable entries
    public List<VCEntry> getSubEntries() {
        return subEntries != null ? subEntries : new ArrayList<>();
    }
    
    public boolean hasSubEntries() {
        return subEntries != null && !subEntries.isEmpty();
    }
    
    public boolean hasToggle() {
        return type == EntryType.EXPANDABLE && configKey != null;
    }
    
    // Helper methods for HUD entries
    public boolean hasHudControls() {
        return hudElementName != null;
    }
    
    public String getHudElementName() {
        return hudElementName;
    }
    
    public boolean hasColorControl() {
        return hasColorControl;
    }

    public boolean hasAdd() {
        return hasAdd;
    }

    // Keybind entry methods
    public boolean isListening() {
        return isListening;
    }
    
    public void setListening(boolean listening) {
        this.isListening = listening;
    }
    
    public String getKeybindValue() {
        if (type == EntryType.KEYBIND && configKey != null) {
            return FishyConfig.getKeyString(configKey);
        }
        return "NONE";
    }
    
    public void setKeybindValue(String key) {
        if (type == EntryType.KEYBIND && configKey != null && key != null) {
            FishyConfig.setKeyString(configKey, key);
        }
    }
    
    // Slider entry methods
    public float getSliderValue() {
        if (type == EntryType.SLIDER && configKey != null) {
            if (Key.MOD_UI_SCALE.equals(configKey)) {
                return uiScaleValue();
            } else if (Key.THEME_MODE.equals(configKey)) {
                return themeValue();
            } else if (Key.CUSTOM_PARTICLE_COLOR_INDEX.equals(configKey)) {
                return presetIndexValue();
            } else {
                return standardValue();
            }
        } else if (type == EntryType.TOGGLE_WITH_SLIDER && buttonText != null) {
            return toggleSliderValue();
        }
        return 0.0f;
    }

    private float uiScaleValue() {
        return FishyConfig.getFloat(Key.MOD_UI_SCALE, 0.4265625f);
    }

    private float themeValue() {
        String currentTheme = FishyMode.getTheme();
        String[] themes = {"default", "purple", "blue", "white", "green"};
        for (int i = 0; i < themes.length; i++) {
            if (themes[i].equalsIgnoreCase(currentTheme)) {
                return i;
            }
        }
        return 0.0f;
    }

    private float presetIndexValue() {
        return FishyConfig.getCustomParticleColorIndex();
    }

    private float standardValue() {
        return FishyConfig.getFloat(configKey, (minValue + maxValue) / 2.0f);
    }

    private float toggleSliderValue() {
        return FishyConfig.getFloat(buttonText, (minValue + maxValue) / 2.0f);
    }
    
    public void setSliderValue(float value) {
        if (type == EntryType.SLIDER && configKey != null) {
            float clampedValue = Math.clamp(value, minValue, maxValue);
            FishyConfig.setFloat(configKey, clampedValue);
            if (valueChangeAction != null) {
                valueChangeAction.accept(clampedValue);
            }
        } else if (type == EntryType.TOGGLE_WITH_SLIDER && buttonText != null) {
            // For toggle with slider, the slider config key is stored in buttonText
            float clampedValue = Math.clamp(value, minValue, maxValue);
            FishyConfig.setFloat(buttonText, clampedValue);
            if (valueChangeAction != null) {
                valueChangeAction.accept(clampedValue);
            }
        }
    }
    
    public float getMinValue() {
        return minValue;
    }
    
    public float getMaxValue() {
        return maxValue;
    }
    
    public String getFormatString() {
        return formatString;
    }
    
    public SliderType getSliderType() {
        return sliderType;
    }
}
