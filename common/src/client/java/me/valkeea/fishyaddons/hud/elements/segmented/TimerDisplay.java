package me.valkeea.fishyaddons.hud.elements.segmented;

import me.valkeea.fishyaddons.feature.skyblock.timer.ChatTimers;
import me.valkeea.fishyaddons.hud.base.SegmentedTextElement;
import me.valkeea.fishyaddons.hud.core.HudUtils;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import net.minecraft.network.chat.Component;

public class TimerDisplay extends SegmentedTextElement {
    
    public TimerDisplay() {
        super(
            BooleanKey.HUD_TIMER_ENABLED,
            "Beacon: ",
            5, 5,
            12,
            0xFFC8D9C0,
            false,
            false
        );
    }

    @Override
    protected boolean shouldRender() {
        var timer = ChatTimers.getInstance();
        return timer.isBeaconAlarmHudOn() && timer.isBeaconActive() && timer.getBeaconTimer() >= 0;
    }

    @Override
    protected Pair[] getContent() {
        long secondsLeft = ChatTimers.getInstance().getBeaconTimer();
        if (secondsLeft < 0) return new Pair[0];
        
        var label = Component.literal("Beacon:");
        var value = Component.literal(" " + HudUtils.formatTime(secondsLeft));
        int labelColor = getCachedState().color;
        int valueColor = 0xFFFFFFFF;
        
        return new Pair[] {
            new Pair(label, value, labelColor, valueColor)
        };
    }
}
