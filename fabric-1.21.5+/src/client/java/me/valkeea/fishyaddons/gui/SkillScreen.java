package me.valkeea.fishyaddons.gui;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.handler.SkyblockCleaner;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public class SkillScreen extends Screen {
    private static final int BTNW = 200;
    private static final int BTNH = 20;
    private TextFieldWidget galateaTextField;
    private Rect2i galateaRect;

    public SkillScreen() {
        super(Text.literal("Skill-related Settings (Foraging)"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int by = centerY - 80;

        galateaTextField = new TextFieldWidget(this.textRenderer, centerX, by, BTNW, BTNH, Text.literal("Moonglade Beacon"));
        galateaTextField.setText("Moonglade Beacon");
        galateaTextField.setEditable(true);
        galateaTextField.setMaxLength(64);
        galateaTextField.setEditableColor(0xFFFFFF);
        galateaTextField.setUneditableColor(0xAAAAAA);
        galateaTextField.setDrawsBackground(false);
        this.addDrawableChild(galateaTextField);
        galateaRect = new Rect2i(centerX - 100, by, BTNW, BTNH);      

        by += 30;

        this.addDrawableChild(new FaButton(
            centerX - 100, by, BTNW / 2, BTNH,
            getAlarmText(),
            btn -> {
                FishyConfig.toggle("beaconAlarm", false);
                btn.setMessage(getAlarmText());
                this.setFocused(null);
            }
        ));

        this.addDrawableChild(new FaButton(
            centerX, by, BTNW / 2, BTNH,
            getTimerText(),
            btn -> {
                FishyConfig.toggle("timerHud", false);
                btn.setMessage(getTimerText());
                this.setFocused(null);
            }
        ));

        this.addDrawableChild(new FaButton(
            centerX + 100, by, 60, BTNH,
            Text.literal("Edit HUD").setStyle(Style.EMPTY.withColor(0xFF808080)),
            btn -> MinecraftClient.getInstance().setScreen(new HudEditScreen())
        ));

        by += 30;

        this.addDrawableChild(new FaButton(
            centerX - 100, by, BTNW, BTNH,
            getPhantomText(),
            btn -> {
                FishyConfig.toggle("mutePhantom", false);
                btn.setMessage(getPhantomText());
                SkyblockCleaner.refresh();
                btn.setFocused(false);
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
        return GuiUtil.onOffLabel("Ingame Alarm", FishyConfig.getState("beaconAlarm", false));
    }

    private Text getTimerText() {
        return GuiUtil.onOffLabel("HUD Timer", FishyConfig.getState("timerHud", false));
    }

    private Text getPhantomText() {
        return GuiUtil.onOffLabel("Mute Phantoms", FishyConfig.getState("mutePhantom", false));
    }    

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        if (galateaRect != null) {
            context.fill(
                galateaRect.getX(),
                galateaRect.getY(),
                galateaRect.getX() + galateaRect.getWidth(),
                galateaRect.getY() + galateaRect.getHeight(),
                0x88000000
            );
        }

        String title = "Skill-related Settings (Foraging)";
        context.drawCenteredTextWithShadow(this.textRenderer, title, this.width / 2, this.height / 4 - 20, 0xFF55FFFF);

        super.render(context, mouseX, mouseY, delta);
    }    
}
