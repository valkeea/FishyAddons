package me.valkeea.fishyaddons.handler;

import net.minecraft.network.packet.s2c.query.PingResultS2CPacket;
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientPing {
    private ClientPing() {}
    private static final Map<Long, Long> pendingPings = new ConcurrentHashMap<>();
    private static volatile int lastPing = -1;

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
}
