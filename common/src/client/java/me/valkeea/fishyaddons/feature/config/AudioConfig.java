package me.valkeea.fishyaddons.feature.config;

import java.util.ArrayList;
import java.util.List;

import me.valkeea.fishyaddons.vconfig.annotation.UIContainer;
import me.valkeea.fishyaddons.vconfig.annotation.UIDropdown;
import me.valkeea.fishyaddons.vconfig.annotation.UISearch;
import me.valkeea.fishyaddons.vconfig.annotation.UISlider;
import me.valkeea.fishyaddons.vconfig.annotation.UIToggle;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.DoubleKey;
import me.valkeea.fishyaddons.vconfig.api.StringKey;
import me.valkeea.fishyaddons.vconfig.core.UICategory;
import me.valkeea.fishyaddons.vconfig.ui.widget.dropdown.item.SoundSearchItem;
import me.valkeea.fishyaddons.vconfig.ui.widget.dropdown.item.ToggleItem;
import me.valkeea.fishyaddons.vconfig.ui.widget.dropdown.item.ToggleMenuItem;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

@VCModule(UICategory.AUDIO)
public class AudioConfig {

    private static final String MAIN = "Fishing *Reel* / Fish Hooked Alert";

    @UIContainer(
        name = MAIN,
        description = {
            "Change the volume and sound of the fishing catch alarm.",
            "Requires Skyblock music to be off! §8/togglemusic"
        },
        tooltip = {
            "Catch Sound:",
            "Override default",
            "Adjust volume"
        }
    )
    private static final boolean REEL = false;

    @UIToggle(
        key = BooleanKey.REEL_TRUEVOL,
        name = "Custom Fishing Catch Volume §7(§8Set to 0 to disable§7)",
        description = "Optional toggle to ignore Minecraft volume settings.",
        parent = MAIN
    )
    @UISlider(key = DoubleKey.REEL_OVERRIDE, min = 0.0, max = 2.0, format = "%.0f%%")
    private static boolean reelTrueVolume;

    @UISearch(
        key = StringKey.REEL_OVERRIDE_ID,
        name = "Change Hypixel default alert",
        description = "Replace with custom sound. Double-click field to reset.",
        provider = "getReelItems",
        parent = MAIN
    )
    private static String reelOverride;

    public static List<ToggleMenuItem> getReelItems() {

        var ids = Registries.SOUND_EVENT.stream()
            .map(Registries.SOUND_EVENT::getId)
            .filter(java.util.Objects::nonNull)
            .map(Identifier::toString)
            .sorted()
            .toList();

        List<ToggleMenuItem> items = new ArrayList<>();

        for (String id : ids) {
            items.add(new SoundSearchItem(
                StringKey.REEL_OVERRIDE_ID,
                DoubleKey.REEL_OVERRIDE,
                BooleanKey.REEL_TRUEVOL,
                id));
        }
        return items;
    }

    @UIToggle(
        key = BooleanKey.REEL_NORANDOM,
        name = "Prevent soundset randomization",
        description = "Enable to always use the first sound variant in the default soundset.",
        parent = MAIN
    )
    private static boolean reelNoRandom;  
    
    private static final String FERO = "*Ferocity* Sound Customization";
    @UIContainer(
        name = FERO,
        description = "Change the volume and sound of ferocity proc feedback.",
        tooltip = {
            "Ferocity Sound:",
            "Override default",
            "Adjust volume"
        }
    )
    private static final boolean FERO_SETTINGS = false;

    @UIToggle(
        key = BooleanKey.FERO_TRUEVOL,
        name = "Ferocity Volume §7(§8Set to 0 to disable§7)",
        description = "Optional toggle to ignore Minecraft volume settings.",
        parent = FERO
    )
    @UISlider(key = DoubleKey.FERO_OVERRIDE, min = 0.0, max = 2.0, format = "%.0f%%")
    private static boolean feroTrueVolume;

    @UISearch(
        key = StringKey.FERO_OVERRIDE_ID,
        name = "Change Ferocity Sound",
        description = "Replace with custom sound. Double-click field to reset.",
        provider = "getFeroItems",
        parent = FERO
    )
    private static String feroOverride;

    public static List<ToggleMenuItem> getFeroItems() {
        var ids = Registries.SOUND_EVENT.stream()
            .map(Registries.SOUND_EVENT::getId)
            .filter(java.util.Objects::nonNull)
            .map(Identifier::toString)
            .sorted()
            .toList();
            
        List<ToggleMenuItem> items = new ArrayList<>();
        for (String id : ids) {
            items.add(new SoundSearchItem(
                StringKey.FERO_OVERRIDE_ID,
                DoubleKey.FERO_OVERRIDE,
                BooleanKey.FERO_TRUEVOL,
                id
            ));
        }
        return items;
    }
    
    @UIDropdown(
        name = "*Mute* List",
        description = "Configure a list of sounds to mute in Skyblock. (Thunder, phantom, runes, endermen)",
        provider = "getMuteListItems",
        buttonText = "Muted"
    )
    private static String muteList;

    public static List<ToggleMenuItem> getMuteListItems() {
        List<ToggleMenuItem> items = new ArrayList<>();
        items.add(createFrom(BooleanKey.MUTE_PHANTOM));
        items.add(createFrom(BooleanKey.MUTE_RUNE));
        items.add(createFrom(BooleanKey.MUTE_THUNDER));
        items.add(createFrom(BooleanKey.MUTE_EMAN));
        return items;
    }

    private static ToggleItem createFrom(BooleanKey muteListKey) {
        var base = muteListKey.getString().replace("mute", "");
        return new ToggleItem(
            muteListKey,
            base.contains("men") ? base : base + "s"
        );
    }

    private AudioConfig() {}
}
