package me.valkeea.fishyaddons.gui;

import me.valkeea.fishyaddons.hud.HudElement;
import me.valkeea.fishyaddons.hud.ElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class HudEditScreen extends Screen {
    private HudElement dragging = null;
    private HudElement selectedElement = null;
    private int dragOffsetX, dragOffsetY;

    public HudEditScreen() {
        super(Text.literal("Edit HUD Elements"));
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), btn -> {
            for (HudElement element : ElementRegistry.getElements()) {
                element.setEditingMode(false);
            }
            MinecraftClient.getInstance().setScreen(null);
        }).dimensions(this.width / 2 - 40, this.height - 40, 80, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Color"), btn -> {
            HudElement element = selectedElement;
            if (element == null) {
                var elements = ElementRegistry.getElements();
                if (elements.isEmpty()) return;
                element = elements.get(0);
            }
            final HudElement finalElement = element;
            int color = finalElement.getHudColor();
            float red = ((color >> 16) & 0xFF) / 255.0f;
            float green = ((color >> 8) & 0xFF) / 255.0f;
            float blue = (color & 0xFF) / 255.0f;
            MinecraftClient.getInstance().setScreen(
                new ColorPickerScreen(this, new float[]{red, green, blue}, rgb -> {
                    int newColor = ((int)(rgb[0] * 255) << 16) | ((int)(rgb[1] * 255) << 8) | (int)(rgb[2] * 255);
                    finalElement.setHudColor(newColor);
                })
            );
        }).dimensions(this.width / 2 + 50, this.height - 40, 60, 20).build());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (HudElement element : ElementRegistry.getElements()) {
            int hudX = element.getHudX();
            int hudY = element.getHudY();
            int size = element.getHudSize();
            if (mouseX >= hudX && mouseX <= hudX + 80 && mouseY >= hudY && mouseY <= hudY + size + 4) {
                dragging = element;
                selectedElement = element;
                dragOffsetX = (int)mouseX - hudX;
                dragOffsetY = (int)mouseY - hudY;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = null;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging != null) {
            dragging.setHudPosition((int)mouseX - dragOffsetX, (int)mouseY - dragOffsetY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        for (HudElement element : ElementRegistry.getElements()) {
            element.setEditingMode(true);
            element.render(context, mouseX, mouseY);
        }
        // Draw border around selected element
        if (selectedElement != null) {
            int hudX = selectedElement.getHudX();
            int hudY = selectedElement.getHudY();
            int size = selectedElement.getHudSize();
            GuiUtil.drawBox(context, hudX, hudY, 80, size + 4, 0x80000000);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void removed() {
        for (HudElement element : ElementRegistry.getElements()) {
            element.setEditingMode(false);
        }
    }
}