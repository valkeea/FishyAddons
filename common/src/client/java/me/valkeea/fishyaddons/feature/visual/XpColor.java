package me.valkeea.fishyaddons.feature.visual;

import me.valkeea.fishyaddons.vconfig.annotation.VCListener;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.api.IntKey;

@VCModule
public class XpColor {
    private XpColor() {}
    private static int color = IntKey.XP_COLOR.getDefault();
    private static boolean isEnabled = false;
    private static boolean isOutlineEnabled = false;

    public static int get() { return color; }
    public static boolean isEnabled() { return isEnabled; }
    public static boolean isOutlineEnabled() { return isOutlineEnabled; }

    @VCListener(
        value = {BooleanKey.XP_OUTLINE, BooleanKey.XP_COLOR_ON},
        ints = IntKey.XP_COLOR
    )
    public static void refresh() {
        color = Config.get(IntKey.XP_COLOR);
        isEnabled = Config.get(BooleanKey.XP_COLOR_ON); 
        isOutlineEnabled = Config.get(BooleanKey.XP_OUTLINE);
    }
}
