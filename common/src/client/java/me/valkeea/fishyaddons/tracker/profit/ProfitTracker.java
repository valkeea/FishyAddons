package me.valkeea.fishyaddons.tracker.profit;

import me.valkeea.fishyaddons.config.TrackerProfiles;
import me.valkeea.fishyaddons.tracker.collection.CollectionTracker;
import me.valkeea.fishyaddons.ui.VCOverlay;
import me.valkeea.fishyaddons.ui.VCPopup;
import me.valkeea.fishyaddons.util.FishyNotis;
import me.valkeea.fishyaddons.util.text.FromText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class ProfitTracker {
    private static boolean enabled = false;
    private static boolean trackSackProfit = false;
    private static boolean pricePerItem = false;

    public static boolean pricePerItem() {  return pricePerItem; }
    public static boolean isEnabled() { return enabled; }

    public static void setConfig(boolean generalTracking, boolean sackTracking, boolean perItem) {
        enabled = generalTracking;
        trackSackProfit = sackTracking;
        pricePerItem = perItem;
    }

    public static boolean handleChat(String s, Text t) { 

        if (s.startsWith("loot share")) {
            InventoryTracker.onLsDetected();
            return true;
        }
        
        var result = ChatDropParser.parseMessage(s);
        
        if (result != null) {
            SackDropParser.registerChatDrop(result.itemName, result.quantity);
            if (result.coinDrop()) TrackedItemData.addCoins(result.quantity);
            else countImmediate(result, t);
            return true;
        }

        return false;
    }

    private static void countImmediate(ChatDropParser.ParseResult r, Text originalMessage) {
        var msg = r.bookDrop() ? originalMessage : null;
        TrackedItemData.registerPendingDrop(r.itemName, r.quantity);
        TrackedItemData.addDrop(r.itemName, r.quantity, msg);
    }

    public static boolean checkForHoverEvents(Text message) {
        if (!trackSackProfit)  return false;
        Text hoverEvent = FromText.findShowText(message);
        return hoverEvent != null && processSackHover(hoverEvent);
    }
    
    private static boolean processSackHover(Text hoverText) {
        var sackDrops = SackDropParser.parseSackHoverEvent(hoverText.getString());
        if (sackDrops.isEmpty()) return false;
        for (var drop : sackDrops) handleSackDrop(drop);
        return true;
    }

    private static void handleSackDrop(ChatDropParser.ParseResult drop) {
        if (drop != null) {
            if (trackSackProfit) TrackedItemData.addDrop(drop.itemName, drop.quantity);
            CollectionTracker.addDrop(drop.itemName, drop.quantity);
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
