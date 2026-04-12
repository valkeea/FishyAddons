package me.valkeea.fishyaddons.ui.widget;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;

public class FaSlider {
    private static final int BASE_SLIDER_WIDTH = 70;
    private static final int BASE_SLIDER_HEIGHT = 12;
    private static final int BASE_KNOB_WIDTH = 6;
    private static final int BASE_KNOB_HEIGHT = 10;
    private static final String DEFAULT_FORMAT = "%.0f%%";
    
    private int x;
    private int y;
    private double value;
    private boolean isDragging = false;
    private final ValueChangeListener listener;
    private final double minValue;
    private final double maxValue;
    private final String displayFormat;
    
    private int sliderWidth;
    private int sliderHeight;
    private int knobWidth;
    private int knobHeight;
    
    public interface ValueChangeListener {
        void onValueChanged(double value);
    }
    
    // Percentage
    public FaSlider(int x, int y, double initialValue, ValueChangeListener listener) {
        this(x, y, initialValue, 0.0, 1.0, DEFAULT_FORMAT, listener);
    }
    
    // Custom range
    public FaSlider(int x, int y, double initialValue, double minValue, double maxValue, 
                         String displayFormat, ValueChangeListener listener) {
        this.x = x;
        this.y = y;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.displayFormat = displayFormat;
        this.listener = listener;
        
        updateScaledDimensions(1.0f);
        this.value = Math.clamp((initialValue - minValue) / (maxValue - minValue), 0.0, 1.0);
    }
    
    public void setUIScale(float scale) {
        updateScaledDimensions(scale);
    }
    
    private void updateScaledDimensions(float scale) {
        this.sliderWidth = Math.max(20, (int)(BASE_SLIDER_WIDTH * scale));
        this.sliderHeight = Math.max(4, (int)(BASE_SLIDER_HEIGHT * scale)); 
        this.knobWidth = Math.max(3, (int)(BASE_KNOB_WIDTH * scale)); 
        this.knobHeight = Math.max(6, (int)(BASE_KNOB_HEIGHT * scale));
    }
    
    public void render(DrawContext context, int mouseX, int mouseY) {
        int themeColor = me.valkeea.fishyaddons.tool.FishyMode.getThemeColor();      
        int trackY = y + (sliderHeight - 4) / 2;
        context.fill(x, trackY, x + sliderWidth, trackY + 4, 0xC0333333);

        int knobX = x + (int)((sliderWidth - knobWidth) * value);
        int knobY = y + (sliderHeight - knobHeight) / 2;
        boolean isHovered = mouseX >= knobX && mouseX <= knobX + knobWidth &&
                           mouseY >= knobY && mouseY <= knobY + knobHeight;
        
        int knobColor;
        if (isDragging) {
            knobColor = themeColor | 0xFF000000;
        } else if (isHovered) {
            knobColor = (themeColor & 0x00FFFFFF) | 0xE0000000;
        } else {
            knobColor = (themeColor & 0x00FFFFFF) | 0xC0000000;
        }
        
        context.fill(knobX, knobY, knobX + knobWidth, knobY + knobHeight, knobColor);
        context.fill(knobX, knobY, knobX + knobWidth, knobY + 1, 0xC0666666);
        context.fill(knobX, knobY + knobHeight - 1, knobX + knobWidth, knobY + knobHeight, 0xC0666666);
        context.fill(knobX, knobY, knobX + 1, knobY + knobHeight, 0xC0666666);
        context.fill(knobX + knobWidth - 1, knobY, knobX + knobWidth, knobY + knobHeight, 0xC0666666);
    }
    
    public boolean mouseClicked(Click click) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        if (button == 0 && isMouseOver(mouseX, mouseY)) {
            isDragging = true;
            updateValue(mouseX);
            return true;
        }
        return false;
    }
    
    public boolean mouseReleased(Click click) {
        if (click.button() == 0 && isDragging) {
            isDragging = false;
            return true;
        }
        return false;
    }
    
    public boolean mouseDragged(Click click) {
        if (isDragging && click.button() == 0) {
            updateValue(click.x());
            return true;
        }
        return false;
    }
    
    private void updateValue(double mouseX) {
        double newValue = (mouseX - x) / (sliderWidth - knobWidth);
        newValue = Math.clamp(newValue, 0.0, 1.0);
        
        if (newValue != value) {
            value = newValue;
            if (listener != null) {
                double actualValue = minValue + (value * (maxValue - minValue));
                listener.onValueChanged(actualValue);
            }
        }
    }
    
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + sliderWidth &&
               mouseY >= y && mouseY <= y + sliderHeight;
    }
    
    public double getValue() {
        return minValue + (value * (maxValue - minValue));
    }
    
    public void setValue(double actualValue) {
        this.value = Math.clamp((actualValue - minValue) / (maxValue - minValue), 0.0, 1.0);
    }
    
    public void setPosition(int newX, int newY) {
        this.x = newX;
        this.y = newY;
    }
    
    public boolean isDragging() {
        return isDragging;
    }
    
    public String getPercentageText() {
        double actualValue = getValue();
        
        if (displayFormat.equals(DEFAULT_FORMAT)) {
            return String.format(displayFormat, actualValue * 100);
        } else {
            return String.format(displayFormat, actualValue);
        }
    }
    
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return sliderWidth; }
    public int getHeight() { return sliderHeight; }
}
