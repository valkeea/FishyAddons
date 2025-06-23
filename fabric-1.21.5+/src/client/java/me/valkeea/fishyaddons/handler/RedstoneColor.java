package me.valkeea.fishyaddons.handler;

import me.valkeea.fishyaddons.config.FishyConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;


public class RedstoneColor {

    private static final Map<Integer, float[]> COLOR_MAP = new HashMap<>();
    private static final float RANDOMIZATION_INTENSITY = 0.4f;
    private static int lastIndex = -1;
    private static float[] cachedColor = null;

    private RedstoneColor() {}

    static {
        COLOR_MAP.put(1, new float[]{0.4F, 1.0F, 1.0F}); // Aqua
        COLOR_MAP.put(2, new float[]{0.4F, 1.0F, 0.6F}); // Mint
        COLOR_MAP.put(3, new float[]{1.0F, 0.8F, 1.0F}); // Pink
        COLOR_MAP.put(4, new float[]{0.9F, 0.9F, 1.0F}); // Light Blue/Prism
    }

    public static void invalidateCache() {
        lastIndex = -1; // Reset lastIndex to force recalculation
        cachedColor = null;
    }

    public static int cachedIndex() {
        if (lastIndex != -1) {
            return lastIndex;
        } else {
            return FishyConfig.getCustomParticleColorIndex();
        }
    }

    public static boolean shouldReplace(float r, float g, float b) {

        boolean isReddish = (r > 0.5f && r > g && r > b);
        boolean isYellowish = (r > 0.6f && g > 0.6f && b < 0.4f);
        boolean isBlack = (r < 0.2f && g < 0.2f && b < 0.2f);
        boolean isGreenish = (g > 0.5f && g > r && g > b);

        boolean shouldReplace = isReddish || isYellowish || isBlack || isGreenish;
        return shouldReplace;
    }

    public static float[] getCustomColor() {
        if ("custom".equals(FishyConfig.getParticleColorMode())) {
            float[] rgb = FishyConfig.getCustomParticleRGB();
            // Randomize to prevent flat colors
            return randomize(rgb);

        } else {
            // Preset logic (with caching)
            int index = FishyConfig.getCustomParticleColorIndex();
            if (index == 0) { return new float[0]; }

            if (index != lastIndex) {
                lastIndex = index;
                cachedColor = COLOR_MAP.get(index);
            }
            if (cachedColor != null) {
                return randomize(cachedColor);

            }
            return cachedColor;
        }
    }

    public static float[] randomize(float[] color) {
        Random random = new Random();
        float[] randomizedColor = new float[3];
        for (int i = 0; i < 3; i++) {
            float variation = (random.nextFloat() - 0.5f) * RANDOMIZATION_INTENSITY;
            randomizedColor[i] = Math.max(0.0f, Math.min(1.0f, color[i] + variation));
        }
        return randomizedColor;
        
    }

    public static float[] getParticleColor() {
        switch (FishyConfig.getCustomParticleColorIndex()) {
            case 1: return new float[]{0.4f, 1.0f, 1.0f};
            case 2: return new float[]{0.4f, 1.0f, 0.6f};
            case 3: return new float[]{1.0f, 0.8f, 1.0f};
            case 4: return new float[]{0.9f, 0.9f, 1.0f};
            default: return new float[]{1.0f, 0.0f, 0.0f};
        }
    }

    public static float[] getActiveParticleColor() {
        if ("custom".equals(FishyConfig.getParticleColorMode())) {
            return FishyConfig.getCustomParticleRGB();
        } else {
            return getParticleColor();
        }
    }     

    public static int getValidatedColorIndex() {
        int idx = FishyConfig.getCustomParticleColorIndex();
        if (idx < 1 || idx > 4) return 1;
        return idx;
    }

    public static void setPresetColorIndex(int index) {
        if (index < 1 || index > 4) index = 1;
        FishyConfig.setCustomParticleColorIndex(index);
        FishyConfig.setParticleColorMode("preset");
        invalidateCache();
    }

    public static void setCustomColor(float[] rgb) {
        if (rgb == null || rgb.length != 3) throw new IllegalArgumentException("RGB array must have exactly 3 elements.");
        FishyConfig.setCustomParticleRGB(
            Math.clamp(rgb[0], 0.0f, 1.0f),
            Math.clamp(rgb[1], 0.0f, 1.0f),
            Math.clamp(rgb[2], 0.0f, 1.0f)
        );
        FishyConfig.setParticleColorMode("custom");
        invalidateCache();
    }
}