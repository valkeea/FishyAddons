package me.valkeea.fishyaddons.feature.visual.ato;

import java.util.function.UnaryOperator;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.MaterialBaker;

/**
 * Wraps a {@link MaterialBaker} to redirect specific vanilla materials to ATO
 * sprites while baking a block model. Resolution still goes entirely through the
 * real baker/atlas, but this substitutes which {@link Material} gets looked up.
 */
final class ATOMaterialBaker extends MaterialBaker {
    private final MaterialBaker delegate;
    private final UnaryOperator<Material> redirect;
    private boolean redirected;

    ATOMaterialBaker(MaterialBaker delegate, TextureAtlasSprite missing, UnaryOperator<Material> redirect) {
        super(missing);
        this.delegate = delegate;
        this.redirect = redirect;
    }

    /** @return true if at least one material lookup was actually redirected to an override. */
    boolean didRedirect() {
        return redirected;
    }

    // 26.2: get(Material, ModelDebugName) uses a method reference to bake(Material)
    @Override
    protected Material.Baked bake(Material material) {
        Material mapped = redirect.apply(material);

        if (mapped != material) {
            redirected = true;
        }
        return delegate.get(mapped, new ATODebugName());
    }

    public static class ATODebugName implements ModelDebugName {
        @Override
        public String debugName() {
            return "ATOMaterialBaker";
        }
    }
}
