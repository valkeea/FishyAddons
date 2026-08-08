package me.valkeea.fishyaddons.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import me.valkeea.fishyaddons.api.skyblock.GameMode;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.PackRepository;

@Mixin(PackRepository.class)
public class MixinPackRepository {

    @ModifyReturnValue(
        method = "openAllSelected",
        at = @At("RETURN")
    )
    private List<PackResources> modify(List<PackResources> original) {
        if (!(GameMode.onHypixel() && Config.get(BooleanKey.REORDER_PACKS))) return original;

        var reordered = new ArrayList<PackResources>(original.size());
        var server = original.stream()
            .filter(MixinPackRepository::isServerPack)
            .toList();

        reordered.addAll(server);
        original.stream()
            .filter(pack -> !isServerPack(pack))
            .forEach(reordered::add);

        return reordered;
    }

    private static boolean isServerPack(PackResources pack) {
        return pack.packId().startsWith("server/");
    }
}
