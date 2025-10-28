package me.valkeea.fishyaddons.tracker;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.config.TrackerProfiles;
import me.valkeea.fishyaddons.event.EventPhase;
import me.valkeea.fishyaddons.event.EventPriority;
import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.event.impl.ScreenClickEvent;
import me.valkeea.fishyaddons.hud.TrackerDisplay;
import me.valkeea.fishyaddons.tracker.fishing.ScStats;
import me.valkeea.fishyaddons.ui.VCOverlay;
import me.valkeea.fishyaddons.ui.VCPopup;
import me.valkeea.fishyaddons.util.FishyNotis;
import me.valkeea.fishyaddons.util.text.FromText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class TrackerUtils {
    private static boolean pricePerItem = false;
    private static boolean enabled = false;

    public static boolean isOn() {  return pricePerItem; }
    public static boolean isEnabled() { return enabled; }

    public static void init() {
        refresh();

        FaEvents.SCREEN_MOUSE_CLICK.register(event -> {

        var profitTracker = TrackerDisplay.getInstance();
            if (profitTracker != null && profitTracker.handleMouseClick(event.mouseX, event.mouseY, event.button)) {
                event.setResult(true);
                event.setConsumed(true);
            }
        }, EventPriority.HIGH, EventPhase.PRE);
    }

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