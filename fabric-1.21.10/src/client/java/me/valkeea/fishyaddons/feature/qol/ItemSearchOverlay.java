package me.valkeea.fishyaddons.feature.qol;

import java.util.HashSet;
import java.util.Set;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.event.EventPhase;
import me.valkeea.fishyaddons.event.EventPriority;
import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.hud.core.ElementRegistry;
import me.valkeea.fishyaddons.hud.ui.SearchHudElement;
import me.valkeea.fishyaddons.mixin.HandledScreenAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

@SuppressWarnings("squid:S6548")
public class ItemSearchOverlay {
    private static ItemSearchOverlay instance;
    private String lastSearchTerm = "";
    private Set<Integer> matchingSlots = new HashSet<>();
    private boolean isSearching = false;
    private SearchHudElement searchHudElement;
    private long lastSlotContentHash = 0;
    private static final float DEFAULT_OVERLAY_OPACITY = 0.5f;
    
    private ItemSearchOverlay() {
        for (var element : ElementRegistry.getElements()) {
            if (element instanceof SearchHudElement searchElement) {
                searchHudElement = searchElement;
                break;
            }
        }
    }

    public static void init() {
        FaEvents.SCREEN_MOUSE_CLICK.register(event -> ItemSearchOverlay.getInstance().handleMouseClicked(event.click, event.doubled), EventPriority.LOW, EventPhase.PRE);
    }

    public static ItemSearchOverlay getInstance() {
        if (instance == null) {
            instance = new ItemSearchOverlay();
        }
        return instance;
    }

    public void refresh() {
        setEnabled(!isEnabled());
    }
    
    public float getOpacity() {
        return FishyConfig.getFloat(Key.INV_SEARCH_OPACITY, DEFAULT_OVERLAY_OPACITY);
    }
    
    public void setOpacity(float opacity) {
        FishyConfig.setFloat(Key.INV_SEARCH_OPACITY, Math.clamp(opacity, 0.0f, 1.0f));
    }
    
    public void render(DrawContext context, HandledScreen<?> screen) {
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
            var client = MinecraftClient.getInstance();
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

        var accessor = (HandledScreenAccessor) screen;
        int guiX = accessor.getX();
        int guiY = accessor.getY();

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
    }
    
    private void renderSegmentedOverlay(DrawContext context, int screenWidth, int screenHeight, Set<Rectangle> excludedAreas) {
        float opacity = getOpacity();
        int alpha = (int)(opacity * 255) << 24;
        int overlayColor = alpha;

        renderHorizontalStrips(context, screenWidth, screenHeight, excludedAreas, overlayColor);
    }
    
    private void renderHorizontalStrips(DrawContext context, int screenWidth, int screenHeight, Set<Rectangle> excludedAreas, int overlayColor) {
        for (int y = 0; y < screenHeight; y++) {
            int x = 0;
            
            while (x < screenWidth) {

                Rectangle blockingRect = findBlockingRect(x, y, excludedAreas);
                if (blockingRect != null) {
                    if (x < blockingRect.x) {
                        context.fillGradient(x, y, blockingRect.x, y + 1, overlayColor, overlayColor);
                    }

                    x = blockingRect.x + blockingRect.width;
                } else {
                    context.fillGradient(x, y, screenWidth, y + 1, overlayColor, overlayColor);
                    break;
                }
            }
        }
    }
    
    private Rectangle findBlockingRect(int startX, int y, Set<Rectangle> excludedAreas) {
        Rectangle closest = null;
        
        for (Rectangle rect : excludedAreas) {
            if (y >= rect.y && y < rect.y + rect.height && 
                rect.x + rect.width > startX && 
                (closest == null || rect.x < closest.x)) {
                closest = rect;
            }
        }
        
        return closest;
    }
    
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
    }
    
    public boolean handleKeyPressed(KeyInput input) {
        if (!isEnabled()) {
            return false;
        }
        
        if (searchHudElement != null) {
            return searchHudElement.handleKeyPressed(input);
        }
        return false;
    }
    
    public boolean handleMouseClicked(Click click, boolean doubled) {
        if (!isEnabled()) {
            return false;
        }
        
        if (searchHudElement != null) {
            return searchHudElement.handleMouseClick(click, doubled);
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
        long hash = 0;
        for (var slot : screen.getScreenHandler().slots) {
            var stack = slot.getStack();
            if (stack != null && !stack.isEmpty()) {
                hash = hash * 31 + stack.getItem().hashCode();
                hash = hash * 31 + stack.getCount();
                hash = hash * 31 + stack.hashCode();
            }
            hash = hash * 31 + slot.id;
        }
        return hash;
    }
    
    public void forceRefresh() {
        lastSlotContentHash = 0;
    }
    
    public void renderOverlay(DrawContext context, HandledScreen<?> screen, String searchTerm) {
        if (searchTerm.isEmpty()) {
            return;
        }
        
        matchingSlots.clear();
        String lowerSearchTerm = searchTerm.toLowerCase();
        for (var slot : screen.getScreenHandler().slots) {
            var stack = slot.getStack();
            if (stack != null && !stack.isEmpty() && matchesSearch(stack, lowerSearchTerm)) {
                matchingSlots.add(slot.id);
            }
        }
        
        if (matchingSlots.isEmpty()) {
            return;
        }
        
        var accessor = (HandledScreenAccessor) screen;
        int guiX = accessor.getX();
        int guiY = accessor.getY();

        Set<Rectangle> excludedAreas = new HashSet<>();
        for (var slot : screen.getScreenHandler().slots) {
            if (matchingSlots.contains(slot.id)) {
                int slotScreenX = guiX + slot.x;
                int slotScreenY = guiY + slot.y;
                Rectangle slotRect = new Rectangle(slotScreenX, slotScreenY, 16, 16);
                excludedAreas.add(slotRect);
            }
        }

        renderSegmentedOverlay(context, screen.width, screen.height, excludedAreas);
    }
}
