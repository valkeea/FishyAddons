package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import me.valkeea.fishyaddons.handler.FaColors;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;

@Mixin(PlayerListHud.class)
public class MixinPlayerListHud {

    @Redirect(
        method = "render(Lnet/minecraft/client/gui/DrawContext;ILnet/minecraft/scoreboard/Scoreboard;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/hud/PlayerListHud;getPlayerName(Lnet/minecraft/client/network/PlayerListEntry;)Lnet/minecraft/text/Text;"
        )
    )
    private Text rewriteTablistName(PlayerListHud inst, PlayerListEntry entry) {
        if (FaColors.shouldColor()) {
            Text original = inst.getPlayerName(entry);
            return FaColors.multipleCached(original);
        }
        return inst.getPlayerName(entry);
    }
}