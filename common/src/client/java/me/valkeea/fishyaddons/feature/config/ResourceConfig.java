package me.valkeea.fishyaddons.feature.config;

import java.util.ArrayList;
import java.util.List;

import me.valkeea.fishyaddons.feature.visual.ResourceHandler;
import me.valkeea.fishyaddons.vconfig.annotation.UIDropdown;
import me.valkeea.fishyaddons.vconfig.annotation.UIToggle;
import me.valkeea.fishyaddons.vconfig.annotation.VCListener;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.core.UICategory;
import me.valkeea.fishyaddons.vconfig.ui.manager.ScreenManager;
import me.valkeea.fishyaddons.vconfig.ui.widget.dropdown.item.ToggleItem;
import me.valkeea.fishyaddons.vconfig.ui.widget.dropdown.item.ToggleMenuItem;
import net.minecraft.client.Minecraft;

@VCModule(UICategory.RESOURCES)
public class ResourceConfig {

    @UIToggle(
        key = BooleanKey.REORDER_PACKS,
        name = "Reorder *Resource Pack*s",
        description = "Deprioritizes all server resource packs on Hypixel."
    )
    private static boolean reorderPacks;

    @UIToggle(
        key = BooleanKey.FISHY_GUI,
        name = "Transparent Minecraft *GUI*",
        description = "Uses textures from ValksfullSbPack."
    )
    private static boolean transparentGui;

    @UIToggle(
        key = BooleanKey.HD_FONT,
        name = "HD *Font*",
        description = {"Replaces the default font with a high-definition one", "from ValksfullSbPack."}
    )
    private static boolean hdFont;

    @VCListener(
        value = BooleanKey.FISHY_GUI,
        phase = VCListener.Phase.CHANGE
    )
    private static void onGuiPackChanged() {
        ResourceHandler.updateGuiPack();
    }    
    
    @VCListener(
        value = BooleanKey.HD_FONT,
        phase = VCListener.Phase.CHANGE
    )
    private static void onFontPackChanged() {
        ResourceHandler.updateFontPack();
    }

    @UIDropdown(
        name = "*Retexture* Skyblock Islands",
        description = "Toggle custom textures for Skyblock areas.",
        provider = "getAreas",
        buttonText = "Areas"
    )
    private static String muteList;

    public static List<ToggleMenuItem> getAreas() {
        List<ToggleMenuItem> items = new ArrayList<>();
        items.add(new ToggleItem(BooleanKey.DARK_END, "Dark End"));
        items.add(new ToggleItem(BooleanKey.DARK_CRIMSON, "Cold Nether"));
        return items;
    }

    @VCListener(
        value = {
            BooleanKey.REORDER_PACKS,
            BooleanKey.DARK_END,
            BooleanKey.DARK_CRIMSON,
            BooleanKey.FISHY_GUI,
            BooleanKey.HD_FONT
        },
        phase = VCListener.Phase.POST_CHANGE
    )
    private static void onResourceChange() {
        ScreenManager.preserveCurrentState();        
        Minecraft.getInstance().reloadResourcePacks();
    }

    private ResourceConfig() {}
}
