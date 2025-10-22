package me.valkeea.fishyaddons.ui;

import java.util.ArrayList;
import java.util.List;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.handler.HeldItems;
import me.valkeea.fishyaddons.tool.FishyMode;
import me.valkeea.fishyaddons.ui.widget.FaButton;
import me.valkeea.fishyaddons.ui.widget.VCSlider;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class HeldItemScreen extends Screen {
    private final Screen parent;
    private final int theme;
    private final float scale;    
    private int btnW;
    private int toggleW;
    private int btnH;
    private int labelW;
    private int centerOffset;
    private int sliderW;
    private int gap;
    private int columnW;
    private int spacing;    
    private FaButton doneBtn;

    private static final String FLOAT_FORMAT = "%.2f";
    private static final String DEGREE_FORMAT = "%.0f°";
    private static final String PERCENTAGE_FORMAT = "%.0f%%";

    private final List<TransformSlider> sliders = new ArrayList<>();

    private static class TransformSlider {
        public final VCSlider slider;
        public final String label;
        public final int x;
        public final int y;
        public final boolean center;

        public TransformSlider(VCSlider slider, String label, int x, int y, boolean center) {
            this.slider = slider;
            this.label = label;
            this.x = x;
            this.y = y;
            this.center = center;
        }
    }
    
    public HeldItemScreen(Screen parent) {
        super(Text.literal("Held Item Animations"));
        this.parent = parent;
        this.theme = FishyMode.getThemeColor();
        this.scale = FishyConfig.getFloat(Key.MOD_UI_SCALE, 0.4265625f);
    }

    private void calcDimensions() {
        spacing = (int) (20 * scale);
        btnW = (int) (60 * scale);
        toggleW = (int) (200 * scale);
        btnH = (int) (20 * scale);
        labelW = (int) (60 * scale);
        centerOffset = (int) (30 * scale);
        sliderW = (int) (70 * scale);
        gap = (int) (10 * scale);
        columnW = sliderW + labelW + gap * 2 + (int) (40 * scale);
    }    
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = this.width / 2;
        int startY = 40;
        int currentY = startY;

        calcDimensions();
        
        sliders.clear();
        
        var handBtn = new FaButton(centerX - toggleW / 2, currentY, toggleW, btnH,
            GuiUtil.onOffLabel("Separate Hands", HeldItems.isSeparateHandSettings()), 
            button -> {
                HeldItems.setSeparateHandSettings(!HeldItems.isSeparateHandSettings());
                button.setMessage(GuiUtil.onOffLabel("Separate Hands", HeldItems.isSeparateHandSettings()));
                clearAndInit();
            });
        handBtn.setUIScale(scale);
        addDrawableChild(handBtn);

        if (HeldItems.isSeparateHandSettings()) {
            var copyBtn = new FaButton(centerX - toggleW / 2, currentY + 25, toggleW, btnH,
                Text.literal("Clone Global Settings to Both Hands"), 
                button -> {
                    HeldItems.cloneGlobal();
                    clearAndInit();
                });
            copyBtn.setUIScale(scale);
            addDrawableChild(copyBtn);
        }

        currentY += spacing * 3;
        
        if (!HeldItems.isSeparateHandSettings()) {
            currentY = addCentralizedSliders(centerX, currentY);
        } else {
            currentY = addThreeColumnLayout(centerX, currentY);
        }

        currentY += spacing;

        var backBtn = new FaButton(centerX - btnW - btnW / 2, currentY, btnW, btnH,
            Text.literal("Back"), button -> {
            if (this.client != null) {
                this.client.setScreen(this.parent);
            }
        });
        backBtn.setUIScale(scale);
        addDrawableChild(backBtn);

        var resetBtn = new FaButton(centerX - btnW / 2, currentY, btnW, btnH,
            Text.literal("Reset"), button -> {
            HeldItems.reset();
            clearAndInit();
        });
        resetBtn.setUIScale(scale);
        addDrawableChild(resetBtn);

        doneBtn = new FaButton(centerX + btnW / 2, currentY, btnW, btnH,
            Text.literal("Done"), button -> {
            if (this.client != null) {
                this.client.setScreen(this.parent);
            }
        });
        doneBtn.setUIScale(scale);
        addDrawableChild(doneBtn);
    }

    private int addCentralizedSliders(int centerX, int currentY) {

        // Position sliders  
        int x = centerX + centerOffset / 2;
        currentY = addSlider("Position X", x, currentY, true,
            HeldItems.getPosOffsetX(), -2.0f, 2.0f, FLOAT_FORMAT,
            HeldItems.createSetter(HeldItems::setPosOffsetX));
        currentY = addSlider("Position Y", x, currentY, true,
            HeldItems.getPosOffsetY(), -2.0f, 2.0f, FLOAT_FORMAT,
            HeldItems.createSetter(HeldItems::setPosOffsetY));
        currentY = addSlider("Position Z", x, currentY, true,
            HeldItems.getPosOffsetZ(), -2.0f, 2.0f, FLOAT_FORMAT,
            HeldItems.createSetter(HeldItems::setPosOffsetZ));

        // Rotation sliders
        currentY = addSlider("Rotation X", x, currentY, true,
            HeldItems.getRotOffsetX(), -180.0f, 180.0f, DEGREE_FORMAT,
            HeldItems.createSetter(HeldItems::setRotOffsetX));
        currentY = addSlider("Rotation Y", x, currentY, true,
            HeldItems.getRotOffsetY(), -180.0f, 180.0f, DEGREE_FORMAT,
            HeldItems.createSetter(HeldItems::setRotOffsetY));
        currentY = addSlider("Rotation Z", x, currentY, true,
            HeldItems.getRotOffsetZ(), -180.0f, 180.0f, DEGREE_FORMAT,
            HeldItems.createSetter(HeldItems::setRotOffsetZ)); 
        
        // Unified scale slider
        currentY = addSlider("Scale", x, currentY, true,
            HeldItems.getScale(), 0.1f, 3.0f, FLOAT_FORMAT,
            HeldItems.createSetter(HeldItems::setScale));
        
        // Animation controls
        currentY = addSlider("Swing Intensity", x, currentY, true,
            HeldItems.getSwingIntensity(), 0.0f, 1.0f, PERCENTAGE_FORMAT,
            HeldItems.createSetter(HeldItems::setSwingIntensity));

        currentY = addSlider("Equip Animation", x, currentY, true,
            HeldItems.getEquipIntensity(), 0.0f, 1.0f, PERCENTAGE_FORMAT,
            HeldItems.createSetter(HeldItems::setEquipIntensity));
        
        return currentY;
    }

    private int addThreeColumnLayout(int centerX, int currentY) {

        int leftX = centerX - columnW / 2 - gap; 
        int rightX = centerX + columnW / 2 + gap;
        int handStartY = currentY;
        
        // Offhand (left)
        int offHandY = handStartY;
        offHandY = addSlider("Off Pos X", leftX, offHandY, false,
            HeldItems.getOffHandPosX(), -2.0f, 2.0f, FLOAT_FORMAT,
            HeldItems.createSetter(HeldItems::setOffHandPosX));
        offHandY = addSlider("Off Pos Y", leftX, offHandY, false,
            HeldItems.getOffHandPosY(), -2.0f, 2.0f, FLOAT_FORMAT,
            HeldItems.createSetter(HeldItems::setOffHandPosY));
        offHandY = addSlider("Off Pos Z", leftX, offHandY, false,
            HeldItems.getOffHandPosZ(), -2.0f, 2.0f, FLOAT_FORMAT,
            HeldItems.createSetter(HeldItems::setOffHandPosZ));
        offHandY = addSlider("Off Rot X", leftX, offHandY, false,
            HeldItems.getOffHandRotX(), -180.0f, 180.0f, DEGREE_FORMAT,
            HeldItems.createSetter(HeldItems::setOffHandRotX));
        offHandY = addSlider("Off Rot Y", leftX, offHandY, false,
            HeldItems.getOffHandRotY(), -180.0f, 180.0f, DEGREE_FORMAT,
            HeldItems.createSetter(HeldItems::setOffHandRotY));
        offHandY = addSlider("Off Rot Z", leftX, offHandY, false,
            HeldItems.getOffHandRotZ(), -180.0f, 180.0f, DEGREE_FORMAT,
            HeldItems.createSetter(HeldItems::setOffHandRotZ));

        // Main hand (right)
        int mainHandY = handStartY;
        mainHandY = addSlider("Main Pos X", rightX, mainHandY, false,
            HeldItems.getMainHandPosX(), -2.0f, 2.0f, FLOAT_FORMAT,
            HeldItems.createSetter(HeldItems::setMainHandPosX));
        mainHandY = addSlider("Main Pos Y", rightX, mainHandY, false,
            HeldItems.getMainHandPosY(), -2.0f, 2.0f, FLOAT_FORMAT,
            HeldItems.createSetter(HeldItems::setMainHandPosY));
        mainHandY = addSlider("Main Pos Z", rightX, mainHandY, false,
            HeldItems.getMainHandPosZ(), -2.0f, 2.0f, FLOAT_FORMAT,
            HeldItems.createSetter(HeldItems::setMainHandPosZ));
        mainHandY = addSlider("Main Rot X", rightX, mainHandY, false,
            HeldItems.getMainHandRotX(), -180.0f, 180.0f, DEGREE_FORMAT,
            HeldItems.createSetter(HeldItems::setMainHandRotX));
        mainHandY = addSlider("Main Rot Y", rightX, mainHandY, false,
            HeldItems.getMainHandRotY(), -180.0f, 180.0f, DEGREE_FORMAT,
            HeldItems.createSetter(HeldItems::setMainHandRotY));
        mainHandY = addSlider("Main Rot Z", rightX, mainHandY, false,
            HeldItems.getMainHandRotZ(), -180.0f, 180.0f, DEGREE_FORMAT,
            HeldItems.createSetter(HeldItems::setMainHandRotZ));

        // Global settings (center)
        int maxHandY = Math.max(offHandY, mainHandY);
        int globalY = maxHandY + spacing;
        int globalX = centerX + centerOffset / 2;

        globalY = addSlider("Scale", globalX, globalY, true,
            HeldItems.getScale(), 0.1f, 3.0f, FLOAT_FORMAT,
            HeldItems.createSetter(HeldItems::setScale));

        globalY = addSlider("Swing Intensity", globalX, globalY, true,
            HeldItems.getSwingIntensity(), 0.0f, 1.0f, PERCENTAGE_FORMAT,
            HeldItems.createSetter(HeldItems::setSwingIntensity));

        globalY = addSlider("Equip Animation", globalX, globalY, true,
            HeldItems.getEquipIntensity(), 0.0f, 1.0f, PERCENTAGE_FORMAT,
            HeldItems.createSetter(HeldItems::setEquipIntensity));
        
        return globalY;
    }

    @SuppressWarnings("squid:S107")
    private int addSlider(String label, int columnCenterX, int currentY, boolean center,
                         float currentValue, float minValue, float maxValue, String format,
                         java.util.function.Consumer<Float> setter) {
                  
        int labelWidth = labelW;
        int sliderX = center ? columnCenterX - sliderW / 2 + centerOffset / 2 : columnCenterX - sliderW / 2;
        int labelX = columnCenterX - labelWidth - sliderW / 2 - gap;

        VCSlider slider = new VCSlider(sliderX, currentY, currentValue, minValue, maxValue, format, setter::accept);
        slider.setUIScale(scale);

        sliders.add(new TransformSlider(slider, label, labelX, currentY, center));

        return currentY + spacing;
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        int bgBottom = doneBtn.getY() + doneBtn.getHeight() + spacing;
        short bgTop = 15;
        short bgWidth = (short)(HeldItems.isSeparateHandSettings() ? columnW * 3 : columnW * 1.5);
        short bgLeft = (short)(this.width / 2 - bgWidth / 2);

        context.fill(bgLeft - 5, bgTop - 5, bgLeft + bgWidth + 5, bgBottom + 5, 0x80000000); 
        VCRenderUtils.gradient(context, bgLeft, bgTop, bgWidth, bgBottom - bgTop, 0x88000000);

        super.render(context, mouseX, mouseY, delta);

        var title = VCText.header("Held Item Animations", null);
        VCText.drawScaledCenteredText(context, this.textRenderer, title, this.width / 2, 20, 0xFFFFFF, scale);


        for (TransformSlider tfs : sliders) {

            String valueText = tfs.slider.getPercentageText();
            int labelX = tfs.center ? tfs.x - centerOffset : tfs.x;
            int w = tfs.center ? this.labelW + centerOffset + gap : this.labelW;
            int valueX = labelX + w + sliderW + gap * 2;

            VCText.drawScaledText(context, this.textRenderer, 
                tfs.label, labelX, tfs.y + 2, theme, scale);

            tfs.slider.render(context, mouseX, mouseY);

            VCText.drawScaledText(context, this.textRenderer, valueText, valueX, tfs.y + 2, theme, scale);
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (TransformSlider tfs : sliders) {
            if (tfs.slider.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (TransformSlider tfs : sliders) {
            if (tfs.slider.mouseReleased(button)) {
                return true;
            }
        }
        
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        for (TransformSlider tfs : sliders) {
            if (tfs.slider.mouseDragged(mouseX, button)) {
                return true;
            }
        }
        
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
    
    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // no blur to make preview clearer
    }     
}