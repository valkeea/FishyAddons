package me.valkeea.fishyaddons.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.gui.VCScreen.ExtraControl;
import me.valkeea.fishyaddons.handler.CopyChat;
import me.valkeea.fishyaddons.handler.ItemSearchOverlay;
import me.valkeea.fishyaddons.handler.MobAnimations;
import me.valkeea.fishyaddons.handler.ParticleVisuals;
import me.valkeea.fishyaddons.handler.PetInfo;
import me.valkeea.fishyaddons.handler.RenderTweaks;
import me.valkeea.fishyaddons.handler.ResourceHandler;
import me.valkeea.fishyaddons.handler.SkyblockCleaner;
import me.valkeea.fishyaddons.handler.WeatherTracker;
import me.valkeea.fishyaddons.handler.XpColor;
import me.valkeea.fishyaddons.hud.EqDisplay;
import me.valkeea.fishyaddons.hud.TrackerDisplay;
import me.valkeea.fishyaddons.tool.FishyMode;
import me.valkeea.fishyaddons.tracker.SackDropParser;
import me.valkeea.fishyaddons.tracker.TrackerUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * Manages all configuration entries for VCScreen.
 */
public class VCManager {
    private static final String CONFIGURE_LABEL = "Configure";

    /**
     * Creates and returns the complete list of configuration entries.
     */
    public static List<VCEntry> createAllEntries() {
        List<VCEntry> entries = new ArrayList<>();
        
        addUi(entries);
        addQol(entries);
        addRender(entries);
        addSb(entries);
        addFg(entries);
        
        return entries;
    }
    
    /**
     * Interface and Mod visuals
     */
    private static void addUi(List<VCEntry> entries) {
        entries.add(VCEntry.header("── Interface ──", ""));

        entries.add(VCEntry.slider(
            "Custom UI Scale",
            "Adjust mod scaling manually (Also influenced by vanilla settings).",
            Key.MOD_UI_SCALE,
            0.0f,
            1.3f,
            "%.1fx",
            value -> {
                FishyConfig.setFloat(Key.MOD_UI_SCALE, value);
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.currentScreen instanceof VCScreen currentScreen) {
                    VCState.preserveState(
                        currentScreen.getScrollOffset(),
                        currentScreen.getLastSearchText(),
                        currentScreen.getExpandedEntries()
                    );
                }
                client.setScreen(new VCScreen());
            }
        )); 

        entries.add(VCEntry.typedSlider(
            "Mod Theme",
            "Choose the visual theme for FishyAddons.\nThis affects colors and styling throughout the mod.",
            "themeSlider",
            0.0f,
            4.0f,
            "%.0f",
            value -> {
                String[] themes = {"default", "purple", "blue", "white", "green"};
                int themeIndex = Math.round(value);
                if (themeIndex >= 0 && themeIndex < themes.length) {
                    FishyMode.setTheme(themes[themeIndex]);
                }
            },
            VCEntry.SliderType.STRING
        ));

        entries.add(VCEntry.toggle(
            "Transparent Minecraft GUI",
            "Uses textures from ValksfullSbPack (WIP), currently being ported from 1.8.9.",
            Key.FISHY_GUI,
            false,
            ResourceHandler::updateGuiPack
        ));

