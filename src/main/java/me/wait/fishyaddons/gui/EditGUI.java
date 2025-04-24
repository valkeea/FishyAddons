package me.wait.fishyaddons.gui;

import me.wait.fishyaddons.config.ConfigHandler;
import me.wait.fishyaddons.handlers.KeybindHandler;
import me.wait.fishyaddons.gui.KeybindListGUI;
import me.wait.fishyaddons.util.FishyNotis;
import net.minecraft.client.gui.*;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class EditGUI extends GuiScreen {
    private GuiTextField commandField;
    private GuiButton keySelectButton, saveButton, backButton;
    private String selectedKey;
    private String originalKey;
    private String command;
    private boolean isSelectingKey = false;

    private static final int COMMAND_FIELD_ID = 0;
    private static final int KEY_SELECT_BUTTON_ID = 1;
    private static final int SAVE_BUTTON_ID = 2;
    private static final int BACK_BUTTON_ID = 3;

    public EditGUI(String originalKey, String command) {
        if (originalKey.contains("(")) {
            this.originalKey = originalKey.split("\\(")[0].trim();
        } else {
            this.originalKey = originalKey;
        }
        this.selectedKey = this.originalKey;
        this.command = command != null ? command : "";
    }

    @Override
    public void initGui() {
        int centerX = width / 2;
        int centerY = height / 2;

        commandField = new GuiTextField(COMMAND_FIELD_ID, fontRendererObj, centerX - 100, centerY - 40, 200, 20);
        commandField.setMaxStringLength(256);
        commandField.setText(command);

        keySelectButton = new GuiButton(KEY_SELECT_BUTTON_ID, centerX - 50, centerY, 100, 20, "Key: " + originalKey);
        keySelectButton.enabled = false; // Disable the button to prevent interaction

        saveButton = new GuiButton(SAVE_BUTTON_ID, centerX - 50, centerY + 30, 100, 20, "Save");
        backButton = new GuiButton(BACK_BUTTON_ID, centerX - 50, centerY + 60, 100, 20, "Back");

        buttonList.clear();
        buttonList.add(keySelectButton);
        buttonList.add(saveButton);
        buttonList.add(backButton);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case SAVE_BUTTON_ID:
                saveKeybind();
                break;
            case BACK_BUTTON_ID:
                mc.displayGuiScreen(new KeybindListGUI());
                break;
        }
    }

    private void saveKeybind() {
        if (isValidCommand()) {
            try {
                ConfigHandler.setKeybindCommand(originalKey, commandField.getText());
                KeybindHandler.refreshKeybindCache();
                ConfigHandler.saveConfigIfNeeded();

                FishyNotis.send(EnumChatFormatting.GRAY + "Keybind updated: " + originalKey + " -> " + commandField.getText());
                mc.displayGuiScreen(new KeybindListGUI());
            } catch (Exception e) {
                FishyNotis.send(EnumChatFormatting.RED + "Error updating keybind: " + e.getMessage());
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
        if (isSelectingKey) {
            String keyName = Keyboard.getKeyName(keyCode);
            if (keyName != null && !keyName.equals("NONE")) {
                selectedKey = keyName;
                keySelectButton.displayString = selectedKey;
            }
            isSelectingKey = false;
        } else if (commandField.isFocused()) {
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
        drawCenteredString(fontRendererObj, "Edit Keybind:", width / 2, height / 2 - 60, 0xFF55FFFF);
        commandField.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
