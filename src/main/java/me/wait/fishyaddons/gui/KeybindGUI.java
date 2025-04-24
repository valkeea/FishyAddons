package me.wait.fishyaddons.gui;

import me.wait.fishyaddons.config.ConfigHandler;
import me.wait.fishyaddons.handlers.KeybindHandler;
import me.wait.fishyaddons.util.FishyNotis;
import net.minecraft.client.gui.*;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;

public class KeybindGUI extends GuiScreen {
    private GuiTextField commandField;
    private GuiButton keySelectButton, saveButton, backButton;
    private String selectedKey = "NONE";
    private boolean isSelectingKey = false;

    private static final int COMMAND_FIELD_ID = 0;
    private static final int KEY_SELECT_BUTTON_ID = 1;
    private static final int SAVE_BUTTON_ID = 2;
    private static final int BACK_BUTTON_ID = 3;

    @Override
    public void initGui() {
        int centerX = width / 2;
        int centerY = height / 2;

        commandField = new GuiTextField(COMMAND_FIELD_ID, fontRendererObj, centerX - 100, centerY - 40, 200, 20);
        commandField.setMaxStringLength(256);

        keySelectButton = new GuiButton(KEY_SELECT_BUTTON_ID, centerX - 50, centerY, 100, 20, "Key: " + selectedKey);
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
            case KEY_SELECT_BUTTON_ID:
                enterKeySelectionMode();
                break;
            case SAVE_BUTTON_ID:
                saveKeybind();
                break;
            case BACK_BUTTON_ID:
                mc.displayGuiScreen(new KeybindListGUI());
                break;
        }
    }

    private void enterKeySelectionMode() {
        isSelectingKey = true;
        keySelectButton.displayString = "Press a key...";
    }

    private void saveKeybind() {
        if (isValidKeybind()) {
            try {
                ConfigHandler.setKeybindCommand(selectedKey, commandField.getText());
                KeybindHandler.refreshKeybindCache();
                ConfigHandler.saveConfigIfNeeded();

                FishyNotis.send(EnumChatFormatting.GRAY + "Keybind added: " + selectedKey + " -> " + commandField.getText());
                mc.displayGuiScreen(new KeybindListGUI());
            } catch (Exception e) {
                FishyNotis.send(EnumChatFormatting.RED + "Error adding keybind: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            FishyNotis.send(EnumChatFormatting.YELLOW + "Please select a valid key and enter a command.");
        }
    }

    private boolean isValidKeybind() {
        return !selectedKey.equals("NONE") && !selectedKey.equals("Press a key...") && !commandField.getText().isEmpty();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (isSelectingKey) {
            String keyName = Keyboard.getKeyName(keyCode);
            if (keyName != null && !keyName.equals("NONE")) {
                selectedKey = keyName;
                keySelectButton.displayString = "Key: " + selectedKey;
            }
            isSelectingKey = false;
        } else if (commandField.isFocused()) {
            commandField.textboxKeyTyped(typedChar, keyCode);
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (isSelectingKey) {
            selectedKey = mouseButton >= 0 ? "MOUSE" + mouseButton : "NONE";
            keySelectButton.displayString = "Key: " + selectedKey;
            isSelectingKey = false;
        } else {
            commandField.mouseClicked(mouseX, mouseY, mouseButton);
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, "Add a Keybind:", width / 2, height / 2 - 60, 0xFF55FFFF);
        commandField.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}