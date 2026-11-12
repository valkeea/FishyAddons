package me.valkeea.fishyaddons.feature.qol.input;

final class SlotGesture<S, T> {
    record Selection<T>(T start, T end) {}

    private S screen;
    private T start;
    private boolean wasPressed;

    Selection<T> update(S currentScreen, T hovered, boolean allowed, boolean pressed) {
        if (screen != currentScreen) {
            start = null;
            screen = currentScreen;
        }

        if (pressed && !wasPressed && allowed) start = hovered;
        Selection<T> result = null;
        if (!pressed && wasPressed) {
            if (start != null && hovered != null) result = new Selection<>(start, hovered);
            start = null;
        }
        wasPressed = pressed;
        return result;
    }

    void cancel(boolean pressed) {
        screen = null;
        start = null;
        wasPressed = pressed;
    }
}
