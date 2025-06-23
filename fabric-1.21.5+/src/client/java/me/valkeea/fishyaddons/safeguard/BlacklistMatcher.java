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
        if (guiTitle != null) {
            for (BlacklistManager.GuiBlacklistEntry entry : getMergedBlacklist()) {
                if (!entry.enabled) continue;

                if (entry.checkTitle) {
                    for (String identifier : entry.identifiers) {
                        String cleanIdentifier = stripColor(identifier).toLowerCase().trim();
                        if (guiTitle.equalsIgnoreCase(cleanIdentifier) || guiTitle.toLowerCase().contains(cleanIdentifier)) {
                            return true;
                        }
                    }
                }
            }
        }

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
        } catch (Throwable t) {
            return false;
        }

        for (GuiBlacklistEntry entry : getMergedBlacklist()) {
            if (!entry.enabled) continue;

            for (String identifier : entry.identifiers) {
                String cleanIdentifier = stripColor(identifier).toLowerCase().trim();

                if (entry.checkTitle && screenClassName != null && screenClassName.toLowerCase().contains(cleanIdentifier)) {
                    return true;
                }
                if (stack.getCustomName() != null) {
                    String cleanName = stripColor(stack.getCustomName().getString()).toLowerCase().trim();
                    if (cleanName.contains(cleanIdentifier)) {
                        return true;
                    }
                }
                for (Text line : tooltip) {
                    String cleanLine = stripColor(line.getString()).toLowerCase().trim();
                    if (cleanLine.contains(cleanIdentifier)) {
                        return true;
                    }
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