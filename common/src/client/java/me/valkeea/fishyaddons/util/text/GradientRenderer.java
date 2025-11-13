package me.valkeea.fishyaddons.util.text;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

public class GradientRenderer {
    private GradientRenderer() {}

    private static final Map<String, int[]> PRESET_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, String> PRESETS = new HashMap<>();
    
    static {
        PRESETS.put("warm", "FF74B3>FFB0DE>DEBCF6>FFBCCA");
        PRESETS.put("sky", "2E85D2>CBD7F0>FDE5C2>92AFF1");
        PRESETS.put("yangii", "877DF5>09E6A8>CCB9FA>C5CED7");
        PRESETS.put("atoll", "0A5FC8>12E6B0>00BCE7>0060D1");
        PRESETS.put("purple", "9052B2>FCD0DE>FEA4D0>B51A8D");
        PRESETS.put("pink", "B6AFFD>84D1FD>CEB8FD>F073F6");
        PRESETS.put("night", "402E90>0A4ACB>743DB2>83B9F1");
        PRESETS.put("soft", "83B9F7>D26DFB>F15CF6>FFE2C0");
        PRESETS.put("melon", "FC6B78>E4E5E9>A8EDF4");
        PRESETS.put("fishy", "F85AE5>A1F6CF>5AEFE9>C3BAD7");
        PRESETS.put("mint", "C4FA9A>F1F1B3>94E5E9");
        PRESETS.put("moonlit", "13F5FA>BC86FE>13F5FA");
        PRESETS.put("peach", "FEB884>FC1F6E>FEB884");
        PRESETS.put("sun", "FF9A76>FCD77F>FF9A76");
        PRESETS.put("ocean", "1E90FF>00BFFF>87CEEB");
        PRESETS.put("epic", "FF00FF>8000FF>FF00FF");
        PRESETS.put("thulite", "FFB6C1>FF69B4>FFB6C1");
        PRESETS.put("sunset", "FF0084>FF4500>FF8C00>FFD700");
        PRESETS.put("depths", "000080>1E90FF>87CEEB");
        PRESETS.put("emerald", "228B22>32CD32>90EE90");
        PRESETS.put("rose", "FF007F>FF69B4>FF1493");
        PRESETS.put("ruby", "FF0000>FF4500>FF6347");
        PRESETS.put("sapphire", "0000FF>1E90FF>00BFFF");
        PRESETS.put("emerald", "008000>32CD32>90EE90");
        PRESETS.put("aquamarine", "7FFFD4>40E0D0>E0FFFF");
        PRESETS.put("slayer", "8B0000>B22222>FF0000");
        PRESETS.put("peridot", "ADFF2F>7CFC00>00FF00");
        PRESETS.put("end", "8A2BE2>4B0082>0000FF");
        PRESETS.put("nebula", "E926FF>9812FF>262AFF");       
        PRESETS.put("snow", "FFFFFF>E0FFFF>AFEEEE>ADD8E6>87CEEB");        
        PRESETS.put("pastel", "FFB3BA>FFDFBA>FFFFBA>BAFFC9>BAE1FF");
        PRESETS.put("metal", "334455>7F8FE>FED3E0>C2B2F1>4A7C94");   
        PRESETS.put("gold", "A9630B>FCAF1E>B36F16>FAE079>E29311>E69B13");                
        PRESETS.put("chartreuse", "1DAF44>98FE64>EBFF6B>E0FE55>73D14B>29C347");        
        PRESETS.put("abyss", "1D1D30>262658>22227E>000099>0000CC>0000FF");         
        PRESETS.put("opal", "FBCBE3>C19CEF>ADD8F3>EBF5E2>FBC5DF>ADACF7>B7F1ED>F1F3E3>FCD2E6");
        PRESETS.put("holo", "4FB2FC>6AFBE0>C3F2DA>F8C7DA>C4B6F1>51BBF8>51BBF8>C1F0D5>A1F4DA>C4D1E0>57ACFA");        
        PRESETS.put("rainbow", "FF0000>FF8000>FFFF00>80FF00>00FF00>00FF80>00FFFF>0080FF>0000FF>8000FF>FF00FF>FF0080");
        init();
    }

