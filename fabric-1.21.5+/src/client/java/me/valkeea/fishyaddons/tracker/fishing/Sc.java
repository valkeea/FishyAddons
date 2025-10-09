package me.valkeea.fishyaddons.tracker.fishing;

import java.util.List;

import me.valkeea.fishyaddons.util.AreaUtils;

public class Sc {
    public static final String THUNDER = "thunder";
    public static final String RAGNAROK = "ragnarok";
    public static final String PLHLEG = "plhlegblast";
    public static final String JAWBUS = "lord_jawbus";
    public static final String CH_MINER = "abyssal_miner";
    public static final String DRAKE = "reindrake";
    public static final String TIKI = "wiki_tiki";
    public static final String YETI = "yeti";
    public static final String ENT = "ent";
    public static final String EMP = "the_loch_emperor";
    public static final String TITANOBOA = "titanoboa";
    public static final String WATER_HYDRA = "water_hydra";
    public static final String NIGHT_SQUID = "night_squid";
    public static final String GRIM_REAPER = "grim_reaper";
    public static final String HSPT_THUNDER = "thunder_hotspot";
    public static final String HSPT_JAWBUS = "lord_jawbus_hotspot";
    public static final String POOL_THUNDER = "thunder_pool";
    public static final String POOL_JAWBUS = "lord_jawbus_pool";

    public static String genAreaKey(String creature) {
        String baseKey = creature.toLowerCase().replaceAll("§[0-9a-fk-or]", "").replaceAll("[^a-z0-9_]", "_");
        
        if (AreaUtils.isCrimson() && (baseKey.equals(Sc.THUNDER) || baseKey.equals(Sc.JAWBUS))) {
            if (ScStats.isPool()) {
                return baseKey + "_pool";
            } else if (ScStats.isHspt()) {
                return baseKey + "_hotspot";
            } else {
                return baseKey;
            }
        }
        
        return baseKey;
    }    

    public static boolean isTracked(String creatureId) {
        return ScRegistry.getInstance().isTracked(creatureId);
    }

    public static List<String> getTrackedCreatures() {
        return ScRegistry.getInstance().getTrackedCreatures();
    }    

    public static String displayName(String creatureKey) {
        return ScRegistry.getInstance().getDisplayName(creatureKey);
    } 

    private Sc() {}
}
