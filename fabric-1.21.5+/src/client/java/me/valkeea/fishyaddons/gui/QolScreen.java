package me.valkeea.fishyaddons.gui;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.handler.SkyblockCleaner;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import java.util.Arrays;
import java.util.List;


public class QolScreen extends Screen {
    private static final int BTNW = 200;
    private static final int BTNH = 20;

    private int cmdBtnX, cmdBtnY, cmdBtnW, cmdBtnH;
    private int keyBtnX, keyBtnY, keyBtnW, keyBtnH;
    private int chatBtnX, chatBtnY, chatBtnW, chatBtnH;
    private int f5BtnX, f5BtnY, f5BtnW, f5BtnH;
    private int hypBtnX, hypBtnY, hypBtnW, hypBtnH;
    private int rndrBtnX, rndrBtnY, rndrBtnW, rndrBtnH;


    public QolScreen() {
        super(Text.literal("General Qol"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        cmdBtnX = centerX - 300;
        cmdBtnY = centerY - 80;
        cmdBtnW = BTNW;
        cmdBtnH = BTNH;

        addDrawableChild(ButtonWidget.builder(
            Text.literal("Custom Commands").styled(style -> style.withColor(0xE2CAE9)),
            btn -> MinecraftClient.getInstance().setScreen(new TabbedListScreen(MinecraftClient.getInstance().currentScreen, TabbedListScreen.Tab.COMMANDS))
        ).dimensions(cmdBtnX, cmdBtnY, cmdBtnW, cmdBtnH).build());

        keyBtnX = centerX - 100;
        keyBtnY = cmdBtnY;
        keyBtnW = BTNW;
        keyBtnH = BTNH;

        addDrawableChild(ButtonWidget.builder(
            Text.literal("Custom Keybinds").styled(style -> style.withColor(0xE2CAE9)),
            btn -> MinecraftClient.getInstance().setScreen(new TabbedListScreen(MinecraftClient.getInstance().currentScreen, TabbedListScreen.Tab.KEYBINDS))
        ).dimensions(keyBtnX, keyBtnY, keyBtnW, keyBtnH).build());

        chatBtnX = centerX + 100;
        chatBtnY = cmdBtnY;
        chatBtnW = BTNW;
        chatBtnH = BTNH;

        addDrawableChild(ButtonWidget.builder(
            Text.literal("Modify Chat").styled(style -> style.withColor(0xE2CAE9)),
            btn -> MinecraftClient.getInstance().setScreen(new TabbedListScreen(MinecraftClient.getInstance().currentScreen, TabbedListScreen.Tab.CHAT) )
        ).dimensions(chatBtnX, chatBtnY, chatBtnW, chatBtnH).build());

        f5BtnX = centerX - 100;
        f5BtnY = centerY - 50;
        f5BtnW = BTNW;  
        f5BtnH = BTNH;

        addDrawableChild(ButtonWidget.builder(getSkipPerspectiveText(), btn -> {
            FishyConfig.toggle("skipPerspective", false);
            btn.setMessage(getSkipPerspectiveText());
            this.setFocused(null);
        }).dimensions(f5BtnX, f5BtnY, f5BtnW, f5BtnH).build());

        addDrawableChild(ButtonWidget.builder(getPingHudText(), btn -> {
            FishyConfig.toggle("pingHud", false);
            btn.setMessage(getPingHudText());
            this.setFocused(null);
        }).dimensions(centerX - 100, centerY - 20, BTNW, BTNH).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Edit Hud")
        .setStyle(Style.EMPTY.withColor(0xFF808080)), btn -> 
            MinecraftClient.getInstance().setScreen(new HudEditScreen())
        ).dimensions(centerX + 100, centerY - 20, 60, BTNH).build());

        hypBtnX = centerX - 100;
        hypBtnY = centerY + 10;
        hypBtnW = BTNW;
        hypBtnH = BTNH;

        addDrawableChild(ButtonWidget.builder(getHypText(), btn -> {
            FishyConfig.toggle("cleanHype", false);
            btn.setMessage(getHypText());
            SkyblockCleaner.refresh();
            this.setFocused(null);
        }).dimensions(hypBtnX, hypBtnY, hypBtnW, hypBtnH).build());

        addDrawableChild(ButtonWidget.builder(getPhantomText(), btn -> {
            FishyConfig.toggle("mutePhantom", false);
            btn.setMessage(getPhantomText());
            SkyblockCleaner.refresh();
            this.setFocused(null);
        }).dimensions(centerX - 100, hypBtnY + 30, BTNW, BTNH).build());

        rndrBtnX = centerX - 100;
        rndrBtnY = centerY + 70;
        rndrBtnW = BTNW;
        rndrBtnH = BTNH;

        addDrawableChild(ButtonWidget.builder(getRndrText(), btn -> {
            FishyConfig.toggle("renderCoords", false);
            btn.setMessage(getRndrText());
            this.setFocused(null);
        }).dimensions(rndrBtnX, rndrBtnY, rndrBtnW, rndrBtnH).build());

        addDrawableChild(ButtonWidget.builder(getColorButtonText(), btn -> {
            MinecraftClient.getInstance().setScreen(new ColorPickerScreen(this,
            ColorPickerScreen.intToRGB(FishyConfig.getHudColor("renderCoordsColor", 0xFF00FFFF)),
            rgb -> {
                FishyConfig.setHudColor("renderCoordsColor", ColorPickerScreen.rgbToInt(rgb));
                FishyConfig.saveConfigIfNeeded();
                btn.setMessage(getColorButtonText());
            }
        ));
        }).dimensions(centerX + 100, rndrBtnY, 70, BTNH).build());        

        addDrawableChild(ButtonWidget.builder(Text.literal("Back")
        .setStyle(Style.EMPTY.withColor(0xFF808080)), btn -> 
            MinecraftClient.getInstance().setScreen(new FishyAddonsScreen())
        ).dimensions(centerX - 100, centerY + 120, 80, BTNH).build());        

        addDrawableChild(ButtonWidget.builder(Text.literal("Close")
        .setStyle(Style.EMPTY.withColor(0xFF808080)), btn -> 
            MinecraftClient.getInstance().setScreen(null)
        ).dimensions(centerX + 20, centerY + 120, 80, BTNH).build());
    }

    private Text getSkipPerspectiveText() {
        return GuiUtil.onOffLabel("Skip Front Perspective", FishyConfig.getState("skipPerspective", false));
    }

    private Text getPingHudText() {
        return GuiUtil.onOffLabel("Ping Display", FishyConfig.getState("pingHud", false));
    }

    private Text getHypText() {
        return GuiUtil.onOffLabel("Clean Wither Impact", FishyConfig.getState("cleanHype", false));
    }

    private Text getPhantomText() {
        return GuiUtil.onOffLabel("Mute Phantoms", FishyConfig.getState("mutePhantom", false));
    }

    private Text getRndrText() {
        return GuiUtil.onOffLabel("Highlight Coordinates", FishyConfig.getState("renderCoords", false));
    }

    private static Text getColorButtonText() {
            float[] rgb = ColorPickerScreen.intToRGB(FishyConfig.getHudColor("renderCoordsColor", 0xFF00FFFF));
            int r = (int)(rgb[0] * 255);
            int g = (int)(rgb[1] * 255);
            int b = (int)(rgb[2] * 255);
            int color = (0xFF << 24) | (r << 16) | (g << 8) | b;
            return Text.literal("Color").styled(style -> style.withColor(color));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, "General Qol", this.width / 2, this.height / 2 - 110, 0xFF55FFFF);
        super.render(context, mouseX, mouseY, delta);

        if (mouseX >= cmdBtnX && mouseX <= cmdBtnX + cmdBtnW && mouseY >= cmdBtnY && mouseY <= cmdBtnY + cmdBtnH) {
            List<Text> tooltipLines = Arrays.asList(
                Text.literal("Command Aliases:"),
                Text.literal("- §8Create custom aliases"),
                Text.literal("  §8for existing commands")
            );
            GuiUtil.fishyTooltip(context, this.textRenderer, tooltipLines, mouseX, mouseY);
        }

        if (mouseX >= keyBtnX && mouseX <= keyBtnX + keyBtnW && mouseY >= keyBtnY && mouseY <= keyBtnY + keyBtnH) {
            List<Text> tooltipLines = Arrays.asList(
                Text.literal("Custom Keybinds:"),
                Text.literal("- §8Create custom keybinds"),
                Text.literal("  §8for existing commands")
            );
            GuiUtil.fishyTooltip(context, this.textRenderer, tooltipLines, mouseX, mouseY);
        }

        if (mouseX >= chatBtnX && mouseX <= chatBtnX + chatBtnW && mouseY >= chatBtnY && mouseY <= chatBtnY + chatBtnH) {
            List<Text> tooltipLines = Arrays.asList(
                Text.literal("Modify Chat:"),
                Text.literal("- §8Customize chat messages"),
                Text.literal("  §8and formatting")
            );
            GuiUtil.fishyTooltip(context, this.textRenderer, tooltipLines, mouseX, mouseY);
        }

        if (mouseX >= f5BtnX && mouseX <= f5BtnX + f5BtnW && mouseY >= f5BtnY && mouseY <= f5BtnY + f5BtnH) {
            List<Text> tooltipLines = Arrays.asList(
                Text.literal("Custom F5/Camera:"),
                Text.literal("- §8Skip front perspective")
            );
            GuiUtil.fishyTooltip(context, this.textRenderer, tooltipLines, mouseX, mouseY);
        }

        if (mouseX >= hypBtnX && mouseX <= hypBtnX + hypBtnW && mouseY >= hypBtnY && mouseY <= hypBtnY + hypBtnH) {
            List<Text> tooltipLines = Arrays.asList(
                Text.literal("Wither Impact:"),
                Text.literal("- §8Remove Explosion particles"),
                Text.literal("  §8and other effects")
            );
            GuiUtil.fishyTooltip(context, this.textRenderer, tooltipLines, mouseX, mouseY);
        }

        if (mouseX >= rndrBtnX && mouseX <= rndrBtnX + rndrBtnW && mouseY >= rndrBtnY && mouseY <= rndrBtnY + rndrBtnH) {
            List<Text> tooltipLines = Arrays.asList(
                Text.literal("Render a Beacon for /fa coords:"),
                Text.literal("- §8Supports other mods if formatted"),
                Text.literal("  §8as 'x:%, y:%, z:%'"),
                Text.literal("- §8Disappears after 1min")
            );
            GuiUtil.fishyTooltip(context, this.textRenderer, tooltipLines, mouseX, mouseY);
        }
    }
}
