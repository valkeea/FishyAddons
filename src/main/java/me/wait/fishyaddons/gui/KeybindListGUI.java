package me.wait.fishyaddons.gui;

import me.wait.fishyaddons.config.ConfigHandler;
import me.wait.fishyaddons.handlers.KeybindHandler;
import net.minecraft.client.gui.*;
import net.minecraft.util.ChatComponentText;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

public class KeybindListGUI extends GuiScreen {
    private GuiButton backButton;
    private int listStartY;
    private int scrollOffset = 0;
    private static final int SCROLL_STEP = 25;
    private static final int MAX_VISIBLE_KEYBINDS = 10;

    private static final int BACK_BUTTON_ID = 0;
    private static final int ADD_BUTTON_ID = 1;
    private static final int CLOSE_BUTTON_ID = 2;
    private static final int EDIT_BUTTON_ID_START = 100;
    private static final int DELETE_BUTTON_ID_START = 200;
    private static final int TOGGLE_BUTTON_ID_START = 300;

    @Override
    public void initGui() {
        int centerX = width / 2;
        listStartY = height / 4;

        backButton = new GuiButton(BACK_BUTTON_ID, centerX - 120, height - 30, 80, 20, "Back");
        GuiButton addButton = new GuiButton(ADD_BUTTON_ID, centerX - 40, height - 30, 80, 20, "Add");
        GuiButton closeButton = new GuiButton(CLOSE_BUTTON_ID, centerX + 40, height - 30, 80, 20, "Close");
        buttonList.clear();
        buttonList.add(backButton);
        buttonList.add(addButton);
        buttonList.add(closeButton);

        int yOffset = 0;
        int buttonIdOffset = 0;
        int startIndex = scrollOffset;
        int endIndex = Math.min(scrollOffset + MAX_VISIBLE_KEYBINDS, ConfigHandler.getKeybinds().size());

        for (Map.Entry<String, String> entry : ConfigHandler.getKeybinds().entrySet().stream()
                .skip(startIndex)
                .limit(endIndex - startIndex)
                .collect(Collectors.toList())) {
            String key = entry.getKey();
            String command = entry.getValue();

            buttonList.add(createEditButton(centerX, yOffset, buttonIdOffset, key, command));
            buttonList.add(createDeleteButton(centerX, yOffset, buttonIdOffset, key));
            buttonList.add(createToggleButton(centerX, yOffset, buttonIdOffset, key));

            yOffset += SCROLL_STEP;
            buttonIdOffset++;
        }
    }

    private GuiButton createEditButton(int centerX, int yOffset, int buttonIdOffset, String key, String command) {
        String truncatedCommand = command.length() > 20 ? command.substring(0, 17) + "..." : command;
        String editText = "Edit: " + key + " (" + truncatedCommand + ")";
        GuiButton editButton = new GuiButton(EDIT_BUTTON_ID_START + buttonIdOffset, centerX - 200, listStartY + yOffset, 200, 20, editText);
        editButton.packedFGColour = 0xFF808080;
        return editButton;
    }

    private GuiButton createDeleteButton(int centerX, int yOffset, int buttonIdOffset, String key) {
        GuiButton deleteButton = new GuiButton(DELETE_BUTTON_ID_START + buttonIdOffset, centerX + 10, listStartY + yOffset, 100, 20, "Delete");
        deleteButton.packedFGColour = 0xFF808080;
        return deleteButton;
    }

    private GuiButton createToggleButton(int centerX, int yOffset, int buttonIdOffset, String key) {
        boolean isToggled = ConfigHandler.isKeybindToggled(key);
        String toggleText = isToggled ? "ON" : "OFF"; // Display only "ON" or "OFF"
        GuiButton toggleButton = new GuiButton(TOGGLE_BUTTON_ID_START + buttonIdOffset, centerX + 120, listStartY + yOffset, 100, 20, toggleText);
        toggleButton.packedFGColour = isToggled ? 0xFFCCFFCC : 0xFFFF8080;
        return toggleButton;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BACK_BUTTON_ID) {
            mc.displayGuiScreen(new FishyAddonsGUI());
        } else if (button.id == ADD_BUTTON_ID) {
            mc.displayGuiScreen(new KeybindGUI());
        } else if (button.id == CLOSE_BUTTON_ID) {
            mc.displayGuiScreen(null);
        } else if (button.id >= EDIT_BUTTON_ID_START && button.id < DELETE_BUTTON_ID_START) {
            handleEditButton(button);
        } else if (button.id >= DELETE_BUTTON_ID_START && button.id < TOGGLE_BUTTON_ID_START) {
            handleDeleteButton(button);
        } else if (button.id >= TOGGLE_BUTTON_ID_START) {
            handleToggleButton(button);
        }
    }

    private void handleEditButton(GuiButton button) {
        String key = extractKeyFromButton(button, EDIT_BUTTON_ID_START, "Edit: ");
        if (key != null) {
            String command = ConfigHandler.getKeybindCommand(key);
            if (command == null) {
                command = "";
            }
            mc.displayGuiScreen(new EditGUI(key, command));
        }
    }

    private void handleDeleteButton(GuiButton button) {
        String key = extractKeyFromButton(button, DELETE_BUTTON_ID_START, null);
        if (key != null) {
            ConfigHandler.removeKeybind(key);
            KeybindHandler.refreshKeybindCache();
            initGui();
        }
    }

    private void handleToggleButton(GuiButton button) {
        String key = extractKeyFromButton(button, TOGGLE_BUTTON_ID_START, null);
        if (key != null) {
            boolean isToggled = !ConfigHandler.isKeybindToggled(key);
            ConfigHandler.toggleKeybind(key, isToggled);
            KeybindHandler.refreshKeybindCache();
            button.displayString = isToggled ? "ON" : "OFF";
            button.packedFGColour = isToggled ? 0xFFCCFFCC : 0xFFFF8080;
        }
    }

    private String extractKeyFromButton(GuiButton button, int baseId, String prefix) {
        if (button.id >= baseId) {
            // Calculate the key index based on the button ID
            int index = button.id - baseId;
            return ConfigHandler.getKeybinds().keySet().stream().skip(index).findFirst().orElse(null);
        } else if (prefix != null && button.displayString.startsWith(prefix)) {
            // Extract the key from the display string
            int startIndex = prefix.length();
            int endIndex = button.displayString.indexOf("(", startIndex);
            if (endIndex == -1) endIndex = button.displayString.length();
            return button.displayString.substring(startIndex, endIndex).trim();
        }
        return null;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, "Keybind List", width / 2, listStartY - 20, 0xFF55FFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);

        // Scrollable list
        int totalKeybinds = ConfigHandler.getKeybinds().size();
        if (totalKeybinds > MAX_VISIBLE_KEYBINDS) {
            int scrollBarHeight = (int) ((float) MAX_VISIBLE_KEYBINDS / totalKeybinds * (height - listStartY - 50));
            int scrollBarY = listStartY + (int) ((float) scrollOffset / totalKeybinds * (height - listStartY - 50));
            drawRect(width - 10, scrollBarY, width - 5, scrollBarY + scrollBarHeight, 0xFF808080);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int scrollDelta = org.lwjgl.input.Mouse.getDWheel();
        if (scrollDelta != 0) {
            scrollOffset -= Integer.signum(scrollDelta);
            scrollOffset = Math.max(0, Math.min(scrollOffset, ConfigHandler.getKeybinds().size() - MAX_VISIBLE_KEYBINDS));
            initGui();
        }
    }
}