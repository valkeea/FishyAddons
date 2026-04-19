package me.valkeea.fishyaddons.feature.config;

import java.util.ArrayList;
import java.util.List;

import me.valkeea.fishyaddons.feature.item.safeguard.BlacklistManager;
import me.valkeea.fishyaddons.feature.item.safeguard.GuiHandler;
import me.valkeea.fishyaddons.feature.item.safeguard.SlotHandler;
import me.valkeea.fishyaddons.vconfig.annotation.UIContainer;
import me.valkeea.fishyaddons.vconfig.annotation.UIDropdown;
import me.valkeea.fishyaddons.vconfig.annotation.UIKeybind;
import me.valkeea.fishyaddons.vconfig.annotation.UIToggle;
import me.valkeea.fishyaddons.vconfig.annotation.VCInit;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.StringKey;
import me.valkeea.fishyaddons.vconfig.core.UICategory;
import me.valkeea.fishyaddons.vconfig.ui.widget.dropdown.item.BlacklistItem;
import me.valkeea.fishyaddons.vconfig.ui.widget.dropdown.item.ToggleMenuItem;

@VCModule(UICategory.ITEMS)
public class ItemConfig {

    @VCInit
    public static void init() {
        GuiHandler.init();
        SlotHandler.init();
    }

    private static final String FG = "Item *Safeguard*";

    @UIContainer(
        name = FG,
        description = {
            "Protect your items from accidental drops and clicks.",
            "Configure settings for visual and auditory feedback, and manage protected GUIs."},
        tooltip = {
            "Item Safeguard:",
            "Manage blocked guis",
            "Toggle settings for;",
            "§7Audio & chat feedback",
            "§7Tooltip tags",
            "/fg for commands"
        }
    )
    private static final boolean FG_SETTINGS = false;    

    @UIToggle(
        key = BooleanKey.FG_TOOLTIP,        
        name = "Safeguard Tooltips",
        description = "Display a tag at the end of a protected item's tooltip.",
        parent = FG
    )
    private static boolean fgTooltip;

    @UIToggle(
        key = BooleanKey.FG_CHAT_FEEDBACK,        
        name = "Safeguard Chat Notifications",
        description = "Show chat notifications when safeguard features are triggered.",
        parent = FG
    )
    private static boolean fgChat;

    @UIToggle(
        key = BooleanKey.FG_AUDIO_FEEDBACK,       
        name = "Safeguard Audio Feedback",
        description = "Give auditory feedback when attempting to drop or sell protected items.",
        parent = FG
    )
    private static boolean fgAudio;

    @UIDropdown(
        key = BooleanKey.FG_GUI_PROTECTION,        
        name = "Sell Protection §7(§8Use /fg to see commands§7)",
        buttonText = "Manage",
        provider = "getBlacklist",        
        parent = FG,        
        description = {
            "Main toggle for protecting items in blacklisted Skyblock containers.",
            "Use menu to manage blocked guis."
        }
    )
    private static boolean fgEnabled;

    static List<ToggleMenuItem> getBlacklist() {
        List<ToggleMenuItem> items = new ArrayList<>();
        for (var e : BlacklistManager.getAllEntries()) {
            items.add(new BlacklistItem(e.identifier));
        }
        return items;
    }

    @UIKeybind(
        key = StringKey.KEY_LOCK_SLOT,
        name = "Bind and Lock *Slots*",
        description = {
            "Set the key to lock (tap) and bind (drag) slots.",
            "Left-click again to remove the key (disables the feature)."
        },
        parent = FG
    )
    private static String lockKey = "NONE";

    private ItemConfig() {}
}
