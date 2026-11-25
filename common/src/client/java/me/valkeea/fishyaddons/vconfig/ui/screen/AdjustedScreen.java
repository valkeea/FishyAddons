package me.valkeea.fishyaddons.vconfig.ui.screen;

import me.valkeea.fishyaddons.vconfig.ui.layout.UIScaleCalculator;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;

/**
 * Abstract screen for bypassing internal GUI scaling.
 * Child classes work in a transformed coordinate space where dimensions are rawPixels / uiScale.
 */
public abstract class AdjustedScreen extends Screen {
    
    /**
     * The calculated UI scale for this screen.
     * Applied after undoing Minecraft's GUI scale transformation.
     */
    protected float uiScale;
    protected int guiScale;
    
    /**
     * Raw window width in actual pixels.
     * Stored for reference and scale calculations.
     */
    protected int rawWidth;
    
    /**
     * Raw window height in actual pixels.
     * Stored for reference and scale calculations.
     */
    protected int rawHeight;
    
    protected AdjustedScreen(Component title) {
        super(title);
    }
    
    @Override
    protected void init() {
        super.init();

        var window = minecraft.getWindow();
        rawWidth = window.getWidth();
        rawHeight = window.getHeight();
        
        uiScale = calculateUIScale(rawWidth, rawHeight);
        guiScale = minecraft.getWindow().getGuiScale();
        
        width = (int)(rawWidth / uiScale);
        height = (int)(rawHeight / uiScale);  
    }
    
    /**
     * Calculate UI scale for this screen.
     * 
     * @param rawWidth Raw window width in pixels
     * @param rawHeight Raw window height in pixels
     * @return The UI scale to apply
     */
    protected float calculateUIScale(int rawWidth, int rawHeight) {
        return UIScaleCalculator.calculateUIScale(rawWidth);
    }
    
    @Override
    public final void extractRenderState(GuiGraphicsExtractor gge, int mX, int mY, float delta) {
        var matrices = gge.pose();
        matrices.pushMatrix();
        
        try {
            matrices.scale(1.0f / (float)guiScale, 1.0f / (float)guiScale);
            matrices.scale(uiScale, uiScale);

            int adjustedMouseX = (int)(mX * guiScale / uiScale);
            int adjustedMouseY = (int)(mY * guiScale / uiScale);
            
            extractContent(gge, adjustedMouseX, adjustedMouseY, delta);
            gge.extractDeferredElements(adjustedMouseX, adjustedMouseY, delta);
            
        } finally {
            matrices.popMatrix();
        }
    }
    
    /**
     * Render the screen content with adjusted coordinates.
     * 
     * @param context Draw context with transformed matrices
     * @param mouseX Mouse X in transformed coordinate space
     * @param mY Mouse Y in transformed coordinate space
     * @param delta Frame delta time
     */
    protected void extractContent(GuiGraphicsExtractor gge, int mouseX, int mY, float delta) {
        super.extractRenderState(gge, mouseX, mY, delta);
    }
    
    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        MouseButtonEvent adjusted = adjustMouseClick(click);
        return super.mouseClicked(adjusted, doubled);
    }
    
    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        MouseButtonEvent adjusted = adjustMouseClick(click);
        return super.mouseReleased(adjusted);
    }
    
    @Override
    public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
        MouseButtonEvent adjusted = adjustMouseClick(click);
        double adjustedOffsetX = offsetX * guiScale / uiScale;
        double adjustedOffsetY = offsetY * guiScale / uiScale;
        return super.mouseDragged(adjusted, adjustedOffsetX, adjustedOffsetY);
    }
    
    @Override
    public boolean mouseScrolled(double mX, double mY, double horizontalAmount, double verticalAmount) {
        double adjustedMouseX = mX * guiScale / uiScale;
        double adjustedMouseY = mY * guiScale / uiScale;

        return super.mouseScrolled(adjustedMouseX, adjustedMouseY, horizontalAmount, verticalAmount);
    }
    
    // Adjust a Click's coordinates from raw screen space to the adjusted UI space
    private MouseButtonEvent adjustMouseClick(MouseButtonEvent click) {
        double adjustedX = click.x() * guiScale / uiScale;
        double adjustedY = click.y() * guiScale / uiScale;
        
        var input = new MouseButtonInfo(click.button(), click.modifiers());
        return new MouseButtonEvent(adjustedX, adjustedY, input);
    }
    
    /**
     * Adjust a Click object before processing click or drag events.
     */
    protected MouseButtonEvent adjustClick(MouseButtonEvent click) {
        return adjustMouseClick(click);
    }
    
    /**
     * Get the current UI scale factor.
     */
    public float getUIScale() {
        return uiScale;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mX, int mY, float delta) {
        // Handled manually in renderContent
    }    
}
