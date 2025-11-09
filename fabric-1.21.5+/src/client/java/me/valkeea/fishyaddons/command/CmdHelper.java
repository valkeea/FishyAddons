package me.valkeea.fishyaddons.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import me.valkeea.fishyaddons.config.ItemConfig;
import me.valkeea.fishyaddons.util.FishyNotis;
import me.valkeea.fishyaddons.util.JsonUtil;
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

            if (aIndex != bIndex) {
                return Integer.compare(aIndex, bIndex);
            }

            String aAlpha = aText.getString();
            String bAlpha = bText.getString();
            return aAlpha.compareToIgnoreCase(bAlpha);
        });

        FishyNotis.send(Text.literal("Protected Items:").formatted(Formatting.AQUA));
        for (Map.Entry<String, String> entry : entries) {
            Text shown = JsonUtil.deserializeText(entry.getValue());
            FishyNotis.alert(Text.literal(" - ").formatted(Formatting.DARK_GRAY).append(shown));
        }
    }

    private static char getFirstColorCode(Text text) {
        TextColor color = text.getStyle().getColor();
        if (color != null) {
            Formatting formatting = getFormattingFromTextColor(color);
            if (formatting != null) {
                return formatting.getCode();
            }
        }

        if (!text.getSiblings().isEmpty()) {
            return getFirstColorCode(text.getSiblings().get(0));
        }
        return '\0';
    }

    private static Formatting getFormattingFromTextColor(TextColor color) {
        int rgb = color.getRgb() & 0xFFFFFF;
        for (Formatting formatting : Formatting.values()) {
            if (formatting.getColorValue() != null && (formatting.getColorValue() & 0xFFFFFF) == rgb) {
                return formatting;
            }
        }
        return null;
    }    
}
