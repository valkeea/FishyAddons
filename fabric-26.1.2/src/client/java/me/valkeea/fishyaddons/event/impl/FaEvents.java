package me.valkeea.fishyaddons.event.impl;

import me.valkeea.fishyaddons.event.FaEventDispatcher;

@SuppressWarnings("squid:S6539") // Intentional registry pattern
public final class FaEvents {
    
    public static final FaEventDispatcher<EnvironmentChangeListener> ENVIRONMENT_CHANGE = new FaEventDispatcher<>();
    public static final FaEventDispatcher<ScreenClickListener> SCREEN_MOUSE_CLICK = new FaEventDispatcher<>();
    public static final FaEventDispatcher<MouseClickListener> MOUSE_CLICK = new FaEventDispatcher<>();
    public static final FaEventDispatcher<MouseScrollListener> MOUSE_SCROLL = new FaEventDispatcher<>();
    public static final FaEventDispatcher<GameMessageListener> GAME_MESSAGE = new FaEventDispatcher<>();
    public static final FaEventDispatcher<HudRenderListener> HUD_RENDER = new FaEventDispatcher<>();
    public static final FaEventDispatcher<ScCatchListener> SEA_CREATURE_CATCH = new FaEventDispatcher<>();
    public static final FaEventDispatcher<XpGainListener> XP_GAIN = new FaEventDispatcher<>();
    public static final FaEventDispatcher<GuiChangeListener> GUI_CHANGE = new FaEventDispatcher<>();
    public static final FaEventDispatcher<ScreenCloseListener> SCREEN_CLOSE = new FaEventDispatcher<>();
    public static final FaEventDispatcher<ScreenOpenListener> SCREEN_OPEN = new FaEventDispatcher<>();

    public interface EnvironmentChangeListener {
        void onEnvironmentChange(EnvironmentChangeEvent event);
    }

    public interface GuiChangeListener {
        void onGuiChange(GuiChangeEvent event);
    }

    public interface ScreenCloseListener {
        void onScreenClose(ScreenCloseEvent event);
    }

    public interface ScreenOpenListener {
        void onScreenOpen(ScreenOpenEvent event);
    }    
        
    public interface ScreenClickListener {
        void onClick(ScreenClickEvent event);
    }

    public interface MouseClickListener {
        void onClick(MouseClickEvent event);
    }

    public interface MouseScrollListener {
        void onScroll(MouseScrollEvent event);
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

    public interface XpGainListener {
        void onXpGain(XpGainEvent event);
    }

    private FaEvents() {}
}
