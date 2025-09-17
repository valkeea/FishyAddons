package me.valkeea.fishyaddons.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import me.valkeea.fishyaddons.config.FilterConfig;
import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.config.TrackerProfiles;
import me.valkeea.fishyaddons.handler.ParticleVisuals;
import me.valkeea.fishyaddons.handler.TransLava;
import me.valkeea.fishyaddons.handler.XpColor;
import me.valkeea.fishyaddons.tool.FishyMode;
import me.valkeea.fishyaddons.ui.list.ChatAlerts;
import me.valkeea.fishyaddons.ui.list.FilterRules;
import me.valkeea.fishyaddons.ui.list.CustomFaColors;
import me.valkeea.fishyaddons.ui.list.ScRules;
import me.valkeea.fishyaddons.ui.list.TabbedListScreen;
import me.valkeea.fishyaddons.ui.widget.VCButton;
import me.valkeea.fishyaddons.ui.widget.VCSlider;
import me.valkeea.fishyaddons.ui.widget.VCTextField;
import me.valkeea.fishyaddons.util.text.GradientRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public class VCScreen extends Screen {
    // Scaled dimensions (calculated in init())
    private int entryHeight;
    private int subEntryHeight;
    private int headerHeight;
    private int entryWidth;
    private float uiScale;
    private int endY;
    
    private List<VCEntry> configEntries;
    private List<VCEntry> filteredEntries;
    private VCTextField searchField;
    private int scrollOffset = 0;
    private int maxVisibleEntries;
    private String lastSearchText = "";
    private Map<String, Boolean> expandedEntries = new HashMap<>();
    
    private boolean isDraggingScrollbar = false;
    private int scrollbarThumbOffset = 0;
    
    // Slider registry for persistent slider instances (key = configKey)
    private Map<String, VCSlider> sliderRegistry = new HashMap<>();
    
    // Renderer for config entries
    private VCGui renderer;
    
    public VCScreen() {
        super(Text.literal("─ α FishyAddons Configuration α ─"));
        // Configuration entries will be initialized in init()
        this.configEntries = new ArrayList<>();
        this.filteredEntries = new ArrayList<>();
        
        // Renderer with suppliers for accessing screen state
        this.renderer = new VCGui(
            this,
            this::getThemeColor,
            () -> this.uiScale,
            () -> this.sliderRegistry
        );
    }
    
    private int getThemeColor() {
        return FishyMode.getThemeColor();
    }
    
    @Override
    public net.minecraft.client.font.TextRenderer getTextRenderer() {
        return this.textRenderer;
    }
    
    // Getters for state preservation
    public int getScrollOffset() {
        return scrollOffset;
    }
    
    public String getLastSearchText() {
        return lastSearchText;
    }
    
    public Map<String, Boolean> getExpandedEntries() {
        return expandedEntries;
    }
    
    @Override
    protected void init() {
        // Initialize scaled dimensions and add entries
        calcScaledDimensions();
        this.configEntries = VCManager.createAllEntries();
        this.filteredEntries = new ArrayList<>(configEntries);

        // Restore preserved state if it exists
        restoreState();

        // Calculate layout and init search field
        int centerX = width / 2;
        int searchY = 40;
        int scaledSearchHeight = VCConstants.getSearchHeight(uiScale);
        searchField = new VCTextField(textRenderer, centerX - entryWidth / 2, searchY, entryWidth, scaledSearchHeight, Text.literal("Search features..."));
        searchField.setUIScale(uiScale);
        searchField.setChangedListener(this::onSearchChanged);
        searchField.setPlaceholder(Text.literal("Search features..."));
        searchField.setText(lastSearchText);
        addDrawableChild(searchField);
        
        // Apply search filter if there was preserved search text
        if (!lastSearchText.isEmpty()) {
            onSearchChanged(lastSearchText);
        }

        // Calculate available screen space
        int scaledButtonHeight = Math.max(12, (int)(20 * uiScale));
        int scaledButtonMargin = Math.max(1, (int)(4 * uiScale));
        int scaledBottomSpacing = Math.max(1, (int)(2 * uiScale));
        int availableHeight = height - searchY - scaledSearchHeight - scaledButtonHeight - scaledButtonMargin - scaledBottomSpacing;
        
        // More aggressive space reclamation for small scales
        if (uiScale < 0.7f) {
            availableHeight += Math.max(0, (int)(8 * uiScale));
            if (uiScale < 0.5f) {
                availableHeight += Math.max(0, (int)(10 * uiScale));
            }
        }

        // Calculate maxVisibleEntries and button positions
        int averageEntryHeight = getAverageEntryHeight();
        maxVisibleEntries = Math.max(1, availableHeight / averageEntryHeight);
        int buttonWidth = Math.max(40, (int)(75 * uiScale));
        int buttonY = height - scaledButtonHeight - scaledBottomSpacing;
        // List ends above navigation buttons
        endY = buttonY - (int)(5 * uiScale);

        addDrawableChild(VCButton.createNavigationButton(
            centerX - buttonWidth / 2, buttonY, buttonWidth, scaledButtonHeight,
            Text.literal("Close").setStyle(Style.EMPTY.withColor(0xFF808080)),
            btn -> {
                VCState.preservePersistentState(scrollOffset, lastSearchText, expandedEntries);
                MinecraftClient.getInstance().setScreen(null);
            }, uiScale
        ));
    }
    
    /**
     * Restore preserved state after screen refresh or reopening
     */
    private void restoreState() {
        scrollOffset = VCState.getLastScrollOffset();
        lastSearchText = VCState.getLastSearchText();
        expandedEntries = VCState.getLastExpandedEntries();
        VCState.clearTemporaryState();
    }

    private void calcScaledDimensions() {
        float customScale = FishyConfig.getFloat(Key.MOD_UI_SCALE, 0.4265625f);
        double scale = Math.clamp(customScale, 0.5, 1.3);

        uiScale = (float) scale; // Stored for button and text scaling

        entryHeight = VCConstants.getEntryHeight(uiScale);
        subEntryHeight = VCConstants.getSubEntryHeight(uiScale);
        headerHeight = VCConstants.getHeaderHeight(uiScale);

        int scaledWidth = (int) (VCConstants.BASE_ENTRY_WIDTH * scale);
        if (uiScale < 0.5f) {
            scaledWidth = (int)(scaledWidth * 0.5f);
        } else if (uiScale < 0.7f) {
            scaledWidth = (int)(scaledWidth * 0.65f);
        }

        // Dynamic minimum to ensure compression actually works
        int dynamicMinWidth = uiScale < VCConstants.COMPACT_SCALE_THRESHOLD ? 300 : 400;
        int maxAllowedWidth = Math.max(dynamicMinWidth, width - 120);
        // Prevent min > max in clamp
        if (dynamicMinWidth > maxAllowedWidth) {
            dynamicMinWidth = maxAllowedWidth;
        }
        entryWidth = Math.clamp(scaledWidth, dynamicMinWidth, maxAllowedWidth);
    }
    
    // Helper for both rendering and click handling
    private int getCompactControlAreaWidth() {
        int baseControlAreaWidth = 140;
        
        if (uiScale < 0.5f) {
            return Math.max(60, (int)(baseControlAreaWidth * uiScale * 0.5f));
        } else if (uiScale < 0.7f) {
            return Math.max(70, (int)(baseControlAreaWidth * uiScale * 0.6f));
        } else {
            return (int)(baseControlAreaWidth * uiScale);
        }
    }
    
    private void onSearchChanged(String text) {
        lastSearchText = text;
        
        if (text.isEmpty()) {
            filteredEntries = new ArrayList<>(configEntries);
            expandMatching("");
        } else {
            String searchText = text.toLowerCase().trim();
            List<VCEntry> matches = configEntries.stream()
                .filter(entry -> matchesSearch(entry, searchText))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            
            if (matches.size() == 1) {
                VCEntry singleMatch = matches.get(0);
                
                // If single match is a header or special navigation case, show all entries and navigate
                if (singleMatch.type == VCEntry.EntryType.HEADER || shouldNavigateTo(singleMatch, searchText)) {
                    filteredEntries = new ArrayList<>(configEntries);
                    expandMatching(searchText);
                    navigateToEntry(singleMatch);
                } else {
                    // Regular single match - show filtered results
                    filteredEntries = matches;
                    scrollOffset = 0;
                    expandMatching(searchText);
                }
            } else {
                // Multiple matches or no matches - use traditional filtering
                filteredEntries = matches;
                scrollOffset = 0;
                expandMatching(searchText);
            }
        }
    }
    
    private boolean shouldNavigateTo(VCEntry entry, String searchText) {
        // Navigate to single match if it's a header or if the search exactly matches the entry name
        return entry.type == VCEntry.EntryType.HEADER || 
               (entry.name != null && entry.name.toLowerCase().equals(searchText));
    }
    
    private void navigateToEntry(VCEntry targetEntry) {
        // Find the position of the target entry in the full list
        List<VCEntry> allVisibleEntries = getAllVisibleEntries();
        int targetIndex = -1;
        
        for (int i = 0; i < allVisibleEntries.size(); i++) {
            if (allVisibleEntries.get(i) == targetEntry) {
                targetIndex = i;
                break;
            }
        }
        
        if (targetIndex != -1) {
            int actualMaxVisible = calculateActualMaxVisibleEntries(allVisibleEntries);
                // Force header to top unless too low
                int maxScroll = allVisibleEntries.size() - actualMaxVisible;
                scrollOffset = Math.clamp(targetIndex, 0, maxScroll);
        }
    }
    
    private void expandMatching(String searchText) {
        // Reset all search-based expansions when starting a new search
        for (VCEntry entry : configEntries) {
            if (entry.hasSubEntries()) {
                if (searchText.isEmpty()) {
                    // When search is cleared, collapse all entries back to default (collapsed)
                    expandedEntries.put(entry.name, false);
                } else {
                    // Check if any entry from an expandable list matches the search text
                    boolean hasMatchingSubEntry = entry.getSubEntries().stream()
                        .anyMatch(subEntry -> 
                            (subEntry.name != null && subEntry.name.toLowerCase().contains(searchText)) ||
                            (subEntry.description != null && subEntry.description.toLowerCase().contains(searchText))
                        );
                    
                    boolean mainEntryMatches = (entry.name != null && entry.name.toLowerCase().contains(searchText)) ||
                        (entry.description != null && entry.description.toLowerCase().contains(searchText));
                    
                    expandedEntries.put(entry.name, hasMatchingSubEntry || mainEntryMatches);
                }
            }
        }
    }
    
    private List<VCEntry> getAllVisibleEntries() {
        List<VCEntry> allVisibleEntries = new ArrayList<>();
        for (VCEntry entry : configEntries) {
            allVisibleEntries.add(entry);
            
            // Add sub-entries if expanded
            if (entry.hasSubEntries() && isExpanded(entry.name)) {
                allVisibleEntries.addAll(entry.getSubEntries());
            }
        }
        return allVisibleEntries;
    }
    
    private boolean matchesSearch(VCEntry entry, String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            return true;
        }

        if (entry.name != null && entry.name.toLowerCase().contains(searchText)) {
            return true;
        }

        if (entry.description != null && entry.description.toLowerCase().contains(searchText)) {
            return true;
        }

        return entry.hasSubEntries() && subEntryMatches(entry, searchText);
    }

    private boolean subEntryMatches(VCEntry entry, String searchText) {
        for (VCEntry subEntry : entry.getSubEntries()) {
            if (subEntry.name != null && subEntry.name.toLowerCase().contains(searchText)) {
                return true;
            }
            if (subEntry.description != null && subEntry.description.toLowerCase().contains(searchText)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        VCRenderUtils.gradient(context, 0, 0, width, height, 0x08000000);

        super.render(context, mouseX, mouseY, delta);

        VCText.drawScaledCenteredText(context, textRenderer, "─ α FishyAddons Configuration α ─", width / 2, 20, 0xFF55FFFF, uiScale);
        renderConfigEntries(context, mouseX, mouseY);
    }
    
    private void renderConfigEntries(DrawContext context, int mouseX, int mouseY) {
        int startY = searchField.getY() + searchField.getHeight() + 10;
        int centerX = width / 2;
        int entryX = centerX - entryWidth / 2;
        
        // Calculate total entries including expanded sub-entries
        List<VCEntry> visibleEntries = getVisibleEntries();
        
        // Calculate actual max visible entries and ensure scroll bounds
        int actualMaxVisible = calculateActualMaxVisibleEntries(visibleEntries);
        int maxScroll = Math.max(0, visibleEntries.size() - actualMaxVisible);
        scrollOffset = Math.clamp(scrollOffset, 0, maxScroll);
        
        int currentY = startY;
        for (int i = scrollOffset; i < visibleEntries.size(); i++) {

            VCEntry entry = visibleEntries.get(i);
            int currentEntryHeight = calculateEntryHeight(entry);
            if (currentY + currentEntryHeight > endY) {
                break;
            }
            
            int renderedHeight = renderConfigEntry(context, entry, entryX, currentY, mouseX, mouseY);
            currentY += renderedHeight;
            
            // Check if we need to render a subentry separator after this entry
            if (shouldDrawSubEnd(visibleEntries, i)) {
                int separatorHeight = drawSubEnd(context, entryX, currentY);
                currentY += separatorHeight;
            }
        }
        
        if (visibleEntries.size() > maxVisibleEntries) {
            renderScrollIndicator(context, centerX + entryWidth/2 + 20, startY);
        }
    }
    
    private boolean shouldDrawSubEnd(List<VCEntry> visibleEntries, int currentIndex) {
        if (currentIndex >= visibleEntries.size() - 1) {
            return false;
        }
        
        VCEntry currentEntry = visibleEntries.get(currentIndex);
        VCEntry nextEntry = visibleEntries.get(currentIndex + 1);
        
        if (isSubEntry(currentEntry) && !isSubEntry(nextEntry) || nextEntry == null) {
            return true;
        }
        
        // Extensibility
        if (isSubEntry(currentEntry) && isSubEntry(nextEntry)) {
            VCEntry currentParent = findParentEntry(currentEntry);
            VCEntry nextParent = findParentEntry(nextEntry);
            return currentParent != nextParent;
        }
        
        return false;
    }
    
    private VCEntry findParentEntry(VCEntry subEntry) {
        for (VCEntry entry : configEntries) {
            if (entry.hasSubEntries()) {
                for (VCEntry sub : entry.getSubEntries()) {
                    if (sub == subEntry) {
                        return entry;
                    }
                }
            }
        }
        return null;
    }
    
    private int drawSubEnd(DrawContext context, int entryX, int currentY) {
        int separatorHeight = VCConstants.getHeaderHeight(uiScale);
        int subBgWidth = (int)(entryWidth * 0.95f);
        context.fill(entryX + (int)(5 * uiScale), currentY, entryX + subBgWidth, currentY + separatorHeight, 0x30000000);
        int lineY = currentY + separatorHeight - (int)(2 * uiScale);
        context.fill(entryX + (int)(5 * uiScale), lineY, entryX + subBgWidth, lineY + 1, 0xFF444444);
        
        return separatorHeight;
    }
    
    private List<VCEntry> getVisibleEntries() {
        List<VCEntry> visibleEntries = new ArrayList<>();
        for (VCEntry entry : filteredEntries) {
            visibleEntries.add(entry);
            
            if (entry.hasSubEntries() && isExpanded(entry.name)) {
                visibleEntries.addAll(entry.getSubEntries());
            }
        }
        return visibleEntries;
    }
    
    public boolean isExpanded(String entryName) {
        return expandedEntries.getOrDefault(entryName, false);
    }
    
    private void toggleExpanded(String entryName) {
        boolean wasExpanded = isExpanded(entryName);
        expandedEntries.put(entryName, !wasExpanded);
        
        // If expanding, adjust scroll to show the sub-entries
        if (!wasExpanded) {
            adjustForExpand(entryName);
        }
    }
    
    private void adjustForExpand(String entryName) {
        // Get current visible entries to find the position of the expanded entry
        List<VCEntry> visibleEntries = getVisibleEntries();
        
        // Find the index of the expanded entry
        int expandedEntryIndex = -1;
        for (int i = 0; i < visibleEntries.size(); i++) {
            VCEntry entry = visibleEntries.get(i);
            if (entry.name.equals(entryName)) {
                expandedEntryIndex = i;
                break;
            }
        }
        
        if (expandedEntryIndex == -1) return;
        
        VCEntry expandedEntry = visibleEntries.get(expandedEntryIndex);
        int subEntryCount = 0;
        if (expandedEntry.hasSubEntries()) {
            subEntryCount = expandedEntry.getSubEntries().size();
        }
        
        int lastSubEntryIndex = expandedEntryIndex + subEntryCount;
        int actualMaxVisible = calculateActualMaxVisibleEntries(visibleEntries);
        int maxVisibleIndex = scrollOffset + actualMaxVisible - 1;
        
        // If the last is not visible, scroll to show it unless it would hide the main entry
        if (lastSubEntryIndex > maxVisibleIndex) {
            int newScrollOffset = lastSubEntryIndex - actualMaxVisible + 1;
            newScrollOffset = Math.max(0, newScrollOffset);
            newScrollOffset = Math.min(newScrollOffset, visibleEntries.size() - actualMaxVisible);

            if (expandedEntryIndex < newScrollOffset) {
                newScrollOffset = expandedEntryIndex;
            }
            scrollOffset = newScrollOffset;
        }
    }
    
    private boolean isSubEntry(VCEntry targetEntry) {
        // Check if this entry exists in any parent's sub-entries list
        for (VCEntry entry : configEntries) {
            if (entry.hasSubEntries()) {
                for (VCEntry subEntry : entry.getSubEntries()) {
                    if (subEntry == targetEntry) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private int renderConfigEntry(DrawContext context, VCEntry entry, int x, int y, int mouseX, int mouseY) {
        // Delegate to renderer
        boolean isSubEntry = isSubEntry(entry);
        
        VCContext renderCtx = new VCContext(context, mouseX, mouseY, x, entryWidth);
        return renderer.renderConfigEntry(renderCtx, entry, x, y, isSubEntry);
    }
    
    private int getHeaderHeight() {
        return headerHeight;
    }
    
    // Slider registry management methods
    private VCSlider getOrCreateSlider(VCEntry entry) {
        if (entry.configKey == null) return null;
        
        return sliderRegistry.computeIfAbsent(entry.configKey, key -> {
            VCSlider slider = new VCSlider(
                0, 0,
                entry.getSliderValue(),
                entry.getMinValue(),
                entry.getMaxValue(),
                entry.getFormatString(),
                entry::setSliderValue
            );
            slider.setUIScale(uiScale);
            return slider;
        });
    }
    
    private int getCurrentColor(VCEntry entry) {
        if (Key.RENDER_COORD_MS.equals(entry.configKey)) {
            return FishyConfig.getInt(Key.RENDER_COORD_COLOR);
        } else if ("Color and Outline".equals(entry.name)) {
            return FishyConfig.getInt(Key.XP_COLOR);
        } else if (Key.FISHY_TRANS_LAVA.equals(entry.configKey)) {
            return FishyConfig.getInt(Key.FISHY_TRANS_LAVA_COLOR, -13700380);
        } else if (Key.CUSTOM_PARTICLE_COLOR_INDEX.equals(entry.configKey)) {
            // For particle colors, show current color based on mode and selection
            if ("custom".equals(FishyConfig.getParticleColorMode())) {
                float[] rgb = FishyConfig.getCustomParticleRGB();
                if (rgb != null && rgb.length == 3) {
                    int r = Math.round(rgb[0] * 255);
                    int g = Math.round(rgb[1] * 255);
                    int b = Math.round(rgb[2] * 255);
                    return (0xFF << 24) | (r << 16) | (g << 8) | b;
                }
            } else {
                int index = FishyConfig.getCustomParticleColorIndex();
                return switch (index) {
                    case 0 -> 0xFF808080;
                    case 1 -> 0xFF66FFFF;
                    case 2 -> 0xFF66FF99;
                    case 3 -> 0xFFFFCCFF;
                    case 4 -> 0xFFE5E5FF;
                    default -> 0xFFFFFFFF;
                };
            }
        }
        return 0xFFFF0000;
    }
    
    private void openColorWheel(VCEntry entry) {
        if (client.currentScreen instanceof VCScreen currentScreen) {
            VCState.preserveState(
                currentScreen.getScrollOffset(),
                currentScreen.getLastSearchText(),
                currentScreen.getExpandedEntries()
            );
        }        
        float[] currentColor = ColorWheel.intToRGB(getCurrentColor(entry));
        
        Consumer<float[]> onColorSelected = rgb -> {
            int colorInt = ColorWheel.rgbToInt(rgb);

            if (Key.RENDER_COORD_MS.equals(entry.configKey)) {
                FishyConfig.setInt(Key.RENDER_COORD_COLOR, colorInt);
            } else if (Key.FISHY_TRANS_LAVA.equals(entry.configKey)) {
                FishyConfig.setInt(Key.FISHY_TRANS_LAVA_COLOR, colorInt);
                TransLava.update();
            } else if ("Color and Outline".equals(entry.name)) {
                FishyConfig.setInt(Key.XP_COLOR, colorInt);
                XpColor.refresh();
            } else if (Key.CUSTOM_PARTICLE_COLOR_INDEX.equals(entry.configKey)) {
                ParticleVisuals.setCustomColor(rgb);
                FishyConfig.setParticleColorMode("custom");
                ParticleVisuals.refreshCache();
            }
        };
        
        MinecraftClient.getInstance().setScreen(new ColorWheel(this, currentColor, onColorSelected));
    }
    
    private void renderScrollIndicator(DrawContext context, int x, int y) {
        // Use actualMaxVisible for correct scrollbar sizing
        List<VCEntry> visibleEntries = getVisibleEntries();
        int actualMaxVisible = calculateActualMaxVisibleEntries(visibleEntries);
        int indicatorHeight =  actualMaxVisible * entryHeight;
        int totalEntries = visibleEntries.size();

        int scrollbarWidth = Math.max(4, (int)(8 * uiScale));

        context.fill(x, y, x + scrollbarWidth, y + indicatorHeight, 0x44000000);

        if (totalEntries > actualMaxVisible) {
            int thumbHeight = Math.max((int)(10 * uiScale), (actualMaxVisible * indicatorHeight) / totalEntries);
            int thumbY = y + (scrollOffset * (indicatorHeight - thumbHeight)) / (totalEntries - actualMaxVisible);

            context.fill(x + 1, thumbY, x + scrollbarWidth - 1, thumbY + thumbHeight, getThemeColor());
            context.fill(x + 1, thumbY + thumbHeight - 1, x + scrollbarWidth - 1, thumbY + thumbHeight, 0xFF000000);
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        
        // Check scrollbar interaction first
        List<VCEntry> visibleEntries = getVisibleEntries();
        if (visibleEntries.size() > maxVisibleEntries) {
            int startY = searchField.getY() + searchField.getHeight() + 10;
            int centerX = width / 2;
            int scrollbarX = centerX + entryWidth/2 + 20;
            int indicatorHeight = maxVisibleEntries * entryHeight;
            int scrollbarWidth = Math.max(4, (int)(8 * uiScale));
            
            if (mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth && 
                mouseY >= startY && mouseY <= startY + indicatorHeight) {
                
                // Calculate thumb position and size
                int totalEntries = visibleEntries.size();
                int thumbHeight = Math.max((int)(10 * uiScale), (maxVisibleEntries * indicatorHeight) / totalEntries);
                int thumbY = startY + (scrollOffset * (indicatorHeight - thumbHeight)) / (totalEntries - maxVisibleEntries);
                
                if (mouseY >= thumbY && mouseY <= thumbY + thumbHeight) {
                    isDraggingScrollbar = true;
                    scrollbarThumbOffset = (int)mouseY - thumbY;
                } else {
                    isDraggingScrollbar = true;
                    scrollbarThumbOffset = thumbHeight / 2;
                    double trackClickY = mouseY - startY - scrollbarThumbOffset;
                    double scrollPercent = trackClickY / (indicatorHeight - thumbHeight);
                    int actualMaxVisible = calculateActualMaxVisibleEntries(getVisibleEntries());
                    int newScrollOffset = (int)(scrollPercent * (totalEntries - actualMaxVisible));
                    scrollOffset = Math.clamp(newScrollOffset, 0, totalEntries - actualMaxVisible);
                }
                return true;
            }
        }

        if (searchField.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        return handleEntryClicks(mouseX, mouseY, visibleEntries);
    }
    
    private boolean handleEntryClicks(double mouseX, double mouseY, List<VCEntry> visibleEntries) {
        int startY = searchField.getY() + searchField.getHeight() + 10;
        int centerX = width / 2;
        int entryX = centerX - entryWidth / 2;
        int currentY = startY;
        
        for (int i = scrollOffset; i < visibleEntries.size(); i++) {
            VCEntry entry = visibleEntries.get(i);
            
            // Skip header entries for interaction
            if (entry.type == VCEntry.EntryType.HEADER) {
                currentY += getHeaderHeight();
                continue;
            }
            
            int currentEntryHeight = calculateEntryHeight(entry);
            
            if (currentY + currentEntryHeight > endY) {
                break;
            }
            
            if (isMouseOverEntry(mouseX, mouseY, entryX, currentY, currentEntryHeight) &&
                entryClick(mouseX, mouseY, entry, entryX, currentY)) {
                return true;
            }
            
            currentY += currentEntryHeight;
            
            if (shouldDrawSubEnd(visibleEntries, i)) {
                currentY += VCConstants.getHeaderHeight(uiScale);
            }
        }
        
        return false;
    }
    
    private int calculateEntryHeight(VCEntry entry) {
        boolean isSubEntry = isSubEntry(entry);
        int baseCurrentHeight = isSubEntry ? subEntryHeight : entryHeight;
        
        // Apply progressive spacing reduction starting below 0.7x scale
        if (uiScale < 0.4f) {
            return Math.max(12, (int)(baseCurrentHeight * 0.6f));
        } else if (uiScale < 0.5f) {
            return Math.max(15, (int)(baseCurrentHeight * 0.7f));
        } else if (uiScale < 0.7f) {
            return Math.max(18, (int)(baseCurrentHeight * 0.8f));
        } else {
            return baseCurrentHeight;
        }
    }
    
    private int getAverageEntryHeight() {
        // Calculate average entry height based on UI scale and entry types
        int mainEntryHeight;
        int subEntryHeightScaled;
        
        if (uiScale < 0.4f) {
            mainEntryHeight = Math.max(12, (int)(entryHeight * 0.6f));
            subEntryHeightScaled = Math.max(12, (int)(subEntryHeight * 0.6f));
        } else if (uiScale < 0.5f) {
            mainEntryHeight = Math.max(15, (int)(entryHeight * 0.7f));
            subEntryHeightScaled = Math.max(15, (int)(subEntryHeight * 0.7f));
        } else if (uiScale < 0.7f) {
            mainEntryHeight = Math.max(18, (int)(entryHeight * 0.8f));
            subEntryHeightScaled = Math.max(18, (int)(subEntryHeight * 0.8f));
        } else {
            mainEntryHeight = entryHeight;
            subEntryHeightScaled = subEntryHeight;
        }
        
        // Return weighted average (assuming roughly 70% main entries, 30% sub entries)
        return (int)(mainEntryHeight * 0.7f + subEntryHeightScaled * 0.3f);
    }

    private int calculateActualMaxVisibleEntries(List<VCEntry> visibleEntries) {
        // Calculate how many entries actually fit on screen before hitting button area
        int startY = searchField.getY() + searchField.getHeight() + 10;
        int currentY = startY;
        int count = 0;
        
        for (int i = 0; i < visibleEntries.size(); i++) {
            VCEntry entry = visibleEntries.get(i);
            int currentEntryHeight = calculateEntryHeight(entry);
            if (currentY + currentEntryHeight > endY) {
                break;
            }
            currentY += currentEntryHeight;
            count++;
            
            if (shouldDrawSubEnd(visibleEntries, i)) {
                int separatorHeight = VCConstants.getHeaderHeight(uiScale);
                if (currentY + separatorHeight > endY) {
                    break; // Separator would not fit
                }
                currentY += separatorHeight;
            }
        }
        
        // Reserve space for 1-2 extra "empty" entries to improve ux
        int reservedSpace = (int)(entryHeight * 1.5f);
        int availableSpace = endY - currentY;
        if (availableSpace > reservedSpace) {
            count -= 1;
        }
        
        return Math.max(1, count);
    }
    
    private boolean isMouseOverEntry(double mouseX, double mouseY, int entryX, int currentY, int currentEntryHeight) {
        return mouseX >= entryX && mouseX <= entryX + entryWidth && 
               mouseY >= currentY && mouseY <= currentY + currentEntryHeight;
    }
    
    private boolean entryClick(double mouseX, double mouseY, VCEntry entry, int entryX, int currentY) {
        return switch (entry.type) {
            case EXPANDABLE -> expandableClick(mouseX, mouseY, entry, entryX, currentY);
            case TOGGLE, ITEM_CONFIG_TOGGLE, BLACKLIST_TOGGLE -> handleToggleClick(mouseX, mouseY, entry, entryX, currentY);
            case SIMPLE_BUTTON -> simpleButtonClick(mouseX, mouseY, entry, entryX, currentY);
            case BUTTON -> buttonClick(entry);
            case KEYBIND -> keybindClick(mouseX, mouseY, entry, entryX, currentY);
            case SLIDER -> sliderClick(mouseX, mouseY, entry, entryX, currentY);
            case TOGGLE_WITH_SLIDER -> toggleSliderClick(mouseX, mouseY, entry, entryX, currentY);
            default -> false;
        };
    }
    
    private boolean expandableClick(double mouseX, double mouseY, VCEntry entry, int entryX, int currentY) {
        int controlAreaWidth = getCompactControlAreaWidth();
        int startX = entryX + entryWidth - controlAreaWidth;
        int buttonY = currentY + (int)(14 * uiScale);
        int buttonHeight = (int)(18 * uiScale);
        int buttonGap = uiScale < 0.7f ? Math.max(1, (int)(2 * uiScale)) : (int)(3 * uiScale);
        int expandButtonWidth = (int)(60 * uiScale);
        int expandButtonX = startX + (int)(40 * uiScale) + buttonGap;
        
        // Optional toggle
        if (entry.hasToggle()) {
            int toggleWidth = (int)(30 * uiScale);
            
            if (mouseX >= startX && mouseX <= startX + toggleWidth &&
                mouseY >= buttonY && mouseY <= buttonY + buttonHeight) {
                entry.toggleSetting();
                return true;
            }
        }
        
        // Expand
        if (mouseX >= expandButtonX && mouseX <= expandButtonX + expandButtonWidth &&
            mouseY >= buttonY && mouseY <= buttonY + buttonHeight) {
            toggleExpanded(entry.name);
            return true;
        }
        
        return false;
    }
    
    private boolean handleToggleClick(double mouseX, double mouseY, VCEntry entry, int entryX, int currentY) {
        ControlClickArea area = calculateControlArea(entry, entryX, currentY);
        int currentButtonX = area.controlX;
        if (isClickOnToggleButton(mouseX, mouseY, currentButtonX, area.controlY)) {
            entry.toggleSetting();
            return true;
        }

        currentButtonX += (int)(30 * uiScale);

        if (entry.hasHudControls() && isClickOnHudButton(mouseX, mouseY, currentButtonX, area.controlY)) {
            VCState.preservePersistentState(scrollOffset, lastSearchText, expandedEntries);
            MinecraftClient.getInstance().setScreen(new HudEditScreen(entry.getHudElementName()));
            return true;
        }

        if (entry.hasColorControl() && isClickOnColorButton(mouseX, mouseY, currentButtonX, area.controlY)) {
            VCState.preservePersistentState(scrollOffset, lastSearchText, expandedEntries);            
            openColorWheel(entry);
            return true;
        }

        if (entry.hasAdd() && isClickOnAddButton(mouseX, mouseY, currentButtonX, area.controlY)) {
            VCState.preservePersistentState(scrollOffset, lastSearchText, expandedEntries);            
            openListGui(entry);
            return true;
        }
        return false;
    }

    private void openListGui(VCEntry entry) {
        MinecraftClient cl = MinecraftClient.getInstance();
        if (entry.configKey == null) return;
        switch (entry.configKey) {
            case Key.CUSTOM_FA_COLORS -> cl.setScreen(new CustomFaColors(this));
            case Key.CHAT_ALERTS_ENABLED -> cl.setScreen(new ChatAlerts(this));
            case Key.ALIASES_ENABLED -> cl.setScreen(new TabbedListScreen(this, TabbedListScreen.Tab.COMMANDS));
            case Key.KEY_SHORTCUTS_ENABLED -> cl.setScreen(new TabbedListScreen(this, TabbedListScreen.Tab.KEYBINDS));
            case Key.CHAT_REPLACEMENTS_ENABLED -> cl.setScreen(new TabbedListScreen(this, TabbedListScreen.Tab.CHAT));
            case Key.CHAT_FILTER_ENABLED -> cl.setScreen(new FilterRules(this));
            case Key.CHAT_FILTER_SC_ENABLED -> tryOpenScGui(cl, this);
            default -> {
                // Unknown entry type
            }
        }
    }

    private void tryOpenScGui(MinecraftClient cl, Screen parent) {
        if (FishyConfig.getState(Key.CHAT_FILTER_SC_ENABLED, false)) {
            cl.setScreen(new ScRules(parent));
        } else {
            VCPopup popup = new VCPopup(
                Text.literal("Sea Creature Overrides are disabled!"),
                "Go Back",
                () -> cl.setScreen(parent),
                "Enable",
                () -> {
                    FishyConfig.enable(Key.CHAT_FILTER_SC_ENABLED, true);
                    FilterConfig.refreshScRules();
                    GradientRenderer.init();
                    cl.setScreen(new ScRules(parent));
                },
                uiScale
            );
            cl.setScreen(new VCOverlay(parent, popup));
        }
    }
    
    private boolean simpleButtonClick(double mouseX, double mouseY, VCEntry entry, int entryX, int currentY) {
        ControlClickArea area = calculateControlArea(entry, entryX, currentY);
        int currentButtonX = area.controlX;
        int toggleWidth = (int)(30 * uiScale);
        if (isClickOnToggleButton(mouseX, mouseY, currentButtonX, area.controlY)) {
            entry.toggleSetting();
            return true;
        }

        currentButtonX += toggleWidth + area.buttonGap;

        String buttonText = entry.buttonText != null ? entry.buttonText : "CONF";
        int textWidth = textRenderer.getWidth(buttonText);
        int simpleButtonWidth = Math.max((int)(30 * uiScale), (int)(textWidth * uiScale) + (int)(10 * uiScale));
        
        if (isClickOnSimpleButton(mouseX, mouseY, currentButtonX, area.controlY, simpleButtonWidth)) {
            entry.toggleSimpleButton();
            return true;
        }
        
        return false;
    }

    private boolean buttonClick(VCEntry entry) {
        if (entry.action != null) {
            entry.action.run();
            return true;
        }
        return false;
    }
    
    private boolean keybindClick(double mouseX, double mouseY, VCEntry entry, int entryX, int currentY) {
        ControlClickArea area = calculateControlArea(entry, entryX, currentY);
        int buttonWidth = (int)(30 * uiScale);
        int buttonHeight = (int)(18 * uiScale);
        
        if (mouseX >= area.controlX && mouseX <= area.controlX + buttonWidth &&
            mouseY >= area.controlY && mouseY <= area.controlY + buttonHeight) {
            
            if (entry.isListening()) {
                entry.setKeybindValue("NONE");
                entry.setListening(false);
            } else {
                entry.setListening(true);
            }
            return true;
        }
        return false;
    }

    private boolean sliderClick(double mouseX, double mouseY, VCEntry entry, int entryX, int currentY) {
        ControlClickArea area = calculateControlArea(entry, entryX, currentY);
        
        if (entry.hasColorControl()) {
            VCSlider slider = getOrCreateSlider(entry);
            if (slider != null) {

                int valueTextX = area.controlX + slider.getWidth() + (int)(8 * uiScale);
                String valueText = renderer.formatSliderValue(entry);
                int valueTextWidth = (int)(textRenderer.getWidth(valueText) * Math.min(uiScale, 1.0f));
                int buttonGap = uiScale < 0.7f ? Math.max(1, (int)(2 * uiScale)) : (int)(3 * uiScale);
                int colorButtonX = valueTextX + valueTextWidth + buttonGap;
                int colorSize = (int)(18 * uiScale);
                
                if (mouseX >= colorButtonX && mouseX <= colorButtonX + colorSize &&
                    mouseY >= area.controlY && mouseY <= area.controlY + colorSize) {
                    openColorWheel(entry);
                    return true;
                }
            }
        }

        // Use the persistent slider from registry and update its position
        VCSlider slider = getOrCreateSlider(entry);
        if (slider == null) return false;
        slider.setPosition(area.controlX, area.controlY + 3);
        
        return slider.mouseClicked(mouseX, mouseY, 0);
    }
    
    private boolean toggleSliderClick(double mouseX, double mouseY, VCEntry entry, int entryX, int currentY) {
        ControlClickArea area = calculateControlArea(entry, entryX, currentY);
        
        int toggleButtonX = area.controlX;
        int toggleButtonY = area.controlY;
        int toggleButtonWidth = (int)(30 * uiScale);
        int toggleButtonHeight = (int)(20 * uiScale);
        
        if (mouseX >= toggleButtonX && mouseX <= toggleButtonX + toggleButtonWidth &&
            mouseY >= toggleButtonY && mouseY <= toggleButtonY + toggleButtonHeight) {
            entry.toggleSetting();
            return true;
        }
        
        int sliderX = toggleButtonX + toggleButtonWidth + (int)(8 * uiScale);
        int sliderY = toggleButtonY + (toggleButtonHeight - (int)(12 * uiScale)) / 2;
        VCSlider slider = getOrCreateSlider(entry);
        if (slider != null) {
            slider.setPosition(sliderX, sliderY);
            return slider.mouseClicked(mouseX, mouseY, 0);
        }
        
        return false;
    }
    
    private ControlClickArea calculateControlArea(VCEntry entry, int entryX, int currentY) {
        int controlAreaWidth = getCompactControlAreaWidth();
        int controlX = entryX + entryWidth - controlAreaWidth;
        
        if (isSubEntry(entry)) {
            controlX += (int)(20 * uiScale);
        }
        
        int controlY = currentY + (int)(14 * uiScale);
        int buttonGap = uiScale < 0.7f ? Math.max(1, (int)(2 * uiScale)) : (int)(3 * uiScale);
        
        return new ControlClickArea(controlX, controlY, buttonGap);
    }
    
    private boolean isClickOnToggleButton(double mouseX, double mouseY, int buttonX, int buttonY) {
        int toggleWidth = (int)(30 * uiScale);
        int buttonHeight = (int)(18 * uiScale);
        
        return mouseX >= buttonX && mouseX <= buttonX + toggleWidth &&
               mouseY >= buttonY && mouseY <= buttonY + buttonHeight;
    }
    
    private boolean isClickOnHudButton(double mouseX, double mouseY, int buttonX, int buttonY) {
        int hudWidth = (int)(40 * uiScale);
        int buttonHeight = (int)(18 * uiScale);
        
        return mouseX >= buttonX && mouseX <= buttonX + hudWidth &&
               mouseY >= buttonY && mouseY <= buttonY + buttonHeight;
    }

    private boolean isClickOnAddButton(double mouseX, double mouseY, int buttonX, int buttonY) {
        int addWidth = (int)(40 * uiScale);
        int buttonHeight = (int)(18 * uiScale);

        return mouseX >= buttonX && mouseX <= buttonX + addWidth &&
               mouseY >= buttonY && mouseY <= buttonY + buttonHeight;
    }

    private boolean isClickOnColorButton(double mouseX, double mouseY, int buttonX, int buttonY) {
        int colorSize = (int)(18 * uiScale);
        
        return mouseX >= buttonX && mouseX <= buttonX + colorSize &&
               mouseY >= buttonY && mouseY <= buttonY + colorSize;
    }
    
    private boolean isClickOnSimpleButton(double mouseX, double mouseY, int buttonX, int buttonY, int buttonWidth) {
        int buttonHeight = (int)(18 * uiScale);
        
        return mouseX >= buttonX && mouseX <= buttonX + buttonWidth &&
               mouseY >= buttonY && mouseY <= buttonY + buttonHeight;
    }
    
    // Helper class to hold control area coordinates
    private static class ControlClickArea {
        final int controlX;
        final int controlY;
        final int buttonGap;
        
        ControlClickArea(int controlX, int controlY, int buttonGap) {
            this.controlX = controlX;
            this.controlY = controlY;
            this.buttonGap = buttonGap;
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDraggingScrollbar = false;

        for (VCSlider slider : sliderRegistry.values()) {
            slider.mouseReleased(button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        for (VCSlider slider : sliderRegistry.values()) {
            if (slider.mouseDragged(mouseX, button)) {
                return true;
            }
        }
        
        if (isDraggingScrollbar) {
            List<VCEntry> visibleEntries = getVisibleEntries();
            if (visibleEntries.size() > maxVisibleEntries) {
                int startY = searchField.getY() + searchField.getHeight() + 10;
                int indicatorHeight = maxVisibleEntries * entryHeight;
                int totalEntries = visibleEntries.size();
                int actualMaxVisible = calculateActualMaxVisibleEntries(visibleEntries);
                int maxScroll = Math.max(0, totalEntries - actualMaxVisible);
                int thumbHeight = Math.max((int)(10 * uiScale), (maxVisibleEntries * indicatorHeight) / totalEntries);
                double thumbTopY = mouseY - startY - scrollbarThumbOffset;
                
                thumbTopY = Math.clamp(thumbTopY, 0.0, (double)indicatorHeight - thumbHeight);
                
                double scrollPercent = maxScroll > 0 ? thumbTopY / (indicatorHeight - thumbHeight) : 0;
                int newScrollOffset = (int)(scrollPercent * maxScroll);
                scrollOffset = Math.clamp(newScrollOffset, 0, maxScroll);
                
                return true;
            }
        }
        
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        List<VCEntry> visibleEntries = getVisibleEntries();
        int actualMaxVisible = calculateActualMaxVisibleEntries(visibleEntries);
        if (visibleEntries.size() > actualMaxVisible) {
            scrollOffset -= (int) verticalAmount * 2; // Scroll 2 entries at a time
            int maxScroll = Math.max(0, visibleEntries.size() - actualMaxVisible);
            scrollOffset = Math.clamp(scrollOffset, 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (handleKeybindInput(keyCode)) {
            return true;
        }
        
        if (keyCode == 256) {
            handleEscapeKey();
            return true;
        }

        if (keyCode == 264) {
            List<VCEntry> currentVisible = getVisibleEntries();
            int actualMaxVisible = calculateActualMaxVisibleEntries(currentVisible);
            scrollOffset = Math.clamp((long)scrollOffset + 1, 0, currentVisible.size() - actualMaxVisible);
            return true;
        } else if (keyCode == 265) {
            scrollOffset = Math.max(scrollOffset - 1, 0);
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    private boolean handleKeybindInput(int keyCode) {
        List<VCEntry> visibleEntries = getVisibleEntries();
        for (VCEntry entry : visibleEntries) {
            if (entry.type == VCEntry.EntryType.KEYBIND && entry.isListening()) {
                String newKey = me.valkeea.fishyaddons.util.KeyUtil.getGlfwKeyName(keyCode);
                if (keyCode == 256 || keyCode == 257) {
                    entry.setListening(false);
                    return true;
                }
                if (newKey != null) {
                    entry.setKeybindValue(newKey);
                } else {
                    // Fallback to the current key
                }
                entry.setListening(false);
                return true;
            }
        }
        return false;
    }
    
    private void handleEscapeKey() {
        List<VCEntry> visibleEntries = getVisibleEntries();
        
        // Check if any expanded entries are currently visible on screen and collapse first one found
        int startY = searchField.getY() + searchField.getHeight() + 10;
        int currentY = startY;
        
        for (int i = scrollOffset; i < visibleEntries.size(); i++) {
            VCEntry entry = visibleEntries.get(i);
            int currentEntryHeight = calculateEntryHeight(entry);

            boolean shouldExit = false;
            if (currentY + currentEntryHeight > endY) {
                shouldExit = true;
            } else if (entry.hasSubEntries() && isExpanded(entry.name)) {
                toggleExpanded(entry.name);
                return;
            } else {
                currentY += currentEntryHeight;
            }

            if (shouldExit) {
                break;
            }
        }
        
        VCState.preservePersistentState(scrollOffset, lastSearchText, expandedEntries);
        MinecraftClient.getInstance().setScreen(null);
    }

    /**
    * Optional redirection button for hud/colorwheel/gui redirect with autofocus,
    * always last in reading order
    */
    public static class ExtraControl {
        private final String elementName;
        private final boolean hasColorControl;
        private final boolean hasAdd;

        public ExtraControl(String elementName, boolean hasColorControl, boolean hasAdd) {
            this.elementName = elementName;
            this.hasColorControl = hasColorControl;
            this.hasAdd = hasAdd;
        }
        
        public String getElementName() {
            return elementName;
        }
        
        public boolean hasColorControl() {
            return hasColorControl;
        }

        public boolean hasAdd() {
            return hasAdd;
        }
    }
}
