package me.valkeea.fishyaddons.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import me.valkeea.fishyaddons.config.ItemConfig;
import me.valkeea.fishyaddons.util.FishyNotis;
import me.valkeea.fishyaddons.util.TextFormatUtil;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;


public class CmdHelper {
    private CmdHelper() {}

    public static void sendSortedProtectedList() {
        // Color order: 4 (red), a (green), 9 (blue), 5 (purple), 6 (gold), d (pink), b (aqua), c (light red), '\0' (no color)
        String colorOrder = "4a956dbc\0";

        List<Map.Entry<String, String>> entries = new ArrayList<>(ItemConfig.getProtectedUUIDs().entrySet());

        entries.sort((a, b) -> {
            Text aText = TextFormatUtil.deserialize(a.getValue());
            Text bText = TextFormatUtil.deserialize(b.getValue());

            // Extract first color code from the Text's style
            char aColor = getFirstColorCode(aText);
            char bColor = getFirstColorCode(bText);

            int aIndex = colorOrder.indexOf(aColor);
            int bIndex = colorOrder.indexOf(bColor);

            if (aIndex == -1) aIndex = colorOrder.length();
            if (bIndex == -1) bIndex = colorOrder.length();

            if (aIndex != bIndex) {
                return Integer.compare(aIndex, bIndex);
            }
            // Sort alphabetically (ignore color)
            String aAlpha = aText.getString();
            String bAlpha = bText.getString();
            return aAlpha.compareToIgnoreCase(bAlpha);
        });

        FishyNotis.send(Text.literal("Protected Items:").formatted(Formatting.AQUA));
        for (Map.Entry<String, String> entry : entries) {
            Text shown = TextFormatUtil.deserialize(entry.getValue());
            FishyNotis.alert(Text.literal(" - ").formatted(Formatting.GRAY).append(shown));
        }
    }

    // Helper to get the first color code character from a Text's style
    private static char getFirstColorCode(Text text) {
        TextColor color = text.getStyle().getColor();
        if (color != null) {
            Formatting formatting = getFormattingFromTextColor(color);
            if (formatting != null) {
                return formatting.getCode();
            }
        }
        // If this Text has children, check the first one recursively
        if (!text.getSiblings().isEmpty()) {
            return getFirstColorCode(text.getSiblings().get(0));
        }
        return '\0'; // No color found
    }

    // Map a TextColor to the closest Formatting color
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
