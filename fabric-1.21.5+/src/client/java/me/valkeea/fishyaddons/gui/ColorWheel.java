package me.valkeea.fishyaddons.gui;

import java.util.function.Consumer;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public class ColorWheel extends Screen {

    private float red = 1.0f;
    private float green = 0.0f;
    private float blue = 0.0f;
    
    // HSL values
    private float hue = 0.0f;
    private float saturation = 1.0f;
    private float lightness = 0.5f;

    private Consumer<float[]> onColorSelected;
    private Screen parent;
    private FaTextField hexField;
    
    // UI element positions and sizes
    private int colorWheelCenterX;
    private int colorWheelCenterY;
    private int colorWheelRadius = 70;
    private int lightnessBarX;
    private int lightnessBarY;
    private int lightnessBarWidth = 160;
    private int lightnessBarHeight = 10;
    
    private boolean isDraggingWheel = false;
    private boolean isDraggingLightness = false;

    public ColorWheel(Screen parent, float[] initialColor, Consumer<float[]> onColorSelected) {
        super(Text.literal("Color Picker"));
        this.parent = parent;
        this.onColorSelected = onColorSelected;

        if (initialColor != null && initialColor.length == 3) {
            this.red = initialColor[0];
            this.green = initialColor[1];
            this.blue = initialColor[2];
            
            // Convert RGB to HSL
            float[] hsl = rgbToHsl(red, green, blue);
            this.hue = hsl[0];
            this.saturation = hsl[1];
            this.lightness = hsl[2];
        }
    }

    @Override
    protected void init() {
        // Calculate positions for the color wheel and lightness bar
        colorWheelCenterX = this.width / 2;
        colorWheelCenterY = this.height / 2 - 40;
        lightnessBarX = colorWheelCenterX - lightnessBarWidth / 2;
        lightnessBarY = colorWheelCenterY + colorWheelRadius + 20;
        
        updateRgbFromHsl();
        
        TextRenderer tr = this.textRenderer;
        int fieldY = lightnessBarY + lightnessBarHeight + 20;
        hexField = new FaTextField(tr, colorWheelCenterX - 75, fieldY, 150, 20, Text.literal("Hex (e.g. #FF00FF)"));
        hexField.setMaxLength(9);
        updateHexField();
        this.addDrawableChild(hexField);

        int buttonY = fieldY + 30;
        this.addDrawableChild(new FaButton(
            colorWheelCenterX - 75, buttonY, 70, 20,
            Text.literal("Cancel").setStyle(Style.EMPTY.withColor(0xFF8080)),
            btn -> this.client.setScreen(parent)
        ));
        
        this.addDrawableChild(new FaButton(
            colorWheelCenterX + 5, buttonY, 70, 20,
            Text.literal("Confirm").setStyle(Style.EMPTY.withColor(0xCCFFCC)),
            btn -> {
                // Parse hex field if it has valid content
                String hex = hexField.getText().trim();
                float[] rgb = new float[]{red, green, blue};
                if (hex.matches("^#?[0-9a-fA-F]{6}$")) {
                    try {
                        int color = Integer.parseInt(hex.replace("#", ""), 16);
                        rgb[0] = ((color >> 16) & 0xFF) / 255f;
                        rgb[1] = ((color >> 8) & 0xFF) / 255f;
                        rgb[2] = (color & 0xFF) / 255f;
                    } catch (NumberFormatException e) {
                        // Use current RGB values if parsing fails
                    }
                }
                onColorSelected.accept(rgb);
                this.client.setScreen(parent);
            }
        ));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {

        // Render background and color wheel
        this.renderBackground(context, mouseX, mouseY, delta);
        renderColorWheel(context);
        renderLightnessBar(context);
        
        // Render color preview
        int previewSize = 50;
        int px = colorWheelCenterX + colorWheelRadius + 20;
        int py = colorWheelCenterY - previewSize / 2;
        int color = 0xFF000000 | (int)(red * 255) << 16 | (int)(green * 255) << 8 | (int)(blue * 255);
        context.fill(px, py, px + previewSize, py + previewSize, color);
        context.drawBorder(px, py, previewSize, previewSize, 0xFFFFFFFF);
        
        // Render selection indicators
        renderSelectionIndicators(context);
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    private void renderColorWheel(DrawContext context) {
        for (int y = -colorWheelRadius; y <= colorWheelRadius; y++) {
            for (int x = -colorWheelRadius; x <= colorWheelRadius; x++) {
                double distance = Math.sqrt((double)x * x + (double)y * y);
                
                if (distance <= colorWheelRadius && distance >= 10) {
                    float angle = (float) Math.atan2(y, x);
                    if (angle < 0) angle += 2 * Math.PI;
                    
                    float hueNormalized = angle / (2 * (float)Math.PI);
                    float sat = (float)(distance / colorWheelRadius);
                    
                    // Use current lightness
                    float[] rgb = hslToRgb(hueNormalized * 360, sat, lightness);
                    int color = 0xFF000000 | 
                        ((int)(rgb[0] * 255) << 16) | 
                        ((int)(rgb[1] * 255) << 8) | 
                        (int)(rgb[2] * 255);
                    
                    int pixelX = colorWheelCenterX + x;
                    int pixelY = colorWheelCenterY + y;
                    
                    context.fill(pixelX, pixelY, pixelX + 1, pixelY + 1, color);
                }
            }
        }
    }
    
    private void renderLightnessBar(DrawContext context) {
        for (int x = 0; x < lightnessBarWidth; x++) {
            float currentLightness = x / (float) lightnessBarWidth;
            float[] rgb = hslToRgb(hue * 360, saturation, currentLightness);
            int color = 0xFF000000 | 
                ((int)(rgb[0] * 255) << 16) | 
                ((int)(rgb[1] * 255) << 8) | 
                (int)(rgb[2] * 255);
            
            context.fill(lightnessBarX + x, lightnessBarY, lightnessBarX + x + 1, lightnessBarY + lightnessBarHeight, color);
        }
        context.drawBorder(lightnessBarX, lightnessBarY, lightnessBarWidth, lightnessBarHeight, 0xFFFFFFFF);
    }
    
    private void renderSelectionIndicators(DrawContext context) {
        // Color wheel indicator
        float angle = hue * 2 * (float)Math.PI;
        int indicatorX = colorWheelCenterX + (int)(Math.cos(angle) * saturation * colorWheelRadius);
        int indicatorY = colorWheelCenterY + (int)(Math.sin(angle) * saturation * colorWheelRadius);
        
        // Draw crosshair
        context.fill(indicatorX - 4, indicatorY, indicatorX + 5, indicatorY + 1, 0xFFFFFFFF);
        context.fill(indicatorX, indicatorY - 4, indicatorX + 1, indicatorY + 5, 0xFFFFFFFF);
        
        // Lightness bar indicator
        int lightnessIndicatorX = lightnessBarX + (int)(lightness * lightnessBarWidth);
        context.fill(lightnessIndicatorX - 1, lightnessBarY - 3, lightnessIndicatorX + 2, lightnessBarY + lightnessBarHeight + 3, 0xFFFFFFFF);
        context.fill(lightnessIndicatorX, lightnessBarY - 2, lightnessIndicatorX + 1, lightnessBarY + lightnessBarHeight + 2, 0xFF000000);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            double dx = mouseX - colorWheelCenterX;
            double dy = mouseY - colorWheelCenterY;
            double distance = Math.sqrt(dx * dx + dy * dy);
            
            if (distance <= colorWheelRadius && distance >= 10) {
                isDraggingWheel = true;
                updateColorFromWheel(mouseX, mouseY);
                return true;
            }
            
            if (mouseX >= lightnessBarX && mouseX <= lightnessBarX + lightnessBarWidth &&
                mouseY >= lightnessBarY && mouseY <= lightnessBarY + lightnessBarHeight) {
                isDraggingLightness = true;
                updateLightnessFromBar(mouseX);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDraggingWheel) {
            updateColorFromWheel(mouseX, mouseY);
            return true;
        }
        if (isDraggingLightness) {
            updateLightnessFromBar(mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDraggingWheel = false;
        isDraggingLightness = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    private void updateColorFromWheel(double mouseX, double mouseY) {
        double dx = mouseX - colorWheelCenterX;
        double dy = mouseY - colorWheelCenterY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        if (distance > colorWheelRadius) {
            // Clamp to wheel edge
            dx = dx / distance * colorWheelRadius;
            dy = dy / distance * colorWheelRadius;
            distance = colorWheelRadius;
        }
        if (distance < 10) {
            distance = 10;
        }
        
        float angle = (float) Math.atan2(dy, dx);
        if (angle < 0) angle += 2 * Math.PI;
        hue = angle / (2 * (float)Math.PI);
        
        saturation = (float) (distance / colorWheelRadius);
        
        updateRgbFromHsl();
        updateHexField();
    }
    
    private void updateLightnessFromBar(double mouseX) {
        double relativeX = mouseX - lightnessBarX;
        relativeX = Math.clamp(relativeX, 0, lightnessBarWidth);
        lightness = (float) (relativeX / lightnessBarWidth);
        
        updateRgbFromHsl();
        updateHexField();
    }
    
    private void updateRgbFromHsl() {
        float[] rgb = hslToRgb(hue * 360, saturation, lightness);
        red = rgb[0];
        green = rgb[1];
        blue = rgb[2];
    }
    
    private void updateHexField() {
        if (hexField != null) {
            hexField.setText(String.format("#%02X%02X%02X", (int)(red * 255), (int)(green * 255), (int)(blue * 255)));
        }
    }
    
    // Simplified HSL to RGB conversion
    private static float[] hslToRgb(float h, float s, float l) {
        h = h / 360.0f;
        
        if (s == 0) {
            return new float[]{l, l, l}; // achromatic
        }
        
        float c = (1 - Math.abs(2 * l - 1)) * s;
        float x = c * (1 - Math.abs((h * 6) % 2 - 1));
        float m = l - c / 2;
        
        float r;
        float g;
        float b;
        
        float h6 = h * 6;
        if (h6 < 1) {
            r = c; g = x; b = 0;
        } else if (h6 < 2) {
            r = x; g = c; b = 0;
        } else if (h6 < 3) {
            r = 0; g = c; b = x;
        } else if (h6 < 4) {
            r = 0; g = x; b = c;
        } else if (h6 < 5) {
            r = x; g = 0; b = c;
        } else {
            r = c; g = 0; b = x;
        }
        
        return new float[]{r + m, g + m, b + m};
    }
    
    // RGB to HSL conversion
    private static float[] rgbToHsl(float r, float g, float b) {
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float h;
        float s;
        float l = (max + min) / 2;

        if (max == min) {
            h = s = 0;
        } else {
            float d = max - min;
            s = l > 0.5f ? d / (2 - max - min) : d / (max + min);
            
            if (max == r) {
                h = (g - b) / d + (g < b ? 6 : 0);
            } else if (max == g) {
                h = (b - r) / d + 2;
            } else {
                h = (r - g) / d + 4;
            }
            h /= 6;
        }

        return new float[]{h, s, l};
    }

    public static float[] intToRGB(int color) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        return new float[]{r, g, b};
    }

    public static int rgbToInt(float[] rgb) {
        int r = (int)(rgb[0] * 255) & 0xFF;
        int g = (int)(rgb[1] * 255) & 0xFF;
        int b = (int)(rgb[2] * 255) & 0xFF;
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }
}