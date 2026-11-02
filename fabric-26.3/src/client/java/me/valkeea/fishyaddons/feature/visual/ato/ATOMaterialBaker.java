package me.valkeea.fishyaddons.feature.visual.ato;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;

import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.client.resources.model.sprite.TextureSlots;

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
        super(placeholderAtlas(missing), placeholderAtlas(missing));
        this.delegate = delegate;
        this.redirect = redirect;
    }

    private static SpriteLoader.Preparations placeholderAtlas(TextureAtlasSprite missing) {
        return new SpriteLoader.Preparations(0, 0, 0, missing, Map.of(), CompletableFuture.completedFuture(null));
    }

    /** @return true if at least one material lookup was actually redirected to an override. */
    boolean didRedirect() {
        return redirected;
    }

    @Override
    public Material.Baked get(Material material, ModelDebugName name) {
        Material mapped = redirect.apply(material);

        if (mapped != material) {
            redirected = true;
        }
        return delegate.get(mapped, name);
    }

    @Override
    public Material.Baked reportMissingReference(String reference, ModelDebugName responsibleModel) {
        return delegate.reportMissingReference(reference, responsibleModel);
    }

    @Override
    public Material.Baked resolveSlot(TextureSlots slots, String id, ModelDebugName name) {
        Material resolved = slots.getMaterial(id);
        return resolved != null ? get(resolved, name) : reportMissingReference(id, name);
    }
}
