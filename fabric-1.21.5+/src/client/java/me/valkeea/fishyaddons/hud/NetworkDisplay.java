package me.valkeea.fishyaddons.hud;

import java.awt.Rectangle;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.handler.NetworkMetrics;
import me.valkeea.fishyaddons.util.text.Color;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class NetworkDisplay implements HudElement {
    private static final String HUD_KEY = me.valkeea.fishyaddons.config.Key.HUD_PING_ENABLED;
    private HudElementState cachedState = null;
    private boolean editingMode = false;
    private int lastPing = -1;
    private double lastTps = -1;
    private int lastFps = -1;
    
    private Text cachedPingLabel = Text.literal("Ping:");
    private Text cachedPingValue = Text.literal(" ?");
    private Text cachedTpsLabel = Text.literal("TPS:");
    private Text cachedTpsValue = Text.literal(" ?");
    private Text cachedFpsLabel = Text.literal("FPS:");
    private Text cachedFpsValue = Text.literal(" ?");
    
    private int totalWidth = -1;

    public void register() {
        HudLayerRegistrationCallback.EVENT.register(layeredDrawer ->
            layeredDrawer.attachLayerAfter(
                IdentifiedLayer.MISC_OVERLAYS,
                Identifier.of("fishyaddons", "network_hud"),
                (context, tickCounter) -> render(context, 0, 0)
            )
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY) {
        if (!editingMode && !NetworkMetrics.isOn()) return;

        var mc = MinecraftClient.getInstance();
        
        int ping = NetworkMetrics.getPing();
        double tps = NetworkMetrics.getTps();
        int fps = mc.getCurrentFps();
        
        boolean showPing = NetworkMetrics.shouldDisplay(Key.HUD_PING_SHOW_PING);
        boolean showTps = NetworkMetrics.shouldDisplay(Key.HUD_PING_SHOW_TPS);
        boolean showFps = NetworkMetrics.shouldDisplay(Key.HUD_PING_SHOW_FPS);

        if (!editingMode && !showPing && !showTps && !showFps) return;
        if (!editingMode && showPing && ping < 0) return;

        boolean needsUpdate = false;
        if (showPing && ping != lastPing) {
            lastPing = ping;
            cachedPingValue = Text.literal(ping >= 0 ? " " + ping + " ms" : " ?");
            needsUpdate = true;
        }
        if (showTps && Math.abs(tps - lastTps) > 0.1) {
            lastTps = tps;
            cachedTpsValue = Text.literal(" " + NetworkMetrics.getTpsString());
            needsUpdate = true;
        }
        if (showFps && fps != lastFps) {
            lastFps = fps;
            cachedFpsValue = Text.literal(" " + fps);
            needsUpdate = true;
        }

        drawDisplays(context, mc, needsUpdate, showPing, showTps, showFps);
    }

    private void drawDisplays(DrawContext context, MinecraftClient mc, boolean needsUpdate, boolean showPing, boolean showTps, boolean showFps) {

        if (needsUpdate || totalWidth == -1) {
            calculateLayout(mc, showPing, showTps, showFps);
        }        

        HudElementState state = getCachedState();
        int hudX = state.x;
        int hudY = state.y;
        int size = state.size;
        int color = state.color;
        boolean showBg = state.bg;

        float scale = size / 12.0F;        

        if (editingMode || showBg) {
            int shadowX1 = hudX + 1;
            int shadowY1 = hudY + 2;
            int shadowX2 = hudX + (int)(totalWidth * scale) + 2;
            int shadowY2 = hudY + (int)(size * 0.8F) - 1;
            context.fill(shadowX1, shadowY1, shadowX2, shadowY2, 0x80000000);
        }

        context.getMatrices().push();
        context.getMatrices().translate(hudX, hudY, 0);
        context.getMatrices().scale(scale, scale, 1.0F);

        var hudRenderer = new HudVisuals(mc, context, state);
        drawMetrics(hudRenderer, mc, showPing, showTps, showFps, color);

        context.getMatrices().pop();

        if (editingMode) {
            context.fill(hudX - 2, hudY - 2, hudX + (int)(totalWidth * scale) + 18, hudY + (int)(size * 1.1F) + 4, 0x80FFFFFF);
        }
    }

    private void drawMetrics(HudVisuals hudRenderer, MinecraftClient mc, boolean showPing, boolean showTps, boolean showFps, int color) {
        int xOffset = 0;
        int valueColor = Color.brighten(color, 0.7f);
        if (showPing || editingMode) {
            hudRenderer.drawText(cachedPingLabel, xOffset, 0, color);
            xOffset += mc.textRenderer.getWidth(cachedPingLabel);
            int pingColor = showPing ? valueColor : 0x808080;
            hudRenderer.drawText(cachedPingValue, xOffset, 0, pingColor);
            xOffset += mc.textRenderer.getWidth(cachedPingValue) + 10;
        }

        if (showTps || editingMode) {
            hudRenderer.drawText(cachedTpsLabel, xOffset, 0, color);
            xOffset += mc.textRenderer.getWidth(cachedTpsLabel);
            int tpsColor = showTps ? valueColor : 0x808080;
            hudRenderer.drawText(cachedTpsValue, xOffset, 0, tpsColor);
            xOffset += mc.textRenderer.getWidth(cachedTpsValue) + 10;
        }

        if (showFps || editingMode) {
            hudRenderer.drawText(cachedFpsLabel, xOffset, 0, color);
            xOffset += mc.textRenderer.getWidth(cachedFpsLabel);
            int fpsColor = showFps ? valueColor : 0x808080;
            hudRenderer.drawText(cachedFpsValue, xOffset, 0, fpsColor);
        }
    }

    private void calculateLayout(MinecraftClient mc, boolean showPing, boolean showTps, boolean showFps) {
        int width = 0;
        
        if (showPing || editingMode) {
            width += mc.textRenderer.getWidth(cachedPingLabel);
            width += mc.textRenderer.getWidth(cachedPingValue);
            if (showTps || showFps || editingMode) width += 10;
        }
        
        if (showTps || editingMode) {
            width += mc.textRenderer.getWidth(cachedTpsLabel);
            width += mc.textRenderer.getWidth(cachedTpsValue);
            if (showFps || editingMode) width += 10;
        }
        
        if (showFps || editingMode) {
            width += mc.textRenderer.getWidth(cachedFpsLabel);
            width += mc.textRenderer.getWidth(cachedFpsValue);
        }
        
        totalWidth = width;
    }

    @Override
    public Rectangle getBounds(MinecraftClient mc) {
        int hudX = getHudX();
        int hudY = getHudY();
        int size = getHudSize();
        float scale = size / 12.0F;
        
        boolean showPing = NetworkMetrics.shouldDisplay(Key.HUD_PING_SHOW_PING);
        boolean showTps = NetworkMetrics.shouldDisplay(Key.HUD_PING_SHOW_TPS);
        boolean showFps = NetworkMetrics.shouldDisplay(Key.HUD_PING_SHOW_FPS);
        
        int estimatedWidth = 0;
        if (showPing || editingMode) estimatedWidth += 80;
        if (showTps || editingMode) estimatedWidth += 70;
        if (showFps || editingMode) estimatedWidth += 60;

        int width = (int)(Math.max(100, estimatedWidth) * scale);
        int height = (int)(size + 4 * scale);

        return new Rectangle(hudX, hudY, width, height);
    }   
    
    @Override
    public HudElementState getCachedState() {
        if (cachedState == null) {
            cachedState = new HudElementState(
                FishyConfig.getHudX(HUD_KEY, 5),
                FishyConfig.getHudY(HUD_KEY, 5),
                FishyConfig.getHudSize(HUD_KEY, 12),
                FishyConfig.getHudColor(HUD_KEY, 0xFFFFFF),
                FishyConfig.getHudOutline(HUD_KEY, false),
                FishyConfig.getHudBg(HUD_KEY, true)
            );
        }
        return cachedState;
    }

    @Override
    public void invalidateCache() {
        cachedState = null;
    }

    @Override public int getHudX() { return FishyConfig.getHudX(HUD_KEY, 5); }
    @Override public int getHudY() { return FishyConfig.getHudY(HUD_KEY, 5); }
    @Override public void setHudPosition(int x, int y) { FishyConfig.setHudX(HUD_KEY, x); FishyConfig.setHudY(HUD_KEY, y); }
    @Override public int getHudSize() { return FishyConfig.getHudSize(HUD_KEY, 12); }
    @Override public void setHudSize(int size) { FishyConfig.setHudSize(HUD_KEY, size); }
    @Override public int getHudColor() { return FishyConfig.getHudColor(HUD_KEY, 0xEECAEC); }
    @Override public void setHudColor(int color) { FishyConfig.setHudColor(HUD_KEY, color); }
    @Override public boolean getHudOutline() { return FishyConfig.getHudOutline(HUD_KEY, false); }
    @Override public void setHudOutline(boolean outline) { FishyConfig.setHudOutline(HUD_KEY, outline); }   
    @Override public boolean getHudBg() { return FishyConfig.getHudBg(HUD_KEY, true); }
    @Override public void setHudBg(boolean bg) { FishyConfig.setHudBg(HUD_KEY, bg); }
    @Override public void setEditingMode(boolean editing) { this.editingMode = editing; }
    @Override public String getDisplayName() { return "Network Display"; }
}