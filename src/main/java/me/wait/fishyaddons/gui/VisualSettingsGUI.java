package me.wait.fishyaddons.gui;

import me.wait.fishyaddons.config.ConfigHandler;
import me.wait.fishyaddons.config.TextureConfig;
import me.wait.fishyaddons.handlers.RetexHandler;
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
            if (("default".equals(island)) || "mineshaft".equals(island)) continue;
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
        String title = "§bVisual Settings";
        int titleX = width / 2;
        int titleY = height / 4 - 20;
        drawCenteredString(fontRendererObj, title, titleX, titleY, 0xFFFFFF);

        allToggleButton.packedFGColour = TextureConfig.isAllToggled() ? 0xFFCCFFCC : 0xFFFF8080;
        statusToggleButton.packedFGColour = TextureConfig.isRetexStatus() ? 0xFFCCFFCC : 0xFFFF8080;
        toggleFishyLavaButton.packedFGColour = ConfigHandler.isFishyLavaEnabled() ? 0xFFCCFFCC : 0xFFFF8080;

        super.drawScreen(mouseX, mouseY, partialTicks);

        int titleWidth = fontRendererObj.getStringWidth(title);
        int titleHeight = fontRendererObj.FONT_HEIGHT;
        int titleLeft = titleX - titleWidth / 2;
        int titleRight = titleX + titleWidth / 2;
        int titleTop = titleY;
        int titleBottom = titleY + titleHeight;

        if (mouseX >= titleLeft && mouseX <= titleRight && mouseY >= titleTop && mouseY <= titleBottom) {
            List<String> tooltip = Arrays.asList(
                "Island retexturing:",
                     "- §7An alternative to ValksfullSBpack biome-based retexturing",
                     "  §7but not intended to be a full replacement for the texture pack.",
                     "- §7The logic used for special blocks (sandstone slabs, glass panes)",
                     "  §7is §cnot compatible §7with OptiFine shaders.");

            GuiUtils.drawTooltip(tooltip, mouseX, mouseY, fontRendererObj);
        }

        if (allToggleButton.isMouseOver()) {
            List<String> tooltip = Arrays.asList(
                "Shortcut to toggle everything.", "- §8Wont prevent models from being overwritten.");

            GuiUtils.drawTooltip(tooltip, mouseX, mouseY, fontRendererObj);

        } else if (statusToggleButton.isMouseOver()) {
            List<String> tooltip = Arrays.asList(
                "Enable/disable retexturing completely. Note:",
                "- §8Full toggle for retexturing features.",
                "- §8Same effect as toggling all islands but",
                "  §8this will fully prevent the code from running.");
                
            GuiUtils.drawTooltip(tooltip, mouseX, mouseY, fontRendererObj);
        }
    }
}