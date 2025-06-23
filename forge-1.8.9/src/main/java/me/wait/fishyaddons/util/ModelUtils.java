package me.wait.fishyaddons.util;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.IModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModelUtils {
    public static final Map<String, String[]> VARIANT_SUFFIXES = new HashMap<>();
    public static final Map<String, String> MODEL_TO_TEXTURE_BASE = new HashMap<>();

    static {
        VARIANT_SUFFIXES.put("sandstone", new String[]{"top", "bottom", "normal"});
        VARIANT_SUFFIXES.put("red_sandstone", new String[]{"top", "bottom", "normal"});
        VARIANT_SUFFIXES.put("sandstone_slab", new String[]{"top", "bottom", "normal"});
        MODEL_TO_TEXTURE_BASE.put("sandstone_slab", "sandstone");
    }

    public static String getVariantTextureName(String blockName, String key, String[] suffixes, EnumFacing face) {
        String textureBase = MODEL_TO_TEXTURE_BASE.getOrDefault(key, key);
        String suffix;
        if (face == EnumFacing.UP) {
            suffix = suffixes[0];
        } else if (face == EnumFacing.DOWN) {
            suffix = suffixes[1];
        } else {
            suffix = suffixes[2];
        }
        if (key.contains("stained_glass_pane")) {
            if (suffix.equals("side")) {
                return "glass_" + key.replace("_stained_glass_pane", "");
            } else if (suffix.equals("top")) {
                return "glass_pane_top_" + key.replace("_stained_glass_pane", "");
            }
        }
        return textureBase + "_" + suffix;
    }

    // Extract the main texture (layer0) from the unbaked IModel
    public static String getPrimaryPath(IModel model) {
        for (ResourceLocation res : model.getTextures()) {
            String path = res.toString();
            if (path.contains("blocks/")) {
                return path;
            }
        }
        return "";
    }

    public static ModelResourceLocation safeLocation(String modelString) {
        if (!modelString.contains("#")) {
            return new ModelResourceLocation(modelString, "normal");
        } else {
            String[] split = modelString.split("#", 2);
            return new ModelResourceLocation(split[0], split[1]);
        }
    }

    public static String extractBasePath(String modelString) {
        // Strips namespace and removes everything after # (variant) - this is to simplify registering models&sprites for simpler blocks
        return modelString.replace("minecraft:", "").replaceAll("#.*", "");
    }

    // Generate blockstate variants for cringe blocks with multiple properties
    
    public static List<String> genNsewVariants(String blockName, String... properties) {
        List<String> variants = new ArrayList<>();
        int combinations = (int) Math.pow(2, properties.length);

        for (int i = 0; i < combinations; i++) {
            StringBuilder variant = new StringBuilder(blockName + "#");
            for (int j = 0; j < properties.length; j++) {
                if (j > 0) variant.append(",");
                variant.append(properties[j]).append("=").append((i & (1 << j)) != 0);
            }
            variants.add(variant.toString());
        }

        return variants;
    }

    public static List<String> genStairVariants(String blockName) {
        List<String> variants = new ArrayList<>();
        String[] facings = {"east", "west", "south", "north"};
        String[] halves = {"bottom", "top"};
        String[] shapes = {"straight", "outer_right", "outer_left", "inner_right", "inner_left"};
    
        for (String facing : facings) {
            for (String half : halves) {
                for (String shape : shapes) {
                    String variant = blockName + "#facing=" + facing + ",half=" + half + ",shape=" + shape;
                    variants.add(variant);
                }
            }
        }
    
        return variants;
    }
}