package me.valkeea.fishyaddons.command;

import java.util.Map;

import me.valkeea.fishyaddons.api.HypixelPriceClient;
import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.tracker.ItemTrackerData;
import me.valkeea.fishyaddons.util.FishyNotis;
import net.minecraft.text.Text;

public class ProfitTrackerCommand {
    private ProfitTrackerCommand() {}
    private static final String KEY = me.valkeea.fishyaddons.config.Key.HUD_TRACKER_ENABLED;

    public static boolean handle(String[] args) {
        if (args.length == 0) {
            showUsage();
            return false;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "toggle":
                return handleToggle();
            case "on":
                return handleOn();
            case "off":
                return handleOff();
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
            default:
                showUsage();
                return false;
        }
    }
    
    private static boolean handleToggle() {
        boolean newState = !FishyConfig.getState(KEY, false);
        FishyConfig.enable(KEY, newState);
        String status = newState ? "§aenabled" : "§cdisabled";
        me.valkeea.fishyaddons.tracker.TrackerUtils.refresh();
        
        // When enabling, check for existing JSON file and load it
        if (newState && ItemTrackerData.hasJsonFile()) {
            if (ItemTrackerData.loadFromJson()) {
                FishyNotis.send(Text.literal("§3Profit Tracker " + status + " §7(loaded from file)"));
            } else {
                FishyNotis.send(Text.literal("§3Profit Tracker " + status));
            }
        } else {
            FishyNotis.send(Text.literal("§3Profit Tracker " + status));
        }
        return true;
    }

    private static boolean handlePriceType(String[] args) {
        if (args.length < 2) {
            FishyNotis.alert(Text.literal("§cUsage: /fa profit type [insta_sell|sell_offer]"));
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
                FishyNotis.alert(Text.literal("§cInvalid type! Use 'insta_sell' or 'sell_offer'."));
                return false;
        }

        me.valkeea.fishyaddons.api.HypixelPriceClient.setPriceType(newType);
        me.valkeea.fishyaddons.config.FishyConfig.setString(me.valkeea.fishyaddons.config.Key.PRICE_TYPE, newType);
        
        // Clear cached values in ItemTrackerData to force re-calculation with new price type
        ItemTrackerData.clearValueCache();
        
        // Refresh prices to populate cache with new price type
        me.valkeea.fishyaddons.tracker.ItemTrackerData.refreshPrices();
        FishyNotis.alert(Text.literal(String.format("§bPrice type set to §3%s", newType)));
        return true;
    }

    private static boolean handleOn() {
        FishyConfig.enable(KEY, true);
        me.valkeea.fishyaddons.tracker.TrackerUtils.refresh();
        handleInit(); // Ensure APIs are initialized
        
        if (ItemTrackerData.hasJsonFile()) {
            if (ItemTrackerData.loadFromJson()) {
                FishyNotis.send(Text.literal("§3Profit Tracker §aenabled §7(loaded from file)"));
            } else {
                FishyNotis.send(Text.literal("§3Profit Tracker §aenabled"));
            }
        } else {
            FishyNotis.send(Text.literal("§3Profit Tracker §aenabled"));
            FishyNotis.alert(Text.literal("§7Started background price updates..."));            
        }
        return true;
    }
    
    private static boolean handleOff() {
        FishyConfig.enable(KEY, false);
        me.valkeea.fishyaddons.tracker.TrackerUtils.refresh();
        FishyNotis.send(Text.literal("§3Profit Tracker §cdisabled"));
        return true;
    }
    
    private static boolean handleClear() {
        ItemTrackerData.clearAll();
        FishyNotis.send(Text.literal("§3Profit Tracker data cleared"));
        return true;
    }
    
    private static boolean handleStats() {
        Map<String, Integer> items = ItemTrackerData.getAllItems();
        
        if (items.isEmpty()) {
            FishyNotis.alert(Text.literal("§7No items tracked this session"));
            return false;
        }
        
        long sessionTime = ItemTrackerData.getSessionDurationMinutes();
        int totalItems = ItemTrackerData.getTotalItemCount();
        double totalValue = ItemTrackerData.getTotalSessionValue();
        
        FishyNotis.alert(Text.literal("§b  α Profit Tracker Stats α  "));
        FishyNotis.alert(Text.literal(String.format("§7Session: §3%d minutes", sessionTime)));
        FishyNotis.alert(Text.literal(String.format("§7Total items: §3%d", totalItems)));
        
        if (totalValue > 0) {
            String valueStr = formatCoins(totalValue);
            long lastApiUpdate = ItemTrackerData.getLastApiUpdateTime();
            boolean hasRecentData = lastApiUpdate > 0 && (System.currentTimeMillis() - lastApiUpdate) < 300000;
            String apiStatus = hasRecentData ? " §a(live prices)" : " §c(estimated)";
            FishyNotis.alert(Text.literal(String.format("§7Value: §e%s%s", valueStr, apiStatus)));
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
        ItemTrackerData.init();
        HypixelPriceClient priceClient = ItemTrackerData.getPriceClient();
        if (priceClient != null) {
            priceClient.clearAuctionCache();
        }
        
        ItemTrackerData.updateAllAsync();
        FishyNotis.send(Text.literal("§3Initialized price client"));
        return true;
    }
    
    private static boolean handleRefresh() {
        ItemTrackerData.refreshPrices();
        FishyNotis.alert(Text.literal("§bRefreshing prices..."));
        return true;
    }
    
    private static boolean handleStatus() {
        long lastBazaar = ItemTrackerData.getLastBazaarUpdateTime();
        long lastAuction = ItemTrackerData.getLastAuctionUpdateTime();
        
        FishyNotis.alert(Text.literal("§b  α API Status α  "));
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
            FishyNotis.alert(Text.literal("§7Auctions: §c✗"));
        }
        
        return true;
    }
    
