package me.valkeea.fishyaddons.feature.config;

import java.util.ArrayList;
import java.util.List;

import me.valkeea.fishyaddons.feature.filter.FilterConfig;
import me.valkeea.fishyaddons.feature.skyblock.WeatherTracker;
import me.valkeea.fishyaddons.tracker.fishing.Sc;
import me.valkeea.fishyaddons.tracker.fishing.ScData;
import me.valkeea.fishyaddons.tracker.monitoring.ActivityMonitor;
import me.valkeea.fishyaddons.vconfig.annotation.UIContainer;
import me.valkeea.fishyaddons.vconfig.annotation.UIDropdown;
import me.valkeea.fishyaddons.vconfig.annotation.UIExtraToggle;
import me.valkeea.fishyaddons.vconfig.annotation.UISlider;
import me.valkeea.fishyaddons.vconfig.annotation.UIToggle;
import me.valkeea.fishyaddons.vconfig.annotation.VCListener;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.DoubleKey;
import me.valkeea.fishyaddons.vconfig.core.UICategory;
import me.valkeea.fishyaddons.vconfig.ui.widget.dropdown.item.ScItem;
import me.valkeea.fishyaddons.vconfig.ui.widget.dropdown.item.ToggleMenuItem;

@VCModule(UICategory.FISHING)
public class FishingConfig {

    @UIToggle(
        key = BooleanKey.RAIN_NOTI,
        name = "*Rain* Warning",
        description = {
            "Tracks the worlds weather state and notifies if rain stops.",
            "Due to Hypixel quirks this will not work reliably in the Park cave."
        }
    )
    private static boolean rainNoti;

    @VCListener(BooleanKey.RAIN_NOTI)
    private static void onRainChange() {
        WeatherTracker.track();
    }

    private static final String HSPT = "Fishing *Hotspots*";

    @UIContainer(
        name = HSPT,
        description = "Hide and track fishing hotspots.",
        tooltip = {
            "Fishing Hotspots:",
            "Manage visibility",
            "Configure alerts"
        }
    )
    private static final boolean HSPT_SETTINGS = false;

    @UIToggle(
        key = BooleanKey.HOTSPOT_HIDE,
        name = "Hide hotspot holograms",
        description = "Hides all fishing hotspots starting from this distance.",
        parent = HSPT
    )
    @UISlider(
        key = DoubleKey.HOTSPOT_DISTANCE,
        min = 0.0, max = 20.0,
        format = "%.0f blocks"
    )
    private static boolean hideHotspot;

    @UIToggle(
        key = BooleanKey.HOTSPOT_TRACK,
        name = "Hotspot Expiration Warning",
        description = "Alerts you on hotspot expiration if nearby.",
        parent = HSPT
    )
    private static boolean trackHotspot;

    @UIToggle(
        key = BooleanKey.HOTSPOT_COORDS,
        name = "Hotspot Spawn Tracker",
        description = "Alerts you (or swap to party msg) when a new hotspot is visible.",
        parent = HSPT
    )
    @UIExtraToggle(key = BooleanKey.HOTSPOT_ANNOUNCE, buttonText = "Party")
    private static boolean hotspotSpawn;

    @UIToggle(
        key = BooleanKey.TRACK_SCS,
        name = "*Track* Fishing Data §7(§8Required for 'Catch Graph' and 'RNG Info'§7)",
        description = {
            "Allows the mod to track sc catches. Optionally count",
            "double hooks as multiple catches (this will skew true catchrate)."
        }
    )
    @UIExtraToggle(key = BooleanKey.TRACK_SCS_WITH_DH, buttonText = "COUNT DH")
    private static boolean fishingData;

    @VCListener({BooleanKey.TRACK_SCS, BooleanKey.TRACK_SCS_WITH_DH})
    private static void onDataChange() {
        FilterConfig.refreshScRules();
        ActivityMonitor.refresh();
    }

    @UIDropdown(
        key = BooleanKey.HUD_CATCH_GRAPH_ENABLED,
        name = "Catch *Graph*",
        provider = "catchGraphSettings",
        description = {
            "Displays a graph of catch statistics over time while fishing.",
            "Rate: successful catches / attempts. Graph and Mean: sc's between catches."
        }
    )
    private static boolean catchGraph;

    static List<ToggleMenuItem> catchGraphSettings() {
        List<ToggleMenuItem> items = new ArrayList<>();
        List<String> trackedCreatures = Sc.getTrackedCreatures();
        
        for (String creatureKey : trackedCreatures) {
            items.add(new ScItem(creatureKey));
        }
        
        return items;
    }    

    @VCListener({BooleanKey.HUD_CATCH_GRAPH_ENABLED})
    private static void onCatchGraphChange() {
        ScData.refresh();
    }

    @UIToggle(
        key = BooleanKey.SC_SINCE,
        name = "Fishing *RNG* Info §7(§8Sc's and vial§7)",
        description = {
            "Counts sea creatures between relevant catches and announces on catch.",
            "This data can be checked anytime with /fa sc since."
        }
    )
    private static boolean scSince;

    @VCListener({BooleanKey.SC_SINCE})
    private static void onScSinceChange() {
        ActivityMonitor.refresh();
    }

    private FishingConfig() {}
}
