package me.valkeea.fishyaddons.ui.widget.dropdown;

import java.util.List;
import java.util.function.Consumer;

import me.valkeea.fishyaddons.ui.widget.VCVisuals;
import me.valkeea.fishyaddons.util.text.Enhancer;
import me.valkeea.fishyaddons.util.text.GradientRenderer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;

public class TextFormatMenu {
    private final List<FormatEntry> formatEntries;
    private List<FormatEntry> filteredEntries;
    private final int x;
    private final int y;
    private final int width;
    private final Consumer<String> onSelect;
    private TextFieldWidget externalField;
    private final float uiScale;
    
    private boolean visible = false;
    private String lastFilterQuery = "";
    private int scrollOffset = 0;
    private int maxEntries = 7;
    private int entryH = 20;

    private boolean isDraggingScrollbar = false;
    private int scrollbarThumbOffset = 0;

    public static class FormatEntry {
        public final String code;
        public final String displayName;
        public final String description;
        public final Integer color;
        public final boolean isGradient;
        
        public FormatEntry(String code, String displayName, String description, Integer color, boolean isGradient) {
            this.code = code;
            this.displayName = displayName;
            this.description = description;
            this.color = color;
            this.isGradient = isGradient;
        }
    }
    
    public TextFormatMenu(int x, int y, int width, Consumer<String> onSelect, 
                                 TextFieldWidget externalField, float uiScale) {
        this.x = x;
        this.y = y + (int)(24 * uiScale);
        this.width = width;
        this.onSelect = onSelect;
        this.externalField = externalField;
        this.uiScale = uiScale;
        this.entryH = externalField.getHeight();
        this.formatEntries = buildFormatEntries();
        this.filteredEntries = this.formatEntries;
    }

    public TextFormatMenu(int x, int y, int width, Consumer<String> onSelect, 
                                    float uiScale) {
        this.x = x;
        this.y = y + (int)(24 * uiScale);
        this.width = width;
        this.onSelect = onSelect;
        this.uiScale = uiScale;
        this.entryH = (int)(20 * uiScale);
        this.formatEntries = buildFormatEntries();
        this.filteredEntries = this.formatEntries;
    }    
    
    private List<FormatEntry> buildFormatEntries() {
        String[] allFormats = Enhancer.getAllCustomFormats();
        java.util.List<FormatEntry> entries = new java.util.ArrayList<>();
        
        var customColors = Enhancer.getCustomColors();
        
        for (String format : allFormats) {
            String displayName;
            String description;
            Integer color = null;
            boolean isGradient = false;
            
            if (format.startsWith("§{") && format.endsWith("}")) {
                String name = format.substring(2, format.length() - 1);
                
                if (isFormattingCode(name)) {
                    displayName = name.toUpperCase();
                    description = "Format: " + name;
                } else {
                    displayName = name;
                    description = "Custom color: " + name;
                    color = customColors.get(name.toLowerCase());
                }
            } else if (format.startsWith("§[") && format.endsWith("]")) {
                String gradientName = format.substring(2, format.length() - 1);
                displayName = gradientName;
                description = "Gradient: " + gradientName;
                isGradient = true;
            } else {
                displayName = format.substring(1);
                description = "Format code";
            }
            
            entries.add(new FormatEntry(format, displayName, description, color, isGradient));
        }
        
        return entries;
    }
    
    private boolean isFormattingCode(String name) {
        return name.equalsIgnoreCase("bold") || name.equalsIgnoreCase("italic") || 
               name.equalsIgnoreCase("underline") || name.equalsIgnoreCase("strikethrough") || 
               name.equalsIgnoreCase("obfuscated") || name.equalsIgnoreCase("reset");
    }
    
