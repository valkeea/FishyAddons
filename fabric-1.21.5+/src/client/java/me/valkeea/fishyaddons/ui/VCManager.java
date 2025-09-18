package me.valkeea.fishyaddons.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.valkeea.fishyaddons.config.FilterConfig;
import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.handler.ActiveBeacons;
import me.valkeea.fishyaddons.handler.ChatAlert;
import me.valkeea.fishyaddons.handler.ChatReplacement;
import me.valkeea.fishyaddons.handler.ChatTimers;
import me.valkeea.fishyaddons.handler.ClientPing;
import me.valkeea.fishyaddons.handler.CommandAlias;
import me.valkeea.fishyaddons.handler.CopyChat;
import me.valkeea.fishyaddons.handler.FaColors;
import me.valkeea.fishyaddons.handler.GuiIcons;
import me.valkeea.fishyaddons.handler.FishingHotspot;
import me.valkeea.fishyaddons.handler.ItemSearchOverlay;
import me.valkeea.fishyaddons.handler.KeyShortcut;
import me.valkeea.fishyaddons.handler.MobAnimations;
import me.valkeea.fishyaddons.handler.ParticleVisuals;
import me.valkeea.fishyaddons.handler.PetInfo;
import me.valkeea.fishyaddons.handler.RenderTweaks;
import me.valkeea.fishyaddons.handler.ResourceHandler;
import me.valkeea.fishyaddons.handler.SkyblockCleaner;
import me.valkeea.fishyaddons.handler.TransLava;
import me.valkeea.fishyaddons.handler.WeatherTracker;
import me.valkeea.fishyaddons.handler.XpColor;
import me.valkeea.fishyaddons.hud.EqDisplay;
import me.valkeea.fishyaddons.hud.TrackerDisplay;
import me.valkeea.fishyaddons.tool.FishyMode;
import me.valkeea.fishyaddons.tracker.SackDropParser;
import me.valkeea.fishyaddons.tracker.TrackerUtils;
import me.valkeea.fishyaddons.ui.VCScreen.ExtraControl;
import me.valkeea.fishyaddons.util.text.GradientRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * Manages all configuration entries for VCScreen.
 */
