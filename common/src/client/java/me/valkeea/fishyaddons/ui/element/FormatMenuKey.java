package me.valkeea.fishyaddons.ui.element;

import com.mojang.blaze3d.platform.InputConstants;

import me.valkeea.fishyaddons.event.EventPriority;
import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.event.impl.KeyInputEvent;
import me.valkeea.fishyaddons.ui.list.ScRules;
import me.valkeea.fishyaddons.ui.screen.AlertEditScreen;

public final class FormatMenuKey {
    private FormatMenuKey() {}

    public static void register() {
        FaEvents.KEYBOARD_INPUT.register(FormatMenuKey::onKey, EventPriority.HIGH);
    }

    static void onKey(KeyInputEvent e) {
        var screen = e.screen;
        if (!(screen instanceof ScRules ||screen instanceof AlertEditScreen)) return;
        if (e.input.key() != InputConstants.KEY_F3) return;

        if (e.action == InputConstants.PRESS) screen.keyPressed(e.input);
        e.setConsumed(true);
    }
}
