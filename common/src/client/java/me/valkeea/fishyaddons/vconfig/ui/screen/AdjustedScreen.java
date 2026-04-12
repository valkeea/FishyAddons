package me.valkeea.fishyaddons.vconfig.ui.screen;

import me.valkeea.fishyaddons.vconfig.ui.layout.UIScaleCalculator;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.MouseInput;
import net.minecraft.text.Text;

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
    
    protected AdjustedScreen(Text title) {
        super(title);
    }
    
    @Override
    protected void init() {
        super.init();
        
        var window = client.getWindow();
        rawWidth = window.getWidth();
        rawHeight = window.getHeight();
        
        uiScale = calculateUIScale(rawWidth, rawHeight);
        
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
    public final void render(DrawContext context, int mouseX, int mouseY, float delta) {
        var matrices = context.getMatrices();
        matrices.pushMatrix();
        
        try {
            double guiScale = client.getWindow().getScaleFactor();

            matrices.scale(1.0f / (float)guiScale, 1.0f / (float)guiScale);
            matrices.scale(uiScale, uiScale);

            int adjustedMouseX = (int)(mouseX * guiScale / uiScale);
            int adjustedMouseY = (int)(mouseY * guiScale / uiScale);
            
            renderContent(context, adjustedMouseX, adjustedMouseY, delta);
            
        } finally {
            matrices.popMatrix();
        }
    }
    
    /**
     * Render the screen content with adjusted coordinates.
     * 
     * @param context Draw context with transformed matrices
     * @param mouseX Mouse X in transformed coordinate space
     * @param mouseY Mouse Y in transformed coordinate space
     * @param delta Frame delta time
     */
    protected void renderContent(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
    }
    
    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        Click adjusted = adjustMouseClick(click);
        return super.mouseClicked(adjusted, doubled);
    }
    
    @Override
    public boolean mouseReleased(Click click) {
        Click adjusted = adjustMouseClick(click);
        return super.mouseReleased(adjusted);
    }
    
    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        Click adjusted = adjustMouseClick(click);
        double guiScale = client.getWindow().getScaleFactor();
        double adjustedOffsetX = offsetX * guiScale / uiScale;
        double adjustedOffsetY = offsetY * guiScale / uiScale;
        return super.mouseDragged(adjusted, adjustedOffsetX, adjustedOffsetY);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        double guiScale = client.getWindow().getScaleFactor();
        double adjustedMouseX = mouseX * guiScale / uiScale;
        double adjustedMouseY = mouseY * guiScale / uiScale;
        return super.mouseScrolled(adjustedMouseX, adjustedMouseY, horizontalAmount, verticalAmount);
    }
    
    // Adjust a Click's coordinates from raw screen space to the adjusted UI space
    private Click adjustMouseClick(Click click) {
        double guiScale = client.getWindow().getScaleFactor();
        double adjustedX = click.x() * guiScale / uiScale;
        double adjustedY = click.y() * guiScale / uiScale;
        
        var input = new MouseInput(click.button(), click.modifiers());
        return new Click(adjustedX, adjustedY, input);
    }
    
    /**
     * Adjust a Click object before processing click or drag events.
     */
    protected Click adjustClick(Click click) {
        return adjustMouseClick(click);
    }
    
    /**
     * Get the current UI scale factor.
     */
    public float getUIScale() {
        return uiScale;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Handled manually in renderContent
    }    
}
