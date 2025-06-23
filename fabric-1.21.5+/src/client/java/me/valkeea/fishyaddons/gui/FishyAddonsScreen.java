package me.valkeea.fishyaddons.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import java.util.Arrays;
import java.util.List;

public class FishyAddonsScreen extends Screen {
    private static final int BTNW = 200;
    private static final int BTNH = 20;

    private int qolBtnX, qolBtnY, qolBtnW, qolBtnH;
    private int visualBtnX, visualBtnY, visualBtnW, visualBtnH;
    private int fgBtnX, fgBtnY, fgBtnW, fgBtnH;


    public FishyAddonsScreen() {
        super(Text.literal("FishyAddons"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        visualBtnX = centerX - 100;
        visualBtnY = centerY - 80;
        visualBtnW = BTNW;
        visualBtnH = BTNH;

        addDrawableChild(ButtonWidget.builder(
            Text.literal("Visual Settings").styled(style -> style.withColor(0xE2CAE9)),
            btn -> MinecraftClient.getInstance().setScreen(new VisualSettingsScreen())
        ).dimensions(visualBtnX, visualBtnY, visualBtnW, visualBtnH).build());

        qolBtnX = centerX - 100;
        qolBtnY = centerY - 50;
        qolBtnW = BTNW;
        qolBtnH = BTNH;
        
        addDrawableChild(ButtonWidget.builder(
            Text.literal("General Qol").styled(style -> style.withColor(0xE2CAE9)),
            btn -> MinecraftClient.getInstance().setScreen(new QolScreen())
        ).dimensions(qolBtnX, qolBtnY, qolBtnW, qolBtnH).build());

        addDrawableChild(ButtonWidget.builder(
            Text.literal("Skills").styled(style -> style.withColor(0xE2CAE9)),
            btn -> MinecraftClient.getInstance().setScreen(new SkillScreen())
        ).dimensions(centerX - 100, centerY - 20, BTNW, BTNH).build());
        
        fgBtnX = centerX - 100;
        fgBtnY = centerY + 10;
        fgBtnW = BTNW;
        fgBtnH = BTNH;

        addDrawableChild(ButtonWidget.builder(
            Text.literal("FA Safeguard").styled(style -> style.withColor(0xE2CAE9)),
            btn -> MinecraftClient.getInstance().setScreen(new SafeguardScreen())
        ).dimensions(fgBtnX, fgBtnY, fgBtnW, fgBtnH).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), btn -> 
            MinecraftClient.getInstance().setScreen(null)
        ).dimensions(centerX - 100, centerY + 40, BTNW, BTNH).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, "FishyAddons", this.width / 2, this.height / 2 - 110, 0xFF55FFFF);
        super.render(context, mouseX, mouseY, delta);

        // Tooltip for General Qol
        if (mouseX >= qolBtnX && mouseX <= qolBtnX + qolBtnW && mouseY >= qolBtnY && mouseY <= qolBtnY + qolBtnH) {
            List<Text> tooltipLines = Arrays.asList(
                Text.literal("General Qol:"),
                Text.literal("- §8Custom Keybinds"),
                Text.literal("- §8Command Aliases"),
                Text.literal("- §8Modify Chat"),
                Text.literal("- §8Custom F5/Camera Settings"),
                Text.literal("- §8Ping HUD display"),
                Text.literal("- §8Clean Wither Impact"),
                Text.literal("- §8Mute Phantoms"),
                Text.literal("- §8Draw Coordinate Beacons")            

            );
            GuiUtil.fishyTooltip(context, this.textRenderer, tooltipLines, mouseX, mouseY);
        }

        // Tooltip for Visual Settings
        if (mouseX >= visualBtnX && mouseX <= visualBtnX + visualBtnW && mouseY >= visualBtnY && mouseY <= visualBtnY + visualBtnH) {
            List<Text> tooltipLines = Arrays.asList(
                Text.literal("Visual Settings:"),
                Text.literal("- §8Custom Redstone Particles"),
                Text.literal("- §8Island Texture Toggles (1.8.9 only)"),
                Text.literal("- §8Lava Fog Removal")
            );
            GuiUtil.fishyTooltip(context, this.textRenderer, tooltipLines, mouseX, mouseY);
        }

        // Tooltip for FA Safeguard
        if (mouseX >= fgBtnX && mouseX <= fgBtnX + fgBtnW && mouseY >= fgBtnY && mouseY <= fgBtnY + fgBtnH) {
            List<Text> tooltipLines = Arrays.asList(
                Text.literal("FishyAddons item safeguard:"),
                Text.literal("- §8Drop/Sell Protection"),
                Text.literal("- §8Slotlocking and -binding"),
                Text.literal("- §8Toggle chat, sound and visual cues")
            );
            GuiUtil.fishyTooltip(context, this.textRenderer, tooltipLines, mouseX, mouseY);
        }
    }
}