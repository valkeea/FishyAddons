package me.valkeea.fishyaddons.mixin;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(ChatHud.class)
public interface ChatHudAccessor {
    @Accessor("visibleMessages")
    List<ChatHudLine.Visible> getVisibleMessages();

    @Accessor("scrolledLines")
    int getScrolledLines();

    @Accessor("messages")
    List<ChatHudLine> getMessages();

    @Invoker("toChatLineX")
    double invokeToChatLineX(double x);

    @Invoker("toChatLineY")
    double invokeToChatLineY(double y);

    @Invoker("getMessageLineIndex")
    int invokeGetMessageLineIndex(double chatLineX, double chatLineY);
}
