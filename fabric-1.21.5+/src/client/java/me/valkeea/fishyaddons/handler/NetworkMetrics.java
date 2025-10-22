package me.valkeea.fishyaddons.handler;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket;
import net.minecraft.network.packet.s2c.query.PingResultS2CPacket;
import net.minecraft.util.Util;

public class NetworkMetrics {
    private NetworkMetrics() {}
    
    // Ping
    private static final Map<Long, Long> pendingPings = new ConcurrentHashMap<>();
    private static volatile int lastPing = -1;
    
    // KeepAlive-based TPS estimation
    private static final Deque<Long> keepAliveTimestamps = new ArrayDeque<>();
    private static volatile double lastTps = -1;

    private static final int MAX_SAMPLES = 30; // Low for real-time changes
    private static final int START_BASELINE_MS = 570; // Added jitter buffer   
    private static final int TRUE_BASELINE_MS = 500;
    private static final int MIN_VALID_DELTA_MS = 100;
    private static final int BASELINE_500MS_TICKS = 10;
    private static final int BASELINE_1000MS_TICKS = 20;
    private static volatile int baselineMs = START_BASELINE_MS;
    private static volatile int baselineTicks;
    private static volatile long lowestDetectedDelta = Long.MAX_VALUE;    
    
    // State for display
    private static boolean enabled = false;
    private static boolean tpsOn = true;
    private static boolean fpsOn = true;
    private static boolean pingOn = true;

    static {
        calcTicks(baselineMs);
    }

    // -- Ping ---
    public static void send() {

        ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();

        if (handler != null) {
            long timestamp = Util.getMeasuringTimeMs();
            pendingPings.put(timestamp, timestamp);
            handler.sendPacket(new QueryPingC2SPacket(timestamp));
            calcTps();
        }
    }

    public static void onPingResponse(PingResultS2CPacket packet) {
        long sent = packet.startTime();
        long now = Util.getMeasuringTimeMs();
        Long original = pendingPings.remove(sent);
        if (original != null) {
            lastPing = (int) (now - original);
        }
    }

    // --- TPS ---
    public static void onKaS2C() {
        if (!PetInfo.isTablistReady()) return;
        long now = System.currentTimeMillis();

        synchronized (keepAliveTimestamps) {
            long last = keepAliveTimestamps.peekLast() != null ? keepAliveTimestamps.peekLast() : 0;
            long delta = now - last;
            if (delta < MIN_VALID_DELTA_MS && !keepAliveTimestamps.isEmpty()) {
                return;
            }

            keepAliveTimestamps.addLast(now);
            while (keepAliveTimestamps.size() > MAX_SAMPLES) {
                keepAliveTimestamps.removeFirst();
            }

            calcTps();
        }
    }

    private static void calcTps() {
        if (keepAliveTimestamps.size() < 2) {
            lastTps = -1;
            return;
        }

        long first = keepAliveTimestamps.peekFirst();
        long last = keepAliveTimestamps.peekLast();
        int intervals = keepAliveTimestamps.size() - 1;
        long timeSpan = last - first;

        if (intervals <= 0 || timeSpan <= 0) {
            lastTps = -1;
            return;
        }

        long avg = timeSpan / intervals;
        double avgInterval = avg;
        double rawTps = (baselineTicks * 1000.0) / avgInterval;
        // Use lowest detected avg delta to adjust baseline
        updateBaseline(avg);
        lastTps = Math.min(20.0, rawTps);
    }

    private static void updateBaseline(long newDelta) {
        if (newDelta != baselineMs && newDelta >= TRUE_BASELINE_MS && keepAliveTimestamps.size() >= MAX_SAMPLES) {

            if (newDelta <= START_BASELINE_MS && newDelta < lowestDetectedDelta) {
                lowestDetectedDelta = newDelta;
                baselineMs = (int)lowestDetectedDelta;
            } else if (newDelta >= 950) { // likely limbo
                baselineMs = 1000;
            }
            calcTicks(newDelta);
        }
    }

    private static void calcTicks(long deltaMs) {
        if (deltaMs >= 1000) {
            baselineTicks = BASELINE_1000MS_TICKS;
        } else if (deltaMs >= TRUE_BASELINE_MS && deltaMs <= START_BASELINE_MS) {
            baselineTicks = (int)((20.0 * deltaMs) / 1000.0);
        } else {
           baselineTicks = BASELINE_500MS_TICKS;
        }
        
    }    

    public static void reset() {
        synchronized (keepAliveTimestamps) {
            keepAliveTimestamps.clear();
        }
        lastTps = -1;
        baselineMs = START_BASELINE_MS;
        calcTicks(baselineMs);
        lowestDetectedDelta = Long.MAX_VALUE;
    }    

    public static void refresh() {
        enabled = FishyConfig.getState(me.valkeea.fishyaddons.config.Key.HUD_PING_ENABLED, false);
        tpsOn = FishyConfig.getState(me.valkeea.fishyaddons.config.Key.HUD_PING_SHOW_TPS, true);
        fpsOn = FishyConfig.getState(me.valkeea.fishyaddons.config.Key.HUD_PING_SHOW_FPS, true);
        pingOn = FishyConfig.getState(me.valkeea.fishyaddons.config.Key.HUD_PING_SHOW_PING, true);
    }

    public static boolean shouldDisplay(String key) {
        switch (key) {
            case Key.HUD_PING_SHOW_PING:
                return pingOn;
            case Key.HUD_PING_SHOW_TPS:
                return tpsOn;
            case Key.HUD_PING_SHOW_FPS:
                return fpsOn;
            default:
                return true;
        }
    }    

    public static boolean isOn() {
        return enabled;
    }    

    public static double getTps() {
        synchronized (keepAliveTimestamps) {
            if (keepAliveTimestamps.size() >= 5) {
                return lastTps;
            }
        }
        return -1;
    }

    public static int getPing() {
        return lastPing;
    }

    public static String getTpsString() {
        double tps = getTps();
        return tps == -1 ? "?" : String.format(java.util.Locale.US, "%.2f", tps);
    }    
}