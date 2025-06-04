package me.wait.fishyaddons.command;

import me.wait.fishyaddons.config.UUIDConfigHandler;
import me.wait.fishyaddons.gui.CommandListGUI;
import me.wait.fishyaddons.gui.FishyAddonsGUI;
import me.wait.fishyaddons.gui.SellProtectionGUI;
import me.wait.fishyaddons.handlers.ProtectedItemHandler;
import me.wait.fishyaddons.tool.GuiScheduler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ProtectCommand extends CommandBase {
    private boolean awaitingClearConfirmation = false;

    @Override
    public String getCommandName() {
        return "faguard";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/faguard <add|remove|list|clear>";
    }

    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList("fg");
    }

    private String formatMessage(String message) {
        return EnumChatFormatting.AQUA + "[FA] " + EnumChatFormatting.RESET + message;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (Minecraft.getMinecraft().thePlayer == null) return;

        if (Minecraft.getMinecraft().currentScreen != null 
            && !(Minecraft.getMinecraft().currentScreen instanceof GuiChat)) {
            return;
        }

        if (args.length == 0) {
            GuiScheduler.scheduleGui(new SellProtectionGUI());
            return;
        }

        String action = args[0].toLowerCase();
        switch (action) {
            case "add":
                handleAddCommand(sender);
                break;

            case "remove":
                handleRemoveCommand(sender);
                break;

            case "list":
                handleListCommand(sender);
                break;

            case "clear":
                handleClearCommand(sender);
                break;

            case "confirmclear":
            case "cancelclear":
                handleConfirmation(sender, action);
                break;

            case "help":
                notify(sender, formatMessage("Usage: /faprotect <add | remove | list | clear>"));
                break;

            default:
                notify(sender, formatMessage(EnumChatFormatting.GRAY + "Invalid subcommand."));
        }
    }

    private void handleAddCommand(ICommandSender sender) {
        Minecraft mc = Minecraft.getMinecraft();
        ItemStack held = mc.thePlayer.getHeldItem();

        if (held == null) {
            notify(sender, formatMessage(EnumChatFormatting.GRAY + "You must be holding an item."));
            return;
        }

        String uuidToAdd = getUUIDFromItem(held);
        String displayName = held.getDisplayName();
        if (uuidToAdd != null) {
            UUIDConfigHandler.addUUID(uuidToAdd, displayName); // Pass display name!
            notify(sender, formatMessage(EnumChatFormatting.GRAY + "Your " + EnumChatFormatting.RESET + displayName + EnumChatFormatting.GRAY + " is now protected."));
        } else {
            notify(sender, formatMessage(EnumChatFormatting.GRAY + "Held item doesn't have a UUID."));
        }
    }

    private void handleRemoveCommand(ICommandSender sender) {
        Minecraft mc = Minecraft.getMinecraft();
        ItemStack held = mc.thePlayer.getHeldItem();

        if (held == null) {
            notify(sender, formatMessage(EnumChatFormatting.GRAY + "You must be holding an item."));
            return;
        }

        String uuidToRemove = getUUIDFromItem(held);
        if (uuidToRemove != null && ProtectedItemHandler.isProtected(held)) {
            UUIDConfigHandler.removeUUID(uuidToRemove);
            notify(sender, formatMessage(EnumChatFormatting.GRAY + "Your " + EnumChatFormatting.RESET + held.getDisplayName() + EnumChatFormatting.GRAY + " is no longer protected."));
        } else {
            notify(sender, formatMessage(EnumChatFormatting.GRAY + "Held item isn't protected or doesn't have a UUID."));
        }
    }

    private void handleListCommand(ICommandSender sender) {
        notify(sender, formatMessage("Protected Items:"));
        List<java.util.Map.Entry<String, String>> entries = new java.util.ArrayList<>(UUIDConfigHandler.getProtectedUUIDs().entrySet());

        String colorOrder = "4a956dbc\0"; // '\0' represents no color code

        entries.sort((a, b) -> {
            String aShown = (a.getValue() != null && !a.getValue().isEmpty()) ? a.getValue() : a.getKey();
            String bShown = (b.getValue() != null && !b.getValue().isEmpty()) ? b.getValue() : b.getKey();

            char aColor = (aShown.length() > 1 && aShown.charAt(0) == '§') ? aShown.charAt(1) : '\0';
            char bColor = (bShown.length() > 1 && bShown.charAt(0) == '§') ? bShown.charAt(1) : '\0';

            int aIndex = colorOrder.indexOf(aColor);
            int bIndex = colorOrder.indexOf(bColor);

            if (aIndex == -1) aIndex = colorOrder.length();
            if (bIndex == -1) bIndex = colorOrder.length();

            if (aIndex != bIndex) {
                return Integer.compare(aIndex, bIndex);
            }
            // Sort alphabeticlly
            String aAlpha = (aColor != '\0') ? aShown.substring(2) : aShown;
            String bAlpha = (bColor != '\0') ? bShown.substring(2) : bShown;
            return aAlpha.compareToIgnoreCase(bAlpha);
        });

        for (java.util.Map.Entry<String, String> entry : entries) {
            String displayName = entry.getValue();
            String uuid = entry.getKey();
            String shown = (displayName != null && !displayName.isEmpty()) ? displayName : uuid;
            notify(sender, " - " + EnumChatFormatting.GRAY + shown);
        }
    }

    private void handleClearCommand(ICommandSender sender) {
        if (!awaitingClearConfirmation) {
            awaitingClearConfirmation = true;
            notify(sender, formatMessage(EnumChatFormatting.GRAY + "Are you SURE you want to clear all protected items?"));
            sendClickableConfirmation(sender);
        } else {
            notify(sender, formatMessage(EnumChatFormatting.GRAY + "Please respond to the confirmation prompt for: /faguard clear."));
        }
    }

    private void sendClickableConfirmation(ICommandSender sender) {
        ChatComponentText yesButton = new ChatComponentText(EnumChatFormatting.GREEN + "[Yes]");
        yesButton.getChatStyle().setChatClickEvent(new net.minecraft.event.ClickEvent(net.minecraft.event.ClickEvent.Action.RUN_COMMAND, "/faguard confirmclear"));

        ChatComponentText noButton = new ChatComponentText(EnumChatFormatting.RED + "[No]");
        noButton.getChatStyle().setChatClickEvent(new net.minecraft.event.ClickEvent(net.minecraft.event.ClickEvent.Action.RUN_COMMAND, "/faguard cancelclear"));

        ChatComponentText message = new ChatComponentText(" ");
        message.appendSibling(yesButton);
        message.appendSibling(new ChatComponentText(" "));
        message.appendSibling(noButton);

        sender.addChatMessage(message);
    }

    private String getUUIDFromItem(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) return null;
        NBTTagCompound extra = stack.getSubCompound("ExtraAttributes", false);
        if (extra != null && extra.hasKey("uuid")) {
            return extra.getString("uuid");
        }
        return null;
    }

    private void notify(ICommandSender sender, String msg) {
        sender.addChatMessage(new ChatComponentText(msg));
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    public void handleConfirmation(ICommandSender sender, String action) {
        if ("confirmclear".equals(action)) {
            UUIDConfigHandler.clearAll();
            notify(sender, formatMessage(EnumChatFormatting.GRAY + "All protected items have been cleared."));
            awaitingClearConfirmation = false;
        } else if ("cancelclear".equals(action)) {
            notify(sender, formatMessage(EnumChatFormatting.GRAY + "/faguard clear was canceled."));
            awaitingClearConfirmation = false;
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "add", "list", "clear");
        }
        return Collections.emptyList();
    }    
}
