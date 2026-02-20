package me.valkeea.fishyaddons.hud.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.text.Text;

/**
 * HUD button layout, rendering, and click detection
 */
public class HudButtonManager {
    private final List<HudButton> buttons = new ArrayList<>();
    private int baseX;
    private int baseY;
    private float scale;
    private int buttonWidth;
    private int buttonHeight;
    private int buttonSpacing;

    public HudButtonManager(int x, int y, float scale) {
        this.baseX = x;
        this.baseY = y - (int)(5 * scale);
        this.scale = scale;
        this.buttonWidth = (int)(45 * scale);
        this.buttonHeight = (int)(16 * scale);
        this.buttonSpacing = (int)(2 * scale);
    }
    
    /**
     * Create with custom button dimensions
     */
    public HudButtonManager(int x, int y, float scale, int buttonWidth, int buttonHeight, int buttonSpacing) {
        this.baseX = x;
        this.baseY = y - (int)(5 * scale);
        this.scale = scale;
        this.buttonWidth = buttonWidth;
        this.buttonHeight = buttonHeight;
        this.buttonSpacing = buttonSpacing;
    }
    
    /** Add a button with label and click handler */
    public HudButtonManager addButton(String label, Consumer<HudButton> onClick) {
        int index = buttons.size();
        int x = baseX + index * (buttonWidth + buttonSpacing);
        int y = baseY;
        buttons.add(new HudButton(label, x, y, buttonWidth, buttonHeight, onClick));
        return this;
    }
    
    /** Clear all buttons */
    public void clear() {
        buttons.clear();
    }
    
    /** Update positions on HUD state change */
    public void updatePositions(int newBaseX, int newBaseY, float newScale) {
        this.baseX = newBaseX;
        this.baseY = newBaseY - (int)(5 * newScale);
        this.scale = newScale;
        this.buttonWidth = (int)(45 * scale);
        this.buttonHeight = (int)(16 * scale);
        this.buttonSpacing = (int)(2 * scale);
        
        for (int i = 0; i < buttons.size(); i++) {
            HudButton button = buttons.get(i);
            button.x = baseX + i * (buttonWidth + buttonSpacing);
            button.y = baseY - buttonHeight;
            button.width = buttonWidth;
            button.height = buttonHeight;
        }
    }
    
    /** Render all buttons */
    public void render(HudDrawer drawer, double mouseX, double mouseY) {
        for (HudButton button : buttons) {
            boolean hovered = button.isHovered(mouseX, mouseY);
            drawer.drawButton(button.x, button.y, button.width, button.height, 
                            Text.literal(button.label), hovered, true);
        }
    }
    
    /**
     * Handle mouse click, returns true if a button was clicked.
     * Buttons only respond to left-click.
     */
    public boolean handleClick(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        
        for (HudButton hudButton : buttons) {
            if (hudButton.isHovered(mouseX, mouseY)) {
                hudButton.onClick.accept(hudButton);
                return true;
            }
        }
        return false;
    }
    
    /** Get number of buttons */
    public int size() {
        return buttons.size();
    }
    
    /** Combined width of all buttons */
    public int getTotalWidth() {
        if (buttons.isEmpty()) return 0;
        return buttons.size() * buttonWidth + (buttons.size() - 1) * buttonSpacing;
    }
    
    /**
     * Individual button data
     */
    public static class HudButton {
        private String label;
        private int x;
        private int y;
        private int width;
        private int height;
        private Consumer<HudButton> onClick;
        
        public HudButton(String label, int x, int y, int width, int height, Consumer<HudButton> onClick) {
            this.label = label;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.onClick = onClick;
        }
        
        public boolean isHovered(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width 
                && mouseY >= y && mouseY <= y + height;
        }

        public String getLabel() { return label; }
        public int getX() { return x; }
        public int getY() { return y; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
    }
}
