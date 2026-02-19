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
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.ItemStack;

@SuppressWarnings("squid:S6548")
public class ItemSearchOverlay {
    private static ItemSearchOverlay instance;
    private Set<Integer> matchingSlots = new HashSet<>();
    private SearchHudElement searchField;
    private static final float DEFAULT_OVERLAY_OPACITY = 0.5f;
    
    private String lastSearchTerm = "";
    private boolean cacheValid = false;
    private boolean enabled = false;
    
    private ItemSearchOverlay() {
        for (var element : ElementRegistry.getElements()) {
            if (element instanceof SearchHudElement hud) {
                searchField = hud;
                break;
            }
        }
    }

    public static void init() {
        FaEvents.SCREEN_MOUSE_CLICK.register(
            event -> getInstance().handleMouseClicked(event.click, event.doubled),
            EventPriority.HIGH, EventPhase.PRE
        );

        FaEvents.GUI_CHANGE.register(event -> getInstance().invalidateCache());
    }

    public static ItemSearchOverlay getInstance() {
        if (instance == null) {
            instance = new ItemSearchOverlay();
            refresh();
        }
        return instance;
    }

    public static void refresh() {
        getInstance().enabled = FishyConfig.getState(Key.INV_SEARCH, false);
    }

    private boolean matchesSearch(ItemStack stack, String searchTerm) {
        if (stack == null || stack.isEmpty()) return false;

        var itemName = stack.getName().getString().toLowerCase();
        if (itemName.contains(searchTerm)) return true;

        try {

            var components = stack.getComponents();
            var loreComponent = components.get(net.minecraft.component.DataComponentTypes.LORE);
            
            if (loreComponent != null) {
                for (var line : loreComponent.lines()) {
                    if (line.getString().toLowerCase().contains(searchTerm)) {
                        return true;
                    }
                }
            }

        } catch (Exception e) {
            // Ignore access error
        }
        
        return false;
    }

    private void renderSegmentedOverlay(DrawContext ctx, int screenWidth, int screenHeight, Set<Rectangle> excludedAreas) {

        float opacity = getOpacity();
        int alpha = (int)(opacity * 255) << 24;
        int overlayColor = alpha;

        renderHorizontalStrips(ctx, screenWidth, screenHeight, excludedAreas, overlayColor);
    }
    
    private void renderHorizontalStrips(DrawContext ctx, int screenWidth, int screenHeight, Set<Rectangle> excludedAreas, int overlayColor) {
        for (int y = 0; y < screenHeight; y++) {
            int x = 0;
            
            while (x < screenWidth) {

                var blockingRect = findBlockingRect(x, y, excludedAreas);
                if (blockingRect != null) {
                    if (x < blockingRect.x) {
                        ctx.fillGradient(x, y, blockingRect.x, y + 1, overlayColor, overlayColor);
                    }

                    x = blockingRect.x + blockingRect.width;
                } else {
                    ctx.fillGradient(x, y, screenWidth, y + 1, overlayColor, overlayColor);
                    break;
                }
            }
        }
    }
    
    private Rectangle findBlockingRect(int startX, int y, Set<Rectangle> excludedAreas) {
        Rectangle closest = null;
        
        for (Rectangle r : excludedAreas) {
            if (y >= r.y && y < r.y + r.height && 
                r.x + r.width > startX && 
                (closest == null || r.x < closest.x)) {
                closest = r;
            }
        }
        
        return closest;
    }

    public void renderOverlay(DrawContext ctx, HandledScreen<?> screen, String searchTerm) {
        if (searchTerm.isEmpty()) return;
        
        var lowerSearchTerm = searchTerm.toLowerCase();
        var searchChanged = !lowerSearchTerm.equals(lastSearchTerm);

        if (searchChanged || !cacheValid) {
            matchingSlots.clear();
            lastSearchTerm = lowerSearchTerm;
            
            for (var slot : screen.getScreenHandler().slots) {
                var stack = slot.getStack();
                if (stack != null && !stack.isEmpty() && matchesSearch(stack, lowerSearchTerm)) {
                    matchingSlots.add(slot.id);
                }
            }
            
            cacheValid = true;
        }
        
        if (matchingSlots.isEmpty()) {
            return;
        }
        
        var hsa = (HandledScreenAccessor) screen;
        int guiX = hsa.getX();
        int guiY = hsa.getY();

        Set<Rectangle> excluded = new HashSet<>();

        for (var slot : screen.getScreenHandler().slots) {
            if (matchingSlots.contains(slot.id)) {
                int slotScreenX = guiX + slot.x;
                int slotScreenY = guiY + slot.y;
                Rectangle slotRect = new Rectangle(slotScreenX, slotScreenY, 16, 16);
                excluded.add(slotRect);
            }
        }

        renderSegmentedOverlay(ctx, screen.width, screen.height, excluded);
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
        if (!isEnabled()) return false;
        
        if (searchField != null) {
            return searchField.handleKeyPressed(input);
        }
        return false;
    }
    
    public boolean handleCharTyped(CharInput input) {
        if (!isEnabled()) return false;
        
        if (searchField != null) {
            return searchField.handleCharTyped(input);
        }
        return false;
    }
    
    public boolean handleMouseClicked(Click click, boolean doubled) {
        if (!isEnabled()) return false;
        if (searchField != null) return searchField.handleMouseClick(click, doubled);
        return false;
    }
    
    public void clearSearch() {
        if (searchField != null) {
            searchField.clearSearch();
            searchField.setOverlayActive(false);
        }
        matchingSlots.clear();
        invalidateCache();
    }

    public void invalidateCache() {
        cacheValid = false;
    }
    
    public static boolean isEnabled() {
        return getInstance().enabled;
    }
    
    public void setEnabled(boolean enabled) {
        FishyConfig.enable(Key.INV_SEARCH, enabled);
        if (!enabled) clearSearch();
    }
    
    public void setSearchField(SearchHudElement field) {
        this.searchField = field;
    }

    public float getOpacity() {
        return FishyConfig.getFloat(Key.INV_SEARCH_OPACITY, DEFAULT_OVERLAY_OPACITY);
    }
    
    public void setOpacity(float opacity) {
        FishyConfig.setFloat(Key.INV_SEARCH_OPACITY, Math.clamp(opacity, 0.0f, 1.0f));
    }    
}
