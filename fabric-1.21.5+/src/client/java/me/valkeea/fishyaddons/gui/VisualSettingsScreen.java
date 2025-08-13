package me.valkeea.fishyaddons.gui;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.handler.ParticleVisuals;
import me.valkeea.fishyaddons.handler.ResourceHandler;
import me.valkeea.fishyaddons.handler.XpColor;
import me.valkeea.fishyaddons.tool.FishyMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;


public class VisualSettingsScreen extends Screen {
    private static final int BTNW = 200;
    private static final int BTNH = 20;
    private final Map<ButtonWidget, String> islandButtons = new HashMap<>();

    private int wtrBtnX, wtrBtnY, wtrBtnW, wtrBtnH;
    private int lavaBtnX, lavaBtnY, lavaBtnW, lavaBtnH;

    public VisualSettingsScreen() {
        super(Text.literal("Visual Settings"));
    }

    @Override
    protected void init() {
        this.clearChildren();
        islandButtons.clear();

        int centerX = this.width / 2;
        int y = this.height / 4;
        
        addDrawableChild(new FaButton(
            centerX - 300, y, BTNW, BTNH,
            getFontText(),
            btn -> {
                FishyConfig.toggle("hdFont", false);
                ResourceHandler.updateFontPack();
                btn.setMessage(getFontText());
            }
        ));

        lavaBtnX = centerX - 100;
        lavaBtnY = y;
        lavaBtnW = BTNW;
        lavaBtnH = BTNH;
        
        addDrawableChild(new FaButton(
            lavaBtnX, lavaBtnY, lavaBtnW, lavaBtnH,
            getLavaToggleText(),
            btn -> {
                FishyConfig.toggle(Key.FISHY_LAVA, false);
                btn.setMessage(getLavaToggleText());
            }
        ));
        
        addDrawableChild(new FaButton(
            centerX + 100, y, BTNW, BTNH,
            getGuiText(),
            btn -> {
                FishyConfig.toggle("fishyGui", false);
                ResourceHandler.updateGuiPack();
                btn.setMessage(getGuiText());
            }
        ));

        y += 20;

        wtrBtnX = centerX - 100;
        wtrBtnY = y;
        wtrBtnW = BTNW;
        wtrBtnH = BTNH;
        
        addDrawableChild(new FaButton(
            wtrBtnX, wtrBtnY, wtrBtnW, wtrBtnH,
            getWaterToggleText(),
            btn -> {
                FishyConfig.toggle(Key.FISHY_WATER, false);
                btn.setMessage(getWaterToggleText());
            }
        ));

        y += 30;

        addDrawableChild(new ParticleColorSlider(centerX - 100, y, BTNW, BTNH,
        "Redstone Particle Color", 0, 4, FishyConfig.getCustomParticleColorIndex())); 

        addDrawableChild(new FaButton(
            centerX + 100, y, 60, BTNH,
            getCustomButtonText(),
            btn -> MinecraftClient.getInstance().setScreen(new ColorPickerScreen(this, ParticleVisuals.getActiveParticleColor(), color -> {
                ParticleVisuals.setCustomColor(color);
                btn.setMessage(getCustomButtonText());
            }))
        ));   
        y += 30;

        addDrawableChild(new ThemeModeSlider(centerX - 100, y, BTNW, BTNH));
        y += 30;

        addDrawableChild(new FaButton(
            centerX - 100, y, BTNW, BTNH,
            getXpColorText(),
            btn -> {
                XpColor.toggle();
                XpColor.refresh();
                btn.setMessage(getXpColorText());
            }
        ));

        addDrawableChild(new FaButton(
            centerX + 100, y, 60, BTNH,
            getCustomXpColorText(),
            btn -> MinecraftClient.getInstance().setScreen(new ColorPickerScreen(this, ColorPickerScreen.intToRGB(XpColor.get()), color -> {
                XpColor.set(ColorPickerScreen.rgbToInt(color));
                btn.setMessage(getCustomXpColorText());
            }))
        ));

        addDrawableChild(new FaButton(
            centerX + 160, y, 60, BTNH,
            getOutlineText(),
            btn -> {
                XpColor.toggleOutline();
                XpColor.refresh();
                btn.setMessage(getOutlineText());
            }
        ));

        y += 60;

        addDrawableChild(new FaButton(
            centerX - 100, y, 80, 20,
            Text.literal("Back"),
            btn -> MinecraftClient.getInstance().setScreen(new FishyAddonsScreen(
            ))
        ));

        addDrawableChild(new FaButton(
            centerX + 20, y, 80, 20,
            Text.literal("Close"),
            btn -> MinecraftClient.getInstance().setScreen(null)
        ));
    }

    private Text getFontText() {
        return GuiUtil.onOffLabel("HD Font", FishyConfig.getState("hdFont", false));
    }    

    private Text getGuiText() {
        return GuiUtil.onOffLabel("Transparent Gui", FishyConfig.getState("fishyGui", false));
    }

    private Text getLavaToggleText() {
        return GuiUtil.onOffLabel("Clear Lava", FishyConfig.getState(Key.FISHY_LAVA, false));
    }

    private Text getWaterToggleText() {
        return GuiUtil.onOffLabel("Clear Water", FishyConfig.getState(Key.FISHY_WATER, false));
    }

    private static Text getCustomButtonText() {
        if ("custom".equals(FishyConfig.getParticleColorMode())) {
            float[] rgb = FishyConfig.getCustomParticleRGB();
            int r = (int)(rgb[0] * 255);
            int g = (int)(rgb[1] * 255);
            int b = (int)(rgb[2] * 255);
            int color = (0xFF << 24) | (r << 16) | (g << 8) | b;
            return Text.literal("Custom").styled(style -> style.withColor(color));
        } else {
            return Text.literal("Custom");
        }
    }

