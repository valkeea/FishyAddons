package me.valkeea.fishyaddons.gui;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class ColorPickerScreen extends Screen {

    private float red = 1.0f;
    private float green = 0.0f;
    private float blue = 0.0f;

    private Consumer<float[]> onColorSelected;
    private Screen parent;

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

        // RGB sliders
        this.addDrawableChild(new Slider(x, y, sliderWidth, sliderHeight, "Red", red, value -> red = value));
        this.addDrawableChild(new Slider(x, y + 25, sliderWidth, sliderHeight, "Green", green, value -> green = value));
        this.addDrawableChild(new Slider(x, y + 50, sliderWidth, sliderHeight, "Blue", blue, value -> blue = value));

        // Confirm / Cancel
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Confirm"), btn -> {
            onColorSelected.accept(new float[]{red, green, blue});
            this.client.setScreen(parent);
        }).dimensions(x, y + 90, 70, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), btn -> 
            this.client.setScreen(parent)
        ).dimensions(x + 80, y + 90, 70, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        // Draw current color preview
        int previewSize = 40;
        int px = (this.width - previewSize) / 2;
        int py = this.height / 2 + 40;

        context.fill(px, py, px + previewSize, py + previewSize, 0xFF000000 | (int)(red * 255) << 16 | (int)(green * 255) << 8 | (int)(blue * 255));
        context.drawBorder(px, py, previewSize, previewSize, 0xFFFFFFFF);
    }

    private static class Slider extends SliderWidget {
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