    public boolean isMouseOver(int mouseX, int mouseY) {
        if (!visible) return false;
        
        int totalEntries = filteredEntries.size();
        int visibleEntries = Math.min(totalEntries, maxEntries);
        int scaledEntryHeight = (int)(entryH * uiScale);
        int menuHeight = visibleEntries * scaledEntryHeight + (int)(4 * uiScale);
        
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + menuHeight;
    }
    
    public void render(DrawContext context, Screen screen, int mouseX, int mouseY) {
        if (!visible || filteredEntries.isEmpty()) return;
        
        TextRenderer textRenderer = screen.getTextRenderer();
        
        int totalEntries = filteredEntries.size();
        int visibleEntries = Math.min(totalEntries, maxEntries);

        int maxOffset = Math.max(0, totalEntries - visibleEntries);
        scrollOffset = Math.clamp(scrollOffset, 0, maxOffset);
        
        int scaledEntryHeight = (int)(entryH * uiScale);
        int menuHeight = visibleEntries * scaledEntryHeight + (int)(4 * uiScale);
        
        context.fill(x, y, x + width, y + menuHeight, 0xEE222222);
        
        int currentY = y + (int)(2 * uiScale);
        
        for (int i = 0; i < visibleEntries; i++) {
            int entryIndex = i + scrollOffset;
            if (entryIndex >= filteredEntries.size()) break;
            
            FormatEntry entry = filteredEntries.get(entryIndex);
            
            boolean hovered = mouseX >= x && mouseX <= x + width && 
                            mouseY >= currentY && mouseY <= currentY + scaledEntryHeight;
            
            if (hovered) {
                context.fill(x + 1, currentY, x + width - 1, currentY + scaledEntryHeight, 0x44FFFFFF);
            }
            
            renderFormatEntry(context, textRenderer, entry, x + (int)(4 * uiScale), currentY, 
                            scaledEntryHeight, hovered);
            
            currentY += scaledEntryHeight;
        }
        
        if (totalEntries > visibleEntries) {
            int scrollbarX = x + width - (int)(6 * uiScale);
            int scrollbarWidth = (int)(6 * uiScale);
            context.fill(scrollbarX, y, scrollbarX + scrollbarWidth, y + menuHeight, 0x44000000);
            
            int thumbHeight = Math.max((int)(8 * uiScale), (visibleEntries * menuHeight) / totalEntries);
            int thumbY = y + (scrollOffset * (menuHeight - thumbHeight)) / (totalEntries - visibleEntries);
            context.fill(scrollbarX + 1, thumbY, scrollbarX + scrollbarWidth - 1, thumbY + thumbHeight, VCVisuals.getThemeColor());
        }
    }
    
    private void renderFormatEntry(DrawContext context, TextRenderer textRenderer, FormatEntry entry, 
                                  int entryX, int entryY, int height, boolean hovered) {

        int themeColor = VCVisuals.getThemeColor();
        int textColor = hovered ? themeColor & 0xFF000000 | 0x88FFFFFF : themeColor;
        int colorPreviewSize = (int)(16 * uiScale);
        int textX = entryX + colorPreviewSize + (int)(6 * uiScale);
        
        if (entry.isGradient) {
            renderGradientPreview(context, entryX, entryY + (int)(2 * uiScale),colorPreviewSize, colorPreviewSize, entry.displayName);

        } else if (entry.color != null) {
            context.fill(entryX, entryY + (int)(2 * uiScale), entryX + colorPreviewSize, entryY + (int)(2 * uiScale) + colorPreviewSize, 
                        0xFF000000 | entry.color);
            context.drawBorder(entryX, entryY + (int)(2 * uiScale), colorPreviewSize, colorPreviewSize, 0xFF666666);

        } else {
            context.fill(entryX, entryY + (int)(2 * uiScale), entryX + colorPreviewSize, entryY + (int)(2 * uiScale) + colorPreviewSize, 
                        0xFF333333);
            context.drawBorder(entryX, entryY + (int)(2 * uiScale), colorPreviewSize, colorPreviewSize, 0xFF666666);
            
            String indicator = getFormatIndicator(entry.code);
            int indicatorWidth = textRenderer.getWidth(indicator);
            context.drawText(textRenderer, indicator, 
                entryX + (colorPreviewSize - indicatorWidth) / 2, 
                entryY + (int)((2 + (colorPreviewSize - 8) / 2.0) * uiScale), 
                0xFFFFFFFF, false);
        }
        
        float textScale = uiScale * 0.9f;
        context.getMatrices().push();
        context.getMatrices().scale(textScale, textScale, 1.0f);
        
        int scaledTextX = (int)(textX / textScale);
        int scaledTextY = (int)((entryY + height / 2.0 - 4) / textScale);
        
        context.drawText(textRenderer, entry.displayName, scaledTextX, scaledTextY, textColor, false);
        context.getMatrices().pop();
    }
    
