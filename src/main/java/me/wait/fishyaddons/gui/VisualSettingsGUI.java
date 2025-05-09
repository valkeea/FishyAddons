package me.wait.fishyaddons.gui;

import me.wait.fishyaddons.config.ConfigHandler;
//import me.wait.fishyaddons.config.TextureConfig;
//import me.wait.fishyaddons.handlers.RetexHandler;
import me.wait.fishyaddons.util.GuiUtils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Arrays;

public class VisualSettingsGUI extends GuiScreen {
    private static final int BACK_BUTTON_ID = 0;
    private static final int SESSION_TOGGLE_BUTTON_ID = 1;
    private static final int COMPLETE_TOGGLE_BUTTON_ID = 2;
    private static final int LAVA_TOGGLE_BUTTON_ID = 3;
    private static final int ISLAND_BUTTON_OFFSET = 4;

    private GuiButton backButton;
    private GuiButton allToggleButton;
    private GuiButton statusToggleButton;
    private GuiButton toggleFishyLavaButton;

    private final Map<Integer, String> buttonIdToIsland = new HashMap<>();

    @Override
    public void initGui() {
        buttonList.clear();
        buttonIdToIsland.clear();

        int centerX = width / 2;
        int y = height / 4;

        allToggleButton = new GuiButton(SESSION_TOGGLE_BUTTON_ID, centerX - 150, y, 140, 20, getAllToggleText());
        buttonList.add(allToggleButton);

        statusToggleButton = new GuiButton(COMPLETE_TOGGLE_BUTTON_ID, centerX + 10, y, 140, 20, getRetexStatusText());
        buttonList.add(statusToggleButton);

        y += 30;

        toggleFishyLavaButton = new GuiButton(LAVA_TOGGLE_BUTTON_ID, centerX - 70, y, 140, 20, getLavaToggleText());
        buttonList.add(toggleFishyLavaButton);

        y += 30;

        int id = ISLAND_BUTTON_OFFSET;
        for (String island : RetexHandler.getKnownIslands()) {
            if ("default".equals(island)) continue; // Skip "default"
            GuiButton islandButton = new GuiButton(id, centerX - 100, y, 200, 20, getIslandButtonText(island));
            buttonList.add(islandButton);
            buttonIdToIsland.put(id, island);

            y += 24;
            id++;
        }

        backButton = new GuiButton(BACK_BUTTON_ID, centerX - 100, height - 30, 200, 20, "Back");
        buttonList.add(backButton);
    }

    private String getAllToggleText() {
        return TextureConfig.isAllToggled() ? "All §aON" : "All §cOFF";
    }

    private String getRetexStatusText() {
        return TextureConfig.isRetexStatus() ? "Retexturing status §aON" : "Retexturing status §cOFF";
    }

    private String getLavaToggleText() {
        return ConfigHandler.isFishyLavaEnabled() ? "Clear Lava §aON" : "Clear Lava §cOFF";
    }

    private String getIslandButtonText(String island) {
        boolean isEnabled = TextureConfig.isIslandTextureEnabled(island);
        String key = "gui.island_toggle." + island + (isEnabled ? ".enabled" : ".disabled");
        return I18n.format(key);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == BACK_BUTTON_ID) {
            mc.displayGuiScreen(new FishyAddonsGUI());
        } else if (button.id == SESSION_TOGGLE_BUTTON_ID) {
            TextureConfig.setAllToggled(!TextureConfig.isAllToggled());
            TextureConfig.save();
            button.displayString = getAllToggleText();

            for (Map.Entry<Integer, String> entry : buttonIdToIsland.entrySet()) {
                int buttonId = entry.getKey();
                String island = entry.getValue();
                GuiButton islandButton = buttonList.stream()
                        .filter(b -> b.id == buttonId)
                        .findFirst()
                        .orElse(null);
                if (islandButton != null) {
                    islandButton.displayString = getIslandButtonText(island);
                }
            }
        } else if (button.id == COMPLETE_TOGGLE_BUTTON_ID) {
            TextureConfig.setRetexStatus(!TextureConfig.isRetexStatus());
            TextureConfig.save();
            button.displayString = getRetexStatusText();
        } else if (button.id == LAVA_TOGGLE_BUTTON_ID) {
            ConfigHandler.setFishyLavaEnabled(!ConfigHandler.isFishyLavaEnabled());
            ConfigHandler.saveConfigIfNeeded();
            button.displayString = getLavaToggleText();
        } else if (buttonIdToIsland.containsKey(button.id)) {
            String island = buttonIdToIsland.get(button.id);
            boolean isEnabled = TextureConfig.isIslandTextureEnabled(island);
            TextureConfig.toggleIslandTexture(island, !isEnabled);
            button.displayString = getIslandButtonText(island);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, "§bVisual Settings", width / 2, height / 4 - 20, 0xFFFFFF);

        allToggleButton.packedFGColour = TextureConfig.isAllToggled() ? 0xFFCCFFCC : 0xFFFF8080;
        statusToggleButton.packedFGColour = TextureConfig.isRetexStatus() ? 0xFFCCFFCC : 0xFFFF8080;
        toggleFishyLavaButton.packedFGColour = ConfigHandler.isFishyLavaEnabled() ? 0xFFCCFFCC : 0xFFFF8080;

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (allToggleButton.isMouseOver()) {
            List<String> tooltip = Arrays.asList(
                "Shortcut to toggle everything.", "- §8With this you can re-enable retexturing anytime.");

            int tooltipX = mouseX + 10;
            int tooltipY = mouseY + 10;
            int width = 250;
            int height = tooltip.size() * 10 + 5;

            drawRect(tooltipX - 3, tooltipY - 3, tooltipX + width + 3, tooltipY + height + 3, 0x90000000);
    
            for (int i = 0; i < tooltip.size(); i++) {
                fontRendererObj.drawString(tooltip.get(i), tooltipX, tooltipY + i * 10, 0xFFE2CAE9);
            }

        } else if (statusToggleButton.isMouseOver()) {
            List<String> tooltip = Arrays.asList(
                "Enable/disable retexturing completely. Note:",
                "- §8Full changes will take effect after a restart. Leaving this off",
                "  will disable everything next session", "- §8Re-enabling requires a restart.",
                "- §8Only use this if you never want to retexture anything");
                
            GuiUtils.drawTooltip(tooltip, mouseX, mouseY, fontRendererObj);
        }
    }
}