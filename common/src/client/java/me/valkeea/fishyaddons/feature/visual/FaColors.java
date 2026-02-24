package me.valkeea.fishyaddons.feature.visual;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.valkeea.fishyaddons.api.skyblock.GameMode;
import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.event.impl.FaEvents;
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
        if (FishyConfig.getState(Key.GLOBAL_FA_COLORS, false)) {

            FaEvents.ENVIRONMENT_CHANGE.register(event -> {
                if (event.gameModeChanged()) {
                    combine(event.isSkyblock() || !FishyConfig.getState(Key.SB_ONLY_FAC, false));
                }
            });

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
            var connection = java.net.URI.create(url).toURL().openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.setRequestProperty("User-Agent", "FishyAddons");

            try (var in = connection.getInputStream();
                 var reader = new java.io.InputStreamReader(in)) {
                var gson = new com.google.gson.Gson();
                var type = new com.google.gson.reflect.TypeToken<Map<String, String>>(){}.getType();
                Map<String, String> map = gson.fromJson(reader, type);

                if (map == null || map.isEmpty() || map.size() > 100) {
                    throw new IllegalStateException("Invalid gist data");
                }

                global.clear();

                map.entrySet().stream()
                    .forEach(entry -> global.put(entry.getKey(), 
                        TextColor.fromRgb(Integer.parseInt(entry.getValue(), 16))));
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
        useGlobal = FishyConfig.getState(Key.GLOBAL_FA_COLORS, false);
        useCustom = FishyConfig.getState(Key.CUSTOM_FA_COLORS, false);
        loadUserMap();
        combine(GameMode.skyblock() || !FishyConfig.getState(Key.SB_ONLY_FAC, false));
        clearCache();
    }

    private static void combine(boolean shouldRecolor) {
        clearCache();
        combinedMap.clear();

        if (shouldRecolor) {

            if (useGlobal) combinedMap.putAll(global);
            if (useCustom) combinedMap.putAll(user);
        }
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
        global.put("valkeea", TextColor.fromRgb(0xFFFFB8E4));
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

            List<Segment> nameSegments = splitForMatches(text);

            nameSegments.stream()
                .map(seg -> seg.color != null 
                    ? new Segment(seg.text, seg.color, style.withColor(seg.color))
                    : new Segment(seg.text, style.getColor(), style))
                .forEach(segments::add);

            return Optional.empty();

        }, Style.EMPTY);
        
        return segments;
    }

    private record MatchResult(String key, TextColor color, int startIndex, int endIndex) {
        boolean isValid() { return startIndex >= 0; }
        int length() { return key.length(); }
    }

    /**
     * Split input string into segments based on matched player names.
     */
    private static List<Segment> splitForMatches(String input) {
        List<Segment> segments = new ArrayList<>();
        int pos = 0;

        while (pos < input.length()) {
            final int currentPos = pos;
            
            // Find the earliest and longest matching player name in the remaining input
            Optional<MatchResult> bestMatch = combinedMap.object2ObjectEntrySet().stream()
                .map(entry -> {
                    String key = entry.getKey();
                    int idx = findNextMatch(input, key, currentPos);
                    return new MatchResult(key, entry.getValue(), idx, idx + key.length());
                })
                .filter(MatchResult::isValid)
                .min((m1, m2) -> {
                    // Prioritize matches that start earlier; if same start, prefer longer matches
                    int startComparison = Integer.compare(m1.startIndex, m2.startIndex);
                    return startComparison != 0 ? startComparison : Integer.compare(m2.length(), m1.length());
                });

            if (bestMatch.isEmpty()) {
                segments.add(new Segment(input.substring(pos), null));
                break;
            }

            var match = bestMatch.get();
            
            // Add unmatched text before the match (if any)
            if (match.startIndex > pos) {
                segments.add(new Segment(input.substring(pos, match.startIndex), null));
            }

            // Add the matched player name with its color
            segments.add(new Segment(match.key, match.color));
            pos = match.endIndex;
        }
        return segments;
    }

    /**
     * Find the next occurrence of a player name at a word boundary.
     * Returns -1 if not found or if the match isn't at a word boundary.
     */
    private static int findNextMatch(String input, String playerName, int fromIndex) {
        int start = input.indexOf(playerName, fromIndex);
        
        while (start >= 0) {
            boolean isStartBoundary = (start == 0) || !isWordChar(input.charAt(start - 1))
                || isPartOfFormattingCode(input, start - 1);
            boolean isEndBoundary = (start + playerName.length() == input.length()) 
                || !isWordChar(input.charAt(start + playerName.length()));
            
            if (isStartBoundary && isEndBoundary) {
                return start;
            }
            
            start = input.indexOf(playerName, start + 1);
        }
        
        return -1;
    }

    private static boolean isPartOfFormattingCode(String input, int pos) {
        return pos > 0 && input.charAt(pos - 1) == '§';
    }

    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c);
    }

    /**
     * Redirected visit that applies recoloring to all segments.
     */
    public static <T> Optional<T> applyRecolorAll(
            String input,
            Style baseStyle,
            StringVisitable.StyledVisitor<T> visitor
    ) {
        for (var seg : createSegments(input)) {
            if (seg.fullStyle != null) {
                visitor.accept(seg.fullStyle, seg.text);
            } else {
                visitor.accept(seg.color == null ? baseStyle : baseStyle.withColor(seg.color), seg.text);
            }
        }
        return Optional.empty();
    }

    private static Text multiple(Text input) {
        var content = input.getContent();
        boolean needsRecolor = false;

        if (content instanceof PlainTextContent plain) {
            List<Segment> segments = createSegments(plain.string());
            needsRecolor = segments.stream().anyMatch(seg -> seg.color != null || seg.fullStyle != null);
        }

        List<Text> processedSiblings = null;
        boolean siblingsToRecolor = false;
        if (!input.getSiblings().isEmpty()) {
            processedSiblings = new ArrayList<>(input.getSiblings().size());
            for (var sibling : input.getSiblings()) {
                Text processed = multiple(sibling);
                processedSiblings.add(processed);
                if (processed != sibling) {
                    siblingsToRecolor = true;
                }
            }
        }

        if (!needsRecolor && !siblingsToRecolor) return input;

        var result = Text.empty().setStyle(input.getStyle());

        if (content instanceof PlainTextContent plain) {
            for (var seg : createSegments(plain.string())) {
                
                Style segStyle;
                if (seg.fullStyle != null) {
                    segStyle = seg.fullStyle;
                } else {
                    segStyle = seg.color == null ? input.getStyle() : input.getStyle().withColor(seg.color);
                }

                result.append(Text.literal(seg.text).setStyle(segStyle));
            }
        }

        if (processedSiblings != null) {
            processedSiblings.forEach(result::append);
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
        
        boolean hasPlayerName = combinedMap.keySet().stream()
            .anyMatch(fullString::contains);
        
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
        boolean needsRecolor = false;
        
        if (content instanceof PlainTextContent plain) {
            String str = plain.string();
            needsRecolor = combinedMap.containsKey(str);
        }

        List<Text> processedSiblings = new ArrayList<>(input.getSiblings().size());
        boolean siblingsToRecolor = false;
        for (var sibling : input.getSiblings()) {
            Text processed = first(sibling);
            processedSiblings.add(processed);
            if (processed != sibling) {
                siblingsToRecolor = true;
            }
        }

        if (!needsRecolor && !siblingsToRecolor) {
            labelCache.put(input, input);
            return input;
        }

        MutableText result;
        if (content instanceof PlainTextContent plain) {
            String str = plain.string();
            var color = combinedMap.get(str);

            if (color != null) {
                result = Text.literal(str).setStyle(input.getStyle().withColor(color));
            } else result = input.copy();

        } else {
            result = input.copy();
        }
        
        result.getSiblings().clear();
        processedSiblings.forEach(result::append);

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

        for (var sibling : text.getSiblings()) {
            result.append(applyColorToMatch(sibling, playerName, playerColor));
        }

        return result;
    }
    
    public static Text recolorSidebarText(Text input) {
        if (!shouldColor() || !ZoneUtils.isDungeonInstance()) return input;

        Text cached = sidebarCache.get(input);
        if (cached != null) return cached;

        boolean needsRecolor = false;
        TextColor foundColor = null;

        if (input.getContent() instanceof PlainTextContent plain) {
            String str = plain.string();
            foundColor = combinedMap.object2ObjectEntrySet().stream()
                .filter(e -> str.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
            needsRecolor = foundColor != null;
        }

        List<Text> processedSiblings = null;
        boolean siblingsToRecolor = false;
        if (!input.getSiblings().isEmpty()) {
            processedSiblings = new ArrayList<>(input.getSiblings().size());
            for (var sibling : input.getSiblings()) {
                Text processed = recolorSidebarText(sibling);
                processedSiblings.add(processed);
                if (processed != sibling) {
                    siblingsToRecolor = true;
                }
            }
        }

        if (!needsRecolor && !siblingsToRecolor) {
            sidebarCache.put(input, input);
            return input;
        }

        Text result;
        if (needsRecolor) {
            result = Text.literal(((PlainTextContent) input.getContent()).string())
                .setStyle(input.getStyle().withColor(foundColor));
        } else {
            result = input.copy();
        }

        if (processedSiblings != null) {
            var mutableResult = result instanceof MutableText mutableText ? mutableText : Text.empty().setStyle(result.getStyle());
            if (!(result instanceof MutableText)) {
                mutableResult.append(result);
            }

            processedSiblings.forEach(mutableResult::append);
            result = mutableResult;
        }

        sidebarCache.put(input, result);
        return result;
    }    

    public static Text tooltip(Text input) {
        if (input.getContent() instanceof PlainTextContent plain) {
            var str = plain.string();
            List<Segment> segments = createSegments(str);
            
            boolean needsRecolor = segments.stream().anyMatch(seg -> seg.color != null || seg.fullStyle != null);
            if (!needsRecolor && input.getSiblings().isEmpty()) return input;
            
            var result = Text.empty().setStyle(input.getStyle());
            
            for (var seg : segments) {
                Style segStyle;

                if (seg.fullStyle != null) {
                    segStyle = seg.fullStyle;
                } else {
                    segStyle = seg.color == null ? input.getStyle() : input.getStyle().withColor(seg.color);
                }

                result.append(Text.literal(seg.text).setStyle(segStyle));
            }
            
            input.getSiblings().stream()
                .map(FaColors::tooltip)
                .forEach(result::append);

            return result;

        } else {

            var result = input.copy();
            result.getSiblings().clear();

            input.getSiblings().stream()
                .map(FaColors::tooltip)
                .forEach(result::append);
                
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
        FishyConfig.getFaC().entrySet().stream()
            .forEach(entry -> user.put(entry.getKey(), TextColor.fromRgb(entry.getValue())));
    }
}
