package me.valkeea.fishyaddons.gui;

import java.util.function.Consumer;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public class ColorPickerScreen extends Screen {

    private float red = 1.0f;
    private float green = 0.0f;
    private float blue = 0.0f;

    private Consumer<float[]> onColorSelected;
    private Screen parent;
    private FaTextField hexField;

    public ColorPickerScreen(Screen parent, float[] initialColor, Consumer<float[]> onColorSelected) {
        super(Text.literal("Color Picker"));
        this.parent = parent;
        this.onColorSelected = onColorSelected;

        if (initialColor != null && initialColor.length == 3) {
            this.red = initialColor[0];
            this.green = initialColor[1];
            this.blue = initialColor[2];
        }
    }

    @Override
    protected void init() {
        int sliderWidth = 150;
        int sliderHeight = 20;
        int x = (this.width - sliderWidth) / 2;
        int y = this.height / 4;

        Runnable updateHexField = () -> {
            if (hexField != null) {
                hexField.setText(String.format("#%02X%02X%02X", (int)(red * 255), (int)(green * 255), (int)(blue * 255)));
            }
        };

        // RGB sliders
        this.addDrawableChild(new Slider(x, y, sliderWidth, sliderHeight, "Red", red, value -> {
            red = value;
            updateHexField.run();
        }));
        this.addDrawableChild(new Slider(x, y + 25, sliderWidth, sliderHeight, "Green", green, value -> {
            green = value;
            updateHexField.run();
        }));
        this.addDrawableChild(new Slider(x, y + 50, sliderWidth, sliderHeight, "Blue", blue, value -> {
            blue = value;
            updateHexField.run();
        }));

        TextRenderer tr = this.textRenderer;
        hexField = new FaTextField(tr, x, y + 75, sliderWidth, 20, Text.literal("Hex (e.g. #FF00FF)"));
        hexField.setMaxLength(9);
        hexField.setText(String.format("#%02X%02X%02X", (int)(red * 255), (int)(green * 255), (int)(blue * 255)));
        this.addDrawableChild(hexField);

        this.addDrawableChild(new FaButton(
            x, y + 110, 70, 20,
            Text.literal("Cancel").setStyle(Style.EMPTY.withColor(0xFF8080)),
            btn -> {
                this.client.setScreen(parent);
            }
        ));
        
        this.addDrawableChild(new FaButton(
            x + 80, y + 110, 70, 20,
            Text.literal("Confirm").setStyle(Style.EMPTY.withColor(0xCCFFCC)),
            btn -> {
                // If hex field is not empty, parse and use it
                String hex = hexField.getText().trim();
                float[] rgb = new float[]{red, green, blue};
                if (hex.matches("^#?[0-9a-fA-F]{6}$")) {
                    try {
                        int color = Integer.parseInt(hex.replace("#", ""), 16);
                        rgb[0] = ((color >> 16) & 0xFF) / 255f;
                        rgb[1] = ((color >> 8) & 0xFF) / 255f;
                        rgb[2] = (color & 0xFF) / 255f;
                    } catch (Exception ignored) {}
                }
                onColorSelected.accept(rgb);
                this.client.setScreen(parent);
            }
        ));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        int previewSize = 40;
        int px = (this.width - previewSize) / 2;
        int py = this.height / 2 + 40;

        context.fill(px, py, px + previewSize, py + previewSize, 0xFF000000 | (int)(red * 255) << 16 | (int)(green * 255) << 8 | (int)(blue * 255));
        context.drawBorder(px, py, previewSize, previewSize, 0xFFFFFFFF);
    }

    private static class Slider extends ThemedSlider {
        private final String labelBase;
        private final java.util.function.Consumer<Float> onChanged;

        public Slider(int x, int y, int width, int height, String label, float value, java.util.function.Consumer<Float> onChanged) {
            super(x, y, width, height, Text.literal(label), value);
            this.labelBase = label;
            this.onChanged = onChanged;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            int displayVal = (int)(value * 255);
            this.setMessage(Text.literal(labelBase + ": " + displayVal));
        }

        @Override
        protected void applyValue() {
            onChanged.accept((float) this.value);
        }
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