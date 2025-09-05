package me.valkeea.fishyaddons.gui;

import java.util.Arrays;
import java.util.List;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.handler.ChatTimers;
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
    private static final String GALATEA_TEXT = "Timers & Alerts";
    private static final String TRACKER_TEXT = "Profit Tracker";
    private CompactSlider dmgScaleSlider;
    private CompactSlider minValueSlider;
    private CompactSlider hotspotDistanceSlider;
    private int petBtnX, petBtnY, petBtnW, petBtnH;
    private int trackBtnX, trackBtnY, trackBtnW, trackBtnH;  
    private int dmgBtnX, dmgBtnY, dmgBtnW, dmgBtnH;
    private int minValueBtnX, minValueBtnY, minValueBtnW, minValueBtnH;
    private int cakeBtnX, cakeBtnY, cakeBtnW, cakeBtnH;

    public SbScreen() {
        super(Text.literal("Skyblock Settings"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int by = centerY - 140;

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

        by += 20;

        addDrawableChild(new FaButton(
            centerX - 100, by, BTNW, BTNH,
            getHotspotText(),
            btn -> {
                FishyConfig.toggle(Key.HIDE_HOTSPOT, false);
                btn.setMessage(getHotspotText());
                SkyblockCleaner.refresh();
                btn.setFocused(false);
            }
        ));

        float currentDistance = FishyConfig.getFloat(Key.HOTSPOT_DISTANCE, 7.0f);
        hotspotDistanceSlider = new CompactSlider(
            centerX + 110, by + 5, currentDistance,
            0.0f, 20.0f, "%.1f",
            newDistance -> {
                FishyConfig.setFloat(Key.HOTSPOT_DISTANCE, newDistance);
                SkyblockCleaner.refresh();
            }
        );

        by += 20;

        addDrawableChild(new FaButton(
            centerX - 100, by, BTNW, BTNH,
            getRainText(),
            btn -> {
                FishyConfig.toggle(Key.RAIN_NOTI, false);
                me.valkeea.fishyaddons.handler.WeatherTracker.track();
                btn.setMessage(getRainText());
            }
        ));

        by += 20;

        addDrawableChild(new FaButton(
            centerX - 100, by, BTNW, BTNH,
            getEqText(),
            btn -> {
                FishyConfig.toggle(Key.EQ_DISPLAY, false);
                me.valkeea.fishyaddons.hud.EqDisplay.reset();
                btn.setMessage(getEqText());
            }
        ));

        by += 20;

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

        by += 20;

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
        dmgScaleSlider = new CompactSlider(dmgBtnX + dmgBtnW + 10, by + 5, dmgScale, 
            0.05f, 1.5f, "%.2f", ParticleVisuals::setDmgScale);

        by += 30;

        int gtw = textRenderer.getWidth(GALATEA_TEXT);

        TextFieldWidget galateaTextField = new TextFieldWidget(
            this.textRenderer, centerX - (gtw / 2), by, BTNW, BTNH, Text.literal(GALATEA_TEXT));
        galateaTextField.setText(GALATEA_TEXT);
        galateaTextField.setEditable(false);
        galateaTextField.setMaxLength(64);
        galateaTextField.setUneditableColor(0x6DE6B5);
        galateaTextField.setDrawsBackground(false);
        this.addDrawableChild(galateaTextField); 

        by += 30;

        addDrawableChild(new FaButton(
            centerX - 100, by, BTNW / 2, BTNH,
            getAlarmText(),
            btn -> {
                FishyConfig.toggle(Key.BEACON_ALARM, false);
                ChatTimers.getInstance().refresh();
                btn.setMessage(getAlarmText());
                this.setFocused(null);
            }
        ));

        addDrawableChild(new FaButton(
            centerX, by, BTNW / 2, BTNH,
            getTimerText(),
            btn -> {
                FishyConfig.toggle(Key.HUD_TIMER_ENABLED, false);
                ChatTimers.getInstance().refresh();
                btn.setMessage(getTimerText());
                this.setFocused(null);
            }
        ));

        addDrawableChild(new FaButton(
            centerX + 100, by, 60, BTNH,
            Text.literal("Edit HUD").setStyle(Style.EMPTY.withColor(0xFF808080)),
            btn -> MinecraftClient.getInstance().setScreen(new HudEditScreen())
        ));

        by += 20;
        cakeBtnX = centerX - 100;
        cakeBtnY = by;
        cakeBtnW = BTNW / 2;
        cakeBtnH = BTNH;

        addDrawableChild(new FaButton(
            cakeBtnX, cakeBtnY, cakeBtnW, cakeBtnH,
            getCakeAlarmText(),
            btn -> {
                FishyConfig.toggle(Key.CAKE_NOTI, false);
                btn.setMessage(getCakeAlarmText());
                this.setFocused(null);
            }
        ));

        addDrawableChild(new FaButton(
            centerX, by, BTNW / 2, BTNH,
            getCakeHudText(),
            btn -> {
                FishyConfig.toggle(Key.HUD_CENTURY_CAKE_ENABLED, false);
                btn.setMessage(getCakeHudText());
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
        trackerField.setUneditableColor(0x6DE6B5);
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

        by += 20;

        minValueBtnX = centerX - 100;
        minValueBtnY = by;
        minValueBtnW = BTNW;
        minValueBtnH = BTNH;

        addDrawableChild(new FaButton(
            minValueBtnX, minValueBtnY, minValueBtnW, minValueBtnH,
            getMinValueToggleText(), 
            btn -> {
                FishyConfig.toggle(Key.VALUE_FILTER, false);
                TrackerDisplay.refreshDisplay();
                btn.setMessage(getMinValueToggleText());
            }
        ));

        float minValue = FishyConfig.getFloat("minItemValue", 0);
        minValueSlider = new CompactSlider(minValueBtnX + minValueBtnW + 10, by + 5, minValue, 
            0, 100000, "%.0f", v -> {
                FishyConfig.setFloat("minItemValue", v);
                TrackerDisplay.refreshDisplay();
        });    
        
        by += 20;

        addDrawableChild(new FaButton(
            centerX - 100, by, BTNW, BTNH,
            getBookAlertText(),
            btn -> {
                FishyConfig.toggle(Key.BOOK_DROP_ALERT, true);
                btn.setMessage(getBookAlertText());
            }
        ));

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
        boolean isEnabled = FishyConfig.getState(Key.BEACON_ALARM, false);
        String title = "Moonglade Beacon";
        return Text.literal(title).styled(s -> s.withColor(isEnabled ? 0xCCFFCC : 0xFF8080));
    }

    private Text getTimerText() {
        return GuiUtil.onOffLabel("HUD Timer", FishyConfig.getState(Key.HUD_TIMER_ENABLED, false));
    }

    private Text getCakeAlarmText() {
        boolean isEnabled = FishyConfig.getState(Key.CAKE_NOTI, true);
        String title = "Century Cake";
        return Text.literal(title).styled(s -> s.withColor(isEnabled ? 0xCCFFCC : 0xFF8080));
    }

    private Text getCakeHudText() {
        return GuiUtil.onOffLabel("Cake HUD", FishyConfig.getState(Key.HUD_CENTURY_CAKE_ENABLED, false));
    }

    private Text getPhantomText() {
        return GuiUtil.onOffLabel("Mute Phantoms", FishyConfig.getState(Key.MUTE_PHANTOM, false));
    }  
    
    private Text getHotspotText() {
        return GuiUtil.onOffLabel("Hide Hotspot", FishyConfig.getState(Key.HIDE_HOTSPOT, false));
    }

    private Text getRainText() {
        return GuiUtil.onOffLabel("Rain Tracker", FishyConfig.getState(Key.RAIN_NOTI, true));
    }

    private Text getEqText() {
        return GuiUtil.onOffLabel("Equipment Display", FishyConfig.getState(Key.EQ_DISPLAY, true));
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
        return GuiUtil.onOffLabel("Scale Invisibug Particles", FishyConfig.getState(Key.SCALE_CRIT, false));
    }    

    private static Text getMinValueToggleText() {
        return GuiUtil.onOffLabel("Minimum Item Value Filter", FishyConfig.getState(Key.VALUE_FILTER, false));
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

    private static Text getBookAlertText() {
        boolean isEnabled = FishyConfig.getState(Key.BOOK_DROP_ALERT, true);
        String title = "Send Tracked Books in Chat";
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
        
        if (hotspotDistanceSlider != null) {
            hotspotDistanceSlider.render(context, mouseX, mouseY);
            String distanceText = hotspotDistanceSlider.getValue() <= 0.0f ? "Always" : hotspotDistanceSlider.getPercentageText() + " blocks";
            context.drawText(this.textRenderer, distanceText, 
                hotspotDistanceSlider.getX() + CompactSlider.getWidth() + 5, 
                hotspotDistanceSlider.getY() + 2, 0xFFFFFFFF, false);
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
                Text.literal("Profit Tracker (WIP):"),
                Text.literal("- §8Tracks most chat, sacks and inventory"),
                Text.literal("  §8for important mobs"),
                Text.literal("- §8If a profile is selected, the data will auto-save."),
                Text.literal("  §8otherwise, data is per-session."),
                Text.literal("- §8Use /fa profit to see commands")
            );
            GuiUtil.fishyTooltip(context, this.textRenderer, tooltipLines, mouseX, mouseY);
        }  
        
        if (mouseX >= dmgBtnX && mouseX <= dmgBtnX + dmgBtnW && mouseY >= dmgBtnY && mouseY <= dmgBtnY + dmgBtnH) {
            List<Text> tooltipLines = Arrays.asList(
                Text.literal("Scale Damage Particles:"),
                Text.literal("- §8Toggle scaling"),
                Text.literal("   §8on/off"),
                Text.literal("- §8Only enabled on Galatea"),
                Text.literal("- §8Color = (custom) redstone color")
            );
            GuiUtil.fishyTooltip(context, this.textRenderer, tooltipLines, mouseX, mouseY);
        }

        if (mouseX >= cakeBtnX && mouseX <= cakeBtnX + cakeBtnW && mouseY >= cakeBtnY && mouseY <= cakeBtnY + cakeBtnH) {
            List<Text> tooltipLines = Arrays.asList(
                Text.literal("Century Cake Tracker:"),
                Text.literal("- §8Tracks all Century Cakes and"),
                Text.literal("   §8notifies if one expires"),
                Text.literal("- §8Timestamps are taken from chat so"),
                Text.literal("   §8the timer is config-dependent.")
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

        if (hotspotDistanceSlider != null && hotspotDistanceSlider.isMouseOver(mouseX, mouseY)) {
            List<Text> tooltipLines = Arrays.asList(
                Text.literal("Hotspot Hide Distance:"),
                Text.literal("- §80 blocks = Always hide"),
                Text.literal("- §8>0 blocks = Hide within range"),
                Text.literal("- §8Current: " + (hotspotDistanceSlider.getValue() <= 0.0f ? "Always" : hotspotDistanceSlider.getPercentageText() + " blocks"))
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
        if (hotspotDistanceSlider != null && hotspotDistanceSlider.mouseClicked(mouseX, mouseY, button)) {
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
        if (hotspotDistanceSlider != null && hotspotDistanceSlider.mouseReleased(button)) {
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
        if (hotspotDistanceSlider != null && hotspotDistanceSlider.mouseDragged(mouseX, button)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
}
