package me.valkeea.fishyaddons.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.util.ZoneUtils;
import me.valkeea.fishyaddons.util.text.Enhancer;
import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.text.TextContent;

public class FaColors {
    private static final Object2ObjectOpenHashMap<String, TextColor> global = new Object2ObjectOpenHashMap<>();
    private static final Object2ObjectOpenHashMap<String, TextColor> user = new Object2ObjectOpenHashMap<>();
    private static final Object2ObjectOpenHashMap<String, TextColor> combinedMap = new Object2ObjectOpenHashMap<>();

    private static final Map<String, List<Segment>> segmentCache = new WeakHashMap<>();
    
    private static final Map<Text, Text> rewriteCache = new WeakHashMap<>();
    private static final Map<Text, Text> sidebarCache = new WeakHashMap<>();
    private static final Map<Text, Text> labelCache = new WeakHashMap<>();
    private static final Map<Text, Text> tooltipCache = new WeakHashMap<>();

    private static boolean useGlobal = false;
    private static boolean useCustom = false;
    private static long lastFetchTime = 0;

    public static void init() {
        if (me.valkeea.fishyaddons.config.FishyConfig.getState(
                me.valkeea.fishyaddons.config.Key.GLOBAL_FA_COLORS, false
        )) {
            fetchGlobal();
            refresh();
        }
    }

