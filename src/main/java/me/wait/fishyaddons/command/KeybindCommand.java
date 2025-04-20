package me.wait.fishyaddons.command;

import me.wait.fishyaddons.config.ConfigHandler;
import me.wait.fishyaddons.handlers.KeybindHandler;
import me.wait.fishyaddons.gui.KeybindGUI;
import me.wait.fishyaddons.gui.KeybindListGUI;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.BlockPos;

import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class KeybindCommand extends CommandBase {
    @Override
    public String getCommandName() {
        return "fakey";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/fakey [add|on|off]";
    }

    private String formatMessage(String message) {
        return EnumChatFormatting.AQUA + "[FA] " + EnumChatFormatting.RESET + message;
    }

    private void scheduleGuiOpening(GuiScreen gui) {
        MinecraftForge.EVENT_BUS.register(new Object() {
            @SubscribeEvent
            public void onClientTick(TickEvent.ClientTickEvent event) {
                Minecraft.getMinecraft().displayGuiScreen(gui);
                MinecraftForge.EVENT_BUS.unregister(this);
            }
        });
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (Minecraft.getMinecraft().currentScreen != null 
            && !(Minecraft.getMinecraft().currentScreen instanceof GuiChat)) {
            sender.addChatMessage(new ChatComponentText(formatMessage(EnumChatFormatting.RED + "Don't try to open any mod GUI while another GUI is open.")));
            return;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("add")) {
            scheduleGuiOpening(new KeybindGUI());


        } else if (args.length == 1 && args[0].equalsIgnoreCase("on")) {
            ConfigHandler.getKeybinds().keySet().forEach(key -> ConfigHandler.toggleKeybind(key, true));
            ConfigHandler.saveConfigIfNeeded();
            KeybindHandler.refreshKeybindCache();
            sender.addChatMessage(new ChatComponentText(formatMessage("Custom keybinds " + EnumChatFormatting.GREEN + "ON.")));
        } else if (args.length == 1 && args[0].equalsIgnoreCase("off")) {
            ConfigHandler.getKeybinds().keySet().forEach(key -> ConfigHandler.toggleKeybind(key, false));
            ConfigHandler.saveConfigIfNeeded();
            KeybindHandler.refreshKeybindCache();
            sender.addChatMessage(new ChatComponentText(formatMessage("Custom keybinds " + EnumChatFormatting.RED + "OFF.")));
        } else {
            MinecraftForge.EVENT_BUS.register(new Object() {
                @SubscribeEvent
                public void onClientTick(TickEvent.ClientTickEvent event) {
                    Minecraft.getMinecraft().displayGuiScreen(new KeybindListGUI());
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