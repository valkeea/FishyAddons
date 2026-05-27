package me.valkeea.fishyaddons.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;

@Mixin(ChatComponent.class)
public interface ChatHudAccessor {
    @Accessor("trimmedMessages")
    List<GuiMessage.Line> getVisibleMessages();

    @Accessor("chatScrollbarPos")
    int getScrolledLines();

    @Accessor("allMessages")
    List<GuiMessage> getMessages();

    @Accessor("minecraft")
    net.minecraft.client.Minecraft getClient();

    @Invoker("getWidth")
    int invokeGetWidth();

    @Invoker("getHeight")
    int invokeGetHeight();

    @Invoker("getScale")
    double invokeGetChatScale();

    @Invoker("getLineHeight")
    int invokeGetLineHeight();

    @Invoker("isChatFocused")
    boolean invokeIsChatFocused();
}
