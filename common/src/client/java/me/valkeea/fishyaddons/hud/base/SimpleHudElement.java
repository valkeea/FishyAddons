package me.valkeea.fishyaddons.hud.base;

import me.valkeea.fishyaddons.hud.core.HudDrawer;
import me.valkeea.fishyaddons.hud.core.HudElementState;
import me.valkeea.fishyaddons.tool.FishyMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Elements with a text component and optional icon, with ability to add custom rendering
 */
public abstract class SimpleHudElement extends BaseHudElement {

    protected SimpleHudElement(String hudKey, String displayName,
                                int defaultX, int defaultY, int defaultSize, int defaultColor,
                                boolean defaultOutline, boolean defaultBg) {
        super(hudKey, displayName, defaultX, defaultY, defaultSize, defaultColor, defaultOutline, defaultBg);
    }

    @Override
    protected void renderEditingMode(HudDrawer drawer, MinecraftClient mc, HudElementState state) {
        if (!hasContent(getText())) {
            drawer.drawFormattedText(Text.literal(getDisplayName()), state.x, state.y, FishyMode.getThemeColor());
        }
    }

    @Override
    protected void renderContent(HudDrawer drawer, MinecraftClient mc, HudElementState state) {

        var text = getText();
        var iconId = getIcon();

        if (iconId != null) {

            int[] iconDims = getIconDimensions();
            int w = iconDims[0];
            int h = iconDims[1];

            drawer.drawIcon(iconId, 0, -mc.textRenderer.fontHeight / 3, w, h);

            if (hasContent(text)) {
                drawer.drawFormattedText(text, 14, 0, state.color);
            }

        } else if (hasContent(text)) {
            drawer.drawFormattedText(text, 0, 0, state.color);
        }

        renderCustom(drawer, mc, state);
    }

    @Override
    protected int calculateContentWidth(MinecraftClient mc) {

        var text = getText();
        var iconId = getIcon();
        int width = 0;

        if (iconId != null) width += getIconDimensions()[0] + 2;
        if (hasContent(text)) { 
            width += mc.textRenderer.getWidth(text);
        } else {
            width += mc.textRenderer.getWidth(getDisplayName());
        }

        return Math.max(12, width);
    }

    @Override
    protected int calculateContentHeight(MinecraftClient mc) {
        return getIcon() != null ? getIconDimensions()[1] : mc.textRenderer.fontHeight;
    }

    /** Add custom rendering after the text is drawn */
    protected void renderCustom(HudDrawer drawer, MinecraftClient mc, HudElementState state) {}

    /** Override to return the icon to display, default: null */
    protected Identifier getIcon() {
        return null;
    }

    /** Specify custom icon dimensions/content height if needed (default 12x12) */
    protected int[] getIconDimensions() {
        return new int[]{12, 12};
    }    

    /** Return the text to display, or null/empty if nothing should be shown */
    protected abstract Text getText();

    /** Check if a Text object has non-empty content */
    private static boolean hasContent(Text text) {
        if (text == null) return false;
        try {
            String str = text.getString();
            return str != null && !str.isEmpty();
        } catch (NullPointerException e) {
            return false;
        }
    }
}
