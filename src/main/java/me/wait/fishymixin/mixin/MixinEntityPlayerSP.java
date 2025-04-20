package me.wait.fishymixin.mixin;

import me.wait.fishyaddons.handlers.AliasHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;   
import org.spongepowered.asm.mixin.injection.At;    
import org.spongepowered.asm.mixin.injection.Inject;    
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo; 



@Mixin(EntityPlayerSP.class)
@SideOnly(Side.CLIENT)
public abstract class MixinEntityPlayerSP {

    @Inject(method = "func_71165_d", at = @At("HEAD"), cancellable = true)
    private void onSendChatMessage(String message, CallbackInfo ci) {
        if (!AliasHandler.hasAnyAliases() || !AliasHandler.isAliasSystemEnabled()) return;
        if (message == null || message.trim().isEmpty()) return;

        String original = message.trim();
        if (!original.startsWith("/")) return;

        String remapped = AliasHandler.getActualCommand(original);
        if (remapped != null && !remapped.equals(original)) {
            Minecraft.getMinecraft().thePlayer.sendChatMessage(remapped); // re-send with remapped
            System.out.println("[FA] Alias intercepted: " + original + " → " + remapped);
            ci.cancel(); // Cancel original send
        }
    }
}
