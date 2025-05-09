package me.wait.fishyaddons.gui;

import me.wait.fishyaddons.config.ConfigHandler;
import me.wait.fishyaddons.util.FishyNotis;
import net.minecraft.client.gui.*;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

public class CommandListGUI extends GuiScreen {
    private GuiButton backButton;
    private int listStartY;
    private int scrollOffset = 0;
    private static final int SCROLL_STEP = 25;
    private static final int MAX_VISIBLE_COMMANDS = 10;

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
        int endIndex = Math.min(scrollOffset + MAX_VISIBLE_COMMANDS, ConfigHandler.getCommandAliases().size());

        for (Map.Entry<String, String> entry : ConfigHandler.getCommandAliases().entrySet().stream()
                .skip(startIndex)
                .limit(endIndex - startIndex)
                .collect(Collectors.toList())) {
            String alias = entry.getKey();
            String command = entry.getValue();

            buttonList.add(createEditButton(centerX, yOffset, buttonIdOffset, alias, command));
            buttonList.add(createDeleteButton(centerX, yOffset, buttonIdOffset, alias));
            buttonList.add(createToggleButton(centerX, yOffset, buttonIdOffset, alias));

            yOffset += SCROLL_STEP;
            buttonIdOffset++;
        }
    }

    private GuiButton createEditButton(int centerX, int yOffset, int buttonIdOffset, String alias, String command) {
        String truncatedCommand = command.length() > 20 ? command.substring(0, 17) + "..." : command;
        String editText = "Edit: " + alias + " (" + truncatedCommand + ")";
        GuiButton editButton = new AliasButton(EDIT_BUTTON_ID_START + buttonIdOffset, centerX - 200, listStartY + yOffset, 200, 20, editText, alias);
        editButton.packedFGColour = 0xFF808080;
        return editButton;
    }

    private GuiButton createDeleteButton(int centerX, int yOffset, int buttonIdOffset, String alias) {
        GuiButton deleteButton = new AliasButton(DELETE_BUTTON_ID_START + buttonIdOffset, centerX + 10, listStartY + yOffset, 100, 20, "Delete", alias);
        deleteButton.packedFGColour = 0xFF808080;
        return deleteButton;
    }

    private GuiButton createToggleButton(int centerX, int yOffset, int buttonIdOffset, String alias) {
        boolean isToggled = ConfigHandler.isCommandToggled(alias);
        String toggleText = isToggled ? "ON" : "OFF";
        GuiButton toggleButton = new AliasButton(TOGGLE_BUTTON_ID_START + buttonIdOffset, centerX + 120, listStartY + yOffset, 100, 20, toggleText, alias);
        toggleButton.packedFGColour = isToggled ? 0xFFCCFFCC : 0xFFFF8080;
        return toggleButton;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BACK_BUTTON_ID) {
            mc.displayGuiScreen(new FishyAddonsGUI());
        } else if (button.id == ADD_BUTTON_ID) {
            mc.displayGuiScreen(new CommandGUI());
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
        if (!(button instanceof AliasButton)) {
            FishyNotis.send(EnumChatFormatting.RED + "Error: Invalid button type.");
            return;
        }
        String alias = ((AliasButton) button).alias;

        if (alias == null) {
            FishyNotis.send(EnumChatFormatting.RED + "Error: Could not extract alias from button.");
            return;
        }

        String command = ConfigHandler.getCommandAlias(alias);
        if (command == null) {
            FishyNotis.send(EnumChatFormatting.RED + "Error: No command found for alias '" + alias + "'.");
            return;
        }

        mc.displayGuiScreen(new EditCommandGUI(alias, command));
    }

    private void handleDeleteButton(GuiButton button) {
        if (!(button instanceof AliasButton)) {
            FishyNotis.send(EnumChatFormatting.RED + "Error: Invalid button type.");
            return;
        }
        String alias = ((AliasButton) button).alias;

        if (alias != null) {
            ConfigHandler.removeCommandAlias(alias);
            initGui(); // Refresh the GUI
        }
    }

    private void handleToggleButton(GuiButton button) {
        if (!(button instanceof AliasButton)) {
            FishyNotis.send(EnumChatFormatting.RED + "Error: Invalid button type.");
            return;
        }
        String alias = ((AliasButton) button).alias;

        if (alias != null) {
            boolean isToggled = !ConfigHandler.isCommandToggled(alias);
            ConfigHandler.toggleCommand(alias, isToggled);
            button.displayString = isToggled ? "ON" : "OFF";
            button.packedFGColour = isToggled ? 0xFFCCFFCC : 0xFFFF8080;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, "Command Alias List", width / 2, listStartY - 20, 0xFF55FFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);

        // Draw scroll indicator
        int totalCommands = ConfigHandler.getCommandAliases().size();
        if (totalCommands > MAX_VISIBLE_COMMANDS) {
            int scrollBarHeight = (int) ((float) MAX_VISIBLE_COMMANDS / totalCommands * (height - listStartY - 50));
            int scrollBarY = listStartY + (int) ((float) scrollOffset / totalCommands * (height - listStartY - 50));
            drawRect(width - 10, scrollBarY, width - 5, scrollBarY + scrollBarHeight, 0xFF808080);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int scrollDelta = org.lwjgl.input.Mouse.getDWheel();
        if (scrollDelta != 0) {
            scrollOffset -= Integer.signum(scrollDelta);
            scrollOffset = Math.max(0, Math.min(scrollOffset, ConfigHandler.getCommandAliases().size() - MAX_VISIBLE_COMMANDS));
            initGui();
        }
    }
}
