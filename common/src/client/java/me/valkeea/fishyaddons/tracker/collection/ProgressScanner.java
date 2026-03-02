package me.valkeea.fishyaddons.tracker.collection;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.valkeea.fishyaddons.tracker.collection.RecipeScanner.SlotStackProvider;
import me.valkeea.fishyaddons.util.text.TextUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/**
 * Scans collection GUIs for progress information to validate tracking.
 */
public class ProgressScanner {
    private static final int COLL_INDEX = 4;
    private static final int SKILL_INDEX = 50;
    private static final long STALENESS_THRESHOLD_MS = 3600000L;
    
    /**
     * Result of scanning an itemstack for collection info
     */
    private static class ScanResult {
        final Long value;
        final boolean isExact;
        
        ScanResult(Long value, boolean isExact) {
            this.value = value;
            this.isExact = isExact;
        }
    }
    
    /**
     * Extracted from a collection tooltip
     */
    private static class CollectionTooltipData {
        Long totalValue;
        Long userContribution;
        boolean userContributionRounded;
        long coopContributionsTotal;
        boolean anyCoopContributionRounded;
        boolean hasCoopSection;
        
        CollectionTooltipData() {
            this.totalValue = null;
            this.userContribution = null;
            this.userContributionRounded = false;
            this.coopContributionsTotal = 0L;
            this.anyCoopContributionRounded = false;
            this.hasCoopSection = false;
        }
    }

    private static final Pattern TOTAL_PATTERN = Pattern.compile(
        "Total Collected:\\s*([\\d,]+)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern MENU_ITEM_PATTERN = Pattern.compile(
        "^\\s*([^:]+):\\s*([\\d,]+|N/A)\\b",
        Pattern.CASE_INSENSITIVE
    );
    
    private ProgressScanner() {}

    /**
     * Scan a collection GUI for collection levels.
     * 
     * @param title The GUI title
     * @param getSlotStack Function to get ItemStack at slot index
     * @return true if collection data was found and updated
     */
    public static boolean scanCollectionGui(String title, SlotStackProvider getSlotStack) {
        if (isIndividualCollection(title)) {
            return scanIndividualCollection(title, getSlotStack);
        } else if (isCollectionsMenu(title)) {
            return scanCollectionsMenu(getSlotStack);
        } else if (isSkillCollectionsMenu(title)) {
            return scanSkillMenu(getSlotStack);
        }
        
        return false;
    }

    // Specific collections

    /**
     * Scan an individual collection screen
     */
    private static boolean scanIndividualCollection(String title, SlotStackProvider getSlotStack) {
        String itemName = extractItemNameFromTitle(title);
        if (itemName == null || itemName.isEmpty()) return false;

        var stack = getSlotStack.get(COLL_INDEX);

        if (stack != null && !stack.isEmpty()) {

            ScanResult r = scanStack(stack);
            if (r != null && r.value != null && (r.isExact || !CollectionData.hasBaseline(itemName))) {
                updateSingular(itemName, r.value);
                return true;
            }
        }
        
        return false;
    } 

