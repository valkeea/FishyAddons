package me.valkeea.fishyaddons.vconfig.config;

import me.valkeea.fishyaddons.feature.filter.FilterConfig;
import me.valkeea.fishyaddons.feature.waypoints.ChainConfig;
import me.valkeea.fishyaddons.vconfig.api.Config;

/**
 * Access to configs managed by {@link Config}.
 * {@link FilterConfig} and {@link ChainConfig} are not included as
 * they are not accessed through the main config interface.
 */
public enum ConfigFile {
    SETTINGS,
    HUD,
    COLORS,
    SHORTCUTS,
    ALERTS,
    STATS,
    ITEMS
}
