package me.valkeea.fishyaddons.vconfig.ui.widget;

import me.valkeea.fishyaddons.vconfig.ui.layout.Dimensions;
import me.valkeea.fishyaddons.vconfig.ui.render.VCRenderContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

@SuppressWarnings("squid:S107")
public class VCSlider {
    private static final int BASE_SLIDER_WIDTH = Dimensions.SLIDER_W;
    private static final int BASE_SLIDER_HEIGHT = Dimensions.SLIDER_H;
    private static final int BASE_KNOB_WIDTH = 6;
    private static final int BASE_KNOB_HEIGHT = 10;
    private static final String DEFAULT_FORMAT = "%.0f%%";
    
    private int x;
    private int y;
    private double internalValue;
    private boolean isDragging = false;
    private boolean showPreview = true;

    private final ValueChangeListener listener;
    private final double minValue;
    private final double maxValue;
    private final String displayFormat;
    private final String[] labels;
    private final int[] labelColors;
    
    private int sliderW;
    private int sliderH;
    private int knobW;
    private int knobH;
    
    public interface ValueChangeListener {
        void onValueChanged(double value);
    }
    
    /** Percentage */
    public VCSlider(int x, int y, double initialValue, ValueChangeListener listener) {
        this(x, y, initialValue, 0.0, 1.0, DEFAULT_FORMAT, new String[0], new int[0], listener);
    }

    /** Custom without labels */
    public VCSlider(int x, int y, double initialValue, double minValue, double maxValue,
                        String displayFormat, ValueChangeListener listener) {
        this(x, y, initialValue, minValue, maxValue, displayFormat, new String[0], new int[0], listener);
    }

    /** Labels without defined colors */
    public VCSlider(int x, int y, double initialValue, double minValue, double maxValue,
                        String displayFormat, String[] labels, ValueChangeListener listener) {
        this(x, y, initialValue, minValue, maxValue, displayFormat, labels, new int[labels.length], listener);
    }

    public VCSlider(int x, int y, double initialValue, double minValue, double maxValue, 
                         String displayFormat, String[] labels, int[] labelColors, ValueChangeListener listener) {
        this.x = x;
        this.y = y;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.displayFormat = displayFormat != null ? displayFormat : DEFAULT_FORMAT;
        this.labels = labels != null ? labels : new String[0];
        this.labelColors = labelColors != null ? labelColors : new int[this.labels.length];
        this.listener = listener;
        
        updateScaledDimensions(1.0f);
        
        this.internalValue = Math.clamp((initialValue - minValue) / (maxValue - minValue), 0.0, 1.0);
    }
    
    public void setUIScale(float scale) {
        updateScaledDimensions(scale);
    }
    
    private void updateScaledDimensions(float scale) {
        this.sliderW = Math.max(20, (int)(BASE_SLIDER_WIDTH * scale));
        this.sliderH = Math.max(4, (int)(BASE_SLIDER_HEIGHT * scale)); 
        this.knobW = Math.max(3, (int)(BASE_KNOB_WIDTH * scale)); 
        this.knobH = Math.max(6, (int)(BASE_KNOB_HEIGHT * scale));
    }

    public void render(VCRenderContext rCtx) {
        render(rCtx.context, rCtx.textRenderer, rCtx.mouseX, rCtx.mouseY, rCtx.themeColor);
    }
    
    public void render(DrawContext ctx, TextRenderer textRenderer, int mouseX, int mouseY, int themeColor) {

        int trackY = y + (sliderH - 4) / 2;
        ctx.fill(x, trackY, x + sliderW, trackY + 4, 0xC0333333);

        // Calculate knob position
        int kX = x + (int)((sliderW - knobW) * internalValue);
        int kY = y + (sliderH - knobH) / 2;
        boolean kHovered = mouseX >= kX && mouseX <= kX + knobW &&
                           mouseY >= kY && mouseY <= kY + knobH;
        
        int knobColor;
        if (isDragging) {
            knobColor = themeColor | 0xFF000000;
        } else if (kHovered) {
            knobColor = (themeColor & 0x00FFFFFF) | 0xE0000000;
        } else {
            knobColor = (themeColor & 0x00FFFFFF) | 0xC0000000;
        }
        
        ctx.fill(kX, kY, kX + knobW, kY + knobH, knobColor);
        ctx.fill(kX, kY, kX + knobW, kY + 1, 0xC0666666);
        ctx.fill(kX, kY + knobH - 1, kX + knobW, kY + knobH, 0xC0666666);
        ctx.fill(kX, kY, kX + 1, kY + knobH, 0xC0666666);
        ctx.fill(kX + knobW - 1, kY, kX + knobW, kY + knobH, 0xC0666666);

        if ((kHovered || isDragging) && showPreview) {
            int labelX = isDragging ? kX + knobW : mouseX + knobW;
            int labelY = isDragging ? kY - knobH : mouseY - knobH;
            ctx.drawText(
                textRenderer,
                getLabel(themeColor),
                labelX, labelY,
                themeColor | 0xFF000000,
                false
            );
        }
    }
    
