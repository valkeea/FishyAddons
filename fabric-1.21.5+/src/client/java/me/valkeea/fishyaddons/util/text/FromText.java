package me.valkeea.fishyaddons.util.text;

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
    
    private FromText() {}

}
