package me.valkeea.fishyaddons.event.impl;

import me.valkeea.fishyaddons.event.CancellableChannel;
import me.valkeea.fishyaddons.event.ObservationChannel;
import me.valkeea.fishyaddons.event.ReturnableChannel;
import me.valkeea.fishyaddons.event.StoppableChannel;

public final class FaEvents {
    /** Consuming exits the container click handling with the supplied result. */
    public static final ReturnableChannel<ContainerClickEvent, Boolean> CONTAINER_CLICK = new ReturnableChannel<>();

    /** Consuming cancels vanilla keyboard handling for this input. */
    public static final CancellableChannel<KeyInputEvent> KEYBOARD_INPUT = new CancellableChannel<>();

    public static final StoppableChannel<EnvironmentChangeEvent> ENVIRONMENT_CHANGE = new StoppableChannel<>();
    public static final StoppableChannel<MouseScrollEvent> MOUSE_SCROLL = new StoppableChannel<>();
    public static final StoppableChannel<GameMessageEvent> GAME_MESSAGE = new StoppableChannel<>();
    public static final StoppableChannel<HudRenderEvent> HUD_RENDER = new StoppableChannel<>();
    public static final StoppableChannel<ScCatchEvent> SEA_CREATURE_CATCH = new StoppableChannel<>();
    public static final StoppableChannel<XpGainEvent> XP_GAIN = new StoppableChannel<>();
    public static final StoppableChannel<ContainerChangeEvent> GUI_CHANGE = new StoppableChannel<>();
    public static final StoppableChannel<ScreenCloseEvent> SCREEN_CLOSE = new StoppableChannel<>();
    public static final StoppableChannel<ScreenOpenEvent> SCREEN_OPEN = new StoppableChannel<>();

    /** Observed container mouse clicks. */
    public static final ObservationChannel<HudClickEvent> HUD_CLICK = new ObservationChannel<>();

    private FaEvents() {}
}
