package me.valkeea.fishyaddons.feature.config;

import me.valkeea.fishyaddons.feature.skyblock.CocoonAlert;
import me.valkeea.fishyaddons.feature.skyblock.EqDetector;
import me.valkeea.fishyaddons.feature.skyblock.timer.EffectTimers;
import me.valkeea.fishyaddons.hud.elements.interactive.ProfitDisplay;
import me.valkeea.fishyaddons.hud.ui.EqDisplay;
import me.valkeea.fishyaddons.tracker.PriceUtil;
import me.valkeea.fishyaddons.tracker.SkillTracker;
import me.valkeea.fishyaddons.tracker.monitoring.ActivityMonitor;
import me.valkeea.fishyaddons.tracker.profit.ValuableMobs;
import me.valkeea.fishyaddons.vconfig.annotation.*;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.DoubleKey;
import me.valkeea.fishyaddons.vconfig.api.IntKey;
import me.valkeea.fishyaddons.vconfig.api.StringKey;
import me.valkeea.fishyaddons.vconfig.core.UICategory;

@VCModule(UICategory.SKYBLOCK)
public class SkyblockConfig {

    @VCInit
    public static void init() {
        SkillTracker.init();        
        CocoonAlert.init();        
        EqDetector.init();
        EffectTimers.getInstance().init();
    }

    @UIToggle(
        key = BooleanKey.EQ_DISPLAY,
        name = "*Equipment* Display",
        description = {
            "Renders a secondary armor display with your current equipment.",
            "Data updates when /eq is opened."
        }
    )
    private static boolean eqDisplay;

    @VCListener(BooleanKey.EQ_DISPLAY)
    private static void onEqDisplayChange() {
        EqDisplay.reset();
    }

    @UIToggle(
        key = BooleanKey.HUD_HEALTH_ENABLED,
        name = "*Mob Health* Bar",
        description = "Hud element to display the current health of relevant mobs (mostly fishing)."
    )
    private static boolean mobHealthBar;

    @VCListener(BooleanKey.HUD_HEALTH_ENABLED)
    private static void onMobHealthBarChange() {
        ValuableMobs.refresh();
    }

    private static final String PET = "*Pet* Display";

    @UIContainer(
        key = BooleanKey.HUD_PET_ENABLED,
        name = PET,
        description = {
            "Active pet display, optionally with xp.",
            "Enable tab widget for accurate data §7/tablist."
        },
        tooltip = {
            "Pet Display:",
            "Optional XP",
            "Edit color"
        }
    )
    private static final boolean PET_SETTINGS = false;

    @UIToggle(
        key = BooleanKey.PET_INCLUDEXP,
        name = "Include Pet XP",
        description = "Toggle to include pet XP. You can toggle overflow in /tablist.",
        parent = PET
    )
    @UIColorPicker(key = IntKey.HUD_PETXP_COLOR)
    private static boolean petIncludeXp;
    
    private static final String PROFIT = "*Profit* Tracker §7(WIP)";
    @UIContainer(
        key = BooleanKey.HUD_PROFIT_ENABLED,
        name = PROFIT,
        description = "Configure settings for the Profit Tracker. Use /fp for available commands.",
        tooltip = {
            "Profit Tracker:",
            "Book drop message",
            "Value filter",
            "Display options",
            "Commands: /fp"
        }
    )
    private static final boolean PROFIT_SETTINGS = false;

    @VCListener({
        BooleanKey.HUD_PROFIT_ENABLED, BooleanKey.PER_ITEM,
        BooleanKey.TRACK_SACK, BooleanKey.HUD_COLLECTION_ENABLED}
    )
    private static void onProfitTrackerChange() {
        PriceUtil.refresh();
    }

    @UIToggle(
        key = BooleanKey.PER_ITEM,
        name = "Price per Item",
        description = "Adds additional price data to the display.",
        parent = PROFIT
    )
    private static boolean perItem;

    @UIToggle(
        key = BooleanKey.TRACK_SACK,
        name = "Track Sack Drops",
        description = "Requires sack notifications on to function.",
        parent = PROFIT
    )
    private static boolean trackSack;

    @UISlider(
        key = DoubleKey.FILTER_MIN_VALUE,
        name = "Filter by Minimum Value",
        description = "Hides items under this value, set to 0 to disable.",
        min = 0.0, max = 50000.0, format = "%.0f",
        parent = PROFIT
    )
    private static double filterMinValue;

    @VCListener(doubles = DoubleKey.FILTER_MIN_VALUE)
    private static void onFilterMinValueChange() {
        ProfitDisplay.refreshDisplay();
    }

    @UIToggle(
        key = BooleanKey.TRACKER_NOTIS,
        name = "Tracker Drop Notifications",
        description = "Adds a drop message to tracked items without one.",
        parent = PROFIT
    )
    private static boolean trackerNotis;

    @UIToggle(
        key = BooleanKey.HUD_COLLECTION_ENABLED,
        name = "*Collection* Progress Display §7(§8Requires sack messages§7)",
        description = {
            "Works best if you open /collection <item> before a session.",
            "Or, toggle the rankings view (updates slow, only used with no session gains),"
        }
    )
    @UIHudRedirect
    private static boolean collectionDisplay;

    private static final String TRACKERS = "Trackers (Statistics)";

    @UIContainer(
        name = TRACKERS,
        description = "Trackers for skill xp, diana and slayer.",
        tooltip = {
            "Trackers (Stats):",
            "Diana burrows, bph,",
            "chim rate etc.",
            "Slayer XP per hour",
            "Skill XP per hour",
            "(with catch rate)"
        }
    )
    private static final boolean TRACKER_SETTINGS = false;

