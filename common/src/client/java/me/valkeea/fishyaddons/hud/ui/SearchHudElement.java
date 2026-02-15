package me.valkeea.fishyaddons.hud.ui;

import java.awt.Rectangle;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.feature.qol.ItemSearchOverlay;
import me.valkeea.fishyaddons.hud.core.HudElement;
import me.valkeea.fishyaddons.hud.core.HudElementState;
import me.valkeea.fishyaddons.ui.GuiUtil;
import me.valkeea.fishyaddons.ui.widget.VCTextField;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

public class SearchHudElement implements HudElement {
    private static final String SEARCH_PLACEHOLDER = "right-click to search...";
    private static final String EDITING_MODE_TEXT = "Search Field";
    private static final String HUD_CONFIG_KEY = "search";
    
    private static SearchHudElement instance = null;    
    private HudElementState cachedState = null;
    private VCTextField searchField;
    private boolean overlayActive = false;
    private boolean editingMode = false;    

    private static boolean isContainer = false;
    
    public SearchHudElement() {
        var client = MinecraftClient.getInstance();
        if (client != null && client.textRenderer != null) {
            searchField = new VCTextField(client.textRenderer, 10, 10, 150, 15, Text.literal(SEARCH_PLACEHOLDER));
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
        if (!shouldRender()) return;
        
        initIfNeeded();
        if (searchField == null) {
            return;
        }

        updateDimensions();
        renderContent(context, mouseX, mouseY);
    }
    
    private boolean shouldRender() {
        return ItemSearchOverlay.isEnabled() && isContainer;
    }
    
    private void initIfNeeded() {
        if (searchField == null) {
            var client = MinecraftClient.getInstance();
            if (client != null && client.textRenderer != null) {
                searchField = new VCTextField(client.textRenderer, 10, 10, 150, 15, Text.literal(SEARCH_PLACEHOLDER));
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
            String currentText = searchField.getText();
            boolean wasFocused = searchField.isFocused();
            searchField = new VCTextField(MinecraftClient.getInstance().textRenderer, 
                                         hudX, hudY, scaledWidth, scaledHeight, 
                                         Text.literal(SEARCH_PLACEHOLDER));
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
            context.drawText(
                MinecraftClient.getInstance().textRenderer, 
                Text.literal(placeholderText), 
                hudX + 4, hudY + (scaledHeight - 8) / 2, 
                placeholderColor, false);
        }
        
        searchField.render(context, mouseX, mouseY, 0);

        if (overlayActive) {
            GuiUtil.wireRect(context, hudX - 2, hudY - 2, scaledWidth + 4, scaledHeight + 4, 0xFF00FFE1);
        }
    }

    @Override
    public void setHudPosition(int x, int y) {
        FishyConfig.setHudX(HUD_CONFIG_KEY, x);
        FishyConfig.setHudY(HUD_CONFIG_KEY, y);
    }

    @Override
    public Rectangle getBounds(MinecraftClient mc) {
        var state = getCachedState();
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
    
    public VCTextField getSearchField() {
        return searchField;
    }
    
    public boolean handleCharTyped(net.minecraft.client.input.CharInput input) {
        if (!shouldRender()) return false;
        
        if (searchField != null && searchField.isVisible() && searchField.isFocused()) {
            return searchField.charTyped(input);
        }
        return false;
    }

    public boolean handleKeyPressed(KeyInput keyInput) {
        if (!shouldRender()) return false;
        
        if (searchField != null && searchField.isVisible()) {
            return searchField.keyPressed(keyInput);
        }
        return false;
    }    
    
    public boolean handleMouseClick(Click click, boolean doubled) {
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
                    searchField.setFocused(overlayActive);
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

    @Override public int getHudX() { return FishyConfig.getHudX(HUD_CONFIG_KEY, 100); }
    @Override public int getHudY() { return FishyConfig.getHudY(HUD_CONFIG_KEY, 10); }
    @Override public int getHudSize() { return FishyConfig.getHudSize(HUD_CONFIG_KEY, 20); }
    @Override public int getHudColor() { return FishyConfig.getHudColor(HUD_CONFIG_KEY, 0xFFFFFFFF); }    
    @Override public boolean getHudOutline() { return FishyConfig.getHudOutline(HUD_CONFIG_KEY, false); }
    @Override public boolean getHudBg() { return FishyConfig.getHudBg(HUD_CONFIG_KEY, true); }
    @Override public String getDisplayName() { return "Item Search"; }    
    @Override public void setEditingMode(boolean editing) { this.editingMode = editing; }
    @Override public void setHudSize(int size) { FishyConfig.setHudSize(HUD_CONFIG_KEY, size); }
    @Override public void setHudColor(int color) { FishyConfig.setHudColor(HUD_CONFIG_KEY, color); }
    @Override public void setHudOutline(boolean outline) { FishyConfig.setHudOutline(HUD_CONFIG_KEY, outline); }
    @Override public void setHudBg(boolean bg) { FishyConfig.setHudBg(HUD_CONFIG_KEY, bg); }        
}
