package me.valkeea.fishyaddons.handler;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;


public class ParticleVisuals {
    private ParticleVisuals() {}
    private static final Map<Integer, float[]> COLOR_MAP = new HashMap<>();
    private static final float RANDOMIZATION_INTENSITY = 0.4f;
    private static final String CUSTOM_MODE = "custom";    
    private static int lastIndex = -1;
    private static float[] cachedColor = null;
    private static float[] dmgScale = {0.15f};
    private static boolean scaleDmg = false;

    static {
        COLOR_MAP.put(1, new float[]{0.4F, 1.0F, 1.0F}); // Aqua
        COLOR_MAP.put(2, new float[]{0.4F, 1.0F, 0.6F}); // Mint
        COLOR_MAP.put(3, new float[]{1.0F, 0.8F, 1.0F}); // Pink
        COLOR_MAP.put(4, new float[]{0.9F, 0.9F, 1.0F}); // Light Blue/Prism
    }

    public static void refreshCache() {
        lastIndex = -1;
        cachedColor = null;
        dmgScale[0] = FishyConfig.getFloat(Key.DMG_SCALE, 0.15f);
        scaleDmg = FishyConfig.getState(Key.SCALE_CRIT, false);
    }

    public static int cachedIndex() {
        if (lastIndex != -1) {
            return lastIndex;
        } else {
            return FishyConfig.getCustomParticleColorIndex();
        }
    }

    public static float cachedScale() {
        if (dmgScale[0] != 0.15f) {
            return dmgScale[0];
        } else {
            dmgScale[0] = FishyConfig.getFloat(Key.DMG_SCALE, 0.15f);
            return dmgScale[0];
        }
    }

    public static void setDmgScale(float scale) {
        FishyConfig.setFloat(Key.DMG_SCALE, Math.clamp(scale, 0.05f, 1.5f));
    }

    public static boolean getDmg() {
        return scaleDmg;
    }

    public static void setDmg(boolean state) {
        scaleDmg = state;
        FishyConfig.toggle(Key.SCALE_CRIT, state);
        refreshCache();
    }

    public static boolean shouldReplace(float r, float g, float b) {
        boolean isReddish = (r > 0.5f && r > g && r > b);
        boolean isYellowish = (r > 0.6f && g > 0.6f && b < 0.4f);
        boolean isBlack = (r < 0.2f && g < 0.2f && b < 0.2f);
        boolean isGreenish = (g > 0.5f && g > r && g > b);

        return isReddish || isYellowish || isBlack || isGreenish;
    }

    public static float[] getCustomColor() {
        try {
            if (CUSTOM_MODE.equals(FishyConfig.getParticleColorMode())) {
                float[] rgb = FishyConfig.getCustomParticleRGB();
                if (rgb == null || rgb.length != 3) {
                    return new float[0];
                }
                // Randomize to prevent flat colors
                return randomize(rgb);

            } else {
                int index = FishyConfig.getCustomParticleColorIndex();
                if (index == 0) { 
                    return new float[0];
                }

                synchronized (ParticleVisuals.class) {
                    if (index != lastIndex) {
                        lastIndex = index;
                        cachedColor = COLOR_MAP.get(index);
                    }
                    if (cachedColor != null) {
                        return randomize(cachedColor.clone());
                    }
                }
                return new float[0];
            }
        } catch (Exception e) {
            System.err.println("Error getting custom particle color: " + e.getMessage());
            return new float[0];
        }
    }

    public static float[] randomize(float[] color) {
        if (color == null || color.length != 3) {
            return new float[0];
        }
        
        float[] randomizedColor = new float[3];
        for (int i = 0; i < 3; i++) {
            float variation = (ThreadLocalRandom.current().nextFloat() - 0.5f) * RANDOMIZATION_INTENSITY;
            randomizedColor[i] = Math.clamp(color[i] + variation, 0.0f, 1.0f);
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
        if (CUSTOM_MODE.equals(FishyConfig.getParticleColorMode())) {
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
        refreshCache();
    }

    public static void setCustomColor(float[] rgb) {
        if (rgb == null || rgb.length != 3) throw new IllegalArgumentException("RGB array must have exactly 3 elements.");
        FishyConfig.setCustomParticleRGB(
            Math.clamp(rgb[0], 0.0f, 1.0f),
            Math.clamp(rgb[1], 0.0f, 1.0f),
            Math.clamp(rgb[2], 0.0f, 1.0f)
        );
        FishyConfig.setParticleColorMode(CUSTOM_MODE);
        refreshCache();
    }
}