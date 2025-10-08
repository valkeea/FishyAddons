package me.valkeea.fishyaddons.command;

import java.util.Map;

import me.valkeea.fishyaddons.api.HypixelPriceClient;
import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.TrackerProfiles;
import me.valkeea.fishyaddons.tracker.ItemTrackerData;
import me.valkeea.fishyaddons.tracker.TrackerUtils;
import me.valkeea.fishyaddons.util.FishyNotis;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class TrackerCmd {
    private TrackerCmd() {}
    private static final String KEY = me.valkeea.fishyaddons.config.Key.HUD_TRACKER_ENABLED;

    public static boolean handle(String[] args) {
        if (args.length == 0) {
            FishyNotis.fp();
            return false;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "toggle":
                return handleToggle();
            case "clear":
                return handleClear();
            case "stats":
                return handleStats();
            case "init":
                return handleInit();
            case "refresh":
                return handleRefresh();
            case "status":
                return handleStatus();
            case "price":
                return handlePrice(args);
            case "type":
                return handlePriceType(args);
            case "profile":
                return handleProfile(args);
            case "ignored":
                return handleIgnored();
            case "restore":
                return handleRestore(args);
            default:
                FishyNotis.fp();
                return false;
        }
    }
    
    private static boolean handleToggle() {
        boolean newState = !FishyConfig.getState(KEY, false);
        FishyConfig.enable(KEY, newState);
        String status = newState ? "§aenabled" : "§cdisabled";
        String msgStart = "§3Profit Tracker " + status;        
        me.valkeea.fishyaddons.tracker.TrackerUtils.refresh();
        
        if (newState && TrackerProfiles.hasJsonFile()) {
            if (TrackerProfiles.loadFromJson()) {
                FishyNotis.send(Text.literal(msgStart + " §7(loaded from file)"));
            } else {
                FishyNotis.send(Text.literal(msgStart));
            }
        } else {
            FishyNotis.send(Text.literal(msgStart));
        }
        return true;
    }

    private static boolean handlePriceType(String[] args) {
        if (args.length < 2) {
            FishyNotis.themed("Usage: /fp type §3<insta_sell | sell_offer>");
            return false;
        }

        String typeArg = args[1].toLowerCase();
        String newType;

        switch (typeArg) {
            case "insta_sell":
                newType = "sellPrice";
                break;
            case "sell_offer":
                newType = "buyPrice";
                break;
            default:
                FishyNotis.warn("Invalid type! Use 'insta_sell' or 'sell_offer'.");
                return false;
        }

        HypixelPriceClient.setPriceType(newType);
        FishyConfig.setString(me.valkeea.fishyaddons.config.Key.PRICE_TYPE, newType);
        ItemTrackerData.clearValueCache();
        ItemTrackerData.refreshPrices();
        FishyNotis.alert(Text.literal(String.format("§bPrice type set to §3%s", newType)));
        return true;
    }
    
    private static boolean handleClear() {
        ItemTrackerData.clearAll();
        FishyNotis.send("Profit Tracker data cleared");
        return true;
    }
    
    private static boolean handleStats() {
        Map<String, Integer> items = ItemTrackerData.getAllItems();
        
        if (items.isEmpty()) {
            FishyNotis.alert(Text.literal("§7No items tracked this session. Use §3/fp toggle §7to enable, §3/fp §7to see available commands."));
            return false;
        }
        
        long sessionTime = ItemTrackerData.getTotalDurationMinutes();
        int totalItems = ItemTrackerData.getTotalItemCount();
        double totalValue = ItemTrackerData.getTotalSessionValue();

        FishyNotis.themed("α Profit Tracker Stats α");

        String sessionTimeDisplay = String.format("§7Playtime: §3%d h", sessionTime / 60);
        if (ItemTrackerData.isCurrentlyPaused()) {
            long inactiveMinutes = ItemTrackerData.getInactiveMinutes();
            sessionTimeDisplay += String.format(" §8(paused for %d min)", inactiveMinutes);
        }
        FishyNotis.alert(Text.literal(sessionTimeDisplay));
        
        FishyNotis.alert(Text.literal(String.format("§7Total items: §3%d", totalItems)));
        
        if (totalValue > 0) {
            String valueStr = formatCoins(totalValue);
            long fullDuration = ItemTrackerData.getTotalDurationMinutes();
            long lastApiUpdate = ItemTrackerData.getLastApiUpdateTime();
            boolean hasRecentData = lastApiUpdate > 0 && (System.currentTimeMillis() - lastApiUpdate) < 300000;
            String apiStatus = hasRecentData ? " §a(live prices)" : " §c(estimated)";
            FishyNotis.alert(Text.literal(String.format("§7Value: §e%s%s", valueStr, apiStatus)));
            FishyNotis.alert(Text.literal(String.format("§7Per hour: §e%s", formatCoins(totalValue / Math.max(1, fullDuration) * 60))));
        }
        
        FishyNotis.alert(Text.literal("§7Top items (by value):"));
        
        HypixelPriceClient priceClient = ItemTrackerData.getPriceClient();
        items.entrySet().stream()
            .map(entry -> {
                String itemName = entry.getKey();
                int quantity = entry.getValue();
                double unitPrice = 0;
                
                if (priceClient != null) {
                    if (priceClient.hasBazaarData(itemName)) {
                        unitPrice = priceClient.getBazaarBuyPrice(itemName);
                    } else if (priceClient.hasAuctionData(itemName)) {
                        unitPrice = priceClient.getCachedAuctionPrice(itemName);
                    }
                }
                
                double itemTotal = unitPrice * quantity;
                return new ItemDisplayData(itemName, quantity, unitPrice, itemTotal);
            })
            .filter(data -> data.totalValue > 0)
            .sorted((a, b) -> Double.compare(b.totalValue, a.totalValue))
            .limit(5)
            .forEach(data -> {
                String itemName = capitalizeItemName(data.itemName);
                String unitValueStr = formatCoins(data.unitPrice);
                String totalValueStr = formatCoins(data.totalValue);
                FishyNotis.alert(Text.literal(String.format("  §a+%d §f%s §7(§e%s§7 each, §b%s§7 total)", 
                    data.quantity, itemName, unitValueStr, totalValueStr)));
            });
        
        return true;
    }
    
    private static boolean handleInit() {
        initTracking();
        FishyNotis.notice("Initialized price client");
        return true;
    }

    private static void initTracking() {
        ItemTrackerData.init();
        HypixelPriceClient priceClient = ItemTrackerData.getPriceClient();
        if (priceClient != null) {
            priceClient.clearAuctionCache();
        }
        
        ItemTrackerData.updateAllAsync();        
    }

    private static boolean handleRefresh() {
        ItemTrackerData.refreshPrices();
        FishyNotis.notice("§b§oRefreshing prices...");
        return true;
    }
    
    private static boolean handleStatus() {
        long lastBazaar = ItemTrackerData.getLastBazaarUpdateTime();
        long lastAuction = ItemTrackerData.getLastAuctionUpdateTime();

        FishyNotis.themed("  α API Status α  ");
        FishyNotis.alert(Text.literal("§7Price type: §3" + me.valkeea.fishyaddons.api.HypixelPriceClient.getType()));
        
        if (lastBazaar > 0) {
            long minutes = (System.currentTimeMillis() - lastBazaar) / 60000;
            FishyNotis.alert(Text.literal(String.format("§7Bazaar: §a✓ §7(%d min ago)", minutes)));
        } else {
            FishyNotis.alert(Text.literal("§7Bazaar: §c✗"));
        }
        
        if (lastAuction > 0) {
            long minutes = (System.currentTimeMillis() - lastAuction) / 60000;
            FishyNotis.alert(Text.literal(String.format("§7Auctions: §a✓ §7(%d min ago)", minutes)));
        } else {
            FishyNotis.alert(Text.literal("§7Auctions: §c✗ §7(on-demand)"));
        }
        
        return true;
    }
    
    private static boolean handleProfile(String[] args) {
        if (args.length < 2) {
            sendProfileClickable();
            return true;
        }

        String action = args[1].toLowerCase();

        if ("delete".equals(action)) {
            return profileDelete(args);
        }

        if ("rename".equals(action)) {
            return profileRename(args);
        }

        return profileSwitchOrCreate(args[1]);
    }

    private static boolean profileDelete(String[] args) {
        if (args.length < 3) {
            FishyNotis.themed("Usage: §3/fp profile delete <name>");
            return false;
        }

        String profileToDelete = args[2];
        if ("default".equals(profileToDelete)) {
            FishyNotis.warn("Cannot delete the default profile!");
            FishyNotis.alert(Text.literal("§7Use §3/fp clear §7if you wish to reset the session."));            
            return false;
        }

        if (TrackerProfiles.deleteProfile(profileToDelete)) {
            me.valkeea.fishyaddons.tracker.TrackerUtils.onDelete(profileToDelete);
        } else {
            FishyNotis.alert(Text.literal("§cProfile '" + profileToDelete + "' not found or cannot be deleted"));
        }
        return true;
    }

    private static boolean profileRename(String[] args) {
        if (args.length < 4) {
            FishyNotis.themed("Usage: §3/fp profile rename <old_name> <new_name>");
            return false;
        }

        String oldName = args[2];
        String newName = args[3];

        if (TrackerProfiles.renameProfile(oldName, newName)) {
            TrackerProfiles.setCurrentProfile(newName);
            FishyNotis.send(Text.literal("§aRenamed profile §b" + oldName + " §ato §b" + newName));
        } else {
            FishyNotis.alert(Text.literal("§cInvalid profile name or profile not found"));
        }

        return true;
    }

    private static boolean profileSwitchOrCreate(String profileName) {
        if (!FishyConfig.getState(KEY, false)) {
            FishyConfig.enable(KEY, true);
            initTracking();
        }

        String currentProfile = TrackerProfiles.getCurrentProfile();

        if (!"default".equals(currentProfile)) {
            TrackerProfiles.saveToJson();
        }

        if (profileName.equals(currentProfile)) {
            FishyNotis.send(Text.literal("§7Already using profile: §3" + profileName));
            return true;
        }

        java.util.List<String> availableProfiles = TrackerProfiles.getAvailableProfiles();
        if (availableProfiles.contains(profileName.toLowerCase())) {
            TrackerProfiles.setCurrentProfile(profileName);
            FishyNotis.send(Text.literal("§dSwitched to profile: §3" + profileName));
        } else {
            if (TrackerProfiles.createProfile(profileName)) {
                FishyNotis.send(Text.literal("§aCreated and switched to new profile: §3" + profileName));
            } else {
                FishyNotis.alert(Text.literal("§cFailed to create profile: " + profileName));
                return false;
            }
        }

        return true;
    }

    public static void sendProfileClickable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        String currentProfile = TrackerProfiles.getCurrentProfile();
        java.util.List<String> availableProfiles = TrackerProfiles.getAvailableProfiles();

        FishyNotis.alert(Text.literal("§b§lCurrent Profile: §3" + currentProfile));
            FishyNotis.alert(Text.literal("§3Click chat to switch to an existing profile:"));
            for (String profile : availableProfiles) {
                String marker = profile.equals(currentProfile) ? "§a▶ " : "§8  ";
                Text clickableProfile = Text.literal(marker + "§7"  + profile)
                        .styled(style -> style.withClickEvent(new net.minecraft.text.ClickEvent.RunCommand("/fp profile " + profile)));
                FishyNotis.alert(clickableProfile);
            }
            FishyNotis.alert(Text.literal("§3  /fp profile <name> §8- §bSwitch to or create profile"));
            FishyNotis.alert(Text.literal("§3  /fp profile delete <name> §8- §bDelete profile"));
    }    

    private static boolean handlePrice(String[] args) {
        if (args.length < 2) {
            FishyNotis.alert(Text.literal("§cUsage: /fp price [amount] <item>"));
            return false;
        }

        PriceQuery priceQuery = parsePriceArgs(args[1]);
        if (!priceQuery.isValid) {
            return false;
        }

        displayPriceQueryHeader(priceQuery);

        FishyNotis.alert(Text.literal("§7§oSearching..."));
        ItemTrackerData.getItemValueAsync(priceQuery.itemName, (value, source) -> handlePriceAsyncCallback(value, source, priceQuery.amount));

        return true;
    }

    private static class PriceQuery {
        final int amount;
        final String itemName;
        final boolean isValid;

        PriceQuery(int amount, String itemName, boolean isValid) {
            this.amount = amount;
            this.itemName = itemName;
            this.isValid = isValid;
        }
    }

    private static PriceQuery parsePriceArgs(String arg) {
        String[] splitArgs = arg.split(" ");
        int amount = 1;
        int itemStartIndex = 0;

        if (splitArgs.length > 1) {
            try {
                amount = Integer.parseInt(splitArgs[0]);
                if (amount <= 0) {
                    FishyNotis.send(Text.literal("§cAmount must be a positive number"));
                    return new PriceQuery(1, "", false);
                }
                itemStartIndex = 1;

                if (splitArgs.length < 2) {
                    FishyNotis.send(Text.literal("§cUsage: /fp price [amount] <item>"));
                    return new PriceQuery(1, "", false);
                }
            } catch (NumberFormatException e) {
                amount = 1;
                itemStartIndex = 0;
            }
        }

        StringBuilder itemName = new StringBuilder();
        for (int i = itemStartIndex; i < splitArgs.length; i++) {
            if (i > itemStartIndex) itemName.append(" ");
            itemName.append(splitArgs[i]);
        }

        return new PriceQuery(amount, itemName.toString(), true);
    }

    private static void displayPriceQueryHeader(PriceQuery priceQuery) {
        if (priceQuery.amount > 1) {
            FishyNotis.alert(Text.literal(String.format("§bPrice for §3%dx %s", priceQuery.amount, priceQuery.itemName)));
        } else {
            FishyNotis.alert(Text.literal(String.format("§bPrice for §3%s", priceQuery.itemName)));
        }
    }

    private static void handlePriceAsyncCallback(double value, String source, int amount) {
        if (value > 0) {
            double totalValue = value * amount;
            if (amount > 1) {
                FishyNotis.alert(Text.literal(String.format("§7[%s] §e%s §7each", source, formatCoins(value))));
                FishyNotis.alert(Text.literal(String.format("§7Total: §e%s", formatCoins(totalValue))));
            } else {
                FishyNotis.alert(Text.literal(String.format("§7[%s] §e%s", source, formatCoins(value))));
            }
        } else {
            FishyNotis.alert(Text.literal("§cNo price data found"));
        }
    }
    
    private static String formatCoins(double coins) {
        if (coins >= 1_000_000.0) {
            return String.format("%.1fm", coins / 1_000_000.0);
        } else if (coins >= 1_000.0) {
            return String.format("%.1fk", coins / 1_000.0);
        } else {
            return String.format("%.0f", coins);
        }
    }
    
    private static boolean handleIgnored() {
        me.valkeea.fishyaddons.hud.TrackerDisplay trackerInstance = me.valkeea.fishyaddons.hud.TrackerDisplay.getInstance();
        if (trackerInstance == null) {
            FishyNotis.alert(Text.literal("§cTracker display not initialized"));
            return false;
        }
        
        java.util.Set<String> excludedItems = trackerInstance.getExcludedItemsForDisplay();
        
        if (excludedItems.isEmpty()) {
            FishyNotis.alert(Text.literal("§7No items are currently ignored"));
            return true;
        }
        
        FishyNotis.send(Text.literal("§b§lIgnored Items:"));
        
        for (String itemName : excludedItems) {
            String displayName = capitalizeItemName(itemName);
            Text clickableItem = Text.literal("§8      - §f" + displayName + " §a[⟳]")
                    .styled(style -> style.withClickEvent(new net.minecraft.text.ClickEvent.RunCommand("/fp restore \"" + itemName + "\"")));
            FishyNotis.alert(clickableItem);
        }

        Text restoreAllText = Text.literal("§         [Restore All Items]").formatted(net.minecraft.util.Formatting.LIGHT_PURPLE)
                .styled(style -> style.withClickEvent(new net.minecraft.text.ClickEvent.RunCommand("/fp restore all")));
        FishyNotis.alert(restoreAllText);
        
        return true;
    }
    
    private static boolean handleRestore(String[] args) {
        me.valkeea.fishyaddons.hud.TrackerDisplay trackerInstance = me.valkeea.fishyaddons.hud.TrackerDisplay.getInstance();
        if (trackerInstance == null) {
            FishyNotis.alert(Text.literal("§cTracker display not initialized"));
            return false;
        }
        
        if (args.length < 2) {
            FishyNotis.alert(Text.literal("§cUsage: /fp restore <item_name> or /fp restore all"));
            return false;
        }
        
        String restoreTarget = args[1];
        
        if ("all".equals(restoreTarget)) {
            trackerInstance.restoreAllExcludedItems();
            return true;
        }
        
        if (restoreTarget.startsWith("\"") && restoreTarget.endsWith("\"")) {
            restoreTarget = restoreTarget.substring(1, restoreTarget.length() - 1);
        }
        
        if (!trackerInstance.getExcludedItemsForDisplay().contains(restoreTarget)) {
            String displayName = capitalizeItemName(restoreTarget);
            FishyNotis.alert(Text.literal("§c" + displayName + " is not in the ignored list"));
            return false;
        }
        
        trackerInstance.restoreExcludedItem(restoreTarget);
        return true;
    }
    
    private static String capitalizeItemName(String itemName) {
        if (itemName == null || itemName.isEmpty()) return itemName;
        
        String[] words = itemName.split(" ");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < words.length; i++) {
            if (i > 0) result.append(" ");
            String word = words[i];
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1));
                }
            }
        }
        
        return result.toString();
    }

    public static void profitPerHour() {
        if (TrackerUtils.isEnabled()) {
            double profitPerHour = ItemTrackerData.getTotalSessionValue() / Math.max(1, ItemTrackerData.getTotalDurationMinutes()) * 60.0;
            FishyNotis.alert(Text.literal(String.format("§7Per hour: §e%s", formatCoins(profitPerHour))));
        }
    }
    
    /**
     * Helper class for displaying item data sorted by value
     */
    private static class ItemDisplayData {
        final String itemName;
        final int quantity;
        final double unitPrice;
        final double totalValue;
        
        ItemDisplayData(String itemName, int quantity, double unitPrice, double totalValue) {
            this.itemName = itemName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.totalValue = totalValue;
        }
    }
}