package me.valkeea.fishyaddons.gui;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.TextureConfig;
import me.valkeea.fishyaddons.handler.RedstoneColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;

public class VisualSettingsScreen extends Screen {
    private static final int BTNW = 200;
    private static final int BTNH = 20;
    private final Map<ButtonWidget, String> islandButtons = new HashMap<>();

    public VisualSettingsScreen() {
        super(Text.literal("Visual Settings"));
    }

    @Override
    protected void init() {
        this.clearChildren();
        islandButtons.clear();

        int centerX = this.width / 2;
        int y = this.height / 4;
        
        addDrawableChild(ButtonWidget.builder(getLavaToggleText(), btn -> {
            FishyConfig.toggle("fishyLava", false);
            FishyConfig.saveConfigIfNeeded();
            btn.setMessage(getLavaToggleText());
            this.setFocused(false);
        }).dimensions(centerX - 300, y, BTNW, BTNH).build());

        addDrawableChild(ButtonWidget.builder(getRetexStatusText(), btn -> {
            //TextureConfig.setRetexStatus(!TextureConfig.isRetexStatus());
            TextureConfig.save();
            btn.setMessage(getRetexStatusText());
        }).dimensions(centerX - 100, y, BTNW, BTNH).build());

        addDrawableChild(ButtonWidget.builder(getAllToggleText(), btn -> {
            TextureConfig.setAllToggled(!TextureConfig.isAllToggled());
            TextureConfig.save();
            btn.setMessage(getAllToggleText());
            // Update all island buttons
            islandButtons.forEach((button, island) -> button.setMessage(getIslandButtonText(island)));
        }).dimensions(centerX + 100, y, BTNW, BTNH).build());        

        y += 30;

        /*for (String island : RetexHandler.getKnownIslands()) {
            if ("default".equals(island) || "mineshaft".equals(island)) continue;
            ButtonWidget islandButton = addDrawableChild(ButtonWidget.builder(getIslandButtonText(island), btn -> {
                boolean isEnabled = TextureConfig.isIslandTextureEnabled(island);
                TextureConfig.toggleIslandTexture(island, !isEnabled);
                btn.setMessage(getIslandButtonText(island));
            }).dimensions(centerX - 100, y, 200, 20).build());
            islandButtons.put(islandButton, island);
            y += 24;
        }*/

        y += 40;

        addDrawableChild(new ParticleColorSlider(centerX - 100, y, BTNW, BTNH, "Redstone Particle Color", 0, 4, FishyConfig.getCustomParticleColorIndex()));  
        
        addDrawableChild(ButtonWidget.builder(getCustomButtonText(), btn -> {
            MinecraftClient.getInstance().setScreen(new ColorPickerScreen(this, RedstoneColor.getActiveParticleColor(), color -> {
                RedstoneColor.setCustomColor(color);
                FishyConfig.saveConfigIfNeeded();
                btn.setMessage(getCustomButtonText());
            }));
        }).dimensions(centerX + 100, y, 60, BTNH).build());

        y += 60;

        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), btn -> {
            MinecraftClient.getInstance().setScreen(new FishyAddonsScreen());
        }).dimensions(centerX - 100, y, 80, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), btn -> {
            MinecraftClient.getInstance().setScreen(null);
        }).dimensions(centerX + 20, y, 80, 20).build());
    }

    private Text getAllToggleText() {
        return GuiUtil.onOffLabel(Formatting.STRIKETHROUGH + "All Islands", TextureConfig.isAllToggled());
    }

    private Text getRetexStatusText() {
        return GuiUtil.onOffLabel(Formatting.STRIKETHROUGH + "Retexturing", TextureConfig.isRetexStatus());
    }

    private Text getLavaToggleText() {
        return GuiUtil.onOffLabel("Clear Lava", FishyConfig.getState("fishyLava", false));
    }

    private Text getIslandButtonText(String island) {
        boolean isEnabled = TextureConfig.isIslandTextureEnabled(island);
        String key = "gui.island_toggle." + island + (isEnabled ? ".enabled" : ".disabled");
        return Text.literal(key).styled(s -> s.withColor(isEnabled ? 0xCCFFCC : 0xFF8080));
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

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        String title = "Visual Settings";
        context.drawCenteredTextWithShadow(this.textRenderer, title, this.width / 2, this.height / 4 - 20, 0xFF55FFFF);

        super.render(context, mouseX, mouseY, delta);
    }

    private static class ParticleColorSlider extends SliderWidget {
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
}