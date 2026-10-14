package me.valkeea.fishyaddons.feature.visual.ato;

import java.util.function.UnaryOperator;

import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.MaterialBaker;

/**
 * Wraps a {@link MaterialBaker} to redirect specific vanilla materials to ATO
 * sprites while baking a block model. Resolution still goes entirely through the
 * real baker/atlas, but this substitutes which {@link Material} gets looked up.
 */
final class ATOMaterialBaker implements MaterialBaker {
    private final MaterialBaker delegate;
    private final UnaryOperator<Material> redirect;
    private boolean redirected;

    ATOMaterialBaker(MaterialBaker delegate, UnaryOperator<Material> redirect) {
        this.delegate = delegate;
        this.redirect = redirect;
    }

    /** @return true if at least one material lookup was actually redirected to an override. */
    boolean didRedirect() {
        return redirected;
    }

    @Override
    public Material.Baked get(Material material, ModelDebugName debugName) {
        Material mapped = redirect.apply(material);

        if (mapped != material) {
            redirected = true;
        }
        return delegate.get(mapped, debugName);
    }

    @Override
    public Material.Baked reportMissingReference(String reference, ModelDebugName name) {
        return delegate.reportMissingReference(reference, name);
    }  
}
