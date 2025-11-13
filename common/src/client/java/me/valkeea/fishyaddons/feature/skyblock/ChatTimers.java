package me.valkeea.fishyaddons.feature.skyblock;

import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.hud.ui.FishyToast;
import me.valkeea.fishyaddons.tool.PlaySound;

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

    public void refresh() {
        alarmEnabled = me.valkeea.fishyaddons.config.FishyConfig.getState(Key.BEACON_ALARM, false);
        hudEnabled = me.valkeea.fishyaddons.config.FishyConfig.getState(Key.HUD_TIMER_ENABLED, false);
    }

    public boolean isBeaconAlarmOn() {
        return alarmEnabled;
    }

    public boolean isBeaconAlarmHudOn() {
        return hudEnabled && alarmEnabled;
    }
}
