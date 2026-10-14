package me.valkeea.fishyaddons.feature.visual.ato;

import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonParser;

import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas;
import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas.Island;
import me.valkeea.fishyaddons.vconfig.annotation.VCListener;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Discovers per-area block/particle/entity overrides from resources.
 *
 * <h4>Convention</h4>
 * An override for {@code <namespace>:textures/block/<path>.png} (or {@code particle/}, {@code entity/})
 * is placed at:
 * <pre>{@code assets/<namespace>/textures/block/ato/<island_key>/<path>.png}</pre>
 * These override files are stitched into the block/particle atlas automatically on reload.
 * <p>
 * <h4>Particle color overrides</h4>
 * Since most particles are colored programmatically at spawn time,
 * they are tinted by placing a JSON file at:
 * <pre>{@code assets/<namespace>/textures/particle/ato/<island_key>/<particle_type>.json}</pre>
 * The file name is the particle type's registry path (not a texture/sprite name). Contents are either
 * {@code {"r":1.0,"g":0.5,"b":0.0}} (0-1 floats) or {@code {"color":"RRGGBB"}}.
 * <p>
 * This registry is rebuilt once per resource reload ({@link ATOHook}).
 */
@VCModule
public final class AreaTextureOverrides {
    private AreaTextureOverrides() {}

    private static final Pattern AREA_PATH = Pattern.compile("^(block|particle|entity)/ato/([a-z0-9_.\\-]+)/(.+)$");
    private static final Pattern PARTICLE_COLOR_PATH = Pattern.compile("^particle/ato/([a-z0-9_.\\-]+)/(.+)$");
    private static final String TEX_PATH = "textures/";

    private static final AtomicReference<Map<Island, Map<Identifier, Identifier>>> overrides =
        new AtomicReference<>(new EnumMap<>(Island.class));
    private static final AtomicReference<Map<Island, Map<Identifier, float[]>>> particleColors =
        new AtomicReference<>(new EnumMap<>(Island.class));

    // Keyed by baked FluidModel instance identity
    private static final AtomicReference<Map<Island, Map<FluidModel, FluidModel>>> fluidModelCache =
        new AtomicReference<>(new EnumMap<>(Island.class));

    /**
     * Rescans the given resource manager for area-prefixed textures and rebuilds the registry.
     */
    @VCListener(
        value = {BooleanKey.DARK_END, BooleanKey.DARK_CRIMSON},
        phase = VCListener.Phase.CHANGE
    )    
    public static void rebuild() {
        ResourceManager manager = Minecraft.getInstance().getResourceManager();

        Map<Island, Map<Identifier, Identifier>> next = new EnumMap<>(Island.class);
        Map<Island, Map<Identifier, float[]>> nextColors = new EnumMap<>(Island.class);
        Set<Island> enabled = enabledIslands();

        if (enabled.isEmpty()) {
            overrides.set(next);
            particleColors.set(nextColors);
            fluidModelCache.set(new EnumMap<>(Island.class));
            return;
        }

        for (String root : new String[] {"textures/block", "textures/particle", "textures/entity"}) {
            var found = manager.listResources(root,
                id -> id.getPath().contains("/ato/") && id.getPath().endsWith(".png"));

            for (var resourceId : found.keySet()) {
                registerOverride(next, resourceId, enabled);
            }
        }

        var colorFiles = manager.listResources("textures/particle",
            id -> id.getPath().contains("/ato/") && id.getPath().endsWith(".json"));
        for (var resourceId : colorFiles.keySet()) {
            registerParticleColor(manager, nextColors, resourceId, enabled);
        }

        overrides.set(next);
        particleColors.set(nextColors);
        fluidModelCache.set(new EnumMap<>(Island.class));
    }

    private static void registerOverride(Map<Island, Map<Identifier, Identifier>> target, Identifier resourceId, Set<Island> enabled) {

        String path = resourceId.getPath();
        if (!path.startsWith(TEX_PATH)) return;

        String trimmed = path.substring(TEX_PATH.length(), path.length() - ".png".length());
        Matcher m = AREA_PATH.matcher(trimmed);
        if (!m.matches()) return;

        String kind = m.group(1);
        String islandKey = m.group(2);
        String relative = m.group(3);

        var island = findIsland(islandKey);
        if (island == null || !enabled.contains(island)) {
            return;
        }

        Identifier vanilla;
        Identifier override;
        if ("entity".equals(kind)) {
            // entity textures are bound directly by their full resource path (no atlas sprite id involved)
            vanilla = Identifier.fromNamespaceAndPath(resourceId.getNamespace(), TEX_PATH + kind + "/" + relative + ".png");
            override = resourceId;
        } else if ("particle".equals(kind)) {
            // particle atlas uses a directory source with an EMPTY prefix, so sprite ids are the bare relative path (e.g. "flame")
            vanilla = Identifier.fromNamespaceAndPath(resourceId.getNamespace(), relative);
            override = Identifier.fromNamespaceAndPath(resourceId.getNamespace(), "ato/" + islandKey + "/" + relative);
        } else {
            vanilla = Identifier.fromNamespaceAndPath(resourceId.getNamespace(), kind + "/" + relative);
            override = Identifier.fromNamespaceAndPath(resourceId.getNamespace(), trimmed);
        }

        target.computeIfAbsent(island, i -> new HashMap<>()).put(vanilla, override);
    }

