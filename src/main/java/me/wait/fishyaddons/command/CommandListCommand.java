package me.wait.fishyaddons.command;

import me.wait.fishyaddons.config.ConfigHandler;
import me.wait.fishyaddons.gui.CommandGUI;
import me.wait.fishyaddons.gui.CommandListGUI;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.BlockPos;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class CommandListCommand extends CommandBase {
    @Override
    public String getCommandName() {
        return "facmd";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/facmd [add|on|off]";
    }

    private String formatMessage(String message) {
        return EnumChatFormatting.AQUA + "[FA] " + EnumChatFormatting.RESET + message;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (Minecraft.getMinecraft().currentScreen != null 
            && !(Minecraft.getMinecraft().currentScreen instanceof GuiChat)) {
            sender.addChatMessage(new ChatComponentText(formatMessage(EnumChatFormatting.RED + "Dont try to open any mod GUI while another GUI is open.")));
            return;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("add")) {
            // Schedule GUI opening on the client tick to work with server restrictions
            MinecraftForge.EVENT_BUS.register(new Object() {
                @SubscribeEvent
                public void onClientTick(TickEvent.ClientTickEvent event) {
                    Minecraft.getMinecraft().displayGuiScreen(new CommandGUI());
                    MinecraftForge.EVENT_BUS.unregister(this);
                }
            });
        } else if (args.length == 1 && args[0].equalsIgnoreCase("on")) {

            ConfigHandler.getCommandAliases().keySet().forEach(alias -> ConfigHandler.toggleCommand(alias, true));
            sender.addChatMessage(new ChatComponentText(formatMessage("Custom commands " + EnumChatFormatting.GREEN + "ON.")));

        } else if (args.length == 1 && args[0].equalsIgnoreCase("off")) {

            ConfigHandler.getCommandAliases().keySet().forEach(alias -> ConfigHandler.toggleCommand(alias, false));
            sender.addChatMessage(new ChatComponentText(formatMessage("Custom commands " + EnumChatFormatting.RED + "OFF.")));
            
        } else {
            MinecraftForge.EVENT_BUS.register(new Object() {
                @SubscribeEvent
                public void onClientTick(TickEvent.ClientTickEvent event) {
                    Minecraft.getMinecraft().displayGuiScreen(new CommandListGUI());
                    MinecraftForge.EVENT_BUS.unregister(this);
                }
            });
        }
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {

            return getListOfStringsMatchingLastWord(args, "add", "on", "off");
        }
        return Collections.emptyList();
    }
}
