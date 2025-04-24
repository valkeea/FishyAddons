package me.wait.fishyaddons.util;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/// Used to schedule a GUI to be displayed on the next client tick
public final class GuiScheduler {

    private GuiScheduler() {
        throw new UnsupportedOperationException("wee");
    }

    public static void scheduleGui(GuiScreen guiScreen) {
        MinecraftForge.EVENT_BUS.register(new Object() {
            @SubscribeEvent
            public void onClientTick(TickEvent.ClientTickEvent event) {
                Minecraft.getMinecraft().displayGuiScreen(guiScreen);
                MinecraftForge.EVENT_BUS.unregister(this);
            }
        });
    }
}