    @UIToggle(
        key = BooleanKey.TRACK_DIANA,
        name = "Track various *Diana* stats",
        description = "Burrows, burrows / inq, bph, chim rate etc. Use /fa diana for details.",
        parent = TRACKERS,
        subcategory = "diana tracker"
    )
    private static boolean trackDiana;

    @VCListener({BooleanKey.TRACK_DIANA, BooleanKey.TRACK_SLAYER})
    private static void onTrackDianaChange() {
        ActivityMonitor.refresh();
    }

    @UIToggle(
        key = BooleanKey.TRACK_SLAYER,
        name = "Track *Slayer* XP per hour §7(§8/fa slayer§7)",
        description = "Adds bosses and XP per hour to the slayer completion messages.",
        parent = TRACKERS,
        subcategory = "slayer xp tracker"
    )
    private static boolean trackSlayer;

    @UIToggle(
        key = BooleanKey.HUD_SKILL_XP,
        name = "*Skill Xp* per hour §7(§8Includes catches / hour§7)",
        description = "Displays your skill xp gain rate. (Needs tabwidget if skill isn't maxed)",
        parent = TRACKERS,
        subcategory = "skill xp tracker"
    )
    private static boolean skillXp;

    private static final String TIMERS = "Timers";

    @UIContainer(
        name = TIMERS,
        description = "Configure settings for various timers.",
        tooltip = {"Timers:","Moby", "Flask", "Gummy", "Truffle", "Century Cake",}
    )
    private static final boolean TIMER_SETTINGS = false;

    @UIToggle(
        key = BooleanKey.CAKE_NOTI,
        name = "*Century Cake* Notifications",
        description = "Notifies you when a century cake is about to expire or expires.",
        parent = TIMERS,
        subcategory = "century cake timer"
    )
    private static boolean cakeNoti;

    @UIToggle(
        key = BooleanKey.HUD_CENTURY_CAKE_ENABLED,
        name = "*Century Cake* Display",
        description = "Enables a HUD timer for century cakes.",
        parent = TIMERS,
        subcategory = "century cake timer"
    )
    private static boolean hudCenturyCakeEnabled;

    @UIToggle(
        key = BooleanKey.HUD_EFFECTS_ENABLED,
        name = "Active *Effects* Display",
        description = "Flask, moby, gummy and truffle duration / cooldown display.",
        parent = TIMERS,
        subcategory = "temporary effects"
    )
    @UIHudRedirect
    private static boolean tempEffects;

    private static final String ALARMS = "Alarms";

    @UIContainer(
        name = ALARMS,
        description = "Cocoon alert and Moonglade minigame alarm."
    )
    private static final boolean ALARM_SETTINGS = false;

    @UIToggle(
        key = BooleanKey.TRACK_COCOON,
        name = "Bloodshot / The Primordial *Cocoon* Alert",
        description = {
            "Attempts to detect cocoons if nearby.",
            "Default chat alert, toggleable title + audio."
        },
        parent = ALARMS,
        subcategory = "cocoon alert"
    )
    @UIExtraToggle(key = BooleanKey.ALERT_COCOON, buttonText = "Title")
    private static boolean cocoonAlert;
    
    @VCListener(BooleanKey.TRACK_COCOON)
    private static void onCocoonAlertChange() {
        CocoonAlert.refresh();
    }

    @UIToggle(
        key = BooleanKey.BEACON_ALARM,
        name = "Alarm",
        description = {
            "Alerts you when the *Moonglade* minigame is ready."
        },
        parent = ALARMS,
        subcategory = "moonglade minigame alarm"
    )
    private static boolean moongladeAlarm;

    @UIToggle(
        key = BooleanKey.HUD_TIMER_ENABLED,
        name = "Timer Display",
        description = "Adds a HUD timer for the *Moonglade* minigame cooldown.",
        parent = ALARMS,
        subcategory = "moonglade minigame alarm"
    )
    @UIHudRedirect
    private static boolean hudTimerEnabled;

    @UIToggle(
        key = BooleanKey.CLEAN_HYPE,
        name = "Clean *Hyperion*",
        description = {
            "Removes explosion particles and any sound effects caused by",
            "wither blade abilities."
        }
    )
    private static boolean cleanHype;

    @UIToggle(
        key = BooleanKey.ACCEPT_NPC,
        name = "Accept *NPC* Dialogue",
        description = {
            "Automatically chooses the confirm option to progress NPC dialogues.",
            "Affects most common dialogue types and Trevor the Trapper."
        }
    )
    private static boolean acceptNpc;

    @UIKeybind(
        key = StringKey.KEY_HIDE_GUI,
        name = "Hide and *Block buttons* in Skyblock GUIs",
        description = {
            "Set the key to hide Skyblock gui icons. Tap the key to add/remove",
            "hovered slots, peek and bypass block by holding shift."
        }
    )
    private static String keyHideGui;    

    @UIToggle(
        key = BooleanKey.SCALE_CRIT,
        name = "Scale Critical Hit Particles (*Invisibug*)",
        description = "Increase the size of critical hit particles in Galatea."
    )
    @UISlider(
        key = DoubleKey.DMG_SCALE,
        min = 0.0, max = 1.0, format = "%.1fx"
    )
    private static boolean scaleCrits;

    private SkyblockConfig() {}
}
