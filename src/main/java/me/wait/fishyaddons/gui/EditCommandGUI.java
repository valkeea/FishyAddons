package me.wait.fishyaddons.gui;

import me.wait.fishyaddons.config.ConfigHandler;
import me.wait.fishyaddons.util.FishyNotis;
import net.minecraft.client.gui.*;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.io.IOException;

public class EditCommandGUI extends GuiScreen {
    private GuiTextField commandField;
    private GuiButton aliasSelectButton, saveButton, backButton;
    private String selectedAlias;
    private String originalAlias;
    private String command;

    private static final int COMMAND_FIELD_ID = 0;
    private static final int ALIAS_SELECT_BUTTON_ID = 1;
    private static final int SAVE_BUTTON_ID = 2;
    private static final int BACK_BUTTON_ID = 3;

    public EditCommandGUI(String originalAlias, String command) {
        this.originalAlias = originalAlias;
        this.selectedAlias = originalAlias;
        this.command = command;
    }

    @Override
    public void initGui() {
        int centerX = width / 2;
        int centerY = height / 2;

        // Ensure mc and fontRendererObj are initialized
        if (mc == null) {
            throw new IllegalStateException("Minecraft instance (mc) is null. Cannot initialize GUI.");
        }
        if (fontRendererObj == null) {
            fontRendererObj = mc.fontRendererObj;
        }

        commandField = new GuiTextField(COMMAND_FIELD_ID, fontRendererObj, centerX - 100, centerY - 40, 200, 20);
        commandField.setMaxStringLength(256);
        commandField.setText(command);

        // Display the existing alias as static text
        aliasSelectButton = new GuiButton(ALIAS_SELECT_BUTTON_ID, centerX - 50, centerY, 100, 20, "Alias: " + originalAlias);
        aliasSelectButton.enabled = false;

        saveButton = new GuiButton(SAVE_BUTTON_ID, centerX - 50, centerY + 30, 100, 20, "Save");
        backButton = new GuiButton(BACK_BUTTON_ID, centerX - 50, centerY + 60, 100, 20, "Back");

        buttonList.clear();
        buttonList.add(aliasSelectButton);
        buttonList.add(saveButton);
        buttonList.add(backButton);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case SAVE_BUTTON_ID:
                saveCommandAlias();
                break;
            case BACK_BUTTON_ID:
                mc.displayGuiScreen(new CommandListGUI());
                break;
        }
    }

    private void saveCommandAlias() {
        if (isValidCommand()) {
            try {
                ConfigHandler.removeCommandAlias(originalAlias);
                ConfigHandler.setCommandAlias(originalAlias, commandField.getText());

                FishyNotis.send(EnumChatFormatting.GRAY + "Command alias updated: " + originalAlias + " -> " + commandField.getText());
                mc.displayGuiScreen(new CommandListGUI());
            } catch (Exception e) {
                FishyNotis.send(EnumChatFormatting.RED + "Error updating command alias: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            FishyNotis.send(EnumChatFormatting.YELLOW + "Please enter a valid command.");
        }
    }

    private boolean isValidCommand() {
        return !commandField.getText().isEmpty();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (commandField.isFocused()) {
            commandField.textboxKeyTyped(typedChar, keyCode);
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        commandField.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, "Edit Command Alias:", width / 2, height / 2 - 60, 0xFF55FFFF);
        commandField.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