    private static void registerParticleColor(ResourceManager manager, Map<Island, Map<Identifier, float[]>> target,
                                               Identifier resourceId, Set<Island> enabled) {

        String path = resourceId.getPath();
        if (!path.startsWith(TEX_PATH)) return;

        String trimmed = path.substring(TEX_PATH.length(), path.length() - ".json".length());
        Matcher m = PARTICLE_COLOR_PATH.matcher(trimmed);
        if (!m.matches()) return;

        String islandKey = m.group(1);
        String particleType = m.group(2);

        var island = findIsland(islandKey);
        if (island == null || !enabled.contains(island)) {
            return;
        }

        float[] color = readColor(manager, resourceId);
        if (color == null) return;

        var type = Identifier.fromNamespaceAndPath(resourceId.getNamespace(), particleType);
        target.computeIfAbsent(island, i -> new HashMap<>()).put(type, color);
    }

    private static @Nullable float[] readColor(ResourceManager manager, Identifier resourceId) {
        try (var reader = manager.openAsReader(resourceId)) {
            var json = JsonParser.parseReader(reader).getAsJsonObject();

            if (json.has("color")) {
                String hex = json.get("color").getAsString();
                int rgb = Integer.parseInt(hex, 16);
                return new float[] {((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f};
            }

            return new float[] {
                json.has("r") ? json.get("r").getAsFloat() : 1f,
                json.has("g") ? json.get("g").getAsFloat() : 1f,
                json.has("b") ? json.get("b").getAsFloat() : 1f
            };
        } catch (IOException | RuntimeException e) {
            System.err.println("[FA/ato] Failed to read particle color override " + resourceId + ": " + e.getMessage());
            return null;
        }
    }

    private static Island findIsland(String key) {
        for (var island : Island.values()) {
            if (island.key().equals(key)) return island;
        }
        return null;
    }

    /** @return true if at least one area override was found on the last reload. */
    public static boolean hasAnyOverrides() {
        return !overrides.get().isEmpty();
    }

    /** @return true if the given island has at least one particle color override. */
    public static boolean hasParticleColors(Island island) {
        var map = particleColors.get().get(island);
        return map != null && !map.isEmpty();
    }    

    /** @return true if the given island has at least one override texture. */
    public static boolean hasOverrides(Island island) {
        var map = overrides.get().get(island);
        return map != null && !map.isEmpty();
    }

    /** @return the set of islands that currently have at least one override texture. */
    public static Set<Island> activeIslands() {
        return overrides.get().keySet();
    }

    /**
     * @return the override sprite identifier for {@code vanillaTexture} under {@code island},
     * or {@code null} if there is no override for that texture in that area.
     */
    public static Identifier remap(Island island, Identifier vanillaTexture) {
        var map = overrides.get().get(island);
        return map == null ? null : map.get(vanillaTexture);
    }

    /**
     * @return the {@code [r, g, b]} (0-1) color override for {@code particleType} under
     * {@code island}, or {@code null} if there is no color override for that particle type in
     * that area.
     */
    public static float[] remapColor(Island island, Identifier particleType) {
        var map = particleColors.get().get(island);
        return map == null ? null : map.get(particleType);
    }

    /**
     * Fluids have their own bake pass in {@code FluidStateModelSet.bake}), so this is called live from
     * {@code MixinFluidStateModelSet}
     *
     * @return a redirected copy of {@code original} for the current island,
     * or {@code null} if no override applies
     */
    public static @Nullable FluidModel remapFluidModel(FluidModel original) {
        var island = SkyblockAreas.getIsland();
        if (!hasOverrides(island)) return null;

        var cache = fluidModelCache.get().computeIfAbsent(island, i -> new IdentityHashMap<>());
        if (cache.containsKey(original)) return cache.get(original);

        var still = remapFluidMaterial(island, original.stillMaterial());
        var flowing = remapFluidMaterial(island, original.flowingMaterial());
        var overlay = remapFluidMaterial(island, original.overlayMaterial());

        FluidModel remapped = (still == original.stillMaterial()
            && flowing == original.flowingMaterial()
            && overlay == original.overlayMaterial())
            ? null
            : new FluidModel(original.layer(), still, flowing, overlay, original.tintSource());

        cache.put(original, remapped);
        return remapped;
    }

    private static Material.Baked remapFluidMaterial(Island island, Material.Baked material) {
        if (material == null) return null;

        Identifier mapped = remap(island, material.sprite().contents().name());
        if (mapped == null) return material;

        if (Minecraft.getInstance().getTextureManager().getTexture(material.sprite().atlasLocation()) instanceof TextureAtlas atlas) {
            return new Material.Baked(atlas.getSprite(mapped), material.forceTranslucent());
        }
        return material;
    }

    /**
     * @return the set of islands that are currently enabled for area texture overrides.
     */
    private static Set<Island> enabledIslands() {

        var all = SkyblockAreas.Island.values();
        Set<Island> enabled = new java.util.HashSet<>();

        for (Island i : all) {
            var key = BooleanKey.fromString("retex_" + i.key());
            if (key != BooleanKey.NONE && Config.get(key)) {
                enabled.add(i);
            }
        }
        return enabled;
    }    
}
