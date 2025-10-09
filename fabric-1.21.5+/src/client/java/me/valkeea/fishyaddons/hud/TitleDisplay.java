package me.valkeea.fishyaddons.hud;

import java.awt.Rectangle;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.ui.GuiUtil;
import me.valkeea.fishyaddons.util.text.Enhancer;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class TitleDisplay implements HudElement {
    private boolean editingMode = false;
    private static String title = null;
    private static int titlecolor = 0xFFFFFF;
    private static final String HUD_KEY = "titleHud";
    private static long alertStartTime = 0L;
    private static long alertDurationMs = 2000L;
    private HudElementState cachedState;    

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
        var formatted = title == null ? Text.empty() : Enhancer.parseFormattedText(title);
        int textWidth = mc.textRenderer.getWidth(formatted.getString());

        context.getMatrices().push();
        context.getMatrices().translate(hudX, hudY, 0);
        context.getMatrices().scale(scale, scale, 1.0F);
        context.drawText(mc.textRenderer, formatted, -textWidth / 2, 0, titlecolor, true);
        context.getMatrices().pop();

        if (editingMode) {
            Rectangle bounds = getBounds(MinecraftClient.getInstance());
            GuiUtil.drawBox(
                context,
                bounds.x - 2,
                bounds.y - 2,
                bounds.width + 4,
                (int)(size + 4 * scale),
                0x80FFFFFF
            );
            context.drawText(
                mc.textRenderer,
                "Alert Title",
                hudX - bounds.width / 2,
                hudY + 2,
                0xFFFFFF,
                false
            );
        }
    }

    public static String getTitle() {
        return title;
    }
    
    @Override
    public Rectangle getBounds(MinecraftClient mc) {
        int hudX = getHudX();
        int hudY = getHudY();
        int size = getHudSize();
        float scale = size / 20.0F;
        int textWidth = mc.textRenderer.getWidth(title == null ? "" : title);
        int boxWidth = Math.max(80, textWidth + 8);
        int scaledBoxWidth = (int) (boxWidth * scale);
        int height = (int)(size + 4 * scale);
        return new Rectangle(hudX - scaledBoxWidth / 2 - 2, hudY - 2, scaledBoxWidth + 4, height);
    } 

    @Override
    public void invalidateCache() {
        cachedState = null;
    }    

    @Override
    public HudElementState getCachedState() {
        if (cachedState == null) {
            cachedState = new HudElementState(
                FishyConfig.getHudX(HUD_KEY, 5),
                FishyConfig.getHudY(HUD_KEY, 5),
                FishyConfig.getHudSize(HUD_KEY, 40),
                0,
                FishyConfig.getHudOutline(HUD_KEY, false),
                FishyConfig.getHudBg(HUD_KEY, true)
            );
        }
        return cachedState;
    }    

    @Override public int getHudX() { return FishyConfig.getHudX(HUD_KEY, 5); }
    @Override public int getHudY() { return FishyConfig.getHudY(HUD_KEY, 5); }
    @Override public void setHudPosition(int x, int y) { FishyConfig.setHudX(HUD_KEY, x); FishyConfig.setHudY(HUD_KEY, y); }
    @Override public int getHudSize() { return FishyConfig.getHudSize(HUD_KEY, 12); }
    @Override public void setHudSize(int size) { FishyConfig.setHudSize(HUD_KEY, size); }
    @Override public int getHudColor() { return FishyConfig.getHudColor(HUD_KEY, 0xFFFFFF); }
    @Override public void setHudColor(int color) { FishyConfig.setHudColor(HUD_KEY, color); }
    @Override public boolean getHudOutline() { return FishyConfig.getHudOutline(HUD_KEY, false); }
    @Override public void setHudOutline(boolean outline) { FishyConfig.setHudOutline(HUD_KEY, outline); }   
    @Override public boolean getHudBg() { return FishyConfig.getHudBg(HUD_KEY, true); }
    @Override public void setHudBg(boolean bg) { FishyConfig.setHudBg(HUD_KEY, bg); }
    @Override public void setEditingMode(boolean editing) { this.editingMode = editing; }
    @Override public String getDisplayName() { return "Title HUD"; }
}