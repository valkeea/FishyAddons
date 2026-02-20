package me.valkeea.fishyaddons.hud.ui;

import java.awt.Rectangle;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import me.valkeea.fishyaddons.hud.core.HudElement;
import me.valkeea.fishyaddons.hud.core.HudElementState;
import me.valkeea.fishyaddons.mixin.HandledScreenAccessor;
import me.valkeea.fishyaddons.tool.FishyMode;
import me.valkeea.fishyaddons.tracker.collection.CollectionTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Temporary UI text element for showing user feedback
 */
public class UIFeedback implements HudElement {
    
    private Noti current;
    private int x;
    private int y;
    private int color;
    private static UIFeedback instance = null;

    public UIFeedback() {
        this.current = null;
        this.x = 10;
        this.y = 10;
        this.color = FishyMode.getThemeColor();
    }

    public static UIFeedback getInstance() {
        if (instance == null) {
            instance = new UIFeedback();
        }
        return instance;
    }

    public static boolean isEnabled() {
        return CollectionTracker.isEnabled();
    }

     /**
     * Set a temporary notice in a GenericContainerScreen.
     * 
     * @param message The text to display, use newlines for multiple lines.
     * @param duration The duration in ticks for which the notice should be displayed.
     * @param x The X position, or null to auto-position based on the current screen.
     * @param y The Y position, or null to auto-position based on the current screen.
     * @param color Optional color, defaults to the current theme color if null.
     */
    public void set(String message, int duration, @Nullable Integer x,
        @Nullable Integer y, @Nullable Integer color) {

        List<String> lines = List.of(message.split("\n"));

        if (x == null || y == null) {
            var hsa = (HandledScreenAccessor) MinecraftClient.getInstance().currentScreen;
            this.x = x != null ? x : hsa.getX() + 10 + hsa.getBackgroundWidth() + 20;
            this.y = y != null ? y : hsa.getY() + hsa.getBackgroundHeight() / 3;

        } else {
            this.x = x;
            this.y = y;
        }

        if (color != null) this.color = color;
        this.current = new Noti(lines, duration);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY) {
        if (current != null && current.isActive()) {
            current.progress(context, x, y);
        }
    }

    @Override
    public Rectangle getBounds(MinecraftClient mc) {
        if (current == null || !current.isActive()) {
            return new Rectangle(x, y, 0, 0);
        }

        int width = 0;
        int height = mc.textRenderer.fontHeight * current.getLines().size();
        for (String line : current.getLines()) {
            width = Math.max(width, mc.textRenderer.getWidth(line));
        }

        return new Rectangle(x, y, width, height);
    }

    @Override
    public HudElementState getCachedState() {
        return new HudElementState(x, y, 12, color, false, false);
    }

    @Override public void invalidateCache() {/*Not configurable */}
    @Override public boolean isConfigurable() { return false; }

    protected static class Noti {
        private List<String> lines;
        private int duration;
        private int age;
        private boolean active;

        public Noti(List<String> lines, int duration) {
            this.lines = lines;
            this.duration = duration;
            this.age = 0;
            this.active = true;
        }

        public void tick() {
            if (active) {
                age++;
                if (age >= duration) {
                    active = false;
                }
            }
        }

        public boolean isActive() {
            return active;
        }

        public List<String> getLines() {
            return lines;
        }

        public int getAlpha() {
            if (age > duration * 0.8) {
                return (int) (255 * (1 - (age - duration * 0.8) / (duration * 0.2)));
            }
            return 255;
        }

        public void render(DrawContext context, int x, int y) {

            int alpha = getAlpha();
            int color = (alpha << 24) | FishyMode.getThemeColor() & 0xFFFFFF;
            int lineHeight = MinecraftClient.getInstance().textRenderer.fontHeight;

            for (int i = 0; i < lines.size(); i++) {
                context.drawText(
                    MinecraftClient.getInstance().textRenderer,
                    lines.get(i),
                    x,
                    y + i * lineHeight,
                    color,
                    true
                );
            }
        }

        public void progress(DrawContext context, int x, int y) {
            if (!active) return;
            tick();
            render(context, x, y);
        }
    }

    @Override public final int getHudX() { return x; }
    @Override public final int getHudY() { return y; }
    @Override public final void setHudPosition(int x, int y) { this.x = x; this.y = y; }
    @Override public final int getHudSize() { return 12; }
    @Override public final void setHudSize(int size) {/*Fixed */}
    @Override public final int getHudColor() { return FishyMode.getThemeColor(); }
    @Override public final void setHudColor(int color) { this.color = color; }
    @Override public final boolean getHudOutline() { return false; }
    @Override public final void setHudOutline(boolean outline) {/*Fixed */}
    @Override public final boolean getHudBg() { return false; }
    @Override public final void setHudBg(boolean bg) {/*Fixed */}
    @Override public final void setEditingMode(boolean editing) {/*Fixed */}
    @Override public final String getDisplayName() { return "UI Feedback"; }
}
