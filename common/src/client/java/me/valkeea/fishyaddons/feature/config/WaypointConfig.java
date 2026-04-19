package me.valkeea.fishyaddons.feature.config;

import java.util.ArrayList;
import java.util.List;

import me.valkeea.fishyaddons.feature.waypoints.WaypointChains;
import me.valkeea.fishyaddons.vconfig.annotation.UIColorPicker;
import me.valkeea.fishyaddons.vconfig.annotation.UIContainer;
import me.valkeea.fishyaddons.vconfig.annotation.UIDropdown;
import me.valkeea.fishyaddons.vconfig.annotation.UISlider;
import me.valkeea.fishyaddons.vconfig.annotation.UIToggle;
import me.valkeea.fishyaddons.vconfig.annotation.VCListener;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.IntKey;
import me.valkeea.fishyaddons.vconfig.core.UICategory;
import me.valkeea.fishyaddons.vconfig.ui.widget.dropdown.item.ToggleMenuItem;
import me.valkeea.fishyaddons.vconfig.ui.widget.dropdown.item.WaypointChainItem;

@VCModule(UICategory.WAYPOINTS)
public class WaypointConfig {

    private static final String BEACON = "Highlight *Coordinates* from Chat";

    @UIContainer(
        key = BooleanKey.RENDER_COORDS,
        name = BEACON,
        description = {
            "Render a beacon-style highlight at coordinates shared in chat.",
            "Use /fa coords <title> to render/send additional text."
        },
        tooltip = {
            "Coordinate Beacons:",
            "Duration and color",
            "Hide when close",
            "[Hide] and [Redraw]"
        }
    )
    private static final boolean BEACON_SETTINGS = false;

    @UISlider(
        altKey = IntKey.RENDER_COORD_MS,
        name = "Duration and Color",
        description = "Beacons will be rendered for this duration with the configured color.",
        parent = BEACON,
        min = 1.0, max = 120.0, format = "%.0f seconds"
    )
    @UIColorPicker(key = IntKey.RENDER_COORD_COLOR)
    private static int renderCoordColor;

    @UIToggle(
        key = BooleanKey.RENDER_COORD_HIDE_CLOSE,
        name = "Hide When Close",
        description = "Hides the beacon when you are within 5 blocks of it.",
        parent = BEACON
    )
    private static boolean hideClose;

    @UIToggle(
        key = BooleanKey.CHAT_FILTER_COORDS_ENABLED,
        name = "Enhanced Messages",
        description = "Adds clickable [Hide] and [Redraw] buttons to coordinate messages.",
        parent = BEACON
    )
    private static boolean enhancedCoords; 

    private static final String MAIN = "Waypoint *Chains*";
    
    @UIContainer(
        key = BooleanKey.FWP_ENABLED,
        name = MAIN,
        description = {
            "Render numbered waypoint sequences based on current area.",
            "Includes preset chains like relic locations and supports user-defined chains."
        },
        tooltip = {
            "§lPreset Chains:", "Relic locations", "§lUser chains", "/fwp to config"}
    )
    private static final boolean WAYPOINT_CHAINS = false;

    @UIDropdown(
        name = "Preset Chains",
        description = "Left-click to toggle, right-click to reset completion status.",
        parent = MAIN,
        provider = "getChains"
    )
    private static boolean fwpPresets;

    @UISlider(
        altKey = IntKey.FWP_DISTANCE,
        name = "Completion Distance",
        parent = MAIN,
        description = "Distance in blocks to automatically mark waypoints as completed.",
        min = 1, max = 10, format = "%.0f blocks"
    )
    private static int fwpDistance;

    @UIToggle(
        key = BooleanKey.FWP_INFO,
        name = "Announce completion times",
        parent = MAIN,
        description = "Sends the completion time in chat upon finishing a chain."
    )
    private static boolean fwpInfo;

    @VCListener(
        value = {BooleanKey.FWP_ENABLED, BooleanKey.FWP_INFO},
        ints = IntKey.FWP_DISTANCE
    )
    public static void refreshChains() {
        WaypointChains.refresh();
    }

    protected static List<ToggleMenuItem> getChains() {
        List<ToggleMenuItem> items = new ArrayList<>();
        items.add(new WaypointChainItem(
            BooleanKey.FWP_RELICS, 
            "Relic Locations (Den)",
            "Toggle relic waypoints for Dragon's Den. Right-click to reset completion."
        ));
        return items;
    }

    private WaypointConfig() {}    
}
