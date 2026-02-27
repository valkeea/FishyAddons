package me.valkeea.fishyaddons.hud.ui;

import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.event.impl.HudRenderEvent;
import me.valkeea.fishyaddons.tool.FishyMode;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
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

    public static void init() {
        FaEvents.HUD_RENDER.register(FishyToast::render);
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

    public static void render(HudRenderEvent event) {
        if (currentToast != null) {
            long elapsed = System.currentTimeMillis() - currentToast.startTime;

            if (elapsed < 4000L) {

                var mc = event.getClient();
                int screenWidth = mc.getWindow().getScaledWidth();
                int toastWidth = 160;
                int toastHeight = 32;
                int x = (screenWidth - toastWidth) / 2;
                int y = 20;

                event.getContext().drawTexture(
                    RenderPipelines.GUI_TEXTURED,
                    Identifier.of("fishyaddons", "textures/gui/" + FishyMode.getTheme() + "/fatoast.png"),
                    x, y, 0, 0, toastWidth, toastHeight, 160, 32
                );

                var tr = mc.textRenderer;
                int titleWidth = tr.getWidth(currentToast.title);
                int msgWidth = tr.getWidth(currentToast.message);

                event.getContext().drawText(tr, Text.literal(currentToast.title), x + toastWidth / 2 - titleWidth / 2, y + 7, 0xFFFFFFFF, true);
                event.getContext().drawText(tr, Text.literal(currentToast.message), x + toastWidth / 2 - msgWidth / 2, y + 18, 0xFFAAAAAA, false);

            } else {
                currentToast = null;
            }
        }
    }
}