    private static Text getXpColorText() {
        return GuiUtil.onOffLabel("XP Color", FishyConfig.getState(Key.XP_COLOR_ON, false));
    }

    private static Text getCustomXpColorText() {
        return Text.literal("Custom")
            .styled(style -> style.withColor((XpColor.get())));
    }  
    
    private static Text getOutlineText() {
        boolean isEnabled = FishyConfig.getState(Key.XP_OUTLINE, false);
        String title = "Outline";
        return Text.literal(title).styled(s -> s.withColor(isEnabled ? 0xCCFFCC : 0xFF8080));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        String title = "Visual Settings";
        context.drawCenteredTextWithShadow(this.textRenderer, title, this.width / 2, this.height / 4 - 20, 0xFF55FFFF);
        super.render(context, mouseX, mouseY, delta);

        if (mouseX >= lavaBtnX && mouseX <= lavaBtnX + lavaBtnW && mouseY >= lavaBtnY && mouseY <= lavaBtnY + lavaBtnH) {
            List<Text> tooltipLines = Arrays.asList(
                Text.literal("Clear Lava:"),
                Text.literal("- §8Removes underlava fog overlay"),
                Text.literal("- §8Disabled outside Skyblock")
            );
            GuiUtil.fishyTooltip(context, this.textRenderer, tooltipLines, mouseX, mouseY);
        }

        if (mouseX >= wtrBtnX && mouseX <= wtrBtnX + wtrBtnW && mouseY >= wtrBtnY && mouseY <= wtrBtnY + wtrBtnH) {
            List<Text> tooltipLines = Arrays.asList(
                Text.literal("Clear Water:"),
                Text.literal("- §8Removes underwater overlay"),
                Text.literal("- §8Disabled outside Skyblock")
            );
            GuiUtil.fishyTooltip(context, this.textRenderer, tooltipLines, mouseX, mouseY);
        }
    }

    private static class ParticleColorSlider extends ThemedSlider {
        private final String prefix;
        private final int min;
        private final int max;

        public ParticleColorSlider(int x, int y, int width, int height, String prefix, int min, int max, int value) {
            super(x, y, width, height, Text.empty(), (value - min) / (double)(max - min));
            this.prefix = prefix;
            this.min = min;
            this.max = max;
            this.value = (value - min) / (double)(max - min);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            if ("custom".equals(FishyConfig.getParticleColorMode())) {
                float[] rgb = FishyConfig.getCustomParticleRGB();
                int r = (int)(rgb[0] * 255);
                int g = (int)(rgb[1] * 255);
                int b = (int)(rgb[2] * 255);
                int color = (0xFF << 24) | (r << 16) | (g << 8) | b;
                this.setMessage(Text.literal(prefix + ": Custom").styled(style -> style.withColor(color)));
            } else {
                int idx = (int)(this.value * (max - min) + min);
                String presetName;
                int color;
                switch (idx) {
                    case 1 -> { presetName = "Aqua"; color = 0xFF66FFFF; }
                    case 2 -> { presetName = "Mint"; color = 0xFF66FF99; }
                    case 3 -> { presetName = "Pink"; color = 0xFFFFCCFF; }
                    case 4 -> { presetName = "Prism"; color = 0xFFE5E5FF; }
                    default -> { presetName = "None"; color = 0xFFFFFFFF; }
                }
                this.setMessage(Text.literal(prefix + ": " + presetName).styled(style -> style.withColor(color)));
                FishyConfig.setParticleColorMode(idx == 0 ? "default" : "preset");
            }
        }

        @Override
        protected void applyValue() {
            int idx = (int)(this.value * (max - min) + min);
            FishyConfig.setCustomParticleColorIndex(idx);
            // If mode is custom, switch to preset when using the slider
            if ("custom".equals(FishyConfig.getParticleColorMode())) {
                FishyConfig.setParticleColorMode("preset");
            }
        }
    }
    
    private static class ThemeModeSlider extends ThemedSlider {
        private static final String[] MODES = {"default", "purple", "blue", "white", "green"};

        public ThemeModeSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Text.empty(), getInitialValue());
            updateMessage();
        }

        private static double getInitialValue() {
            String current = FishyMode.getTheme();
            for (int i = 0; i < MODES.length; i++) {
                if (MODES[i].equalsIgnoreCase(current)) return i / (double)(MODES.length - 1);
            }
            return 0.0;
        }

        @Override
        protected void updateMessage() {
            int idx = (int)(this.value * (MODES.length - 1) + 0.5);
            String mode = MODES[Math.clamp(idx, 0, MODES.length - 1)];
            String color = switch (mode) {
                case "purple" -> "BB80DF";
                case "blue" -> "A2C8FF";
                case "white" -> "E5E5FF";
                case "green" -> "A2FFA2";
                default -> "E2CAE9";
            };
            this.setMessage(Text.literal("Mod Gui Theme: " + capitalize(mode)).styled(
                style -> style.withColor(Integer.parseInt(color, 16))));
        }

        @Override
        protected void applyValue() {
            int idx = (int)(this.value * (MODES.length - 1) + 0.5);
            String mode = MODES[Math.clamp(idx, 0, MODES.length - 1)];
            FishyMode.setTheme(mode);
            updateMessage();
        }

        private static String capitalize(String s) {
            return s.substring(0, 1).toUpperCase() + s.substring(1);
        }
    }   
}