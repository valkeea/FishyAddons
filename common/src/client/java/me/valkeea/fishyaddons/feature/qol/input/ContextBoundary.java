package me.valkeea.fishyaddons.feature.qol.input;

import me.valkeea.fishyaddons.compat.McApi;
import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.ui.element.FormatMenuKey;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public final class ContextBoundary {
    private ContextBoundary() {}

    public static void register() {
        FormatMenuKey.register();
        FaEvents.SCREEN_CHANGE.register(_ -> {
            var mc = Minecraft.getInstance();
            if (mc.isWindowActive()) {
                SlotLockKeys.suppressHeldKey();
                SlotHiderKeys.suppressHeldKey(mc);
            } else {
                SlotLockKeys.cancel();
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> reconcile(mc));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, mc) -> reconcile(mc));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            boolean focused = mc.isWindowActive();

            KeyShortcut.updateFocus(focused);
            SlotHiderKeys.update(mc, focused);

            if (!focused || mc.player == null || !(McApi.screen() instanceof AbstractContainerScreen<?> screen)) {
                if (focused) SlotLockKeys.suppressHeldKey();
                else SlotLockKeys.cancel();
                return;
            }
            
            SlotLockKeys.update(mc, screen);
        });
    }

    public static void onWindowFocusChanged(boolean focused) {
        if (!focused) {
            onUnfocus();
            return;
        }
        var mc = Minecraft.getInstance();
        KeyShortcut.updateFocus(true);
        SlotHiderKeys.focusRegained(mc);
        SlotLockKeys.suppressHeldKey();
    }

    static void reconcile(Minecraft mc) {
        if (!mc.isWindowActive()) {
            onUnfocus();
            return;
        }
        KeyShortcut.suppressHeldKeys();
        SlotHiderKeys.suppressHeldKey(mc);
        SlotLockKeys.suppressHeldKey();
    }

    private static void onUnfocus() {
        KeyShortcut.updateFocus(false);
        SlotHiderKeys.focusLost();
        SlotLockKeys.cancel();
    }
}