    private static boolean handleProfile(String[] args) {
        if (args.length < 2) {
            // Show current profile and available profiles
            String currentProfile = ItemTrackerData.getCurrentProfile();
            java.util.List<String> availableProfiles = ItemTrackerData.getAvailableProfiles();
            
            FishyNotis.send(Text.literal("§bCurrent Profile: §3" + currentProfile));
            FishyNotis.alert(Text.literal("§7Available Profiles:"));
            for (String profile : availableProfiles) {
                String marker = profile.equals(currentProfile) ? "§a▶ " : "§7- ";
                FishyNotis.alert(Text.literal(marker + "§f" + profile));
            }
            FishyNotis.alert(Text.literal("§7Usage: /fa profit profile <name> - Switch to or create profile"));
            FishyNotis.alert(Text.literal("§7Usage: /fa profit profile delete <name> - Delete profile"));
            return true;
        }

        String action = args[1].toLowerCase();
        
        if ("delete".equals(action)) {
            if (args.length < 3) {
                FishyNotis.alert(Text.literal("§cUsage: /fa profit profile delete <name>"));
                return false;
            }
            
            String profileToDelete = args[2];
            if ("default".equals(profileToDelete)) {
                FishyNotis.alert(Text.literal("§cCannot delete the default profile!"));
                return false;
            }
            
            if (ItemTrackerData.deleteProfile(profileToDelete)) {
                FishyNotis.send(Text.literal("§cDeleted profile: " + profileToDelete));
                // If current profile was deleted, switch to default
                if (profileToDelete.equals(ItemTrackerData.getCurrentProfile())) {
                    ItemTrackerData.setCurrentProfile("default");
                    FishyNotis.send(Text.literal("§3Switched to default profile"));
                }
            } else {
                FishyNotis.alert(Text.literal("§cProfile '" + profileToDelete + "' not found or cannot be deleted"));
            }
            return true;
        }
        
        String profileName = args[1];
        String currentProfile = ItemTrackerData.getCurrentProfile();

        if (!"default".equals(currentProfile)) {
            ItemTrackerData.saveToJson();
        }

        if (profileName.equals(currentProfile)) {
            FishyNotis.send(Text.literal("§7Already using profile: §3" + profileName));
            return true;
        }
        
        // Check if profile exists
        java.util.List<String> availableProfiles = ItemTrackerData.getAvailableProfiles();
        if (availableProfiles.contains(profileName)) {
            ItemTrackerData.setCurrentProfile(profileName);
            FishyNotis.send(Text.literal("§3Switched to profile: §b" + profileName));
        } else {
            // Create new profile
            if (ItemTrackerData.createProfile(profileName)) {
                FishyNotis.send(Text.literal("§aCreated and switched to new profile: §b" + profileName));
            } else {
                FishyNotis.alert(Text.literal("§cFailed to create profile: " + profileName));
            }
        }
        
        return true;
    }
    
    private static boolean handlePrice(String[] args) {
        if (args.length < 2) {
            FishyNotis.alert(Text.literal("§cUsage: /fa profit price [amount] <item>"));
            return false;
        }

        String fullArgument = args[1];
        String[] splitArgs = fullArgument.split(" ");

        int amount = 1;
        int itemStartIndex = 0;

        if (splitArgs.length > 1) {
            try {
                amount = Integer.parseInt(splitArgs[0]);
                if (amount <= 0) {
                    FishyNotis.send(Text.literal("§cAmount must be a positive number"));
                    return false;
                }
                itemStartIndex = 1;

                if (splitArgs.length < 2) {
                    FishyNotis.send(Text.literal("§cUsage: /fa profit price [amount] <item>"));
                    return false;
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

        String item = itemName.toString();
        final int finalAmount = amount;

        if (amount > 1) {
            FishyNotis.alert(Text.literal(String.format("§bPrice for §3%dx %s", amount, item)));
        } else {
            FishyNotis.alert(Text.literal(String.format("§bPrice for §3%s", item)));
        }
        FishyNotis.alert(Text.literal("§7Searching..."));
        ItemTrackerData.getItemValueAsync(item, (value, source) -> {
            if (value > 0) {
                double totalValue = value * finalAmount;
                if (finalAmount > 1) {
                    FishyNotis.alert(Text.literal(String.format("§7[%s] §e%s §7each", source, formatCoins(value))));
                    FishyNotis.alert(Text.literal(String.format("§7Total: §e%s", formatCoins(totalValue))));
                } else {
                    FishyNotis.alert(Text.literal(String.format("§7[%s] §e%s", source, formatCoins(value))));
                }
            } else {
                FishyNotis.alert(Text.literal("§cNo price data found"));
            }
        });

        return true;
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
    
    protected static void showUsage() {
        FishyNotis.alert(Text.literal("§b  α Profit Tracker Commands α  "));
        FishyNotis.alert(Text.literal("§3/fa profit on/off/toggle §7- Enable/disable"));        
        FishyNotis.alert(Text.literal("§3/fa profit clear §7- Clear current data"));
        FishyNotis.alert(Text.literal("§3/fa profit refresh §7- Refresh cached prices"));        
        FishyNotis.alert(Text.literal("§3/fa profit show §7- Show session stats"));
        FishyNotis.alert(Text.literal("§3/fa profit init §7- Manually initialize APIs"));
        FishyNotis.alert(Text.literal("§3/fa profit status §7- Check API status"));
        FishyNotis.alert(Text.literal("§3/fa profit price [amount] <item> §7- Check price"));
        FishyNotis.alert(Text.literal("§3/fa profit profile [name] §7- Create or switch to a profile"));
        FishyNotis.send(Text.literal("§bYou can also switch profile using HUD buttons!"));
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