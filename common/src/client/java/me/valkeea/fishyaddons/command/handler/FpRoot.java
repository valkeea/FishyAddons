package me.valkeea.fishyaddons.command.handler;

import java.util.Map;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import me.valkeea.fishyaddons.api.hypixel.PriceService;
import me.valkeea.fishyaddons.command.CommandBuilderUtils;
import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.TrackerProfiles;
import me.valkeea.fishyaddons.hud.elements.interactive.ProfitDisplay;
import me.valkeea.fishyaddons.tracker.PriceUtil;
import me.valkeea.fishyaddons.tracker.profit.ProfitTracker;
import me.valkeea.fishyaddons.tracker.profit.TrackedItemData;
import me.valkeea.fishyaddons.util.FishyNotis;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class FpRoot implements CommandHandler {
    
    @Override
    public String[] getRootNames() {
        return new String[]{"profit", "fp"};
    }
    
    @Override
    public void register(LiteralArgumentBuilder<FabricClientCommandSource> builder) {
        builder
            .then(toggleCmd())
            .then(statsCmd())
            .then(refreshCmd())
            .then(clearCmd())
            .then(ignoredCmd())
            .then(typeCmd())
            .then(priceCmd())
            .then(profileCmd())
            .then(restoreCmd())
            .executes(context -> {
                FishyNotis.fp();
                return 1;
            });
    }
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> toggleCmd() {
        return ClientCommandManager.literal("toggle")
        .executes(ctx -> handleToggle() ? 1 : 0);
    }
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> statsCmd() {
        return ClientCommandManager.literal("stats")
        .executes(ctx -> handleStats() ? 1 : 0);
    }
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> refreshCmd() {
        return ClientCommandManager.literal("refresh")
        .executes(ctx -> handleRefresh() ? 1 : 0);
    }
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> clearCmd() {
        return ClientCommandManager.literal("clear")
        .executes(ctx -> handleClear() ? 1 : 0);
    }
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> ignoredCmd() {
        return ClientCommandManager.literal("ignored")
        .executes(ctx -> handleIgnored() ? 1 : 0);
    }
    
    private static final String PROFILE = "profile";
    private static final String PRICE = "price";
    private static final String DELETE = "delete";    
    private static final String RENAME = "rename";
    private static final String RESTORE = "restore";
    private static final String INSTA = "insta_sell";
    private static final String OFFER = "sell_offer";
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> typeCmd() {
        return ClientCommandManager.literal("type")
        .then(ClientCommandManager.literal(INSTA)
        .executes(ctx -> handlePriceType(new String[]{"type", INSTA}) ? 1 : 0))
        .then(ClientCommandManager.literal(OFFER)
        .executes(ctx -> handlePriceType(new String[]{"type", OFFER}) ? 1 : 0))
        .executes(ctx -> handlePriceType(new String[]{"type"}) ? 1 : 0);
    }
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> priceCmd() {
        return ClientCommandManager.literal(PRICE)
        .then(ClientCommandManager.argument("item", StringArgumentType.greedyString())
        .executes(ctx -> {
            String item = StringArgumentType.getString(ctx, "item");
            return handlePrice(new String[]{PRICE, item}) ? 1 : 0;
        }))
        .executes(ctx -> handlePrice(new String[]{PRICE}) ? 1 : 0);
    }
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> profileCmd() {
        return ClientCommandManager.literal(PROFILE)
            .then(ClientCommandManager.literal(DELETE)
                .then(ClientCommandManager.argument(PROFILE, StringArgumentType.greedyString())
                    .executes(ctx -> {
                        String profileName = StringArgumentType.getString(ctx, PROFILE);
                        return handleProfile(new String[]{PROFILE, DELETE, profileName}) ? 1 : 0;
                    })))
            .then(ClientCommandManager.literal(RENAME)
                .then(ClientCommandManager.argument("oldName", StringArgumentType.word())
                    .then(ClientCommandManager.argument("newName", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            String oldName = StringArgumentType.getString(ctx, "oldName");
                            String newName = StringArgumentType.getString(ctx, "newName");
                            return handleProfile(new String[]{PROFILE, RENAME, oldName, newName}) ? 1 : 0;
                        }))))
            .then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
                .executes(ctx -> {
                    String profileName = StringArgumentType.getString(ctx, "name");
                    return handleProfile(new String[]{PROFILE, profileName}) ? 1 : 0;
                }))
            .executes(ctx -> {
                sendProfileClickable();
                return 1;
            });
    }
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> restoreCmd() {
        return ClientCommandManager.literal(RESTORE)
            .then(ClientCommandManager.literal("all")
                .executes(ctx -> handleRestore(new String[]{RESTORE, "all"}) ? 1 : 0))
            .then(ClientCommandManager.argument("item", StringArgumentType.greedyString())
                .executes(ctx -> {
                    String item = StringArgumentType.getString(ctx, "item");
                    return handleRestore(new String[]{RESTORE, item}) ? 1 : 0;
                }))
            .executes(ctx -> handleRestore(new String[]{RESTORE}) ? 1 : 0);
    }

    private static final String KEY = me.valkeea.fishyaddons.config.Key.HUD_PROFIT_ENABLED;
    
    private static boolean handleToggle() {
        boolean newState = !FishyConfig.getState(KEY, false);
        FishyConfig.setState(KEY, newState);
        String status = newState ? "§aenabled" : "§cdisabled";
        String msgStart = "§3Profit Tracker " + status;        
        me.valkeea.fishyaddons.tracker.PriceUtil.refresh();
        
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
        var cmdArgs = new CommandBuilderUtils.CommandArgs(args);
        
        if (!cmdArgs.has(1)) {
            FishyNotis.themed("Usage: /fp type §3<insta_sell | sell_offer>");
            return false;
        }

        String newType;
        if (cmdArgs.matches(1, INSTA)) {
            newType = "sellPrice";
        } else if (cmdArgs.matches(1, OFFER)) {
            newType = "buyPrice";
        } else {
            FishyNotis.warn("Invalid type! Use 'insta_sell' or 'sell_offer'.");
            return false;
        }

        PriceService.setPriceType(newType);
        FishyConfig.setString(me.valkeea.fishyaddons.config.Key.PRICE_TYPE, newType);
        PriceUtil.refreshPrices();
        FishyNotis.alert(Text.literal(String.format("§bPrice type set to §3%s", newType)));
        return true;
    }
    
    private static boolean handleClear() {
        TrackedItemData.clearAll();
        FishyNotis.send("Profit Tracker data cleared");
        return true;
    }
    
    private static boolean handleStats() {
        Map<String, Integer> items = TrackedItemData.getAllItems();
        
        if (items.isEmpty()) {
            FishyNotis.alert(Text.literal("§7No items tracked this session. Use §3/fp toggle §7to enable, §3/fp §7to see available commands."));
            return false;
        }
        
        long sessionTime = TrackedItemData.getTotalDurationMinutes();
        int totalItems = TrackedItemData.getTotalItemCount();
        double totalValue = TrackedItemData.getTotalSessionValue();

        FishyNotis.themed("α Profit Tracker Stats α");

        String sessionTimeDisplay = String.format("§7Playtime: §3%d h", sessionTime / 60);
        if (TrackedItemData.isCurrentlyPaused()) {
            long inactiveMinutes = TrackedItemData.getInactiveMinutes();
            sessionTimeDisplay += String.format(" §8(paused for %d min)", inactiveMinutes);
        }

        FishyNotis.alert(Text.literal(sessionTimeDisplay));
        FishyNotis.alert(Text.literal(String.format("§7Total items: §3%d", totalItems)));
        
        if (totalValue > 0) {
            String valueStr = formatCoins(totalValue);
            long fullDuration = TrackedItemData.getTotalDurationMinutes();
            long lastApiUpdate = PriceUtil.getLastApiUpdateTime();
            boolean hasRecentData = lastApiUpdate > 0 && (System.currentTimeMillis() - lastApiUpdate) < 300000;
            String apiStatus = hasRecentData ? " §a(live prices)" : " §c(estimated)";
            FishyNotis.alert(Text.literal(String.format("§7Value: §e%s%s", valueStr, apiStatus)));
            FishyNotis.alert(Text.literal(String.format("§7Per hour: §e%s", formatCoins(totalValue / Math.max(1, fullDuration) * 60))));
        }
        
        FishyNotis.alert(Text.literal("§7Top items (by value):"));
        
        items.entrySet().stream()
            .map(entry -> {
                String itemName = entry.getKey();
                int quantity = entry.getValue();
                double unitPrice = TrackedItemData.getPrice(itemName);
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

    private static boolean handleRefresh() {
        PriceUtil.refreshPrices();
        FishyNotis.notice("§b§oRefreshing prices...");
        return true;
    }
    
    private static boolean handleProfile(String[] args) {
        var cmdArgs = new CommandBuilderUtils.CommandArgs(args);
        
        if (!cmdArgs.has(1)) {
            sendProfileClickable();
            return true;
        }

        if (cmdArgs.matches(1, DELETE)) {
            return profileDelete(cmdArgs);
        }
        
        if (cmdArgs.matches(1, RENAME)) {
            return profileRename(cmdArgs);
        }

        return profileSwitchOrCreate(cmdArgs.get(1));
    }

    private static boolean profileDelete(CommandBuilderUtils.CommandArgs args) {
        if (!args.has(2)) {
            FishyNotis.themed("Usage: §3/fp profile delete <name>");
            return false;
        }

        if ("default".equals(args.get(2))) {
            FishyNotis.warn("Cannot delete the default profile!");
            FishyNotis.alert(Text.literal("§7Use §3/fp clear §7if you wish to reset the session."));            
            return false;
        }

        String profileToDelete = args.get(2);
        if (TrackerProfiles.deleteProfile(profileToDelete)) {
            ProfitTracker.onDelete(profileToDelete);
        } else {
            FishyNotis.alert(Text.literal("§cProfile '" + profileToDelete + "' not found or cannot be deleted"));
        }
        return true;
    }

    private static boolean profileRename(CommandBuilderUtils.CommandArgs args) {
        if (!args.has(3)) {
            FishyNotis.themed("Usage: §3/fp profile rename <old_name> <new_name>");
            return false;
        }

        if (TrackerProfiles.renameProfile(args.get(2), args.get(3))) {
            String newName = args.get(3);
            TrackerProfiles.setCurrentProfile(newName);
            String oldName = args.get(2);
            FishyNotis.send(Text.literal("§aRenamed profile §b" + oldName + " §ato §b" + newName));
        } else {
            FishyNotis.alert(Text.literal("§cInvalid profile name or profile not found"));
        }

        return true;
    }

    private static boolean profileSwitchOrCreate(String profileName) {
        if (!FishyConfig.getState(KEY, false)) {
            FishyConfig.setState(KEY, true);
        }

        var currentProfile = TrackerProfiles.getCurrentProfile();

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
            ProfitDisplay.refreshDisplay();
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
        var mc = MinecraftClient.getInstance();
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
        var cmdArgs = new CommandBuilderUtils.CommandArgs(args);
        
        if (!cmdArgs.has(1)) {
            FishyNotis.alert(Text.literal("§cUsage: /fp price [amount] <item>"));
            return false;
        }

        var priceQuery = parsePriceArgs(cmdArgs.get(1));
        if (!priceQuery.isValid) {
            return false;
        }

        displayPriceQueryHeader(priceQuery);

        FishyNotis.alert(Text.literal("§7§oSearching..."));
        handlePrice(TrackedItemData.getPrice(priceQuery.itemName), priceQuery.amount);

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

        var itemName = new StringBuilder();
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

    private static void handlePrice(double value, int amount) {
        if (value > 0) {
            double totalValue = value * amount;
            if (amount > 1) {
                FishyNotis.alert(Text.literal(String.format("§7§e%s §7each", formatCoins(value))));
                FishyNotis.alert(Text.literal(String.format("§7Total: §e%s", formatCoins(totalValue))));
            } else {
                FishyNotis.alert(Text.literal(String.format("§7§e%s", formatCoins(value))));
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
        var tracker = ProfitDisplay.getInstance();
        if (tracker == null) {
            FishyNotis.alert(Text.literal("§cTracker display not initialized"));
            return false;
        }
        
        java.util.Set<String> excludedItems = tracker.getExcludedItemsForDisplay();
        
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
        var cmdArgs = new CommandBuilderUtils.CommandArgs(args);
        var tracker = ProfitDisplay.getInstance();
        
        if (tracker == null) {
            FishyNotis.alert(Text.literal("§cTracker display not initialized"));
            return false;
        }
        
        if (!cmdArgs.has(1)) {
            FishyNotis.alert(Text.literal("§cUsage: /fp restore <item_name> or /fp restore all"));
            return false;
        }
        
        String restoreTarget = cmdArgs.get(1);
        
        if ("all".equals(restoreTarget)) {
            tracker.restoreAllExcludedItems();
            return true;
        }
        
        if (restoreTarget.startsWith("\"") && restoreTarget.endsWith("\"")) {
            restoreTarget = restoreTarget.substring(1, restoreTarget.length() - 1);
        }
        
        if (!tracker.getExcludedItemsForDisplay().contains(restoreTarget)) {
            String displayName = capitalizeItemName(restoreTarget);
            FishyNotis.alert(Text.literal("§c" + displayName + " is not in the ignored list"));
            return false;
        }
        
        tracker.restoreExcludedItem(restoreTarget);
        return true;
    }
    
    private static String capitalizeItemName(String itemName) {
        if (itemName == null || itemName.isEmpty()) return itemName;
        
        String[] words = itemName.split(" ");
        var result = new StringBuilder();
        
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
        if (ProfitTracker.isEnabled()) {
            double profitPerHour = TrackedItemData.getTotalSessionValue() / Math.max(1, TrackedItemData.getTotalDurationMinutes()) * 60.0;
            FishyNotis.alert(Text.literal(String.format("§7Per hour: §e%s", formatCoins(profitPerHour))));
        }
    }

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
