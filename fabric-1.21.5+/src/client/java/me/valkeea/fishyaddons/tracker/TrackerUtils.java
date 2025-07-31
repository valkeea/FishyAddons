package me.valkeea.fishyaddons.tracker;

import net.minecraft.text.Style;
import net.minecraft.text.Text;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.util.HelpUtil;

public class TrackerUtils {
    private TrackerUtils() {}
    private static boolean pricePerItem = false;
    private static boolean enabled = false;

    public static boolean isOn() {  return pricePerItem; }
    public static boolean isEnabled() { return enabled; }

    public static void refresh() {
        pricePerItem = me.valkeea.fishyaddons.config.FishyConfig.getState(Key.PER_ITEM, false);
        enabled = me.valkeea.fishyaddons.config.FishyConfig.getState(Key.HUD_TRACKER_ENABLED, true);
    }

    public static void setPricePerItem(boolean state) {
        pricePerItem = state;
        me.valkeea.fishyaddons.config.FishyConfig.toggle(Key.PER_ITEM, state);
    }

    public static void handleChat(String message) {
        if (!me.valkeea.fishyaddons.util.SkyblockCheck.getInstance().rules() ||
            !enabled) return;
        String s = HelpUtil.stripColor(message);
        if (s.startsWith("[") || s.startsWith("Guild") || 
            s.startsWith("Party")) {
            return;
        }
        
        if (s.toLowerCase().contains("loot share")) {
            InventoryTracker.onLsDetected();
        }
        
        ChatDropParser.ParseResult result = ChatDropParser.parseMessage(message);
        if (result != null) {
            SackDropParser.registerChatDrop(result.itemName, result.quantity);
            
            if (result.isCoinDrop) {
                ItemTrackerData.addCoins(result.quantity);
            } else {
                if (result.itemName.toLowerCase().contains("enchanted book")) {
                    InventoryTracker.onEnchantedBookDropDetected(result.quantity, result.magicFind);
                } else {
                    ItemTrackerData.addDrop(result.itemName, result.quantity);
                }
            }
        }
    }    
    
    public static void handleSackDrop(ChatDropParser.ParseResult drop) {
        if (!me.valkeea.fishyaddons.util.SkyblockCheck.getInstance().rules()) return;
        
        if (drop != null) {
            ItemTrackerData.addDrop(drop.itemName, drop.quantity);
        }
    }

    public static void checkForHoverEvents(Text message) {
        boolean sackTrackingEnabled = SackDropParser.isOn();
        if (sackTrackingEnabled) {
            String fullMessageText = reconstruct(message);

            if (SackDropParser.isSackNotification(fullMessageText)) {
                handleSackNotification(message);
                return;
            }
        }
        
        for (Text sibling : message.getSiblings()) {
            checkForHoverEvents(sibling);
        }
    }
    
    private static String reconstruct(Text message) {
        StringBuilder fullText = new StringBuilder();
        fullText.append(message.getString());
        
        for (Text sibling : message.getSiblings()) {
            fullText.append(reconstruct(sibling));
        }
        
        return fullText.toString();
    }
    
    private static void handleSackNotification(Text message) {
        if (processSackHover(message.getStyle())) {
            return;
        }
        for (Text sibling : message.getSiblings()) {
            if (processSackHover(sibling.getStyle())) {
                return;
            }
        }
    }
    
    private static boolean processSackHover(Style style) {
        if (style != null && style.getHoverEvent() != null) {
            var sackDrops = SackDropParser.parseSackHoverEvent(style.getHoverEvent());

            for (var drop : sackDrops) {
                TrackerUtils.handleSackDrop(drop);
            }
            return true;
        }
        return false;
    }    
}