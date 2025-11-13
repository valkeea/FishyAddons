package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.Style;

@Mixin(ChatScreen.class)
public interface ChatScreenAccessor {
    @Invoker("getTextStyleAt")
    Style invokeGetTextStyleAt(double x, double y);
}
