package me.valkeea.fishyaddons.render;

import net.minecraft.client.gui.DrawContext;

public class FaLayers {
    private FaLayers() {}
    
    // Reasonable Z-levels that still work with Minecraft's depth buffer
    private static final float GUI_TOP_Z = 4200.0f;
    private static final float GUI_OVERLAY_Z = 4000.0f;    
    

    // --- Z-levels for rendering above mod overlays and backgrounds ---
    public static void fillAboveOverlay(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, GUI_OVERLAY_Z);
        context.fill(x1, y1, x2, y2, color);
        context.getMatrices().pop();
    }

    // Wireframe box
    public static void drawBoxAboveOverlay(DrawContext context, int x, int y, int width, int height, int color) {
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, GUI_OVERLAY_Z);
        context.fill(x - 1, y - 1, x + width + 1, y, color);
        context.fill(x - 1, y + height, x + width + 1, y + height + 1, color);
        context.fill(x - 1, y, x, y + height, color);
        context.fill(x + width, y, x + width + 1, y + height, color);
        context.getMatrices().pop();
    }
    
    public static void drawTextAboveOverlay(DrawContext context, net.minecraft.client.font.TextRenderer textRenderer, 
                                           String text, int x, int y, int color, boolean shadow) {
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, GUI_OVERLAY_Z);
        if (shadow) {
            context.drawTextWithShadow(textRenderer, text, x, y, color);
        } else {
            context.drawText(textRenderer, text, x, y, color, false);
        }
        context.getMatrices().pop();
    }
    
    public static void drawCenteredTextAboveOverlay(DrawContext context, net.minecraft.client.font.TextRenderer textRenderer, 
                                                   String text, int centerX, int y, int color) {
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, GUI_OVERLAY_Z);
        context.drawCenteredTextWithShadow(textRenderer, text, centerX, y, color);
        context.getMatrices().pop();
    }
    
    // Execute a rendering block at the overlay Z-level
    public static void renderAboveOverlay(DrawContext context, Runnable renderBlock) {
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, GUI_OVERLAY_Z);
        renderBlock.run();
        context.getMatrices().pop();
    }
    
    // --- Z-levels for rendering critical UI elements at the absolute top level ---
    public static void renderAtTopLevel(DrawContext context, Runnable renderBlock) {
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, GUI_TOP_Z);
        renderBlock.run();
        context.getMatrices().pop();
    }
    
    public static void drawTextAtTopLevel(DrawContext context, net.minecraft.client.font.TextRenderer textRenderer, 
                                         String text, int x, int y, int color, boolean shadow) {
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, GUI_TOP_Z + 100);
        if (shadow) {
            context.drawTextWithShadow(textRenderer, text, x, y, color);
        } else {
            context.drawText(textRenderer, text, x, y, color, false);
        }
        context.getMatrices().pop();
    }
    
    public static void fillAtTopLevel(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, GUI_TOP_Z);
        context.fill(x1, y1, x2, y2, color);
        context.getMatrices().pop();
    }
    
    public static void drawBoxAtTopLevel(DrawContext context, int x, int y, int width, int height, int color) {
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, GUI_TOP_Z);
        context.fill(x - 1, y - 1, x + width + 1, y, color);
        context.fill(x - 1, y + height, x + width + 1, y + height + 1, color);
        context.fill(x - 1, y, x, y + height, color);
        context.fill(x + width, y, x + width + 1, y + height, color);
        context.getMatrices().pop();
    }
}