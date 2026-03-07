package me.valkeea.fishyaddons.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import me.valkeea.fishyaddons.config.ItemConfig;
import me.valkeea.fishyaddons.util.FishyNotis;
import me.valkeea.fishyaddons.util.JsonUtil;
import me.valkeea.fishyaddons.util.text.ChatButton;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

public class CmdHelper {
    private CmdHelper() {}

    public static void sendSortedProtectedList() {

        String colorOrder = "4a956dbc\0";
        List<Map.Entry<String, String>> entries = new ArrayList<>(ItemConfig.getProtectedUUIDs().entrySet());

        entries.sort((a, b) -> {
            Text aText = JsonUtil.deserializeText(a.getValue());
            Text bText = JsonUtil.deserializeText(b.getValue());

            char aColor = getFirstColorCode(aText);
            char bColor = getFirstColorCode(bText);

            int aIndex = colorOrder.indexOf(aColor);
            int bIndex = colorOrder.indexOf(bColor);

            if (aIndex == -1) aIndex = colorOrder.length();
            if (bIndex == -1) bIndex = colorOrder.length();
            if (aIndex != bIndex)  return Integer.compare(aIndex, bIndex);

            String aAlpha = aText.getString();
            String bAlpha = bText.getString();
            return aAlpha.compareToIgnoreCase(bAlpha);
        });

        FishyNotis.send(Text.literal("Protected Items:").formatted(Formatting.AQUA));
        
        for (Map.Entry<String, String> e : entries) {

            var line = JsonUtil.deserializeText(e.getValue());
            var btn = ChatButton.create("/fg remove " + e.getKey(), "Remove");

            FishyNotis.alert(
                Text.literal(" - ").styled(s -> s.withColor(0xFFAAAAAA))
                .append(line).append(Text.literal(" "))
                .append(btn)
            );
        }
    }

    private static char getFirstColorCode(Text text) {

        var color = getActualColor(text);
        char code = '\0';
        
        if (color != null) {
            var formatting = getFormattingFromTextColor(color);
            if (formatting != null) code = formatting.getCode();
        }

        return code;
    }

    private static TextColor getActualColor(Text text) {

        var siblings = text.getSiblings();
        if (siblings.isEmpty()) return text.getStyle().getColor();

        else return siblings.stream()
            .filter(s -> !s.getString().isEmpty() && Character.isAlphabetic(s.getString().charAt(0)))
            .map(s -> s.getStyle().getColor())
            .findFirst()
            .orElse(null);
    }

    private static Formatting getFormattingFromTextColor(TextColor color) {
        
        int rgb = color.getRgb() & 0xFFFFFFFF;
        for (var candidate : Formatting.values()) {

            var v = candidate.getColorValue();
            if (v != null && (v & 0xFFFFFFFF) == rgb) {
                return candidate;
            }
        }

        return null;
    }

    public static void sendClickable(String onAccept, String onDecline) {
        var mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        Text yes = Text.literal("[Yes]")
            .styled(style -> style.withClickEvent(new net.minecraft.text.ClickEvent.RunCommand(onAccept)).withColor(0xFFCCFFCC));
        Text no = Text.literal("[No]")
            .styled(style -> style.withClickEvent(new net.minecraft.text.ClickEvent.RunCommand(onDecline)).withColor(0xFFFF8080));
        mc.player.sendMessage(Text.literal(" ").append(yes).append(Text.literal(" ")).append(no), false);
    }
    
    public static int checkGUI() {
        if (MinecraftClient.getInstance().currentScreen != null
            && !(MinecraftClient.getInstance().currentScreen instanceof ChatScreen)) {
            return 1;
        }
        return 0;
    }    
}
