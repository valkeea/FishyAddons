package me.wait.fishyaddons.util;

import me.wait.fishyaddons.config.UUIDConfigHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class FishyNotis {
    private static final String format = "   [ ";
    private static final String space = "     ";
    private static final EnumChatFormatting RESET = EnumChatFormatting.RESET;
    private static final EnumChatFormatting GRAY = EnumChatFormatting.GRAY;
    private static final EnumChatFormatting AQUA = EnumChatFormatting.AQUA;
    private static final EnumChatFormatting GOLD = EnumChatFormatting.GOLD;
    private static final EnumChatFormatting YELLOW = EnumChatFormatting.YELLOW;
    private static final EnumChatFormatting DARK_AQUA = EnumChatFormatting.DARK_AQUA;
    private static final EnumChatFormatting WHITE = EnumChatFormatting.WHITE;


    private static String formatMessage(String message) {
        return AQUA + "[FA] " + RESET + message;
    }

    public static void send(String message) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(formatMessage(message)));
        }
    }

    public static void alert(String message) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }

    public static void protectNotification() {
        if (!UUIDConfigHandler.isProtectNotiEnabled()) return;
        send(GRAY + "Item protected");
    }

    public static void helpNoti() {
        alert(AQUA + " " + EnumChatFormatting.BOLD + "[FA] Available Commands:");
        alert(GRAY + "/fishyaddons = /fa, /faguard = /fg");
        alert(GRAY + "/fg clear | add | remove" + RESET + " - Clear, add or remove guarded items.");
        alert(GRAY + "/fakey | /facmd | /fg | faretex " + RESET + " - Commands for guis.");
        alert(GRAY + "/fakey | facmd + on | off" + RESET + " - Toggle all custom keybinds / commands.");
        alert(GRAY + "/fakey | facmd | fg + add" + RESET + " - Add a new keybind / command / guarded item.");
        alert(GRAY + "/fa lava on | off" + RESET + " - Toggle clear lava.");
        alert(GRAY + "/fa retex on | off" + RESET + " - Toggle retexturing on the current island");
        alert(GRAY + "/fa guide" + RESET + " - Send introduction message");

    }

    public static void guideNoti() {
        alert(AQUA + "" + EnumChatFormatting.BOLD + "α Welcome to FishyAddons! α");
        alert(GRAY + "Current features include:");
    
        send(GOLD + "FishyAddons Safeguard:");
        alert(YELLOW + " - " + GRAY + "Protects your items from being dropped or");
        alert(space + GRAY + "interacted with in certain GUIs.");
        alert(DARK_AQUA + format + AQUA + "/faguard = fg" + DARK_AQUA + " ] " + WHITE + "Open GUI");
        alert(DARK_AQUA + format + AQUA + "/fg add | remove" + DARK_AQUA + " ] " + WHITE + "Add or remove held item");
        alert(DARK_AQUA + format + AQUA + "/fg list" + DARK_AQUA + " ] " + WHITE + "List added UUIDs");
        alert(DARK_AQUA + format + AQUA + "/fg clear" + DARK_AQUA + " ] " + WHITE + "Clear all UUIDs");
    
        send(GOLD + "Visual Features:");
        alert(YELLOW + " - " + GRAY + "Clear lava (under), custom redstone particles");
        alert(space + GRAY + "for Jawbus laser and Flaming Flay.");
        alert(YELLOW + " - " + GRAY + "Alternative for island CTM retexturing from ValksfullSBpack.");
        alert(DARK_AQUA + format + AQUA + "/fa retex" + DARK_AQUA + " ] " + WHITE + "Open GUI");
        alert(DARK_AQUA + format + AQUA + "/fa retex on | off" + DARK_AQUA + " ] " + WHITE + "Toggle for the current island");
    
        send(GOLD + "General QoL:");
        alert(YELLOW + " - " + GRAY + "Custom keybinds and command aliases.");
        alert(DARK_AQUA + format + AQUA + "/fakey | add | on | off |" + DARK_AQUA + " ] " + WHITE + "Open GUI, add keybinds, toggle all");
        alert(DARK_AQUA + format + AQUA + "/facmd | add | on | off" + DARK_AQUA + " ] " + WHITE + "Open GUI, add aliases, toggle all");
    }
}