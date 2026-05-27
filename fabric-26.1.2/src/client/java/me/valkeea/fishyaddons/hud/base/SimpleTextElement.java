package me.valkeea.fishyaddons.hud.base;

import java.awt.Rectangle;

import me.valkeea.fishyaddons.hud.core.HudDrawer;
import me.valkeea.fishyaddons.hud.core.HudElementState;
import me.valkeea.fishyaddons.hud.core.HudUtils;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Elements with one text component and predefined styling.
 * 3 alignment options: left, center, right.
 */
public abstract class SimpleTextElement extends BaseHudElement {
    private final String placeholderText;

    @SuppressWarnings("java:S107")
    protected SimpleTextElement(BooleanKey hudKey, String displayName, String placeholderText,
                                 int defaultX, int defaultY, int defaultSize, int defaultColor,
                                 boolean defaultOutline, boolean defaultBg) {
        super(hudKey, displayName, defaultX, defaultY, defaultSize, defaultColor, defaultOutline, defaultBg);
        this.placeholderText = placeholderText;
    }

    @Override
    protected final void renderContent(HudDrawer drawer, Minecraft mc, HudElementState state) {

        Component text = getText();
        if (text == null || text.getString().isEmpty()) {
            if (isEditingMode()) {
                text = Component.literal(placeholderText);
            } else return;
        }
        
        int alignment = getTextAlignment();
        int textWidth = mc.font.width(text);
        int x = switch (alignment) {
            case 1 -> -textWidth / 2;
            case 2 -> -textWidth;
            default -> 0;
        };

        drawer.drawFormattedText(text, x, 0, getTextColor());
    }

    @Override
    public void drawBackGround(GuiGraphicsExtractor context, Minecraft mc, HudElementState state) {
        float scale = state.size / 12.0F;
        int textWidth = (int)(calculateContentWidth(mc) * scale);
        int textHeight = (int)(calculateContentHeight(mc) * scale);
        int alignment = getTextAlignment();
        int xOffset = switch (alignment) {
            case 1 -> -textWidth / 2;
            case 2 -> -textWidth;
            default -> 0;
        };
        HudUtils.drawBackground(context, state.x + xOffset, state.y, textWidth, textHeight);
    }

    @Override
    protected final int calculateContentWidth(Minecraft mc) {
        Component text = getText();
        if (text == null || text.getString().isEmpty()) {
            text = Component.literal(placeholderText);
        }
        return Math.max(80, mc.font.width(text));
    }

    @Override
    protected final int calculateContentHeight(Minecraft mc) {
        return mc.font.lineHeight;
    }

    @Override
    public Rectangle getBounds(Minecraft mc) {

        var state = getCachedState();
        float scale = state.size / 12.0F;

        int textWidth = (int)(calculateContentWidth(mc) * scale);
        int textHeight = (int)(calculateContentHeight(mc) * scale);

        int alignment = getTextAlignment();
        int x = switch (alignment) {
            case 1 -> -textWidth / 2;
            case 2 -> -textWidth;
            default -> 0;
        };

        return new Rectangle(state.x + x, state.y, textWidth, textHeight);
    }

    /**
     * Set alignment of the text within the element bounds
     * 0 = left, 1 = center, 2 = right, default is left
     */
    public int getTextAlignment() {
        return 0;
    }

    /**
     * Return the text color, or -1 to use the default element color
     */
    protected int getTextColor() {
        return getCachedState().color;
    }

    /**
     * Return the text to display, or null/empty if nothing should be shown
     */
    protected abstract Component getText();
}
