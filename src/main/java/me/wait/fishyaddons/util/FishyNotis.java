package me.wait.fishyaddons.util;

import me.wait.fishyaddons.config.UUIDConfigHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class FishyNotis {

    // Future-proofing for more notis / cleanup

    private static String formatMessage(String message) {
        return EnumChatFormatting.AQUA + "[FA] " + EnumChatFormatting.RESET + message;
    }

    public static void send(String message) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(formatMessage(message)));
        }
    }

    public static void protectNotification() {
        if (!UUIDConfigHandler.isProtectNotiEnabled()) return;
        send(EnumChatFormatting.GRAY + "Item protected");
    }
}