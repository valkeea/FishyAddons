package me.valkeea.fishyaddons.util.text;

import net.minecraft.text.ClickEvent.RunCommand;
import net.minecraft.text.HoverEvent.ShowText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class FromText {

    /**
     * Finds the first literal text component with non-empty string
     */
    public static Text firstLiteral(Text text) {
        if (!text.getString().trim().isEmpty()) {
            return text;
        }
        
        for (Text sibling : text.getSiblings()) {
            Text found = firstLiteral(sibling);
            if (found != null) {
                return found;
            }
        }
        return null;
    } 
    
    /**
     * Recursively search for text with the specified color
     */
    public static Text findNodeWithColor(Text text, Formatting targetColor) {
        Style style = text.getStyle();
        if (style.getColor() != null && targetColor.getColorValue() != null 
            && style.getColor().getRgb() == targetColor.getColorValue()) {
            return text;
        }
        
        for (Text sibling : text.getSiblings()) {
            Text found = findNodeWithColor(sibling, targetColor);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /** 
     * Returns the first ShowText hoverevent or null if none found 
     */
    public static Text findShowText(Text text) {

        if (text.getStyle().getHoverEvent() instanceof ShowText textEvent) {
            return textEvent.value();
        }

        for (Text sibling : text.getSiblings()) {
            Text found = findShowText(sibling);
            if (found != null) {
                return found;
            }
        }
        
        return null;
    }

    /** 
     * Returns the first RunCommand clickevent or null if none found
     */
    public static String findCommand(Text text) {

        var event = text.getStyle().getClickEvent();
        if (event != null && event instanceof RunCommand runnable) {
            return runnable.command();
        }

        for (Text sibling : text.getSiblings()) {
            String found = findCommand(sibling);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private FromText() {}

}
