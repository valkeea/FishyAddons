package me.valkeea.fishyaddons.tracker;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.config.TrackerProfiles;
import me.valkeea.fishyaddons.tracker.fishing.ScStats;
import me.valkeea.fishyaddons.ui.VCOverlay;
import me.valkeea.fishyaddons.ui.VCPopup;
import me.valkeea.fishyaddons.util.FishyNotis;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public class TrackerUtils {
    private static boolean pricePerItem = false;
    private static boolean enabled = false;

    public static boolean isOn() {  return pricePerItem; }
    public static boolean isEnabled() { return enabled; }

    public static void refresh() {
        pricePerItem = FishyConfig.getState(Key.PER_ITEM, false);
        enabled = FishyConfig.getState(Key.HUD_TRACKER_ENABLED, false);
    }

    public static void setPricePerItem(boolean state) {
        pricePerItem = state;
        FishyConfig.toggle(Key.PER_ITEM, state);
    }

    public static boolean handleChat(String s, Text originalMessage) { 
        ChatDropParser.ParseResult result = ChatDropParser.parseMessage(s);

        ScStats.getInstance().checkForVial(s);

        if (s.startsWith("loot share")) {
            InventoryTracker.onLsDetected();
            return true;
        }

        if (s.startsWith("[Bazaar] Bought")) {
            SackDropParser.onBazaarBuy(s);
            return true;
        }
        
        if (result != null) {
            SackDropParser.registerChatDrop(result.itemName, result.quantity);

            if (result.isCoinDrop) {
                ItemTrackerData.addCoins(result.quantity);
            } else {
                if (s.contains("enchanted book")) {
                    ItemTrackerData.addDrop(result.itemName, result.quantity, originalMessage);
                } else {
                    ItemTrackerData.addDrop(result.itemName, result.quantity);
                }
            }
            return true;
        }
        return false;
    }    
    
    private static void handleSackDrop(ChatDropParser.ParseResult drop) {
        if (drop != null) {
            ItemTrackerData.addDrop(drop.itemName, drop.quantity);
        }
    }

    public static boolean checkForHoverEvents(Text message) {

            boolean sackTrackingEnabled = SackDropParser.isOn();
            if (sackTrackingEnabled) {
                String fullMessageText = reconstruct(message);
                if (SackDropParser.isSackNotification(fullMessageText)) {
                    handleSackNotification(message);
                    return true;
                }
            }

            for (Text sibling : message.getSiblings()) {
                if (checkForHoverEvents(sibling)) {
                    return true;
                }
            }
            return false;
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
                handleSackDrop(drop);
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
    
    private TrackerUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
}