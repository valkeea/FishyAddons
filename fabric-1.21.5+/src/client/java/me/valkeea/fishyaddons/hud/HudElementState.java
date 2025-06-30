package me.valkeea.fishyaddons.hud;

public class HudElementState {
    public final int x;
    public final int y;
    public final int size;
    public final int color;
    public final boolean outlined;
    public final boolean bg;

    public HudElementState(int x, int y, int size, int color, boolean outlined, boolean bg) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.color = color;
        this.outlined = outlined;
        this.bg = bg;
    }
}