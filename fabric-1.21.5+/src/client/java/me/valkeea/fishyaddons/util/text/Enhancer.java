package me.valkeea.fishyaddons.util.text;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

/**
 * Enhanced text parser supporting legacy codes, RGB colors, custom colors, and gradients
 */
public class Enhancer {
    private Enhancer() {}

    private static class ParseResult {
        final Text text;
        final Style finalStyle;
        
        ParseResult(Text text, Style finalStyle) {
            this.text = text;
            this.finalStyle = finalStyle;
        }
    }
    
    private static final Pattern ALL_FORMATTING_PATTERN = Pattern.compile(
        "([§&]([0-9a-fk-or]))|" +
        "([§&](#[0-9A-F]{6}))|" + 
        "([§&]\\{([^}]+)\\})|" +
        "([§&]\\[([^\\]]+)\\])",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Map<String, Integer> COLORS = new HashMap<>();
    
    static { 
        COLORS.put("crystal_teal", 0x0066883);
        COLORS.put("sea_green", 0x2E8B57);
        COLORS.put("coral", 0xFF7F50);
        COLORS.put("common", 0xB0B0B0);
        COLORS.put("unc", 0x55FF55);
        COLORS.put("rare", 0x5555FF);
        COLORS.put("purple", 0xAA00AA);
        COLORS.put("leg", 0xFFAA00);
        COLORS.put("mythic", 0xFF55FF);
        COLORS.put("lapis", 0x2832C2);
        COLORS.put("blue", 0x0000FF);
        COLORS.put("periwinkle", 0x4F86F7);
        COLORS.put("black", 0x000000);
        COLORS.put("dark_blue", 0x0000AA);
        COLORS.put("dark_aqua", 0x00AAAA);
        COLORS.put("dark_red", 0xAA0000);
        COLORS.put("orchid", 0xAF69EF);
        COLORS.put("lavender", 0xE39FF6);
        COLORS.put("magenta", 0xE11584);
        COLORS.put("fuchsia", 0xFC46AA);
        COLORS.put("blush", 0xFEC5E5);
        COLORS.put("heliotrope", 0xDF73FF);
        COLORS.put("dark_gray", 0x555555);
        COLORS.put("yes", 0xCCFFCC);
        COLORS.put("aqua", 0x55FFFF);
        COLORS.put("red", 0xFF5555);
        COLORS.put("vista_blue", 0x8093F1);
        COLORS.put("sky_blue", 0x72DDF7);
        COLORS.put("yellow", 0xFFFF55);
        COLORS.put("orange", 0xFF8800);
        COLORS.put("taffy", 0xFF69B4);
        COLORS.put("jade", 0x00A86B);
        COLORS.put("magenta", 0xFF00FF);
        COLORS.put("brown", 0x8B4513);
        COLORS.put("lime", 0xB0FC38);
        COLORS.put("celadon", 0xACF2C9);
        COLORS.put("seafoam", 0x3DED97);
        COLORS.put("dark_green", 0x228B22);
        COLORS.put("pine", 0x234F1E);
        COLORS.put("fire", 0xFF4500);
        COLORS.put("violet", 0x483D8B);
    }

    /**
     * Parses an existing Text object for any formatting, mod or vanilla.
     *
     * @param original The original Text to parse
     * @return The parsed text with formatting applied,
     * starting from the first identified code.
     */
    public static Text parseExistingStyle(Text original) {
        String raw = original.getString();
        if (!hasFormattingCodes(raw)) {
            return original;
        }

        if (original.getSiblings().isEmpty()) {
            return parseFormattedText(raw);
        }

        MutableText result = Text.literal("");

        for (Text sibling : original.getSiblings()) {
            Style currentStyle = sibling.getStyle() != null ? sibling.getStyle() : Style.EMPTY;
            String siblingStr = sibling.getString();

            if (siblingStr == null || siblingStr.isEmpty()) {
                continue;
            }

            if (hasFormattingCodes(siblingStr)) {
                Text parsed = parseFormattedText(siblingStr);
                result.append(parsed);
            } else {
                result.append(Text.literal(siblingStr).setStyle(currentStyle));
            }
        }

        return result;
    }

    /**
     * Parses the input text for any formatting, mod or vanilla.
     *
     * @param input The input text to parse
     * @return The parsed text with formatting applied
     */
    public static Text parseFormattedText(String input) {
        if (input == null || input.isEmpty()) {
            return Text.literal("");
        }
        
        if (!hasFormattingCodes(input)) {
            return Text.literal(input);
        }
        
        Pattern gradientPattern = Pattern.compile("[§&]\\[([^\\]]+)\\]([^§&]+)(?=[§&]|$)", Pattern.CASE_INSENSITIVE);
        Matcher gradientMatcher = gradientPattern.matcher(input);
        
        if (gradientMatcher.find()) {
            MutableText result = Text.literal("");
            int lastEnd = 0;
            Style currentStyle = Style.EMPTY;
            
            do {
                if (gradientMatcher.start() > lastEnd) {
                    String beforeText = input.substring(lastEnd, gradientMatcher.start());
                    
                    ParseResult beforeResult = parseAndTrackStyle(beforeText, currentStyle);
                    result.append(beforeResult.text);
                    currentStyle = beforeResult.finalStyle;
                }
                
                String gradientName = gradientMatcher.group(1);
                String gradientText = gradientMatcher.group(2);
                
                Text renderedGradient;
                
                if (gradientName.contains(">")) {
                    renderedGradient = GradientRenderer.renderCustomGradient(gradientText, gradientName, currentStyle);
                } else {
                    renderedGradient = GradientRenderer.renderGradientText(gradientText, gradientName, currentStyle);
                }
                
                result.append(renderedGradient);
                
                lastEnd = gradientMatcher.end();
            } while (gradientMatcher.find());
            
            if (lastEnd < input.length()) {
                String remainingText = input.substring(lastEnd);
                ParseResult remainingResult = parseAndTrackStyle(remainingText, currentStyle);
                result.append(remainingResult.text);
            }
            
            return result;
        }
        
        return parseFormattedTextSimple(input);
    }
    
    /**
     * Simple parsing without gradients
     */
    public static Text parseFormattedTextSimple(String input) {
        if (input == null || input.isEmpty()) {
            return Text.literal("");
        }
        
        MutableText result = Text.literal("");
        Matcher matcher = ALL_FORMATTING_PATTERN.matcher(input);
        
        int lastEnd = 0;
        Style currentStyle = Style.EMPTY;
        
        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                String textBefore = input.substring(lastEnd, matcher.start());
                if (!textBefore.isEmpty()) {
                    result.append(Text.literal(textBefore).setStyle(currentStyle));
                }
            }
            
            currentStyle = processFormattingCode(matcher, currentStyle);
            lastEnd = matcher.end();
        }
        
