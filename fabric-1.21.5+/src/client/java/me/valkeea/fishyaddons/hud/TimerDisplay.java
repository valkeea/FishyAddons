package me.valkeea.fishyaddons.hud;

import java.awt.Rectangle;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.handler.ChatTimers;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class TimerDisplay implements HudElement {
    private boolean editingMode = false;
    private static final String HUD_KEY = me.valkeea.fishyaddons.config.Key.HUD_TIMER_ENABLED;
    private HudElementState cachedState = null;

    public void register() {
        HudLayerRegistrationCallback.EVENT.register(layeredDrawer ->
            layeredDrawer.attachLayerAfter(
                IdentifiedLayer.MISC_OVERLAYS,
                Identifier.of("fishyaddons", "timer_hud"),
                (context, tickCounter) -> 
                        render(context, 0, 0)
            )
        );
    }
    
    public void render(DrawContext context, int mouseX, int mouseY) {
        if (!editingMode && (!ChatTimers.getInstance().isBeaconAlarmHudOn() || 
            !ChatTimers.getInstance().isBeaconActive())) return;
        long secondsLeft = ChatTimers.getInstance().getBeaconTimer();
        if (secondsLeft < 0) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        HudElementState state = getCachedState();
        int hudX = state.x;
        int hudY = state.y;
        int size = state.size;
        int color = state.color;
        boolean outlined = state.outlined;
        boolean showBg = state.bg;

        float scale = size / 12.0F;
        Text timerLabel = Text.literal("Moonglade:");
        Text timerValue = Text.literal(" " + formatTime(secondsLeft));

        int labelWidth = mc.textRenderer.getWidth(timerLabel);
        int valueWidth = mc.textRenderer.getWidth(timerValue);
        int textWidth = labelWidth + valueWidth;

        if (editingMode || showBg) {
            int shadowX1 = hudX + 1;
            int shadowY1 = hudY + 2;
            int shadowX2 = hudX + (int)(textWidth * scale) + 2;
            int shadowY2 = hudY + (int)(size * 0.8F) - 1;
            context.fill(shadowX1, shadowY1, shadowX2, shadowY2, 0x80000000);
        }

        context.getMatrices().push();
        context.getMatrices().translate(hudX, hudY, 0);
        context.getMatrices().scale(scale, scale, 1.0F);

        if (outlined) {
            me.valkeea.fishyaddons.util.TextUtils.drawOutlinedText(
                context,
                mc.textRenderer,
                timerLabel,
                0, 0,
                color,
                0xFF000000,
                context.getMatrices().peek().getPositionMatrix(),
                MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers(),
                net.minecraft.client.font.TextRenderer.TextLayerType.NORMAL,
                0xF000F0
            );
            me.valkeea.fishyaddons.util.TextUtils.drawOutlinedText(
                context,
                mc.textRenderer,
                timerValue,
                labelWidth, 0,
                0xFFFFFF,
                0xFF000000,
                context.getMatrices().peek().getPositionMatrix(),
                MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers(),
                net.minecraft.client.font.TextRenderer.TextLayerType.NORMAL,
                0xF000F0
            );
        } else {
            context.drawText(mc.textRenderer, timerLabel, 0, 0, color, true);
            context.drawText(mc.textRenderer, timerValue, labelWidth, 0, 0xFFFFFF, true);
        }
        context.getMatrices().pop();

        if (editingMode) {
            context.fill(hudX - 2, hudY - 2, hudX + (int)(textWidth * scale) + 18, hudY + (int)(size * 1.1F) + 4, 0x80FFFFFF);
        }
    }
 
    public static String formatTime(long seconds) {
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        return String.format("%02d:%02d", minutes, remainingSeconds);
    }

    @Override
    public Rectangle getBounds(MinecraftClient mc) {
        int hudX = getHudX();
        int hudY = getHudY();
        int size = getHudSize();
        float scale = size / 12.0F;
        int textWidth = mc.textRenderer.getWidth(this.getDisplayName() == null ? "" : this.getDisplayName());
        int width = (int)(Math.max(80, textWidth) * scale);
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
    @Override public int getHudColor() { return FishyConfig.getHudColor(HUD_KEY, 0xC8D9C0); }
    @Override public void setHudColor(int color) { FishyConfig.setHudColor(HUD_KEY, color); }
    @Override public boolean getHudOutline() { return FishyConfig.getHudOutline(HUD_KEY, false); }
    @Override public void setHudOutline(boolean outline) { FishyConfig.setHudOutline(HUD_KEY, outline); }   
    @Override public boolean getHudBg() { return FishyConfig.getHudBg(HUD_KEY, true); }
    @Override public void setHudBg(boolean bg) { FishyConfig.setHudBg(HUD_KEY, bg); }
    @Override public void setEditingMode(boolean editing) { editingMode = editing; }
    @Override public String getDisplayName() { return  "Moonglade: ";  }
}