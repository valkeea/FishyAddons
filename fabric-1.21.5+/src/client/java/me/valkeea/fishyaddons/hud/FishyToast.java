package me.valkeea.fishyaddons.hud;

import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.render.RenderLayer;

public class FishyToast implements Toast {
    private static final Identifier TEXTURE = Identifier.of("fishyaddons", "gui/fatoast");
    private long startTime = -1;
    private final String title;
    private final String message;
    private static FishyToast currentToast = null;

    public FishyToast(String title, String message) {
        this.title = title;
        this.message = message;
    }

    @Override
    public void draw(DrawContext context, TextRenderer textRenderer, long startTime) {
        context.drawTexture(RenderLayer::getGuiTextured, TEXTURE, 160, 32, 0, 0, getWidth(), getHeight(), 160, 32);
        context.drawText(textRenderer, Text.literal(title), 30, 7, 0xFFFFFF, true);
        context.drawText(textRenderer, Text.literal(message), 30, 20, 0xAAAAAA, false);
    }

    @Override
    public Visibility getVisibility() {
        if (startTime < 0) return Toast.Visibility.SHOW;
        return (System.currentTimeMillis() - startTime) >= 3000L ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
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
                        if (elapsed < 3000L) {
                            MinecraftClient mc = MinecraftClient.getInstance();
                            int screenWidth = mc.getWindow().getScaledWidth();
                            int toastWidth = 160;
                            int toastHeight = 32;
                            int x = (screenWidth - toastWidth) / 2;
                            int y = 20;
                            context.fill(x, y, x + toastWidth, y + toastHeight, 0x80000000);
                            TextRenderer tr = mc.textRenderer;
                            context.drawText(tr, Text.literal(currentToast.title), x + 16, y + 7, 0xFFFFFF, true);
                            context.drawText(tr, Text.literal(currentToast.message), x + 16, y + 20, 0xAAAAAA, false);
                        } else {
                            currentToast = null;
                        }
                    }
                }
            )
        );
    }
}