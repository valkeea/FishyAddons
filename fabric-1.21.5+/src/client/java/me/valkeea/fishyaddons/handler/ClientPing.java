package me.valkeea.fishyaddons.handler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import me.valkeea.fishyaddons.config.FishyConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket;
import net.minecraft.network.packet.s2c.query.PingResultS2CPacket;
import net.minecraft.util.Util;

public class ClientPing {
    private ClientPing() {}
    private static final Map<Long, Long> pendingPings = new ConcurrentHashMap<>();
    private static volatile int lastPing = -1;
    private static boolean enabled = false;

    public static void send() {
        ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
        if (handler != null) {
            long timestamp = Util.getMeasuringTimeMs();
            pendingPings.put(timestamp, timestamp);
            handler.sendPacket(new QueryPingC2SPacket(timestamp));
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

    public static int get() {
        return lastPing;
    }

    public static void refresh() {
        enabled = FishyConfig.getState(me.valkeea.fishyaddons.config.Key.HUD_PING_ENABLED, false);
    }

    public static boolean isOn() {
        return enabled;
    }    
}