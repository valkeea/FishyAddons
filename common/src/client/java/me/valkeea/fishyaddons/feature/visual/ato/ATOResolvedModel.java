package me.valkeea.fishyaddons.feature.visual.ato;

import me.valkeea.fishyaddons.feature.visual.ato.ATOResolvedModel;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;

/**
 * Wraps a {@link ResolvedModel} so that {@link #resolveParticleMaterial} and
 * {@link #bakeTopGeometry} are always recomputed fresh against whichever {@link ModelBaker}
 * is passed in.
 */
final class ATOResolvedModel implements ResolvedModel {
    private final ResolvedModel delegate;

    ATOResolvedModel(ResolvedModel delegate) {
        this.delegate = delegate;
    }

    @Override
    public UnbakedModel wrapped() {
        return delegate.wrapped();
    }

    @Override
    public ResolvedModel parent() {
        ResolvedModel parent = delegate.parent();
        return parent == null ? null : new ATOResolvedModel(parent);
    }

    @Override
    public String debugName() {
        return delegate.debugName();
    }

    @Override
    public Material.Baked resolveParticleMaterial(TextureSlots slots, ModelBaker baker) {
        return ResolvedModel.resolveParticleMaterial(slots, baker, this);
    }

    @Override
    public QuadCollection bakeTopGeometry(TextureSlots slots, ModelBaker baker, ModelState state) {
        return getTopGeometry().bake(slots, baker, state, this);
    }
}
