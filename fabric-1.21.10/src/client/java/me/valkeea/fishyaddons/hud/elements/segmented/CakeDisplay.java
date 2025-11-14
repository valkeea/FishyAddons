package me.valkeea.fishyaddons.hud.elements.segmented;

import java.awt.Rectangle;
import java.util.Map;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.feature.skyblock.CakeTimer;
import me.valkeea.fishyaddons.hud.core.HudDrawer;
import me.valkeea.fishyaddons.hud.core.HudElement;
import me.valkeea.fishyaddons.hud.core.HudElementState;
import me.valkeea.fishyaddons.tool.FishyMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class CakeDisplay implements HudElement {
    private boolean editingMode = false;
    private static final String HUD_KEY = me.valkeea.fishyaddons.config.Key.HUD_CENTURY_CAKE_ENABLED;
    private HudElementState cachedState = null;
    
    public void render(DrawContext context, int mouseX, int mouseY) {
        if (!editingMode && !FishyConfig.getState(HUD_KEY, false)) return;
        
        var timer = CakeTimer.getInstance();
        var mc = MinecraftClient.getInstance();
        var state = getCachedState();
        int hudX = state.x;
        int hudY = state.y;
        int size = state.size;
        int color = state.color;
        boolean showBg = state.bg;

        float scale = size / 12.0F;
        
        String displayText;
        String symbolText = "";
        
        if (editingMode) {
            displayText = "1d 23h 45m";

        } else {
            Map<String, Long> activeCakes = timer.getActiveCakes();
            if (activeCakes.isEmpty()) {
                displayText = "Expired";

            } else {
                String nextCake = timer.getNextExpiringCake();
                symbolText = nextCake != null ? timer.symbol(nextCake) : "";
                long timeLeft = timer.getTimeUntilNextExpiry();
                displayText = CakeTimer.formatTimeLeft(timeLeft);
            }
        }

        Text timerText = Text.literal(displayText);
        Text symbolTextComponent = Text.literal(symbolText);
        int textWidth = mc.textRenderer.getWidth(timerText);
        int symbolWidth = symbolText.isEmpty() ? 0 : mc.textRenderer.getWidth(symbolTextComponent);
        int symbolPadding = symbolText.isEmpty() ? 0 : 2;
        
        int iconSize = (int)(12 * scale);
        int totalWidth = iconSize + 2 + symbolWidth + symbolPadding + textWidth;

        int bgWidth = (int)(totalWidth * scale);
        int bgHeight = (int)(size * 0.8F);
                
        if (showBg) {
            int shadowX1 = hudX + 1;
            int shadowY1 = hudY + 2;
            int shadowX2 = hudX + bgWidth + 2;
            int shadowY2 = hudY + bgHeight - 1;
            context.fill(shadowX1, shadowY1, shadowX2, shadowY2, 0x80000000);
        }

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(hudX, hudY);
        context.getMatrices().scale(scale, scale);

        var cakeTexture = Identifier.of("fishyaddons", "textures/gui/" + FishyMode.getTheme() + "/cake.png");
        context.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            cakeTexture,
            0, -3, 0, 0, 12, 12, 12, 12
        ); 

        int textX = iconSize + 2;

        var drawer = new HudDrawer(mc, context, state);
        if (!symbolText.isEmpty()) {
            drawer.drawText(symbolTextComponent, textX, 0, 0xFF808080);
            textX += symbolWidth + symbolPadding;
        }

        drawer.drawText(timerText, textX, 0, color);
        context.getMatrices().popMatrix();
    }

    @Override
    public Rectangle getBounds(MinecraftClient mc) {
        int hudX = getHudX();
        int hudY = getHudY();
        int size = getHudSize();
        float scale = size / 12.0F;
        int iconSize = (int)(12 * scale);
        
        String sampleText = this.getDisplayName() == null ? "⚡ 1d 23h 45m" : this.getDisplayName();
        int textWidth = mc.textRenderer.getWidth(sampleText);
        int totalWidth = iconSize + 2 + textWidth;
        int width = (int)(Math.max(100, totalWidth) * scale);
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
                FishyConfig.getHudColor(HUD_KEY, 0xFFAA00),
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
    @Override public int getHudColor() { return FishyConfig.getHudColor(HUD_KEY, 0xFFAA00); }
    @Override public void setHudColor(int color) { FishyConfig.setHudColor(HUD_KEY, color); }
    @Override public boolean getHudOutline() { return FishyConfig.getHudOutline(HUD_KEY, false); }
    @Override public void setHudOutline(boolean outline) { FishyConfig.setHudOutline(HUD_KEY, outline); }   
    @Override public boolean getHudBg() { return FishyConfig.getHudBg(HUD_KEY, true); }
    @Override public void setHudBg(boolean bg) { FishyConfig.setHudBg(HUD_KEY, bg); }
    @Override public void setEditingMode(boolean editing) { editingMode = editing; }
    @Override public String getDisplayName() { return "Century Cakes: "; }
}
