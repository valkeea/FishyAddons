package me.valkeea.fishyaddons.gui;

import me.valkeea.fishyaddons.hud.ElementRegistry;
import me.valkeea.fishyaddons.hud.HudElement;
import me.valkeea.fishyaddons.hud.TitleDisplay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.awt.Rectangle;

public class HudEditScreen extends Screen {
    private HudElement dragging = null;
    private HudElement selectedElement = null;
    private int dragOffsetX;
    private int dragOffsetY;
    private FaButton outlineBtn;
    private FaButton bgBtn;

    public HudEditScreen() {
        super(Text.literal("Edit HUD Elements"));
    }

    @Override
    protected void init() {
        addDrawableChild(new FaButton(
            this.width / 2 - 40, this.height - 40, 80, 20,
            Text.literal("Exit"),
            btn -> {
                for (HudElement element : ElementRegistry.getElements()) {
                    element.setEditingMode(false);
                }
                MinecraftClient.getInstance().setScreen(null);
            }
        ));

        addDrawableChild(new FaButton(
            this.width / 2 - 40, this.height - 60, 80, 20,
            Text.literal("Color"),
            btn -> {
                HudElement element = selectedElement;
                if (element == null) {
                    var elements = ElementRegistry.getElements();
                    if (elements.isEmpty()) return;
                    element = elements.get(0);
                }
                if (element instanceof me.valkeea.fishyaddons.hud.TitleDisplay) {
                    this.client.setScreen(new Screen(Text.literal("Alert Color")) {
                        FishyPopup popup;
                        @Override
                        protected void init() {
                            popup = new FishyPopup(
                                Text.literal("Alert color is set in the alert editor!"),
                                Text.literal("GO"), () -> this.client.setScreen(new TabbedListScreen(
                                    client.currentScreen, TabbedListScreen.Tab.ALERT)),
                                Text.literal("Back"), () -> this.client.setScreen(HudEditScreen.this)
                            );
                            popup.init(this.width, this.height);
                        }
                        @Override
                        public void render(DrawContext context, int mouseX, int mouseY, float delta) {   
                            this.renderBackground(context, mouseX, mouseY, delta);
                            super.render(context, mouseX, mouseY, delta);
                            popup.render(context, this.textRenderer, mouseX, mouseY, delta);
                        }
                        @Override
                        public boolean mouseClicked(double mouseX, double mouseY, int button) {
                            return popup.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
                        }
                    });
                    return;
                }
                // Normal color picker for other HUD elements
                final HudElement finalElement = element;
                int color = finalElement.getHudColor();
                float red = ((color >> 16) & 0xFF) / 255.0f;
                float green = ((color >> 8) & 0xFF) / 255.0f;
                float blue = (color & 0xFF) / 255.0f;
                MinecraftClient.getInstance().setScreen(
                    new ColorPickerScreen(this, new float[]{red, green, blue}, rgb -> {
                        int newColor = ((int)(rgb[0] * 255) << 16) | ((int)(rgb[1] * 255) << 8) | (int)(rgb[2] * 255);
                        finalElement.setHudColor(newColor);
                        finalElement.invalidateCache();
                    })
                );
            }));

        outlineBtn = new FaButton(
            this.width / 2 - 40, this.height - 80, 80, 20,
            GuiUtil.onOffLabel("Outline", selectedElement != null && selectedElement.getHudOutline()),
            btn -> {
                HudElement element = selectedElement;
                if (element != null) {
                    boolean outlined = element.getHudOutline();
                    element.setHudOutline(!outlined);
                    element.invalidateCache();
                    btn.setMessage(GuiUtil.onOffLabel("Outline", !outlined));
                }
            }
        );
        addDrawableChild(outlineBtn);

        bgBtn = new FaButton(
            this.width / 2 - 40, this.height - 100, 80, 20,
            GuiUtil.onOffLabel("BG", selectedElement != null && selectedElement.getHudBg()),
            btn -> {
                HudElement element = selectedElement;
                if (element != null) {
                    boolean bg = element.getHudBg();
                    element.setHudBg(!bg);
                    element.invalidateCache();
                    btn.setMessage(GuiUtil.onOffLabel("BG", !bg));
                }
            }
        );
        addDrawableChild(bgBtn);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        MinecraftClient mc = MinecraftClient.getInstance();
        for (HudElement element : ElementRegistry.getElements()) {
            Rectangle bounds = element.getBounds(mc);
            if (bounds.contains(mouseX, mouseY)) {
                dragging = element;
                selectedElement = element;
                dragOffsetX = (int)mouseX - bounds.x;
                dragOffsetY = (int)mouseY - bounds.y;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging != null) {
            dragging.setHudPosition((int)mouseX - dragOffsetX, (int)mouseY - dragOffsetY);
            dragging.invalidateCache();
            dragging = null;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging != null) {
            dragging.setHudPosition((int)mouseX - dragOffsetX, (int)mouseY - dragOffsetY);
            dragging.invalidateCache();
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
            float scale = size / 12.0F;
            int width = (int)(80 * scale);
            int height = (int)(size + 4);

            if (selectedElement instanceof TitleDisplay) {
                // Centered box for TitleDisplay
                int textWidth = this.textRenderer.getWidth("Alert Title");
                int scaledTextWidth = (int) (textWidth * scale);
                int boxWidth = Math.max(80, scaledTextWidth + 8);
                int scaledBoxWidth = (int) (boxWidth * scale);
                GuiUtil.drawBox(
                    context,
                    hudX - scaledBoxWidth / 2 - 2,
                    hudY - 2,
                    scaledBoxWidth + 4,
                    (int)(size + 4 * scale),
                    0x80000000
                );
            } else {
                GuiUtil.drawBox(context, hudX, hudY, width, height, 0x80000000);
            }
        }
        // Dynamically update button labels based on selected element
        if (outlineBtn != null) {
            outlineBtn.setMessage(GuiUtil.onOffLabel("Outline", selectedElement != null && selectedElement.getHudOutline()));
        }
        if (bgBtn != null) {
            bgBtn.setMessage(GuiUtil.onOffLabel("BG", selectedElement != null && selectedElement.getHudBg()));
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void removed() {
        for (HudElement element : ElementRegistry.getElements()) {
            element.setEditingMode(false);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        double amount = verticalAmount;
        if (selectedElement != null) {
            int currentSize = selectedElement.getHudSize();
            int newSize = currentSize + (int) amount;

            if (newSize < 8) newSize = 8;
            if (newSize > 80) newSize = 80;
            selectedElement.setHudSize(newSize);
            selectedElement.invalidateCache();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
}