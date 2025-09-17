package me.valkeea.fishyaddons.hud;

import java.awt.Rectangle;
import java.util.Map;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.handler.CakeTimer;
import me.valkeea.fishyaddons.tool.FishyMode;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class CakeDisplay implements HudElement {
    private boolean editingMode = false;
    private static final String HUD_KEY = me.valkeea.fishyaddons.config.Key.HUD_CENTURY_CAKE_ENABLED;
    private HudElementState cachedState = null;

    public void register() {
        HudLayerRegistrationCallback.EVENT.register(layeredDrawer ->
            layeredDrawer.attachLayerAfter(
                IdentifiedLayer.MISC_OVERLAYS,
                Identifier.of("fishyaddons", "century_cake_hud"),
                (context, tickCounter) -> 
                        render(context, 0, 0)
            )
        );
    }
    
    public void render(DrawContext context, int mouseX, int mouseY) {
        if (!editingMode && !FishyConfig.getState(HUD_KEY, false)) return;
        
        CakeTimer timer = CakeTimer.getInstance();

        MinecraftClient mc = MinecraftClient.getInstance();
        HudElementState state = getCachedState();
        int hudX = state.x;
        int hudY = state.y;
        int size = state.size;
        int color = state.color;
        boolean outlined = state.outlined;
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

        Identifier cakeTexture = Identifier.of("fishyaddons", "textures/gui/" + FishyMode.getTheme() + "/cake.png");
        context.drawTexture(
            RenderLayer::getGuiTextured,
            cakeTexture,
            0, -3, 0, 0, 12, 12, 12, 12
        );

        int textX = iconSize + 2;

        if (!symbolText.isEmpty()) {
            if (outlined) {
                me.valkeea.fishyaddons.util.text.TextUtils.drawOutlinedText(
                    context,
                    mc.textRenderer,
                    symbolTextComponent,
                    textX, 0,
                    0x808080,
                    0xFF000000,
                    context.getMatrices().peek().getPositionMatrix(),
                    MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers(),
                    net.minecraft.client.font.TextRenderer.TextLayerType.NORMAL,
                    0xF000F0
                );
            } else {
                context.drawText(mc.textRenderer, symbolTextComponent, textX, 0, 0x808080, true);
            }
            textX += symbolWidth + symbolPadding;
        }

        if (outlined) {
            me.valkeea.fishyaddons.util.text.TextUtils.drawOutlinedText(
                context,
                mc.textRenderer,
                timerText,
                textX, 0,
                color,
                0xFF000000,
                context.getMatrices().peek().getPositionMatrix(),
                MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers(),
                net.minecraft.client.font.TextRenderer.TextLayerType.NORMAL,
                0xF000F0
            );
        } else {
            context.drawText(mc.textRenderer, timerText, textX, 0, color, true);
        }
        context.getMatrices().pop();

        if (editingMode) {
            context.fill(hudX - 2, hudY - 2, hudX + (int)(totalWidth * scale) + 18, hudY + (int)(size * 1.1F) + 4, 0x80FFFFFF);
        }
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