        if (lastEnd < input.length()) {
            String remainingText = input.substring(lastEnd);
            if (!remainingText.isEmpty()) {
                result.append(Text.literal(remainingText).setStyle(currentStyle));
            }
        }
        
        return result;
    }

    private static ParseResult parseAndTrackStyle(String input, Style initialStyle) {
        if (input == null || input.isEmpty()) {
            return new ParseResult(Text.literal(""), initialStyle);
        }
        
        MutableText result = Text.literal("");
        Matcher matcher = ALL_FORMATTING_PATTERN.matcher(input);
        
        int lastEnd = 0;
        Style currentStyle = initialStyle;
        
        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                String textBefore = input.substring(lastEnd, matcher.start());
                if (!textBefore.isEmpty()) {
                    result.append(Text.literal(textBefore).setStyle(currentStyle));
                }
            }
            
            currentStyle = processFormattingCode(matcher, currentStyle);
            lastEnd = matcher.end();
        }
        
        if (lastEnd < input.length()) {
            String remainingText = input.substring(lastEnd);
            if (!remainingText.isEmpty()) {
                result.append(Text.literal(remainingText).setStyle(currentStyle));
            }
        }
        
        return new ParseResult(result, currentStyle);
    }    

    private static Style processFormattingCode(Matcher matcher, Style currentStyle) {

        if (matcher.group(1) != null) {
            String formatChar = matcher.group(2);
            return applyLegacyFormatting(currentStyle, formatChar.toLowerCase().charAt(0));
        }
        
        if (matcher.group(3) != null) {
            String hexColor = matcher.group(4);
            return applyRgbColor(currentStyle, hexColor);
        }
        
        if (matcher.group(5) != null) {
            String name = matcher.group(6);
            return applyCustomColorOrFormat(currentStyle, name);
        }
        return currentStyle;
    }
    
    private static Style applyLegacyFormatting(Style currentStyle, char formatChar) {
        String formatChars = "0123456789abcdefklmnor";
        Formatting[] formatMap = {
            Formatting.BLACK, Formatting.DARK_BLUE, Formatting.DARK_GREEN, Formatting.DARK_AQUA,
            Formatting.DARK_RED, Formatting.DARK_PURPLE, Formatting.GOLD, Formatting.GRAY,
            Formatting.DARK_GRAY, Formatting.BLUE, Formatting.GREEN, Formatting.AQUA,
            Formatting.RED, Formatting.LIGHT_PURPLE, Formatting.YELLOW, Formatting.WHITE,
            Formatting.OBFUSCATED, Formatting.BOLD, Formatting.STRIKETHROUGH, Formatting.UNDERLINE,
            Formatting.ITALIC, null
        };
        
        int index = formatChars.indexOf(formatChar);
        if (index == -1) return currentStyle;
        
        if (formatChar == 'r') return Style.EMPTY;
        
        Formatting formatting = formatMap[index];
        if (formatting == null) return currentStyle;
        
        if (formatting.isColor()) {
            return Style.EMPTY.withColor(formatting)
                .withBold(currentStyle.isBold())
                .withItalic(currentStyle.isItalic())
                .withUnderline(currentStyle.isUnderlined())
                .withStrikethrough(currentStyle.isStrikethrough())
                .withObfuscated(currentStyle.isObfuscated());
        } else {
            switch (formatting) {
                case BOLD: return currentStyle.withBold(true);
                case ITALIC: return currentStyle.withItalic(true);
                case UNDERLINE: return currentStyle.withUnderline(true);
                case STRIKETHROUGH: return currentStyle.withStrikethrough(true);
                case OBFUSCATED: return currentStyle.withObfuscated(true);
                default: return currentStyle;
            }
        }
    }
    
    private static Style applyRgbColor(Style currentStyle, String hexColor) {
        try {
            int color = Integer.parseInt(hexColor.substring(1), 16);
            return Style.EMPTY.withColor(TextColor.fromRgb(color))
                .withBold(currentStyle.isBold())
                .withItalic(currentStyle.isItalic())
                .withUnderline(currentStyle.isUnderlined())
                .withStrikethrough(currentStyle.isStrikethrough())
                .withObfuscated(currentStyle.isObfuscated());
        } catch (NumberFormatException e) {
            return currentStyle;
        }
    }
    
    private static Style applyCustomColorOrFormat(Style currentStyle, String name) {
        String lowerName = name.toLowerCase();
        
        switch (lowerName) {
            case "bold":
                return currentStyle.withBold(true);
            case "italic":
                return currentStyle.withItalic(true);
            case "underline":
                return currentStyle.withUnderline(true);
            case "strikethrough":
                return currentStyle.withStrikethrough(true);
            case "obfuscated":
                return currentStyle.withObfuscated(true);
            case "reset":
                return Style.EMPTY;
            default:
                // Not a formatting code
                break;
        }
        
        Integer color = COLORS.get(lowerName);
        if (color != null) {
            return Style.EMPTY.withColor(TextColor.fromRgb(color))
                .withBold(currentStyle.isBold())
                .withItalic(currentStyle.isItalic())
                .withUnderline(currentStyle.isUnderlined())
                .withStrikethrough(currentStyle.isStrikethrough())
                .withObfuscated(currentStyle.isObfuscated());
        }
            if (lowerName.matches("^[0-9a-fA-F]{6}$")) {
                try {
                    int hexColor = Integer.parseInt(lowerName, 16);
                    return Style.EMPTY.withColor(TextColor.fromRgb(hexColor))
                        .withBold(currentStyle.isBold())
                        .withItalic(currentStyle.isItalic())
                        .withUnderline(currentStyle.isUnderlined())
                        .withStrikethrough(currentStyle.isStrikethrough())
                        .withObfuscated(currentStyle.isObfuscated());
                } catch (NumberFormatException e) {
                    // ignore, fall through
                }
            }
            return currentStyle;
    }
    
    public static boolean hasFormattingCodes(String input) {
        return ALL_FORMATTING_PATTERN.matcher(input).find();
    }
    
    public static String stripFormattingCodes(String input) {
        if (input == null) return null;
        return ALL_FORMATTING_PATTERN.matcher(input).replaceAll("");
    }
    
    public static Map<String, Integer> getCustomColors() {
        return new HashMap<>(COLORS);
    }

    public static String[] getAllCustomFormats() {
        String[] availableGradients = GradientRenderer.getAvailableGradients();
        
        String[] customFormats = {
            "§{bold}", "§{italic}", "§{underline}", "§{strikethrough}", "§{obfuscated}", "§{reset}"
        };
        
        java.util.List<String> result = new java.util.ArrayList<>();
        
        Arrays.stream(availableGradients)
            .sorted((g1, g2) -> compareByColorOrder(
                GradientRenderer.getGradientStartColor(g1), 
                GradientRenderer.getGradientStartColor(g2)
            ))
            .map(name -> "§[" + name + "]")
            .forEach(result::add);
        
        COLORS.entrySet().stream()
            .sorted((e1, e2) -> compareByColorOrder(e1.getValue(), e2.getValue()))
            .map(entry -> "§{" + entry.getKey() + "}")
            .forEach(result::add);
        
        Arrays.stream(customFormats).forEach(result::add);
        
        return result.toArray(String[]::new);
    }
    
    private static int compareByColorOrder(int color1, int color2) {

        float[] hsv1 = rgbToHsv(color1);
        float[] hsv2 = rgbToHsv(color2);
        
        if (Math.abs(hsv1[0] - hsv2[0]) > 0.01f) {
            return Float.compare(hsv1[0], hsv2[0]);
        }
        if (Math.abs(hsv1[1] - hsv2[1]) > 0.01f) {
            return Float.compare(hsv2[1], hsv1[1]);
        }
        return Float.compare(hsv2[2], hsv1[2]);
    }
    
    private static float[] rgbToHsv(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255.0f;
        float g = ((rgb >> 8) & 0xFF) / 255.0f;
        float b = (rgb & 0xFF) / 255.0f;
        
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        
        float h = 0;
        if (delta != 0) {
            if (max == r) {
                h = ((g - b) / delta) % 6;
            } else if (max == g) {
                h = (b - r) / delta + 2;
            } else {
                h = (r - g) / delta + 4;
            }
            h *= 60;
            if (h < 0) h += 360;
        }
        
        float s = max == 0 ? 0 : delta / max;
        float v = max;
        
        return new float[]{h, s, v};
    }
    
}