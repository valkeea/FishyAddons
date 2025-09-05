package me.valkeea.fishyaddons.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.valkeea.fishyaddons.util.ZoneUtils;
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

    static {
        fetchGlobal();
    }

    private static void fetchGlobal() {
        try {
            String url = "https://gist.githubusercontent.com/valkeea/de655343d713bf3378555fe6775f8e3b/raw/facolors.json";
            java.net.URLConnection conn = java.net.URI.create(url).toURL().openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestProperty("User-Agent", "FishyAddons");

            try (java.io.InputStream in = conn.getInputStream();
                 java.io.InputStreamReader reader = new java.io.InputStreamReader(in)) {
                com.google.gson.Gson gson = new com.google.gson.Gson();
                java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<java.util.Map<String, String>>(){}.getType();
                java.util.Map<String, String> map = gson.fromJson(reader, type);

                if (map == null || map.isEmpty() || map.size() > 100) {
                    throw new IllegalStateException("Invalid gist data");
                }

                global.clear();
                for (var entry : map.entrySet()) {
                    String hex = entry.getValue();
                    int color = Integer.parseInt(hex, 16);
                    global.put(entry.getKey(), TextColor.fromRgb(color));
                }
                refresh();
            }
        } catch (Exception e) {
            e.printStackTrace();
            useFallbackColors();
        }
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

    public record Segment(String text, @Nullable TextColor color) {}

    private static List<Segment> splitWithMatches(String input) {
        if (input.isEmpty()) return List.of();
        
        List<Segment> cached = segmentCache.get(input);
        if (cached != null) return cached;

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
        segmentCache.put(input, segments);
        return segments;
    }

    public static <T> Optional<T> applyRecolorAll(
            String input,
            Style baseStyle,
            StringVisitable.StyledVisitor<T> visitor
    ) {
        for (Segment seg : splitWithMatches(input)) {
            Style style = seg.color == null ? baseStyle : baseStyle.withColor(seg.color);
            visitor.accept(style, seg.text);
        }
        return Optional.empty();
    }

    private static Text multiple(Text input) {
        TextContent content = input.getContent();
        MutableText result = Text.empty().setStyle(input.getStyle());

        if (content instanceof PlainTextContent plain) {
            for (Segment seg : splitWithMatches(plain.string())) {
                Style segStyle = seg.color == null ? input.getStyle() : input.getStyle().withColor(seg.color);
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
     * Recolors only the first literal node that exactly matches a configured player name.
     */
    public static Text first(Text input) {
        Text cached = labelCache.get(input);
        if (cached != null) return cached;

        if (input.getContent() instanceof PlainTextContent plain) {
            String str = plain.string();
            TextColor color = null;
            for (Map.Entry<String, TextColor> e : combinedMap.object2ObjectEntrySet()) {
                if (str.equals(e.getKey())) {
                    color = e.getValue();
                    break;
                }
            }
            if (color != null) {
                MutableText colored = Text.literal(str).setStyle(input.getStyle().withColor(color));
                for (Text sibling : input.getSiblings()) {
                    colored.append(sibling);
                }
                return colored;
            }
        }
        
        MutableText result = input.copy();
        result.getSiblings().clear();
        for (Text sibling : input.getSiblings()) {
            result.append(first(sibling));
        }
        labelCache.put(input, result);
        return result;
    }
    
    /**
    * Redirecting to multipleCached() at decorateName works in a void, this is for compatibility with other mods
    */
    public static Text recolorSidebarText(Text input) {
        if (!shouldColor() || !ZoneUtils.isInDungeon()) return input;
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
            String str = plain.string();
            TextColor color = null;
            for (Map.Entry<String, TextColor> e : combinedMap.object2ObjectEntrySet()) {
                if (str.equals(e.getKey())) {
                    color = e.getValue();
                    break;
                }
            }
            Style newStyle = color == null ? input.getStyle() : input.getStyle().withColor(color);
            MutableText result = Text.literal(str).setStyle(newStyle);
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
        me.valkeea.fishyaddons.config.FishyConfig.setFaC(key, color);
        refresh();
    }

    public static void deleteUserEntry(String key) {
        user.remove(key);
        me.valkeea.fishyaddons.config.FishyConfig.removeFaC(key);
        refresh();
    }

    public static void loadUserMap() {
        user.clear();
        Map<String, Integer> map = me.valkeea.fishyaddons.config.FishyConfig.getFaC();
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            user.put(entry.getKey(), TextColor.fromRgb(entry.getValue()));
        }
        combine();
    }
}
