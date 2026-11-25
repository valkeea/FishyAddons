package me.valkeea.fishyaddons.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public class GuiUtil {

    public static void wireBox(GuiGraphicsExtractor gge, int x, int y, int w, int h, int color) {
        gge.fill(x - 1, y - 1, x + w + 1, y, color);
        gge.fill(x - 1, y + h, x + w + 1, y + h + 1, color);
        gge.fill(x - 1, y, x, y + h, color);
        gge.fill(x + w, y, x + w + 1, y + h, color);
    }
    
    public static void wireBox(GuiGraphicsExtractor gge, java.awt.Rectangle bounds, int color) {
        wireBox(gge, bounds.x, bounds.y, bounds.width, bounds.height, color);
    }

    public static Component onOffLabel(String label, boolean enabled) {
        String state = enabled ? "ON" : "OFF";
        int color = enabled ? 0xFFCCFFCC : 0xFFFF8080;
        return Component.literal(label + ": ")
                .append(Component.literal(state).setStyle(Style.EMPTY.withColor(color)));
    }

    public static MutableComponent onOffLabel(boolean enabled) {
        String state = enabled ? "ON" : "OFF";
        int color = enabled ? 0xFFCCFFCC : 0xFFFF8080;
        return Component.literal(state).setStyle(Style.EMPTY.withColor(color));
    }

    public static void drawScaledText(GuiGraphicsExtractor gge, Font tr, String text, 
                                    int x, int y, int color, float scale) {
        var matrices = gge.pose();                                        
        matrices.pushMatrix();
        matrices.scale(scale, scale);
        int scaledX = (int)(x / scale);
        int scaledY = (int)(y / scale);
        gge.text(tr, text, scaledX, scaledY, color, false);
        matrices.popMatrix();
    }

    public static void drawScaledCenteredText(GuiGraphicsExtractor gge, Font tr, String text, 
                                            int centerX, int y, int color, float scale) {
        var matrices = gge.pose();                                        
        matrices.pushMatrix();
        matrices.scale(scale, scale);
        int scaledCenterX = (int)(centerX / scale);
        int scaledY = (int)(y / scale);
        int textWidth = getScaledTextWidth(tr, text, scale);
        int textX = scaledCenterX - textWidth / 2;
        gge.text(tr, text, textX, scaledY, color, false);
        matrices.popMatrix();
    }

    public static void drawScaledCenteredText(GuiGraphicsExtractor gge, Font tr, Component text, 
                                            int centerX, int y, int color, float scale) {
        var matrices = gge.pose();                                        
        matrices.pushMatrix();
        matrices.scale(scale, scale);
        int scaledCenterX = (int)(centerX / scale);
        int scaledY = (int)(y / scale);
        int textWidth = getScaledTextWidth(tr, text.getString(), scale);
        int textX = scaledCenterX - textWidth / 2;
        gge.text(tr, text, textX, scaledY, color, false);
        matrices.popMatrix();
    }

    public static int getScaledTextWidth(Font tr, String text, float scale) {
        int baseWidth = tr.width(text);
        float scaledWidth = baseWidth * scale;
        return (int)Math.ceil(scaledWidth * 1.02f);
    }    

    private GuiUtil() {
        throw new UnsupportedOperationException("Utility class");
    }
}