        entries.add(VCEntry.toggle(
            "HD Font",
            "Replaces the default font with a high-definition one\nfrom ValksfullSbPack.",
            Key.HD_FONT,
            false,
            ResourceHandler::updateFontPack
        ));
    }

    // General QoL Features
    private static void addQol(List<VCEntry> entries) {
        entries.add(VCEntry.header("── General QoL ──", ""));

        List<VCEntry> invSearchSubEntries = new ArrayList<>();

        invSearchSubEntries.add(VCEntry.toggleColorOrHud(
            "Main Toggle",
            "Enables a HUD textfield for inventory search.",
            Key.INV_SEARCH,
            false,
            null,
            new ExtraControl("Item Search", false)
        ));

        invSearchSubEntries.add(VCEntry.slider(
            "Overlay Opacity",
            "Adjust the darkness of the search overlay when highlighting items.",
            Key.INV_SEARCH_OPACITY,
            0.0f,
            1.0f,
            "%.0f%%",
            opacity -> ItemSearchOverlay.getInstance().setOpacity(opacity)
        ));

        entries.add(VCEntry.expandable(
            "Inventory Search",
            "Scan your inventory items by name or lore.\nRight-click the search field to toggle search mode.",
            invSearchSubEntries,
            Arrays.asList(
                Text.literal("Inventory Search:"),
                Text.literal("- §8Toggle setting"),
                Text.literal("- §8Configure overlay opacity"),
                Text.literal("- §8Shortcut to HUD editor")
            )
        ));

        entries.add(VCEntry.toggleColorOrHud(
            "Highlight Coordinates",
            "Render a beacon-style highlight at coordinates shared in chat.\nUse /fa coords <title> to render/send additional text.\nSupports any coordinates in the format: x:<x>, y:<y>, z:<z>",
            Key.RENDER_COORDS,
            false,
            null,
            new ExtraControl(null, true)
        ));
        
        entries.add(VCEntry.toggleColorOrHud(
            "Ping Display",
            "Shows your current ping in the HUD.\nUse the HUD editor to customize position and appearance.\nUse /fa ping to send this value in chat.",
            Key.HUD_PING_ENABLED,
            false,
            null,
            new ExtraControl("Ping Display", false)
        ));
        
        entries.add(VCEntry.toggle2(
            "Copy Chat",
            "Right-click chat messages to copy them to clipboard.\nHold shift to copy just the hovered line.",
            Key.COPY_CHAT,
            false,
            "NOTI",
            Key.COPY_NOTI,
            false,
            CopyChat::refresh
        ));
        
        entries.add(VCEntry.toggle(
            "Skip Front Perspective",
            "Skip the front-facing perspective when cycling camera views (F5).\nGoes directly from first person to back view.",
            Key.SKIP_F5,
            false,
            null
        ));

        entries.add(VCEntry.redirect(
            "Custom Keybinds",
            "Configure custom keybinds for commands.",
            CONFIGURE_LABEL,
            TabbedListScreen::keyTab
        ));

        entries.add(VCEntry.redirect(
            "Custom Commands",
            "Configure custom commands.",
            CONFIGURE_LABEL,
            TabbedListScreen::cmdTab
        ));

        entries.add(VCEntry.redirect(
            "Chat Replacements",
            "Configure chat message replacements.",
            CONFIGURE_LABEL,
            TabbedListScreen::chatTab
        ));

        entries.add(VCEntry.redirect(
            "Chat Alerts",
            "Custom alerts with optional title, sound and autochat on\nchat events. Be specific to prevent undesired matches.",
            CONFIGURE_LABEL,
            TabbedListScreen::alertTab
        ));

    }
    
    /**
     * Rendering Tweaks
     */
    private static void addRender(List<VCEntry> entries) {
        entries.add(VCEntry.header("── Rendering Tweaks ──", ""));

        entries.add(VCEntry.toggle(
            "Skip Death Animation",
            "Prevent death animation from rendering on entities.\nUseful for reducing visual clutter caused by already-dead entities.",
            Key.DEATH_ANI,
            false,
            MobAnimations::refresh
        ));
        
        entries.add(VCEntry.toggle(
            "Skip Fire Animation",
            "Prevent fire from rendering on entities.\nThis will not remove the FOV effect.",
            Key.FIRE_ANI,
            false,
            MobAnimations::refresh
        ));

        entries.add(VCEntry.toggle(
            "Clear Lava",
            "Removes the red fog overlay when submerged in lava.\nDisabled outside Skyblock.",
            Key.FISHY_LAVA,
            false,
            RenderTweaks::refresh
        ));

        entries.add(VCEntry.toggle(
            "Clear Water",
            "Removes underwater fog and improves visibility,\ndisabled outside Skyblock.",
            Key.FISHY_WATER,
            false,
            RenderTweaks::refresh
        ));  
        
        entries.add(VCEntry.toggle2(
            "XP Text Color",
            "Customize the appearance of vanilla experience display by\naltering the color and adding an outline.",
            Key.XP_COLOR_ON,
            false,
            "OUTLINE",
            Key.XP_OUTLINE,
            false,
            XpColor::refresh
        ));

        entries.add(VCEntry.typedSliderColor(
            "Redstone Particle Color",
            "Customize redstone particle colors. Use the slider to select presets\n(Off/Aqua/Mint/Pink/Prism) or click the color square for custom colors.",
            Key.CUSTOM_PARTICLE_COLOR_INDEX,
            0.0f,
            4.0f,
            "%.0f",
            value -> {
                int index = Math.round(value);
                FishyConfig.setCustomParticleColorIndex(index);
                if ("custom".equals(FishyConfig.getParticleColorMode())) {
                    FishyConfig.setParticleColorMode("preset");
                }
                ParticleVisuals.refreshCache();
            },
            VCEntry.SliderType.PRESET,
            true
        ));
    }
    
    /**
     * SkyBlock features
     */
    private static void addSb(List<VCEntry> entries) {
        entries.add(VCEntry.header("── SkyBlock Features ──", ""));

        List<VCEntry> petSubEntries = new ArrayList<>();
        List<VCEntry> profitEntries = new ArrayList<>();
        List<VCEntry> trackerEntries = new ArrayList<>(); 

        entries.add(VCEntry.toggle(
            "Equipment Display",
            "Renders a secondary armor display with your current equipment.\nData updates when /eq is opened.",
            Key.EQ_DISPLAY,
            false,
            EqDisplay::reset
        ));

        petSubEntries.add(VCEntry.toggleColorOrHud(
            "Dynamic",
            "Updates data on tab updates, limited to onece per second.\nIf disabled, data will rely on chat.",
            Key.HUD_PET_DYNAMIC,
            false,
            PetInfo::refresh,
            new ExtraControl("Pet Display", false)
        ));

        petSubEntries.add(VCEntry.toggle(
            "Show XP",
            "Displays the XP of your active pet.",
            Key.HUD_PETXP,
            false,
            PetInfo::refresh
        ));

        entries.add(VCEntry.toggleExpandable(
            "Pet Display",
            "Renders an editable HUD element for your currently active pet.",
            petSubEntries,
            Arrays.asList(
                Text.literal("Pet Display:"),
                Text.literal("- §8Show/Hide pet XP"),
                Text.literal("- §8Toggle dynamic/chat-based mode"),
                Text.literal("- §8Shortcut to HUD editor")
            ),
            Key.HUD_PET_ENABLED,
            false,
            PetInfo::refresh
        ));

        profitEntries.add(VCEntry.toggle(
            "Price per Item",
            "Adds additional price data to the display.",
            Key.PER_ITEM,
            false,
            TrackerUtils::refresh
        ));

        profitEntries.add(VCEntry.toggle(
            "Track Sack Drops",
            "Requires sack notifications on to function.",
            Key.TRACK_SACK,
            false,
            SackDropParser::refresh
        ));

        profitEntries.add(VCEntry.slider(
            "Filter by Minimum Value",
            "Hides items under this value, set to 0 to disable.",
            Key.FILTER_MIN_VALUE,
            0,
            100000,
            "%.0f",
            value -> {
                FishyConfig.setFloat(Key.FILTER_MIN_VALUE, value);
                FishyConfig.enable(Key.VALUE_FILTER, value > 0.0f);
                TrackerDisplay.refreshDisplay();
            }
        ));
    
        profitEntries.add(VCEntry.toggle(
            "Book Drop Message",
            "Displays a message with the book title when a book is dropped.",
            Key.BOOK_DROP_ALERT,
            false,
            null
        ));

        entries.add(VCEntry.toggleExpandable(
            "Profit Tracker (WIP)",
            "Configure settings for the Profit Tracker. Use /fp for available commands.\nTIP: Right- click an entry in the display to hide it.",
            profitEntries,
            Arrays.asList(
                Text.literal("Profit Tracker:"),
                Text.literal("- §8Toggle book drop message"),
                Text.literal("- §8Configure Profit Tracker:"),
                Text.literal("  §8- Minimum value filter"),
                Text.literal("  §8- Set profit display options"),
                Text.literal("- §8- /fp toggle, /fp help")
            ),
            Key.HUD_TRACKER_ENABLED,
            false,
            TrackerUtils::refresh
        ));

        trackerEntries.add(VCEntry.header("── Century Cake Tracker ──", ""));

        trackerEntries.add(VCEntry.toggle(
            "Enable Notifications",
            "Notifies the user when a century cake is about to expire or expires.",
            Key.CAKE_NOTI,
            false,
            null
        ));

        trackerEntries.add(VCEntry.toggleColorOrHud(
            "Century Cake Display",
            "Enables a HUD timer.",
            Key.HUD_CENTURY_CAKE_ENABLED,
            false,
            TrackerUtils::refresh,
            new ExtraControl("Century Cakes: ", false)
        ));        

        trackerEntries.add(VCEntry.header("── Rain Tracker ──", ""));

        trackerEntries.add(VCEntry.toggle(
            "Rain Tracker",
            "Tracks the worlds weather state and notifies if rain stops.\nDue to Hypixel quirks this will not work reliably in the Park cave.",
            Key.RAIN_NOTI,
            false,
            WeatherTracker::track
        ));

        trackerEntries.add(VCEntry.header("── Moonglade Minigame Alarm ──", ""));

        trackerEntries.add(VCEntry.toggleColorOrHud(
            "In-game Alarm and timer display",
            "Adds a HUD timer if the minigame is on cooldown.",
            Key.BEACON_ALARM,
            false,
            null,
            new ExtraControl("Moonglade: ", false)
        ));        

        entries.add(VCEntry.expandable(
            "Trackers and alerts",
            "Configure settings for Century Cake Display, Rain Tracker\nand Moonglade Minigame alarm.",
            trackerEntries,
            Arrays.asList(
                Text.literal("Trackers and alerts:"),
                Text.literal("- §8Toggle century cake chat alert/display"),
                Text.literal("- §8Toggle Moonglade Alarm"),
                Text.literal("- §8Toggle rain warning"),                
                Text.literal("  §8- Or use /fa rain on | off")
            )
        ));

        entries.add(VCEntry.toggle(
            "Clean Wither Impact",
            "Removes explosion particles and any sound effects caused by\nwither blade abilities completely.",
            Key.CLEAN_HYPE,
            false,
            SkyblockCleaner::refresh
        ));        

        entries.add(VCEntry.toggle(
            "Mute Phantoms",
            "Disables all phantom mob sounds.",
            Key.MUTE_PHANTOM,
            false,
            SkyblockCleaner::refresh
        ));

        entries.add(VCEntry.toggle(
            "Mute Runes",
            "Disables all annoying rune sounds.",
            Key.MUTE_RUNE,
            false,
            SkyblockCleaner::refresh
        ));

        entries.add(VCEntry.toggleSlider(
            "Hide Hotspot Holograms",
            "Hides all fishing hotspot armor stands if they are\nin the configured distance.",
            Key.HIDE_HOTSPOT,
            false,
            Key.HOTSPOT_DISTANCE,
            0.0f,
            100.0f,
            "%.0f blocks",
            SkyblockCleaner::refresh
        ));

        entries.add(VCEntry.toggleSlider(
            "Invisibug Helper",
            "Scales the particles emitted by Invisibugs (crit) while on Galatea.",
            Key.SCALE_CRIT,
            false,
            Key.DMG_SCALE,
            0.05f,
            1.5f,
            "%.2f",
            ParticleVisuals::refreshCache
        ));
    }

    private static void addFg(List<VCEntry> entries) {
        entries.add(VCEntry.header("── Item Safeguard ──", ""));

        List<VCEntry> itemManagementSubEntries = new ArrayList<>();
        
        itemManagementSubEntries.add(VCEntry.icToggle(
            "Sell Protection",
            "Main toggle for protecting items in blacklisted GUIs.\n(Auction, salvaging, trade menu, npc shops)",
            Key.SELL_PROTECTION_ENABLED,
            false
        ));
        
        itemManagementSubEntries.add(VCEntry.icToggle(
            "Safeguard Tooltips",
            "Display a tag at the end of a protected item's tooltip.",
            Key.TOOLTIP_ENABLED,
            false
        ));

        itemManagementSubEntries.add(VCEntry.icToggle(
            "Protection Notification",
            "Show chat notifications when safeguard features are triggered.",
            Key.PROTECT_NOTI_ENABLED,
            false
        ));
        
        itemManagementSubEntries.add(VCEntry.icToggle(
            "Protection Alert",
            "Give auditory feedback when attempting to drop or sell protected items.",
            Key.PROTECT_TRIGGER_ENABLED,
            false
        ));
        

        itemManagementSubEntries.add(VCEntry.header(
            "── Manage Blocked GUIs ──",
            ""
        ));
        
        itemManagementSubEntries.add(VCEntry.blToggle(
            "Block Auction House",
            "Prevent interactions in auctions.",
            "Create Auction",
            true
        ));

        itemManagementSubEntries.add(VCEntry.blToggle(
            "Block Player Trades",
            "Prevent trading guarded items.",
            "Coins Transaction",
            true
        ));
        
        itemManagementSubEntries.add(VCEntry.blToggle(
            "Block Salvaging",
            "Prevent interactions with item salvaging interfaces.",
            "Salvage Items",
            true
        ));

        itemManagementSubEntries.add(VCEntry.blToggle(
            "Block NPC Sales",
            "Prevent interactions with NPC shops.",
            "Sell Item",
            true
        ));
        
        entries.add(VCEntry.expandable(
            "Item Safeguard",
            "Protect Skyblock items from drops and undesired clicks.\nExpand to configure advanced settings",
            itemManagementSubEntries,
            Arrays.asList(
                Text.literal("Item Safeguard:"),
                Text.literal("- §8Manage blocked guis"),
                Text.literal("- §8Toggle settings for visual and auditory feedback"),
                Text.literal("- §8/fg add to add items, /fg help to see all commands")
            )
        ));

        entries.add(VCEntry.keybind(
            "Bind and Lock Slots ",
            "Set the key to lock (tap) and bind (drag) slots.\nLeft-click again to remove key.",
            Key.MOD_KEY_LOCK,
            false
        ));


        // to-do: fix key+btn
    }

    /**
     * Filters the entry list based on search text
     */
    public static List<VCEntry> filterEntries(List<VCEntry> allEntries, String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            return new ArrayList<>(allEntries);
        }
        
        List<VCEntry> filtered = new ArrayList<>();
        String lowerSearch = searchText.toLowerCase().trim();
        
        for (VCEntry entry : allEntries) {
            if (matchesSearch(entry, lowerSearch)) {
                filtered.add(entry);
            }
        }
        
        return filtered;
    }
    
    private static boolean matchesSearch(VCEntry entry, String search) {
        if (entry.type == VCEntry.EntryType.HEADER) {
            return false;
        }
        
        String name = entry.name.startsWith("  ") ? entry.name.substring(2) : entry.name;
        if (name.toLowerCase().contains(search)) {
            return true;
        }

        return entry.description != null && entry.description.toLowerCase().contains(search);
    }
    
    public static boolean needsHudButton(VCEntry entry) {
        return entry.hudElementName != null;
    }
    
    public static boolean needsColorButton(VCEntry entry) {
        return entry.hasColorControl;
    }

    private VCManager() {
        throw new UnsupportedOperationException("Utility class");
    }    
}
