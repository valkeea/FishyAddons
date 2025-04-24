package me.wait.fishyaddons.command;

import me.wait.fishyaddons.FishyAddons;
import me.wait.fishyaddons.gui.FishyAddonsGUI;
import me.wait.fishyaddons.gui.SellProtectionGUI;
import me.wait.fishyaddons.config.ConfigHandler;
import me.wait.fishyaddons.gui.CustomGuiSlider;
import me.wait.fishyaddons.util.GuiScheduler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.util.BlockPos;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;


public class FishyAddonsCommand extends CommandBase {
    private static final EnumChatFormatting GRAY = EnumChatFormatting.GRAY;
    private static final EnumChatFormatting RESET = EnumChatFormatting.RESET;
    private static final EnumChatFormatting AQUA = EnumChatFormatting.AQUA;


    @Override
    public String getCommandName() {
        return "fa";
    }

    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList("fishyaddons");
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/fa [lava on|lava off|help] or /fishyaddons";
    }

    private String formatMessage(String message) {
        return AQUA + "[FA] " + RESET + message;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (Minecraft.getMinecraft().currentScreen != null 
            && !(Minecraft.getMinecraft().currentScreen instanceof GuiChat)) {
            sender.addChatMessage(new ChatComponentText(formatMessage(EnumChatFormatting.RED + "Don't try to open any mod GUI while another GUI is open.")));
            return;
        }

        if (args.length == 0) {
            GuiScheduler.scheduleGui(new FishyAddonsGUI());
            return;
            
        } else if (args.length == 1 && args[0].equalsIgnoreCase("help")) {

            sender.addChatMessage(new ChatComponentText(AQUA + " " + EnumChatFormatting.BOLD + "[FA] Available Commands:"));
            sender.addChatMessage(new ChatComponentText(GRAY + "/fishyaddons = /fa, /faprotect = /fapr"));
            sender.addChatMessage(new ChatComponentText(GRAY + "/fapr clear | add | remove" + RESET + " - Clear/add/remove protected items."));
            sender.addChatMessage(new ChatComponentText(GRAY + "/fakey | /facmd | /fapr " + RESET + " - Open Keybind/Command/FAprotect List."));
            sender.addChatMessage(new ChatComponentText(GRAY + "/fakey | facmd + on | off" + RESET + " - Toggle all custom keybinds/commands."));
            sender.addChatMessage(new ChatComponentText(GRAY + "/fakey | facmd | fapr + add" + RESET + " - Add a new keybind/command."));
            sender.addChatMessage(new ChatComponentText(GRAY + "/fa lava on | off" + RESET + " - Toggle clear lava."));

        } else if (args.length == 2 && args[0].equalsIgnoreCase("lava")) {

            if (args[1].equalsIgnoreCase("on")) {

                ConfigHandler.setFishyLavaEnabled(true);
                ConfigHandler.setCustomParticlesEnabled(true);
                ConfigHandler.saveConfigIfNeeded();
                sender.addChatMessage(new ChatComponentText(formatMessage("Clear lava " + EnumChatFormatting.GREEN + "ON.")));

            } else if (args[1].equalsIgnoreCase("off")) {
                
                ConfigHandler.setFishyLavaEnabled(false);
                ConfigHandler.setCustomParticlesEnabled(false);
                ConfigHandler.saveConfigIfNeeded();
                sender.addChatMessage(new ChatComponentText(formatMessage("Clear lava " + EnumChatFormatting.RED + "OFF.")));

            } else {
                sender.addChatMessage(new ChatComponentText(formatMessage(EnumChatFormatting.YELLOW + "Invalid argument. Use '/fa lava on' or '/fa lava off'.")));
            }
        } else {
            sender.addChatMessage(new ChatComponentText(formatMessage(EnumChatFormatting.YELLOW + "Invalid usage. Use '/fa help' for a list of commands.")));
        }
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "lava", "help");

        } else if (args.length == 2 && args[0].equalsIgnoreCase("lava")) {
            return getListOfStringsMatchingLastWord(args, "on", "off");
        }
        return Collections.emptyList();
    }
}
