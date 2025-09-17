package me.valkeea.fishyaddons.util.text;

import java.util.Collection;
import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;


public class TablistUtils {
    private TablistUtils() {}

    public static List<Text> getLines() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return java.util.Collections.emptyList();
        List<Text> lines = new java.util.ArrayList<>();
        if (mc.player != null && mc.player.networkHandler != null) {
            Collection<net.minecraft.client.network.PlayerListEntry> entries = mc.player.networkHandler.getListedPlayerListEntries();
            for (net.minecraft.client.network.PlayerListEntry entry : entries) {
                Text displayName = entry.getDisplayName();
                if (displayName != null) {
                    lines.add(displayName);
                } else {
                    lines.add(Text.literal(entry.getProfile().getName()));
                }
            }
        }
        return lines;
    }
}