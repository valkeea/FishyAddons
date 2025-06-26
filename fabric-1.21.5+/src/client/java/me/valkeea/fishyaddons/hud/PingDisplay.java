package me.valkeea.fishyaddons.hud;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.handler.ClientPing;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class PingDisplay implements HudElement {
    private boolean editingMode = false;
    private static final String HUD_KEY = "pingHud";

    public void register() {
        HudLayerRegistrationCallback.EVENT.register(layeredDrawer ->
            layeredDrawer.attachLayerAfter(
                IdentifiedLayer.MISC_OVERLAYS,
                Identifier.of("fishyaddons", "ping_hud"),
                (context, tickCounter) -> {
                        render(context, 0, 0);
                }
            )
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY) {
        if (!editingMode && !FishyConfig.getState(HUD_KEY, false)) return;
        int ping = ClientPing.get();
        if (ping < 0) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        int hudX = FishyConfig.getHudX(HUD_KEY, 5);
        int hudY = FishyConfig.getHudY(HUD_KEY, 5);
        int size = FishyConfig.getHudSize(HUD_KEY, 12);
        int color = FishyConfig.getHudColor(HUD_KEY, 0xFFFFFF);

        float scale = size / 12.0F;

        Text pingLabel = Text.literal("Ping:").styled(style -> style.withColor(color));
        Text pingValue = Text.literal(" " + ping + " ms");
        Text fullText = Text.empty().append(pingLabel).append(pingValue);

        int textWidth = mc.textRenderer.getWidth(fullText);

        // Draw shadow box with fixed offset (not scaled)
        int shadowX1 = hudX + 1;
        int shadowY1 = hudY + 2;
        int shadowX2 = hudX + (int)(textWidth * scale) + 2;
        int shadowY2 = hudY + (int)(size * 0.8F) - 1;
        context.fill(shadowX1, shadowY1, shadowX2, shadowY2, 0x80000000);

        // Draw scaled text
        context.getMatrices().push();
        context.getMatrices().translate(hudX, hudY, 0);
        context.getMatrices().scale(scale, scale, 1.0F);
        context.drawText(mc.textRenderer, fullText, 0, 0, 0xFFFFFF, true);
        context.getMatrices().pop();

        if (editingMode) {
            context.fill(hudX - 2, hudY - 2, hudX + (int)(textWidth * scale) + 18, hudY + (int)(size * 1.1F) + 4, 0x80FFFFFF);
        }
    }

    @Override public int getHudX() { return FishyConfig.getHudX(HUD_KEY, 5); }
    @Override public int getHudY() { return FishyConfig.getHudY(HUD_KEY, 5); }
    @Override public void setHudPosition(int x, int y) { FishyConfig.setHudX(HUD_KEY, x); FishyConfig.setHudY(HUD_KEY, y); }
    @Override public int getHudSize() { return FishyConfig.getHudSize(HUD_KEY, 12); }
    @Override public void setHudSize(int size) { FishyConfig.setHudSize(HUD_KEY, size); }
    @Override public int getHudColor() { return FishyConfig.getHudColor(HUD_KEY, 0xFFFFFF); }
    @Override public void setHudColor(int color) { FishyConfig.setHudColor(HUD_KEY, color); }
    @Override public void setEditingMode(boolean editing) { this.editingMode = editing; }
    @Override public String getDisplayName() { return "Ping HUD"; }
}