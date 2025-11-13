package me.valkeea.fishyaddons.hud.elements.segmented;

import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.feature.skyblock.ChatTimers;
import me.valkeea.fishyaddons.hud.base.SegmentedTextElement;
import net.minecraft.text.Text;

public class TimerDisplay extends SegmentedTextElement {
    
    public TimerDisplay() {
        super(
            Key.HUD_TIMER_ENABLED,
            "Moonglade: ",
            5, 5,
            12,
            0xC8D9C0,
            false,
            true
        );
    }

    @Override
    protected boolean shouldRender() {
        var timer = ChatTimers.getInstance();
        return timer.isBeaconAlarmHudOn() && timer.isBeaconActive() && timer.getBeaconTimer() >= 0;
    }

    @Override
    protected Component[] getComponents() {
        long secondsLeft = ChatTimers.getInstance().getBeaconTimer();
        if (secondsLeft < 0) return new Component[0];
        
        Text label = Text.literal("Moonglade:");
        Text value = Text.literal(" " + formatTime(secondsLeft));
        int labelColor = getCachedState().color;
        int valueColor = 0xFFFFFF;
        
        return new Component[] {
            new Component(label, value, labelColor, valueColor, 0)
        };
    }
    
    private static String formatTime(long seconds) {
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        return String.format("%02d:%02d", minutes, remainingSeconds);
    }
}
