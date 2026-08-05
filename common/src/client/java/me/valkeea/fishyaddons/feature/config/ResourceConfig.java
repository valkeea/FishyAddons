package me.valkeea.fishyaddons.feature.config;

import me.valkeea.fishyaddons.feature.visual.ResourceHandler;
import me.valkeea.fishyaddons.vconfig.annotation.UIToggle;
import me.valkeea.fishyaddons.vconfig.annotation.VCListener;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.core.UICategory;
import me.valkeea.fishyaddons.vconfig.ui.manager.ScreenManager;
import net.minecraft.client.Minecraft;

@VCModule(UICategory.RESOURCES)
public class ResourceConfig {

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
        ScreenManager.preserveCurrentState();
        ResourceHandler.updateGuiPack();
    }    
    
    @VCListener(
        value = BooleanKey.HD_FONT,
        phase = VCListener.Phase.CHANGE
    )
    private static void onFontPackChanged() {
        ScreenManager.preserveCurrentState();
        ResourceHandler.updateFontPack();
    }

    @VCListener(
        value = {
            BooleanKey.FISHY_GUI,
            BooleanKey.HD_FONT
        },
        phase = VCListener.Phase.POST_CHANGE
    )
    private static void onResourceChange() {
        Minecraft.getInstance().reloadResourcePacks();
    }

    private ResourceConfig() {}
}
