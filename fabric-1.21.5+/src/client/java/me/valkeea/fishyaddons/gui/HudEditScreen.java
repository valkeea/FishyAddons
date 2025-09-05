package me.valkeea.fishyaddons.gui;

import java.awt.Rectangle;

import me.valkeea.fishyaddons.hud.ElementRegistry;
import me.valkeea.fishyaddons.hud.HudElement;
import me.valkeea.fishyaddons.hud.PetDisplay;
import me.valkeea.fishyaddons.hud.TitleDisplay;
import me.valkeea.fishyaddons.hud.TrackerDisplay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class HudEditScreen extends Screen {
    private static final String OUTLINE = "Outline"; 
    private String targetElementName;
    private VCPopup popup = null;
    private HudElement dragging = null;
    private HudElement selectedElement = null;
    private FaButton outlineBtn;
    private FaButton colorBtn;
    private FaButton bgBtn;
    private int dragOffsetX;
    private int dragOffsetY;    

    public HudEditScreen() {
        super(Text.literal("Edit HUD Elements"));
        this.targetElementName = null;
    }
    
    public HudEditScreen(String targetElementName) {
        super(Text.literal("Edit HUD Elements"));
        this.targetElementName = targetElementName;
    }

    @Override
    protected void init() {
        if (targetElementName != null) {
            for (HudElement element : ElementRegistry.getElements()) {
                if (targetElementName.equals(element.getDisplayName())) {
                    selectedElement = element;
                    break;
                }
            }
        }
        
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

        colorBtn();

        outlineBtn = new FaButton(
            this.width / 2 - 40, this.height - 80, 80, 20,
            GuiUtil.onOffLabel(OUTLINE, selectedElement != null && selectedElement.getHudOutline()),
            btn -> {
                HudElement element = selectedElement;
                if (element != null && !(element instanceof TrackerDisplay)) {
                    boolean outlined = element.getHudOutline();
                    element.setHudOutline(!outlined);
                    element.invalidateCache();
                    btn.setMessage(GuiUtil.onOffLabel(OUTLINE, !outlined));
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

    private void colorBtn() {
        colorBtn =new FaButton(
            this.width / 2 - 40, this.height - 60, 80, 20,
            Text.literal("Color"),
            btn -> {
                HudElement element = selectedElement;
                if (element == null) {
                    var elements = ElementRegistry.getElements();
                    if (elements.isEmpty()) return;
                    element = elements.get(0);
                }
                if (element instanceof TitleDisplay) {
                    this.popup = new VCPopup(
                        Text.literal("Alert color is set in the alert editor!"),
                        "Back", () -> {
                            this.client.setScreen(HudEditScreen.this);
                            this.popup = null;
                        },
                        "GO", () -> {
                            this.client.setScreen(new GUIChatAlert(client.currentScreen));
                            this.popup = null;
                        },
                        1.0f
                    );
                    this.popup.init(this.textRenderer, this.width, this.height);
                    return;
                }

                final HudElement finalElement = element;
                int color = finalElement.getHudColor();
                float red = ((color >> 16) & 0xFF) / 255.0f;
                float green = ((color >> 8) & 0xFF) / 255.0f;
                float blue = (color & 0xFF) / 255.0f;
                MinecraftClient.getInstance().setScreen(
                    new ColorWheel(this, new float[]{red, green, blue}, rgb -> {
                        int newColor = ((int)(rgb[0] * 255) << 16) | ((int)(rgb[1] * 255) << 8) | (int)(rgb[2] * 255);
                        finalElement.setHudColor(newColor);
                        finalElement.invalidateCache();
                    })
                );
            });
        addDrawableChild(colorBtn);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (popup != null && popup.mouseClicked(mouseX, mouseY, button)) return true;
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (popup != null && popup.keyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        for (HudElement element : ElementRegistry.getElements()) {
            element.setEditingMode(true);
            element.render(context, mouseX, mouseY);
        }
        if (selectedElement != null) {
            int hudX = selectedElement.getHudX();
            int hudY = selectedElement.getHudY();
            int size = selectedElement.getHudSize();
            float scale = size / 12.0F;
            int width = (int)(80 * scale);
            int height = size + 4;

            if (selectedElement instanceof TitleDisplay) {
                Rectangle bounds = selectedElement.getBounds(MinecraftClient.getInstance());
                GuiUtil.drawBox(
                    context,
                    bounds.x - 2,
                    bounds.y - 2,
                    bounds.width + 4,
                    (int)(size + 4 * scale),
                    0x80000000
                );
            } else {
                GuiUtil.drawBox(context, hudX, hudY, width, height, 0x80000000);
            }
        }

        updateButtons();

        super.render(context, mouseX, mouseY, delta);

        if (popup != null) {
            popup.render(context, this.textRenderer, mouseX, mouseY, delta);
        }

        String helpText = "Select an element to edit";
        int textWidth = this.textRenderer.getWidth(helpText);
        int helpX = (this.width - textWidth) / 2;
        int helpY = this.height - 120;
        context.drawText(this.textRenderer, helpText, helpX, helpY, 0xFFFFD1FF, false);         
    }

    private void updateButtons() {
        if (outlineBtn != null && (selectedElement instanceof TrackerDisplay)) {
            outlineBtn.setMessage(Text.literal("-").styled(s -> s.withColor(0x848484)));
        } else if (outlineBtn != null) {
            outlineBtn.setMessage(GuiUtil.onOffLabel(OUTLINE, selectedElement != null && selectedElement.getHudOutline()));
        }

        if (colorBtn != null && (selectedElement instanceof PetDisplay)) {
            colorBtn.setMessage(Text.literal("-").styled(s -> s.withColor(0x848484)));
        } else if (colorBtn != null) {
            int color = selectedElement != null ? selectedElement.getHudColor() : 0xFFFFFF;
            colorBtn.setMessage(Text.literal("Color").styled(s -> s.withColor(color)));
        }

        if (bgBtn != null) {
            bgBtn.setMessage(GuiUtil.onOffLabel("BG", selectedElement != null && selectedElement.getHudBg()));
        }
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