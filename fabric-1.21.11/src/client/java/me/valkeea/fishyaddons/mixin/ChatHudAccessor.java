package me.valkeea.fishyaddons.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;

@Mixin(ChatHud.class)
public interface ChatHudAccessor {
    @Accessor("visibleMessages")
    List<ChatHudLine.Visible> getVisibleMessages();

    @Accessor("scrolledLines")
    int getScrolledLines();

    @Accessor("messages")
    List<ChatHudLine> getMessages();

    @Accessor("client")
    net.minecraft.client.MinecraftClient getClient();

    @Invoker("getWidth")
    int invokeGetWidth();

    @Invoker("getHeight")
    int invokeGetHeight();

    @Invoker("getChatScale")
    double invokeGetChatScale();

    @Invoker("getLineHeight")
    int invokeGetLineHeight();

    @Invoker("isChatFocused")
    boolean invokeIsChatFocused();
}
