package me.valkeea.fishyaddons.ui.widget;

public class VCVisuals {
    private VCVisuals() {}
    private static int themeColor = 0xFFE2CAE9;

    public static void set(int color) {
        themeColor = color;
    }

    public static int getThemeColor() {
        return themeColor;
    }

    /**
     * Theme-aware background colors for buttons
     */
    public static int bgHex(boolean hovered, boolean enabled) {
        if (hovered) {
            return (themeColor & 0x088FFFFF) | 0x99000000;
        } else if (!enabled) {
            return 0xFF444444;
        } else {
            return (themeColor & 0x00FFFFFF) | 0x10000000;
        }
    }

    protected static int bgHex2(boolean hovered) {
        if (hovered) {
            return (themeColor & 0x088FFFFF) | 0x99000000;
        } else {
            return (themeColor & 0x00FFFFFF) | 0x10000000;
        }
    }    

    /**
     * Theme-aware border colors for buttons
     */
    public static int borderHex(boolean hovered, boolean enabled) {
        if (hovered) {
            return (themeColor & 0x088FFFFF) | 0x99000000;
        } else if (!enabled) {
            return 0xFF444444;
        } else {
            return (themeColor & 0x00FFFFFF) | 0x10000000;
        }
    }

    protected static int borderHex2(boolean hovered) {
        if (hovered) {
            return (themeColor & 0x088FFFFF) | 0x99000000;
        } else {
            return (themeColor & 0x00FFFFFF) | 0x10000000;
        }
    }
}
