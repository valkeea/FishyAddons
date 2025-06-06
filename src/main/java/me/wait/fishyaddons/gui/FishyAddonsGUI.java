package me.wait.fishyaddons.gui;

import me.wait.fishyaddons.FishyAddons;
import me.wait.fishyaddons.config.ConfigHandler;
import me.wait.fishyaddons.util.ScoreboardUtils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

public class FishyAddonsGUI extends GuiScreen {
    private GuiButton toggleFishyLavaButton;
    private GuiButton openKeybindListButton;
    private CustomGuiSlider particleColorSlider;
    private GuiButton openCommandAliasesButton;
    private GuiButton closeButton;

    @Override
    public void initGui() {
        int centerX = width / 2;
        int centerY = height / 2;

        GuiButton visualSettings = new GuiButton(0, centerX - 100, centerY - 80, 200, 20, "Visual Settings");
        visualSettings.packedFGColour = 0xFFE2CAE9;
        buttonList.add(visualSettings);

        openKeybindListButton = new GuiButton(1, centerX - 100, centerY - 50, 200, 20, "Keybind List");
        openKeybindListButton.packedFGColour = 0xFFE2CAE9;
        buttonList.add(openKeybindListButton);

        openCommandAliasesButton = new GuiButton(4, centerX - 100, centerY - 20, 200, 20, "Command Aliases");
        openCommandAliasesButton.packedFGColour = 0xFFE2CAE9;
        buttonList.add(openCommandAliasesButton);

        GuiButton faProtectButton = new GuiButton(5, centerX - 100, centerY + 10, 200, 20, "Item Protection");
        faProtectButton.packedFGColour = 0xFFE2CAE9;
        buttonList.add(faProtectButton);

        particleColorSlider = new CustomGuiSlider(3, centerX - 100, centerY + 40, 200, 20, "Custom Particle Color", 0, 4, ConfigHandler.getCustomParticleColorIndex());
        buttonList.add(particleColorSlider);

        closeButton = new GuiButton(2, centerX - 100, centerY + 70, 200, 20, "Close");
        buttonList.add(closeButton);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            mc.displayGuiScreen(new VisualSettingsGUI());
        } else if (button.id == 1) {
            mc.displayGuiScreen(new KeybindListGUI());
        } else if (button.id == 4) {
            mc.displayGuiScreen(new CommandListGUI());
        } else if (button.id == 5) {
            mc.displayGuiScreen(new SellProtectionGUI());
        } else if (button.id == 2) {
            mc.displayGuiScreen(null);
            ScoreboardUtils.logSidebar();
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
        drawCenteredString(fontRendererObj, "FishyAddons", width / 2, height / 2 - 110, 0xFF55FFFF); // Adjusted header position
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}