    public boolean mouseClicked(Click click) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        if (button == 0 && isMouseOver(mouseX, mouseY)) {
            isDragging = true;
            return true;
        }
        return false;
    }
    
    public boolean mouseReleased(Click click) {
        if (click.button() == 0 && isDragging) {
            isDragging = false;
            updateValue(click.x(), true);
            return true;
        }
        return false;
    }
    
    public boolean mouseDragged(Click click) {
        if (isDragging && click.button() == 0) {
            updateValue(click.x(), usingSteps());
            return true;
        }
        return false;
    }
    
    private void updateValue(double mouseX, boolean saveToFile) {
        double newValue = (mouseX - x) / (sliderW - knobW);
        newValue = Math.clamp(newValue, 0.0, 1.0);
        internalValue = newValue;

        if (saveToFile && listener != null) {
            double actualValue = minValue + (internalValue * (maxValue - minValue));
            if (usingSteps()) {
                actualValue = minValue + stepIndex() * (maxValue - minValue) / (labels.length - 1);
            }
            listener.onValueChanged(actualValue);
        }
    }
    
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + sliderW &&
               mouseY >= y && mouseY <= y + sliderH;
    }
    
    public double getValue() {
        return minValue + (internalValue * (maxValue - minValue));
    }
    
    public void setValue(double actualValue) {
        if (usingSteps()) {
            int steps = labels.length - 1;
            double stepSize = (maxValue - minValue) / steps;
            actualValue = minValue + Math.round((actualValue - minValue) / stepSize) * stepSize;
        }
        this.internalValue = Math.clamp((actualValue - minValue) / (maxValue - minValue), 0.0, 1.0);
    }
    
    public void setPosition(int newX, int newY) {
        this.x = newX;
        this.y = newY;
    }
    
    public boolean isDragging() {
        return isDragging;
    }
    
    private Text getLabel(int defaultColor) {
        double actualValue = getValue();
        
        if (labels != null && labels.length > 0) {
            return getDescriptiveLabel(actualValue, defaultColor);
        } else {
            return getNumericLabel(actualValue, defaultColor);
        }
    }

    private Text getDescriptiveLabel(double value, int defaultColor) {

        int index = stepIndex();
        int valueAmt = labels.length;
        int color = labelColors != null && labelColors.length == valueAmt
            ? labelColors[index]
            : defaultColor;

        if (valueAmt > 1) {
            return style(labels[index], color);

        } else return labels.length == 1 && value <= 0.0
            ? style(labels[0], color)
            : getNumericLabel(value, defaultColor);
    }

    private Text getNumericLabel(double value, int defaultColor) {
        value = displayFormat.equals(DEFAULT_FORMAT) ? value * 100 : value;
        return style(String.format(displayFormat, value), defaultColor);
    }

    public String getPercentageLabel() {
        double actualValue = getValue();
        return String.format(displayFormat, actualValue * 100);
    }

    private Text style(String s, int color) {
        return Text.literal(s).styled(st -> st.withColor(color));
    }

    public void setShowPreview(boolean show) {
        this.showPreview = show;
    }

    private boolean usingSteps() {
        // Standalone label is treated as a special case for 0.0 value, not as a step        
        return labels != null && labels.length > 1;
    }

    private int stepIndex() {
        if (!usingSteps()) return -1;
        int steps = labels.length - 1;
        double stepSize = (maxValue - minValue) / (steps);
        return (int)Math.floor((getValue() - minValue) / stepSize);
    }
    
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return sliderW; }
    public int getHeight() { return sliderH; }
    public void setWidth(int w) { this.sliderW = w; }
}
