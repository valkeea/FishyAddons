package me.valkeea.fishyaddons.feature.config;

import me.valkeea.fishyaddons.tool.FishyMode;
import me.valkeea.fishyaddons.vconfig.annotation.UISlider;
import me.valkeea.fishyaddons.vconfig.annotation.VCListener;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.api.DoubleKey;
import me.valkeea.fishyaddons.vconfig.api.IntKey;
import me.valkeea.fishyaddons.vconfig.core.UICategory;
import me.valkeea.fishyaddons.vconfig.ui.manager.ScreenManager;

@VCModule(UICategory.INTERFACE)
public class InterfaceConfig {
    private InterfaceConfig() {}

    @UISlider(
        key = DoubleKey.MOD_UI_SCALE,
        name = "Custom *UI Scale*",
        description = "Modify the scale of of mod UI elements.",
        min = 1.0, max = 2.0, format = "%.1fx"
    )
    private static double uiScale;

    @VCListener(doubles = DoubleKey.MOD_UI_SCALE)
    public static void onUiScaleChanged(double newValue) {
        ScreenManager.refreshCurrentScreen();
    }

    @UISlider(
        altKey = IntKey.THEME_MODE,
        name = "Mod *Theme*",
        min = 0.0, max = 5.0, format = "%s",
        labels = {"default", "purple", "blue", "white", "green", "rose"},
        description = {"Choose the visual theme for FishyAddons.",
            "This affects colors and styling throughout the mod."
        }        
    )
    private static int themeMode;

    @VCListener(ints = IntKey.THEME_MODE)
    public static void onThemeModeChanged(int newValue) {
        FishyMode.setTheme(newValue);
    }
}
