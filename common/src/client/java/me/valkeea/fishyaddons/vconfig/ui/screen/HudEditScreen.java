package me.valkeea.fishyaddons.vconfig.ui.screen;

import java.awt.Rectangle;

import me.valkeea.fishyaddons.compat.McApi;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

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
        super(Component.literal("Edit HUD Elements"));
        this.targetElementName = null;
    }
    
    public HudEditScreen(BooleanKey target, Screen parent) {
        super(Component.literal("Edit HUD Elements"));
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
        addRenderableWidget(new FaButton(
            10, y, w, h,
            Component.literal("Reset"),
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

        addRenderableWidget(new FaButton(
            10, y - h, w, h,
            Component.literal("Reset All"),
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
        addRenderableWidget(new FaButton(
            this.width / 2 - w / 2, y, w, h,
            Component.literal("Exit"),
            btn -> {
                for (HudElement e : ElementRegistry.getConfigurable()) {
                    e.setEditingMode(false);
                }
                McApi.setScreen(null);
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
                if (e != null && e.hasCosmetics()) {
                    boolean outlined = e.getHudOutline();
                    e.setHudOutline(!outlined);
                    e.invalidateCache();
                    btn.setMessage(GuiUtil.onOffLabel(OUTLINE, !outlined));
                }
            }
        );
        addRenderableWidget(outlineBtn);
        y -= 20;

        bgBtn = new FaButton(
            this.width / 2 - w / 2, y, w, h,
            GuiUtil.onOffLabel("BG", selectedElement != null && selectedElement.getHudBg()),
            btn -> {
                HudElement e = selectedElement;
                if (e != null && e.hasCosmetics()) {
                    boolean bg = e.getHudBg();
                    e.setHudBg(!bg);
                    e.invalidateCache();
                    btn.setMessage(GuiUtil.onOffLabel("BG", !bg));
                }
            }
        );
        addRenderableWidget(bgBtn);
        y -= 40;
        
        addRenderableWidget(new FaButton(
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
            Component.literal("Color"),
            btn -> {
                HudElement e = selectedElement;
                if (e == null) {
                    var elements = ElementRegistry.getConfigurable();
                    if (elements.isEmpty()) return;
                    e = elements.get(0);
                }
                if (!e.hasCosmetics()) return;
                if (e instanceof TitleDisplay) {
                    this.popup = new VCPopup(
                        Component.literal("Alert color is set in the alert editor!"),
                        "Back", () -> {
                            McApi.setScreen(HudEditScreen.this);
                            this.popup = null;
                        },
                        "GO", () -> {
                            McApi.setScreen(new ChatAlerts());
                            this.popup = null;
                        },
                        1.0f
                    );
                    this.popup.init(this.font, this.width, this.height);
                    return;
                }

                final HudElement finalElement = e;
                int color = finalElement.getHudColor();
                McApi.setScreen(
                    new ColorWheel(this, color, newColor -> {
                        finalElement.setHudColor(newColor);
                        finalElement.invalidateCache();
                    })
                );
            });
        addRenderableWidget(colorBtn);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (popup != null && popup.mouseClicked(click)) return true;

        var mc = Minecraft.getInstance();
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

    private int getDragOffsetX(HudElement e, Rectangle b, MouseButtonEvent c) {

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
    public boolean mouseReleased(MouseButtonEvent click) {
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
    public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
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
    public boolean keyPressed(KeyEvent input) {
        if (popup != null && popup.keyPressed(input)) return true;
        if (input.isEscape()) {
            if (this.minecraft != null) {
                McApi.setScreen(parent);
            }
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {

        var mc = Minecraft.getInstance();
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

        super.extractRenderState(context, mouseX, mouseY, delta);

        if (popup != null) {
            popup.render(context, this.font, mouseX, mouseY, delta);
        }

        String globalText = "All Elements";
        String helpText = "Select an element to edit";
        int helpWidth = this.font.width(helpText);
        int globalWidth = this.font.width(globalText);
        int helpX = (this.width - helpWidth) / 2;
        int helpY = this.height - 115;
        int globalX = (this.width - globalWidth) / 2;
        int globalY = helpY - 45;
        int textColor = FishyMode.getThemeColor();
        context.text(this.font, globalText, globalX, globalY, textColor, false);
        context.text(this.font, helpText, helpX, helpY, textColor, false);
    }

    private void updateButtons() {
        boolean active = selectedElement != null;
        boolean valid = active && selectedElement.hasCosmetics();

        if (outlineBtn != null) {
            outlineBtn.setMessage(GuiUtil.onOffLabel(valid ? OUTLINE : "-", active && selectedElement.getHudOutline()));
        }

        if (colorBtn != null && (selectedElement instanceof PetDisplay || !valid)) {
            colorBtn.setMessage(Component.literal("-").withStyle(s -> s.withColor(0x84848484)));
        } else if (colorBtn != null) {
            int color = active ? selectedElement.getHudColor() : 0xFFFFFFFF;
            colorBtn.setMessage(Component.literal("Color").withStyle(s -> s.withColor(color)));
        }

        if (bgBtn != null) {
            bgBtn.setMessage(GuiUtil.onOffLabel(valid ? "BG" : "-", active && selectedElement.getHudBg()));
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
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // force no blur
    }    
}
