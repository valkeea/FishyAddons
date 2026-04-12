package me.valkeea.fishyaddons.vconfig.ui.model;

/**
 * Represents a rectangular area for UI controls.
 */
public class Bounds {
    public final int x;
    public final int y;
    public final int width;
    public final int height;
    
    public Bounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    public boolean contains(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width &&
               mouseY >= y && mouseY < y + height;
    }
    
    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width &&
               mouseY >= y && mouseY < y + height;
    }
    
    public int centerX() {
        return x + width / 2;
    }
    
    public int centerY() {
        return y + height / 2;
    }
    
    public int right() {
        return x + width;
    }
    
    public int bottom() {
        return y + height;
    }

    public boolean intersects(Bounds other) {
        return x < other.right() && right() > other.x &&
               y < other.bottom() && bottom() > other.y;
    }
    
    @Override
    public String toString() {
        return String.format("VCBounds[x=%d, y=%d, width=%d, height=%d]", x, y, width, height);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Bounds)) return false;
        Bounds other = (Bounds) obj;
        return x == other.x && y == other.y && 
               width == other.width && height == other.height;
    }
    
    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        result = 31 * result + width;
        result = 31 * result + height;
        return result;
    }
}
