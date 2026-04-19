package me.valkeea.fishyaddons.vconfig.ui.screen;

import java.awt.Rectangle;

import me.valkeea.fishyaddons.hud.base.SimpleTextElement;
import me.valkeea.fishyaddons.hud.core.ElementRegistry;
import me.valkeea.fishyaddons.hud.core.HudElement;
import me.valkeea.fishyaddons.hud.core.ScreenRenderContext;
import me.valkeea.fishyaddons.hud.elements.simple.PetDisplay;
import me.valkeea.fishyaddons.hud.elements.simple.TitleDisplay;
import me.valkeea.fishyaddons.hud.ui.UIFeedback;
import me.valkeea.fishyaddons.tool.FishyMode;
import me.valkeea.fishyaddons.ui.GuiUtil;
import me.valkeea.fishyaddons.ui.list.ChatAlerts;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.ui.widget.FaButton;
import me.valkeea.fishyaddons.vconfig.ui.widget.VCPopup;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

public class HudEditScreen extends Screen {
    private static final String OUTLINE = "Outline"; 
    private Screen parent;
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
    
    public HudEditScreen(BooleanKey target, Screen parent) {
        super(Text.literal("Edit HUD Elements"));
        this.targetElementName = target.toString();
        this.parent = parent;
    }

    @Override
    protected void init() {
        ScreenRenderContext.setEditMode(true);
        
        if (targetElementName != null) {
            for (HudElement e : ElementRegistry.getConfigurable()) {
                if (targetElementName.equals(e.getDisplayName())) {
                    selectedElement = e;
                    break;
                }
            }
        }

        int y = this.height - 40;
        int w = 80;
        int h = 20;

        addResets(y, w, h);
        addButtons(y, w, h);
    }

    private void addResets(int y, int w, int h) {
        addDrawableChild(new FaButton(
            10, y, w, h,
            Text.literal("Reset"),
            btn -> {
                String msg = null;
                int sx = this.width / 2;
                int sy = this.height / 2;

                if (selectedElement != null) {
                    sx = selectedElement.getHudX();
                    sy = selectedElement.getHudY();
                    selectedElement.resetAll();
                    msg = "Reset " + selectedElement.getDisplayName() + " to default settings.";
                }

                UIFeedback.getInstance().set(
                    msg == null ? "No element selected to reset!" : msg,
                    300, sx, sy, null
                );
            }
        ));

        addDrawableChild(new FaButton(
            10, y - h, w, h,
            Text.literal("Reset All"),
            btn -> {
                for (HudElement e : ElementRegistry.getConfigurable()) {
                    e.resetAll();
                }
                UIFeedback.getInstance().set(
                    "Reset all elements to default settings.",
                    300, this.width / 2, this.height / 2, null
                );
            }
        ));
    }

    private void addButtons(int y, int w, int h) {
        addDrawableChild(new FaButton(
            this.width / 2 - w / 2, y, w, h,
            Text.literal("Exit"),
            btn -> {
                for (HudElement e : ElementRegistry.getConfigurable()) {
                    e.setEditingMode(false);
                }
                MinecraftClient.getInstance().setScreen(null);
            }
        ));
        y -= 20;
        
        colorBtn(y, w, h);
        y -= 20;

        outlineBtn = new FaButton(
            this.width / 2 - w / 2, y, w, h,
            GuiUtil.onOffLabel(OUTLINE, selectedElement != null && selectedElement.getHudOutline()),
            btn -> {
                HudElement e = selectedElement;
                if (e != null) {
                    boolean outlined = e.getHudOutline();
                    e.setHudOutline(!outlined);
                    e.invalidateCache();
                    btn.setMessage(GuiUtil.onOffLabel(OUTLINE, !outlined));
                }
            }
        );
        addDrawableChild(outlineBtn);
        y -= 20;

        bgBtn = new FaButton(
            this.width / 2 - w / 2, y, w, h,
            GuiUtil.onOffLabel("BG", selectedElement != null && selectedElement.getHudBg()),
            btn -> {
                HudElement e = selectedElement;
                if (e != null) {
                    boolean bg = e.getHudBg();
                    e.setHudBg(!bg);
                    e.invalidateCache();
                    btn.setMessage(GuiUtil.onOffLabel("BG", !bg));
                }
            }
        );
        addDrawableChild(bgBtn);
        y -= 40;
        
        addDrawableChild(new FaButton(
            this.width / 2 - (w + 20) / 2, y, w + 20, h,
            GuiUtil.onOffLabel("Shadow", Config.get(BooleanKey.HUD_TEXT_SHADOW)),
            btn -> {
                Config.toggle(BooleanKey.HUD_TEXT_SHADOW);
                btn.setMessage(GuiUtil.onOffLabel("Shadow", Config.get(BooleanKey.HUD_TEXT_SHADOW)));
                for (HudElement e : ElementRegistry.getConfigurable()) {
                    e.invalidateCache();
                }
            }
        ));
    }

    private void colorBtn(int y, int w, int h) {
        colorBtn = new FaButton(
            this.width / 2 - w / 2, y, w, h,
            Text.literal("Color"),
            btn -> {
                HudElement e = selectedElement;
                if (e == null) {
                    var elements = ElementRegistry.getConfigurable();
                    if (elements.isEmpty()) return;
                    e = elements.get(0);
                }
                if (e instanceof TitleDisplay) {
                    this.popup = new VCPopup(
                        Text.literal("Alert color is set in the alert editor!"),
                        "Back", () -> {
                            this.client.setScreen(HudEditScreen.this);
                            this.popup = null;
                        },
                        "GO", () -> {
                            this.client.setScreen(new ChatAlerts());
                            this.popup = null;
                        },
                        1.0f
                    );
                    this.popup.init(this.textRenderer, this.width, this.height);
                    return;
                }

                final HudElement finalElement = e;
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
        for (HudElement e : ElementRegistry.getConfigurable()) {
            Rectangle bounds = e.getBounds(mc);
            if (bounds.contains(click.x(), click.y())) {
                dragging = e;
                selectedElement = e;
                dragOffsetX = getDragOffsetX(e, bounds, click);
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
        if (input.isEscape()) {
            if (this.client != null) {
                this.client.setScreen(parent);
            }
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {

        var mc = MinecraftClient.getInstance();
        for (HudElement e : ElementRegistry.getConfigurable()) {
            e.setEditingMode(true);
            e.render(context, mc, mouseX, mouseY);
            GuiUtil.wireRect(context, e.getBounds(mc), 0x80FFFFFF);
        }

        UIFeedback.getInstance().render(context, mc, mouseX, mouseY);

        if (selectedElement != null) {
            Rectangle bounds = selectedElement.getBounds(mc);
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
        ScreenRenderContext.setEditMode(false);
        for (HudElement e : ElementRegistry.getConfigurable()) {
            e.setEditingMode(false);
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
