package me.valkeea.fishyaddons.feature.config;

import java.util.ArrayList;
import java.util.List;

import me.valkeea.fishyaddons.feature.qol.CopyChat;
import me.valkeea.fishyaddons.ui.list.ChatAlerts;
import me.valkeea.fishyaddons.ui.list.TabbedListScreen;
import me.valkeea.fishyaddons.ui.list.TabbedListScreen.Tab;
import me.valkeea.fishyaddons.vconfig.annotation.UIDropdown;
import me.valkeea.fishyaddons.vconfig.annotation.UIExtraToggle;
import me.valkeea.fishyaddons.vconfig.annotation.UIRedirect;
import me.valkeea.fishyaddons.vconfig.annotation.UIToggle;
import me.valkeea.fishyaddons.vconfig.annotation.VCListener;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.core.UICategory;
import me.valkeea.fishyaddons.vconfig.ui.manager.ScreenManager;
import me.valkeea.fishyaddons.vconfig.ui.widget.dropdown.item.NetworkDisplayItem;
import me.valkeea.fishyaddons.vconfig.ui.widget.dropdown.item.ToggleMenuItem;

@VCModule(UICategory.QOL)
public class QolConfig {
    private QolConfig() {}

    @UIToggle(
        key = BooleanKey.COPY_CHAT,
        name = "*Copy Chat*",
        description = {
            "Right-click chat messages to copy them to clipboard.",
            "Hold shift to copy just the hovered line."
        }
    )
    @UIExtraToggle(key = BooleanKey.COPY_NOTI, buttonText = "Noti")
    private static boolean copyNoti;

    @VCListener({BooleanKey.COPY_CHAT, BooleanKey.COPY_NOTI})
    private static void onCopyChatChange(boolean newValue) {
        CopyChat.refresh();
    }    

    @UIToggle(
        key = BooleanKey.SKIP_F5,
        name = "Skip Front *Perspective*",
        description = {
            "Skip the front-facing perspective when cycling camera views (F5).",
            "Goes directly from first person to back view."
        }
    )
    private static boolean skipF5;

    @UIDropdown(
        key = BooleanKey.HUD_METRICS_ENABLED, provider = "getProvider",
        name = "Debug HUD §7(§8*Ping/TPS/FPS*§7)",        
        description = {
            "Shows your current ping, fps and a tps estimate in the hud.",
            "Use /fa ping to send this value in chat, /fa hud to customize."
        }
    )
    private static boolean hudMetricsEnabled;

    public static List<ToggleMenuItem> getProvider() {
        List<ToggleMenuItem> items = new ArrayList<>();
        items.add(new NetworkDisplayItem(BooleanKey.METRICS_SHOW_PING));
        items.add(new NetworkDisplayItem(BooleanKey.METRICS_SHOW_TPS));
        items.add(new NetworkDisplayItem(BooleanKey.METRICS_SHOW_FPS));
        return items;
    }    

    @UIToggle(
        key = BooleanKey.KEY_SHORTCUTS,
        name = "Custom *Keybinds*",
        description = "Configure custom keybinds for commands. Feature has a delay to prevent spam."
    )
    @UIRedirect(method = "keyEditor")
    private static boolean keyShortcuts;

    public static void keyEditor() {
        ScreenManager.navigateConfigScreen(new TabbedListScreen(Tab.KEYBINDS));
    }

    @UIToggle(
        key = BooleanKey.ALIASES,
        name = "Custom *Commands*",
        description = "Add shortcuts (aliases) for existing server / mod commands."
    )
    @UIRedirect(method = "commandEditor")
    private static boolean commandAliases;

    public static void commandEditor() {
        ScreenManager.navigateConfigScreen(new TabbedListScreen(Tab.COMMANDS));
    }

    @UIToggle(
        key = BooleanKey.CHAT_REPLACEMENTS,
        name = "*Chat* Replacements",
        description = {
            "Replaces sent words/phrases with your configured alternatives.",
            "Supports commands and chat (Example <3 → ❤)."
        }
    )
    @UIRedirect(method = "chatReplacementEditor")
    private static boolean chatReplacements;

    public static void chatReplacementEditor() {
        ScreenManager.navigateConfigScreen(new TabbedListScreen(Tab.CHAT));
    }

    @UIToggle(
        key = BooleanKey.CHAT_ALERTS,
        name = "Chat *Alerts*",
        description = {
            "Custom alerts with optional title, sound and autochat on",
            " chat events. Be specific to prevent undesired matches."
        }
    )
    @UIRedirect(method = "chatAlertEditor")
    private static boolean chatAlerts;

    public static void chatAlertEditor() {
        ScreenManager.navigateConfigScreen(new ChatAlerts());
    }   
}
