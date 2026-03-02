package me.valkeea.fishyaddons.hud.ui;

import me.valkeea.fishyaddons.hud.core.HudUtils;
import me.valkeea.fishyaddons.mixin.HandledScreenAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.util.Identifier;

/**
 * Button element for GenericContainerScreen, indexed to render around the container
 */
public class IndexedButton {
    private final short index;
    private final Runnable onClick;
    private final Identifier icon;
    private final String requiredGuiName;
    private int x;
    private int y;
    private int size = 16;   

    public IndexedButton(HandledScreen<?> screen, short index, Runnable onClick, Identifier icon) {
        this(screen, index, onClick, icon, null);
    }

    public IndexedButton(HandledScreen<?> screen, short index, Runnable onClick,
        Identifier icon, String requiredGuiName) {
        
        if (!(screen instanceof GenericContainerScreen) || index < 1 || index > 46) {
            throw new IllegalArgumentException("Invalid screen or index out of bounds for IndexedButton");
        }

        this.index = index;
        this.onClick = onClick;
        this.icon = icon;
        this.requiredGuiName = requiredGuiName;
    }

    public void render(DrawContext context, int mouseX, int mouseY) {

        var cs = MinecraftClient.getInstance().currentScreen;
        if (!(cs instanceof GenericContainerScreen gcs) ||
            !isVisible(gcs.getTitle().getString())) {
            return;
        }

        calculatePosition(gcs);

        HudUtils.iconButton(
            context, x, y, size, size, isMouseOver(mouseX, mouseY),
            true, icon
        );
    }

    public void calculatePosition(GenericContainerScreen gcs) {

        var handler = gcs.getScreenHandler();
        if (handler == null) {
            this.x = 100;
            this.y = 100;
            return;
        }
        
        var hsa = (HandledScreenAccessor) gcs;
        
        int guiLeft = hsa.getX();
        int guiRight = hsa.getX() + hsa.getBackgroundWidth();
        int guiTop = hsa.getY();
        
        int btnIdx = index;

        if (btnIdx < 10) { // top row, left to right
            this.x = guiLeft + btnIdx * 18; 
            this.y = guiTop - 18;

        } else if (btnIdx < 20) { // bottom, left to right
            btnIdx -= 10;
            this.x = guiLeft + btnIdx * 18;
            this.y = guiTop + hsa.getBackgroundHeight() + 2;

        } else if (btnIdx < 32) { // left column, top to bottom
            btnIdx -= 20;
            this.x = guiLeft - 18;
            this.y = guiTop + btnIdx * 18;

        } else { // right, top to bottom
            btnIdx -= 32;
            this.x = guiRight + 2;
            this.y = guiTop + btnIdx * 18;
        }
    }

    public boolean isVisible(String currentGuiName) {
        return !(requiredGuiName != null && !currentGuiName.contains(requiredGuiName));
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + size && mouseY >= y && mouseY <= y + size;
    }

    public boolean mouseClicked(double mouseX, double mouseY) {

        if (isMouseOver(mouseX, mouseY) && onClick != null) {
            onClick.run();
            return true;
        }

        return false;
    }    

    public void setSize(int size) { this.size = size; }
    public int getSize() { return size; }
    public int getX() { return x; }
    public int getY() { return y; }
}
