package me.valkeea.fishyaddons.feature.config;

import me.valkeea.fishyaddons.ui.list.CustomFaColors;
import me.valkeea.fishyaddons.vconfig.annotation.UIColorPicker;
import me.valkeea.fishyaddons.vconfig.annotation.UIContainer;
import me.valkeea.fishyaddons.vconfig.annotation.UIRedirect;
import me.valkeea.fishyaddons.vconfig.annotation.UISlider;
import me.valkeea.fishyaddons.vconfig.annotation.UIToggle;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.IntKey;
import me.valkeea.fishyaddons.vconfig.core.UICategory;
import me.valkeea.fishyaddons.vconfig.ui.manager.ScreenManager;

@VCModule(UICategory.RENDERING)
public class RenderConfig {

    private static final String LAVA = "*Lava* Options";

    @UIContainer(
        name = LAVA,
        description =  "Configure Translucent Lava, fog effects and fire overlay.",
        tooltip = {"Lava Options:", "Translucent lava", "Remove lava fog", "Matching fire overlay"}
    )
    private static final boolean LAVA_SETTINGS = false;

    @UIToggle(
        key = BooleanKey.TRANS_LAVA,
        name = "Translucent Lava (Crimson Isles only)",
        description = {
            "Makes lava look translucent like water, with a custom tint.",
            "Adds subtle underwater fog if Clear Lava is disabled.",
        },
        parent = LAVA
    )
    @UIColorPicker(key = IntKey.TRANS_LAVA_COLOR)
    private static boolean enabled;

    @UIToggle(
        key = BooleanKey.FISHY_LAVA,
        name = "Clear Lava",
        description = {"Removes any fog overlay when submerged in lava.", "Disabled outside Skyblock."},
        parent = LAVA
    )
    public boolean clearLava = false; 

    @UIToggle(
        key = BooleanKey.FIRE_OVERLAY,
        name = "*Fire* FOV",
        description = "Uses a less intrusive texture for fire matching the configured lava color.",
        parent = LAVA
    )
    public boolean customFireOverlay = false;    
    
    @UIToggle(
        key = BooleanKey.FISHY_WATER,
        name = "Clear *Water*",
        description = {
            "Removes underwater fog and improves visibility.",
            "Disabled outside Skyblock."
        },
        order = 20
    )
    public boolean clearWater = false;

    @UIContainer(
        key = BooleanKey.XP_COLOR_ON,
        name = "*XP* Text Color",
        description = {
            "Customize the appearance of vanilla experience display by",
            "altering the color and adding an outline."
        }
    )
    private static final boolean XP_COLOR_SETTINGS = false;

    @UIToggle(
        key = BooleanKey.XP_OUTLINE,
        name = "Color and Outline",
        description = "Set a color and toggle a bold outline.",
        buttonText = "Outline",
        parent = "*XP* Text Color"
    )
    @UIColorPicker(key = IntKey.XP_COLOR)
    private static boolean xpOutline;

    @UISlider(
        name = "*Redstone* Particle Color",
        altKey = IntKey.REDSTONE_COLOR_INDEX,
        min = 0.0, max = 5.0,
        labels = {"Off", "Aqua", "Mint", "Pink", "Prism", "Custom"},
        labelColors = {0xFF808080, 0xFF66FFFF, 0xFF66FF99, 0xFFFFCCFF, 0xFFE5E5FF, 0xFFFFFFFF},
        description = {
            "Customize redstone particle colors. Use the slider to select presets or",
            "click the color square for custom colors."
        }
    )
    @UIColorPicker(key = IntKey.REDSTONE_COLOR)
    private static int colorIndex;

    private static final String FACOLORS = "*FA Colors*";

    @UIContainer(
        key = BooleanKey.GLOBAL_FA_COLORS,
        name = FACOLORS,
        description = {
            "Colors player names for all users.",
            "§8Note: Sidebar/tab recoloring is incompatible with Skyhanni's custom HUD."
        },
        tooltip = {
            "FA Colors:",
            "§7Global list is visible",
            "§7to all users."
        }
    )
    private static final boolean FAC_SETTINGS = false;

    @UIToggle(
        key = BooleanKey.CUSTOM_FA_COLORS,
        name = "Custom FA Colors",
        description = "Add your own entries (not shared with others).",
        parent = FACOLORS
    )
    @UIRedirect(method = "colorEditor", buttonText = "Add")
    private static boolean customFaColors;

    public static void colorEditor() {
        ScreenManager.navigateConfigScreen(new CustomFaColors());
    }

    @UIToggle(
        key = BooleanKey.SB_ONLY_FAC,
        name = "Skyblock Only",
        description = "Enables FA Colors only in Skyblock.",
        parent = FACOLORS
    )
    private static boolean sbOnlyFac;
}