    private void renderGradientPreview(DrawContext context, int x, int y, int width, int height, String gradientName) {
        int[] gradientColors = GradientRenderer.getGradientColors(gradientName);
        
        if (gradientColors.length >= 2) {
            int segmentWidth = width / gradientColors.length;
            for (int i = 0; i < gradientColors.length; i++) {
                int segmentX = x + i * segmentWidth;
                int segmentEndX = (i == gradientColors.length - 1) ? x + width : segmentX + segmentWidth;
                context.fill(segmentX, y, segmentEndX, y + height, 0xFF000000 | gradientColors[i]);
            }
        } else {
            context.fill(x, y, x + width, y + height, 0xFF888888);
        }
        
        context.drawBorder(x, y, width, height, 0xFF666666);
    } 
    
    public void updateFilter(String query) {

        String normalizedQuery = query == null ? "" : query.trim();
        boolean filterChanged = !normalizedQuery.equals(lastFilterQuery);
        lastFilterQuery = normalizedQuery;
        
        if (normalizedQuery.isEmpty()) {
            filteredEntries = formatEntries;
        } else {
            String lowerQuery = normalizedQuery.toLowerCase();
            filteredEntries = formatEntries.stream()
                .filter(entry -> entry.displayName.toLowerCase().contains(lowerQuery) ||
                               (entry.description != null && entry.description.toLowerCase().contains(lowerQuery)))
                .toList();
        }
        
        if (filterChanged) {
            scrollOffset = 0;
        }
    }
    
    public boolean mouseClicked(double mouseX, double mouseY) {
        if (!visible) return false;
        
        int totalEntries = filteredEntries.size();
        int visibleEntries = Math.min(totalEntries, maxEntries);
        int scaledEntryHeight = (int)(entryH * uiScale);
        int menuHeight = visibleEntries * scaledEntryHeight + (int)(4 * uiScale);
        
        if (totalEntries > visibleEntries) {
            int startY = y;
            int scrollbarX = x + width - (int)(6 * uiScale);
            int scrollbarWidth = (int)(6 * uiScale);
            
            if (mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth && 
                mouseY >= startY && mouseY <= startY + menuHeight) {

                int thumbHeight = Math.max((int)(8 * uiScale), (visibleEntries * menuHeight) / totalEntries);
                int thumbY = y + (scrollOffset * (menuHeight - thumbHeight)) / (totalEntries - visibleEntries);
                
                if (mouseY >= thumbY && mouseY <= thumbY + thumbHeight) {
                    isDraggingScrollbar = true;
                    scrollbarThumbOffset = (int)mouseY - thumbY;
                } else {
                    isDraggingScrollbar = true;
                    scrollbarThumbOffset = thumbHeight / 2;
                    double trackClickY = mouseY - startY - scrollbarThumbOffset;
                    double scrollPercent = trackClickY / (menuHeight - thumbHeight);
                    int newScrollOffset = (int)(scrollPercent * (totalEntries - visibleEntries));
                    scrollOffset = Math.clamp(newScrollOffset, 0, totalEntries - visibleEntries);
                }
                return true;
            }
        }

        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + menuHeight) {
            int entryIndex = ((int)mouseY - y - (int)(2 * uiScale)) / scaledEntryHeight + scrollOffset;
            if (entryIndex >= 0 && entryIndex < filteredEntries.size()) {
                FormatEntry selectedEntry = filteredEntries.get(entryIndex);
                insertAtCaret(selectedEntry.code);
                setVisible(false);
                return true;
            }
        }        
        
