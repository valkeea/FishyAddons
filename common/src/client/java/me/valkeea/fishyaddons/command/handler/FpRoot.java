package me.valkeea.fishyaddons.command.handler;

import java.util.Map;
import java.util.Set;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import me.valkeea.fishyaddons.api.hypixel.PriceService;
import me.valkeea.fishyaddons.command.CommandBuilderUtils;
import me.valkeea.fishyaddons.command.GatedGreedyString;
import me.valkeea.fishyaddons.hud.elements.interactive.ProfitDisplay;
import me.valkeea.fishyaddons.tracker.PriceUtil;
import me.valkeea.fishyaddons.tracker.profit.ProfitTracker;
import me.valkeea.fishyaddons.tracker.profit.TrackedItemData;
import me.valkeea.fishyaddons.tracker.profit.TrackerProfiles;
import me.valkeea.fishyaddons.util.FishyNotis;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.api.StringKey;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

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
            .executes(ctx -> {
                FishyNotis.fp();
                return 1;
            });
    }
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> toggleCmd() {
        return literal("toggle")
        .executes(ctx -> handleToggle() ? 1 : 0);
    }
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> statsCmd() {
        return literal("stats")
        .executes(ctx -> handleStats() ? 1 : 0);
    }
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> refreshCmd() {
        return literal("refresh")
        .executes(ctx -> handleRefresh() ? 1 : 0);
    }
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> clearCmd() {
        return literal("clear")
        .executes(ctx -> handleClear() ? 1 : 0);
    }
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> ignoredCmd() {
        return literal("ignored")
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
        return literal("type")
        .then(literal(INSTA)
            .executes(ctx -> handlePriceType(new String[]{"type", INSTA}) ? 1 : 0))
        .then(literal(OFFER)
            .executes(ctx -> handlePriceType(new String[]{"type", OFFER}) ? 1 : 0))
        .executes(ctx -> handlePriceType(new String[]{"type"}) ? 1 : 0);
    }
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> priceCmd() {
        return literal(PRICE).then(ClientCommands.argument("item", StringArgumentType.greedyString())
            .executes(ctx -> {
                var item = StringArgumentType.getString(ctx, "item");
                return handlePrice(new String[]{PRICE, item}) ? 1 : 0;
            }))
        .executes(ctx -> handlePrice(new String[]{PRICE}) ? 1 : 0);
    }
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> profileCmd() {
        return literal(PROFILE).then(literal(DELETE).then(ClientCommands.argument(PROFILE, StringArgumentType.greedyString())
            .executes(ctx -> {
                var profileName = StringArgumentType.getString(ctx, PROFILE);
                return handleProfile(new String[]{PROFILE, DELETE, profileName}) ? 1 : 0;
            })))
        .then(literal(RENAME).then(ClientCommands.argument("oldName", StringArgumentType.word())
        .then(ClientCommands.argument("newName", StringArgumentType.greedyString())
            .executes(ctx -> {
                var oldName = StringArgumentType.getString(ctx, "oldName");
                var newName = StringArgumentType.getString(ctx, "newName");
                return handleProfile(new String[]{PROFILE, RENAME, oldName, newName}) ? 1 : 0;
            }))))
        .then(ClientCommands.argument("name", GatedGreedyString.excluding(Set.of(DELETE, RENAME)))
            .executes(ctx -> {
                var profileName = StringArgumentType.getString(ctx, "name");
                return handleProfile(new String[]{PROFILE, profileName}) ? 1 : 0;
            }))
        .executes(ctx -> {
            sendProfileClickable();
            return 1;
        });
    }

    private static final String ALL = "all";
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> restoreCmd() {
        return literal(RESTORE).then(literal(ALL)
            .executes(ctx -> handleRestore(new String[]{RESTORE, ALL}) ? 1 : 0))
        .then(ClientCommands.argument("item", GatedGreedyString.excluding(Set.of(ALL)))
            .executes(ctx -> {
                var item = StringArgumentType.getString(ctx, "item");
                return handleRestore(new String[]{RESTORE, item}) ? 1 : 0;
            }))
        .executes(ctx -> handleRestore(new String[]{RESTORE}) ? 1 : 0);
    }

    private static final BooleanKey KEY = BooleanKey.HUD_PROFIT_ENABLED;
    
    private static boolean handleToggle() {
        boolean newState = !Config.get(KEY);
        Config.set(KEY, newState);
        var status = newState ? "§aenabled" : "§cdisabled";
        var msgStart = "§3Profit Tracker " + status;        
        PriceUtil.refresh();
        
        if (newState && TrackerProfiles.hasJsonFile()) {
            if (TrackerProfiles.loadFromJson()) {
                FishyNotis.send(Component.literal(msgStart + " §7(loaded from file)"));
            } else {
                FishyNotis.send(Component.literal(msgStart));
            }
        } else {
            FishyNotis.send(Component.literal(msgStart));
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
        Config.set(StringKey.PRICE_TYPE, newType);
        PriceUtil.refreshPrices();
        FishyNotis.alert(Component.literal(String.format("§bPrice type set to §3%s", newType)));
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
            FishyNotis.alert(Component.literal("§7No items tracked this session. Use §3/fp toggle §7to enable, §3/fp §7to see available commands."));
            return false;
        }
        
        long sessionTime = TrackedItemData.getTotalDurationMinutes();
        int totalItems = TrackedItemData.getTotalItemCount();
        double totalValue = TrackedItemData.getTotalSessionValue();

        FishyNotis.themed("α Profit Tracker Stats α");

        var sessionTimeDisplay = String.format("§7Playtime: §3%d h", sessionTime / 60);
        if (TrackedItemData.isCurrentlyPaused()) {
            long inactiveMinutes = TrackedItemData.getInactiveMinutes();
            sessionTimeDisplay += String.format(" §8(paused for %d min)", inactiveMinutes);
        }

        FishyNotis.alert(Component.literal(sessionTimeDisplay));
        FishyNotis.alert(Component.literal(String.format("§7Total items: §3%d", totalItems)));
        
        if (totalValue > 0) {
            var valueStr = formatCoins(totalValue);
            long fullDuration = TrackedItemData.getTotalDurationMinutes();
            FishyNotis.alert(Component.literal(String.format("§7Value: §e%s", valueStr)));
            FishyNotis.alert(Component.literal(String.format("§7Per hour: §e%s", formatCoins(totalValue / Math.max(1, fullDuration) * 60))));
        }
        
        FishyNotis.alert(Component.literal("§7Top items (by value):"));
        
        items.entrySet().stream()
            .map(entry -> {
                var itemName = entry.getKey();
                int quantity = entry.getValue();
                double unitPrice = TrackedItemData.getPrice(itemName);
                double itemTotal = unitPrice * quantity;
                return new ItemDisplayData(itemName, quantity, unitPrice, itemTotal);
            })
            .filter(data -> data.totalValue > 0)
            .sorted((a, b) -> Double.compare(b.totalValue, a.totalValue))
            .limit(5)
            .forEach(data -> {
                var itemName = capitalizeItemName(data.itemName);
                var unitValueStr = formatCoins(data.unitPrice);
                var totalValueStr = formatCoins(data.totalValue);
                FishyNotis.alert(Component.literal(String.format("  §a+%d §f%s §7(§e%s§7 each, §b%s§7 total)", 
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
            FishyNotis.alert(Component.literal("§7Use §3/fp clear §7if you wish to reset the session."));            
            return false;
        }

        var profileToDelete = args.get(2);
        if (TrackerProfiles.deleteProfile(profileToDelete)) {
            ProfitTracker.onDelete(profileToDelete);
        } else {
            FishyNotis.alert(Component.literal("§cProfile '" + profileToDelete + "' not found or cannot be deleted"));
        }
        return true;
    }

    private static boolean profileRename(CommandBuilderUtils.CommandArgs args) {
        if (!args.has(3)) {
            FishyNotis.themed("Usage: §3/fp profile rename <old_name> <new_name>");
            return false;
        }

        if (TrackerProfiles.renameProfile(args.get(2), args.get(3))) {
            var newName = args.get(3);
            TrackerProfiles.setCurrentProfile(newName);
            var oldName = args.get(2);
            FishyNotis.send(Component.literal("§aRenamed profile §b" + oldName + " §ato §b" + newName));
        } else {
            FishyNotis.alert(Component.literal("§cInvalid profile name or profile not found"));
        }

        return true;
    }

    private static boolean profileSwitchOrCreate(String profileName) {
        if (!Config.get(KEY)) {
            Config.set(KEY, true);
        }

        var currentProfile = TrackerProfiles.getCurrentProfile();

        if (!"default".equals(currentProfile)) {
            TrackerProfiles.saveToJson();
        }

        if (profileName.equals(currentProfile)) {
            FishyNotis.send(Component.literal("§7Already using profile: §3" + profileName));
            return true;
        }

        var availableProfiles = TrackerProfiles.getAvailableProfiles();
        if (availableProfiles.contains(profileName.toLowerCase())) {
            TrackerProfiles.setCurrentProfile(profileName);
            ProfitDisplay.refreshDisplay();
            FishyNotis.send(Component.literal("§dSwitched to profile: §3" + profileName));
        } else {
            if (TrackerProfiles.createProfile(profileName)) {
                FishyNotis.send(Component.literal("§aCreated and switched to new profile: §3" + profileName));
            } else {
                FishyNotis.alert(Component.literal("§cFailed to create profile: " + profileName));
                return false;
            }
        }

        return true;
    }

    public static void sendProfileClickable() {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;

        var currentProfile = TrackerProfiles.getCurrentProfile();
        var availableProfiles = TrackerProfiles.getAvailableProfiles();

        FishyNotis.alert(Component.literal("§b§lCurrent Profile: §3" + currentProfile));
        FishyNotis.alert(Component.literal("§3Click chat to switch to an existing profile:"));

        for (var profile : availableProfiles) {
            var marker = profile.equals(currentProfile) ? "§a▶ " : "§8  ";
            var clickableProfile = Component.literal(marker + "§7"  + profile)
                    .withStyle(style -> style.withClickEvent(new net.minecraft.network.chat.ClickEvent.RunCommand("/fp profile " + profile)));
            FishyNotis.alert(clickableProfile);
        }

        FishyNotis.alert(Component.literal("§3  /fp profile <name> §8- §bSwitch to or create profile"));
        FishyNotis.alert(Component.literal("§3  /fp profile delete <name> §8- §bDelete profile"));
    }    

    private static boolean handlePrice(String[] args) {
        var cmdArgs = new CommandBuilderUtils.CommandArgs(args);
        
        if (!cmdArgs.has(1)) {
            FishyNotis.alert(Component.literal("§cUsage: /fp price [amount] <item>"));
            return false;
        }

        var priceQuery = parsePriceArgs(cmdArgs.get(1));
        if (!priceQuery.isValid) {
            return false;
        }

        displayPriceQueryHeader(priceQuery);

        FishyNotis.alert(Component.literal("§7§oSearching..."));
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
                    FishyNotis.send(Component.literal("§cAmount must be a positive number"));
                    return new PriceQuery(1, "", false);
                }

                itemStartIndex = 1;

                if (splitArgs.length < 2) {
                    FishyNotis.send(Component.literal("§cUsage: /fp price [amount] <item>"));
                    return new PriceQuery(1, "", false);
                }

            } catch (NumberFormatException _) {
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
            FishyNotis.alert(Component.literal(String.format("§bPrice for §3%dx %s", priceQuery.amount, priceQuery.itemName)));
        } else {
            FishyNotis.alert(Component.literal(String.format("§bPrice for §3%s", priceQuery.itemName)));
        }
    }

    private static void handlePrice(double value, int amount) {
        if (value > 0) {
            double totalValue = value * amount;
            if (amount > 1) {
                FishyNotis.alert(Component.literal(String.format("§7§e%s §7each", formatCoins(value))));
                FishyNotis.alert(Component.literal(String.format("§7Total: §e%s", formatCoins(totalValue))));
            } else {
                FishyNotis.alert(Component.literal(String.format("§7§e%s", formatCoins(value))));
            }
        } else {
            FishyNotis.alert(Component.literal("§cNo price data found"));
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
            FishyNotis.alert(Component.literal("§cTracker display not initialized"));
            return false;
        }
        
        var excludedItems = tracker.getExcludedItemsForDisplay();
        
        if (excludedItems.isEmpty()) {
            FishyNotis.alert(Component.literal("§7No items are currently ignored"));
            return true;
        }
        
        FishyNotis.send(Component.literal("§b§lIgnored Items:"));
        
        for (var itemName : excludedItems) {
            var displayName = capitalizeItemName(itemName);
            var clickableItem = Component.literal("§8      - §f" + displayName + " §a[⟳]")
                    .withStyle(style -> style.withClickEvent(new net.minecraft.network.chat.ClickEvent.RunCommand("/fp restore \"" + itemName + "\"")));
            FishyNotis.alert(clickableItem);
        }

        var restoreAllText = Component.literal("§         [Restore All Items]").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE)
                .withStyle(style -> style.withClickEvent(new net.minecraft.network.chat.ClickEvent.RunCommand("/fp restore all")));
        FishyNotis.alert(restoreAllText);
        
        return true;
    }
    
    private static boolean handleRestore(String[] args) {
        var cmdArgs = new CommandBuilderUtils.CommandArgs(args);
        var tracker = ProfitDisplay.getInstance();
        
        if (tracker == null) {
            FishyNotis.alert(Component.literal("§cTracker display not initialized"));
            return false;
        }
        
        if (!cmdArgs.has(1)) {
            FishyNotis.alert(Component.literal("§cUsage: /fp restore <item_name> or /fp restore all"));
            return false;
        }
        
        var restoreTarget = cmdArgs.get(1);
        
        if (ALL.equals(restoreTarget)) {
            tracker.restoreAllExcludedItems();
            return true;
        }
        
        if (restoreTarget.startsWith("\"") && restoreTarget.endsWith("\"")) {
            restoreTarget = restoreTarget.substring(1, restoreTarget.length() - 1);
        }
        
        if (!tracker.getExcludedItemsForDisplay().contains(restoreTarget)) {
            var displayName = capitalizeItemName(restoreTarget);
            FishyNotis.alert(Component.literal("§c" + displayName + " is not in the ignored list"));
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
            FishyNotis.alert(Component.literal(String.format("§7Per hour: §e%s", formatCoins(profitPerHour))));
        }
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> literal(String name) {
        return ClientCommands.literal(name);
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
