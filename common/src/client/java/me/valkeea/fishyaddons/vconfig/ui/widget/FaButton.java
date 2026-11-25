package me.valkeea.fishyaddons.vconfig.ui.widget;

import com.mojang.blaze3d.platform.InputConstants;

import me.valkeea.fishyaddons.tool.FishyMode;
import me.valkeea.fishyaddons.util.Keyboard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

public class FaButton extends AbstractWidget implements ValueHolder {
    private static final String FA = "fishyaddons";
    private static final Identifier BUTTON_TEXTURE = Identifier.fromNamespaceAndPath(FA, "textures/gui/default/button.png");
    private static final Identifier BUTTON_DISABLED = Identifier.fromNamespaceAndPath(FA, "textures/gui/defaul/button_disabled.png");
    private Identifier hoverSprite;
    private String value;

    protected final OnPress action;
    private float scale = 1.0f;

    public FaButton(int x, int y, int w, int h, Component msg, OnPress action) {
        super(x, y, w, h, msg);
        this.action = action;
        setHoverSprite(FishyMode.themeName());
    }

    public void setUIScale(float scale) {
        this.scale = scale;
    }

    @Override
    public void set(String value) {
        this.value = value;
    }

    @Override
    public String get() {
        return value;
    }

    private void setHoverSprite(String theme) {
        this.hoverSprite = Identifier.fromNamespaceAndPath(FA, "textures/gui/" + theme + "/button_highlighted.png");
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor gge, int mX, int mY, float delta) {
        Identifier sprite;
        if (!this.active)  sprite = BUTTON_DISABLED;
        else sprite = this.isMouseOver(mX, mY) ? this.hoverSprite : BUTTON_TEXTURE;

        gge.blit(
            RenderPipelines.GUI_TEXTURED,
            sprite,
            this.getX(), this.getY(),
            0.0F, 0.0F,
            this.width, this.height,
            this.width, this.height
        );


        int color = this.active ? 0xFFFFFFFF : 0xFFA0A0A0;
        if (scale != 1.0f) {
            gge.pose().pushMatrix();
            gge.pose().scale(scale, scale);
            gge.centeredText(
                Minecraft.getInstance().font,
                this.getMessage(),
                (int) ((this.getX() + ((double)this.width / 2)) / scale),
                (int) ((this.getY() + ((double)this.height / 2) - 3) / scale) + 1,
                color
            );
            gge.pose().popMatrix();
        } else {
            gge.centeredText(
                Minecraft.getInstance().font,
                this.getMessage(),
                this.getX() + this.width / 2,
                this.getY() + (this.height - 8) / 2 + 1,
                color
            );
        }
    }

	public interface OnPress {
		void onPress(FaButton button);
	}

    @Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (this.action != null) {
            this.action.onPress(this);
        }
	}

    @Override 
    protected void updateWidgetNarration(NarrationElementOutput output) {/**Access */}

    /** A button that triggers an action without retaining state or focus */
    public static class Trigger extends FaButton {
        protected Trigger(int x, int y, int width, int height, Component message, OnPress action) {
            super(x, y, width, height, message, action);
        }

        @Override
        public boolean shouldTakeFocusAfterInteraction() {
            return false;
        }
    }

    public static class Toggle extends FaButton {
        private boolean state;

        protected Toggle(int x, int y, int width, int height, OnPress action, boolean initialState) {
            super(x, y, width, height, toggleLabel(initialState), action);
            this.state = initialState;
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            super.onClick(event, doubleClick);
            setState(!this.state);
        }

        public boolean getState() {
            return this.state;
        }

        public void setState(boolean newState) {
            this.state = newState;
            this.setMessage(toggleLabel(newState));
        }

        private static MutableComponent toggleLabel(boolean state) {
            return Component
                .literal(state ? "ON" : "OFF")
                .setStyle(Style.EMPTY.withColor(state ? 0xFFCCFFCC : 0xFFFF8080));
        }        
    }

    public abstract static class Keybind extends FaButton {
        public static final String PROMPT = "> Press a Key <";

        private boolean listening;

        protected Keybind(int x, int y, int width, int height, String keyValue, OnPress action) {
            super(x, y, width, height,
                Component.literal(keyValue.isEmpty()
                    ? PROMPT
                    : Keyboard.getDisplayNameFor(keyValue)), action
                );
            set(keyValue);
        }

        /** Called when a mouse button is handled as a click */
        protected abstract boolean onMouseClick(int btn);

        /** Called when a mouse button is handled as a new key input */
        protected abstract boolean newMouseKey(int btn);
        
        /** Called when the key value changes */
        protected abstract void onChange(String newKey);

        public void setListening(boolean newState) {
            this.listening = newState;
            this.setMessage(Component.literal(newState ? PROMPT : Keyboard.getDisplayNameFor(get())));
        }

        public boolean isListening() {
            return this.listening;
        }

        @Override
        public boolean keyPressed(KeyEvent input) {
            if (!this.listening)  return false;
            var newKey = Keyboard.getGlfwKeyName(input.key());
            if (newKey == null) newKey = getKeyFrom(input);
            onChange(newKey);
            return true;
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
            var btn = click.button();
            if (treatAsClick(btn)) return onMouseClick(btn);
            else if (this.listening) return newMouseKey(btn);
            return false;
        }

        private boolean treatAsClick(int btn) {
            return btn == InputConstants.MOUSE_BUTTON_LEFT || btn == InputConstants.MOUSE_BUTTON_RIGHT;
        }  

        private String getKeyFrom(KeyEvent input) {
            var translated = InputConstants.getKey(input).getName();
            if (translated.startsWith("key.keyboard.")) {
                return translated.substring("key.keyboard.".length()).toUpperCase();
            } else return translated.toUpperCase();
        }
    }
}
