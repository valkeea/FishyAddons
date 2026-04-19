package me.valkeea.fishyaddons.feature.visual;

import java.util.ArrayList;
import java.util.List;

import me.valkeea.fishyaddons.vconfig.annotation.VCInit;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.util.Identifier;

@VCModule
public class ResourceHandler {
    private ResourceHandler() {}
    public static final String MOD_ID = "fishyaddons";
    public static final Identifier HD_FONT = Identifier.of(MOD_ID, "hd_font");
    public static final Identifier FISHY_GUI = Identifier.of(MOD_ID, "fishy_gui");
    public static final Identifier FIRE_OVERLAY = Identifier.of(MOD_ID, "fire_overlay");

    @VCInit
    public static void init() {
        FabricLoader.getInstance().getModContainer(MOD_ID).ifPresent(modContainer -> {
            ResourceManagerHelper.registerBuiltinResourcePack(
                HD_FONT,
                modContainer,
                ResourcePackActivationType.NORMAL
            );
            ResourceManagerHelper.registerBuiltinResourcePack(
                FISHY_GUI,
                modContainer,
                ResourcePackActivationType.NORMAL
            );
            ResourceManagerHelper.registerBuiltinResourcePack(
                FIRE_OVERLAY,
                modContainer,
                ResourcePackActivationType.NORMAL
            );
        });
    }

    public static void updateFontPack() {
        updateBuiltinPack(BooleanKey.HD_FONT, "fishyaddons:hd_font");
    }

    public static void updateGuiPack() {
        updateBuiltinPack(BooleanKey.FISHY_GUI, "fishyaddons:fishy_gui");
    }

    public static void updateFirePack() {
        updateBuiltinPack(BooleanKey.FIRE_OVERLAY, "fishyaddons:fire_overlay");
    }

    private static void updateBuiltinPack(BooleanKey configKey, String packId) {
        boolean enabled = Config.get(configKey);
        var mc = MinecraftClient.getInstance();
        var options = mc.options;

        options.resourcePacks.removeIf(id -> id.equals(packId));
        if (enabled) {
            options.resourcePacks.add(packId);
        }

        if (options.incompatibleResourcePacks != null) {
            options.incompatibleResourcePacks.removeIf(id -> id.equals(packId));
        }

        options.write();
        var packManager = mc.getResourcePackManager();
        var packProfiles = packManager.getProfiles() == null ? List.<ResourcePackProfile>of() : packManager.getProfiles();

        if (packProfiles.isEmpty()) return;
        
        var enabledProfiles = new ArrayList<>(packManager.getEnabledProfiles());
        enabledProfiles.removeIf(profile -> profile.getId().equals(packId));

        if (enabled) {
            packProfiles.stream()
                .filter(profile -> profile.getId().equals(packId))
                .findFirst()
                .ifPresent(enabledProfiles::add);
        }

        List<String> enabledIds = enabledProfiles.stream()
            .map(ResourcePackProfile::getId)
            .toList();

        packManager.setEnabledProfiles(enabledIds);
        mc.reloadResources();
    }
}
