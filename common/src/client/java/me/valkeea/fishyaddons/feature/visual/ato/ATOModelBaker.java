package me.valkeea.fishyaddons.feature.visual.ato;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.resources.Identifier;

/**
 * Wraps a real {@link ModelBaker} to intercept material lookups and redirect them through an {@link ATOMaterialBaker}.
 */
final class ATOModelBaker implements ModelBaker {
    private final ModelBaker delegate;
    private final ATOMaterialBaker materials;

    // Local cache for SharedOperationKey computations to ensure that redirected materials are used
    // instead of the real cache, which can already contain un-redirected results.
    private final Map<ModelBaker.SharedOperationKey<?>, Object> operationCache = new ConcurrentHashMap<>();

    ATOModelBaker(ModelBaker delegate, UnaryOperator<Material> redirect) {
        this.delegate = delegate;
        this.materials = new ATOMaterialBaker(delegate.materials(), redirect);
    }

    /** @return true if at least one material lookup was actually redirected to an override. */
    boolean didRedirect() {
        return materials.didRedirect();
    }

    @Override
    public ResolvedModel getModel(Identifier id) {
        return new ATOResolvedModel(delegate.getModel(id));
    }

    @Override
    public BlockStateModelPart missingBlockModelPart() {
        return delegate.missingBlockModelPart();
    }

    @Override
    public MaterialBaker materials() {
        return materials;
    }

    @Override
    public Interner interner() {
        return delegate.interner();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T compute(SharedOperationKey<T> key) {
        return (T) operationCache.computeIfAbsent(key, k -> ((SharedOperationKey<Object>) k).compute(this));
    }
}
