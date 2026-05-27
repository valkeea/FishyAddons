package me.valkeea.fishyaddons.feature.config;

import java.util.ArrayList;
import java.util.List;

import me.valkeea.fishyaddons.feature.filter.FilterConfig;
import me.valkeea.fishyaddons.ui.list.FilterRules;
import me.valkeea.fishyaddons.ui.list.ScRules;
import me.valkeea.fishyaddons.vconfig.annotation.UIDropdown;
import me.valkeea.fishyaddons.vconfig.annotation.UIRedirect;
import me.valkeea.fishyaddons.vconfig.annotation.UIToggle;
import me.valkeea.fishyaddons.vconfig.annotation.VCListener;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.core.UICategory;
import me.valkeea.fishyaddons.vconfig.ui.manager.ScreenManager;
import me.valkeea.fishyaddons.vconfig.ui.widget.dropdown.item.ToggleItem;
import me.valkeea.fishyaddons.vconfig.ui.widget.dropdown.item.ToggleMenuItem;

@VCModule(UICategory.FILTER)
public class ChatConfig {

    @UIToggle(
        key = BooleanKey.CHAT_FILTER_SC_ENABLED,
        name = "Custom *Sea Creature* Messages",
        description = "Toggle and configure Fishing / Sc Filter."
    )
    @UIRedirect(buttonText = "Configure", method = "openScEditor")
    private static boolean scFilterEnabled;

    static void openScEditor() {
        ScreenManager.navigateConfigScreen(new ScRules());
    }

    @VCListener(BooleanKey.CHAT_FILTER_SC_ENABLED)
    private static void onRulesChanged() {
        FilterConfig.refreshScRules();
    }

    @UIToggle(
        key = BooleanKey.CHAT_FILTER_ENABLED,
        name = "*Custom* Chat Filter",
        description = "Filter out or override any chat message."
    )
    @UIRedirect(buttonText = "Configure", method = "openFilterEditor")
    private static boolean chatFilterEnabled;

    static void openFilterEditor() {
        ScreenManager.navigateConfigScreen(new FilterRules());
    }

    @UIDropdown(
        name = "*Gameplay* Filters",
        description = "Hide various spammy Hypixel messages.",
        provider = "gameplayFilters",
        buttonText = "Hidden",
        tooltip = {
            "Additional Filters:",
            "Hide sack",
            "Hide autopet",
            "Hide implosion"
        }
    )
    private static boolean gameplayFilters;

    static List<ToggleMenuItem> gameplayFilters() {
        List<ToggleMenuItem> items = new ArrayList<>();
        items.add(new ToggleItem(BooleanKey.CHAT_FILTER_HIDE_SACK_MESSAGES, "Sack"));
        items.add(new ToggleItem(BooleanKey.CHAT_FILTER_HIDE_AUTOPET_MESSAGES, "Autopet"));
        items.add(new ToggleItem(BooleanKey.CHAT_FILTER_HIDE_HYPE, "Implosion"));
        return items;
    }

    @UIToggle(
        key = BooleanKey.CHAT_FILTER_PARTYBTN,
        name = "Click to *Party*",
        description = "Adds a clickable [Party] button to specific guild messages."
    )
    private static boolean partyBtn;

    @UIToggle(
        key = BooleanKey.CHAT_FORMATTING,
        name = "Ingame Chat *Formatting*",
        description = {
            "Allow rendering and using custom / minecraft formatting codes",
            "(&c) in chat messages."
        }
    )
    private static boolean chatFormatting;

    private ChatConfig() {}
}
