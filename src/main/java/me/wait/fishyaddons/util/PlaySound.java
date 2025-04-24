package me.wait.fishyaddons.util;

import me.wait.fishyaddons.config.UUIDConfigHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

public class PlaySound {

    public static void playProtectTrigger() {
        if (!UUIDConfigHandler.isProtectTriggerEnabled()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return;
        mc.thePlayer.playSound("fishyaddons:custom.protect_trigger", 1.0F, 1.0F);
    }
}
