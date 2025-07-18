package me.valkeea.fishyaddons.gui;

import java.util.Arrays;
import java.util.List;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.handler.ParticleVisuals;
import me.valkeea.fishyaddons.handler.PetInfo;
import me.valkeea.fishyaddons.handler.SkyblockCleaner;
import me.valkeea.fishyaddons.hud.TrackerDisplay;
import me.valkeea.fishyaddons.tracker.SackDropParser;
import me.valkeea.fishyaddons.tracker.TrackerUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public class SbScreen extends Screen {
    private static final int BTNW = 200;
    private static final int BTNH = 20;
    private static final String GALATEA_TEXT = "Moonglade Beacon";
    private static final String TRACKER_TEXT = "Profit Tracker";
    private CompactSlider dmgScaleSlider;
    private CompactSlider minValueSlider;
    private int petBtnX, petBtnY, petBtnW, petBtnH;
    private int trackBtnX, trackBtnY, trackBtnW, trackBtnH;  
    private int dmgBtnX, dmgBtnY, dmgBtnW, dmgBtnH;
    private int minValueBtnX, minValueBtnY, minValueBtnW, minValueBtnH;

    public SbScreen() {
        super(Text.literal("Skyblock Settings"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int by = centerY - 130;

        addDrawableChild(new FaButton(
            centerX - 100, by, BTNW, BTNH,
            getPhantomText(),
            btn -> {
                FishyConfig.toggle(Key.MUTE_PHANTOM, false);
                btn.setMessage(getPhantomText());
                SkyblockCleaner.refresh();
                btn.setFocused(false);
            }
        ));

        by += 30;

        petBtnX = centerX - 100;
        petBtnY = by;
        petBtnW = BTNW;
        petBtnH = BTNH;

        addDrawableChild(new FaButton(
            petBtnX, petBtnY, petBtnW, petBtnH,
            getPetText(),
            btn -> {
                FishyConfig.toggle(Key.HUD_PET_ENABLED, false);
                PetInfo.refresh();
                PetInfo.setNextCheck(FishyConfig.getState(Key.HUD_PET_ENABLED, false));
                PetInfo.setPending(!FishyConfig.getState(Key.HUD_PET_DYNAMIC, false));
                btn.setMessage(getPetText());
            }
        ));

        addDrawableChild(new FaButton(
            centerX + 100, by, 60, BTNH,
            getDynamicText(),
            btn -> {
                FishyConfig.toggle(Key.HUD_PET_DYNAMIC, false);
                PetInfo.refresh();
                btn.setMessage(getDynamicText());
            }
        ));

        addDrawableChild(new FaButton(
            centerX + 160, by, 60, BTNH,
            getPetXpText(),
            btn -> {
                FishyConfig.toggle(Key.HUD_PETXP, false);
                PetInfo.refresh();
                btn.setMessage(getPetXpText());
            }
        ));

        by += 30;

        dmgBtnX = centerX - 100;
        dmgBtnY = by;
        dmgBtnW = BTNW;
        dmgBtnH = BTNH;

        addDrawableChild(new FaButton(
            dmgBtnX, dmgBtnY, dmgBtnW, dmgBtnH,
            getDmgToggleText(), 
            btn -> {
                FishyConfig.toggle(Key.SCALE_CRIT, false);
                ParticleVisuals.refreshCache();
                btn.setMessage(getDmgToggleText());
            }
        ));    

        float dmgScale = FishyConfig.getFloat(Key.DMG_SCALE, 0.15f);
        dmgScaleSlider = new CompactSlider(dmgBtnX + dmgBtnW + 10, by, dmgScale, 
            0.05f, 1.5f, "%.2f", ParticleVisuals::setDmgScale);

        by += 30;

        int gtw = textRenderer.getWidth(GALATEA_TEXT);

        TextFieldWidget galateaTextField = new TextFieldWidget(
            this.textRenderer, centerX - (gtw / 2), by, BTNW, BTNH, Text.literal(GALATEA_TEXT));
        galateaTextField.setText(GALATEA_TEXT);
        galateaTextField.setEditable(false);
        galateaTextField.setMaxLength(64);
        galateaTextField.setUneditableColor(0xD0AEFF);
        galateaTextField.setDrawsBackground(false);
        this.addDrawableChild(galateaTextField); 

        by += 30;

        addDrawableChild(new FaButton(
            centerX - 100, by, BTNW / 2, BTNH,
            getAlarmText(),
            btn -> {
                FishyConfig.toggle(Key.BEACON_ALARM, false);
                btn.setMessage(getAlarmText());
                this.setFocused(null);
            }
        ));

        addDrawableChild(new FaButton(
            centerX, by, BTNW / 2, BTNH,
            getTimerText(),
            btn -> {
                FishyConfig.toggle(Key.HUD_TIMER_ENABLED, false);
                btn.setMessage(getTimerText());
                this.setFocused(null);
            }
        ));

        addDrawableChild(new FaButton(
            centerX + 100, by, 60, BTNH,
            Text.literal("Edit HUD").setStyle(Style.EMPTY.withColor(0xFF808080)),
            btn -> MinecraftClient.getInstance().setScreen(new HudEditScreen())
        ));

        by += 30;        

        trackBtnX = centerX - 100;
        trackBtnY = by;
        trackBtnW = BTNW;
        trackBtnH = BTNH;

        int ttw = textRenderer.getWidth(TRACKER_TEXT);
        TextFieldWidget trackerField = new TextFieldWidget(
        this.textRenderer, centerX - (ttw / 2), by, BTNW, BTNH, Text.literal(TRACKER_TEXT));
        trackerField.setText(TRACKER_TEXT);
        trackerField.setEditable(false);
        trackerField.setMaxLength(64);
        trackerField.setUneditableColor(0xD0AEFF);
        trackerField.setDrawsBackground(false);
        this.addDrawableChild(trackerField);  
        
        by += 30;

        addDrawableChild(new FaButton(
            centerX - 100, by, BTNW, BTNH,
            getTrackerText(),
            btn -> {
                FishyConfig.toggle(Key.PER_ITEM, false);
                TrackerUtils.refresh();
                btn.setMessage(getTrackerText());
            }
        ));        
        
        addDrawableChild(new FaButton(
            centerX + 100, by, 60, BTNH,
            getSackText(),
            btn -> {
                FishyConfig.toggle(Key.TRACK_SACK, false);
                SackDropParser.refresh();
                btn.setMessage(getSackText());
            }
        ));

        by += 30;

        minValueBtnX = centerX - 100;
        minValueBtnY = by;
        minValueBtnW = BTNW;
        minValueBtnH = BTNH;

        addDrawableChild(new FaButton(
            minValueBtnX, minValueBtnY, minValueBtnW, minValueBtnH,
            getMinValueToggleText(), 
            btn -> {
                FishyConfig.toggle("minValueFilter", false);
                TrackerDisplay.refreshDisplay();
                btn.setMessage(getMinValueToggleText());
            }
        ));

        float minValue = FishyConfig.getFloat("minItemValue", 0);
        minValueSlider = new CompactSlider(minValueBtnX + minValueBtnW + 10, by, minValue, 
            0, 100000, "%.0f", v -> {
                FishyConfig.setFloat("minItemValue", v);
                TrackerDisplay.refreshDisplay();
            });        

        by += 60;

        this.addDrawableChild(new FaButton(
            centerX - 100, by, 80, BTNH,
            Text.literal("Back"),
            btn -> MinecraftClient.getInstance().setScreen(new FishyAddonsScreen())
        ));

        this.addDrawableChild(new FaButton(
            centerX + 20, by, 80, BTNH,
            Text.literal("Close"),
            btn -> MinecraftClient.getInstance().setScreen(null)
        ));
    }

    private Text getAlarmText() {
        return GuiUtil.onOffLabel("Ingame Alarm", FishyConfig.getState(Key.BEACON_ALARM, false));
    }

    private Text getTimerText() {
        return GuiUtil.onOffLabel("HUD Timer", FishyConfig.getState(Key.HUD_TIMER_ENABLED, false));
    }

    private Text getPhantomText() {
        return GuiUtil.onOffLabel("Mute Phantoms", FishyConfig.getState(Key.MUTE_PHANTOM, false));
    }   
    
    private static Text getPetText() {
        return GuiUtil.onOffLabel("Pet Display", FishyConfig.getState(Key.HUD_PET_ENABLED, false));
    }

    private static Text getDynamicText() {
        boolean isEnabled = FishyConfig.getState(Key.HUD_PET_DYNAMIC, false);
        String title = "Dynamic";
        return Text.literal(title).styled(s -> s.withColor(isEnabled ? 0xCCFFCC : 0xFF8080));
    }

    private static Text getPetXpText() {
        boolean isEnabled = FishyConfig.getState(Key.HUD_PETXP, false);
        String title = "Include XP";
        return Text.literal(title).styled(s -> s.withColor(isEnabled ? 0xCCFFCC : 0xFF8080));
    }

    private static Text getDmgToggleText() {
        return GuiUtil.onOffLabel("Scale Crit Particles", FishyConfig.getState(Key.SCALE_CRIT, false));
    }    

    private static Text getMinValueToggleText() {
        return GuiUtil.onOffLabel("Minimum Item Value Filter", FishyConfig.getState("minValueFilter", false));
    }

    private static Text getTrackerText() {
        boolean isEnabled = FishyConfig.getState(Key.PER_ITEM, false);
        String title = "Coins per Item type";
        return Text.literal(title).styled(s -> s.withColor(isEnabled ? 0xCCFFCC : 0xFF8080));
    }       

    private static Text getSackText() {
        boolean isEnabled = FishyConfig.getState(Key.TRACK_SACK, false);
        String title = "Track Sack";
        return Text.literal(title).styled(s -> s.withColor(isEnabled ? 0xCCFFCC : 0xFF8080));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        // Render title text above the blur overlay
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 100); // Push forward in Z-depth
        String title = "Skyblock Settings";
        context.drawCenteredTextWithShadow(this.textRenderer, title, this.width / 2, this.height / 4 - 40, 0xFF55FFFF);
        context.getMatrices().pop();
        
        super.render(context, mouseX, mouseY, delta);

        if (dmgScaleSlider != null) {
            dmgScaleSlider.render(context, mouseX, mouseY);
            String percentageText = dmgScaleSlider.getPercentageText();
            context.drawText(this.textRenderer, percentageText, 
                dmgScaleSlider.getX() + CompactSlider.getWidth() + 5, 
                dmgScaleSlider.getY() + 2, 0xFFFFFFFF, false);
        }
        
        if (minValueSlider != null) {
            minValueSlider.render(context, mouseX, mouseY);
            String valueText = minValueSlider.getPercentageText();
            context.drawText(this.textRenderer, valueText, 
                minValueSlider.getX() + CompactSlider.getWidth() + 5, 
                minValueSlider.getY() + 2, 0xFFFFFFFF, false);
        }
     
        if (mouseX >= petBtnX && mouseX <= petBtnX + petBtnW && mouseY >= petBtnY && mouseY <= petBtnY + petBtnH) {
            List<Text> tooltipLines = Arrays.asList(
                Text.literal("Renders Tablist info:"),
                Text.literal("- §8Dynamic scans ~once per second and"),
                Text.literal("   §8enables xp display."),
                Text.literal("- §8Without, updates on chat messages.")
            );
            GuiUtil.fishyTooltip(context, this.textRenderer, tooltipLines, mouseX, mouseY);
        }

        if (mouseX >= trackBtnX && mouseX <= trackBtnX + trackBtnW && mouseY >= trackBtnY && mouseY <= trackBtnY + trackBtnH) {
            List<Text> tooltipLines = Arrays.asList(
                Text.literal("Profit Tracker:"),
                Text.literal("- §8Shows the total profit"),
                Text.literal("   §8per item type"),
                Text.literal("- §8Refresh to update prices"),
                Text.literal("   §8from api")
            );
            GuiUtil.fishyTooltip(context, this.textRenderer, tooltipLines, mouseX, mouseY);
        }  
        
        if (mouseX >= dmgBtnX && mouseX <= dmgBtnX + dmgBtnW && mouseY >= dmgBtnY && mouseY <= dmgBtnY + dmgBtnH) {
            List<Text> tooltipLines = Arrays.asList(
                Text.literal("Scale Damage Particles:"),
                Text.literal("- §8Toggle scaling"),
                Text.literal("   §8on/off"),
                Text.literal("- §8Color = (custom) redstone color")
            );
            GuiUtil.fishyTooltip(context, this.textRenderer, tooltipLines, mouseX, mouseY);
        }
        

        if (dmgScaleSlider != null && dmgScaleSlider.isMouseOver(mouseX, mouseY)) {
            List<Text> tooltipLines = Arrays.asList(
                Text.literal("Damage Particle Scale:"),
                Text.literal("- §8Adjusts the size of"),
                Text.literal("   §8damage particles"),
                Text.literal("- §8Current scale: " + dmgScaleSlider.getPercentageText())
            );
            GuiUtil.fishyTooltip(context, this.textRenderer, tooltipLines, mouseX, mouseY);
        }

        if (mouseX >= minValueBtnX && mouseX <= minValueBtnX + minValueBtnW && mouseY >= minValueBtnY && mouseY <= minValueBtnY + minValueBtnH) {
            List<Text> tooltipLines = Arrays.asList(
                Text.literal("Minimum Item Value Filter:"),
                Text.literal("- §8Toggle filtering"),
                Text.literal("   §8on/off"),
                Text.literal("- §8Hides items below threshold")
            );
            GuiUtil.fishyTooltip(context, this.textRenderer, tooltipLines, mouseX, mouseY);
        }

        if (minValueSlider != null && minValueSlider.isMouseOver(mouseX, mouseY)) {
            List<Text> tooltipLines = Arrays.asList(
                Text.literal("Minimum Item Value:"),
                Text.literal("- §8Items below this value"),
                Text.literal("   §8will be filtered out"),
                Text.literal("- §8Current value: " + minValueSlider.getPercentageText())
            );
            GuiUtil.fishyTooltip(context, this.textRenderer, tooltipLines, mouseX, mouseY);
        }
    } 
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (dmgScaleSlider != null && dmgScaleSlider.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (minValueSlider != null && minValueSlider.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dmgScaleSlider != null && dmgScaleSlider.mouseReleased(button)) {
            return true;
        }
        if (minValueSlider != null && minValueSlider.mouseReleased(button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dmgScaleSlider != null && dmgScaleSlider.mouseDragged(mouseX, button)) {
            return true;
        }
        if (minValueSlider != null && minValueSlider.mouseDragged(mouseX, button)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
}
