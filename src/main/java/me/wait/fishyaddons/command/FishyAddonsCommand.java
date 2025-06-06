package me.wait.fishyaddons.command;

import me.wait.fishyaddons.FishyAddons;
import me.wait.fishyaddons.gui.FishyAddonsGUI;
import me.wait.fishyaddons.handlers.RetexHandler;
import me.wait.fishyaddons.gui.VisualSettingsGUI;
import me.wait.fishyaddons.config.ConfigHandler;
import me.wait.fishyaddons.config.TextureConfig;
import me.wait.fishyaddons.tool.GuiScheduler;
import me.wait.fishyaddons.util.FishyNotis;
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
    private static final EnumChatFormatting YELLOW = EnumChatFormatting.YELLOW;


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
        return "/fa [lava on|lava off|retex on|retex off|retex set <island>|help|guide] or /fishyaddons";
    }

    private String formatMessage(String message) {
        return AQUA + "[FA] " + RESET + message;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (Minecraft.getMinecraft().currentScreen != null 
            && !(Minecraft.getMinecraft().currentScreen instanceof GuiChat)) {
            return;
        }

        if (args.length == 0) {
            GuiScheduler.scheduleGui(new FishyAddonsGUI());
            return;
            
        } else if (args.length == 1 && args[0].equalsIgnoreCase("help")) {
            FishyNotis.helpNoti();

        } else if (args.length == 1 && args[0].equalsIgnoreCase("guide")) {
            FishyNotis.guideNoti();

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
                sender.addChatMessage(new ChatComponentText(formatMessage(YELLOW + "Invalid argument. Use '/fa lava on' or '/fa lava off'.")));
            }

        } else if (args.length == 2 && args[0].equalsIgnoreCase("retex")) {
            
            if (args[1].equalsIgnoreCase("on")) {
                String island = RetexHandler.getIsland();
                TextureConfig.toggleIslandTexture(island, true);

            } else if (args[1].equalsIgnoreCase("off")) {
                String island = RetexHandler.getIsland();
                TextureConfig.toggleIslandTexture(island, false);

            }

        } else if (args.length >= 2 && args[0].equalsIgnoreCase("retex") && args[1].equalsIgnoreCase("set")) {
            if (args.length == 3) {
                String island = args[2];
                if (RetexHandler.getKnownIslands().contains(island)) {
                    RetexHandler.setIsland(island);
                    sender.addChatMessage(new ChatComponentText(formatMessage(GRAY + "Set retexturing to " + island + ".")));
                } else {
                    sender.addChatMessage(new ChatComponentText(formatMessage(YELLOW + "Invalid island name.")));
                }
            } else {
                sender.addChatMessage(new ChatComponentText(formatMessage(YELLOW + "Usage: /fa retex set <island>.")));
            }

        } else {
            GuiScheduler.scheduleGui(new VisualSettingsGUI());
        }
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "lava", "help", "guide", "retex");

        } else if (args.length == 2 && args[0].equalsIgnoreCase("lava")) {
            return getListOfStringsMatchingLastWord(args, "on", "off");
        }
        else if (args.length == 2 && args[0].equalsIgnoreCase("retex")) {
            return getListOfStringsMatchingLastWord(args, "on", "off", "set");
        }
        else if (args.length == 3 && args[0].equalsIgnoreCase("retex") && args[1].equalsIgnoreCase("set")) {
            return getListOfStringsMatchingLastWord(args, RetexHandler.getKnownIslands().toArray(new String[0]));
        }
        return Collections.emptyList();
    }
}