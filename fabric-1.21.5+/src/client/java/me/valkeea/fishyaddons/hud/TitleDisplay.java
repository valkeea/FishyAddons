package me.valkeea.fishyaddons.hud;

import me.valkeea.fishyaddons.config.FishyConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public class TitleDisplay implements HudElement {
    private boolean editingMode = false;
    private static String title = null;
    private static int titlecolor = 0xFFFFFF;
    private static final String HUD_KEY = "titleHud";
    private static long alertStartTime = 0L;
    private static long alertDurationMs = 2000L;

    public void register() {
        HudLayerRegistrationCallback.EVENT.register(layeredDrawer ->
            layeredDrawer.attachLayerAfter(
                IdentifiedLayer.MISC_OVERLAYS,
                Identifier.of("fishyaddons", "title_hud"),
                (context, tickCounter) -> 
                        render(context, 0, 0)
            )
        );
    }

    public static void setTitle(String t, int color) {
        title = t;
        titlecolor = color;
        alertStartTime = System.currentTimeMillis();
        alertDurationMs = 2000L;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY) {
        boolean showAlert = title != null && !title.isEmpty()
            && (System.currentTimeMillis() - alertStartTime < alertDurationMs);

        if (!editingMode && !showAlert) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        int hudX = FishyConfig.getHudX(HUD_KEY, 5);
        int hudY = FishyConfig.getHudY(HUD_KEY, 5);
        int size = FishyConfig.getHudSize(HUD_KEY, 40);

        float scale = size / 12.0F;
        int textWidth = mc.textRenderer.getWidth(title == null ? "" : title);

        context.getMatrices().push();
        context.getMatrices().translate(hudX, hudY, 0);
        context.getMatrices().scale(scale, scale, 1.0F);
        context.drawText(mc.textRenderer, title, -textWidth / 2, 0, titlecolor, true);
        context.getMatrices().pop();

        if (editingMode) {
            int boxWidth = Math.max(80, textWidth + 8);
            int scaledBoxWidth = (int) (boxWidth * scale);
            context.fill(
                hudX - scaledBoxWidth / 2 - 2,
                hudY - 2,
                hudX + scaledBoxWidth / 2 + 2,
                hudY + (int)(size + 4 * scale),
                0x80FFFFFF
            );
        }
    }

    public static String getTitle() {
        return title;
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