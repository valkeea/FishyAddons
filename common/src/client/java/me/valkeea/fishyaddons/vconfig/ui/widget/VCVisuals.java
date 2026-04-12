package me.valkeea.fishyaddons.vconfig.ui.widget;

import me.valkeea.fishyaddons.vconfig.ui.layout.Colors;

public class VCVisuals {
    private VCVisuals() {}
    private static int themeColor = 0xFFE2CAE9;
    
    // Alpha values for different states (0-255 range, stored in high byte)
    private static final int ALPHA = 0x10000000;
    
    // Masks: 0x00FFFFFF extracts RGB, 0x088FFFFF extracts reduced RGB
    private static final int RGB_MASK_FULL = 0x00FFFFFF;
    private static final int RGB_MASK_REDUCED = 0x088FFFFF;

    public static void set(int color) {
        themeColor = color;
    }

    public static int getThemeColor() {
        return themeColor;
    }

    /**
     * Applies alpha transparency to theme color based on hover state
     * @param hovered if true, uses aqua (43%), else theme (6%)
     * @return ARGB color value
     */
    private static int applyStateAlpha(boolean hovered) {
        if (hovered) {
            return (Colors.AQUA & RGB_MASK_REDUCED) | ALPHA;
        } else {
            return (themeColor & RGB_MASK_FULL) | ALPHA;
        }
    }

    /**
     * Theme-aware background colors for buttons
     */
    public static int bgHex(boolean hovered, boolean enabled) {
        if (!enabled && !hovered) {
            return Colors.DISABLED_GREY;
        }
        return applyStateAlpha(hovered);
    }

    protected static int bgHex2(boolean hovered) {
        return applyStateAlpha(hovered);
    }

    /**
     * Theme-aware border colors for buttons
     */
    public static int borderHex(boolean hovered, boolean enabled) {
        if (!enabled && !hovered) {
            return Colors.TRANSPARENT;
        }
        return applyStateAlpha(hovered);
    }

    protected static int borderHex2(boolean hovered) {
        return applyStateAlpha(hovered);
    }
}
