package me.valkeea.fishyaddons.vconfig.api;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import me.valkeea.fishyaddons.vconfig.config.ConfigFile;

public enum BooleanKey implements ConfigKey<Boolean> {

    // Visual
    TRANS_LAVA("fishyTransLava"),
    FIRE_OVERLAY("fishyFireOverlay"),
    FISHY_LAVA("fishyLava"),
    FISHY_WATER("fishyWater"),
    FIRE_ANI("fireAni"),
    DEATH_ANI("deathAni"),
    SCALE_CRIT("scaleCritParticles"),
    XP_COLOR_ON("xpColorEnabled"),
    XP_OUTLINE("xpOutline"),
    HD_FONT("hdFont"),
    FISHY_GUI("fishyGui"),
    SKIP_F5("skipPerspective"),
    CLEAN_HYPE("cleanHype"),
    HELD_ITEM_TRANSFORMS("heldItemTransforms"),
    
    // Audio
    MUTE_PHANTOM("mutePhantom"),
    MUTE_RUNE("muteRune"),
    MUTE_THUNDER("muteThunder"),
    MUTE_EMAN("muteEndermen"),
    BEACON_ALARM("beaconAlarm"),
    REEL_NORANDOM("reelNoRandom"),
    REEL_TRUEVOL("reelTrueVolume"),
    FERO_TRUEVOL("feroTrueVolume"),
    
    // HUD
    HUD_METRICS_ENABLED("pingHud"),
    METRICS_SHOW_PING("pingHudShowPing", true),
    METRICS_SHOW_TPS("pingHudShowTps", true),
    METRICS_SHOW_FPS("pingHudShowFps", true),
    HUD_TIMER_ENABLED("timerHud", true),
    HUD_PET_ENABLED("petHud"),
    PET_INCLUDEXP("petXpCheck", true),
    HUD_PROFIT_ENABLED("profitTrackerHud"),
    HUD_CENTURY_CAKE_ENABLED("centuryCakeHud"),
    HUD_CATCH_GRAPH_ENABLED("scGraphHud"),
    HUD_SKILL_XP("skillXpHud"),
    HUD_HEALTH_ENABLED("healthHud"),
    HUD_EFFECTS_ENABLED("tempEffectsHud"),
    HUD_COLLECTION_ENABLED("collectionHud"),
    HUD_TITLE_ENABLED("titleHud", true),
    
    // Waypoints
    FWP_RELICS("waypointChainsShowRelics"),
    FWP_ENABLED("waypointChainsEnabled"),
    FWP_INFO("waypointChainsInfo", true),
    RENDER_COORDS("renderCoords", true),
    RENDER_COORD_HIDE_CLOSE("renderCoordsHideClose", true),
    
    // Tracking
    TRACK_SACK("trackSack", true),
    PER_ITEM("pricePerItem", true),
    TRACKER_NOTIS("trackerNotis"),
    TRACK_SCS("trackScs"),
    TRACK_SCS_WITH_DH("trackScsWithDh"),
    SC_SINCE("scSince"),
    TRACK_DIANA("trackDiana"),
    TRACK_COCOON("trackCocoon"),
    ALERT_COCOON("alertCocoon"),
    TRACK_SLAYER("trackSlayer"),
    
    // Hotspots
    HOTSPOT_TRACK("trackHotspot"),
    HOTSPOT_ANNOUNCE("announceHotspot"),
    HOTSPOT_COORDS("hotspotCoords"),
    HOTSPOT_HIDE("hideHotspot"),
    
    // Chat
    COPY_CHAT("copyChat", true),
    COPY_NOTI("ccNoti", true),
    CHAT_FILTER_ENABLED("chatFilterOn"),
    CHAT_FILTER_SC_ENABLED("chatFilterSCOn"),
    CHAT_ALERTS("chatAlertsOn"),
    ALIASES("aliasesOn"),
    CHAT_REPLACEMENTS("chatReplacementsOn"),
    CHAT_FILTER_COORDS_ENABLED("chatFilterCoordsOn", true),
    CHAT_FILTER_HIDE_SACK_MESSAGES("chatFilterSackOn"),
    CHAT_FILTER_HIDE_AUTOPET_MESSAGES("chatFilterAutopetOn"),
    CHAT_FILTER_HIDE_HYPE("chatFilterHypeOn"),
    CHAT_FILTER_PARTYBTN("chatFilterPartyBtnOn"),
    CHAT_FORMATTING("chatFormatting", true),
    ACCEPT_NPC("acceptNpcDialogue"),
    CAKE_NOTI("centuryCakeChatReminder"),
    
    // Misc
    INV_SEARCH("invSearch"),
    VALUE_FILTER("minValueFilter", true),
    GLOBAL_FA_COLORS("globalFaColors"),
    CUSTOM_FA_COLORS("customFaColors"),
    SB_ONLY_FAC("sbOnlyFaColors"),
    HUD_TEXT_SHADOW("hudTextShadow", true),
    RAIN_NOTI("rainNoti"),
    KEY_SHORTCUTS("keyShortcutsOn"),
    EQ_DISPLAY("equipmentDisplay"),
    
    // Items
    SLOT_LOCK_AUDIO("lockTriggerEnabled", true),
    FG_GUI_PROTECTION("sellProtectionEnabled"),
    FG_TOOLTIP("tooltipEnabled", true),
    FG_CHAT_FEEDBACK("protectNotiEnabled", true),
    FG_AUDIO_FEEDBACK("protectTriggerEnabled", true),

    // Annotations
    NONE("NaN", false)
    ;
    
    private final String jsonKey;
    private final Boolean defaultValue;
    private final ConfigFile configFile;
    private final List<Consumer<Boolean>> listeners = new ArrayList<>();

    BooleanKey(String jsonKey) {
        this(jsonKey, false, ConfigFile.SETTINGS);
    }

    BooleanKey(String jsonKey, Boolean def) {
        this(jsonKey, def, ConfigFile.SETTINGS);
    }    
    
    BooleanKey(String jsonKey, Boolean def, ConfigFile configFile) {
        this.jsonKey = jsonKey;
        this.defaultValue = def;
        this.configFile = configFile;
    }
    
    @Override
    public String getString() {
        return jsonKey;
    }
    
    @Override
    public Boolean getDefault() {
        return defaultValue != null && defaultValue;
    }
    
    @Override
    public ConfigFile getConfigFile() {
        return configFile;
    }
    
    @Override
    public void addListener(Consumer<Boolean> listener) {
        listeners.add(listener);
    }
    
    @Override
    public void notifyChange(Boolean newValue) {
        for (Consumer<Boolean> listener : listeners) {
            listener.accept(newValue);
        }
    }
}
