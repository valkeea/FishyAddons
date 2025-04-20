package me.wait.fishyaddons.gui;

import me.wait.fishyaddons.config.ConfigHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

public class CustomGuiSlider extends GuiButton {
    private float sliderValue;
    private boolean dragging;
    private final float minValue;
    private final float maxValue;
    private final String prefix;

    public CustomGuiSlider(int id, int x, int y, int width, int height, String prefix, float minValue, float maxValue, float initialValue) {
        super(id, x, y, width, height, prefix);
        this.prefix = prefix;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.sliderValue = (initialValue - minValue) / (maxValue - minValue);
        updateDisplayString();
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (this.visible) {
            mc.getTextureManager().bindTexture(buttonTextures);
            drawTexturedModalRect(this.xPosition, this.yPosition, 0, 46 + (this.enabled ? 0 : 20), this.width / 2, this.height);
            drawTexturedModalRect(this.xPosition + this.width / 2, this.yPosition, 200 - this.width / 2, 46 + (this.enabled ? 0 : 20), this.width / 2, this.height);

            int sliderX = this.xPosition + (int) (this.sliderValue * (this.width - 8));
            drawTexturedModalRect(sliderX, this.yPosition, 0, 66, 8, 20);

            this.drawCenteredString(mc.fontRendererObj, this.displayString, this.xPosition + this.width / 2, this.yPosition + (this.height - 8) / 2, this.packedFGColour);
        }
    }

    @Override
    protected void mouseDragged(Minecraft mc, int mouseX, int mouseY) {
        if (this.visible) {
            if (this.dragging) {
                this.sliderValue = (float) (mouseX - (this.xPosition + 4)) / (float) (this.width - 8);
                this.sliderValue = Math.max(0.0F, Math.min(1.0F, this.sliderValue));
                updateDisplayString();
            }
        }
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (super.mousePressed(mc, mouseX, mouseY)) {
            this.sliderValue = (float) (mouseX - (this.xPosition + 4)) / (float) (this.width - 8);
            this.sliderValue = Math.max(0.0F, Math.min(1.0F, this.sliderValue));
            this.dragging = true;
            updateDisplayString();
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY) {
        this.dragging = false;
        int newIndex = Math.round(getValue());
        ConfigHandler.setCustomParticleColorIndex(newIndex);
        ConfigHandler.saveConfigIfNeeded();
    }

    public float getValue() {
        return this.minValue + (this.maxValue - this.minValue) * this.sliderValue;
    }

    public int getValueInt() {
        return Math.round(getValue());
    }

    private void updateDisplayString() {
        String[] particleColors = {"Off", "Aqua", "Mint", "Pink", "Light Blue"};
        int[] colorCodes = {0xAAAAAA, 0x00FFFF, 0x00FF99, 0xFF99FF, 0x99CCFF};
        int index = Math.round(getValue());
        this.displayString = this.prefix + ": " + particleColors[index];
        this.packedFGColour = colorCodes[index];
    }
}
