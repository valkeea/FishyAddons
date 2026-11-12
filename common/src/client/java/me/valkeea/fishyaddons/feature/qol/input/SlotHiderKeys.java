package me.valkeea.fishyaddons.feature.qol.input;

import java.util.Set;

import me.valkeea.fishyaddons.compat.McApi;
import me.valkeea.fishyaddons.feature.skyblock.SlotHider;
import me.valkeea.fishyaddons.mixin.AbstractContainerScreenAccessor;
import me.valkeea.fishyaddons.util.Keyboard;
import me.valkeea.fishyaddons.vconfig.annotation.VCListener;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.api.StringKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

@VCModule
final class SlotHiderKeys {
    private SlotHiderKeys() {}

    private static final KeyPressTracker<Integer> keys = new KeyPressTracker<>();
    private static int keyCode;
    private static boolean changed;
    private static boolean resynchronize;

    @VCListener(strings = StringKey.KEY_HIDE_GUI)
    static void onKeyConfigChange(String v) {
        changed = keyCode != 0;
        keyCode = "NONE".equals(v) ? -1 : Keyboard.getKeyCodeFromString(v);
    }

    static void update(Minecraft mc, boolean focused) {
        if (keyCode == -1) return;
        if (!focused) {
            focusLost();
            return;
        }

        boolean allowed = !resynchronize && mc.player != null
            && McApi.screen() instanceof AbstractContainerScreen<?>;
        resynchronize = false;
        observe(mc, allowed);
    }

    static void suppressHeldKey(Minecraft mc) {
        observe(mc, false);
    }

    static void focusLost() {
        resynchronize = true;
    }

    static void focusRegained(Minecraft mc) {
        resynchronize = false;
        observe(mc, false);
    }
    
    private static void observe(Minecraft mc, boolean allowed) {
        if (changed) {
            keys.retainKeys(Set.of(keyCode));
            changed = false;
            allowed = false;
        }

        if (keyCode == -1) return;

        boolean pressed = Keyboard.isKeyDown(keyCode);
        if (!keys.update(keyCode, pressed, allowed))  return;

        var screen = (AbstractContainerScreen<?>) McApi.screen();
        var hovered = ((AbstractContainerScreenAccessor) screen).getHoveredSlot();
        if (hovered == null || hovered.container == mc.player.getInventory()) return;
        
        SlotHider.addGuiSlot(screen.getTitle().getString(), hovered.index);
    }
}
