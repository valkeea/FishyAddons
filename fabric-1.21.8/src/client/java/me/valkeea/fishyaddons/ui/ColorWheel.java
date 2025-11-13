package me.valkeea.fishyaddons.ui;

import java.util.function.Consumer;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.ui.widget.FaButton;
import me.valkeea.fishyaddons.ui.widget.VCTextField;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ColorWheel extends Screen {

    private float red = 1.0f;
    private float green = 0.0f;
    private float blue = 0.0f;
    private float hue = 0.0f;
    private float saturation = 1.0f;
    private float lightness = 0.5f;

    private Consumer<Integer> onColorSelected;
    private Screen parent;
    private VCTextField hexField;

    private NativeImage wheelImage;
    private Identifier wheelId;
    private float uiScale;
    
    private int wheelCenterX;
    private int wheelCenterY;
    private int wheelRadius;
    private int barX;
    private int barY;
    private int barWidth;
    private int barHeight;
    private int widgetHeight;
    private int fieldWidth;
    private int btnWidth;
    
    private boolean draggingWheel = false;
    private boolean draggingBar = false;

    public ColorWheel(Screen parent, int initialColor, Consumer<Integer> onColorSelected) {
        super(Text.literal("Color Picker"));
        this.parent = parent;
        this.onColorSelected = onColorSelected;
        float[] rgb = intToRGB(initialColor);

        if (rgb.length == 3) {
            this.red = rgb[0];
            this.green = rgb[1];
            this.blue = rgb[2];
            float[] hsl = rgbToHsl(red, green, blue);
            this.hue = hsl[0];
            this.saturation = hsl[1];
            this.lightness = hsl[2];
        } else {
            this.hue = 0.0f;
            this.saturation = 1.0f;
            this.lightness = 0.5f;
            float[] fallback = hslToRgb(this.hue * 360, this.saturation, this.lightness);
            this.red = fallback[0];
            this.green = fallback[1];
            this.blue = fallback[2];
        }
    }

    private void calcDimensions() {
        uiScale = FishyConfig.getFloat(Key.MOD_UI_SCALE, 0.4265625f);
        wheelRadius = (int)(70 * uiScale);
        wheelCenterX = this.width / 2;
        wheelCenterY = this.height / 2 - 40;
        barWidth = (int)(140 * uiScale);
        barHeight = (int)(10 * uiScale); 
        widgetHeight = (int)(20 * uiScale);
        fieldWidth = (int)(150 * uiScale);
        btnWidth = (int)(70 * uiScale);               
        barX = wheelCenterX - barWidth / 2;
        barY = wheelCenterY + wheelRadius + 20;
    }

    @Override
    protected void init() {

        calcDimensions();        
        genTexture();
        updateRgbFromHsl();
        
        var tr = this.textRenderer;
        int fieldY = barY + barHeight + 20;
        hexField = new VCTextField(tr, wheelCenterX - fieldWidth / 2, fieldY, fieldWidth, widgetHeight, Text.literal("Hex (e.g. #FF00FF)"));
        hexField.setMaxLength(9);
        hexField.setUIScale(uiScale);
        updateHexField();
        this.addDrawableChild(hexField);

        int buttonY = fieldY + 30;
        var cancelBtn = new FaButton(
            wheelCenterX - fieldWidth  / 2, buttonY, btnWidth, widgetHeight,
            Text.literal("Cancel").setStyle(Style.EMPTY.withColor(0xFFFF8080)),
            btn -> this.client.setScreen(parent)
        );
        cancelBtn.setUIScale(uiScale);
        this.addDrawableChild(cancelBtn);

        var confirmBtn = new FaButton(
            wheelCenterX + 5, buttonY, btnWidth, widgetHeight,
            Text.literal("Confirm").setStyle(Style.EMPTY.withColor(0xFFCCFFCC)),
            btn -> {
                String hex = hexField.getText().trim();
                float[] rgb = new float[]{red, green, blue};
                if (hex.matches("^#?[0-9a-fA-F]{6}$")) {
                    try {
                        int color = Integer.parseInt(hex.replace("#", ""), 16);
                        rgb[0] = ((color >> 16) & 0xFF) / 255f;
                        rgb[1] = ((color >> 8) & 0xFF) / 255f;
                        rgb[2] = (color & 0xFF) / 255f;
                    } catch (NumberFormatException e) {
                        // Current RGB if parsing fails
                    }
                }
                onColorSelected.accept(rgbToInt(rgb));
                this.client.setScreen(parent);
            }
        );
        confirmBtn.setUIScale(uiScale);
        this.addDrawableChild(confirmBtn);
    }

    // --- Color wheel texture generation ---

    private void genTexture() {
        int size = wheelRadius * 2 + 1;
        
        deletePrevious();
        createNew(size);
        fillPixels(size);
        register();
    }
    
    private void deletePrevious() {
        if (wheelId != null) {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(wheelId);
        }
        if (wheelImage != null) {
            wheelImage.close();
        }
    }
    
    private void createNew(int size) {
        wheelImage = new NativeImage(size, size, false);
        wheelImage.fillRect(0, 0, size, size, 0x00000000);
    }
    
    private void fillPixels(int size) {
        for (int y = -wheelRadius; y <= wheelRadius; y++) {
            for (int x = -wheelRadius; x <= wheelRadius; x++) {
                int pixelX = x + wheelRadius;
                int pixelY = y + wheelRadius;
                
                if (pixelX < 0 || pixelX >= size || pixelY < 0 || pixelY >= size) continue;
                
                double distance = Math.sqrt((double)x * x + (double)y * y);
                int pixelColor = calculatePixelColor(x, y, distance);
                wheelImage.setColor(pixelX, pixelY, pixelColor);
            }
        }
    }
    
    private int calculatePixelColor(int x, int y, double distance) {
        if (distance > wheelRadius) {
            return 0x00000000;
        }
        
        float angle = calculateCorrectedAngle(x, y);
        float hueNormalized = angle / (2 * (float)Math.PI);
        float sat = (float)(distance / wheelRadius);
        float[] rgb = hslToRgb(hueNormalized * 360, sat, lightness);
        
        int baseColor = 0xFF000000 |
            ((int)(rgb[0] * 255) << 16) |
            ((int)(rgb[1] * 255) << 8) |
            (int)(rgb[2] * 255);
        
        return applyAntiAliasing(baseColor, distance);
    }
    
    private float calculateCorrectedAngle(int x, int y) {
        float angle = (float) Math.atan2(y, x);
        if (angle < 0) angle += 2 * Math.PI;
        
        // Apply 120-degree correction for coordinate system alignment
        angle -= (2 * Math.PI / 3);
        if (angle < 0) angle += 2 * Math.PI;
        
        return angle;
    }
    
    private int applyAntiAliasing(int baseColor, double distance) {
        if (distance <= wheelRadius - 1) {
            return baseColor;
        }
        
        float alpha = wheelRadius - (float)distance;
        return alpha > 0 ? blend(baseColor, 0x00000000, alpha) : 0x00000000;
    }
    
    private void register() {
        long timestamp = System.currentTimeMillis();
        wheelId = Identifier.of("fishyaddons", "color_wheel_" + timestamp);
        var texture = new NativeImageBackedTexture(() -> "Color Wheel", wheelImage);
        MinecraftClient.getInstance().getTextureManager().registerTexture(wheelId, texture);
    }

    // --- Rendering ---

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawTexture(RenderPipelines.GUI_TEXTURED, wheelId, wheelCenterX - wheelRadius, wheelCenterY - wheelRadius,
            0, 0, wheelRadius * 2, wheelRadius * 2, wheelRadius * 2, wheelRadius * 2);

        lightnessBar(context);
        selectionIndicators(context);
    }
    
    private void lightnessBar(DrawContext context) {
        for (int x = 0; x < barWidth; x++) {
            float currentLightness = x / (float) barWidth;
            float[] rgb = hslToRgb(hue * 360, saturation, currentLightness);
            int color = 0xFF000000 | 
                ((int)(rgb[0] * 255) << 16) | 
                ((int)(rgb[1] * 255) << 8) | 
                (int)(rgb[2] * 255);
            
            context.fill(barX + x, barY, barX + x + 1, barY + barHeight, color);
        }
        VCRenderUtils.border(context, barX, barY, barWidth, barHeight, 0xFFFFFFFF);
    }
    
    private void selectionIndicators(DrawContext context) {
        previewIndicator(context);
        lightnessIndicator(context);
    }
    
    private void previewIndicator(DrawContext context) {
        float angle = hue * 2 * (float)Math.PI;
        
        // Convert from texture coordinates back to screen coordinates
        int indicatorX = wheelCenterX + (int)(Math.cos(angle) * saturation * wheelRadius);
        int indicatorY = wheelCenterY + (int)(-Math.sin(angle) * saturation * wheelRadius);
        
        float indicatorRadius = 7 * uiScale;
        int color = 0xFF000000 | (int)(red * 255) << 16 | (int)(green * 255) << 8 | (int)(blue * 255);
        
        renderAntiAliasedCircle(context, indicatorX, indicatorY, indicatorRadius, color);
    }
    
    private void renderAntiAliasedCircle(DrawContext context, int centerX, int centerY, float radius, int fillColor) {
        int minX = (int)(centerX - radius - 2);
        int maxX = (int)(centerX + radius + 2);
        int minY = (int)(centerY - radius - 2);
        int maxY = (int)(centerY + radius + 2);
        
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                float dx = (float)x - centerX;
                float dy = (float)y - centerY;
                float distance = (float)Math.sqrt(dx * dx + dy * dy);
                
                int pixelColor = calcCirclePixelColor(distance, radius, fillColor);
                if (pixelColor != 0) {
                    context.fill(x, y, x + 1, y + 1, pixelColor);
                }
            }
        }
    }
    
    private int calcCirclePixelColor(float distance, float radius, int fillColor) {

        if (distance <= radius) {
            float alpha = distance > radius - 1 ? radius - distance : 1.0f;
            return alpha > 0 ? blend(fillColor, 0xFFFFFFFF, alpha) : 0;
        }
        
        if (distance <= radius + 1.5f) {
            float borderAlpha = distance > radius + 0.5f ? 
                (radius + 1.5f) - distance : 1.0f;
            return borderAlpha > 0 ? blend(0xFFFFFFFF, 0x80FFFFFF, borderAlpha) : 0;
        }
        
        return 0;
    }
    
    private void lightnessIndicator(DrawContext context) {
        int lightnessIndicatorX = barX + (int)(lightness * barWidth);
        context.fill(lightnessIndicatorX - 1, barY - 3, lightnessIndicatorX + 2, barY + barHeight + 3, 0xFFFFFFFF);
        context.fill(lightnessIndicatorX, barY - 2, lightnessIndicatorX + 1, barY + barHeight + 2, 0xFF000000);
    }

    // --- Interaction ---

    private void updateColorFromWheel(double mouseX, double mouseY) {
        double dx = mouseX - wheelCenterX;
        double dy = mouseY - wheelCenterY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        if (distance > wheelRadius) {
            dx = dx / distance * wheelRadius;
            dy = dy / distance * wheelRadius;
            distance = wheelRadius;
        }

        float textureX = (float)dx;
        float textureY = (float)(-dy);
        
        float angle = (float) Math.atan2(textureY, textureX);
        if (angle < 0) angle += 2 * Math.PI;
        hue = angle / (2 * (float)Math.PI);
        
        saturation = (float) (distance / wheelRadius);
        
        updateRgbFromHsl();
        updateHexField();
    }

    private void updateLightnessFromBar(double mouseX) {
        double relativeX = mouseX - barX;
        relativeX = Math.clamp(relativeX, 0, barWidth);
        lightness = (float) (relativeX / barWidth);
        
        updateRgbFromHsl();
        updateHexField();
        genTexture();
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

    // --- Utility functions ---

    private int blend(int color1, int color2, float alpha) {
        alpha = (float)Math.clamp(alpha, 0.0, 1.0);
        
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        
        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        
        int a = (int)(a1 * alpha + a2 * (1 - alpha));
        int r = (int)(r1 * alpha + r2 * (1 - alpha));
        int g = (int)(g1 * alpha + g2 * (1 - alpha));
        int b = (int)(b1 * alpha + b2 * (1 - alpha));
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    
    private static float[] hslToRgb(float h, float s, float l) {
        h = h / 360.0f;
        
        if (s == 0) {
            return new float[]{l, l, l};
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            double dx = mouseX - wheelCenterX;
            double dy = mouseY - wheelCenterY;
            double distance = Math.sqrt(dx * dx + dy * dy);
            
            if (distance <= wheelRadius) {
                draggingWheel = true;
                updateColorFromWheel(mouseX, mouseY);
                return true;
            }
            
            if (mouseX >= barX && mouseX <= barX + barWidth &&
                mouseY >= barY && mouseY <= barY + barHeight) {
                draggingBar = true;
                updateLightnessFromBar(mouseX);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingWheel) {
            updateColorFromWheel(mouseX, mouseY);
            return true;
        }

        if (draggingBar) {
            updateLightnessFromBar(mouseX);
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingWheel = false;
        draggingBar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public void close() {
        if (wheelId != null) {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(wheelId);
            wheelId = null;
        }
        if (wheelImage != null) {
            wheelImage.close();
            wheelImage = null;
        }
        super.close();
    }
}
