package me.valkeea.fishyaddons.processor.handlers;

import me.valkeea.fishyaddons.compat.McApi;
import me.valkeea.fishyaddons.feature.skyblock.NpcDialogue;
import me.valkeea.fishyaddons.processor.ChatHandler;
import me.valkeea.fishyaddons.processor.ChatHandlerResult;
import me.valkeea.fishyaddons.processor.ChatMessageContext;

public class ClickEventHandler implements ChatHandler {
    
    @Override
    public int getPriority() {
        return 35;
    }
    
    @Override
    public String getHandlerName() {
        return "ClickEvents";
    }
    
    @Override
    public boolean shouldHandle(ChatMessageContext ctx) {
        return ctx.isSkyblockMessage() && canAccessChatScreen();
    }
    
    @Override
    public ChatHandlerResult handle(ChatMessageContext ctx) {
        try {
            NpcDialogue.checkForCommands(ctx.getOriginalText(), ctx.getLowerCleanString());
            return ChatHandlerResult.CONTINUE;
        } catch (Exception e) {
            System.err.println("[FishyAddons] Error in ClickEvents handler: " + e.getMessage());
            return ChatHandlerResult.SKIP;
        }
    }

    private boolean canAccessChatScreen() {
        var screen = McApi.screen();
        return screen == null || screen instanceof net.minecraft.client.gui.screens.ChatScreen;
    }
    
    @Override
    public boolean isEnabled() {
        return NpcDialogue.enabled();
    }
}
