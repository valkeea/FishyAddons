package me.wait.fishyaddons.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;


public class ParticleColorConfig {

    private static final Map<Integer, float[]> COLOR_MAP = new HashMap<>();
    private static final float RANDOMIZATION_INTENSITY = 0.4f;
    private static int lastIndex = -1;
    private static float[] cachedColor = null;

    private ParticleColorConfig() {}

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
        }
        else {
            return ConfigHandler.getCustomParticleColorIndex();
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
        int index = cachedIndex();

        if (index == 0) {
            return new float[0];
        }

        if (index != lastIndex) {
            lastIndex = index;
            cachedColor = COLOR_MAP.get(index);
        }

        // Randomize the color slightly
        if (cachedColor != null) {
            Random random = new Random();
            float[] randomizedColor = new float[3];
            for (int i = 0; i < 3; i++) {
                float variation = (random.nextFloat() - 0.5f) * RANDOMIZATION_INTENSITY; // Random offset between -0.1 and 0.1
                randomizedColor[i] = Math.max(0.0f, Math.min(1.0f, cachedColor[i] + variation)); // Clamp between 0 and 1
            }
            return randomizedColor;
        }

        return cachedColor;
    }
}