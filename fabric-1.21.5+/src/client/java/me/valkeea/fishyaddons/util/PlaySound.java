package me.valkeea.fishyaddons.util;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.ItemConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

public class PlaySound {
    private PlaySound() {}
    public static final Identifier PROTECT_TRIGGER_ID = Identifier.of("fishyaddons", "protect_trigger");
    public static final SoundEvent PROTECT_TRIGGER_EVENT = SoundEvent.of(PROTECT_TRIGGER_ID);
    private static MinecraftClient mc = MinecraftClient.getInstance();

    public static void protectTrigger() {
        if (ItemConfig.isProtectNotiEnabled()) {
            amethyst(1.0F);
        }
    }

    public static void playBindOrLock() {
        if (mc.player != null && ItemConfig.isLockTriggerEnabled()) {
            amethyst(1.2F); // pitched up
        }
    }
        
    public static void playUnbindOrUnlock() {
        if (mc.player != null && ItemConfig.isLockTriggerEnabled()) {
            amethyst(0.8F); // pitched down
        }
    }

    public static void beaconAlarm() {
        if (mc.player != null && FishyConfig.getState("beaconAlarm", false)) {
            mc.player.playSound(SoundEvents.BLOCK_BELL_USE, 1.0F, 1.0F);  
        }
    }

    private static void amethyst(float pitch) {
        if (mc.player != null) {
            mc.player.playSound(PROTECT_TRIGGER_EVENT, 1.0F, pitch);
        }
    }
}
