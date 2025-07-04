package me.valkeea.fishyaddons.handler;

import me.valkeea.fishyaddons.config.FishyConfig;

import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.minecraft.util.Identifier;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.resource.ResourcePackProfile;

import java.util.List;
import java.util.ArrayList;

public class ResourceHandler {
    private ResourceHandler() {}
    public static final String MOD_ID = "fishyaddons";
    public static final Identifier HD_FONT = Identifier.of(MOD_ID, "hd_font");
    public static final Identifier FISHY_GUI = Identifier.of(MOD_ID, "fishy_gui");

    public static void register() {
        ResourceManagerHelper.registerBuiltinResourcePack(
            HD_FONT,
            FabricLoader.getInstance().getModContainer(MOD_ID).get(),
            ResourcePackActivationType.NORMAL
        );
        ResourceManagerHelper.registerBuiltinResourcePack(
            FISHY_GUI,
            FabricLoader.getInstance().getModContainer(MOD_ID).get(),
            ResourcePackActivationType.NORMAL
        );
    }

    public static void updateFontPack() {
        updateBuiltinPack("hdFont", "fishyaddons:hd_font");
    }

    public static void updateGuiPack() {
        updateBuiltinPack("fishyGui", "fishyaddons:fishy_gui");
    }

    private static void updateBuiltinPack(String configKey, String packId) {
        boolean enabled = FishyConfig.getState(configKey, false);
        MinecraftClient mc = MinecraftClient.getInstance();
        GameOptions options = mc.options;

        options.resourcePacks.removeIf(id -> id.equals(packId));
        if (enabled) {
            options.resourcePacks.add(packId);
        }

        if (options.incompatibleResourcePacks != null) {
            options.incompatibleResourcePacks.removeIf(id -> id.equals(packId));
        }

        options.write();
        var packManager = mc.getResourcePackManager();
        var enabledProfiles = new ArrayList<>(packManager.getEnabledProfiles());
        enabledProfiles.removeIf(profile -> profile.getId().equals(packId));

        if (enabled) {
            packManager.getProfiles().stream()
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
