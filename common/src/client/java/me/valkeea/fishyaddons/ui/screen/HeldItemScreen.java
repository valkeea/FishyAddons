package me.valkeea.fishyaddons.ui.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;

import me.valkeea.fishyaddons.feature.item.animations.HeldItems;
import me.valkeea.fishyaddons.tool.FishyMode;
import me.valkeea.fishyaddons.ui.GuiUtil;
import me.valkeea.fishyaddons.ui.widget.FaSlider;
import me.valkeea.fishyaddons.vconfig.ui.manager.ScreenManager;
import me.valkeea.fishyaddons.vconfig.ui.render.RenderUtils;
import me.valkeea.fishyaddons.vconfig.ui.render.VCText;
import me.valkeea.fishyaddons.vconfig.ui.screen.AdjustedScreen;
import me.valkeea.fishyaddons.vconfig.ui.widget.FaButton;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class HeldItemScreen extends AdjustedScreen {
    private final int theme;   
    private static final short SPACING = 15;
    private static final short BTNW = 45;
    private static final short TOGGLEW = 150;
    private static final short BTNH = 15;
    private static final short LABELW = 45;
    private static final short CENTEROFFSET = 20;
    private static final short SLIDERW = 55;
    private static final short GAP = 10;
    private static final short COLUMNW = SLIDERW + LABELW + GAP * 2 + 30;  
    private FaButton doneBtn;

    private static final String DOUBLE_FORMAT = "%.2f";
    private static final String DEGREE_FORMAT = "%.0f°";
    private static final String PERCENTAGE_FORMAT = "%.0f%%";

    private final List<TransformSlider> sliders = new ArrayList<>();

    private static class TransformSlider {
        public final FaSlider slider;
        public final String label;
        public final int x;
        public final int y;
        public final boolean center;

        public TransformSlider(FaSlider slider, String label, int x, int y, boolean center) {
            this.slider = slider;
            this.label = label;
            this.x = x;
            this.y = y;
            this.center = center;
        }
    }
    
    public HeldItemScreen() {
        super(Component.literal("Held Item Animations"));
        this.theme = FishyMode.getThemeColor();
    }
    
    @Override
    protected void init() {
        super.init();
        sliders.clear();
        int centerX = this.width / 2;
        int startY = 40;
        int currentY = startY;
        
        var handBtn = new FaButton(centerX - TOGGLEW / 2, currentY, TOGGLEW, BTNH,
            GuiUtil.onOffLabel("Separate Hands", HeldItems.isSeparateHandSettings()), 
            button -> {
                HeldItems.setSeparateHandSettings(!HeldItems.isSeparateHandSettings());
                button.setMessage(GuiUtil.onOffLabel("Separate Hands", HeldItems.isSeparateHandSettings()));
                rebuildWidgets();
            });
        addRenderableWidget(handBtn);

        if (HeldItems.isSeparateHandSettings()) {
            var copyBtn = new FaButton(centerX - TOGGLEW / 2, currentY + 25, TOGGLEW, BTNH,
                Component.literal("Clone Main to Both Hands"), 
                button -> {
                    HeldItems.cloneGlobal();
                    rebuildWidgets();
                });
            addRenderableWidget(copyBtn);
        }

        currentY += SPACING * 3;
        
        if (!HeldItems.isSeparateHandSettings()) {
            currentY = addCentralizedSliders(centerX, currentY);
        } else {
            currentY = addThreeColumnLayout(centerX, currentY);
        }

        currentY += SPACING;

        var backBtn = new FaButton(centerX - BTNW - BTNW / 2, currentY, BTNW, BTNH,
            Component.literal("Back"), button -> 
            onClose()
        );
        addRenderableWidget(backBtn);

        var resetBtn = new FaButton(centerX - BTNW / 2, currentY, BTNW, BTNH,
            Component.literal("Reset"), button -> {
            HeldItems.reset();
            rebuildWidgets();
        });
        addRenderableWidget(resetBtn);

        doneBtn = new FaButton(centerX + BTNW / 2, currentY, BTNW, BTNH,
            Component.literal("Done"), button -> 
            onClose()
        );
        addRenderableWidget(doneBtn);
    }

    private int addCentralizedSliders(int centerX, int currentY) {

        // Position sliders  
        int x = centerX + CENTEROFFSET / 2;
        currentY = addSlider("Position X", x, currentY, true,
            HeldItems.getPosOffsetX(), -2.0, 2.0, DOUBLE_FORMAT,
            HeldItems.createSetter(HeldItems::setPosOffsetX));
        currentY = addSlider("Position Y", x, currentY, true,
            HeldItems.getPosOffsetY(), -2.0, 2.0, DOUBLE_FORMAT,
            HeldItems.createSetter(HeldItems::setPosOffsetY));
        currentY = addSlider("Position Z", x, currentY, true,
            HeldItems.getPosOffsetZ(), -2.0, 2.0, DOUBLE_FORMAT,
            HeldItems.createSetter(HeldItems::setPosOffsetZ));

        // Rotation sliders
        currentY = addSlider("Rotation X", x, currentY, true,
            HeldItems.getRotOffsetX(), -180.0, 180.0, DEGREE_FORMAT,
            HeldItems.createSetter(HeldItems::setRotOffsetX));
        currentY = addSlider("Rotation Y", x, currentY, true,
            HeldItems.getRotOffsetY(), -180.0, 180.0, DEGREE_FORMAT,
            HeldItems.createSetter(HeldItems::setRotOffsetY));
        currentY = addSlider("Rotation Z", x, currentY, true,
            HeldItems.getRotOffsetZ(), -180.0, 180.0, DEGREE_FORMAT,
            HeldItems.createSetter(HeldItems::setRotOffsetZ)); 
        
        // Unified scale slider
        currentY = addSlider("Scale", x, currentY, true,
            HeldItems.getScale(), 0.1, 3.0, DOUBLE_FORMAT,
            HeldItems.createSetter(HeldItems::setScale));
        
        // Animation controls
        currentY = addSlider("Swing Intensity", x, currentY, true,
            HeldItems.getSwingIntensity(), 0.0, 1.0, PERCENTAGE_FORMAT,
            HeldItems.createSetter(HeldItems::setSwingIntensity));

        currentY = addSlider("Equip Animation", x, currentY, true,
            HeldItems.getEquipIntensity(), 0.0, 1.0, PERCENTAGE_FORMAT,
            HeldItems.createSetter(HeldItems::setEquipIntensity));
        
        return currentY;
    }

    private int addThreeColumnLayout(int centerX, int currentY) {

        int leftX = centerX - COLUMNW / 2 - GAP; 
        int rightX = centerX + COLUMNW / 2 + GAP;
        int handStartY = currentY;
        
        // Offhand (left)
        int offHandY = handStartY;
        offHandY = addSlider("Off Pos X", leftX, offHandY, false,
            HeldItems.getOffHandPosX(), -2.0, 2.0, DOUBLE_FORMAT,
            HeldItems.createSetter(HeldItems::setOffHandPosX));
        offHandY = addSlider("Off Pos Y", leftX, offHandY, false,
            HeldItems.getOffHandPosY(), -2.0, 2.0, DOUBLE_FORMAT,
            HeldItems.createSetter(HeldItems::setOffHandPosY));
        offHandY = addSlider("Off Pos Z", leftX, offHandY, false,
            HeldItems.getOffHandPosZ(), -2.0, 2.0, DOUBLE_FORMAT,
            HeldItems.createSetter(HeldItems::setOffHandPosZ));
        offHandY = addSlider("Off Rot X", leftX, offHandY, false,
            HeldItems.getOffHandRotX(), -180.0, 180.0, DEGREE_FORMAT,
            HeldItems.createSetter(HeldItems::setOffHandRotX));
        offHandY = addSlider("Off Rot Y", leftX, offHandY, false,
            HeldItems.getOffHandRotY(), -180.0, 180.0, DEGREE_FORMAT,
            HeldItems.createSetter(HeldItems::setOffHandRotY));
        offHandY = addSlider("Off Rot Z", leftX, offHandY, false,
            HeldItems.getOffHandRotZ(), -180.0, 180.0, DEGREE_FORMAT,
            HeldItems.createSetter(HeldItems::setOffHandRotZ));

        // Main hand (right)
        int mainHandY = handStartY;
        mainHandY = addSlider("Main Pos X", rightX, mainHandY, false,
            HeldItems.getMainHandPosX(), -2.0, 2.0, DOUBLE_FORMAT,
            HeldItems.createSetter(HeldItems::setMainHandPosX));
        mainHandY = addSlider("Main Pos Y", rightX, mainHandY, false,
            HeldItems.getMainHandPosY(), -2.0, 2.0, DOUBLE_FORMAT,
            HeldItems.createSetter(HeldItems::setMainHandPosY));
        mainHandY = addSlider("Main Pos Z", rightX, mainHandY, false,
            HeldItems.getMainHandPosZ(), -2.0, 2.0, DOUBLE_FORMAT,
            HeldItems.createSetter(HeldItems::setMainHandPosZ));
        mainHandY = addSlider("Main Rot X", rightX, mainHandY, false,
            HeldItems.getMainHandRotX(), -180.0, 180.0, DEGREE_FORMAT,
            HeldItems.createSetter(HeldItems::setMainHandRotX));
        mainHandY = addSlider("Main Rot Y", rightX, mainHandY, false,
            HeldItems.getMainHandRotY(), -180.0, 180.0, DEGREE_FORMAT,
            HeldItems.createSetter(HeldItems::setMainHandRotY));
        mainHandY = addSlider("Main Rot Z", rightX, mainHandY, false,
            HeldItems.getMainHandRotZ(), -180.0, 180.0, DEGREE_FORMAT,
            HeldItems.createSetter(HeldItems::setMainHandRotZ));

        // Global settings (center)
        int maxHandY = Math.max(offHandY, mainHandY);
        int globalY = maxHandY + SPACING;
        int globalX = centerX + CENTEROFFSET / 2;

        globalY = addSlider("Scale", globalX, globalY, true,
            HeldItems.getScale(), 0.1, 3.0, DOUBLE_FORMAT,
            HeldItems.createSetter(HeldItems::setScale));

        globalY = addSlider("Swing Intensity", globalX, globalY, true,
            HeldItems.getSwingIntensity(), 0.0, 1.0, PERCENTAGE_FORMAT,
            HeldItems.createSetter(HeldItems::setSwingIntensity));

        globalY = addSlider("Equip Animation", globalX, globalY, true,
            HeldItems.getEquipIntensity(), 0.0, 1.0, PERCENTAGE_FORMAT,
            HeldItems.createSetter(HeldItems::setEquipIntensity));
        
        return globalY;
    }

    @SuppressWarnings("squid:S107")
    private int addSlider(String label, int columnCenterX, int currentY, boolean center,
                         double currentValue, double minValue, double maxValue, String format,
                         DoubleConsumer setter) {
                  
        int labelWidth = LABELW;
        int sliderX = center ? columnCenterX - SLIDERW / 2 + CENTEROFFSET / 2 : columnCenterX - SLIDERW / 2;
        int labelX = columnCenterX - labelWidth - SLIDERW / 2 - GAP;

        var slider = new FaSlider(sliderX, currentY, currentValue, minValue, maxValue, format, setter::accept);
        sliders.add(new TransformSlider(slider, label, labelX, currentY, center));

        return currentY + SPACING;
    }
    
    @Override
    public void extractContent(GuiGraphicsExtractor gge, int mX, int mY, float delta) {

        int bgBottom = doneBtn.getY() + doneBtn.getHeight() + SPACING;
        short bgTop = 15;
        short bgWidth = (short)(HeldItems.isSeparateHandSettings() ? COLUMNW * 3 : COLUMNW * 1.5);
        short bgLeft = (short)(this.width / 2 - bgWidth / 2);

        gge.fill(bgLeft - 5, bgTop - 5, bgLeft + bgWidth + 5, bgBottom + 5, 0x80000000); 
        RenderUtils.gradient(gge, bgLeft, bgTop, bgWidth, bgBottom - bgTop, 0x88000000);

        super.extractContent(gge, mX, mY, delta);

        var title = VCText.header("Held Item Animations", null);
        gge.centeredText(this.font, title, this.width / 2, 20, 0xFFFFFFFF);


        for (TransformSlider tfs : sliders) {

            String valueText = tfs.slider.getPercentageText();
            int labelX = tfs.center ? tfs.x - CENTEROFFSET : tfs.x;
            int w = tfs.center ? LABELW + CENTEROFFSET + GAP : LABELW;
            int valueX = labelX + w + SLIDERW + GAP * 2;

            gge.text(this.font, tfs.label, labelX, tfs.y + 2, theme);
            tfs.slider.render(gge, mX, mY);
            gge.text(this.font, valueText, valueX, tfs.y + 2, theme);
        }
    }
    
    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        var adjusted = adjustClick(click);
        for (TransformSlider tfs : sliders) {
            if (tfs.slider.mouseClicked(adjusted)) {
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }
    
    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        var adjusted = adjustClick(click);
        for (TransformSlider tfs : sliders) {
            if (tfs.slider.mouseReleased(adjusted)) {
                return true;
            }
        }
        
        return super.mouseReleased(click);
    }
    
    @Override
    public boolean mouseDragged(MouseButtonEvent click, double h, double v) {
        var adjusted = adjustClick(click);
        for (TransformSlider tfs : sliders) {
            if (tfs.slider.mouseDragged(adjusted)) {
                return true;
            }
        }

        return super.mouseDragged(click, h, v);
    }
    
    @Override
    public void onClose() {
        ScreenManager.openConfigScreen();
        HeldItems.saveChanges();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor gge, int mX, int mY, float delta) {
        // no blur to make preview clearer
    }     
}
