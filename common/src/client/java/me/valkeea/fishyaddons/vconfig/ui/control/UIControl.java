package me.valkeea.fishyaddons.vconfig.ui.control;

import me.valkeea.fishyaddons.vconfig.ui.model.Bounds;
import me.valkeea.fishyaddons.vconfig.ui.model.ClickContext;
import me.valkeea.fishyaddons.vconfig.ui.model.DragContext;
import me.valkeea.fishyaddons.vconfig.ui.render.VCRenderContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;

public interface UIControl {

    /**
     * Render this control at the specified position.
     * 
     * @param rCtx The rendering context containing draw context, mouse position, etc.
     * @param x The X coordinate to render at
     * @param y The Y coordinate to render at
     */
    void render(VCRenderContext rCtx, int x, int y);

    /**
     * Check if this control is currently hovered by the mouse.
     * 
     * @param mouseX The current mouse X position
     * @param mouseY The current mouse Y position
     * @return true if the control is hovered, false otherwise
     */
    boolean isHovered(double mouseX, double mouseY);
    
    /**
     * Get the preferred height for this control at the current scale.
     * 
     * @return The preferred height in pixels, default is button height
     */
    int getPreferredHeight();
    
    /**
     * Get the preferred width for this control at the current scale.
     * 
     * @return The preferred width in pixels, default is button width
     */
    int getPreferredWidth();
    
    /**
     * Update the hover state of this control based on mouse position.
     * This should trigger bounds calculation in implementing classes.
     * 
     * @param mouseX The current mouse X position
     * @param mouseY The current mouse Y position
     * @param x The X coordinate of the control
     * @param y The Y coordinate of the control
     */
    void updateHover(int mouseX, int mouseY, int x, int y);
    
    /**
     * Get the current bounds of this control.
     * May return null if the control hasn't been rendered yet.
     * 
     * @return The bounds of this control, or null
     */
    Bounds getBounds();

    /**
     * Handle a mouse click event.
     * 
     * @param cCtx The click context containing click position, button, etc.
     * @param x The X coordinate of the control
     * @param y The Y coordinate of the control
     * @return true if the click was consumed by this control, false otherwise
     */
    default boolean handleClick(ClickContext cCtx, int x, int y) {
        return false;
    }
    
    /**
     * Handle a mouse drag event.
     * 
     * @param dCtx The drag context containing drag offset, position, etc.
     * @param x The X coordinate of the control
     * @param y The Y coordinate of the control
     * @return true if the drag was consumed by this control, false otherwise
     */
    default boolean handleDrag(DragContext dCtx, int x, int y) {
        return false;
    }

    /**
     * Handle a mouse release event.
     * 
     * @param cCtx The click context containing release position, button, etc.
     * @param x The X coordinate of the control
     * @param y The Y coordinate of the control
     * @return true if the release was consumed by this control, false otherwise
     */
    default boolean handleMouseRelease(ClickContext cCtx) {
        return false;
    }

    /**
     * Handle a key press event.
     * 
     * @param input The key input context
     * @return true if the key press was consumed by this control, false otherwise
     */
    default boolean handleKeyPress(KeyInput input) {
        return false;
    }

    /**
     * Handle a character input event.
     * @param charInput The character input context
     * @return true if the character input was consumed by this control, false otherwise
     */
    default boolean handleCharInput(CharInput charInput) {
        return false;
    }    
    
    /**
     * Render any overlays this control displays.
     * Called by VCScreen after all regular rendering to ensure overlays appear on top.
     * 
     * @param rCtx The rendering context
     * @return true if an overlay was rendered
     */
    default boolean renderOverlay(VCRenderContext rCtx, int scrollX, int endY) {
        return false;
    }
    
    /**
     * Check if this control has an active overlay that should block other interactions.
     * 
     * @return true if an overlay is currently active
     */
    default boolean hasActiveOverlay() {
        return false;
    }
    
    /**
     * Handle scroll events when this control has an active overlay.
     * Only called if hasActiveOverlay() returns true.
     * 
     * @param amount The scroll amount (positive = up, negative = down)
     * @return true if the scroll was consumed
     */
    default boolean handleOverlayScroll(double amount) {
        return false;
    }
}
