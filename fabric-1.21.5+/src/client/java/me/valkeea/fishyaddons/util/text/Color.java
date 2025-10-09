package me.valkeea.fishyaddons.util.text;

public class Color {
    private Color() {}

    public static float[] intToRGB(int color) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        return new float[]{r, g, b};
    }

    public static int darken(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        r = (int)(r * factor);
        g = (int)(g * factor);
        b = (int)(b * factor);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int darkenRGB(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        r = (int)(r * 0.8);
        g = (int)(g * 0.8);
        b = (int)(b * 0.8);

        return (color & 0xFF000000) | (r << 16) | (g << 8) | b;
    }    

    public static int lighten(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        r = Math.min((int)(r * factor), 255);
        g = Math.min((int)(g * factor), 255);
        b = Math.min((int)(b * factor), 255);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int brighten(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        r = Math.min(255, (int)(r + (255 - r) * factor));
        g = Math.min(255, (int)(g + (255 - g) * factor));
        b = Math.min(255, (int)(b + (255 - b) * factor));

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    
    /**
     * Create a gradient color between the base color and a brighter version
     * @param color Base color
     * @param intensity 0.0 to 1.0, where 0 is darkest and 1 is brightest
     * @return Gradient color
     */
    public static int createGradient(int color, float intensity) {
        int a = (color >> 24) & 0xFF;
        if (a == 0) a = 0xFF;
        
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        
        if (intensity < 0.5f) {
            float darkFactor = 0.4f + (intensity * 1.2f);
            r = (int)(r * darkFactor);
            g = (int)(g * darkFactor);
            b = (int)(b * darkFactor);
        } else {
            float brightFactor = (intensity - 0.5f) * 0.8f;
            r = Math.min(255, (int)(r + (255 - r) * brightFactor));
            g = Math.min(255, (int)(g + (255 - g) * brightFactor));
            b = Math.min(255, (int)(b + (255 - b) * brightFactor));
        }
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    
    /**
     * Ensure a color has full alpha
     * @param color Input color
     * @return Color with full alpha
     */
    public static int ensureOpaque(int color) {
        return color | 0xFF000000;
    }
    
    /**
     * Create a smooth gradient
     * @param color Base color
     * @param intensity 0.0 to 1.0, where 0 is darkest and 1 is brightest
     * @return Smooth gradient color
     */
    public static int createSmoothGradient(int color, float intensity) {
        int a = (color >> 24) & 0xFF;
        if (a == 0) a = 0xFF;
        
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        
        // Smooth curve
        float smoothIntensity = (float) (0.5 + 0.5 * Math.sin((intensity - 0.5) * Math.PI));
        
        if (smoothIntensity < 0.5f) {
            // Darker side: transition from 70% darker to base color
            float darkFactor = 0.3f + (smoothIntensity * 1.4f);
            r = (int)(r * darkFactor);
            g = (int)(g * darkFactor);
            b = (int)(b * darkFactor);
        } else {
            // Brighter side: transition from base color to 50% brighter
            float brightFactor = (smoothIntensity - 0.5f) * 1.0f;
            r = Math.min(255, (int)(r + (255 - r) * brightFactor));
            g = Math.min(255, (int)(g + (255 - g) * brightFactor));
            b = Math.min(255, (int)(b + (255 - b) * brightFactor));
        }
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
