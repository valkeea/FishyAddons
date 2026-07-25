package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.feature.visual.FaColors;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.PlayerTeam;

@Mixin(PlayerTeam.class)
public class MixinInGameHudSidebar {

    @Inject(method = "getFormattedName", at = @At("TAIL"))
    private void modifySidebarNameText(Component name, CallbackInfoReturnable<Component> cir) {
        FaColors.recolorSidebarText(name);
    }
}
