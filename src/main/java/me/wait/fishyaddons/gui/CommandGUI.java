package me.wait.fishyaddons.gui;

import me.wait.fishyaddons.config.ConfigHandler;
import net.minecraft.client.gui.*;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.io.IOException;

public class CommandGUI extends GuiScreen {
    private GuiTextField aliasField, commandField;
    private GuiButton saveButton, backButton;

    private static final int ALIAS_FIELD_ID = 0;
    private static final int COMMAND_FIELD_ID = 1;
    private static final int SAVE_BUTTON_ID = 2;
    private static final int BACK_BUTTON_ID = 3;

    @Override
    public void initGui() {
        int centerX = width / 2;
        int centerY = height / 2;

        aliasField = new GuiTextField(ALIAS_FIELD_ID, fontRendererObj, centerX - 100, centerY - 70, 200, 20);
        aliasField.setMaxStringLength(256);
        aliasField.setFocused(true);

        commandField = new GuiTextField(COMMAND_FIELD_ID, fontRendererObj, centerX - 100, centerY - 30, 200, 20);
        commandField.setMaxStringLength(256);

        saveButton = new GuiButton(SAVE_BUTTON_ID, centerX - 50, centerY + 10, 100, 20, "Save");
        backButton = new GuiButton(BACK_BUTTON_ID, centerX - 50, centerY + 40, 100, 20, "Back");

        buttonList.clear();
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
        if (isValidCommandAlias()) {
            try {
                ConfigHandler.setCommandAlias(aliasField.getText(), commandField.getText());
                mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.AQUA + "[FA] " + EnumChatFormatting.RESET + EnumChatFormatting.GRAY + "Command alias added: " + aliasField.getText() + " -> " + commandField.getText()));
                mc.displayGuiScreen(new CommandListGUI());
            } catch (Exception e) {
                mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.AQUA + "[FA] " + EnumChatFormatting.RESET + EnumChatFormatting.RED + "Error adding command alias: " + e.getMessage()));
                e.printStackTrace();
            }
        } else {
            mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.AQUA + "[FA] " + EnumChatFormatting.RESET + EnumChatFormatting.YELLOW + "Please enter a valid alias and command."));
        }
    }

    private boolean isValidCommandAlias() {
        return !aliasField.getText().isEmpty() && !commandField.getText().isEmpty();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (aliasField.isFocused()) {
            aliasField.textboxKeyTyped(typedChar, keyCode);
        } else if (commandField.isFocused()) {
            commandField.textboxKeyTyped(typedChar, keyCode);
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        aliasField.mouseClicked(mouseX, mouseY, mouseButton);
        commandField.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, "Add a Command Alias:", width / 2, height / 2 - 90, 0xFF55FFFF);
        drawString(fontRendererObj, "Alias:", width / 2 - 100, height / 2 - 80, 0xFF808080);
        drawString(fontRendererObj, "Command:", width / 2 - 100, height / 2 - 40, 0xFF808080);
        aliasField.drawTextBox();
        commandField.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