        return false;
    }
    
    private void insertAtCaret(String text) {
        text = text.replace('§', '&');
        onSelect.accept(text);
    }
    
    public boolean keyPressed(int keyCode) {
        if (!visible) return false;
        
        if (keyCode == 256) {
            setVisible(false);
            return true;
        }
        
        int totalEntries = filteredEntries.size();
        int visibleEntries = Math.min(totalEntries, maxEntries);
        int maxOffset = Math.max(0, totalEntries - visibleEntries);
        
        if (keyCode == 264) {
            scrollOffset = Math.min(scrollOffset + 1, maxOffset);
            return true;
        } else if (keyCode == 265) {
            scrollOffset = Math.max(scrollOffset - 1, 0);
            return true;
        }
        
        return false;
    }
    
    public boolean mouseScrolled(double amount) {
        if (!visible) return false;
        
        int totalEntries = filteredEntries.size();
        int visibleEntries = Math.min(totalEntries, maxEntries);

        if (totalEntries > visibleEntries) {
            scrollOffset -= (int)amount;
            int maxOffset = totalEntries - visibleEntries;
            scrollOffset = Math.clamp(scrollOffset, 0, maxOffset);
            return true;
        }
        
        return false;
    }

    public void mouseReleased() {
        if (isDraggingScrollbar) {
            isDraggingScrollbar = false;
        }
    }

    public boolean mouseDragged(double mouseY) {
        if (!visible) return false;
        if (isDraggingScrollbar) {
            int totalEntries = filteredEntries.size();
            int visibleEntries = Math.min(totalEntries, maxEntries);
            int scaledEntryHeight = (int)(entryH * uiScale); 
            int actualMaxVisible = Math.min(totalEntries, maxEntries);           
            if (totalEntries > visibleEntries) {
                int menuHeight = visibleEntries * scaledEntryHeight + (int)(4 * uiScale);
                int startY = y;
                int maxScroll = Math.max(0, totalEntries - actualMaxVisible);
                int thumbHeight = Math.max((int)(10 * uiScale), (visibleEntries * menuHeight) / totalEntries);
                double thumbTopY = mouseY - startY - scrollbarThumbOffset;

                thumbTopY = Math.clamp(thumbTopY, 0.0, (double)menuHeight - thumbHeight);

                double scrollPercent = maxScroll > 0 ? thumbTopY / (menuHeight - thumbHeight) : 0;
                int newScrollOffset = (int)(scrollPercent * maxScroll);
                scrollOffset = Math.clamp(newScrollOffset, 0, maxScroll);
                
                return true;
            }
        }
        return false;
    }        


    public void setVisible(boolean visible) {
        this.visible = visible;
        if (visible && externalField != null) {
            updateFilter(externalField.getText());
        }
    }
    
    public boolean isVisible() {
        return visible;
    }

    public void setMaxEntries(int maxEntries) {
        this.maxEntries = maxEntries;
    }
    
    private String getFormatIndicator(String code) {
        if (code.startsWith("§{") && code.endsWith("}")) {
            String formatName = code.substring(2, code.length() - 1).toLowerCase();
            switch (formatName) {
                case "bold": return "§lB";
                case "italic": return "§oI";
                case "underline": return "§nU";
                case "strikethrough": return "§mS";
                case "obfuscated": return "§k?";
                case "reset": return "§rR";
                default: return "C"; // Custom color
            }
        }
        
        return "F";
    }    
}
