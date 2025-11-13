package me.valkeea.fishyaddons.hud.ui;

import java.awt.Rectangle;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.hud.core.HudElement;
import me.valkeea.fishyaddons.hud.core.HudElementState;
import me.valkeea.fishyaddons.render.FaLayers;
import me.valkeea.fishyaddons.ui.widget.FaTextField;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class SearchHudElement implements HudElement {
    private boolean editingMode = false;
    private static final String HUD_KEY = me.valkeea.fishyaddons.config.Key.INV_SEARCH;
    private static final String SEARCH_PLACEHOLDER = "right-click to search...";
    private static final String EDITING_MODE_TEXT = "Search Field";
    private static final String HUD_CONFIG_KEY = "search";
    private HudElementState cachedState = null;
    private FaTextField searchField;
    private static SearchHudElement instance = null;
    private boolean overlayActive = false;
    
    public SearchHudElement() {
        var client = MinecraftClient.getInstance();
        if (client != null && client.textRenderer != null) {
            searchField = new FaTextField(client.textRenderer, 10, 10, 150, 15, Text.literal(SEARCH_PLACEHOLDER), true);
            searchField.setVisible(false);
        }
    }
    
    public static SearchHudElement getInstance() {
        if (instance == null) {
            instance = new SearchHudElement();
        }
        return instance;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY) {
        if (!shouldRender()) {
            return;
        }
        
        initIfNeeded();
        if (searchField == null) {
            return;
        }

        updateDimensions();
        renderContent(context, mouseX, mouseY);
    }
    
    private boolean shouldRender() {
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean isInInventory = mc.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen;
        boolean isEnabled = FishyConfig.getState(HUD_KEY, false);
        
        if (!isInInventory && searchField != null) {
            searchField.setFocused(false);
        }
        
        if (!editingMode && (!isEnabled || !isInInventory)) {
            if (searchField != null) {
                searchField.setVisible(false);
                searchField.setFocused(false);
            }
            return false;
        }
        
        return true;
    }
    
    private void initIfNeeded() {
        if (searchField == null) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.textRenderer != null) {
                searchField = new FaTextField(client.textRenderer, 10, 10, 150, 15, Text.literal(SEARCH_PLACEHOLDER), true);
            }
        }
    }
    
    private void updateDimensions() {
        HudElementState state = getCachedState();
        int hudX = state.x;
        int hudY = state.y;
        int size = state.size;
        float scale = size / 15.0F;
        int scaledWidth = (int)(150 * scale);
        int scaledHeight = (int)(15 * scale);

        if (searchField.getWidth() != scaledWidth || searchField.getHeight() != scaledHeight) {
            String currentText = searchField.getText();
            boolean wasFocused = searchField.isFocused();
            searchField = new FaTextField(MinecraftClient.getInstance().textRenderer, 
                                         hudX, hudY, scaledWidth, scaledHeight, 
                                         Text.literal(SEARCH_PLACEHOLDER), true);
            searchField.setText(currentText);
            if (wasFocused) {
                searchField.setFocused(false);
            }
        } else {
            searchField.setX(hudX);
            searchField.setY(hudY);
        }
        searchField.setVisible(true);
    }
    
    private void renderContent(DrawContext context, int mouseX, int mouseY) {
        var state = getCachedState();
        int hudX = state.x;
        int hudY = state.y;
        int size = state.size;
        float scale = size / 15.0F;
        int scaledWidth = (int)(150 * scale);
        int scaledHeight = (int)(15 * scale);

        if (searchField.getText().isEmpty()) {
            String placeholderText = editingMode ? EDITING_MODE_TEXT : SEARCH_PLACEHOLDER;
            int placeholderColor = editingMode ? 0x80FFFFFF : 0x80808080;
            FaLayers.drawTextAtTopLevel(context, 
                MinecraftClient.getInstance().textRenderer, 
                placeholderText, 
                hudX + 4, hudY + (scaledHeight - 8) / 2, 
                placeholderColor, false);
        }
        
        FaLayers.renderAtTopLevel(context, 
            () -> searchField.render(context, mouseX, mouseY, 0));

        if (overlayActive) {
            FaLayers.drawBoxAtTopLevel(context, hudX - 1, hudY - 1, scaledWidth + 2, scaledHeight + 2, 0xFF00FFE1);
        }
    }

    @Override
    public void setHudPosition(int x, int y) {
        FishyConfig.setHudX(HUD_CONFIG_KEY, x);
        FishyConfig.setHudY(HUD_CONFIG_KEY, y);
    }

    @Override
    public Rectangle getBounds(MinecraftClient mc) {
        HudElementState state = getCachedState();
        float scale = state.size / 15.0F;
        int scaledWidth = (int)(150 * scale);
        int scaledHeight = (int)(15 * scale);
        return new Rectangle(state.x, state.y, scaledWidth, scaledHeight);
    }

    @Override
    public HudElementState getCachedState() {
        if (cachedState == null) {
            cachedState = new HudElementState(
                getHudX(), getHudY(), getHudSize(), getHudColor(),
                getHudOutline(), getHudBg()
            );
        }
        return cachedState;
    }

    @Override
    public void invalidateCache() {
        cachedState = null;
    }
    
    public FaTextField getSearchField() {
        return searchField;
    }
    
    public boolean handleKeyPressed(int keyCode, int scanCode, int modifiers) {
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean isInInventory = mc.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen;
        
        if (!FishyConfig.getState(HUD_KEY, false) || !isInInventory) {
            return false;
        }
        
        if (searchField != null && searchField.isVisible()) {
            return searchField.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }
    
    public boolean handleMouseClick(double mouseX, double mouseY, int button) {
        var mc = MinecraftClient.getInstance();
        boolean isInInventory = mc.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen;
        
        if (!FishyConfig.getState(HUD_KEY, false) || !isInInventory) {
            return false;
        }
        
        if (searchField != null && searchField.isVisible()) {
            HudElementState state = getCachedState();
            int hudX = state.x;
            int hudY = state.y;
            float scale = state.size / 15.0F;
            int scaledWidth = (int)(150 * scale);
            int scaledHeight = (int)(15 * scale);
            
            if (mouseX >= hudX && mouseX <= hudX + scaledWidth &&
                mouseY >= hudY && mouseY <= hudY + scaledHeight) {
                
                if (button == 0) {
                    searchField.setFocused(true);
                    double relativeX = mouseX - hudX;
                    double relativeY = mouseY - hudY; 
                    return searchField.mouseClicked(relativeX, relativeY, button);
                    
                } else if (button == 1) {
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
        return searchField != null ? searchField.getText() : "";
    }
    
    public boolean isOverlayActive() {
        return overlayActive;
    }
    
    public void setOverlayActive(boolean active) {
        this.overlayActive = active;
    }
    
    public void clearSearch() {
        if (searchField != null) {
            searchField.setText("");
        }
    }

    public void onScreenChange() {
        if (searchField != null) {
            MinecraftClient mc = MinecraftClient.getInstance();
            boolean isInInventory = mc.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen;
            searchField.setFocused(false);
            
            if (!isInInventory) {
                searchField.setVisible(false);
            }
        }
    }

    @Override public int getHudX() { return FishyConfig.getHudX(HUD_CONFIG_KEY, 100); }
    @Override public int getHudY() { return FishyConfig.getHudY(HUD_CONFIG_KEY, 10); }
    @Override public int getHudSize() { return FishyConfig.getHudSize(HUD_CONFIG_KEY, 20); }
    @Override public int getHudColor() { return FishyConfig.getHudColor(HUD_CONFIG_KEY, 0xFFFFFF); }    
    @Override public boolean getHudOutline() { return FishyConfig.getHudOutline(HUD_CONFIG_KEY, false); }
    @Override public boolean getHudBg() { return FishyConfig.getHudBg(HUD_CONFIG_KEY, true); }
    @Override public String getDisplayName() { return "Item Search"; }    
    @Override public void setEditingMode(boolean editing) { this.editingMode = editing; }
    @Override public void setHudSize(int size) { FishyConfig.setHudSize(HUD_CONFIG_KEY, size); }
    @Override public void setHudColor(int color) { FishyConfig.setHudColor(HUD_CONFIG_KEY, color); }
    @Override public void setHudOutline(boolean outline) { FishyConfig.setHudOutline(HUD_CONFIG_KEY, outline); }
    @Override public void setHudBg(boolean bg) { FishyConfig.setHudBg(HUD_CONFIG_KEY, bg); }        
}
