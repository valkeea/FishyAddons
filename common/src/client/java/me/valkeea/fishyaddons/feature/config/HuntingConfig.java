package me.valkeea.fishyaddons.feature.config;

import me.valkeea.fishyaddons.vconfig.annotation.UIColorPicker;
import me.valkeea.fishyaddons.vconfig.annotation.UIContainer;
import me.valkeea.fishyaddons.vconfig.annotation.UISlider;
import me.valkeea.fishyaddons.vconfig.annotation.UIToggle;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.IntKey;
import me.valkeea.fishyaddons.vconfig.core.UICategory;

@VCModule(UICategory.HUNTING)
public class HuntingConfig {

    private static final String SAFARI = "Track *Safari* critters";

    @UIContainer(
        key = BooleanKey.TRACK_CRITTERS,
        name = SAFARI,
        description = "Track captured critters and shards."
    )
    private static boolean trackSafari;

    @UISlider(
        altKey = IntKey.CRITTER_MODE,
        name = "Display Mode",
        min = 0, max = 2,
        labels = {"Completion", "Raw Counts", "Shard Amounts"},
        description = "Display either unique critter completion, raw capture counts, or shard gains.",
        parent = SAFARI
    )
    private static int critterMode;

    @UIToggle(
        key = BooleanKey.SAFARI_OWN_ZONE_ONLY,
        name = "Own Zone Only",
        description = "Only display stats in your active safari zone.",
        parent = SAFARI
    )
    private static boolean safariOwnZoneOnly;

    @UIToggle(
        key = BooleanKey.SAFARI_ANNOUNCE,
        name = "Announce Completion",
        description = "Announce in chat once all unique critters have been captured.",
        parent = SAFARI
    )
    private static boolean safariAnnounce;

    private static final String SAFARI_HIGHLIGHT = "Safari Highlight";

    @UIContainer(
        name = SAFARI_HIGHLIGHT,
        description = {
            "Highlight floor drops and critters in the safari zone.",
            "Disabled while vanilla hitboxes are enabled."
        }
    )
    private static boolean safariHighlight;

    @UIColorPicker(
        key = IntKey.CRITTER_COLOR,
        name = "Highlight Critters",
        description = "Draw a box for critters and related entities. (Set a color to enable)",
        parent = SAFARI_HIGHLIGHT        
    )
    private static int critterHighlight;

    @UIColorPicker(
        key = IntKey.FLOOR_DROP_COLOR,
        name = "Highlight Floor Drops",
        description = "Draw a box around blocks containing floor drops. (Set a color to enable)",
        parent = SAFARI_HIGHLIGHT        
    )
    private static int floorDropHighlight;

    private HuntingConfig() {}
}
