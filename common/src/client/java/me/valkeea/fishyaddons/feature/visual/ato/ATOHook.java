package me.valkeea.fishyaddons.feature.visual.ato;

import java.util.EnumMap;
import java.util.Map;

import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas.Island;
import me.valkeea.fishyaddons.vconfig.annotation.VCInit;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;

/**
 * Wires up dynamic per-area block texture overrides.
 * <p>
 * On resource reload, {@link AreaTextureOverrides} is rescanned, then every baked block
 * model is offered the chance to be wrapped in a swappable {@link ATOBlockStateModel}.
 * <p>
 * Entity and particle textures arent baked at model-load time at all, so they are redirected live
 * per render/sprite-fetch, using the same {@link AreaTextureOverrides#remap} registry via
 * {@link MixinLivingEntityRenderer} and {@link MixinParticleSpriteSet}.
 */
@VCModule
public final class ATOHook {
    private ATOHook() {}

    @VCInit
    public static void init() {

        ModelLoadingPlugin.register(ctx -> {
            AreaTextureOverrides.rebuild();

            ctx.modifyBlockModelAfterBake().register(ATOHook::wrapBlock);
        });
    }

    private static BlockStateModel wrapBlock(BlockStateModel original, ModelModifier.AfterBakeBlock.Context ctx) {
        if (!AreaTextureOverrides.hasAnyOverrides()) return original;

        Map<Island, BlockStateModel> variants = null;

        for (Island island : AreaTextureOverrides.activeIslands()) {
            var baker = new ATOModelBaker(ctx.baker(), material -> remapMaterial(island, material));
            var baked = ctx.sourceModel().bake(ctx.state(), baker);

            if (baker.didRedirect()) {
                if (variants == null) variants = new EnumMap<>(Island.class);
                variants.put(island, baked);
            }
        }

        return variants == null ? original : new ATOBlockStateModel(original, variants);
    }

    private static Material remapMaterial(Island island, Material material) {
        Identifier mapped = AreaTextureOverrides.remap(island, material.sprite());
        return mapped == null ? material : new Material(mapped, material.forceTranslucent());
    }
}
