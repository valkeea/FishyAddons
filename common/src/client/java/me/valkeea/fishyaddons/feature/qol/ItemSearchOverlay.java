package me.valkeea.fishyaddons.feature.qol;

import java.util.HashSet;
import java.util.Set;

import me.valkeea.fishyaddons.event.EventGroup;
import me.valkeea.fishyaddons.event.impl.ContainerKeyInputEvent;
import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.hud.core.ElementRegistry;
import me.valkeea.fishyaddons.hud.ui.SearchHudElement;
import me.valkeea.fishyaddons.mixin.AbstractContainerScreenAccessor;
import me.valkeea.fishyaddons.vconfig.annotation.VCListener;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.api.DoubleKey;
import me.valkeea.fishyaddons.vconfig.api.IntKey;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("squid:S6548")
@VCModule
public class ItemSearchOverlay {
    private static final EventGroup events = new EventGroup();
    private final Set<Integer> matchingSlots = new HashSet<>();

    private static ItemSearchOverlay instance;
    private SearchHudElement searchField;
    
    private String lastSearchTerm = "";
    private int lastHash = 0;
    private boolean enabled = false;
    private double opacity = DoubleKey.INV_SEARCH_OPACITY.getDefault();
    private int color = IntKey.INV_SEARCH_COLOR.getDefault();
    
    private ItemSearchOverlay() {
        for (var e : ElementRegistry.getElements()) {
            if (e instanceof SearchHudElement hud) {
                searchField = hud;
                break;
            }
        }
    }

    public static void register(boolean v) {
        events.subscribe(v, () -> events.addAll(
            () -> FaEvents.CONTAINER_CLICK.register(e -> getInstance().handleMouseClicked(e.click, e.doubled)),
            () -> FaEvents.CONTAINER_KEY_INPUT.register(e -> getInstance().handleKeyPressed(e)),
            () -> FaEvents.SCREEN_CLOSE.register(_ -> getInstance().onScreenClose())            
        ));
    }

    @VCListener(BooleanKey.INV_SEARCH)
    private void onInvSearchChanged(boolean v) {
        enabled = v;
        if (!enabled) clearSearch();
        register(enabled);
    }

    @VCListener(doubles = DoubleKey.INV_SEARCH_OPACITY)
    private void onOpacityChanged(double v) {
        opacity = v;
    }

    @VCListener(ints = IntKey.INV_SEARCH_COLOR)
    private void onColorChanged(int v) {
        color = v;
    }    

    private void onScreenClose() {
        matchingSlots.clear();
        lastSearchTerm = "";
        lastHash = 0;
    }

    private int computeHash(Object handler) {
        
        try {
            var slots = ((net.minecraft.world.inventory.AbstractContainerMenu) handler).slots;
            int h = 1;
            for (var slot : slots) {
                var stack = slot.getItem();
                if (!stack.isEmpty()) {
                    h = 31 * h + stack.getItem().hashCode();
                    h = 31 * h + stack.getCount();
                    h = 31 * h + stack.getComponents().hashCode();
                }
            }
            return h;

        } catch (Exception _) {
            return 0;
        }
    }

    public static ItemSearchOverlay getInstance() {
        if (instance == null) {
            instance = new ItemSearchOverlay();
            refresh();
        }
        return instance;
    }

    public static void refresh() {
        var instance = getInstance();
        instance.enabled = Config.get(BooleanKey.INV_SEARCH);
        instance.opacity = Config.get(DoubleKey.INV_SEARCH_OPACITY);
        instance.color = Config.get(IntKey.INV_SEARCH_COLOR);
    }

    private boolean matchesSearch(ItemStack stack, String searchTerm) {
        if (stack == null || stack.isEmpty()) return false;

        var itemName = stack.getHoverName().getString().toLowerCase();
        if (itemName.contains(searchTerm)) return true;

        try {

            var components = stack.getComponents();
            var lore = components.get(net.minecraft.core.component.DataComponents.LORE);
            
            if (lore != null) {
                for (var line : lore.lines()) {
                    if (line.getString().toLowerCase().contains(searchTerm)) {
                        return true;
                    }
                }
            }
        } catch (Exception _) {
            // Ignore access error
        }
        
        return false;
    }

    private void renderSegmentedOverlay(GuiGraphicsExtractor ctx, int screenWidth, int screenHeight, Set<Rectangle> excludedAreas) {
        int overlayColor = (int) (opacity * 255) << 24 | (color & 0x00FFFFFF);
        renderHorizontalStrips(ctx, screenWidth, screenHeight, excludedAreas, overlayColor);
    }
    
    private void renderHorizontalStrips(GuiGraphicsExtractor ctx, int screenWidth, int screenHeight, Set<Rectangle> excludedAreas, int overlayColor) {
        for (int y = 0; y < screenHeight; y++) {
            int x = 0;
            
            while (x < screenWidth) {

                var blockingRect = findBlockingRect(x, y, excludedAreas);
                if (blockingRect != null) {
                    if (x < blockingRect.x) {
                        ctx.fill(x, y, blockingRect.x, y + 1, overlayColor);
                    }

                    x = blockingRect.x + blockingRect.width;
                } else {
                    ctx.fill(x, y, screenWidth, y + 1, overlayColor);
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

    public void renderOverlay(GuiGraphicsExtractor ctx, AbstractContainerScreen<?> screen, String searchTerm) {
        if (searchTerm.isEmpty()) return;
        
        var lowerSearchTerm = searchTerm.toLowerCase();
        var searchChanged = !lowerSearchTerm.equals(lastSearchTerm);
        var handler = screen.getMenu();
        if (handler == null) return;

        int currentHash = computeHash(handler);
        boolean slotsChanged = currentHash != lastHash;

        if (searchChanged || slotsChanged) {
            matchingSlots.clear();
            lastSearchTerm = lowerSearchTerm;
            lastHash = currentHash;
            
            for (var slot : handler.slots) {
                var stack = slot.getItem();
                if (stack != null && !stack.isEmpty() && matchesSearch(stack, lowerSearchTerm)) {
                    matchingSlots.add(slot.index);
                }
            }
        }
        
        if (matchingSlots.isEmpty()) {
            return;
        }
        
        var hsa = (AbstractContainerScreenAccessor) screen;
        int guiX = hsa.getX();
        int guiY = hsa.getY();

        Set<Rectangle> excluded = new HashSet<>();

        for (var slot : handler.slots) {
            if (matchingSlots.contains(slot.index)) {
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
    
    public void handleKeyPressed(ContainerKeyInputEvent e) {
        if (isEnabled() && searchField != null && searchField.handleKeyPressed(e.input)) {
            e.consumeWithTrue();
        }
    }
    
    public boolean handleCharTyped(CharacterEvent input) {
        if (!isEnabled()) return false;
        if (searchField != null) {
            return searchField.handleCharTyped(input);
        }
        return false;
    }
    
    public boolean handleMouseClicked(MouseButtonEvent click, boolean doubled) {
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
        lastSearchTerm = "";
        lastHash = 0;
    }
    
    public static boolean isEnabled() {
        return getInstance().enabled;
    }
    
    public void setSearchField(SearchHudElement field) {
        this.searchField = field;
    }
}
