package me.wait.fishyaddons.fishyprotection;

import me.wait.fishyaddons.util.GuiBlacklistEntry;
import me.wait.fishyaddons.fishyprotection.BlacklistStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import java.util.*;

public class BlacklistMatcher {

    public static boolean isBlacklistedGUI(GuiContainer guiContainer, String screenClassName) {
        String guiTitle = getGuiTitle(guiContainer);
        if (guiTitle != null) {
            for (GuiBlacklistEntry entry : getMergedBlacklist()) {
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

        for (Slot slot : guiContainer.inventorySlots.inventorySlots) {
            ItemStack stack = slot.getStack();
            if (stack == null) continue;

            if (isBlacklistedItem(stack, screenClassName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBlacklistedItem(ItemStack stack, String screenClassName) {
        if (stack == null) return false;

        List<String> lore;
        try {
            lore = stack.getTooltip(Minecraft.getMinecraft().thePlayer, false);
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

                if (stack.hasDisplayName()) {
                    String cleanDisplayName = stripColor(stack.getDisplayName()).toLowerCase().trim();
                    if (cleanDisplayName.contains(cleanIdentifier)) {
                        return true;
                    }
                }

                for (String line : lore) {
                    String cleanLine = stripColor(line).toLowerCase().trim();
                    if (cleanLine.contains(cleanIdentifier)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    // Combine default and user-defined entries
    private static List<GuiBlacklistEntry> getMergedBlacklist() {
        return BlacklistStore.getMergedBlacklist();
    }
    

    private static GuiBlacklistEntry findUserOverride(GuiBlacklistEntry def, List<GuiBlacklistEntry> userList) {
        for (String id : def.identifiers) {
            for (GuiBlacklistEntry userEntry : userList) {
                if (matchesIdentifier(userEntry, id)) {
                    return userEntry;
                }
            }
        }
        return null;
    }

    private static boolean matchesIdentifier(GuiBlacklistEntry entry, String identifier) {
        for (String id : entry.identifiers) {
            if (id.equalsIgnoreCase(identifier)) {
                return true;
            }
        }
        return false;
    }

    private static String getGuiTitle(GuiContainer guiContainer) {
        if (guiContainer instanceof GuiChest) {
            ContainerChest containerChest = (ContainerChest) guiContainer.inventorySlots;
            IInventory lower = containerChest.getLowerChestInventory();
            if (lower != null && lower.getDisplayName() != null) {
                String title = lower.getDisplayName().getUnformattedText();
                return title;
            }
        }
        return null;
    }

    // Strip color codes from a string
    public static String stripColor(String text) {
        return text == null ? "" : text.replaceAll("(?i)§[0-9a-fk-or]", "");
    }
}
