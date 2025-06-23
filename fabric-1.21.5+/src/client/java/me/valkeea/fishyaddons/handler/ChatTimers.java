package me.valkeea.fishyaddons.handler;

import me.valkeea.fishyaddons.hud.FishyToast;
import me.valkeea.fishyaddons.util.PlaySound;

public class ChatTimers {
    private static final ChatTimers INSTANCE = new ChatTimers();
    private ChatTimers() {}
    private long timerStart = 0L;
    private boolean timerAlerted = false;

    public static ChatTimers getInstance() {
        return INSTANCE;
    }

    public void beaconStart() {
        if (timerStart == 0L) {
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
        } else if (timer > 0) {
            timerAlerted = false;
        }
    }
}