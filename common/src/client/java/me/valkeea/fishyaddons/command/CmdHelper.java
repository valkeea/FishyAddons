package me.valkeea.fishyaddons.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import me.valkeea.fishyaddons.compat.McApi;
import me.valkeea.fishyaddons.util.FishyNotis;
import me.valkeea.fishyaddons.util.JsonUtil;
import me.valkeea.fishyaddons.util.text.ChatButton;
import me.valkeea.fishyaddons.vconfig.config.impl.ItemConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

public class CmdHelper {
    private CmdHelper() {}

    public static void sendSortedProtectedList() {

        String colorOrder = "4a956dbc\0";
        List<Map.Entry<String, String>> items = new ArrayList<>(ItemConfig.getProtectedUUIDs().entrySet());

        items.sort((a, b) -> {
            var aName = JsonUtil.deserializeText(a.getValue());
            var bName = JsonUtil.deserializeText(b.getValue());

            char aColor = getFirstColorCode(aName);
            char bColor = getFirstColorCode(bName);

            int aIdx = colorOrder.indexOf(aColor);
            int bIdx = colorOrder.indexOf(bColor);

            if (aIdx == -1) aIdx = colorOrder.length();
            if (bIdx == -1) bIdx = colorOrder.length();
            if (aIdx != bIdx)  return Integer.compare(aIdx, bIdx);

            var aAlpha = aName.getString();
            var bAlpha = bName.getString();
            return aAlpha.compareToIgnoreCase(bAlpha);
        });

        FishyNotis.themed("§lProtected Items:\n");
        
        if (items.isEmpty()) {
            FishyNotis.notice("No items are currently protected."
            + "\nUse /fg add while holding an item to add it to the protected list.");

        } else {
            items.forEach(i -> FishyNotis.alert(
                ChatButton.create("/fg remove " + i.getKey(), "§7🗑")
                .append(Component.literal(" "))
                .append(JsonUtil.deserializeText(i.getValue()))
            ));
        }
    }

    private static char getFirstColorCode(Component text) {

        var color = getActualColor(text);
        char code = '\0';
        
        if (color != null) {
            var formatting = getFormattingFromTextColor(color);
            if (formatting != null) code = formatting.getChar();
        }

        return code;
    }

    private static TextColor getActualColor(Component text) {

        var siblings = text.getSiblings();
        if (siblings.isEmpty()) return text.getStyle().getColor();

        else return siblings.stream()
            .filter(s -> !s.getString().isEmpty() && Character.isAlphabetic(s.getString().charAt(0)))
            .map(s -> s.getStyle().getColor())
            .findFirst()
            .orElse(null);
    }

    private static ChatFormatting getFormattingFromTextColor(TextColor color) {
        
        int rgb = color.getValue() & 0xFFFFFFFF;
        for (var candidate : ChatFormatting.values()) {

            var v = candidate.getColor();
            if (v != null && (v & 0xFFFFFFFF) == rgb) {
                return candidate;
            }
        }

        return null;
    }

    public static void sendClickable(String onAccept, String onDecline) {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        var yes = Component.literal("[Yes]")
            .withStyle(style -> style.withClickEvent(new net.minecraft.network.chat.ClickEvent.RunCommand(onAccept)).withColor(0xFFCCFFCC));
        var no = Component.literal("[No]")
            .withStyle(style -> style.withClickEvent(new net.minecraft.network.chat.ClickEvent.RunCommand(onDecline)).withColor(0xFFFF8080));
        FishyNotis.alert(Component.literal(" ").append(yes).append(Component.literal(" ")).append(no));
    }
    
    public static int checkGUI() {
        var screen = McApi.screen();
        if (screen != null && !(screen instanceof ChatScreen)) {
            return 1;
        }
        return 0;
    }    
}
