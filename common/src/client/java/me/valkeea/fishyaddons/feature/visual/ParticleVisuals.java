package me.valkeea.fishyaddons.feature.visual;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import me.valkeea.fishyaddons.util.text.Color;
import me.valkeea.fishyaddons.vconfig.annotation.VCListener;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.api.DoubleKey;
import me.valkeea.fishyaddons.vconfig.api.IntKey;

@VCModule
public class ParticleVisuals {
    private ParticleVisuals() {}

    private static final Map<Integer, float[]> COLOR_MAP = new HashMap<>();
    private static final float RANDOMIZATION_INTENSITY = 0.4f; 
      
    private static float[] rgb = null;
    private static double dmgScale = 0.0;
    private static boolean scaleDmg = false;

    static {
        COLOR_MAP.put(1, new float[]{0.4F, 1.0F, 1.0F}); // Aqua
        COLOR_MAP.put(2, new float[]{0.4F, 1.0F, 0.6F}); // Mint
        COLOR_MAP.put(3, new float[]{1.0F, 0.8F, 1.0F}); // Pink
        COLOR_MAP.put(4, new float[]{0.9F, 0.9F, 1.0F}); // Light Blue/Prism
    }

    public static float[] getCustomColor(float r, float g, float b) {
        if (rgb == null || !shouldReplace(r, g, b)) return new float[0];
        return randomize(rgb.clone());
    }

    public static void updateColor(int idx) {
        switch(idx) {
            case 1, 2, 3, 4 -> rgb = COLOR_MAP.get(idx);
            case 5 -> rgb = Color.intToRGB(Config.get(IntKey.REDSTONE_COLOR));
            default -> rgb = new float[0];
        }
    }

    public static float[] randomize(float[] color) {
        if (color.length != 3) return new float[0];
        
        float[] randomized = new float[3];
        for (int i = 0; i < 3; i++) {
            float variation = (ThreadLocalRandom.current().nextFloat() - 0.5f) * RANDOMIZATION_INTENSITY;
            randomized[i] = Math.clamp(color[i] + variation, 0.0f, 1.0f);
        }
        return randomized;
    }

    public static boolean shouldReplace(float r, float g, float b) {
        boolean red = (r > 0.5f && r > g && r > b);
        boolean yellow = (r > 0.6f && g > 0.6f && b < 0.4f);
        boolean black = (r < 0.2f && g < 0.2f && b < 0.2f);
        boolean green = (g > 0.5f && g > r && g > b);

        return red || yellow || black || green;
    }    

    public static float cachedScale() {
        return (float) dmgScale;
    }

    public static boolean getDmg() {
        return scaleDmg && me.valkeea.fishyaddons.api.skyblock.SkyblockAreas.isGalatea();
    }

    @VCListener(ints = IntKey.REDSTONE_COLOR)
    public static void onCustomColorChanged(int newValue) {
        if (newValue == 0) return;
        rgb = Color.intToRGB(newValue);
        Config.set(IntKey.REDSTONE_COLOR_INDEX, 5);
    }

    @VCListener(ints = IntKey.REDSTONE_COLOR_INDEX)
    public static void onIndexChanged(int newValue) {
        updateColor(newValue);
    }    

    @VCListener(value = BooleanKey.SCALE_CRIT, doubles = DoubleKey.DMG_SCALE)
    public static void onScaleChanged() {
        dmgScale = Config.get(DoubleKey.DMG_SCALE);
        scaleDmg = Config.get(BooleanKey.SCALE_CRIT);
    }
}
