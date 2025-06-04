package me.wait.fishyaddons.util;

import me.wait.fishyaddons.handlers.RetexHandler;
import me.wait.fishyaddons.fishyprotection.BlacklistMatcher;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.Minecraft;

import java.util.HashSet;
import java.util.Set;
import java.io.File;
import java.io.IOException;

// Preload for textures in the mod's assets or use ValksfullSbpack as fallback if it exists

public class ResourceUtils {

    private static final Set<ResourceLocation> validTextures = new HashSet<>();

    public static void preload() {
        System.out.println("[FishyAddons] Preloading textures...");
        IResourceManager resourceManager = Minecraft.getMinecraft().getResourceManager();

        File externalResourcePack = findExternal("valksfullsbpack");

        for (String island : RetexHandler.getKnownIslands()) {
            for (String modelString : RetexHandler.getRegisteredModels()) {
                String texPath = ModelUtils.extractBasePath(modelString);
                String islandTexturePath = texPath.replace("{island}", island);
                ResourceLocation textureLoc = new ResourceLocation("fishyaddons:islands/" + island + "/" + islandTexturePath);

                try {
                    resourceManager.getResource(textureLoc);
                    validTextures.add(textureLoc);
                    System.out.println("[FishyAddons] Valid texture added: " + textureLoc);
                } catch (IOException e) {
                    System.err.println("[FishyAddons] Texture not found in mod assets: " + textureLoc);

                    if (externalResourcePack != null && checkExternal(textureLoc, externalResourcePack)) {
                        validTextures.add(textureLoc);
                    } else {
                        System.err.println("[FishyAddons] Texture not found in external resource pack: " + textureLoc);
                    }
                }
            }
        }
    }

    private static File findExternal(String packNamePart) {
        File resourcePacksDir = new File(Minecraft.getMinecraft().mcDataDir, "resourcepacks");
        if (resourcePacksDir.exists() && resourcePacksDir.isDirectory()) {
            for (File pack : resourcePacksDir.listFiles()) {
                String name = BlacklistMatcher.stripColor(pack.getName().toLowerCase());
                if (name.contains(packNamePart)) {
                    System.out.println("[FishyAddons] Found matching resource pack: " + pack.getName());
                    return pack;
                }
            }
        }
        System.err.println("[FishyAddons] No matching resource pack found with name containing: " + packNamePart);
        return null;
    }

    private static boolean checkExternal(ResourceLocation textureLoc, File resourcePack) {
        // Construct the path for the external resource pack
        String externalPath = resourcePack.getAbsolutePath() + "/assets/" + textureLoc.getResourceDomain() + "/textures/" + textureLoc.getResourcePath() + ".png";
        File textureFile = new File(externalPath);

        return textureFile.exists();
    }

    private static boolean doesTextureExist(ResourceLocation location) {
        return validTextures.contains(location);
    }
}
