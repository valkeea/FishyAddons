package me.valkeea.fishyaddons.util.text;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

public class GradientRenderer {
    private GradientRenderer() {}
    
    private static final Map<String, int[]> PRESET_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, String> PRESETS = new HashMap<>();
    
    static {
        PRESETS.put("ocean", "1E90FF>00BFFF>87CEEB");
        PRESETS.put("fire", "FF0000>FF4500>FF8C00");
        PRESETS.put("shore", "00FFCC>66FFFF>CCFFFF");
        PRESETS.put("leg", "FF8000>FFD700>FF8000");
        PRESETS.put("myth", "FF00FF>8000FF>FF00FF");
        PRESETS.put("meow", "FFB6C1>FF69B4>FFB6C1");
        PRESETS.put("sunset", "FF0084>FF4500>FF8C00>FFD700");
        PRESETS.put("depths", "000080>1E90FF>87CEEB");
        PRESETS.put("emerald", "228B22>32CD32>90EE90");
        PRESETS.put("ice", "87CEEB>B0E0E6>F0F8FF");
        PRESETS.put("void", "000000>003300>000000");
        PRESETS.put("rose", "FF007F>FF69B4>FF1493");
        PRESETS.put("gold", "FFD700>FFFF00>FFFACD");
        PRESETS.put("silver", "C0C0C0>FFFFFF>D3D3D3");
        PRESETS.put("ruby", "FF0000>FF4500>FF6347");
        PRESETS.put("sapphire", "0000FF>1E90FF>00BFFF");
        PRESETS.put("emerald", "008000>32CD32>90EE90");
        PRESETS.put("aquamarine", "7FFFD4>40E0D0>E0FFFF");
        PRESETS.put("onyx", "0F0F0F>2F2F2F>4F4F4F");
        PRESETS.put("slayer", "8B0000>B22222>FF0000");
        PRESETS.put("peridot", "ADFF2F>7CFC00>00FF00");
        PRESETS.put("end", "8A2BE2>4B0082>0000FF");
        PRESETS.put("nebula", "E926FF>9812FF>262AFF");
        PRESETS.put("fishy", "00FFFF>7FFFD4>FF00FF");
        PRESETS.put("swamp", "2E8B57>3CB371>20B2AA");
        PRESETS.put("abyss", "262626>000033>000066>000099>0000CC>0000FF");        
        PRESETS.put("snow", "FFFFFF>E0FFFF>AFEEEE>ADD8E6>87CEEB");        
        PRESETS.put("candy", "FFB6C1>FF69B4>BA55D3>9370DB>00BFFF");
        PRESETS.put("pastel", "FFB3BA>FFDFBA>FFFFBA>BAFFC9>BAE1FF");
        PRESETS.put("rainbow", "FF0000>FF8000>FFFF00>80FF00>00FF00>00FF80>00FFFF>0080FF>0000FF>8000FF>FF00FF>FF0080");
        init();
    }

    public static void init() {
        if (PRESET_CACHE.isEmpty() && FishyConfig.getState(Key.CHAT_FILTER_ENABLED, false) || FishyConfig.getState(Key.CHAT_FILTER_SC_ENABLED, false)) {
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
        
        if (text.length() == 1) {
            int color = getStartColor(gradientName);
            return Text.literal(text).setStyle(baseStyle.withColor(TextColor.fromRgb(color)));
        }
        
        String cacheKey = gradientName.toLowerCase() + ":" + text.length();
        int[] colors = PRESET_CACHE.get(cacheKey);
        
        if (colors == null) {
            colors = calcGradient(gradientName, text.length());
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

        // Batch consecutive characters of same color
        int currentColor = colors[0];
        StringBuilder currentSegment = new StringBuilder();
        
        for (int i = 0; i < text.length(); i++) {
            if (colors[i] == currentColor) {
                currentSegment.append(text.charAt(i));
            } else {
                if (!currentSegment.isEmpty()) {
                    result.append(Text.literal(currentSegment.toString())
                        .setStyle(baseStyle.withColor(TextColor.fromRgb(currentColor))));
                }
                
                // Start new segment
                currentColor = colors[i];
                currentSegment = new StringBuilder();
                currentSegment.append(text.charAt(i));
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
    protected static Text renderCustomGradient(String text, String gradientSpec, Style baseStyle) {
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
        
        if (text.length() == 1) {
            return Text.literal(text).setStyle(baseStyle.withColor(TextColor.fromRgb(colors[0])));
        }
        
        MutableText result = Text.literal("");
        int textLength = text.length();
        
        for (int i = 0; i < textLength; i++) {
            // Calculate position in gradient (0.0 to 1.0)
            float position = (float) i / (textLength - 1);
            
            // Interpolate between colors
            int interpolatedColor = interpolate(colors, position);
            
            // Add character with interpolated color
            String character = String.valueOf(text.charAt(i));
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
    
    public static String[] getAvailableGradients() {
        return PRESETS.keySet().toArray(new String[0]);
    }
}