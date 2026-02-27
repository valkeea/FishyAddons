package me.valkeea.fishyaddons.event.impl;

import me.valkeea.fishyaddons.event.BaseEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class HudRenderEvent extends BaseEvent {
    private final DrawContext context;
    private final RenderTickCounter tickCounter;
    private final MinecraftClient client;
    private final boolean inScreenContext;

    public HudRenderEvent(DrawContext context, RenderTickCounter tickCounter, MinecraftClient client, boolean inScreenContext) {
        this.context = context;
        this.tickCounter = tickCounter;
        this.client = client;
        this.inScreenContext = inScreenContext;
    }

    public DrawContext getContext() {
        return context;
    }

    public RenderTickCounter getTickCounter() {
        return tickCounter;
    }

    public MinecraftClient getClient() {
        return client;
    }

    public boolean isInScreenContext() {
        return inScreenContext;
    }
}
