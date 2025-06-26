package me.valkeea.fishyaddons.hud;

import me.valkeea.fishyaddons.tool.FishyMode;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class FishyToast implements Toast {
    private long startTime = -1;
    private final String title;
    private final String message;
    private static FishyToast currentToast = null;

    public FishyToast(String title, String message) {
        this.title = title;
        this.message = message;
    }

    @Override
    public void draw(DrawContext context, TextRenderer textRenderer, long startTime) {}

    @Override
    public Visibility getVisibility() {
        if (startTime < 0) return Toast.Visibility.SHOW;
        return (System.currentTimeMillis() - startTime) >= 4000L ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
    }

    @Override
    public void update(ToastManager manager, long time) {
        if (startTime < 0) startTime = System.currentTimeMillis();
    }

    public static void show(String title, String message) {
        currentToast = new FishyToast(title, message);
        currentToast.startTime = System.currentTimeMillis();
    }

    public static void init() {
        HudLayerRegistrationCallback.EVENT.register(layeredDrawer -> 
            layeredDrawer.attachLayerAfter(
                IdentifiedLayer.MISC_OVERLAYS,
                Identifier.of("fishyaddons", "fatoast"),
                (context, tickCounter) -> {
                    if (currentToast != null) {
                        long elapsed = System.currentTimeMillis() - currentToast.startTime;
                        if (elapsed < 4000L) {
                            MinecraftClient mc = MinecraftClient.getInstance();
                            int screenWidth = mc.getWindow().getScaledWidth();
                            int toastWidth = 160;
                            int toastHeight = 32;
                            int x = (screenWidth - toastWidth) / 2;
                            int y = 20;
                            context.drawTexture(
                                RenderLayer::getGuiTextured,
                                Identifier.of("fishyaddons", "textures/gui/" + FishyMode.getTheme() + "/fatoast.png"),
                                x, y, 0, 0, toastWidth, toastHeight, 160, 32
                            );
                            TextRenderer tr = mc.textRenderer;
                            int titleWidth = tr.getWidth(currentToast.title);
                            int msgWidth = tr.getWidth(currentToast.message);                           
                            context.drawText(tr, Text.literal(currentToast.title), x + toastWidth / 2 - titleWidth / 2, y + 7, 0xFFFFFF, true);
                            context.drawText(tr, Text.literal(currentToast.message), x + toastWidth / 2 - msgWidth / 2, y + 18, 0xAAAAAA, false);
                        } else {
                            currentToast = null;
                        }
                    }
                }
            )
        );
    }
}