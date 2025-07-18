package me.valkeea.fishyaddons.handler;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.hud.ElementRegistry;
import me.valkeea.fishyaddons.hud.SearchHudElement;
import me.valkeea.fishyaddons.mixin.HandledScreenAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

import java.util.HashSet;
import java.util.Set;

public class ItemSearchOverlay {
    private static ItemSearchOverlay instance;
    private String lastSearchTerm = "";
    private Set<Integer> matchingSlots = new HashSet<>();
    private boolean isSearching = false;
    private SearchHudElement searchHudElement;
    private long lastSlotContentHash = 0;
    private static final String OVERLAY_OPACITY_CONFIG_KEY = "searchOverlayOpacity";
    private static final float DEFAULT_OVERLAY_OPACITY = 0.5f;
    
    private ItemSearchOverlay() {
        for (var element : ElementRegistry.getElements()) {
            if (element instanceof SearchHudElement searchElement) {
                searchHudElement = searchElement;
                break;
            }
        }
    }
    
    public static ItemSearchOverlay getInstance() {
        if (instance == null) {
            instance = new ItemSearchOverlay();
        }
        return instance;
    }
    
    public float getOpacity() {
        return FishyConfig.getFloat(OVERLAY_OPACITY_CONFIG_KEY, DEFAULT_OVERLAY_OPACITY);
    }
    
    public void setOpacity(float opacity) {
        FishyConfig.setFloat(OVERLAY_OPACITY_CONFIG_KEY, Math.clamp(opacity, 0.0f, 1.0f));
    }
    
    public void render(DrawContext context, HandledScreen<?> screen, int mouseX, int mouseY, float delta) {
        if (!isEnabled()) {
            return;
        }
        ensureSearchHudElement();
        String currentSearchTerm = searchHudElement != null ? searchHudElement.getSearchTerm() : "";
        boolean overlayActive = searchHudElement != null && searchHudElement.isOverlayActive();
        long currentSlotContentHash = calculateSlotContentHash(screen);
        if (!currentSearchTerm.equals(lastSearchTerm) || currentSlotContentHash != lastSlotContentHash) {
            updateSearch(currentSearchTerm, screen);
            lastSearchTerm = currentSearchTerm;
            lastSlotContentHash = currentSlotContentHash;
        }
    
        if (overlayActive && isSearching && !currentSearchTerm.isEmpty()) {
            renderSearchOverlay(context, screen);
        }
    }
    
    private void updateSearch(String searchTerm, HandledScreen<?> screen) {
        matchingSlots.clear();
        isSearching = !searchTerm.isEmpty();
        
        if (!isSearching) {
            return;
        }
        
        String lowerSearchTerm = searchTerm.toLowerCase();
        for (Slot slot : screen.getScreenHandler().slots) {
            ItemStack stack = slot.getStack();
            if (stack != null && !stack.isEmpty() && matchesSearch(stack, lowerSearchTerm)) {
                matchingSlots.add(slot.id);
            }
        }
    }
    
