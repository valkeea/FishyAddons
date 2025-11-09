package me.valkeea.fishyaddons.event.impl;

import me.valkeea.fishyaddons.event.FaEventDispatcher;

public final class FaEvents {  
    public static final FaEventDispatcher<ScreenClickListener> SCREEN_MOUSE_CLICK = new FaEventDispatcher<>();
    public static final FaEventDispatcher<GameMessageListener> GAME_MESSAGE = new FaEventDispatcher<>();
    public static final FaEventDispatcher<HudRenderListener> HUD_RENDER = new FaEventDispatcher<>();
    public static final FaEventDispatcher<ScCatchListener> SEA_CREATURE_CATCH = new FaEventDispatcher<>();

    public interface ScreenClickListener {
        void onClick(ScreenClickEvent event);
    }

    public interface GameMessageListener {
        void onGameMessage(GameMessageEvent event);
    }

    public interface HudRenderListener {
        void onHudRender(HudRenderEvent event);
    }

    public interface ScCatchListener {
        void onScCatch(ScCatchEvent event);
    }

    private FaEvents() {}
}