    private static ScanResult scanStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        
        var lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) return null;

        List<Text> tooltip = lore.lines();
        if (tooltip == null || tooltip.isEmpty()) return null;
        
        var data = parseTooltip(tooltip);
        return determineExactCollection(data);
    }
    
    /**
     * Parse all collection-related information from tooltip lines
     */
    private static CollectionTooltipData parseTooltip(List<Text> tooltip) {
        var data = new CollectionTooltipData();
        String username = "";
        
        for (Text line : tooltip) {
            String cleanLine = TextUtils.stripColor(line.getString());
            if (cleanLine.isEmpty()) continue;
            
            parseTotalCollected(cleanLine, data);
            
            if (cleanLine.contains("Co-op Contributions") && username.isEmpty()) {
                username = me.valkeea.fishyaddons.api.skyblock.Profile.getUserName();
                data.hasCoopSection = true;
            }
            
            if (!username.isEmpty()) {
                parseCoopContribution(cleanLine, username, data);
            }
        }
        
        return data;
    }

    private static void parseTotalCollected(String cleanLine, CollectionTooltipData data) {
        Matcher m = TOTAL_PATTERN.matcher(cleanLine);
        if (m.find()) {
            try {
                String totalStr = m.group(1).replace(",", "");
                long total = Long.parseLong(totalStr);
                
                if (data.totalValue == null || total > data.totalValue) {
                    data.totalValue = total;
                }
            } catch (NumberFormatException e) {
                // Ignore invalid numbers
            }
        }
    }
    
    private static void parseCoopContribution(String cleanLine, String username, CollectionTooltipData data) {
        if (!cleanLine.contains(":") || !cleanLine.matches(".*\\d.*")) {
            return;
        }
        
        String[] parts = cleanLine.split(":");
        if (parts.length < 2) {
            return;
        }
        
        String contributionStr = parts[1].trim();
        boolean isRounded = contributionStr.matches(".*[kMB]\\s*$");
        
        try {
            long contribution = convertMagnitude(contributionStr);
            
            if (cleanLine.contains(username.toLowerCase())) {
                data.userContribution = contribution;
                data.userContributionRounded = isRounded;
            } else {
                data.coopContributionsTotal += contribution;
                if (isRounded) {
                    data.anyCoopContributionRounded = true;
                }
            }
        } catch (NumberFormatException e) {
            // Ignore invalid numbers (non-collection co-op contribution lines)
        }
    }
    
    /**
     * Determine the exact collection value based on parsed data.
     * Valid if:
     * 1. Solo player: total is user's exact collection
     * 2. User's contribution not rounded: can be used directly
     * 3. User's contribution rounded BUT all co-op values exact: calculate
     * 
     * @param data Parsed tooltip data
     * @return ScanResult with exact flag, or null if no valid data
     */
    private static ScanResult determineExactCollection(CollectionTooltipData data) {

        if (!data.hasCoopSection && data.totalValue != null) { // Case 1
            return new ScanResult(data.totalValue, true);
        }
        
        if (data.userContribution != null && !data.userContributionRounded) { // Case 2
            return new ScanResult(data.userContribution, true);
        }
        
        if (data.hasCoopSection && data.totalValue != null &&
            data.userContribution != null && data.userContributionRounded && 
            !data.anyCoopContributionRounded) { // Case 3
            long exactValue = data.totalValue - data.coopContributionsTotal;
            return new ScanResult(exactValue, true);
        }
        
        // User's contribution exists but is rounded and cant be calculated exactly
        if (data.userContribution != null) {
            return new ScanResult(data.userContribution, false);
        }
        
        // Fallback: return total if available, marked as inexact (used if no baseline exists)
        if (data.totalValue != null) {
            return new ScanResult(data.totalValue, false);
        }
        
        return null;
    }

    // Main collections menu

    /**
     * Scan the main Collections menu
     */
    private static boolean scanCollectionsMenu(SlotStackProvider getSlotStack) {
        
        boolean foundAny = false;

        var testStack = getSlotStack.get(COLL_INDEX);
        if (!infoToggled(testStack)) return false;

        for (int slot : new int[]{20, 21, 22, 23, 24, 32}) {
            if (scanRankings(getSlotStack, slot)) {
                foundAny = true;
            }
        }
        
        if (foundAny) CollectionData.save();
        
        return foundAny;
    }

    private static boolean infoToggled(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        
        var lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) return false;

        List<Text> tooltip = lore.lines();
        if (tooltip == null || tooltip.isEmpty()) return false;

        for (Text line : tooltip) {
            String cleanLine = line.getString().replaceAll("§.", "").trim().toLowerCase();
            if (cleanLine.contains("click to show rankings")) {
                return false;
            }
        }
        
        return true;
    }

    private static boolean scanRankings(SlotStackProvider getSlotStack, int slot) {

        var stack = getSlotStack.get(slot);
        if (stack == null || stack.isEmpty())  return false;

        var lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) return false;

        List<Text> tooltip = lore.lines();
        if (tooltip == null || tooltip.isEmpty()) return false;

        boolean foundAny = false;
        for (Text line : tooltip) {
            String lineStr = line.getString();
            if (lineStr.replaceAll("§.", "").trim().isEmpty()) continue;
            
            if (exposesInfo(lineStr)) {
                foundAny = true;
            }
        }
        return foundAny;
    }

    private static boolean exposesInfo(String line) {
        if (isMenuMetaLine(line)) return false;

        Matcher m = MENU_ITEM_PATTERN.matcher(line);
        if (!m.find()) return false;

        String itemName = cleanItemName(m.group(1));
        String amountStr = m.group(2);

        if (itemName.isEmpty() || amountStr.equalsIgnoreCase("N/A")) {
            return false;
        }

        try {
            long collectionAmount = Long.parseLong(amountStr.replace(",", ""));
            if (!updateNeeded(itemName, collectionAmount)) return false;
            CollectionData.updateProgress(itemName, collectionAmount);
            return true;

        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Determine if the new collection amount is valid to update based on
     * staleness and expected value checks. (Only increase as data can be stale)
     */
    private static boolean updateNeeded(String itemName, long newAmount) {

        if (!CollectionData.hasBaseline(itemName)) return true;

        long currentBaseline = CollectionData.getCurrentCollection(itemName);
        long sessionGain = CollectionData.getSessionGain(itemName);
        
        if (sessionGain == 0) return true;
        
        long lastUpdated = CollectionData.getLastUpdated(itemName);
        long now = System.currentTimeMillis();
        
        if ((now - lastUpdated) > STALENESS_THRESHOLD_MS) return true;
        
        long currentTotal = currentBaseline + sessionGain;
        long diff = Math.abs(newAmount - currentTotal);

        return ((double)diff / currentTotal) < 0.0001 && newAmount >= currentTotal;
    }

    // Skill collection view

    private static boolean scanSkillMenu(SlotStackProvider getSlotStack) {
        return scanRankings(getSlotStack, SKILL_INDEX, net.minecraft.item.Items.EXPERIENCE_BOTTLE);
    }
    
    private static boolean scanRankings(SlotStackProvider getSlotStack, int slotIndex, net.minecraft.item.Item expectedItem) {

        var testStack = getSlotStack.get(slotIndex);
        if (testStack == null || testStack.isEmpty()) return false;
        if (expectedItem != null && !testStack.getItem().equals(expectedItem)) return false;

        var lore = testStack.get(DataComponentTypes.LORE);
        if (lore == null) return false;

        List<Text> tooltip = lore.lines();
        if (tooltip == null || tooltip.isEmpty()) return false;

        boolean foundAny = false;
        for (Text line : tooltip) {
            String lineStr = line.getString();
            if (lineStr.replaceAll("§.", "").trim().isEmpty()) continue;
            
            if (exposesInfo(lineStr)) {
                foundAny = true;
            }
        }

        if (foundAny) CollectionData.save();
        return foundAny;
    }

    private static boolean isIndividualCollection(String title) {
        return title != null && title.toLowerCase().endsWith(" collection");
    }    

    private static boolean isCollectionsMenu(String title) {
        return title != null && title.equalsIgnoreCase("collections");
    }

    private static boolean isSkillCollectionsMenu(String title) {
        return title != null && title.toLowerCase().endsWith(" collections");
    }

    private static void updateSingular(String itemName, long collectionAmount) {
        CollectionData.updateProgress(itemName, collectionAmount);
        CollectionData.save();
        ActiveDisplay.getInstance().invalidateAll();
    }

    private static boolean isMenuMetaLine(String line) {
        String lower = line.toLowerCase();
        return lower.contains("collections maxed out") ||
               lower.contains("boss collections maxed out") ||
               lower.contains("rankings may take") ||
               lower.contains("requires ") ||
               lower.contains("click to view") ||
               lower.contains("click to show rankings") ||
               lower.startsWith("view your") ||
               lower.startsWith("this menu") ||
               lower.startsWith("crafted minions") ||
               lower.startsWith("collections unlocked:") ||
               lower.startsWith("minion limit");
    }

    private static String extractItemNameFromTitle(String title) {
        if (title == null || title.isEmpty()) {
            return null;
        }
    
        String cleaned = title.replaceAll("(?i)\\s*collection\\s*$", "").trim();
        
        return cleaned.isEmpty() ? null : cleanItemName(cleaned);
    }

    private static String cleanItemName(String name) {
        if (name == null) return "";
        name = TextUtils.stripColor(name);
        name = name.replaceAll("[^a-zA-Z0-9 ]", "");
        return name.trim();
    }

    private static long convertMagnitude(String str) throws NumberFormatException {
        str = str.toLowerCase().replace(",", "").trim();
        
        if (str.endsWith("k")) {
            str = str.substring(0, str.length() - 1);
            double val = Double.parseDouble(str);
            return (long) (val * 1_000);
        } else if (str.endsWith("m")) {
            str = str.substring(0, str.length() - 1);
            double val = Double.parseDouble(str);
            return (long) (val * 1_000_000);
        } else {
            return Long.parseLong(str);
        }
    }
}