    private static void fetchGlobal() {
        if (System.currentTimeMillis() - lastFetchTime < 5 * 60 * 1000) {
            return;
        }

        try {
            lastFetchTime = System.currentTimeMillis();
            String url = "https://gist.githubusercontent.com/valkeea/de655343d713bf3378555fe6775f8e3b/raw/facolors.json";
            java.net.URLConnection conn = java.net.URI.create(url).toURL().openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestProperty("User-Agent", "FishyAddons");

            try (java.io.InputStream in = conn.getInputStream();
                 java.io.InputStreamReader reader = new java.io.InputStreamReader(in)) {
                com.google.gson.Gson gson = new com.google.gson.Gson();
                java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<Map<String, String>>(){}.getType();
                Map<String, String> map = gson.fromJson(reader, type);

                if (map == null || map.isEmpty() || map.size() > 100) {
                    throw new IllegalStateException("Invalid gist data");
                }

                global.clear();
                for (var entry : map.entrySet()) {
                    String hex = entry.getValue();
                    int color = Integer.parseInt(hex, 16);
                    global.put(entry.getKey(), TextColor.fromRgb(color));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            useFallbackColors();
        }
    }

    public static void refreshGlobal() {
        fetchGlobal();
        refresh();
    }

    public static void refresh() {
        useGlobal = me.valkeea.fishyaddons.config.FishyConfig.getState(
                me.valkeea.fishyaddons.config.Key.GLOBAL_FA_COLORS, false
        );
        useCustom = me.valkeea.fishyaddons.config.FishyConfig.getState(
                me.valkeea.fishyaddons.config.Key.CUSTOM_FA_COLORS, false
        );
        loadUserMap();
        combine();
        clearCache();
    }

    private static void combine() {
        combinedMap.clear();
        if (useGlobal) combinedMap.putAll(global);
        if (useCustom) combinedMap.putAll(user);
    }

    private static void clearCache() {
        segmentCache.clear();
        rewriteCache.clear();
        sidebarCache.clear();
        labelCache.clear();
        tooltipCache.clear();
    }    

    public static boolean shouldColor() {
        return useGlobal || useCustom;
    }

    private static void useFallbackColors() {
        global.clear();
        global.put("valkeea", TextColor.fromRgb(0xFFB8E4));
        refresh();
    }

    public record Segment(String text, @Nullable TextColor color, @Nullable Style fullStyle) {
        public Segment(String text, @Nullable TextColor color) {
            this(text, color, null);
        }
    }

    /**
    * Restructure input text into segments, each segment either being a plain text,
    * or a matched player name with color.
    * This allows restyling partial matches inside content.
    */
    private static List<Segment> createSegments(String input) {
        if (input.isEmpty()) return List.of();
        
        List<Segment> cached = segmentCache.get(input);
        if (cached != null) return cached;

        List<Segment> segments;

        if (input.contains("&") && FishyConfig.getState(Key.CHAT_FORMATTING, true)) {
            try {
                var parsed = Enhancer.parseFormattedText(input);
                segments = findMatchedNames(parsed);
                segmentCache.put(input, segments);
                return segments;

            } catch (Exception e) {
                System.err.println("[FaColors] Enhancer parsing failed: " + e.getMessage());
            }
        }
        
        segments = splitForMatches(input);
        segmentCache.put(input, segments);
        return segments;
    }

    // Find matched names in parsed Text structure
    private static List<Segment> findMatchedNames(Text parsedText) {
        List<Segment> segments = new ArrayList<>();
        
        parsedText.visit((style, text) -> {

            List<Segment> playerColoredSegments = splitForMatches(text);
            for (Segment seg : playerColoredSegments) {

                if (seg.color != null) {
                    var enhancedStyle = style.withColor(seg.color);
                    segments.add(new Segment(seg.text, seg.color, enhancedStyle));
                } else {
                    segments.add(new Segment(seg.text, style.getColor(), style));
                }
            }
            return Optional.empty();

        }, Style.EMPTY);
        
        return segments;
    }

    // Split input string into segments based on matched player names
    private static List<Segment> splitForMatches(String input) {
        List<Segment> segments = new ArrayList<>();
        int pos = 0;

        while (pos < input.length()) {
            int bestStart = -1;
            int bestEnd = -1;
            TextColor bestColor = null;
            String bestKey = null;

            for (Map.Entry<String, TextColor> e : combinedMap.object2ObjectEntrySet()) {
                String key = e.getKey();
                int idx = input.indexOf(key, pos);
                if (idx >= 0) {
                    int end = idx + key.length();
                    if (bestStart == -1 || idx < bestStart || (idx == bestStart && end > bestEnd)) {
                        bestStart = idx;
                        bestEnd = end;
                        bestColor = e.getValue();
                        bestKey = key;
                    }
                }
            }

            if (bestStart == -1) {
                segments.add(new Segment(input.substring(pos), null));
                break;
            }

            if (bestStart > pos) {
                segments.add(new Segment(input.substring(pos, bestStart), null));
            }

            segments.add(new Segment(bestKey, bestColor));

            pos = bestEnd;
        }
        return segments;
    }

    /**
     * Redirected visit that applies recoloring to all segments.
     */
    public static <T> Optional<T> applyRecolorAll(
            String input,
            Style baseStyle,
            StringVisitable.StyledVisitor<T> visitor
    ) {
        for (Segment seg : createSegments(input)) {
            if (seg.fullStyle != null) {
                visitor.accept(seg.fullStyle, seg.text);
            } else {
                visitor.accept(seg.color == null ? baseStyle : baseStyle.withColor(seg.color), seg.text);
            }
        }
        return Optional.empty();
    }

    private static Text multiple(Text input) {
        TextContent content = input.getContent();
        MutableText result = Text.empty().setStyle(input.getStyle());

        if (content instanceof PlainTextContent plain) {
            for (Segment seg : createSegments(plain.string())) {
                Style segStyle;
                if (seg.fullStyle != null) {
                    segStyle = seg.fullStyle;
                } else {
                    segStyle = seg.color == null ? input.getStyle() : input.getStyle().withColor(seg.color);
                }
                result.append(Text.literal(seg.text).setStyle(segStyle));
            }
        }

        for (Text sibling : input.getSiblings()) {
            result.append(multiple(sibling));
        }
        return result;
    }
    
    public static Text multipleCached(Text input) {
        Text cached = rewriteCache.get(input);
        if (cached != null) return cached;
        Text rewritten = multiple(input);
        rewriteCache.put(input, rewritten);
        return rewritten;
    }

    /**
     * Recolors player names in labels. Handles nested Text structures
     * with full / partial match and flattened formatting code strings.
     */
    public static Text first(Text input) {
        Text cached = labelCache.get(input);
        if (cached != null) return cached;

        var fullString = input.getString();
        if (fullString.isEmpty() || combinedMap.isEmpty()) {
            labelCache.put(input, input);
            return input;
        }
        
        boolean hasPlayerName = false;
        for (String playerName : combinedMap.keySet()) {
            if (fullString.contains(playerName)) {
                hasPlayerName = true;
                break;
            }
        }
        
        if (!hasPlayerName) {
            labelCache.put(input, input);
            return input;
        }

        var content = input.getContent();

        if (content instanceof PlainTextContent plain) {
            String str = plain.string();
            
            if (str.contains("§")) {
                var result = rebuildFlattened(input);
                labelCache.put(input, result);
                return result;
            }
            
            // Exact match (common)
            TextColor color = combinedMap.get(str);
            if (color != null) {
                MutableText colored = Text.literal(str).setStyle(input.getStyle().withColor(color));
                for (Text sibling : input.getSiblings()) {
                    colored.append(sibling);
                }
                labelCache.put(input, colored);
                return colored;
            }
        }
        
        // Nested partial (rare but possible)
        return complex(input, content);
    }

    private static Text complex(Text input, TextContent content) {
        MutableText result;
        if (content instanceof PlainTextContent plain) {
            String str = plain.string();
            TextColor color = combinedMap.get(str);
            if (color != null) {
                result = Text.literal(str).setStyle(input.getStyle().withColor(color));
            } else {
                result = input.copy();
            }
        } else {
            result = input.copy();
        }
        
        result.getSiblings().clear();
        for (Text sibling : input.getSiblings()) {
            result.append(first(sibling));
        }
        labelCache.put(input, result);
        return result;
    }


    // Rebuild flattened text to allow colors
    private static Text rebuildFlattened(Text input) {
        if (!(input.getContent() instanceof PlainTextContent plain)) {
            return input;
        }
        
        var str = plain.string();
        var cleanStr = Enhancer.stripFormattingCodes(str);

        for (Map.Entry<String, TextColor> entry : combinedMap.object2ObjectEntrySet()) {
            var playerName = entry.getKey();
            if (cleanStr.contains(playerName)) {

                var parsedText = Enhancer.parseFormattedTextSimple(str);
                return applyColorToMatch(parsedText, playerName, entry.getValue());
            }
        }
        
        return input;
    }
    
    /**
     * Recursively apply player color to Text nodes that contain the player name.
     */
    private static Text applyColorToMatch(Text text, String playerName, TextColor playerColor) {
        if (text.getContent() instanceof PlainTextContent plain) {
            var content = plain.string();

            if (content.contains(playerName)) {
                var result = Text.literal(content).setStyle(text.getStyle().withColor(playerColor));

                for (Text sibling : text.getSiblings()) {
                    result.append(applyColorToMatch(sibling, playerName, playerColor));
                }
                return result;
            }
        }
        
        var result = text.copy();
        result.getSiblings().clear();
        for (Text sibling : text.getSiblings()) {
            result.append(applyColorToMatch(sibling, playerName, playerColor));
        }
        return result;
    }
    
    public static Text recolorSidebarText(Text input) {
        if (!shouldColor() || !ZoneUtils.isDungeonInstance()) return input;
        Text cached = sidebarCache.get(input);
        if (cached != null) return cached;

        Text result;
        if (input.getContent() instanceof PlainTextContent plain) {
            String str = plain.string();
            TextColor color = null;

            for (Map.Entry<String, TextColor> e : combinedMap.object2ObjectEntrySet()) {
                String key = e.getKey();
                if (str.contains(key)) {
                    color = e.getValue();
                    break;
                }
            }

            if (color != null) {
                result = Text.literal(str).setStyle(input.getStyle().withColor(color));
            } else {
                result = input;
            }

        } else {
            result = input;
        }

        if (!input.getSiblings().isEmpty()) {
            MutableText mutableResult = Text.literal("").setStyle(input.getStyle());
            for (Text sibling : input.getSiblings()) {
                mutableResult.append(recolorSidebarText(sibling));
            }
            result = mutableResult;
        }

        sidebarCache.put(input, result);
        return result;
    }    

    public static Text tooltip(Text input) {
        if (input.getContent() instanceof PlainTextContent plain) {
            var str = plain.string();
            var result = Text.empty().setStyle(input.getStyle());
            List<Segment> segments = createSegments(str);
            
            
            for (Segment seg : segments) {
                Style segStyle;

                if (seg.fullStyle != null) {
                    segStyle = seg.fullStyle;
                } else {
                    segStyle = seg.color == null ? input.getStyle() : input.getStyle().withColor(seg.color);
                }

                result.append(Text.literal(seg.text).setStyle(segStyle));
            }
            
            for (Text sibling : input.getSiblings()) {
                result.append(tooltip(sibling));
            }
            return result;

        } else {
            MutableText result = input.copy();
            result.getSiblings().clear();

            for (Text sibling : input.getSiblings()) {
                result.append(tooltip(sibling));
            }
            return result;
        }
    }

    public static Text tooltipCached(Text input) {
        Text cached = tooltipCache.get(input);
        if (cached != null) return cached;
        Text rewritten = tooltip(input);
        tooltipCache.put(input, rewritten);
        return rewritten;
    }    

    public static void loadUserMap(Map<String, Integer> jsonMap) {
        user.clear();
        for (Map.Entry<String, Integer> entry : jsonMap.entrySet()) {
            user.put(entry.getKey(), TextColor.fromRgb(entry.getValue()));
        }
        combine();
    }

    public static void clearUserMap() {
        user.clear();
        combine();
    }

    public static void saveUserEntry(String key, int color) {
        user.put(key, TextColor.fromRgb(color));
        FishyConfig.setFaC(key, color);
        refresh();
    }

    public static void deleteUserEntry(String key) {
        user.remove(key);
        FishyConfig.removeFaC(key);
        refresh();
    }

    public static void loadUserMap() {
        user.clear();
        Map<String, Integer> map = FishyConfig.getFaC();
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            user.put(entry.getKey(), TextColor.fromRgb(entry.getValue()));
        }
        combine();
    }
}
