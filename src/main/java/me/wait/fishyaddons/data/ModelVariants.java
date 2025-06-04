package me.wait.fishyaddons.data;

import me.wait.fishyaddons.util.ModelUtils;

import java.util.*;

public class ModelVariants {
    protected static final Map<String, List<String>> ISLAND_MODEL_MAP = new HashMap<>();

    private ModelVariants() {}

    static {
        ISLAND_MODEL_MAP.put("the_end", new ArrayList<>(Arrays.asList(
            "minecraft:bedrock", "minecraft:birch_planks", "minecraft:black_wool", 
            "minecraft:clay", "minecraft:cyan_stained_hardened_clay",
            "minecraft:end_stone", "minecraft:magenta_stained_glass", "minecraft:purple_stained_glass", "minecraft:light_blue_wool",
            "minecraft:brown_mushroom_block#variant=all_inside", "minecraft:red_mushroom_block#variant=all_inside",
            "minecraft:obsidian", "minecraft:purple_carpet", "minecraft:purple_stained_hardened_clay",
            "minecraft:sand", "minecraft:silver_stained_hardened_clay", "minecraft:sandstone",
            "minecraft:purple_stained_hardened_clay",
            "minecraft:magenta_wool",
            "minecraft:sandstone_slab#half=bottom", "minecraft:sandstone_slab#half=top",
            "minecraft:end_stone"
        )));

        ISLAND_MODEL_MAP.get("the_end").addAll(ModelUtils.genStairVariants("minecraft:birch_stairs"));
        ISLAND_MODEL_MAP.get("the_end").addAll(ModelUtils.genNsewVariants("minecraft:magenta_stained_glass_pane", "east", "north", "south", "west"));
        ISLAND_MODEL_MAP.get("the_end").addAll(ModelUtils.genNsewVariants("minecraft:purple_stained_glass_pane", "east", "north", "south", "west"));


        ISLAND_MODEL_MAP.put("crimson_isles", new ArrayList<>(Arrays.asList(
            "minecraft:acacia_log#axis=z", "minecraft:acacia_log#axis=x", "minecraft:acacia_log#axis=y",
            "minecraft:black_stained_hardened_clay", "minecraft:brown_stained_hardened_clay",
            "minecraft:brick_slab#half=bottom", "minecraft:cyan_stained_hardened_clay", "minecraft:hardened_clay",
            "minecraft:bedrock", "minecraft:red_stained_glass", "minecraft:glowstone", "minecraft:granite",
            "minecraft:smooth_granite", "minecraft:gray_stained_hardened_clay", "minecraft:gray_wool",
            "minecraft:netherrack", "minecraft:nether_brick", "minecraft:nether_brick_slab#half=top",
            "minecraft:nether_brick_double_slab", "minecraft:nether_brick_double_slab#all",
            "minecraft:nether_brick_slab#half=bottom", "minecraft:orange_stained_hardened_clay",
            "minecraft:red_sand", "minecraft:silver_stained_hardened_clay", 
            "minecraft:soul_sand", "minecraft:sponge#wet=false", "minecraft:dead_bush",
            "minecraft:trapdoor#facing=north,half=top,open=false",
            "minecraft:brown_mushroom_block#variant=all_outside", "minecraft:brown_mushroom"
        )));

        ISLAND_MODEL_MAP.get("crimson_isles").addAll(ModelUtils.genStairVariants("minecraft:nether_brick_stairs"));
        ISLAND_MODEL_MAP.get("crimson_isles").addAll(ModelUtils.genNsewVariants("minecraft:nether_brick_fence", "east", "north", "south", "west"));


        ISLAND_MODEL_MAP.put("hub", new ArrayList<>(Arrays.asList(
            "minecraft:hardened_clay", "minecraft:brick_block", "minecraft:brick_slab#half=bottom",
            "minecraft:brick_slab#half=top", "minecraft:jungle_slab#half=bottom"
        )));

        ISLAND_MODEL_MAP.get("hub").addAll(ModelUtils.genStairVariants("minecraft:brick_stairs"));
        ISLAND_MODEL_MAP.get("hub").addAll(ModelUtils.genNsewVariants("minecraft:orange_stained_glass_pane", "east", "north", "south", "west"));
        ISLAND_MODEL_MAP.get("hub").addAll(ModelUtils.genNsewVariants("minecraft:yellow_stained_glass_pane", "east", "north", "south", "west"));


        ISLAND_MODEL_MAP.put("dwarven_mines", new ArrayList<>(Arrays.asList(
            "minecraft:brown_carpet", "minecraft:gray_stained_hardened_clay",
            "minecraft:light_blue_wool"
        )));


        ISLAND_MODEL_MAP.put("dungeon", new ArrayList<>(Arrays.asList(
            "minecraft:gray_stained_glass", "minecraft:silver_stained_glass", "minecraft:magenta_stained_hardened_clay",
            "minecraft:purple_stained_hardened_clay", "minecraft:purple_carpet", "minecraft:magenta_stained_glass",
            "minecraft:blue_stained_hardened_clay", "minecraft:magenta_carpet"
        )));
        
        ISLAND_MODEL_MAP.get("dungeon").addAll(ModelUtils.genNsewVariants("minecraft:magenta_stained_glass_pane", "east", "north", "south", "west"));        
        

        ISLAND_MODEL_MAP.put("mineshaft", new ArrayList<>(Arrays.asList(
            "minecraft:lapis_block"
        )));         
    }

    public static List<String> get(String island) {
        return ISLAND_MODEL_MAP.getOrDefault(island, Collections.emptyList());
    }
}