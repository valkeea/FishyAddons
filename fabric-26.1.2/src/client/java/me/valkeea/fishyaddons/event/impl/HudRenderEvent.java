package me.valkeea.fishyaddons.event.impl;

import me.valkeea.fishyaddons.event.BaseEvent;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class HudRenderEvent extends BaseEvent {
    private final GuiGraphicsExtractor context;
    private final DeltaTracker tickCounter;
    private final Minecraft client;
    private final boolean inScreenContext;

    public HudRenderEvent(GuiGraphicsExtractor context, DeltaTracker tickCounter, Minecraft client, boolean inScreenContext) {
        this.context = context;
        this.tickCounter = tickCounter;
        this.client = client;
        this.inScreenContext = inScreenContext;
    }

    public GuiGraphicsExtractor getContext() {
        return context;
    }

    public DeltaTracker getTickCounter() {
        return tickCounter;
    }

    public Minecraft getClient() {
        return client;
    }

    public boolean isInScreenContext() {
        return inScreenContext;
    }
}
