package me.wait.fishyaddons.handlers;

import java.util.WeakHashMap;

import me.wait.fishyaddons.config.ConfigHandler;
import me.wait.fishyaddons.util.ArmorStandTweaks;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class ArmorStandHandler {
    private static ArmorStandHandler instance;
    private static boolean isRegistered = false;
    
    // Cache of armor stands that should be hidden
    private final WeakHashMap<EntityArmorStand, Boolean> hiddenArmorStands = new WeakHashMap<>();
    private int tickCounter = 0;
    private static final int CACHE_REFRESH_INTERVAL = 20;

    public static void updateRegistration() {
        if (ConfigHandler.isHideHotspotEnabled()) {
            if (!isRegistered) {
                instance = new ArmorStandHandler();
                MinecraftForge.EVENT_BUS.register(instance);
                isRegistered = true;
            }
        } else {
            if (isRegistered && instance != null) {
                MinecraftForge.EVENT_BUS.unregister(instance);
                instance = null;
                isRegistered = false;
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        tickCounter++;
        if (tickCounter >= CACHE_REFRESH_INTERVAL) {
            tickCounter = 0;
            refreshHiddenArmorStands();
        }
    }

    private void refreshHiddenArmorStands() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) return;

        hiddenArmorStands.clear();
        for (Entity entity : mc.theWorld.getLoadedEntityList()) {
            if (entity instanceof EntityArmorStand) {
                EntityArmorStand armorStand = (EntityArmorStand) entity;
                
                if (armorStand.hasCustomName()) {
                    String labelText = armorStand.getCustomNameTag();
                    if (ArmorStandTweaks.shouldHide(labelText)) {
                        hiddenArmorStands.put(armorStand, true);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onRenderLivingPre(RenderLivingEvent.Pre event) {
        if (!(event.entity instanceof EntityArmorStand)) return;
        
        EntityArmorStand armorStand = (EntityArmorStand) event.entity;
        if (hiddenArmorStands.containsKey(armorStand)) {
            event.setCanceled(true);
        }
    }
}