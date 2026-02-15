package me.valkeea.fishyaddons.tracker.profit;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.config.TrackerProfiles;
import me.valkeea.fishyaddons.ui.VCOverlay;
import me.valkeea.fishyaddons.ui.VCPopup;
import me.valkeea.fishyaddons.util.FishyNotis;
import me.valkeea.fishyaddons.util.text.FromText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class ProfitTracker {
    private static boolean pricePerItem = false;
    private static boolean enabled = false;

    public static boolean isOn() {  return pricePerItem; }
    public static boolean isEnabled() { return enabled; }

    public static void init() {
        refresh();
    }

    public static void refresh() {
        pricePerItem = FishyConfig.getState(Key.PER_ITEM, false);
        enabled = FishyConfig.getState(Key.HUD_TRACKER_ENABLED, false);
    }

    public static void setPricePerItem(boolean state) {
        pricePerItem = state;
        FishyConfig.toggle(Key.PER_ITEM, state);
    }

    public static boolean handleChat(String s, Text t) { 
        ChatDropParser.ParseResult result = ChatDropParser.parseMessage(s);

        if (s.startsWith("loot share")) {
            InventoryTracker.onLsDetected();
            return true;
        }
        
        if (result != null) {

            SackDropParser.registerChatDrop(result.itemName, result.quantity);
            if (result.isCoinDrop) ItemTrackerData.addCoins(result.quantity);
            else countImmediate(result, t);
            return true;
        }

        return false;
    }

    private static void countImmediate(ChatDropParser.ParseResult result, Text originalMessage) {
        boolean isBook = result.itemName.contains("enchanted book");
        ItemTrackerData.registerPendingDrop(result.itemName, result.quantity);
        
        if (isBook) {
            ItemTrackerData.addDrop(result.itemName, result.quantity, originalMessage);
        } else {
            ItemTrackerData.addDrop(result.itemName, result.quantity);
        }
    }

    public static boolean checkForHoverEvents(Text message) {
        if (!SackDropParser.isOn())  return false;
        Text hoverEvent = FromText.findShowText(message);
        return hoverEvent != null && processSackHover(hoverEvent);
    }
    
    private static boolean processSackHover(Text hoverText) {
            var sackDrops = SackDropParser.parseSackHoverEvent(hoverText.getString());

            if (sackDrops.isEmpty()) {
                return false;
            }

            for (var drop : sackDrops) {
                handleSackDrop(drop);
            }
            return true;
    }

    private static void handleSackDrop(ChatDropParser.ParseResult drop) {
        if (drop != null) {
            ItemTrackerData.addDrop(drop.itemName, drop.quantity);
        }
    }

    // --- Profile management ---

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
        var popup = new VCPopup(
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
        var cl = MinecraftClient.getInstance();
        cl.setScreen(new VCOverlay(cl.currentScreen, popup));
	}
    
    private ProfitTracker() {
        throw new UnsupportedOperationException("Utility class");
    }
}
