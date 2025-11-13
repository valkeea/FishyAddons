package me.valkeea.fishyaddons.hud.base;

import me.valkeea.fishyaddons.hud.core.HudElementState;
import me.valkeea.fishyaddons.hud.core.HudDrawer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * Elements with one text component
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
        
        drawer.drawFormattedText(text, 0, 0, state.color);
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

    /**
     * Return the text to display, or null/empty if nothing should be shown
     */
    protected abstract Text getText();
}
