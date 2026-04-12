package me.valkeea.fishyaddons.vconfig.ui.widget;

import me.valkeea.fishyaddons.util.Keyboard;
import me.valkeea.fishyaddons.vconfig.ui.layout.Colors;
import me.valkeea.fishyaddons.vconfig.ui.render.RenderUtils;
import me.valkeea.fishyaddons.vconfig.ui.render.VCText;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;

public class VCKeybind {
    float uiScale;

    private String key = "NONE";
    private String displayKey;
    private int x;
    private int y;
    private int width;
    private int height;

    private boolean hovered;
    private boolean listening;
    private boolean widthDirty = true;

    public VCKeybind(String key, int x, int y, int width, int height) {
        this.key = key;
        this.displayKey = Keyboard.getDisplayNameFor(key);
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void renderWidget(DrawContext context, TextRenderer tr, float uiScale) {
        this.uiScale = uiScale;
        if (widthDirty) setWidth(getWidthForKey(key, tr));
        String buttonText = "> <";
        int textColor;
        if (listening) {
            textColor = 0xFFFFFF80;
        } else if (noKey()) {
            textColor = Colors.TRANSPARENT_GREY;
        } else {
            textColor = 0xFF55FFFF;
            buttonText = displayKey;
        }
        
        boolean enabled = !noKey() || listening;
        int bgColor = VCVisuals.bgHex(hovered, enabled);
        int borderColor = VCVisuals.borderHex(hovered, enabled);

        RenderUtils.gradient(context, x, y, width, height, bgColor);
        RenderUtils.border(context, x, y, width, height, borderColor); 
           
        VCText.flatCentered(
            context, tr,
            buttonText,
            x + width / 2,
            y + (height - (int)(6 * uiScale)) / 2,
            textColor
        );
    }

    private boolean noKey() {
        return key.equals("NONE");
    }

    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0 || !hovered) return false;
        if (listening || doubled) {
            setKey("NONE");
        }
        setListening(!listening);
        return true;
    }

    public boolean keyPressed(KeyInput input) {
        if (!listening) return false;

        int keyCode = input.key();
        String newKey = me.valkeea.fishyaddons.util.Keyboard.getGlfwKeyName(keyCode);
        if (keyCode == 256 || keyCode == 257) {
            listening = false;
            return true;
        }
        if (newKey != null) {
            setKey(newKey);
        } else {
            setKey("NONE");
        }
        setListening(false);
        return true;
    }

    public boolean isListening() {
        return listening;
    }
    
    public String getKey() {
        return key;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
        this.widthDirty = false;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setListening(boolean listening) {
        this.listening = listening;
    }

    public void setKey(String key) {
        this.key = key;
        this.displayKey = Keyboard.getDisplayNameFor(key);
        this.widthDirty = true;
    }

    public void setHovered(boolean hovered) {
        this.hovered = hovered;
    }

    private int getWidthForKey(String key, TextRenderer tr) {
        var s = Keyboard.getDisplayNameFor(key);
        return tr.getWidth(s) + tr.fontHeight;
    }    
}
