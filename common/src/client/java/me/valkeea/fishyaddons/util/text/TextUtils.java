package me.valkeea.fishyaddons.util.text;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public class TextUtils {
    private TextUtils() {}

    private static final java.util.LinkedHashMap<Component, Component> stripColorCache = new java.util.LinkedHashMap<Component, Component>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<Component, Component> eldest) {
            return size() > 10;
        }
    };

    public static Component stripColor(Component text) {
        Component cached = stripColorCache.get(text);
        if (cached != null) {
            return cached;
        }
        
        var style = text.getStyle();
        boolean wasBold = style != null && style.isBold();
        boolean wasItalic = style != null && style.isItalic();

        var newStyle = Style.EMPTY;
        if (wasBold) {
            newStyle = newStyle.withBold(true);
        }

        if (wasItalic) {
            newStyle = newStyle.withItalic(true);
        }
        
        MutableComponent base;
        if (text.getSiblings().isEmpty()) {
            // Leaf node - use its content
            base = Component.literal(text.getString()).setStyle(newStyle);
        } else {
            // Container node - start empty and only add siblings
            base = Component.empty().setStyle(newStyle);
        }

        for (Component sibling : text.getSiblings()) {
            base.append(stripColor(sibling));
        }
        
        stripColorCache.put(text, base);
        return base;
    }

    public static Component stripFormatting(Component text) {
        if (text == null) return Component.literal("");
        String cleanString = stripColor(text.getString());
        return Component.literal(cleanString);
    }    

    public static String stripColor(String text) {
        return text == null ? "" : text.replaceAll("(?i)§[0-9a-fk-or]", "");
    }
}
