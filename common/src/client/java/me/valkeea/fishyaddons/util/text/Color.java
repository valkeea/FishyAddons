package me.valkeea.fishyaddons.util.text;

public class Color {
    private Color() {}

    public static float[] intToRGB(int color) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        return new float[]{r, g, b};
    }

    public static float[] intToRGBA(int color) {
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        return new float[]{r, g, b, a};
    }

    /**
     * Multiply the RGB components of a color by a factor, keeping alpha unchanged.
     * 
     * @param color The original color as an ARGB integer
     * @param factor The factor to multiply the RGB components by (e.g., 0.5 to darken, 1.5 to brighten)
     * @return The resulting color as an ARGB integer
     */
    public static int mulRGB(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        r = Math.min((int)(r * factor), 255);
        g = Math.min((int)(g * factor), 255);
        b = Math.min((int)(b * factor), 255);

        return (a << 24) | (r << 16) | (g << 8) | b;
    } 

    /**
     * Brighten a color by blending it with white based on the given factor.
     * 
     * @param color The original color as an ARGB integer
     * @param factor 0.0 to 1.0, where 0 returns the original color and 1 returns white
     * @return The brightened color as an ARGB integer
     */
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
     * Increase saturation by blending the color with a more saturated version of itself based on the given factor.
     * 
     * @param color The original color as an ARGB integer
     * @param factor 0.0 to 1.0, where 0 returns the original color and 1 returns the fully saturated color
     * @return The saturated color as an ARGB integer
     */
    public static int saturate(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        int gray = (r + g + b) / 3;

        r = (int)(gray + (r - gray) * (1 + factor));
        g = (int)(gray + (g - gray) * (1 + factor));
        b = (int)(gray + (b - gray) * (1 + factor));

        r = Math.min(255, r);
        g = Math.min(255, g);
        b = Math.min(255, b);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Darken a color by blending it with black based on the given factor.
     * 
     * @param color The original color as an ARGB integer
     * @param factor 0.0 to 1.0, where 0 returns the original color and 1 returns black
     * @return The darkened color as an ARGB integer
     */
    public static int desaturateAndDarken(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        int gray = (r + g + b) / 3;

        r = (int)(gray + (r - gray) * (1 - factor));
        g = (int)(gray + (g - gray) * (1 - factor));
        b = (int)(gray + (b - gray) * (1 - factor));

        r = (int)(r * (1 - factor));
        g = (int)(g * (1 - factor));
        b = (int)(b * (1 - factor));

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Desaturate a color by blending it with its grayscale version based on the given factor.
     * 
     * @param color The original color as an ARGB integer
     * @param factor 0.0 to 1.0, where 0 returns the original color and 1 returns the fully desaturated color
     * @return The desaturated color as an ARGB integer
     */
    public static int desaturate(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        int gray = (r + g + b) / 3;

        r = (int)(gray + (r - gray) * (1 - factor));
        g = (int)(gray + (g - gray) * (1 - factor));
        b = (int)(gray + (b - gray) * (1 - factor));

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
