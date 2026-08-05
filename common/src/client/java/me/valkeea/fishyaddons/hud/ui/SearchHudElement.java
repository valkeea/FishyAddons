package me.valkeea.fishyaddons.hud.ui;

import java.awt.Rectangle;
import java.util.List;

import me.valkeea.fishyaddons.compat.McApi;
import me.valkeea.fishyaddons.feature.qol.ItemSearchOverlay;
import me.valkeea.fishyaddons.hud.base.InteractiveHudElement;
import me.valkeea.fishyaddons.hud.core.HudElementState;
import me.valkeea.fishyaddons.tool.FishyMode;
import me.valkeea.fishyaddons.ui.GuiUtil;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.IntKey;
import me.valkeea.fishyaddons.vconfig.ui.widget.VCTextField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class SearchHudElement extends InteractiveHudElement {
    private static final String SEARCH_PLACEHOLDER = "right-click to search...";
    private static final String EDITING_MODE_TEXT = "Search Field";
    
    private static SearchHudElement instance = null;    
    private VCTextField searchField;
    private boolean overlayActive = false;  

    private static boolean isContainer = false;
    
    public SearchHudElement() {
        super(BooleanKey.INV_SEARCH, "Item Search", 100, 10, 20, 0xFFFFFFFF, false, true);
        var mc = Minecraft.getInstance();
        if (mc.font != null) {
            searchField = new VCTextField(mc.font, 10, 10, 150, 15, Component.literal(SEARCH_PLACEHOLDER));
            searchField.setVisible(false);
            searchField.interceptInventory(true);
        }
    }
    
    public static SearchHudElement getInstance() {
        if (instance == null) {
            instance = new SearchHudElement();
        }
        return instance;
    }

    @Override
    protected boolean shouldRender() {
        return isEditingMode() || (ItemSearchOverlay.isEnabled() && isContainer);
    }

    private boolean isSearching() {
        return isOverlayActive() && !getSearchTerm().isEmpty();
    }

    @Override
    public void postRenderCustom(GuiGraphicsExtractor context, Minecraft mc, HudElementState state, int mouseX, int mouseY) {
        if (!shouldRender()) return;
        
        initIfNeeded();
        if (searchField == null) return;
        
        if (isSearching() && McApi.screen() instanceof AbstractContainerScreen<?> hs) {
            ItemSearchOverlay.getInstance().renderOverlay(context, hs, getSearchTerm());
        }
        
        updateDimensions();
        renderContent(context, mouseX, mouseY);
    }
    
    private void initIfNeeded() {
        if (searchField == null) {
            var mc = Minecraft.getInstance();
            if (mc.font != null) {
                searchField = new VCTextField(mc.font, 10, 10, 150, 15, Component.literal(SEARCH_PLACEHOLDER));
                searchField.interceptInventory(true);
            }
        }
    }
    
    private void updateDimensions() {
        var state = getCachedState();
        int hudX = state.x;
        int hudY = state.y;
        int size = state.size;
        float scale = size / 15.0F;
        int scaledWidth = (int)(150 * scale);
        int scaledHeight = (int)(15 * scale);

        if (searchField.getWidth() != scaledWidth || searchField.getHeight() != scaledHeight) {
            String currentText = searchField.getValue();
            boolean wasFocused = searchField.isFocused();
            searchField = new VCTextField(Minecraft.getInstance().font, 
                                         hudX, hudY, scaledWidth, scaledHeight, 
                                         Component.literal(SEARCH_PLACEHOLDER));
            searchField.setValue(currentText);
            searchField.interceptInventory(true);
            if (wasFocused) {
                searchField.setFocused(false);
            }
        } else {
            searchField.setX(hudX);
            searchField.setY(hudY);
        }
        searchField.setVisible(true);
    }
    
    private void renderContent(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        
        var state = getCachedState();
        int hudX = state.x;
        int hudY = state.y;
        int size = state.size;
        float scale = size / 15.0F;
        int scaledWidth = (int)(150 * scale);
        int scaledHeight = (int)(15 * scale);

        if (searchField.getValue().isEmpty()) {
            String placeholderText = isEditingMode() ? EDITING_MODE_TEXT : SEARCH_PLACEHOLDER;
            int placeholderColor = isEditingMode() ? 0x80FFFFFF : 0x80808080;
            context.text(
                Minecraft.getInstance().font, 
                Component.literal(placeholderText), 
                hudX + 4, hudY + (scaledHeight - 8) / 2, 
                placeholderColor, false);
        }
        
        searchField.extractRenderState(context, mouseX, mouseY, 0);

        if (overlayActive) {
            GuiUtil.wireRect(
                context, hudX - 2, hudY - 2, scaledWidth + 4,
                scaledHeight + 4, FishyMode.getThemeColor()
            );
        }
    }
    
    public VCTextField getSearchField() {
        return searchField;
    }
    
    public boolean handleCharTyped(net.minecraft.client.input.CharacterEvent input) {
        if (!shouldRender()) return false;
        
        if (searchField != null && searchField.isVisible() && searchField.isFocused()) {
            return searchField.charTyped(input);
        }
        return false;
    }

    public boolean handleKeyPressed(KeyEvent keyInput) {
        if (!shouldRender()) return false;
        
        if (searchField != null && searchField.isVisible()) {
            return searchField.keyPressed(keyInput);
        }
        return false;
    }    
    
    public boolean handleMouseClick(MouseButtonEvent click, boolean doubled) {
        if (!shouldRender()) return false;

        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        
        if (searchField != null && searchField.isVisible()) {
            var state = getCachedState();
            int hudX = state.x;
            int hudY = state.y;
            float scale = state.size / 15.0F;
            int scaledWidth = (int)(150 * scale);
            int scaledHeight = (int)(15 * scale);

            if (mouseX >= hudX && mouseX <= hudX + scaledWidth &&
                mouseY >= hudY && mouseY <= hudY + scaledHeight) {

                if (button == 0) return searchField.mouseClicked(click, doubled);

                else if (button == 1) {
                    overlayActive = !overlayActive;
                    return true;
                }

                return false;

            } else {
                searchField.setFocused(false);
                return false;
            }
        }

        return false;
    }
    
    public String getSearchTerm() {
        return searchField != null ? searchField.getValue() : "";
    }
    
    public boolean isOverlayActive() {
        return overlayActive;
    }
    
    public void setOverlayActive(boolean active) {
        this.overlayActive = active;
    }
    
    public void clearSearch() {
        if (searchField != null) {
            searchField.setValue("");
        }
    }

    public static void onScreenChange(boolean opened) {
        if (instance != null) {
            instance.toggleField(opened, isContainer);
            isContainer = opened;
        }
    }

    private void toggleField(boolean opened, boolean waContainer) {

        if (opened) {
            if (searchField == null) initIfNeeded();
            if (searchField != null && !waContainer) searchField.setVisible(true);

        } else {
            if (searchField != null) {
                searchField.setVisible(false);
                searchField.setFocused(false);
            }
        }
    }

    @Override
    public boolean isHovered(double mouseX, double mouseY) {
        if (isSearching()) return true;
        return super.isHovered(mouseX, mouseY);
    }

    @Override
    public Rectangle getBounds(Minecraft mc) {
        var state = getCachedState();
        float scale = state.size / 15.0F;
        int scaledWidth = (int)(150 * scale);
        int scaledHeight = (int)(15 * scale);
        return new Rectangle(state.x, state.y, scaledWidth, scaledHeight);
    }

    @Override
    protected IntKey getMaxLinesConfigKey() {
        return IntKey.NONE;
    }

    @Override
    protected String getHudKey() {
        return "search";
    }
    
    @Override
    protected List<Component> getDisplayLines(HudElementState state) {
        return List.of();
    }

    @Override
    public boolean hasCosmetics() {
        return false;
    }    
}
