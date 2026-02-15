package me.valkeea.fishyaddons.util.text;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public class TextUtils {
    private TextUtils() {}

    private static final java.util.LinkedHashMap<Text, Text> stripColorCache = new java.util.LinkedHashMap<Text, Text>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<Text, Text> eldest) {
            return size() > 10;
        }
    };

    public static Text stripColor(Text text) {
        Text cached = stripColorCache.get(text);
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
        
        MutableText base;
        if (text.getSiblings().isEmpty()) {
            // Leaf node - use its content
            base = Text.literal(text.getString()).setStyle(newStyle);
        } else {
            // Container node - start empty and only add siblings
            base = Text.empty().setStyle(newStyle);
        }

        for (Text sibling : text.getSiblings()) {
            base.append(stripColor(sibling));
        }
        
        stripColorCache.put(text, base);
        return base;
    }

    public static Text stripFormatting(Text text) {
        if (text == null) return Text.literal("");
        String cleanString = stripColor(text.getString());
        return Text.literal(cleanString);
    }    

    public static String stripColor(String text) {
        return text == null ? "" : text.replaceAll("(?i)§[0-9a-fk-or]", "");
    }

    /**
     * Flatten and combine overly nested text into a single line, preserving styles
     */
    public static void combineToFlat(Text suffix, MutableText target) {
        if (suffix.getSiblings().isEmpty()) {
            String content = suffix.getString();
            if (!content.isEmpty()) {
                target.append(Text.literal(content).setStyle(suffix.getStyle()));
            }

        } else {
            for (Text sibling : suffix.getSiblings()) {
                combineToFlat(sibling, target);
            }
        }
    }
    
    /**
     * Recursively recolor text while preserving bold/italic formatting
     */
    public static MutableText recolorText(Text text, int color) {
        if (text == null) return Text.literal("");
        
        var style = text.getStyle();
        boolean wasBold = style != null && style.isBold();
        boolean wasItalic = style != null && style.isItalic();
        
        var newStyle = Style.EMPTY.withColor(color);
        if (wasBold) newStyle = newStyle.withBold(true);
        if (wasItalic) newStyle = newStyle.withItalic(true);
        
        MutableText base = text.getSiblings().isEmpty() 
            ? Text.literal(text.getString()).setStyle(newStyle)
            : Text.empty().setStyle(newStyle);
        
        for (Text sibling : text.getSiblings()) {
            base.append(recolorText(sibling, color));
        }
        
        return base;
    }
}
