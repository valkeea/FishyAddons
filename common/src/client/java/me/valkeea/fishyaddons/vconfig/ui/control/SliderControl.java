package me.valkeea.fishyaddons.vconfig.ui.control;

import me.valkeea.fishyaddons.vconfig.binding.ConfigBinding.NumberBinding;
import me.valkeea.fishyaddons.vconfig.ui.layout.Dimensions;
import me.valkeea.fishyaddons.vconfig.ui.model.ClickContext;
import me.valkeea.fishyaddons.vconfig.ui.model.DragContext;
import me.valkeea.fishyaddons.vconfig.ui.render.VCRenderContext;
import me.valkeea.fishyaddons.vconfig.ui.widget.VCSlider;

public class SliderControl extends AbstractUIControl {
    private static final int BASE_WIDTH = 70;
    private static final int BASE_HEIGHT = 12;
    
    private final NumberBinding<?> binding;
    private final double minValue;
    private final double maxValue;
    private final String formatString;
    private final String[] labels;
    private final int[] labelColors;
    private final boolean composite;

    private VCSlider widget;
    private double cachedValue;

    public SliderControl(
        NumberBinding<?> binding,
        double[] minMax,
        String formatString,
        String[] labels,
        int[] labelColors,
        String[] tooltip,
        boolean composite
    ) {
        this.binding = binding;
        this.minValue = minMax[0];
        this.maxValue = minMax[1];
        this.formatString = formatString;
        this.labels = formatLabels(labels);
        this.labelColors = labelColors != null ? labelColors : new int[0];
        this.tooltip = tooltip;
        this.composite = composite;
    }
    
    @Override
    public void render(VCRenderContext ctx, int x, int y) {
        int adjustedY = y + (Dimensions.BUTTON_H - BASE_HEIGHT) / 2;
        
        if (cachedValueDirty) {
            cachedValue = binding.get();
            markCacheFresh();
        }        

        if (widget == null) {

            widget = new VCSlider(
                x, adjustedY,
                cachedValue,
                minValue,
                maxValue,
                formatString,
                labels,
                labelColors,
                this::onValueChanged
            );

        } else {
            widget.setPosition(x, adjustedY);

            if (!widget.isDragging() && cachedValueDirty) {
                cachedValue = binding.get();
                markCacheFresh();
                widget.setValue(cachedValue);
            }
        }

        widget.setWidth(getPreferredWidth());        
        widget.render(ctx);

        this.lastBounds = calculateBounds(x, adjustedY);
    }
    
    @Override
    public boolean handleClick(ClickContext ctx, int x, int y) {
        return widget != null && widget.mouseClicked(ctx.click);
    }
    
    @Override
    public boolean handleDrag(DragContext ctx, int x, int y) {
        if (widget == null) return false;
        return widget.mouseDragged(ctx.click);
    }

    @Override
    public boolean handleMouseRelease(ClickContext ctx) {
        return widget != null && widget.mouseReleased(ctx.click);
    }
    
    @Override
    public int getPreferredHeight() {
        return BASE_HEIGHT;
    }
    
    @Override
    public int getPreferredWidth() {
        return BASE_WIDTH - (composite ? Dimensions.SUB_CONTROL_OUTDENT : 0);
    }
    
    private void onValueChanged(double newValue) {
        if (cachedValue != newValue) {
            cachedValue = newValue;
            binding.set(cachedValue);
        }
    }

    private String[] formatLabels(String[] rawLabels) {
        if (rawLabels.length == 0) return rawLabels;
        String[] formatted = new String[rawLabels.length];
        for (int i = 0; i < rawLabels.length; i++) {
            String label = rawLabels[i];
            if (label != null && !label.isEmpty()) {
                formatted[i] = label.substring(0, 1)
                .toUpperCase() + label.substring(1);
            } else {
                formatted[i] = label;
            }
        }
        return formatted;
    }
}