    public static void init() {
        if (PRESET_CACHE.isEmpty()) {
            precacheAllGradients();
        }
    }
    
    private static void precacheAllGradients() {
        int[] commonLengths = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 15, 18, 20, 25, 30};
        
        for (String gradientName : PRESETS.keySet()) {
            for (int length : commonLengths) {
                String cacheKey = gradientName + ":" + length;
                PRESET_CACHE.put(cacheKey, calcGradient(gradientName, length));
            }
        }
    }
    
    public static Text renderGradientText(String text, String gradientName, Style baseStyle) {
        if (text == null || text.isEmpty()) {
            return Text.literal("");
        }
        
        if (!PRESETS.containsKey(gradientName.toLowerCase())) {
            int fallbackColor = getStartColor("meow");
            return Text.literal(text).setStyle(baseStyle.withColor(TextColor.fromRgb(fallbackColor)));
        }
        
        // Use code point length for proper Unicode handling
        int codePointLength = text.codePointCount(0, text.length());
        
        if (codePointLength == 1) {
            int color = getStartColor(gradientName);
            return Text.literal(text).setStyle(baseStyle.withColor(TextColor.fromRgb(color)));
        }
        
        String cacheKey = gradientName.toLowerCase() + ":" + codePointLength;
        int[] colors = PRESET_CACHE.get(cacheKey);
        
        if (colors == null) {
            colors = calcGradient(gradientName, codePointLength);
        }
        
        return buildText(text, colors, baseStyle);
    }
    
    private static int[] calcGradient(String gradientName, int length) {
        String gradientDef = PRESETS.get(gradientName.toLowerCase());
        if (gradientDef == null) {
            gradientDef = PRESETS.get("meow");
        }
        
        String[] colorHexes = gradientDef.split(">");
        
        if (colorHexes.length < 2) {
            int color = parseHex(colorHexes[0]);
            int[] result = new int[length];
            for (int i = 0; i < length; i++) {
                result[i] = color;
            }
            return result;
        }
        
        int[] keyColors = new int[colorHexes.length];
        for (int i = 0; i < colorHexes.length; i++) {
            keyColors[i] = parseHex(colorHexes[i]);
        }
        
        int[] result = new int[length];
        for (int i = 0; i < length; i++) {
            result[i] = interpolate(keyColors, (float) i / (length - 1));
        }
        return result;
    }
    
    private static Text buildText(String text, int[] colors, Style baseStyle) {
        MutableText result = Text.literal("");
        int[] codePoints = text.codePoints().toArray();
        
        if (codePoints.length == 0) {
            return result;
        }
        
        int[] adjustedColors = colors;
        if (colors.length != codePoints.length) {
            adjustedColors = new int[codePoints.length];
            for (int i = 0; i < codePoints.length; i++) {
                adjustedColors[i] = colors[Math.min(i, colors.length - 1)];
            }
        }

        int currentColor = adjustedColors[0];
        StringBuilder currentSegment = new StringBuilder();
        
        for (int i = 0; i < codePoints.length; i++) {
            if (adjustedColors[i] == currentColor) {
                currentSegment.appendCodePoint(codePoints[i]);
            } else {
                if (!currentSegment.isEmpty()) {
                    result.append(Text.literal(currentSegment.toString())
                        .setStyle(baseStyle.withColor(TextColor.fromRgb(currentColor))));
                }
                
                // Start new segment
                currentColor = adjustedColors[i];
                currentSegment = new StringBuilder();
                currentSegment.appendCodePoint(codePoints[i]);
            }
        }
        
        // Final segment
        if (!currentSegment.isEmpty()) {
            result.append(Text.literal(currentSegment.toString())
                .setStyle(baseStyle.withColor(TextColor.fromRgb(currentColor))));
        }
        return result;
    }

    /**
     * Renders a custom gradient like "FF0000>0000FF" manually
     */
    public static Text renderCustomGradient(String text, String gradientSpec, Style baseStyle) {
        if (text == null || text.isEmpty()) {
            return Text.literal("");
        }
        
        String[] colorStrings = gradientSpec.split(">");
        if (colorStrings.length < 2) {
            try {
                int color = Integer.parseInt(colorStrings[0], 16);
                return Text.literal(text).setStyle(baseStyle.withColor(TextColor.fromRgb(color)));
            } catch (NumberFormatException e) {
                return Text.literal(text).setStyle(baseStyle);
            }
        }
        
        // Convert color strings to integers
        int[] colors = new int[colorStrings.length];
        for (int i = 0; i < colorStrings.length; i++) {
            try {
                colors[i] = Integer.parseInt(colorStrings[i].trim(), 16);
            } catch (NumberFormatException e) {
                colors[i] = 0xFFFFFF;
            }
        }
        
        int[] codePoints = text.codePoints().toArray();
        if (codePoints.length == 1) {
            return Text.literal(text).setStyle(baseStyle.withColor(TextColor.fromRgb(colors[0])));
        }
        
        MutableText result = Text.literal("");
        int codePointLength = codePoints.length;
        
        for (int i = 0; i < codePointLength; i++) {
            // Calculate position in gradient (0.0 to 1.0)
            float position = (float) i / (codePointLength - 1);
            
            // Interpolate between colors
            int interpolatedColor = interpolate(colors, position);
            
            // Add character with interpolated color - properly handle Unicode
            String character = new String(new int[]{codePoints[i]}, 0, 1);
            Style charStyle = baseStyle.withColor(TextColor.fromRgb(interpolatedColor));
            result.append(Text.literal(character).setStyle(charStyle));
        }
        
        return result;
    }
    
    /**
     * Interpolate color between multiple key points
     */
    private static int interpolate(int[] keyColors, float position) {
        if (keyColors.length == 1) return keyColors[0];
        
        position = Math.clamp(position, 0, 1);
        
        float segmentSize = 1.0f / (keyColors.length - 1);
        int segment = Math.min((int) (position / segmentSize), keyColors.length - 2);
        float localPosition = (position - segment * segmentSize) / segmentSize;
        
        int color1 = keyColors[segment];
        int color2 = keyColors[segment + 1];
        
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        
        int r = (int) (r1 + (r2 - r1) * localPosition);
        int g = (int) (g1 + (g2 - g1) * localPosition);
        int b = (int) (b1 + (b2 - b1) * localPosition);
        
        return (r << 16) | (g << 8) | b;
    }
    
    private static int parseHex(String hex) {
        try {
            if (hex.startsWith("#")) hex = hex.substring(1);
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            return 0xFFFFFF;
        }
    }
    
    private static int getStartColor(String gradientName) {
        String gradientDef = PRESETS.get(gradientName.toLowerCase());
        if (gradientDef == null) {
            return 0xFFC0CB;
        }
        String firstColor = gradientDef.split(">")[0];
        return parseHex(firstColor);
    }
    
    public static int getGradientStartColor(String gradientName) {
        return getStartColor(gradientName);
    }
    
    public static int[] getGradientColors(String gradientName) {
        String gradientDef = PRESETS.get(gradientName.toLowerCase());
        if (gradientDef == null) {
            return new int[]{0x888888, 0xAAAAAA};
        }
        
        String[] colorHexes = gradientDef.split(">");
        int[] colors = new int[colorHexes.length];
        for (int i = 0; i < colorHexes.length; i++) {
            colors[i] = parseHex(colorHexes[i]);
        }
        return colors;
    }
    
    public static String[] getAvailableGradients() {
        return PRESETS.keySet().toArray(new String[0]);
    }
}
