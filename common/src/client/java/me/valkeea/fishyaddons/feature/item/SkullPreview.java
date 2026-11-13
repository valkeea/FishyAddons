package me.valkeea.fishyaddons.feature.item;

import com.mojang.blaze3d.platform.InputConstants;

import me.valkeea.fishyaddons.event.EventGroup;
import me.valkeea.fishyaddons.event.EventPriority;
import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.event.impl.KeyInputEvent;
import me.valkeea.fishyaddons.mixin.AbstractContainerScreenAccessor;
import me.valkeea.fishyaddons.render.SkullRenderer;
import me.valkeea.fishyaddons.util.FishyNotis;
import me.valkeea.fishyaddons.util.Keyboard;
import me.valkeea.fishyaddons.vconfig.annotation.VCListener;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.api.StringKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

@VCModule
public final class SkullPreview {
    private static int keyCode = -1;
    private static final SelectedSkull selection = new SelectedSkull();
    private static final EventGroup events = new EventGroup();    

    private SkullPreview() {}

    private static void register(boolean v) {
        events.subscribe(v, () ->
            events.add(FaEvents.KEYBOARD_INPUT.register(SkullPreview::onKeyboardInput, EventPriority.LOWEST))
        );
    }

    @VCListener(strings = StringKey.KEY_SKULL_PREVIEW)
    public static void onKeyConfigChange(String v) {
        keyCode = "NONE".equals(v) ? -1 : Keyboard.getKeyCodeFromString(v);
        register(enabled());
    }

    private static boolean enabled() {
        return keyCode != -1;
    }

    static void onKeyboardInput(KeyInputEvent e) {
        if (!enabled() || e.mc.player == null || !matches(e.input)) return;

        if (e.screen instanceof AbstractContainerScreen<?> container) {
            if (e.action == InputConstants.PRESS) handleContainer(container);
        } else if (e.screen == null) {
            if (e.action == InputConstants.PRESS) toggle();
        } else return;
        
        if (e.action != InputConstants.RELEASE) e.setConsumed(true);
    }

    private static boolean matches(KeyEvent input) {
        return keyCode == input.input();
    }

    public static void handleContainer(AbstractContainerScreen<?> screen) {
        var slot = ((AbstractContainerScreenAccessor) screen).getHoveredSlot();
        if (slot != null && selection.capture(slot.getItem())) {
            FishyNotis.send(
                "Skull saved for preview. Use '" + keyName() + "' again after closing the container to toggle it."
            );
        }
    }

    public static void toggle() {
        if (!selection.toggle()) {
            FishyNotis.send("Hover over a skull and press '" + keyName() + "' to save it first.");
            return;
        }
        var info = "Skull preview";
        if (selection.enabled()) FishyNotis.on(info);
        else FishyNotis.off(info);
    }

    public static void apply(LivingEntity entity, HumanoidRenderState state) {
        selection.apply(entity == Minecraft.getInstance().player, state);
    }

    private static String keyName() {
        return Keyboard.getDisplayNameFor(Keyboard.getGlfwKeyName(keyCode));
    }

    private static class SelectedSkull {
        private ItemStack saved = ItemStack.EMPTY;
        private boolean enabled;

        boolean capture(ItemStack stack) {
            if (!SkullRenderer.isSkull(stack)) return false;
            saved = stack.copy();
            return true;
        }

        boolean hasSkull() {
            return !saved.isEmpty();
        }

        boolean toggle() {
            if (!hasSkull()) return false;
            enabled = !enabled;
            return true;
        }

        boolean enabled() {
            return enabled;
        }

        void apply(boolean isLocalPlayer, HumanoidRenderState state) {
            if (!enabled || !isLocalPlayer) return;
            SkullRenderer.applyWornHead(state, saved);
            state.headEquipment = ItemStack.EMPTY;
        }
    }
}
