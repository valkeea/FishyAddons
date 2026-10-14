package me.valkeea.fishyaddons.feature.visual.ato;

import java.util.List;
import java.util.Map;

import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas;
import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas.Island;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.util.RandomSource;

/**
 * A {@link BlockStateModel} that swaps between pre-baked per-area variants based on the
 * current {@link SkyblockAreas} island.
 */
final class ATOBlockStateModel implements BlockStateModel {
    private final BlockStateModel base;
    private final Map<Island, BlockStateModel> variants;

    ATOBlockStateModel(BlockStateModel base, Map<Island, BlockStateModel> variants) {
        this.base = base;
        this.variants = variants;
    }

    private BlockStateModel active() {
        return variants.getOrDefault(SkyblockAreas.getIsland(), base);
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
        active().collectParts(random, parts);
    }

    @Override
    public Material.Baked particleMaterial() {
        return active().particleMaterial();
    }

    @Override
    public int materialFlags() {
        return active().materialFlags();
    }
}