    private boolean matchesSearch(ItemStack stack, String searchTerm) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        String itemName = stack.getName().getString().toLowerCase();
        if (itemName.contains(searchTerm)) {
            return true;
        }
        
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.player != null) {
                var tooltip = stack.getTooltip(TooltipContext.DEFAULT, client.player, TooltipType.BASIC);
                for (Text line : tooltip) {
                    if (line.getString().toLowerCase().contains(searchTerm)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // Ignore tooltip errors
        }
        
        return false;
    }
    
    private void renderSearchOverlay(DrawContext context, HandledScreen<?> screen) {
        if (!isSearching || lastSearchTerm.isEmpty()) {
            return;
        }
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 300); // Render above everything
        HandledScreenAccessor accessor = (HandledScreenAccessor) screen;
        int guiX = accessor.getX();
        int guiY = accessor.getY();

        // Create a set of excluded areas (matching slots)
        Set<Rectangle> excludedAreas = new HashSet<>();
        for (Slot slot : screen.getScreenHandler().slots) {
            if (matchingSlots.contains(slot.id)) {
                int slotScreenX = guiX + slot.x;
                int slotScreenY = guiY + slot.y;
                Rectangle slotRect = new Rectangle(slotScreenX, slotScreenY, 16, 16);
                excludedAreas.add(slotRect);
            }
        }

        renderSegmentedOverlay(context, screen.width, screen.height, excludedAreas);
        context.getMatrices().pop();
    }
    
    private void renderSegmentedOverlay(DrawContext context, int screenWidth, int screenHeight, Set<Rectangle> excludedAreas) {
        int segmentSize = 3;
        
        float opacity = getOpacity();
        int alpha = (int)(opacity * 255) << 24;
        int overlayColor = alpha; // Black with dynamic alpha

        for (int y = 0; y < screenHeight; y += segmentSize) {
            for (int x = 0; x < screenWidth; x += segmentSize) {
                // Calculate segment bounds
                int segmentEndX = Math.min(x + segmentSize, screenWidth);
                int segmentEndY = Math.min(y + segmentSize, screenHeight);
                
                // Check if this segment overlaps with any excluded area
                if (!overlapsWithExcludedArea(x, y, segmentEndX - x, segmentEndY - y, excludedAreas)) {
                    context.fill(x, y, segmentEndX, segmentEndY, overlayColor);
                }
            }
        }
    }
    
    private boolean overlapsWithExcludedArea(int x, int y, int width, int height, Set<Rectangle> excludedAreas) {
        Rectangle segment = new Rectangle(x, y, width, height);
        for (Rectangle excluded : excludedAreas) {
            if (segment.intersects(excluded)) {
                return true;
            }
        }
        return false;
    }
    
    // Area calculations
    private static class Rectangle {
        int x;
        int y;
        int width;
        int height;
        
        Rectangle(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
        
        boolean intersects(Rectangle other) {
            return x < other.x + other.width &&
                   x + width > other.x &&
                   y < other.y + other.height &&
                   y + height > other.y;
        }
    }
    
    public boolean handleKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isEnabled()) {
            return false;
        }
        
        if (searchHudElement != null) {
            return searchHudElement.handleKeyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }
    
    public boolean handleMouseClicked(double mouseX, double mouseY, int button) {
        if (!isEnabled()) {
            return false;
        }
        
        if (searchHudElement != null) {
            return searchHudElement.handleMouseClicked(mouseX, mouseY, button);
        }
        return false;
    }
    
    public void clearSearch() {
        if (searchHudElement != null) {
            searchHudElement.clearSearch();
            searchHudElement.setOverlayActive(false);
        }
        matchingSlots.clear();
        isSearching = false;
        lastSearchTerm = "";
        lastSlotContentHash = 0;
    }
    
    public boolean isEnabled() {
        return FishyConfig.getState("invSearch", false);
    }
    
    public void setEnabled(boolean enabled) {
        FishyConfig.enable("invSearch", enabled);
        if (!enabled) {
            clearSearch();
        }
    }
    
    public void setSearchHudElement(SearchHudElement element) {
        this.searchHudElement = element;
    }
    
    private void ensureSearchHudElement() {
        if (searchHudElement == null) {
            for (var element : ElementRegistry.getElements()) {
                if (element instanceof SearchHudElement searchElement) {
                    searchHudElement = searchElement;
                    break;
                }
            }
        }
    }
    
    private long calculateSlotContentHash(HandledScreen<?> screen) {
        // Calculate a hash of all slot contents to detect changes
        long hash = 0;
        for (Slot slot : screen.getScreenHandler().slots) {
            ItemStack stack = slot.getStack();
            if (stack != null && !stack.isEmpty()) {
                // Combine item type, count into hash
                hash = hash * 31 + stack.getItem().hashCode();
                hash = hash * 31 + stack.getCount();
                
                // Use the stack's own hashCode which includes NBT data
                hash = hash * 31 + stack.hashCode();
            }
            // Include slot position in hash to detect slot reordering
            hash = hash * 31 + slot.id;
        }
        return hash;
    }
    
    public void forceRefresh() {
        lastSlotContentHash = 0;
    }
}