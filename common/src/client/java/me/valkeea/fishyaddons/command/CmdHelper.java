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
        List<Map.Entry<String, String>> entries = new ArrayList<>(ItemConfig.getProtectedUUIDs().entrySet());

        entries.sort((a, b) -> {
            var aText = JsonUtil.deserializeText(a.getValue());
            var bText = JsonUtil.deserializeText(b.getValue());

            char aColor = getFirstColorCode(aText);
            char bColor = getFirstColorCode(bText);

            int aIndex = colorOrder.indexOf(aColor);
            int bIndex = colorOrder.indexOf(bColor);

            if (aIndex == -1) aIndex = colorOrder.length();
            if (bIndex == -1) bIndex = colorOrder.length();
            if (aIndex != bIndex)  return Integer.compare(aIndex, bIndex);

            var aAlpha = aText.getString();
            var bAlpha = bText.getString();
            return aAlpha.compareToIgnoreCase(bAlpha);
        });

        FishyNotis.send(Component.literal("Protected Items:").withStyle(ChatFormatting.AQUA));
        
        for (Map.Entry<String, String> e : entries) {

            var line = JsonUtil.deserializeText(e.getValue());
            var btn = ChatButton.create("/fg remove " + e.getKey(), "Remove");

            FishyNotis.alert(
                Component.literal(" - ").withStyle(s -> s.withColor(0xFFAAAAAA))
                .append(line).append(Component.literal(" "))
                .append(btn)
            );
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
        mc.player.sendSystemMessage(Component.literal(" ").append(yes).append(Component.literal(" ")).append(no));
    }
    
    public static int checkGUI() {
        var screen = McApi.screen();
        if (screen != null && !(screen instanceof ChatScreen)) {
            return 1;
        }
        return 0;
    }    
}
