package me.valkeea.fishyaddons.vconfig.ui.widget;

import me.valkeea.fishyaddons.ui.GuiUtil;
import me.valkeea.fishyaddons.vconfig.ui.layout.Colors;
import me.valkeea.fishyaddons.vconfig.ui.render.RenderUtils;
import me.valkeea.fishyaddons.vconfig.ui.render.VCText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

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

        public ButtonConfig(int x, int y, int width, int height, String text) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.text = text;
        }
        
        public ButtonConfig withPos(int x, int y) { this.x = x; this.y = y; return this; }
        public ButtonConfig withScale(float scale) { this.uiScale = scale; return this; }
        public ButtonConfig withEnabled(boolean enabled) { this.enabled = enabled; return this; }
        public ButtonConfig withHovered(boolean hovered) { this.hovered = hovered; return this; }
        public ButtonConfig withType(ButtonType type) { this.type = type; return this; }
    }
    
    /**
     * Enum for different button types with specific styling
     */
    public enum ButtonType {
        STANDARD,
        TOGGLE,
        SIMPLE,
        NAVIGATION,
        MCTOGGLE
    }
    
    /**
     * Unified button rendering method using ButtonConfig
     */
    public static void render(GuiGraphicsExtractor context, Font textRenderer, ButtonConfig config) {
        switch (config.type) {
            case TOGGLE -> renderToggle(context, textRenderer, config);
            case NAVIGATION -> renderNavigation(context, textRenderer, config);
            case STANDARD -> renderVCWidget(context, textRenderer, config);
            case MCTOGGLE -> renderMcToggle(context, textRenderer, config);
            default -> renderStandard(context, textRenderer, config);
        }
    }
    
    /**
     * Functional ButtonWidget with the standardized styling
     */
    public static Button createNavigationButton(int x, int y, int width, int height, MutableComponent message, 
                                                     Button.OnPress onPress, float uiScale) {
        return new Button(x, y, width, height, message, onPress, button -> message) {
            @Override
            protected void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
                var config = new ButtonConfig(this.getX(), this.getY(), this.width, this.height, this.getMessage().getString())
                    .withScale(uiScale)
                    .withEnabled(this.active)
                    .withHovered(this.isHovered())
                    .withType(ButtonType.NAVIGATION);
                
                VCButton.render(context, Minecraft.getInstance().font, config);
            }
        };
    }

    public static Button vcScreenWidget(int x, int y, int width, int height, MutableComponent message, 
                                                     Button.OnPress onPress, float uiScale) {
        return new Button(x, y, width, height, message, onPress, button -> message) {
            @Override
            protected void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
                var config = new ButtonConfig(this.getX(), this.getY(), this.width, this.height, this.getMessage().getString())
                    .withScale(uiScale)
                    .withEnabled(this.active)
                    .withHovered(this.isHovered())
                    .withType(ButtonType.STANDARD);
                
                VCButton.render(context, Minecraft.getInstance().font, config);
            }
        };
    }    

    public static Button createMcToggle(int x, int y, int width, int height, boolean enabled,
                                                  Button.OnPress onPress, float uiScale) {

        MutableComponent message = enabled ? Component.literal("ON") : Component.literal("OFF");
        return new Button(x, y, width, height, message, onPress, button -> message) {
            @Override
            protected void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
                
                boolean currentEnabled = this.getMessage().getString().equals("ON");
                var config = new ButtonConfig(this.getX(), this.getY(), this.width, this.height, this.getMessage().getString())
                    .withScale(uiScale)
                    .withEnabled(currentEnabled)
                    .withHovered(this.isHovered())
                    .withType(ButtonType.MCTOGGLE);

                VCButton.render(context, Minecraft.getInstance().font, config);
            }
        };
    }

    // -- Internal rendering methods --
    private static void renderNavigation(GuiGraphicsExtractor context, Font textRenderer, ButtonConfig config) {

        int bgColor = config.enabled ? VCVisuals.bgHex2(config.hovered) : 0x80404040;
        int borderColor = config.enabled ? VCVisuals.borderHex2(config.hovered) : 0x80404040;

        RenderUtils.gradient(context, config.x, config.y, config.width, config.height, bgColor);
        RenderUtils.border(context, config.x, config.y, config.width, config.height, borderColor);

        int textColor = config.enabled ? 0xFFFFFFFF : 0xFFC4FFFF;
        
        GuiUtil.drawScaledCenteredText(context, textRenderer, config.text, config.x + config.width / 2,
            config.y + (config.height - (int)(6 * config.uiScale)) / 2, textColor, config.uiScale
        );
    }

    private static void renderVCWidget(GuiGraphicsExtractor context, Font textRenderer, ButtonConfig config) {

        int bgColor = config.enabled ? VCVisuals.bgHex2(config.hovered) : 0x80404040;
        int borderColor = config.enabled ? VCVisuals.borderHex2(config.hovered) : 0x80404040;

        RenderUtils.gradient(context, config.x, config.y, config.width, config.height, bgColor);
        RenderUtils.border(context, config.x, config.y, config.width, config.height, borderColor);

        int textColor = config.enabled ? 0xFFFFFFFF : 0xFFC4FFFF;
        
        VCText.flatCentered(context, textRenderer, config.text, config.x + config.width / 2,
            config.y + (config.height - (int)(6 * config.uiScale)) / 2, textColor
        );
    }    

    private static void renderMcToggle(GuiGraphicsExtractor context, Font textRenderer, ButtonConfig config) {
        int textColor = config.enabled ? Colors.ENABLED_GREEN : Colors.TRANSPARENT_GREY;
        int bgColor = VCVisuals.bgHex2(config.hovered);
        int borderColor = VCVisuals.borderHex2(config.hovered);

        RenderUtils.gradient(context, config.x, config.y, config.width, config.height, bgColor);
        RenderUtils.border(context, config.x, config.y, config.width, config.height, borderColor);

        GuiUtil.drawScaledCenteredText(context, textRenderer, config.text, config.x + config.width / 2,
            config.y + (config.height - (int)(6 * config.uiScale)) / 2, textColor, config.uiScale
        );
    }

    private static void renderStandard(GuiGraphicsExtractor context, Font textRenderer, ButtonConfig config) {

        int bgColor = VCVisuals.bgHex(config.hovered, config.enabled);
        RenderUtils.gradient(context, config.x, config.y, config.width, config.height, bgColor);

        int borderColor = VCVisuals.borderHex(config.hovered, config.enabled);
        RenderUtils.border(context, config.x, config.y, config.width, config.height, borderColor);
        
        int textColor;
        if (!config.enabled) {
            textColor = Colors.TRANSPARENT_GREY;
        } else if (config.text.equals("ON") || config.text.equals("CONF")) {
            textColor = 0xFFCCFFCC;
        } else {
            textColor = 0xFFFFFFFF;
        }

        VCText.flatCentered(context, textRenderer, config.text, config.x + config.width / 2,
            config.y + (config.height - (int)(6 * config.uiScale)) / 2, textColor
        );
    }

    private static void renderToggle(GuiGraphicsExtractor context, Font textRenderer, ButtonConfig config) {
        int textColor = config.enabled ? Colors.ENABLED_GREEN : Colors.TRANSPARENT_GREY;

        int bgColor = VCVisuals.bgHex(config.hovered, config.enabled);
        int borderColor = VCVisuals.borderHex(config.hovered, config.enabled);

        RenderUtils.gradient(context, config.x, config.y, config.width, config.height, bgColor);
        RenderUtils.border(context, config.x, config.y, config.width, config.height, borderColor);

        VCText.flatCentered(context, textRenderer, config.text, config.x + config.width / 2,
            config.y + (config.height - (int)(6 * config.uiScale)) / 2, textColor
        );
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

    public static ButtonConfig updateButtonState(Button button, boolean enabled) {
        MutableComponent newText = enabled ? Component.literal("ON").withStyle(style -> style.withColor(0xFFFFFFFF))
                               : Component.literal("OFF").withStyle(style -> style.withColor(0xFFC4FFFF));        
        button.setMessage(newText);
        return new ButtonConfig(button.getX(), button.getY(), button.getWidth(), button.getHeight(), newText.getString())
            .withType(ButtonType.TOGGLE)
            .withEnabled(enabled);
    }
}
