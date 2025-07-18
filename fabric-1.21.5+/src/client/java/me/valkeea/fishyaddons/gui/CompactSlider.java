package me.valkeea.fishyaddons.gui;

import net.minecraft.client.gui.DrawContext;

public class CompactSlider {
    private static final int SLIDER_WIDTH = 70;
    private static final int SLIDER_HEIGHT = 12;
    private static final int KNOB_WIDTH = 6;
    private static final int KNOB_HEIGHT = 10;
    private static final String DEFAULT_FORMAT = "%.0f%%";
    
    private final int x;
    private final int y;
    private float value;
    private boolean isDragging = false;
    private final ValueChangeListener listener;
    private final float minValue;
    private final float maxValue;
    private final String displayFormat;
    
    public interface ValueChangeListener {
        void onValueChanged(float value);
    }
    
    // Constructor for percentage slider (0-100%)
    public CompactSlider(int x, int y, float initialValue, ValueChangeListener listener) {
        this(x, y, initialValue, 0.0f, 1.0f, DEFAULT_FORMAT, listener);
    }
    
    // Constructor for custom range slider
    public CompactSlider(int x, int y, float initialValue, float minValue, float maxValue, 
                         String displayFormat, ValueChangeListener listener) {
        this.x = x;
        this.y = y;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.displayFormat = displayFormat;
        this.listener = listener;
        
        // Convert actual value to internal 0-1 range
        this.value = Math.clamp((initialValue - minValue) / (maxValue - minValue), 0.0f, 1.0f);
    }
    
    public void render(DrawContext context, int mouseX, int mouseY) {
        context.fill(x, y + 4, x + SLIDER_WIDTH, y + 8, 0xFF333333); // Slider background
        // Border lines
        context.fill(x, y + 4, x + SLIDER_WIDTH, y + 5, 0xFF666666);
        context.fill(x, y + 7, x + SLIDER_WIDTH, y + 8, 0xFF666666);
        context.fill(x, y + 4, x + 1, y + 8, 0xFF666666);
        context.fill(x + SLIDER_WIDTH - 1, y + 4, x + SLIDER_WIDTH, y + 8, 0xFF666666);
        
        // Calculate knob position
        int knobX = x + (int)((SLIDER_WIDTH - KNOB_WIDTH) * value);
        int knobY = y + 1;
        boolean isHovered = mouseX >= knobX && mouseX <= knobX + KNOB_WIDTH &&
                           mouseY >= knobY && mouseY <= knobY + KNOB_HEIGHT;
        
        int knobColor;
        if (isDragging) {
            knobColor = 0xFFFFFFFF;
        } else if (isHovered) {
            knobColor = 0xFFCCCCCC;
        } else {
            knobColor = 0xFFAAAAAA;
        }
        
        context.fill(knobX, knobY, knobX + KNOB_WIDTH, knobY + KNOB_HEIGHT, knobColor);
        context.fill(knobX, knobY, knobX + KNOB_WIDTH, knobY + 1, 0xFF666666);
        context.fill(knobX, knobY + KNOB_HEIGHT - 1, knobX + KNOB_WIDTH, knobY + KNOB_HEIGHT, 0xFF666666);
        context.fill(knobX, knobY, knobX + 1, knobY + KNOB_HEIGHT, 0xFF666666);
        context.fill(knobX + KNOB_WIDTH - 1, knobY, knobX + KNOB_WIDTH, knobY + KNOB_HEIGHT, 0xFF666666);
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isMouseOver(mouseX, mouseY)) {
            isDragging = true;
            updateValue(mouseX);
            return true;
        }
        return false;
    }
    
    public boolean mouseReleased(int button) {
        if (button == 0 && isDragging) {
            isDragging = false;
            return true;
        }
        return false;
    }
    
    public boolean mouseDragged(double mouseX, int button) {
        if (isDragging && button == 0) {
            updateValue(mouseX);
            return true;
        }
        return false;
    }
    
    private void updateValue(double mouseX) {
        float newValue = (float)(mouseX - x) / (SLIDER_WIDTH - KNOB_WIDTH);
        newValue = Math.clamp(newValue, 0.0f, 1.0f);
        
        if (newValue != value) {
            value = newValue;
            if (listener != null) {
                float actualValue = minValue + (value * (maxValue - minValue));
                listener.onValueChanged(actualValue);
            }
        }
    }
    
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + SLIDER_WIDTH &&
               mouseY >= y && mouseY <= y + SLIDER_HEIGHT;
    }
    
    public float getValue() {
        return minValue + (value * (maxValue - minValue));
    }
    
    public void setValue(float actualValue) {
        this.value = Math.clamp((actualValue - minValue) / (maxValue - minValue), 0.0f, 1.0f);
    }
    
    public String getPercentageText() {
        float actualValue = getValue();
        
        // Use custom format if provided, otherwise default behavior
        if (displayFormat.equals(DEFAULT_FORMAT)) {
            return String.format(displayFormat, actualValue * 100);
        } else {
            return String.format(displayFormat, actualValue);
        }
    }
    
    public int getX() { return x; }
    public int getY() { return y; }
    public static int getWidth() { return SLIDER_WIDTH; }
    public static int getHeight() { return SLIDER_HEIGHT; }
}
