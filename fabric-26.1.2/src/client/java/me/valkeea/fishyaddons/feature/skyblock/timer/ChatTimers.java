package me.valkeea.fishyaddons.feature.skyblock.timer;

import me.valkeea.fishyaddons.hud.ui.FishyToast;
import me.valkeea.fishyaddons.tool.PlaySound;
import me.valkeea.fishyaddons.vconfig.annotation.VCListener;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;

@VCModule
@SuppressWarnings("squid:S6548")
public class ChatTimers {
    private static final ChatTimers INSTANCE = new ChatTimers();
    private ChatTimers() {}
    private long timerStart = 0L;
    private boolean timerAlerted = false;
    private boolean alarmEnabled = false;
    private boolean hudEnabled = false;

    public static ChatTimers getInstance() {
        return INSTANCE;
    }

    public void beaconStart() {
        if (alarmEnabled && timerStart == 0L) {
            timerStart = System.currentTimeMillis();
            timerAlerted = false;
        }
    }

    public long getBeaconTimer() {
        if (timerStart == 0L) return 0;
        long elapsed = (System.currentTimeMillis() - timerStart) / 1000;
        long secondsLeft = 600 - elapsed;
        return Math.max(secondsLeft, 0);
    }

    public boolean isBeaconActive() {
        return timerStart > 0L;
    }

    public void checkTimerAlert() {

        long timer = getBeaconTimer();
        
        if (timer == 0 && !timerAlerted && timerStart != 0L) {
            timerAlerted = true;
            FishyToast.show("§b§lMoonglade Beacon Alarm", "Cooldown has been reset!");
            PlaySound.beaconAlarm();
            timerStart = 0L;

        } else if (timer > 0)  timerAlerted = false;
    }

    @VCListener({BooleanKey.BEACON_ALARM, BooleanKey.HUD_TIMER_ENABLED})
    public static void refresh() {
        getInstance();
        INSTANCE.alarmEnabled = Config.get(BooleanKey.BEACON_ALARM);
        INSTANCE.hudEnabled = Config.get(BooleanKey.HUD_TIMER_ENABLED);
    }

    public boolean isBeaconAlarmOn() {
        return alarmEnabled;
    }

    public boolean isBeaconAlarmHudOn() {
        return hudEnabled && alarmEnabled;
    }
}
