package me.valkeea.fishyaddons.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

/**
 * Standardized buttons
 */
public class VCButton {
    private VCButton() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static class ButtonConfig {
        private int x;
        private int y;
        private int width;
        private int height;
        private String text;
        private boolean enabled = true;
        private boolean hovered = false;
        private float uiScale = 1.0f;
        private ButtonType type = ButtonType.STANDARD;

        // Keybind specific fields
        private boolean listening = false;
        private boolean hasKey = true;


        
        public ButtonConfig(int x, int y, int width, int height, String text) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.text = text;
        }
        
        public ButtonConfig withScale(float scale) { this.uiScale = scale; return this; }
        public ButtonConfig withEnabled(boolean enabled) { this.enabled = enabled; return this; }
        public ButtonConfig withHovered(boolean hovered) { this.hovered = hovered; return this; }
        public ButtonConfig withType(ButtonType type) { this.type = type; return this; }
        public ButtonConfig withListening(boolean listening) { this.listening = listening; return this; }
        public ButtonConfig withHasKey(boolean hasKey) { this.hasKey = hasKey; return this; }
    }
    
    /**
     * Enum for different button types with specific styling
     */
    public enum ButtonType {
        STANDARD,
        TOGGLE,
        SIMPLE,
        KEYBIND,
        NAVIGATION,
        MCTOGGLE
    }
    
    /**
     * Unified button rendering method using ButtonConfig
     */
    public static void render(DrawContext context, TextRenderer textRenderer, ButtonConfig config) {
        switch (config.type) {
            case TOGGLE -> renderToggle(context, textRenderer, config);
            case SIMPLE -> renderToggleWithText(context, textRenderer, config, config.text);
            case KEYBIND -> renderKeybind(context, textRenderer, config);
            case NAVIGATION -> renderNavigation(context, textRenderer, config);
            case MCTOGGLE -> renderMcToggle(context, textRenderer, config);
            default -> renderStandard(context, textRenderer, config);
        }
    }
    
    /**
     * Functional ButtonWidget with the standardized styling
     */
    public static ButtonWidget createNavigationButton(int x, int y, int width, int height, MutableText message, 
                                                     ButtonWidget.PressAction onPress, float uiScale) {
        return new ButtonWidget(x, y, width, height, message, onPress, button -> message) {
            @Override
            protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
                ButtonConfig config = new ButtonConfig(this.getX(), this.getY(), this.width, this.height, this.getMessage().getString())
                    .withScale(uiScale)
                    .withEnabled(this.active)
                    .withHovered(this.isHovered())
                    .withType(ButtonType.NAVIGATION);
                
                VCButton.render(context, net.minecraft.client.MinecraftClient.getInstance().textRenderer, config);
            }
        };
    }

    public static ButtonWidget createMcToggle(int x, int y, int width, int height, boolean enabled,
                                                  ButtonWidget.PressAction onPress, float uiScale) {

        MutableText message = enabled ? Text.literal("ON") : Text.literal("OFF");
        return new ButtonWidget(x, y, width, height, message, onPress, button -> message) {
            @Override
            protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
                ButtonConfig config = new ButtonConfig(this.getX(), this.getY(), this.width, this.height, this.getMessage().getString())
                    .withScale(uiScale)
                    .withEnabled(enabled)
                    .withHovered(this.isHovered())
                    .withType(ButtonType.MCTOGGLE);

                VCButton.render(context, net.minecraft.client.MinecraftClient.getInstance().textRenderer, config);
            }
        };
    }

    // -- Internal rendering methods --
    private static void renderNavigation(DrawContext context, TextRenderer textRenderer, ButtonConfig config) {

        int bgColor = config.enabled ? VCVisuals.bgHex2(config.hovered) : 0x80404040;
        int borderColor = config.enabled ? VCVisuals.borderHex2(config.hovered) : 0x80404040;

        VCRenderUtils.gradient(context, config.x, config.y, config.width, config.height, bgColor);
        context.drawBorder(config.x, config.y, config.width, config.height, borderColor);

        int textColor = config.enabled ? 0xFFFFFFFF : 0xFFC4FFFF;
        int textX = config.x + config.width / 2;
        int textY = config.y + (config.height - (int)(8 * config.uiScale)) / 2;
        
        VCText.drawScaledCenteredText(context, textRenderer, config.text, textX, textY, textColor, config.uiScale);
    }

    private static void renderMcToggle(DrawContext context, TextRenderer textRenderer, ButtonConfig config) {
        int textColor = config.enabled ? 0xCCFFCC : 0xFF8080;
        int bgColor = VCVisuals.bgHex2(config.hovered);
        int borderColor = VCVisuals.borderHex2(config.hovered);

        VCRenderUtils.gradient(context, config.x, config.y, config.width, config.height, bgColor);
        context.drawBorder(config.x, config.y, config.width, config.height, borderColor);

        int textX = config.x + config.width / 2;
        int textY = config.y + (config.height - (int)(8 * config.uiScale)) / 2;

        VCText.drawScaledCenteredText(context, textRenderer, config.text, textX, textY, textColor, config.uiScale);
    }

    private static void renderStandard(DrawContext context, TextRenderer textRenderer, ButtonConfig config) {

        int bgColor = VCVisuals.bgHex(config.hovered, config.enabled);
        VCRenderUtils.gradient(context, config.x, config.y, config.width, config.height, bgColor);

        int borderColor = VCVisuals.borderHex(config.hovered, config.enabled);
        context.drawBorder(config.x, config.y, config.width, config.height, borderColor);
        
        int textColor;
        if (!config.enabled) {
            textColor = 0xFF8080;
        } else if (config.text.equals("ON") || config.text.equals("CONF")) {
            textColor = 0xCCFFCC;
        } else if (config.text.equals("OFF")) {
            textColor = 0xFF8080;
        } else {
            textColor = 0xFFFFFFFF;
        }
        
        int textX = config.x + config.width / 2 - (int)(textRenderer.getWidth(config.text) * Math.min(config.uiScale, 1.0f)) / 2;
        int textY = config.y + (config.height - textRenderer.fontHeight) / 2 + 2;
        VCText.drawScaledButtonText(context, textRenderer, config.text, textX, textY, textColor, config.uiScale);
    }

    private static void renderToggle(DrawContext context, TextRenderer textRenderer, ButtonConfig config) {
        String text = config.enabled ? "ON" : "OFF";
        int textColor = config.enabled ? 0xCCFFCC : 0xFF8080;
        
        int bgColor = VCVisuals.bgHex(config.hovered, config.enabled);
        int borderColor = VCVisuals.borderHex(config.hovered, config.enabled);

        VCRenderUtils.gradient(context, config.x, config.y, config.width, config.height, bgColor);
        context.drawBorder(config.x, config.y, config.width, config.height, borderColor);

        int textX = config.x + config.width / 2 - (int)(textRenderer.getWidth(text) * Math.min(config.uiScale, 1.0f)) / 2;
        int textY = config.y + (config.height - textRenderer.fontHeight) / 2 + 2;
        VCText.drawScaledButtonText(context, textRenderer, text, textX, textY, textColor, config.uiScale);
    }

    private static void renderToggleWithText(DrawContext context, TextRenderer textRenderer, ButtonConfig config, String buttonText) {

        String text = buttonText;
        int textColor = config.enabled ? 0xCCFFCC : 0xFF8080;

        int bgColor = VCVisuals.bgHex(config.hovered, config.enabled);
        int borderColor = VCVisuals.borderHex(config.hovered, config.enabled);

        VCRenderUtils.gradient(context, config.x, config.y, config.width, config.height, bgColor);
        context.drawBorder(config.x, config.y, config.width, config.height, borderColor);

        int textX = config.x + config.width / 2 - (int)(textRenderer.getWidth(text) * Math.min(config.uiScale, 1.0f)) / 2;
        int textY = config.y + (config.height - textRenderer.fontHeight) / 2 + 2;
        VCText.drawScaledButtonText(context, textRenderer, text, textX, textY, textColor, config.uiScale);
    }

    private static void renderKeybind(DrawContext context, TextRenderer textRenderer, ButtonConfig config) {

        int textColor;
        if (config.listening) {
            textColor = 0xFFFFFF80;
        } else if (!config.hasKey) {
            textColor = 0xFFDD4444;
        } else {
            textColor = 0xFF55FFFF;
        }
        
        boolean enabled = config.hasKey || config.listening;
        int bgColor = VCVisuals.bgHex(config.hovered, enabled);
        int borderColor = VCVisuals.borderHex(config.hovered, enabled);

        VCRenderUtils.gradient(context, config.x, config.y, config.width, config.height, bgColor);
        context.drawBorder(config.x, config.y, config.width, config.height, borderColor);

        int textX = config.x + config.width / 2 - (int)(textRenderer.getWidth(config.text) * Math.min(config.uiScale, 1.0f)) / 2;
        int textY = config.y + (config.height - textRenderer.fontHeight) / 2 + 2;
        VCText.drawScaledButtonText(context, textRenderer, config.text, textX, textY, textColor, config.uiScale);
    }

    // -- Convenience methods for button creation --
    public static ButtonConfig standard(int x, int y, int width, int height, String text) {
        return new ButtonConfig(x, y, width, height, text);
    }
    
    public static ButtonConfig toggle(int x, int y, int width, int height, boolean enabled) {
        return new ButtonConfig(x, y, width, height, enabled ? "ON" : "OFF")
            .withType(ButtonType.TOGGLE)
            .withEnabled(enabled);
    }
    
    public static ButtonConfig toggleWithText(int x, int y, int width, int height, String text, boolean enabled) {
        return new ButtonConfig(x, y, width, height, text)
            .withType(ButtonType.SIMPLE)
            .withEnabled(enabled);
    }
    
    public static ButtonConfig keybind(int x, int y, int width, int height, String displayText) {
        return new ButtonConfig(x, y, width, height, displayText)
            .withType(ButtonType.KEYBIND);
    }
    
    public static ButtonConfig navigation(int x, int y, int width, int height, String text) {
        return new ButtonConfig(x, y, width, height, text)
            .withType(ButtonType.NAVIGATION);
    }
    
    /**
     * Utility method to check if a point is within button bounds
     */
    public static boolean isHovered(int buttonX, int buttonY, int buttonWidth, int buttonHeight, int mouseX, int mouseY) {
        return mouseX >= buttonX && mouseX <= buttonX + buttonWidth && 
               mouseY >= buttonY && mouseY <= buttonY + buttonHeight;
    }
}
