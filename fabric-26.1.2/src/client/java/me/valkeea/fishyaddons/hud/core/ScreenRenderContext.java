package me.valkeea.fishyaddons.hud.core;

import java.util.ArrayList;
import java.util.List;

import me.valkeea.fishyaddons.hud.base.InteractiveHudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

public class ScreenRenderContext {
    
    private static final List<InteractiveHudElement> hoveredElements = new ArrayList<>();
    private static boolean inScreenContext = false;
    private static boolean inEditMode = false;
    private static double currentMouseX = 0;
    private static double currentMouseY = 0;
    
    private ScreenRenderContext() {}
    
    /**
     * Update hover state for all interactive elements.
     */
    public static void updateHoverState(double mouseX, double mouseY, List<InteractiveHudElement> elements) {
        hoveredElements.clear();
        currentMouseX = mouseX;
        currentMouseY = mouseY;
        
        var mc = Minecraft.getInstance();
        if (mc.screen instanceof AbstractContainerScreen || mc.screen instanceof InventoryScreen) {
            inScreenContext = true;
            
            for (InteractiveHudElement element : elements) {
                if (element != null && element.isHovered(mouseX, mouseY)) {
                    hoveredElements.add(element);
                }
            }
        } else {
            inScreenContext = false;
        }
    }
    
    /**
     * Check if an element should be skipped during normal HUD rendering.
     */
    public static boolean shouldSkipInHudRender(InteractiveHudElement element) {
        return inScreenContext && hoveredElements.contains(element);
    }
    
    /**
     * Render all hovered InteractiveHudElements.
     */
    public static void renderHoveredElements(GuiGraphicsExtractor context, Minecraft mc) {
        if (!inScreenContext || hoveredElements.isEmpty()) {
            return;
        }
        
        for (InteractiveHudElement element : hoveredElements) {
            element.render(context, mc, (int) currentMouseX, (int) currentMouseY);
        }
    }
    
    /**
     * Check if currently in a screen context (inventory or handledscreen).
     */
    public static boolean isInScreenContext() {
        return inScreenContext;
    }
    
    /**
     * Get list of currently hovered elements.
     */
    public static List<InteractiveHudElement> getHoveredElements() {
        return new ArrayList<>(hoveredElements);
    }
    
    /**
     * In edit mode, prevent normal HUD rendering to avoid double rendering.
     */
    public static void setEditMode(boolean editMode) {
        inEditMode = editMode;
        if (editMode) {

            hoveredElements.clear();
            inScreenContext = false;
        }
    }
    
    /**
     * Check if in edit mode.
     */
    public static boolean isInEditMode() {
        return inEditMode;
    }
    
    /**
     * Reset the context.
     */
    public static void reset() {
        hoveredElements.clear();
        inScreenContext = false;
        inEditMode = false;
        currentMouseX = 0;
        currentMouseY = 0;
    }
}
