package me.valkeea.fishyaddons.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.feature.skyblock.TransLava;
import me.valkeea.fishyaddons.feature.visual.ParticleVisuals;
import me.valkeea.fishyaddons.feature.visual.XpColor;
import me.valkeea.fishyaddons.ui.list.ChatAlerts;
import me.valkeea.fishyaddons.ui.list.CustomFaColors;
import me.valkeea.fishyaddons.ui.list.FilterRules;
import me.valkeea.fishyaddons.ui.list.ScRules;
import me.valkeea.fishyaddons.ui.list.TabbedListScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

public class ConfigUIResolver {

    private ConfigUIResolver() {}

    private static final String XP_COLOR_ENTRY_NAME = "Color and Outline";

    private static final Map<String, Supplier<Screen>> SCREEN_PROVIDERS = new HashMap<>(); 
    private static final Map<String, IntSupplier> COLOR_GETTERS = new HashMap<>();
    private static final Map<String, IntConsumer> COLOR_SETTERS = new HashMap<>();
    
    /**
     * Register a color handler
     */
    public static void registerColorHandler(String configKey, IntSupplier colorGetter, IntConsumer colorSetter) {
        COLOR_GETTERS.put(configKey, colorGetter);
        COLOR_SETTERS.put(configKey, colorSetter);
    }
    
    /**
     * Register a screen provider
     */
    public static void registerRedirectHandler(String configKey, Supplier<Screen> screenProvider) {
        SCREEN_PROVIDERS.put(configKey, screenProvider);
    }
    
    /**
     * Open a screen for a VCEntry.
     */
    public static void openScreen(VCEntry entry) {
        if (entry.configKey == null) return;
        
        Supplier<Screen> provider = SCREEN_PROVIDERS.get(entry.configKey);
        if (provider != null) {
            Screen screen = provider.get();
            if (screen != null) {
                MinecraftClient.getInstance().setScreen(screen);
            }
        }
    }
    
    public static void initializeHandlers() {

        registerColorHandler(Key.RENDER_COORD_MS, 
            () -> FishyConfig.getInt(Key.RENDER_COORD_COLOR, -5653771),
            color -> FishyConfig.setInt(Key.RENDER_COORD_COLOR, color));
            
        registerColorHandler(Key.FISHY_TRANS_LAVA,
            () -> FishyConfig.getInt(Key.FISHY_TRANS_LAVA_COLOR, -13700380),
            color -> {
                FishyConfig.setInt(Key.FISHY_TRANS_LAVA_COLOR, color);
                TransLava.update();
            });
            
        registerColorHandler(XP_COLOR_ENTRY_NAME,
            () -> FishyConfig.getInt(Key.XP_COLOR),
            color -> {
                FishyConfig.setInt(Key.XP_COLOR, color);
                XpColor.refresh();
            });
            
        registerColorHandler(Key.XP_OUTLINE,
            () -> FishyConfig.getInt(Key.XP_COLOR),
            color -> {
                FishyConfig.setInt(Key.XP_COLOR, color);
                XpColor.refresh();
            });
            
        registerColorHandler(Key.CUSTOM_PARTICLE_COLOR_INDEX,
            () -> {
                if ("custom".equals(FishyConfig.getParticleColorMode())) {
                    float[] rgb = FishyConfig.getCustomParticleRGB();
                    if (rgb != null && rgb.length == 3) {
                        int r = Math.round(rgb[0] * 255);
                        int g = Math.round(rgb[1] * 255);
                        int b = Math.round(rgb[2] * 255);
                        return (0xFF << 24) | (r << 16) | (g << 8) | b;
                    }
                } else {
                    int index = FishyConfig.getCustomParticleColorIndex();
                    return switch (index) {
                        case 0 -> 0xFF808080;
                        case 1 -> 0xFF66FFFF;
                        case 2 -> 0xFF66FF99;
                        case 3 -> 0xFFFFCCFF;
                        case 4 -> 0xFFE5E5FF;
                        default -> 0xFFFFFFFF;
                    };
                }
                return 0xFFFFFFFF;
            },
            color -> {
                float r = ((color >> 16) & 0xFF) / 255.0f;
                float g = ((color >> 8) & 0xFF) / 255.0f;
                float b = (color & 0xFF) / 255.0f;
                ParticleVisuals.setCustomColor(new float[]{r, g, b});
                FishyConfig.setParticleColorMode("custom");
                ParticleVisuals.refreshCache();
            });
        
        registerRedirectHandler(Key.CUSTOM_FA_COLORS,
            () -> new CustomFaColors(MinecraftClient.getInstance().currentScreen));
            
        registerRedirectHandler(Key.CHAT_ALERTS_ENABLED,
            () -> new ChatAlerts(MinecraftClient.getInstance().currentScreen));
            
        registerRedirectHandler(Key.ALIASES_ENABLED,
            () -> new TabbedListScreen(MinecraftClient.getInstance().currentScreen, TabbedListScreen.Tab.COMMANDS));
            
        registerRedirectHandler(Key.KEY_SHORTCUTS_ENABLED,
            () -> new TabbedListScreen(MinecraftClient.getInstance().currentScreen, TabbedListScreen.Tab.KEYBINDS));
            
        registerRedirectHandler(Key.CHAT_REPLACEMENTS_ENABLED,
            () -> new TabbedListScreen(MinecraftClient.getInstance().currentScreen, TabbedListScreen.Tab.CHAT));
            
        registerRedirectHandler(Key.CHAT_FILTER_ENABLED,
            () -> new FilterRules(MinecraftClient.getInstance().currentScreen));
            
        registerRedirectHandler(Key.CHAT_FILTER_SC_ENABLED,
            () -> {
                Screen parent = MinecraftClient.getInstance().currentScreen;
                if (FishyConfig.getState(Key.CHAT_FILTER_SC_ENABLED, false)) {
                    return new ScRules(parent);
                }
                return null;
            });
            
        registerRedirectHandler(Key.HELD_ITEM_TRANSFORMS,
            () -> new me.valkeea.fishyaddons.ui.HeldItemScreen(
                MinecraftClient.getInstance().currentScreen));
    }
    
    /**
     * Get the current color for a VCEntry.
     */    
    public static int getColor(VCEntry entry) {
        if (XP_COLOR_ENTRY_NAME.equals(entry.name)) {
            IntSupplier getter = COLOR_GETTERS.get(XP_COLOR_ENTRY_NAME);
            return getter != null ? getter.getAsInt() : 0xFFFF0000;
        }

        if (entry.configKey == null) return 0xFFFF0000;
        
        IntSupplier getter = COLOR_GETTERS.get(entry.configKey);
        return getter != null ? getter.getAsInt() : 0xFFFF0000;
    }
    
    /**
     * Set a color for a VCEntry.
     */
    public static void setColor(VCEntry entry, int color) {
        if (XP_COLOR_ENTRY_NAME.equals(entry.name)) {
            IntConsumer setter = COLOR_SETTERS.get(XP_COLOR_ENTRY_NAME);
            if (setter != null) {
                setter.accept(color);
            }
            return;
        }

        if (entry.configKey == null) return;
        
        IntConsumer setter = COLOR_SETTERS.get(entry.configKey);
        if (setter != null) {
            setter.accept(color);
        }
    }
}
