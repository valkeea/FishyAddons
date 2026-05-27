package me.valkeea.fishyaddons.hud.base;

import me.valkeea.fishyaddons.hud.core.HudDrawer;
import me.valkeea.fishyaddons.hud.core.HudElementState;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import net.minecraft.client.Minecraft;

/**
 * Elements with multi-component text display (label + value pairs)
 */
public abstract class SegmentedTextElement extends BaseHudElement {
    
    @SuppressWarnings("java:S107")    
    protected SegmentedTextElement(BooleanKey hudKey, String displayName,
                                     int defaultX, int defaultY, int defaultSize, int defaultColor,
                                     boolean defaultOutline, boolean defaultBg) {
        super(hudKey, displayName, defaultX, defaultY, defaultSize, defaultColor, defaultOutline, defaultBg);
    }

    @Override
    protected final void renderContent(HudDrawer drawer, Minecraft mc, HudElementState state) {
        Component[] components = getComponents();
        if (components == null || components.length == 0) return;

        int xOffset = 0;
        for (int i = 0; i < components.length; i++) {
            Component comp = components[i];
            if (comp == null) continue;
            
            if (comp.label != null) {
                drawer.drawText(comp.label, xOffset, 0, comp.labelColor);
                xOffset += mc.font.width(comp.label);
            }
            
            if (comp.value != null) {
                drawer.drawText(comp.value, xOffset, 0, comp.valueColor);
                xOffset += mc.font.width(comp.value);
            }
            
            if (i < components.length - 1) {
                xOffset += comp.spacing;
            }
        }
    }

    @Override
    protected final int calculateContentWidth(Minecraft mc) {
        Component[] components = getComponents();
        if (components == null || components.length == 0) return 100;

        int totalWidth = 0;
        for (int i = 0; i < components.length; i++) {
            Component comp = components[i];
            if (comp == null) continue;
            
            if (comp.label != null) {
                totalWidth += mc.font.width(comp.label);
            }
            if (comp.value != null) {
                totalWidth += mc.font.width(comp.value);
            }
            
            if (i < components.length - 1) {
                totalWidth += comp.spacing;
            }
        }
        return Math.max(100, totalWidth);
    }

    @Override
    protected final int calculateContentHeight(Minecraft mc) {
        return mc.font.lineHeight;
    }

    /**
     * Return the components to display. Each component can have a label, value, and colors.
     */
    protected abstract Component[] getComponents();

    /**
     * Represents a label-value pair component
     */
    protected static class Component {
        public final net.minecraft.network.chat.Component label;
        public final net.minecraft.network.chat.Component value;
        public final int labelColor;
        public final int valueColor;
        public final int spacing;

        public Component(net.minecraft.network.chat.Component label, net.minecraft.network.chat.Component value, int labelColor, int valueColor, int spacing) {
            this.label = label;
            this.value = value;
            this.labelColor = labelColor;
            this.valueColor = valueColor;
            this.spacing = spacing;
        }
        
        public Component(net.minecraft.network.chat.Component label, net.minecraft.network.chat.Component value, int labelColor, int valueColor) {
            this(label, value, labelColor, valueColor, 10);
        }
    }
}
