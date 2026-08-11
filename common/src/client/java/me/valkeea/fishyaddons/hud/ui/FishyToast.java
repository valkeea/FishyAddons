package me.valkeea.fishyaddons.hud.ui;

import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.event.impl.HudRenderEvent;
import me.valkeea.fishyaddons.tool.FishyMode;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class FishyToast implements Toast {
    private long startTime = -1;
    private final String title;
    private final String message;
    private static FishyToast currentToast = null;

    public FishyToast(String title, String message) {
        this.title = title;
        this.message = message;
    }

    public static void init() {
        FaEvents.HUD_RENDER.register(FishyToast::render);
    }    

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, Font textRenderer, long startTime) { /** Access */ }

    @Override
    public Visibility getWantedVisibility() {
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

    public static void render(HudRenderEvent event) {
        if (currentToast != null) {
            long elapsed = System.currentTimeMillis() - currentToast.startTime;

            if (elapsed < 4000L) {

                var mc = event.getClient();
                var window = mc.getWindow();
                int screenWidth = window.getGuiScaledWidth();
                int toastWidth = 160;
                int toastHeight = 32;
                int x = (screenWidth - toastWidth) / 2;
                int y = 20;

                event.getContext().blit(
                    RenderPipelines.GUI_TEXTURED,
                    Identifier.fromNamespaceAndPath("fishyaddons", "textures/gui/" + FishyMode.themeName() + "/fatoast.png"),
                    x, y, 0, 0, toastWidth, toastHeight, 160, 32
                );

                var tr = mc.font;
                int titleWidth = tr.width(currentToast.title);
                int msgWidth = tr.width(currentToast.message);

                event.getContext().text(tr, Component.literal(currentToast.title), x + toastWidth / 2 - titleWidth / 2, y + 7, 0xFFFFFFFF, true);
                event.getContext().text(tr, Component.literal(currentToast.message), x + toastWidth / 2 - msgWidth / 2, y + 18, 0xFFAAAAAA, false);

            } else {
                currentToast = null;
            }
        }
    }
}
