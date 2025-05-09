package me.wait.fishyaddons.gui;

import me.wait.fishyaddons.config.UUIDConfigHandler;
import me.wait.fishyaddons.tool.GuiBlacklistEntry;
import me.wait.fishyaddons.fishyprotection.BlacklistStore;
import me.wait.fishyaddons.fishyprotection.BlacklistConfigHandler;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;

import java.util.HashMap;
import java.util.Map;

public class SellProtectionGUI extends GuiScreen {
    private static final int BACK_BUTTON_ID = 0;
    private static final int CLOSE_BUTTON_ID = 1;
    private static final int GUARD_BUTTON_ID = 100;
    private static final int TOOLTIP_BUTTON_ID = 101;
    private static final int ALERT_BUTTON_ID = 102;
    private static final int NOTI_BUTTON_ID = 103;
    private static final int BLACKLIST_BUTTON_OFFSET = 2; // Start blacklist button IDs after special buttons

    private GuiButton guardButton;

    private final Map<Integer, GuiBlacklistEntry> buttonIdToBlacklistEntry = new HashMap<>();

    private void updateToggleSellProtectionButton() {
        guardButton.displayString = getToggleButtonText();
        guardButton.packedFGColour = UUIDConfigHandler.isSellProtectionEnabled() ? 0xFFCCFFCC : 0xFFFF8080;
    }

    private String getToggleButtonText() {
        return UUIDConfigHandler.isSellProtectionEnabled() ? "Sell Protection §aON" : "Sell Protection §cOFF";
    }

    private void updateBlacklistButton(GuiButton button, GuiBlacklistEntry entry) {
        String localizedLabel = StatCollector.translateToLocal("blacklist." + entry.identifiers.get(0).toLowerCase().replace(" ", "_"));
        button.displayString = localizedLabel;
        button.packedFGColour = entry.enabled ? 0xFFCCFFCC : 0xFFFF8080;
    }

    private String getTooltipToggleText() {
        return UUIDConfigHandler.isTooltipEnabled() ? "Tooltip §aON" : "Tooltip §cOFF";
    }

    private String getSoundToggleText() {
        return UUIDConfigHandler.isProtectTriggerEnabled() ? "Soundeffect §aON" : "Soundeffect §cOFF";
    }

    private String getNotiToggleText() {
        return UUIDConfigHandler.isProtectNotiEnabled() ? "Chat Notifications §aON" : "Chat Notifications §cOFF";
    }

    @Override
    public void initGui() {
        buttonList.clear();
        buttonIdToBlacklistEntry.clear();

        // Add Back and Close buttons
        GuiButton backButton = new GuiButton(BACK_BUTTON_ID, width / 2 - 100, height - 30, 80, 20, "Back");
        GuiButton closeButton = new GuiButton(CLOSE_BUTTON_ID, width / 2 + 20, height - 30, 80, 20, "Close");
        buttonList.add(backButton);
        buttonList.add(closeButton);

        // Add toggle button for Sell Protection
        guardButton = new GuiButton(GUARD_BUTTON_ID, width / 2 - 240, height / 3 - 30, 120, 20, getToggleButtonText());
        guardButton.packedFGColour = UUIDConfigHandler.isSellProtectionEnabled() ? 0xFFCCFFCC : 0xFFFF8080;
        buttonList.add(guardButton);

        // Add toggle button for Tooltip
        GuiButton tooltipButton = new GuiButton(TOOLTIP_BUTTON_ID, width / 2 - 120, height / 3 -30, 120, 20, getTooltipToggleText());
        tooltipButton.packedFGColour = UUIDConfigHandler.isTooltipEnabled() ? 0xFFCCFFCC : 0xFFFF8080;
        buttonList.add(tooltipButton);

        // Add toggle button for Sound Effects
        GuiButton alertButton = new GuiButton(ALERT_BUTTON_ID, width / 2, height / 3 - 30, 120, 20, getSoundToggleText());
        alertButton.packedFGColour = UUIDConfigHandler.isProtectTriggerEnabled() ? 0xFFCCFFCC : 0xFFFF8080;
        buttonList.add(alertButton);

        // Add toggle button for Notifications
        GuiButton notiButton = new GuiButton(NOTI_BUTTON_ID, width / 2 + 120, height / 3 - 30, 120, 20, getNotiToggleText());
        notiButton.packedFGColour = UUIDConfigHandler.isProtectNotiEnabled() ? 0xFFCCFFCC : 0xFFFF8080;
        buttonList.add(notiButton);

        // Add buttons for blacklist entries
        int id = BLACKLIST_BUTTON_OFFSET;
        int y = height / 3 + 30;

        for (GuiBlacklistEntry entry : BlacklistStore.getMergedBlacklist()) {
            String localizedLabel = StatCollector.translateToLocal("blacklist." + entry.identifiers.get(0).toLowerCase().replace(" ", "_"));
            String buttonText = localizedLabel;

            GuiButton blacklistButton = new GuiButton(id, width / 2 - 100, y, 200, 20, buttonText);
            blacklistButton.packedFGColour = entry.enabled ? 0xFFCCFFCC : 0xFFFF8080;
            buttonList.add(blacklistButton);

            buttonIdToBlacklistEntry.put(id, entry);

            id++;
            y += 24;
        }

        super.initGui();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == BACK_BUTTON_ID) {
            mc.displayGuiScreen(new FishyAddonsGUI());
        } else if (button.id == CLOSE_BUTTON_ID) {
            mc.displayGuiScreen(null);
        } else if (button.id == GUARD_BUTTON_ID) {
            UUIDConfigHandler.setSellProtectionEnabled(!UUIDConfigHandler.isSellProtectionEnabled());
            UUIDConfigHandler.saveConfigIfNeeded();
            updateToggleSellProtectionButton();
        } else if (button.id == TOOLTIP_BUTTON_ID) {
            UUIDConfigHandler.setTooltipEnabled(!UUIDConfigHandler.isTooltipEnabled());
            UUIDConfigHandler.saveConfigIfNeeded();
            button.displayString = getTooltipToggleText();
            button.packedFGColour = UUIDConfigHandler.isTooltipEnabled() ? 0xFFCCFFCC : 0xFFFF8080;
        } else if (button.id == ALERT_BUTTON_ID) {
            UUIDConfigHandler.setProtectTriggerEnabled(!UUIDConfigHandler.isProtectTriggerEnabled());
            UUIDConfigHandler.saveConfigIfNeeded();
            button.displayString = getSoundToggleText();
            button.packedFGColour = UUIDConfigHandler.isProtectTriggerEnabled() ? 0xFFCCFFCC : 0xFFFF8080;
        } else if (button.id == NOTI_BUTTON_ID) {
            UUIDConfigHandler.setProtectNotiEnabled(!UUIDConfigHandler.isProtectNotiEnabled());
            UUIDConfigHandler.saveConfigIfNeeded();
            button.displayString = getNotiToggleText();
            button.packedFGColour = UUIDConfigHandler.isProtectNotiEnabled() ? 0xFFCCFFCC : 0xFFFF8080;    
        } else if (buttonIdToBlacklistEntry.containsKey(button.id)) {
            GuiBlacklistEntry entry = buttonIdToBlacklistEntry.get(button.id);
            entry.enabled = !entry.enabled;

            BlacklistConfigHandler.updateBlacklistEntry(entry.identifiers.get(0), entry.enabled);
            updateBlacklistButton(button, entry);
            BlacklistConfigHandler.saveUserBlacklist();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, "FA safeguard", width / 2, height / 3 - 60, 0xFF55FFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}