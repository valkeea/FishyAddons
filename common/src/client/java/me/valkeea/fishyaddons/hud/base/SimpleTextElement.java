package me.valkeea.fishyaddons.hud.base;

import java.awt.Rectangle;

import me.valkeea.fishyaddons.hud.core.HudDrawer;
import me.valkeea.fishyaddons.hud.core.HudElementState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * Elements with one text component and predefined styling.
 * 3 alignment options: left, center, right.
 */
public abstract class SimpleTextElement extends BaseHudElement {
    private final String placeholderText;

    @SuppressWarnings("java:S107")
    protected SimpleTextElement(String hudKey, String displayName, String placeholderText,
                                 int defaultX, int defaultY, int defaultSize, int defaultColor,
                                 boolean defaultOutline, boolean defaultBg) {
        super(hudKey, displayName, defaultX, defaultY, defaultSize, defaultColor, defaultOutline, defaultBg);
        this.placeholderText = placeholderText;
    }

    @Override
    protected final void renderContent(HudDrawer drawer, MinecraftClient mc, HudElementState state) {

        Text text = getText();
        if (text == null || text.getString().isEmpty()) {
            if (isEditingMode()) {
                text = Text.literal(placeholderText);
            } else {
                return;
            }
        }
        
        int alignment = getTextAlignment();
        int textWidth = mc.textRenderer.getWidth(text);
        int x = switch (alignment) {
            case 1 -> -textWidth / 2;
            case 2 -> -textWidth;
            default -> 0;
        };

        drawer.drawFormattedText(text, x, 0, getTextColor());
    }

    @Override
    protected final int calculateContentWidth(MinecraftClient mc) {
        Text text = getText();
        if (text == null || text.getString().isEmpty()) {
            text = Text.literal(placeholderText);
        }
        return Math.max(80, mc.textRenderer.getWidth(text));
    }

    @Override
    protected final int calculateContentHeight(MinecraftClient mc) {
        return mc.textRenderer.fontHeight;
    }

    @Override
    public Rectangle getBounds(MinecraftClient mc) {

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
    protected abstract Text getText();
}
