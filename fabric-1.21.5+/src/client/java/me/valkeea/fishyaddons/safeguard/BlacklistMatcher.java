package me.valkeea.fishyaddons.safeguard;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.screen.slot.Slot;

import java.util.List;

import me.valkeea.fishyaddons.safeguard.BlacklistManager.GuiBlacklistEntry;

public class BlacklistMatcher {
    private BlacklistMatcher() {}

    public static boolean isBlacklistedGUI(HandledScreen<?> gui, String screenClassName) {
        String guiTitle = getGuiTitle(gui);
        return isBlacklistedByTitle(guiTitle) || isBlacklistedBySlots(gui, screenClassName);
    }

    private static boolean isBlacklistedByTitle(String guiTitle) {
        if (guiTitle == null) return false;
        for (BlacklistManager.GuiBlacklistEntry entry : getMergedBlacklist()) {
            if (!entry.isEnabled()) continue;
            if (entry.checkTitle && containsIdentifier(guiTitle, entry.identifiers)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsIdentifier(String guiTitle, List<String> identifiers) {
        String lowerGuiTitle = guiTitle.toLowerCase();
        for (String identifier : identifiers) {
            String stripped = stripColor(identifier);
            if (stripped == null) continue;
            String cleanIdentifier = stripped.toLowerCase().trim();
            if (guiTitle.equalsIgnoreCase(cleanIdentifier) || lowerGuiTitle.contains(cleanIdentifier)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlacklistedBySlots(HandledScreen<?> gui, String screenClassName) {
        for (Slot slot : gui.getScreenHandler().slots) {
            ItemStack stack = slot.getStack();
            if (stack == null || stack.isEmpty()) continue;
            if (isBlacklistedItem(stack, screenClassName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBlacklistedItem(ItemStack stack, String screenClassName) {
        if (stack == null || stack.isEmpty()) return false;

        MinecraftClient client = MinecraftClient.getInstance();
        List<Text> tooltip;

        try {
            tooltip = stack.getTooltip(TooltipContext.DEFAULT, client.player, TooltipType.BASIC);
        } catch (Exception e) {
            return false;
        }

        for (GuiBlacklistEntry entry : getMergedBlacklist()) {
            if (!entry.isEnabled()) continue;
            if (matchesAnyIdentifier(entry, stack, tooltip, screenClassName)) {
                return true;
            }
        }

        return false;
    }

    private static boolean matchesAnyIdentifier(GuiBlacklistEntry entry, ItemStack stack, List<Text> tooltip, String screenClassName) {
        for (String identifier : entry.identifiers) {
            String cleanIdentifier = getCleanIdentifier(identifier);
            if (cleanIdentifier == null) continue;

            if (entry.checkTitle && matchesScreenClassName(screenClassName, cleanIdentifier)) {
                return true;
            }
            if (matchesCustomName(stack, cleanIdentifier)) {
                return true;
            }
            if (matchesTooltip(tooltip, cleanIdentifier)) {
                return true;
            }
        }
        return false;
    }

    private static String getCleanIdentifier(String identifier) {
        String stripped = stripColor(identifier);
        return stripped == null ? null : stripped.toLowerCase().trim();
    }

    private static boolean matchesScreenClassName(String screenClassName, String cleanIdentifier) {
        return screenClassName != null && screenClassName.toLowerCase().contains(cleanIdentifier);
    }

    private static boolean matchesCustomName(ItemStack stack, String cleanIdentifier) {
        if (stack.getCustomName() != null) {
            String strippedName = stripColor(stack.getCustomName().getString());
            if (strippedName != null) {
                String cleanName = strippedName.toLowerCase().trim();
                return cleanName.contains(cleanIdentifier);
            }
        }
        return false;
    }

    private static boolean matchesTooltip(List<Text> tooltip, String cleanIdentifier) {
        for (Text line : tooltip) {
            String strippedLine = stripColor(line.getString());
            if (strippedLine != null) {
                String cleanLine = strippedLine.toLowerCase().trim();
                if (cleanLine.contains(cleanIdentifier)) {
                    return true;
                }
            }
        }
        return false;
    }


    public static String getGuiTitle(HandledScreen<?> gui) {
        try {
            return gui.getTitle().getString();
        } catch (Exception e) {
            return null;
        }
    }

    public static String stripColor(String input) {
        return input == null ? null : input.replaceAll("(?i)§[0-9a-fklmnor]", "");
    }

    public static List<BlacklistManager.GuiBlacklistEntry> getMergedBlacklist() {
        return BlacklistManager.getMergedBlacklist();
    }
}