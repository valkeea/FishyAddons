package me.valkeea.fishyaddons.vconfig.ui.screen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import me.valkeea.fishyaddons.util.text.StringUtils;
import me.valkeea.fishyaddons.vconfig.ui.model.VCEntry;

public final class Navigation {

    /** Filters the given list of VCEntry objects based on the search text. */
    protected static List<VCEntry> filterEntries(List<VCEntry> unfiltered, String searchText) {
        String s = searchText.toLowerCase().trim();
        Map<String, VCEntry> result = new LinkedHashMap<>();

        // First pass: direct matches (name/desc/alias)
        for (VCEntry e : unfiltered) {
            if (e.isHeader()) continue;
            if (matchesSearch(e, s)) {
                result.putIfAbsent(e.category.name(), VCEntry.header(e.category));
                result.put(e.name, e);
            } else if (e.hasSubEntries()) {
                addMatchingSubEntries(result, e, s);
            }
        }

        // Fallback: forgiving search if too few results
        if (lowResults(new ArrayList<>(result.values()))) {
            addFallbackMatches(result, unfiltered, s);
        }

        if (lowResults(new ArrayList<>(result.values()))) {
            return unfiltered;
        } else return new ArrayList<>(result.values());
    }

    private static void addMatchingSubEntries(Map<String, VCEntry> r, VCEntry e, String s) {

        List<VCEntry> matchingChildren = e.getSubEntries().stream()
            .filter(child -> matchesSearch(child, s))
            .toList();

        if (!matchingChildren.isEmpty()) {
            r.putIfAbsent(e.category.name(), VCEntry.header(e.category));
            r.put(e.name, e.withSubEntries(matchingChildren));
        }
    }

    private static void addFallbackMatches(Map<String, VCEntry> r, List<VCEntry> unfiltered, String s) {

        for (VCEntry e : unfiltered) {
            if (e.isHeader()) continue;

            if (StringUtils.closeMatch(s, e.name)) {
                r.putIfAbsent(e.category.name(), VCEntry.header(e.category));
                r.putIfAbsent(e.name, e);

            } else if (e.hasSubEntries()) {
                List<VCEntry> closeMatchChildren = e.getSubEntries().stream()
                    .filter(child -> StringUtils.closeMatch(s, child.name))
                    .toList();
                if (!closeMatchChildren.isEmpty()) {
                    r.putIfAbsent(e.category.name(), VCEntry.header(e.category));
                    r.putIfAbsent(e.name, e.withSubEntries(closeMatchChildren));
                }
            }
        }
    }

    private static boolean matchesSearch(VCEntry e, String s) {
        
        var name = e.name.toLowerCase();
        var desc = e.description != null ? String.join(" ", e.description).toLowerCase() : "";
        if (name.contains(s) || desc.contains(s)) return true;
        if (isAliasMatch(s, name)) return true;

        for (String word : name.split("\\s+")) {
            if (word.contains(s)) return true;
        }

        for (String word : desc.split("\\s+")) {
            if (word.contains(s)) return true;
        }

        return false;
    }

    private static boolean lowResults(List<VCEntry> filtered) {
        return filtered.isEmpty() || (filtered.size() == 1 && filtered.get(0).isHeader());
    }

    private static boolean isAliasMatch(String s, String text) {
        for (var aliasSet : SYNONYMS) {
            if (aliasSet.contains(s)) {
                for (var alias : aliasSet) {
                    if (text.toLowerCase().contains(alias)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static final List<Set<String>> SYNONYMS = List.of(
        Set.of("hyperion", "implosion", "wither impact", "explosion"),
        Set.of("mob", "entity", "entities")
    );

    /** Finds VCEntry objects by their navigation key. */
    protected static List<VCEntry> findByNavigationKey(String navKey, List<VCEntry> allEntries) {

        String lowerNavKey = navKey.toLowerCase();
        List<VCEntry> matches = new ArrayList<>();
        
        for (VCEntry e : allEntries) {
            if (e.name != null && e.name.toLowerCase().contains(lowerNavKey)) {
                matches.add(e);
            }
            if (e.hasSubEntries()) {
                for (VCEntry sub : e.getSubEntries()) {
                    if (sub.name != null && sub.name.toLowerCase().contains(lowerNavKey)) {
                        matches.add(sub);
                    }
                }
            }
        }
        return matches;
    }

    private Navigation() {}    
}
