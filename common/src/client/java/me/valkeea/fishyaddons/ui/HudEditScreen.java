package me.valkeea.fishyaddons.ui;

import java.awt.Rectangle;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.hud.base.SimpleTextElement;
import me.valkeea.fishyaddons.hud.core.ElementRegistry;
import me.valkeea.fishyaddons.hud.core.HudElement;
import me.valkeea.fishyaddons.hud.elements.simple.PetDisplay;
import me.valkeea.fishyaddons.hud.elements.simple.TitleDisplay;
import me.valkeea.fishyaddons.tool.FishyMode;
import me.valkeea.fishyaddons.ui.list.ChatAlerts;
import me.valkeea.fishyaddons.ui.widget.FaButton;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
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
            for (HudElement element : ElementRegistry.getConfigurable()) {
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
                for (HudElement element : ElementRegistry.getConfigurable()) {
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
                if (element != null) {
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
        
        addDrawableChild(new FaButton(
            this.width / 2 - 50, this.height - 140, 100, 20,
            GuiUtil.onOffLabel("Shadow", FishyConfig.getState(Key.HUD_TEXT_SHADOW, true)),
            btn -> {
                FishyConfig.toggle(Key.HUD_TEXT_SHADOW, true);
                btn.setMessage(GuiUtil.onOffLabel("Shadow", FishyConfig.getState(Key.HUD_TEXT_SHADOW, true)));
                for (HudElement element : ElementRegistry.getConfigurable()) {
                    element.invalidateCache();
                }
            }
        ));
    }

    private void colorBtn() {
        colorBtn =new FaButton(
            this.width / 2 - 40, this.height - 60, 80, 20,
            Text.literal("Color"),
            btn -> {
                HudElement element = selectedElement;
                if (element == null) {
                    var elements = ElementRegistry.getConfigurable();
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
                            this.client.setScreen(new ChatAlerts(client.currentScreen));
                            this.popup = null;
                        },
                        1.0f
                    );
                    this.popup.init(this.textRenderer, this.width, this.height);
                    return;
                }

                final HudElement finalElement = element;
                int color = finalElement.getHudColor();
                MinecraftClient.getInstance().setScreen(
                    new ColorWheel(this, color, newColor -> {
                        finalElement.setHudColor(newColor);
                        finalElement.invalidateCache();
                    })
                );
            });
        addDrawableChild(colorBtn);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (popup != null && popup.mouseClicked(click)) return true;

        var mc = MinecraftClient.getInstance();
        for (HudElement element : ElementRegistry.getConfigurable()) {
            Rectangle bounds = element.getBounds(mc);
            if (bounds.contains(click.x(), click.y())) {
                dragging = element;
                selectedElement = element;
                dragOffsetX = getDragOffsetX(element, bounds, click);
                dragOffsetY = (int)click.y() - bounds.y;
                return true;
            }
        }
        
        return super.mouseClicked(click, doubled);
    }

    private int getDragOffsetX(HudElement e, Rectangle b, Click c) {

        if (e instanceof SimpleTextElement ste && ste.getTextAlignment() != 0) {
            int a = ste.getTextAlignment();

            if (a == 1) {
                return b.width / 2 - (int)(c.x() - b.x);
            } else if (a == 2) {
                return b.width - (int)(c.x() - b.x);
            }
        }
        
        return (int)c.x() - b.x;
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragging != null) {
            int newX = (int)click.x() - dragOffsetX;
            int newY = (int)click.y() - dragOffsetY;
            dragging.setHudPosition(newX, newY);
            dragging.invalidateCache();
            dragging = null;
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (dragging != null) {
            int newX = (int)click.x() - dragOffsetX;
            int newY = (int)click.y() - dragOffsetY;
            dragging.setHudPosition(newX, newY);
            dragging.invalidateCache();
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (popup != null && popup.keyPressed(input)) return true;
        return super.keyPressed(input);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {

        for (HudElement element : ElementRegistry.getConfigurable()) {
            element.setEditingMode(true);
            element.render(context, mouseX, mouseY);
            GuiUtil.wireRect(context, element.getBounds(MinecraftClient.getInstance()), 0x80FFFFFF);
        }

        if (selectedElement != null) {
            Rectangle bounds = selectedElement.getBounds(MinecraftClient.getInstance());
            context.fill(
                bounds.x,
                bounds.y,
                bounds.x + bounds.width,
                bounds.y + bounds.height,
                0x30FFFFFF
            );
        }

        updateButtons();

        super.render(context, mouseX, mouseY, delta);

        if (popup != null) {
            popup.render(context, this.textRenderer, mouseX, mouseY, delta);
        }

        String globalText = "All Elements";
        String helpText = "Select an element to edit";
        int helpWidth = this.textRenderer.getWidth(helpText);
        int globalWidth = this.textRenderer.getWidth(globalText);
        int helpX = (this.width - helpWidth) / 2;
        int helpY = this.height - 115;
        int globalX = (this.width - globalWidth) / 2;
        int globalY = helpY - 45;
        int textColor = FishyMode.getThemeColor();
        context.drawText(this.textRenderer, globalText, globalX, globalY, textColor, false);
        context.drawText(this.textRenderer, helpText, helpX, helpY, textColor, false);
    }

    private void updateButtons() {
        if (outlineBtn != null) {
            outlineBtn.setMessage(GuiUtil.onOffLabel(OUTLINE, selectedElement != null && selectedElement.getHudOutline()));
        }

        if (colorBtn != null && (selectedElement instanceof PetDisplay)) {
            colorBtn.setMessage(Text.literal("-").styled(s -> s.withColor(0x84848484)));
        } else if (colorBtn != null) {
            int color = selectedElement != null ? selectedElement.getHudColor() : 0xFFFFFFFF;
            colorBtn.setMessage(Text.literal("Color").styled(s -> s.withColor(color)));
        }

        if (bgBtn != null) {
            bgBtn.setMessage(GuiUtil.onOffLabel("BG", selectedElement != null && selectedElement.getHudBg()));
        }
    }

    @Override
    public void removed() {
        for (HudElement element : ElementRegistry.getConfigurable()) {
            element.setEditingMode(false);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        double amount = verticalAmount;
        if (selectedElement != null) {
            int currentSize = selectedElement.getHudSize();
            int newSize = currentSize + (int) amount;

            newSize = Math.clamp(newSize, 8, 140);
            selectedElement.setHudSize(newSize);
            selectedElement.invalidateCache();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // force no blur
    }    
}
