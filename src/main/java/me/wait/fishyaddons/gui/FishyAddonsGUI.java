package me.wait.fishyaddons.gui;

import me.wait.fishyaddons.FishyAddons;
import me.wait.fishyaddons.config.ConfigHandler;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

public class FishyAddonsGUI extends GuiScreen {
    private GuiButton toggleFishyLavaButton;
    private GuiButton openKeybindListButton;
    private CustomGuiSlider particleColorSlider;
    private GuiButton openCommandAliasesButton;
    private GuiButton closeButton;

    private void updateToggleFishyLavaButton() {
        toggleFishyLavaButton.displayString = getToggleButtonText();
        toggleFishyLavaButton.packedFGColour = ConfigHandler.isFishyLavaEnabled() ? 0xFFCCFFCC : 0xFFFF8080;
    }

    @Override
    public void initGui() {
        int centerX = width / 2;
        int centerY = height / 2;

        toggleFishyLavaButton = new GuiButton(0, centerX - 100, centerY - 60, 200, 20, getToggleButtonText());
        toggleFishyLavaButton.packedFGColour = ConfigHandler.isFishyLavaEnabled() ? 0xFFCCFFCC : 0xFFFF8080;
        buttonList.add(toggleFishyLavaButton);

        openKeybindListButton = new GuiButton(1, centerX - 100, centerY - 30, 200, 20, "Keybind List");
        openKeybindListButton.packedFGColour = 0xFFE2CAE9;
        buttonList.add(openKeybindListButton);

        openCommandAliasesButton = new GuiButton(4, centerX - 100, centerY, 200, 20, "Command Aliases");
        openCommandAliasesButton.packedFGColour = 0xFFE2CAE9;
        buttonList.add(openCommandAliasesButton);

        particleColorSlider = new CustomGuiSlider(3, centerX - 100, centerY + 30, 200, 20, "Custom Particle Color", 0, 4, ConfigHandler.getCustomParticleColorIndex());
        buttonList.add(particleColorSlider);

        closeButton = new GuiButton(2, centerX - 100, centerY + 60, 200, 20, "Close");
        buttonList.add(closeButton);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            ConfigHandler.setFishyLavaEnabled(!ConfigHandler.isFishyLavaEnabled());
            ConfigHandler.saveConfigIfNeeded();
            updateToggleFishyLavaButton();
            toggleFishyLavaButton.packedFGColour = ConfigHandler.isFishyLavaEnabled() ? 0xFFCCFFCC : 0xFFFF8080;
        } else if (button.id == 1) {
            mc.displayGuiScreen(new KeybindListGUI());
        } else if (button.id == 4) {
            mc.displayGuiScreen(new CommandListGUI());    
        } else if (button.id == 2) {
            mc.displayGuiScreen(null);
        } else if (button.id == 3) {
            int newIndex = particleColorSlider.getValueInt();
            if (newIndex != ConfigHandler.getCustomParticleColorIndex()) { // Update only if the value has changed
                ConfigHandler.setCustomParticleColorIndex(newIndex);
                ConfigHandler.saveConfigIfNeeded();
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, "FishyAddons", width / 2, height / 2 - 90, 0xFF55FFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private String getToggleButtonText() {
        return ConfigHandler.isFishyLavaEnabled() ? "Clear Lava ON" : "Clear Lava OFF";
    }
}
