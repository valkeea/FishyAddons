package me.valkeea.fishyaddons.util.text;

import java.util.Collection;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

public class TablistUtils {
    private TablistUtils() {}

    public static List<Component> getLines() {

        var mc = Minecraft.getInstance();
        if (mc.player == null) return java.util.Collections.emptyList();

        List<Component> lines = new java.util.ArrayList<>();

        if (mc.player != null && mc.player.connection != null) {
            Collection<PlayerInfo> entries = mc.player.connection.getListedOnlinePlayers();

            for (PlayerInfo e : entries) {
                var displayName = e.getTabListDisplayName();

                if (displayName != null) {
                    lines.add(displayName);
                }
            }
        }
        
        return lines;
    }
}
