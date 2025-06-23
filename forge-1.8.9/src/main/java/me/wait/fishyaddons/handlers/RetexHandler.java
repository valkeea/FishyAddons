package me.wait.fishyaddons.handlers;

import me.wait.fishyaddons.impl.GhostModel;
import me.wait.fishyaddons.data.ModelVariants;
import me.wait.fishyaddons.util.ModelUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.renderer.block.statemap.BlockStateMapper;
import net.minecraft.block.state.IBlockState;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


@SideOnly(Side.CLIENT)
public class RetexHandler {
    private static final Map<ModelResourceLocation, IBakedModel> overrideModels = new HashMap<>();
    private static final Map<String, ModelResourceLocation> registeredModels = new HashMap<>();
    private static boolean isRetexEnabled = true;
    private static String currentIsland = "";

    private static final Set<String> KNOWN_ISLANDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "default", "hub", "crimson_isles", "the_end", "dwarven_mines", "mineshaft", "dungeon"
    )));

    private static final Set<String> FORCED = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "the_end"
    )));

    public static Set<String> getKnownIslands() {
        if (isRetexEnabled) {
            return KNOWN_ISLANDS;
        } else {
            return Collections.emptySet();
        }
    }

    public static Set<String> getForced() {
        return FORCED;
    }

    public static void setIsland(String islandName) {
        islandName = islandName.toLowerCase();

        if (isRetexEnabled || !currentIsland.equals(islandName)) {
            currentIsland = islandName;
            reloadOverrides();
            GhostModel.clearSpriteCache();
        } else {
            currentIsland = "default";
        }
    }

    public static String getIsland() {
        return currentIsland;
    }

    public static IBakedModel getOverrideModel(ModelResourceLocation location) {
        return overrideModels.get(location);
    }

    public static Set<String> getRegisteredModels() {
        return registeredModels.keySet();
    }

    public static void reloadOverrides() {
        overrideModels.clear();
        registeredModels.clear();

        List<String> islandModels = ModelVariants.get(currentIsland);

        for (String modelString : islandModels) {
            ModelResourceLocation loc = ModelUtils.safeLocation(modelString);

            try {
                IBakedModel defaultModel = Minecraft.getMinecraft().getRenderItem().getItemModelMesher().getModelManager().getModel(loc);
                IModel unbaked = ModelLoaderRegistry.getModel(loc);
                String texturePath = ModelUtils.getPrimaryPath(unbaked);

                IBakedModel ghost = new GhostModel(defaultModel, loc, texturePath);
                overrideModels.put(loc, ghost);
                registeredModels.put(modelString, loc);
            } catch (Exception e) {
                System.err.println("[FishyAddons] Error loading model: " + modelString);
                e.printStackTrace();
            }
        }
    }

    public static List<String> getIslandOverrides(String island) {
        return ModelVariants.get(island);
    } 
}
