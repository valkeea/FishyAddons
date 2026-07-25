package me.valkeea.fishyaddons.feature.item.safeguard;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class BlacklistMatcher {
    private BlacklistMatcher() {}

    public static boolean isBlacklistedGUI(ContainerScreen gui) {
        var guiTitle = getGuiTitle(gui);
        if (isBlacklistedByTitle(guiTitle)) return true;
        return isBlacklistedByItems(gui);
    }

    private static boolean isBlacklistedByTitle(String guiTitle) {
        if (guiTitle == null) return false;       
        for (var entry : BlacklistManager.getEnabledEntries()) {
            if (entry.checkTitle && containsAnyPattern(guiTitle, entry.matchPatterns)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAnyPattern(String guiTitle, List<String> patterns) {
        var lowerGuiTitle = guiTitle.toLowerCase();
        for (var pattern : patterns) {
            var stripped = stripColor(pattern);
            if (stripped.isEmpty()) continue;
            var cleanPattern = stripped.toLowerCase().trim();
            if (guiTitle.equalsIgnoreCase(cleanPattern) || lowerGuiTitle.contains(cleanPattern)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlacklistedByItems(ContainerScreen gui) {
        List<BlacklistManager.BlacklistEntry> itemCheckEntries = new ArrayList<>();
        for (var entry : BlacklistManager.getEnabledEntries()) {
            if (!entry.checkTitle) {
                itemCheckEntries.add(entry);
            }
        }
        
        if (itemCheckEntries.isEmpty()) {
            return false;
        }
        
        for (int i = 0; i < Math.min(gui.getMenu().slots.size(), 54); i++) {
            var slot = gui.getMenu().slots.get(i);
            var stack = slot.getItem();
            if (stack.isEmpty()) continue;
            
            if (isBlacklistedItem(stack, itemCheckEntries)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlacklistedItem(ItemStack stack, List<BlacklistManager.BlacklistEntry> entries) {
        var client = Minecraft.getInstance();
        List<Component> tooltip;

        try {
            tooltip = stack.getTooltipLines(TooltipContext.EMPTY, client.player, TooltipFlag.NORMAL);
        } catch (Exception _) {
            return false;
        }

        int end = tooltip.size();
        int start = Math.max(0, end - 5);
        List<Component> limitedTooltip = tooltip.subList(start, end);

        for (var entry : entries) {
            if (matchesItemAgainstPatterns(stack, limitedTooltip, entry.matchPatterns)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesItemAgainstPatterns(ItemStack stack, List<Component> tooltip, List<String> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }

        for (var pattern : patterns) {
            var cleanPattern = getCleanIdentifier(pattern);
            if (cleanPattern == null) continue;
            if (matchesCustomName(stack, cleanPattern)) return true;
            if (matchesTooltip(tooltip, cleanPattern)) return true;
        }

        return false;
    }

    private static String getCleanIdentifier(String identifier) {
        var stripped = stripColor(identifier);
        return stripped.isEmpty() ? null : stripped.toLowerCase().trim();
    }

    private static boolean matchesCustomName(ItemStack stack, String cleanIdentifier) {
        var name = stack.getCustomName();
        if (name != null) {
            var strippedName = stripColor(name.getString());
            if (!strippedName.isEmpty()) {
                var cleanName = strippedName.toLowerCase().trim();
                return cleanName.contains(cleanIdentifier);
            }
        }
        return false;
    }

    private static boolean matchesTooltip(List<Component> tooltip, String cleanIdentifier) {
        for (Component line : tooltip) {
            var strippedLine = stripColor(line.getString());
            if (!strippedLine.isEmpty()) {
                var cleanLine = strippedLine.toLowerCase().trim();
                if (cleanLine.contains(cleanIdentifier)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String getGuiTitle(ContainerScreen gui) {
        try {
            return gui.getTitle().getString();
        } catch (Exception _) {
            return null;
        }
    }

    public static String stripColor(String input) {
        return input == null ? "" : input.replaceAll("(?i)§[0-9a-fklmnor]", "");
    }
}