public class VCManager {

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
        addFilter(entries);
        
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
            Key.THEME_MODE,
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
            () -> { 
                ResourceHandler.updateGuiPack();
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.currentScreen instanceof VCScreen currentScreen) {
                    VCState.preservePersistentState(
                        currentScreen.getScrollOffset(),
                        currentScreen.getLastSearchText(),
                        currentScreen.getExpandedEntries()
                    );
                }
                client.setScreen(new VCScreen());
            }
        ));

        entries.add(VCEntry.toggle(
            "HD Font",
            "Replaces the default font with a high-definition one\nfrom ValksfullSbPack.",
            Key.HD_FONT,
            false,
            () -> {
                ResourceHandler.updateFontPack();
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.currentScreen instanceof VCScreen currentScreen) {
                    VCState.preservePersistentState(
                        currentScreen.getScrollOffset(),
                        currentScreen.getLastSearchText(),
                        currentScreen.getExpandedEntries()
                    );
                }
                client.setScreen(new VCScreen());
            }
        ));
    }

    // General QoL Features
    private static void addQol(List<VCEntry> entries) {
        entries.add(VCEntry.header("── General QoL ──", ""));

        List<VCEntry> invSearchSubEntries = new ArrayList<>();
        List<VCEntry> beaconSubEntries = new ArrayList<>();

        invSearchSubEntries.add(VCEntry.toggleColorOrHud(
            "Main Toggle",
            "Enables a HUD textfield for inventory search.",
            Key.INV_SEARCH,
            false,
            null,
            new ExtraControl("Item Search", false, false)
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

        beaconSubEntries.add(VCEntry.sliderColor(
            "Duration and Color",
            "Beacons will be rendered for this duration with the configured color.",
            Key.RENDER_COORD_MS,
            10.0f,
            120.0f,
            "%.0fs",
            value -> {
                FishyConfig.setInt(Key.RENDER_COORD_MS, Math.round(value));
                ActiveBeacons.refresh();
            }
        ));

        beaconSubEntries.add(VCEntry.toggle(
            "Hide when close",
            "Hides the beacon when you are within 5 blocks of it.",
            Key.RENDER_COORD_HIDE_CLOSE,
            true,
            ActiveBeacons::refresh
        ));

        beaconSubEntries.add(VCEntry.toggle(
            "Enhanced Message",
            "Adds a clickable [Hide] and [Redraw] buttons to coordinate messages.",
            Key.CHAT_FILTER_COORDS_ENABLED,
            true,
            null
        ));

        entries.add(VCEntry.toggleExpandable(
            "Highlight Coordinates",
            "Render a beacon-style highlight at coordinates shared in chat.\nUse /fa coords <title> to render/send additional text.\nSupports any coordinates in the format: x:<x>, y:<y>, z:<z>",
            beaconSubEntries,
            Arrays.asList(
                Text.literal("Coordinate Beacons:"),
                Text.literal("- §8Set beacon duration and color"),
                Text.literal("- §8Toggle redraw and hide options"),
                Text.literal("- §8Hide when close")
            ),
            Key.RENDER_COORDS,
            true,
            null
        ));
        
        entries.add(VCEntry.toggleColorOrHud(
            "Ping Display",
            "Shows your current ping in the HUD.\nUse the HUD editor to customize position and appearance.\nUse /fa ping to send this value in chat.",
            Key.HUD_PING_ENABLED,
            false,
            ClientPing::refresh,
            new ExtraControl("Ping Display", false, false)
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

        entries.add(VCEntry.toggleColorOrHud(
            "Custom Keybinds",
            "Configure custom keybinds for commands.",
            Key.KEY_SHORTCUTS_ENABLED,
            false,
            KeyShortcut::refresh,
            new ExtraControl(null, false, true)
        ));

        entries.add(VCEntry.toggleColorOrHud(
            "Custom Commands",
            "Configure custom commands.",
            Key.ALIASES_ENABLED,
            false,
            CommandAlias::refresh,
            new ExtraControl(null, false, true)
        ));

        entries.add(VCEntry.toggleColorOrHud(
            "Chat Replacement",
            "Configure chat message replacements.",
            Key.CHAT_REPLACEMENTS_ENABLED,
            false,
            ChatReplacement::refresh,
            new ExtraControl(null, false, true)
        ));

        entries.add(VCEntry.toggleColorOrHud(
            "Chat Alerts",
            "Custom alerts with optional title, sound and autochat on\nchat events. Be specific to prevent undesired matches.",
            Key.CHAT_ALERTS_ENABLED,
            false,
            ChatAlert::refresh,
            new ExtraControl(null, false, true)
        ));            

    }
    
    /**
     * Rendering Tweaks
     */
    private static void addRender(List<VCEntry> entries) {
        entries.add(VCEntry.header("── Rendering Tweaks ──", ""));

        List<VCEntry> colorSubEntries = new ArrayList<>();
        List<VCEntry> xpSubEntries = new ArrayList<>();
        List<VCEntry> lavaSubEntries = new ArrayList<>();

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

        lavaSubEntries.add(VCEntry.toggleColorOrHud(
                "Translucent Lava",
                "Makes lava look translucent like water, with a custom tint (Crimson Isles only).",
                Key.FISHY_TRANS_LAVA,
                false,
                TransLava::update,
                new ExtraControl(null, true, false)
        ));        

        lavaSubEntries.add(VCEntry.toggle(
            "Clear Lava",
            "Removes any fog overlay when submerged in lava.\nDisabled outside Skyblock.",
            Key.FISHY_LAVA,
            false,
            RenderTweaks::refresh
        ));

        lavaSubEntries.add(VCEntry.toggle(
            "Fire FOV",
            "Uses a less intrusive texture for fire matching the configured lava color.",
            Key.FISHY_FIRE_OVERLAY,
            false,
            () -> { 
                ResourceHandler.updateGuiPack();
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.currentScreen instanceof VCScreen currentScreen) {
                    VCState.preservePersistentState(
                        currentScreen.getScrollOffset(),
                        currentScreen.getLastSearchText(),
                        currentScreen.getExpandedEntries()
                    );
                }
                RenderTweaks.refresh();
                client.setScreen(new VCScreen());
            }
        ));

        entries.add(VCEntry.expandable(
            "Lava Options",
            "Configure Translucent Lava, fog effects and fire overlay.",
            lavaSubEntries,
            Arrays.asList(
                Text.literal("Lava Options:"),
                Text.literal("- §8Translucent, water-like lava"),
                Text.literal("  §8with custom tint"),
                Text.literal("- §8Note: If the tint is off, check your"),
                Text.literal("  §8resource pack for colored water!"),
                Text.literal("- §8Remove lava fog overlay when submerged"),
                Text.literal("- §8Custom fire texture matching lava color")
            )
        ));

        entries.add(VCEntry.toggle(
            "Clear Water",
            "Removes underwater fog and improves visibility.\nDisabled outside Skyblock.",
            Key.FISHY_WATER,
            false,
            RenderTweaks::refresh
        ));

        xpSubEntries.add(VCEntry.toggleColorOrHud(
            "Color and Outline",
            "Set a color and toggle a bold outline.",
            Key.XP_OUTLINE,
            false,
            XpColor::refresh,
            new ExtraControl(null, true, false)
        ));

        entries.add(VCEntry.toggleExpandable(
            "XP Text Color",
            "Customize the appearance of vanilla experience display by\naltering the color and adding an outline.",
            xpSubEntries,
            Arrays.asList(
                Text.literal("XP Color:"),
                Text.literal("- §8Set the color and"),
                Text.literal("  §8optional outline")),
            Key.XP_COLOR_ON,
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

        colorSubEntries.add(VCEntry.toggleColorOrHud(
            "Custom Fa Colors",
            "Add your own entries (not shared with others).",
            Key.CUSTOM_FA_COLORS,
            false,
            FaColors::refresh,
            new ExtraControl(null, false, true)
        ));

        entries.add(VCEntry.toggleExpandable(
            "FA Colors",
            "Colors player names for all users.\nNote: Name label recoloring is incompatible with Skyhanni.",
            colorSubEntries,
            Arrays.asList(
                Text.literal("FA Colors:"),
                Text.literal("- §8Predetermined list of players will be"),
                Text.literal("  §8recolored globally"),
                Text.literal("- §8You can add your own entries, but"),
                Text.literal("  §8other players will not see them")
            ),
            Key.GLOBAL_FA_COLORS,
            false,
            FaColors::refreshGlobal
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
            "Updates data on tab updates, limited to once per second.\nIf disabled, data will rely on chat.",
            Key.HUD_PET_DYNAMIC,
            false,
            PetInfo::refresh,
            new ExtraControl("Pet Display", false, false)
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
            50000,
            "%.0f",
            value -> {
                FishyConfig.setFloat(Key.FILTER_MIN_VALUE, value);
                FishyConfig.enable(Key.VALUE_FILTER, value > 0.0f);
                TrackerDisplay.refreshDisplay();
            }
        ));

        profitEntries.add(VCEntry.toggle(
            "Tracker Drop Notifications",
            "Adds a drop message to tracked items without one.",
            Key.TRACKER_NOTIS,
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
            new ExtraControl("Century Cakes: ", false, false)
        ));

        trackerEntries.add(VCEntry.header("── Rain Tracker ──", ""));

        trackerEntries.add(VCEntry.toggle(
            "Rain Warning",
            "Tracks the worlds weather state and notifies if rain stops.\nDue to Hypixel quirks this will not work reliably in the Park cave.",
            Key.RAIN_NOTI,
            false,
            WeatherTracker::track
        ));

        trackerEntries.add(VCEntry.header("── Moonglade Minigame Alarm ──", ""));

        trackerEntries.add(VCEntry.toggle(
            "Alarm",
            "Alerts you with a sound and system toast when the minigame is ready.",
            Key.BEACON_ALARM,
            false,
            ChatTimers.getInstance()::refresh
        ));

        trackerEntries.add(VCEntry.toggleColorOrHud(
            "Timer Display",
            "Adds a HUD timer if the minigame is on cooldown.",
            Key.HUD_TIMER_ENABLED,
            false,
            ChatTimers.getInstance()::refresh,
            new ExtraControl("Moonglade: ", false, false)
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
            "Removes explosion particles and any sound effects caused by\nwither blade abilities.",
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
            20.0f,
            "%.0f blocks",
            SkyblockCleaner::refresh
        ));

        entries.add(VCEntry.toggle2(
            "Hotspot Warning",
            "Alerts you on hotspot spawn and expiration if nearby. Optionally, announce\nnew hotspots in the toggled chat.",
            Key.TRACK_HOTSPOT,
            false,
            "ANNOUNCE",
            Key.ANNOUNCE_HOTSPOT,
            false,
            FishingHotspot::refresh
        ));

        entries.add(VCEntry.keybind(
            "Hide Gui Buttons",
            "Set the key to hide Skyblock gui icons. Tap the key to add/remove\n hovered slots, peek and bypass block by holding shift.",
            Key.MOD_KEY_LOCK_GUISLOT,
            false,
            GuiIcons::refresh
        ));        

        entries.add(VCEntry.slider(
            "Invisibug Helper",
            "Scales the particles emitted by Invisibugs (crit) while on Galatea.\nSet to 0 to disable.",
            Key.DMG_SCALE,
            0.0f,
            1.5f,
            "%.2f",
                        value -> {
                FishyConfig.setFloat(Key.DMG_SCALE, value);
                FishyConfig.enable(Key.SCALE_CRIT, value > 0.0f);
                ParticleVisuals.refreshCache();
            }
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
            false,
            null
        ));
    }

    /**
     * Chat Filter
     */
    private static void addFilter(List<VCEntry> entries) {
        entries.add(VCEntry.header("── Chat Filter ──", ""));

        List<VCEntry> filterSubEntries = new ArrayList<>();        
        
        entries.add(VCEntry.toggleColorOrHud(
            "Custom Sea Creature Messages",
            "Toggle and configure filter for all sea creatures.",
            Key.CHAT_FILTER_SC_ENABLED,
            false,
            () -> {
                FilterConfig.refreshScRules();
                GradientRenderer.init();
            },
            new ExtraControl(null, false, true)
        ));

        entries.add(VCEntry.toggleColorOrHud(
            "Custom Filter",
            "Filter out or override any chat message.",
            Key.CHAT_FILTER_ENABLED,
            false,
            GradientRenderer::init,
            new ExtraControl(null, false, true)
        ));

        filterSubEntries.add(VCEntry.toggle(
            "Hide Sack Messages",
            "Hide sack drop messages from chat (tracker still receives them).",
            Key.CHAT_FILTER_HIDE_SACK_MESSAGES,
            false,
            null
        ));

        filterSubEntries.add(VCEntry.toggle(
            "Hide Autopet Messages",
            "Hide autopet equip/summon messages from chat (pet display still tracks them).",
            Key.CHAT_FILTER_HIDE_AUTOPET_MESSAGES,
            false,
            null
        ));

        filterSubEntries.add(VCEntry.toggle(
            "Hide Implosion Messages",
            "Hide the ability + failed teleport messages from chat.",
            Key.CHAT_FILTER_HIDE_HYPE,
            false,
            null
        ));

        entries.add(VCEntry.expandable(
            "Gameplay Filters",
            "Hide various spammy Hypixel messages.",
            filterSubEntries,
            Arrays.asList(
                Text.literal("Additional Filters:"),
                Text.literal("- §8Hide sack notifications"),
                Text.literal("- §8Hide autopet"),
                Text.literal("- §8Hide implosion")
            )
        ));

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
            String lowerName = name.toLowerCase();
            int idx = lowerName.indexOf(search);
            if (idx != -1) {
                boolean atStart = idx == 0;
                boolean afterSpace = idx > 0 && lowerName.charAt(idx - 1) == ' ';
                if (atStart || afterSpace) {
                    return true;
                }
            }

            if (entry.description != null) {
                String lowerDesc = entry.description.toLowerCase();
                int descIdx = lowerDesc.indexOf(search);
                if (descIdx != -1) {
                    boolean atStart = descIdx == 0;
                    boolean afterSpace = descIdx > 0 && lowerDesc.charAt(descIdx - 1) == ' ';
                    if (atStart || afterSpace) {
                        return true;
                    }
                }
            }
            return false;
    }
    
    public static boolean needsHudButton(VCEntry entry) {
        return entry.hudElementName != null;
    }
    
    public static boolean needsColorButton(VCEntry entry) {
        return entry.hasColorControl;
    }

    public static boolean needsAddButton(VCEntry entry) {
        return entry.hasAdd();
    }

    private VCManager() {
        throw new UnsupportedOperationException("Utility class");
    }    
}
