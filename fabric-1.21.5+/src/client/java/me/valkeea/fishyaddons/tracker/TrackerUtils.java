package me.valkeea.fishyaddons.tracker;

import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.config.TrackerProfiles;
import me.valkeea.fishyaddons.gui.VCOverlay;
import me.valkeea.fishyaddons.gui.VCPopup;
import me.valkeea.fishyaddons.util.FishyNotis;
import me.valkeea.fishyaddons.util.HelpUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public class TrackerUtils {
    private static boolean pricePerItem = false;
    private static boolean bookEnabled = false;
    private static boolean enabled = false;

    public static boolean isOn() {  return pricePerItem; }
    public static boolean bookEnabled() { return bookEnabled; }
    public static boolean isEnabled() { return enabled; }

    public static void refresh() {
        pricePerItem = me.valkeea.fishyaddons.config.FishyConfig.getState(Key.PER_ITEM, false);
        bookEnabled = me.valkeea.fishyaddons.config.FishyConfig.getState(Key.BOOK_DROP_ALERT, false);
        enabled = me.valkeea.fishyaddons.config.FishyConfig.getState(Key.HUD_TRACKER_ENABLED, false);
    }

    public static void setPricePerItem(boolean state) {
        pricePerItem = state;
        me.valkeea.fishyaddons.config.FishyConfig.toggle(Key.PER_ITEM, state);
    }

    public static void handleChat(String message) {
        if (!me.valkeea.fishyaddons.util.SkyblockCheck.getInstance().rules()) return;
        
        String s = HelpUtil.stripColor(message);
        if (s.startsWith("[") || s.startsWith("Guild") || 
            s.startsWith("Party")) {
            return;
        }
                
        ChatDropParser.ParseResult result = ChatDropParser.parseMessage(message);
        if (!enabled && bookEnabled) {
            if (result != null && !result.isCoinDrop && 
                result.itemName.toLowerCase().contains("enchanted book")) {
                InventoryTracker.onEnchantedBookDropDetected(result.quantity, result.magicFind);
            }
            return;
        }
        
        if (s.toLowerCase().contains("loot share")) {
            InventoryTracker.onLsDetected();
            return;
        }
        
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
        if (drop != null) {
            ItemTrackerData.addDrop(drop.itemName, drop.quantity);
        }
    }

    public static void checkForHoverEvents(Text message) {
        if (!me.valkeea.fishyaddons.util.SkyblockCheck.getInstance().rules() ||
            !enabled) return;

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

    public static void onDelete(String profile) {
        FishyNotis.send(Text.literal("§cDeleted profile: " + profile));
        if (profile.equals(TrackerProfiles.getCurrentProfile())) {
            TrackerProfiles.setCurrentProfile("default");
            FishyNotis.notice("Switched to default profile");
        }
    }

    public static void save() {
        if (TrackerProfiles.getCurrentProfile().equals("default")) {
            createOrSavePopup(1.0f);
        } else {
            TrackerProfiles.saveToJson();
            FishyNotis.notice("Saved tracker data to file");
        }
    }

	public static void createOrSavePopup(float scale) {
        VCPopup popup = new VCPopup(
			Text.literal("Profile name:"),
            () -> MinecraftClient.getInstance().setScreen(null),
			"Cancel",
			profileName -> {
                TrackerProfiles.saveOrCreate(profileName);
                MinecraftClient.getInstance().setScreen(null);
            },
			"Save",
			scale
		);
        MinecraftClient cl = MinecraftClient.getInstance();
        cl.setScreen(new VCOverlay(cl.currentScreen, popup));
	}

    public static void trackerNoti(String itemName) {
        if (me.valkeea.fishyaddons.config.FishyConfig.getState(Key.TRACKER_NOTIS, false)) {
            me.valkeea.fishyaddons.util.FishyNotis.send("Tracker drop: " + itemName);
        }
    }
    
    private TrackerUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
}