package me.valkeea.fishyaddons.vconfig.ui.screen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.lwjgl.glfw.GLFW;

import me.valkeea.fishyaddons.hud.elements.custom.InfoDisplay;
import me.valkeea.fishyaddons.tool.FishyMode;
import me.valkeea.fishyaddons.util.Keyboard;
import me.valkeea.fishyaddons.util.ModInfo;
import me.valkeea.fishyaddons.vconfig.core.UICategory;
import me.valkeea.fishyaddons.vconfig.ui.control.AbstractUIControl;
import me.valkeea.fishyaddons.vconfig.ui.control.ColorControl;
import me.valkeea.fishyaddons.vconfig.ui.control.ExpandableControl;
import me.valkeea.fishyaddons.vconfig.ui.control.SearchControl;
import me.valkeea.fishyaddons.vconfig.ui.control.UIControl;
import me.valkeea.fishyaddons.vconfig.ui.factory.UIFactory;
import me.valkeea.fishyaddons.vconfig.ui.layout.Colors;
import me.valkeea.fishyaddons.vconfig.ui.layout.Dimensions;
import me.valkeea.fishyaddons.vconfig.ui.manager.LayoutManager;
import me.valkeea.fishyaddons.vconfig.ui.manager.LayoutManager.KnobBounds;
import me.valkeea.fishyaddons.vconfig.ui.manager.LayoutManager.ScrollKnob;
import me.valkeea.fishyaddons.vconfig.ui.manager.RenderManager;
import me.valkeea.fishyaddons.vconfig.ui.manager.ScreenManager;
import me.valkeea.fishyaddons.vconfig.ui.manager.TabManager;
import me.valkeea.fishyaddons.vconfig.ui.model.BaseContext;
import me.valkeea.fishyaddons.vconfig.ui.model.Bounds;
import me.valkeea.fishyaddons.vconfig.ui.model.ClickContext;
import me.valkeea.fishyaddons.vconfig.ui.model.DragContext;
import me.valkeea.fishyaddons.vconfig.ui.model.EntryBounds;
import me.valkeea.fishyaddons.vconfig.ui.model.VCEntry;
import me.valkeea.fishyaddons.vconfig.ui.render.RenderUtils;
import me.valkeea.fishyaddons.vconfig.ui.render.VCRenderContext;
import me.valkeea.fishyaddons.vconfig.ui.render.VCText;
import me.valkeea.fishyaddons.vconfig.ui.widget.VCButton;
import me.valkeea.fishyaddons.vconfig.ui.widget.VCTextField;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class VCScreen extends AdjustedScreen {
    
    private static class ScrollState {
        boolean isDragging = false;
        int knobOffset = 0;
        int offset = 0;
        
        void reset() {
            isDragging = false;
        }
    }
    
    private static class MouseState {
        int lastX = -1;
        int lastY = -1;
        int lastScrollOffset = -1;
        
        boolean hasMoved(int x, int y) {
            return x != lastX || y != lastY;
        }
        
        boolean scrollChanged(int currentScroll) {
            return currentScroll != lastScrollOffset;
        }
        
        void update(int x, int y, int scroll) {
            lastX = x;
            lastY = y;
            lastScrollOffset = scroll;
        }
    }
    
    private static class CacheState {
        List<VCEntry> filtered = null;
        List<VCEntry> targets = null;
        UIControl activeOverlay = null;
        boolean overlaySearched = false;
        List<UIControl> overlayControls = null;
        
        void invalidateAll() {
            filtered = null;
            targets = null;
            overlayControls = null;
        }
        
        void clearPerFrame() {
            activeOverlay = null;
            overlaySearched = false;
        }
    }
    
    private class EntryBoundsIterator {
        private final EntryRenderContext ctx;
        private int finalY = 0;
        
        EntryBoundsIterator(EntryRenderContext context) {
            this.ctx = context;
        }
        
        void forEach(EntryBoundsHandler handler) {
            int currentY = ctx.startY();
            
            for (int i = ctx.scrollOffset(); i < ctx.filteredEntries().size(); i++) {

                VCEntry e = ctx.filteredEntries().get(i);
                boolean isSub = isSubEntry(e);
                int entryH = e.isHeader()
                    ? layout.getHeaderH(isSub)
                    : layout.getEntryH(isSub);

                if (currentY + entryH > ctx.endY()) break;
                
                boolean needsSeparator = shouldDrawSubEnd(ctx.filteredEntries(), i);
                var bounds = new EntryBounds(
                    e, ctx.entryX(), currentY,
                    entryH, i,
                    isSub,
                    needsSeparator
                );

                if (handler.handle(bounds)) return;
                
                currentY += needsSeparator
                    ? layout.getHeaderH(isSub) + entryH
                    : entryH;
            }

            finalY = currentY;
        }

        private boolean shouldDrawSubEnd(List<VCEntry> filtered, int currentIdx) {
            if (currentIdx >= filtered.size() - 1) {
                return false;
            }
            
            VCEntry current = filtered.get(currentIdx);
            VCEntry next = filtered.get(currentIdx + 1);

            if ((isSubEntry(current) && !isSubEntry(next)) || next == null) {
                return true;
            }

            return currentIdx == filtered.size() - 1 && isSubEntry(current);
        }

        public int getFinalY() {
            return finalY;
        }
    }
    
    @FunctionalInterface
    private interface EntryBoundsHandler {
        boolean handle(EntryBounds bounds);
    }
    
    private class ScrollManager {

        boolean handleClick(double mouseX, double mouseY, List<VCEntry> filtered, LayoutManager layout) {
            int maxVisible = layout.calcAllowedEntries(filtered, VCScreen.this::isSubEntry);
            KnobBounds b = layout.getStretchedBounds(maxVisible);
            if (!b.contains(mouseX, mouseY)) return false;
            
            int totalEntries = filtered.size();
            ScrollKnob knob = layout.calcScrollKnob(scrollState.offset, totalEntries, maxVisible);

            if (knob == null) return false;

            if (knob.contains(mouseX, mouseY)) {
                scrollState.isDragging = true;
                scrollState.knobOffset = (int)mouseY - knob.y;
                return true;

            } else {
                scrollState.isDragging = true;
                scrollState.knobOffset = knob.height / 2;

                double trackClickY = mouseY - b.y - scrollState.knobOffset;
                double scrollPercent = (b.height - knob.height) != 0
                    ? trackClickY / (b.height - knob.height)
                    : 0.0;

                int newScrollOffset = (int)(scrollPercent * (totalEntries - maxVisible));
                scrollState.offset = layout.clampScrollOffset(newScrollOffset, totalEntries, maxVisible);

                return true;
            }
        }
        
        boolean handleDrag(Click click, List<VCEntry> filtered, LayoutManager layout) {
            if (!scrollState.isDragging || filtered.size() <= layout.getAllowedEntries()) {
                return false;
            }

            int maxVisible = layout.calcAllowedEntries(filtered, VCScreen.this::isSubEntry);
            KnobBounds b = layout.getStretchedBounds(maxVisible);
            ScrollKnob knob = layout.calcScrollKnob(scrollState.offset, filtered.size(), maxVisible);
            if (knob == null) return true;
            
            double thumbTopY = click.y() - b.y - scrollState.knobOffset;
            double scrollPercent = (b.height - knob.height) != 0
                ? thumbTopY / (b.height - knob.height)
                : 0;

            int maxScroll = Math.max(0, filtered.size() - maxVisible);
            int newScrollOffset = (int)(scrollPercent * maxScroll);
            scrollState.offset = layout.clampScrollOffset(newScrollOffset, filtered.size(), maxVisible);
            
            return true;
        }
        
        void render(DrawContext context, List<VCEntry> filtered, LayoutManager layout, int themeColor) {
            int maxVisible = layout.calcAllowedEntries(filtered, VCScreen.this::isSubEntry);
            int totalEntries = filtered.size();

            KnobBounds b = layout.getKnobBounds(maxVisible);

            context.fill(b.x, b.y, b.x + b.width, 
                        b.y + b.height, Colors.SCROLLBAR_TRACK);

            ScrollKnob knob = layout.calcScrollKnob(scrollState.offset, totalEntries, maxVisible);

            if (knob != null) {
                context.fill(knob.x, knob.y, knob.x + knob.width, knob.y + knob.height, themeColor);
                context.fill(knob.x, knob.y + knob.height - 1, knob.x + knob.width, knob.y + knob.height, Colors.SCROLLBAR_BORDER);
            }
        }
    }

    private class ModIcon {
        Bounds bounds;
        String msg = ModInfo.getInfoMessage();
        boolean show = false;

        public void render(DrawContext ctx, TextRenderer tr, Identifier icon, int x, int y, int s) {
            if (bounds == null) bounds = new Bounds(x, y, s, s);

            ctx.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                icon, x, y, 0, 0, s, s, s, s
            );

            if (!msg.isEmpty()) {
                int dotSize = s / 4;
                ctx.drawText(tr, "●", x + s - dotSize, y, Colors.DISABLED_RED, false);
                if (show) InfoDisplay.getInstance().externalDisplay(ctx, tr, msg);
            }
        }

        public boolean handleClick(double mouseX, double mouseY) {
            if (bounds != null && bounds.contains(mouseX, mouseY)) {
                show = !show;
                return true;
            }
            return false;
        }
    }

    // Layout management
    private LayoutManager layout; 
    
    // State management
    private TabManager tabManager;
    private final ScrollState scrollState = new ScrollState();
    private final MouseState mouseState = new MouseState();
    private final CacheState cache = new CacheState();
    private final ScrollManager scroller = new ScrollManager();

    private String lastSearch = "";
    private Map<String, Boolean> expandedEntries = new HashMap<>();
    private UICategory activeCategory = null;  // null = show all

    // Entry data
    private List<VCEntry> configEntries;
    private List<VCEntry> filteredEntries;
    
    private VCTextField searchField;
    private ModIcon modIcon;
    private Bounds resetBounds;

    private final Consumer<ColorControl> colorWheelOpener = this::openColorWheel;  
    private final Identifier icon = Identifier.of("fishyaddons", "icon.png");
    
    private RenderManager renderer;

    public VCScreen() {
        super(Text.literal("FishyAddons Configuration"));
        this.configEntries = new ArrayList<>();
        this.filteredEntries = new ArrayList<>();
    }

    private int getThemeColor() {
        return FishyMode.getThemeColor();
    }

    public int getScrollOffset() { return scrollState.offset; }
    public String getLastSearchText() { return lastSearch; }
    public LayoutManager getLayout() { return layout; }
    public Map<String, Boolean> getExpandedEntries() { return expandedEntries; }  

    @Override
    protected void init() {
        super.init();
        
        this.layout = new LayoutManager(width, height, uiScale);
        this.renderer = new RenderManager(
            this,
            this::getThemeColor,
            layout
        );
        
        var stateProvider = createStateProvider();
        
        this.configEntries = UIFactory.generateEntries(colorWheelOpener, stateProvider);
        this.filteredEntries = new ArrayList<>(configEntries);

        this.tabManager = new TabManager(
            this,
            this.renderer,
            this::getThemeColor,
            getNavigationCallback(),
            configEntries
        );        

        restoreState();
        invalidateCache();

        int listStartX = layout.getEntryX();

        this.clearChildren();
        searchField = createSearchField(listStartX, Dimensions.SEARCH_Y, layout.getEntryW(), Dimensions.SEARCH_H);
        addDrawableChild(searchField);
        
        int resetSize = Dimensions.SEARCH_H;
        int buttonX = listStartX + layout.getEntryW() - resetSize;

        modIcon = new ModIcon();
        resetBounds = new Bounds(buttonX, Dimensions.SEARCH_Y, resetSize, resetSize);

        var closeStr = "Close";
        int btnH = Dimensions.BUTTON_H;
        int btnW = VCText.getWidthWithPadding(textRenderer, closeStr);
        int btnY = height - btnH;

        addDrawableChild(VCButton.vcScreenWidget(
            layout.getCenterX() - btnW / 2, btnY, btnW, btnH,
            Text.literal(closeStr).setStyle(Style.EMPTY.withColor(Colors.CLOSE_BUTTON_TEXT)),
            btn -> this.close(), uiScale
        ));
    }

    private void restoreState() {
        var savedState = VCState.getPreviousState();
        var savedNames = savedState.filteredNames();
        var lastExpanded = savedState.expanded();

        if (savedState.searchText() != null && !savedState.searchText().isEmpty()) {
            onSearchChanged(savedState.searchText());
            return;
        } else if (savedState.tabIndex() > 0) {
            tabManager.navigateToTab(savedState.tabIndex());
        }

        if (savedNames != null && !savedNames.isEmpty()) {
            filteredEntries = resolveSavedFiltered(savedNames);
            activeCategory = null;
        } else {
            activeCategory = null;
            filteredEntries = new ArrayList<>(configEntries);
        }

        if (lastExpanded != null && !lastExpanded.isEmpty()) {
            restoreExpanded(lastExpanded);
            markExpandedDirty();
        }

        var visible = getAllFiltered();
        int maxVisible = layout.calcAllowedEntries(visible, this::isSubEntry);
        scrollState.offset = layout.clampScrollOffset(savedState.offset(), visible.size(), maxVisible);

        VCState.clear();
        invalidateAllControlCaches();
    }

    private List<VCEntry> resolveSavedFiltered(List<String> savedNames) {
        List<VCEntry> restored = new ArrayList<>();

        for (String name : savedNames) {
            for (VCEntry e : configEntries) {
                if (e.name.equals(name)) {
                    restored.add(e);
                    break;
                }
            }
        }
        return restored.isEmpty() ? new ArrayList<>(configEntries) : restored;
    }

    public void preserveState() {
        List<String> filteredNames = filteredEntries != null 
            ? new ArrayList<>(filteredEntries.stream().map(e -> e.name).toList())
            : new ArrayList<>();

        Map<String, Boolean> expanded = expandedEntries == null
            ? new HashMap<>()
            : new HashMap<>(expandedEntries);

        VCState.preserveState(
            new VCState.StateSnapshot(
                scrollState.offset,
                lastSearch,
                tabManager.getActiveTabIndex(),
                filteredNames,
                expanded
            )
        );
    }
    
    /**
     * Refresh the layout without creating a new screen instance.
     */
    public void refreshLayout() {
        preserveState();
        init();
    }

    private VCTextField createSearchField(int entryX, int searchY, int entryWidth, int searchH) {
        var vctf = new VCTextField(textRenderer, entryX, searchY, entryWidth, searchH, Text.empty());
        vctf.setText(lastSearch);
        vctf.setChangedListener(this::onSearchChanged);
        return vctf;
    }

    // --- Rendering ---
    
    @Override
    protected void renderContent(DrawContext context, int mouseX, int mouseY, float delta) {

        clearPerFrameCache();
        context.applyBlur();
        RenderUtils.gradient(context, 0, 0, width, height, Colors.BACKGROUND_GRADIENT);        
        super.renderContent(context, mouseX, mouseY, delta);

        int y = Dimensions.HEADER_Y;

        var header = this.title.getString();
        var styled = VCText.header(header, null);
        int headerW = textRenderer.getWidth(header);        

        int side = (int)Math.ceil(textRenderer.fontHeight * 1.5);
        int scaledSide = Dimensions.getIconSize(side);

        var hx = layout.getCenterX() + scaledSide / 2;
        int ix = hx - (headerW / 2) - (int)Math.ceil(scaledSide * 1.5);
        int iy = y - (side / 3);

        VCText.drawCenteredTextWithShadow(context, textRenderer, styled, hx, y, 0xFF55FFFF);
        
        renderTabs(context, mouseX, mouseY);
        renderSearchActionButton(context, mouseX, mouseY);
        renderConfigEntries(context, mouseX, mouseY);
        renderControlOverlays(context, mouseX, mouseY);
        modIcon.render(context, textRenderer, icon, ix, iy, scaledSide);
    }

    private void renderTabs(DrawContext context, int mouseX, int mouseY) {
        if (tabManager.isEmpty()) return;

        int edge = layout.getEntryX() - Dimensions.TAB_W - Dimensions.TAB_ENTRY_GAP;
        int startY = Dimensions.SEARCH_Y;
        int tabHeight = Dimensions.TAB_H;
        int tabWidth = Dimensions.TAB_W;
        
        tabManager.updateTabCoordinates(edge, startY, tabWidth, tabHeight, height);
        tabManager.render(context, mouseX, mouseY, edge, layout.getTabAreaW());
    }
    
    private void renderSearchActionButton(DrawContext context, int mouseX, int mouseY) {
        if (resetBounds == null) return;
        boolean hasText = !searchField.getText().isEmpty();
        boolean isHovered = resetBounds.contains(mouseX, mouseY) && hasText;
        renderer.renderSearchReset(context, resetBounds, hasText, isHovered, textRenderer, getThemeColor());
    }
    
    private void renderConfigEntries(DrawContext context, int mouseX, int mouseY) {
        var filtered = getAllFiltered();
        int maxVisible = layout.calcAllowedEntries(filtered, this::isSubEntry);

        scrollState.offset = layout.clampScrollOffset(scrollState.offset, filtered.size(), maxVisible);
        
        var renderCtx = new EntryRenderContext(
            layout.getEntryX(),
            layout.getListStartY(),
            layout.getEndY(),
            scrollState.offset,
            filtered
        );

        var iterator = new EntryBoundsIterator(renderCtx);

        iterator.forEach(b -> {
            renderConfigEntry(context, b.entry(), b.x(), b.y(), mouseX, mouseY);
            if (b.needsSeparator()) drawSubEnd(context, b.x(), b.getEndY());
            return false;
        });
        
        if (filtered.size() > maxVisible) {
            scroller.render(context, filtered, layout, getThemeColor());
        }

        if (tabManager.anyActive()) {

            int color = Colors.TRANSPARENT;
            int promtY = iterator.getFinalY();
            
            if (promtY != 0) {
            VCText.flatText(
                context, textRenderer,
                Text.literal("Press ESC to clear search filters"),
                Dimensions.getMetadataX(layout.getEntryX(), false),
                promtY + Dimensions.HEADER_GAP_PADDING,
                color
            );}
        }
    }

    private void renderConfigEntry(DrawContext context, VCEntry entry, int x, int y, int mouseX, int mouseY) {
        boolean isSubEntry = isSubEntry(entry);
        BaseContext ctx = new BaseContext(context, mouseX, mouseY, x, layout.getEntryW());
        renderer.renderConfigEntry(ctx, entry, x, y, isSubEntry);
    }
    
    private void renderControlOverlays(DrawContext context, int mouseX, int mouseY) {
        // Rebuild overlay cache only when mouse moves, scroll changes, or cache is null
        boolean needsRebuild = mouseState.hasMoved(mouseX, mouseY) || 
                              mouseState.scrollChanged(scrollState.offset) || 
                              cache.overlayControls == null;
        
        if (needsRebuild) {
            cache.overlayControls = buildOverlayControlList(mouseX, mouseY);
            mouseState.update(mouseX, mouseY, scrollState.offset);
        }
        
        if (!cache.overlayControls.isEmpty()) {
            var rCtx = createRenderContext(context, mouseX, mouseY);
            for (UIControl c : cache.overlayControls) {
                if (c.hasActiveOverlay()) {
                    c.renderOverlay(rCtx, layout.getScrollbarX(), layout.getEndY());
                }
            }
        }

        renderFixedTooltip(context, mouseX, mouseY);
    }
    
    private void renderFixedTooltip(DrawContext context, int mouseX, int mouseY) {
        AbstractUIControl hovered = findHoveredWithTooltip(mouseX, mouseY);
        if (hovered == null) return;

        int tooltipY = tabManager.getTabAreaBottomY() + 8;
        int tooltipX = layout.getEntryX() - Dimensions.TAB_W - Dimensions.TAB_ENTRY_GAP;
        int tooltipWidth = Dimensions.TAB_W;
        int tooltipMaxHeight = layout.getEndY() - tooltipY - 8;

        if (tooltipMaxHeight < 30)  return;        
        
        renderer.formatAndRenderTooltip(
            createRenderContext(context, mouseX, mouseY), hovered.getTooltipContent(),
            tooltipX, tooltipY, tooltipWidth, tooltipMaxHeight
        );
    }
    
    /**
     * Find the currently hovered control that has a tooltip.
     * 
     * @return The hovered control with tooltip, or null if none
     */
    private AbstractUIControl findHoveredWithTooltip(int mouseX, int mouseY) {
        if (cache.overlayControls == null) return null;
        
        for (UIControl c : cache.overlayControls) {
            if (c instanceof AbstractUIControl ac && 
                ac.isHovered(mouseX, mouseY)) {
                String[] tooltipContent = ac.getTooltipContent();
                if (tooltipContent != null && tooltipContent.length > 0) {
                    return ac;
                }
            }
        }
        return null;
    }
    
    private void drawSubEnd(DrawContext context, int entryX, int currentY) {
        int lineY = currentY + Dimensions.SUB_ENTRY_VERTICAL_OFFSET;
        Bounds b = Dimensions.getSubEntryBgBounds(entryX, currentY, layout.getEntryW(), Dimensions.SUB_ENTRY_VERTICAL_OFFSET);

        context.fill(b.x, b.y, b.x + b.width, b.y + b.height, Colors.SUB_ENTRY_BG);
        context.fill(entryX + Dimensions.SUB_SEPARATOR_X_OFFSET, lineY, b.x + b.width, lineY + 1, Colors.SEPARATOR_LINE);
    }

    private void invalidateCache() {
        cache.invalidateAll();
    }
    
    private void clearPerFrameCache() {
        cache.clearPerFrame();
    } 

    // --- Navigation ---
    private long recordedSearch = 0;

    private void onSearchChanged(String newSearch) {
        if (!newSearch.equals(lastSearch)) {
            expandedEntries.clear();
            markExpandedDirty();
        }

        if (newSearch.isEmpty()) {
            filteredEntries = new ArrayList<>(configEntries);
            expandMatching("");

        } else {

            long startTime = System.currentTimeMillis();
            long timeStamp = newSearch.length() < lastSearch.length()
                ? TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())
                : startTime;

            if (timeStamp != recordedSearch) {
                String searchText = newSearch.toLowerCase().trim();
                filteredEntries = Navigation.filterEntries(configEntries, searchText);
                scrollState.offset = 0;
                expandMatching(searchText);
                recordedSearch = timeStamp;
            }
        }

        lastSearch = newSearch;
        invalidateCache();
    }
    
    private void navigateToEntry(VCEntry target) {
        var filtered = getAllFiltered();
        int targetIdx = -1;
        
        for (int i = 0; i < filtered.size(); i++) {
            if (filtered.get(i) == target) {
                targetIdx = i;
                break;
            }
        }
        
        if (targetIdx != -1) {
            int maxVisible = layout.calcAllowedEntries(filtered, this::isSubEntry);
            int maxScroll = filtered.size() - maxVisible;
            scrollState.offset = Math.clamp(targetIdx, 0, maxScroll);
        }
    }
    
    private void expandMatching(String searchText) {
        for (VCEntry e : configEntries) {
            if (!e.hasSubEntries()) continue;
            
            if (searchText.isEmpty()) {
                expandedEntries.put(e.name, false);
            } else if (anyContainerMatches(e, searchText)) {
                expandedEntries.put(e.name, true);
            }
        }
        invalidateCache();
    }

    private void restoreExpanded(Map<String, Boolean> saved) {
        saved.forEach((entryName, isExpanded) -> expandedEntries.put(entryName, isExpanded));
    }

    private boolean anyContainerMatches(VCEntry e, String searchText) {
        if (e.name != null && e.name.toLowerCase().contains(searchText)) {
            return true;
        }
        
        if (e.description != null) {
            for (String line : e.description) {
                if (line.toLowerCase().contains(searchText)) {
                    return true;
                }
            }
        }
        
        if (e.hasSubEntries()) {
            for (VCEntry sub : e.getSubEntries()) {
                if (anyContainerMatches(sub, searchText)) {
                    return true;
                }
            }
        }
        
        return false;
    }

    private int findEntryIndex(List<VCEntry> entries, String entryName) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).name.equals(entryName)) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Filter entries to show only those from the specified category.
     * Called when a tab is selected.
     * 
     * @param category The category to filter by, or null to show all
     */
    public void filterByCategory(UICategory category) {
        activeCategory = category;
        expandedEntries.clear();
        markExpandedDirty();

        if (category == null) {
            filteredEntries = new ArrayList<>(configEntries);

        } else {
            scrollState.offset = 0;            
            filteredEntries = new ArrayList<>();
            boolean inCategory = false;

            for (VCEntry e : configEntries) {
                if (e.isHeader() && category.equals(e.category)) {
                    inCategory = true;
                    filteredEntries.add(e);  // Include the header
                } else if (e.isHeader() && e.category != null && !category.equals(e.category)) {
                    inCategory = false;
                } else if (inCategory || category.equals(e.category)) {
                    filteredEntries.add(e);
                }
            }
        }
        
        invalidateCache();
    }
    
    /**
     * Show only specific entries.
     * 
     * @param targetEntries The entries to show and navigate to
     */
    public void showOnlyEntries(List<VCEntry> targets) {
        if (targets == null || targets.isEmpty()) return;

        activeCategory = null;
        filteredEntries = new ArrayList<>(targets);
        expandedEntries.clear();
        markExpandedDirty();

        for (VCEntry e : targets) {
            if (e.hasSubEntries()) {
                expandedEntries.put(e.name, true);
                break;
            }

            VCEntry parent = e.parent;
            if (parent != null) {
                expandedEntries.put(parent.name, true);
                filteredEntries.add(parent);
            }
        }

        scrollState.offset = 0;
        invalidateCache();
    }

    public void removeCategoryFilter() {
        filterByCategory(null);
    }

    public boolean isExpanded(String entryName) {
        return expandedEntries.getOrDefault(entryName, false);
    }
    
    private boolean isEntryExpanded(VCEntry entry) {
        return entry.hasSubEntries() && isExpanded(entry.name);
    }
    
    private void toggleExpanded(String entryName) {
        boolean wasExpanded = isExpanded(entryName);
        expandedEntries.put(entryName, !wasExpanded);

        markExpandedDirty();
        if (!wasExpanded) {
            adjustForExpand(entryName);
        }

        invalidateCache();        
    }

    private void adjustForExpand(String entryName) {
        var filtered = getAllFiltered();
        
        int expandedIdx = findEntryIndex(filtered, entryName);
        if (expandedIdx == -1) return;
        
        VCEntry expanded = filtered.get(expandedIdx);

        int subEntryCount = expanded.hasSubEntries() ? expanded.getSubEntries().size() : 0;
        int lastSubIdx = expandedIdx + subEntryCount;
        int actualMaxVisible = layout.calcAllowedEntries(filtered, this::isSubEntry);
        int maxVisibleIdx = scrollState.offset + actualMaxVisible - 1;
        
        if (lastSubIdx > maxVisibleIdx) {
            int newOffset = lastSubIdx - actualMaxVisible + 1;
            newOffset = Math.max(0, newOffset);
            newOffset = Math.min(newOffset, filtered.size() - actualMaxVisible);

            if (expandedIdx < newOffset) {
                newOffset = expandedIdx;
            }

            scrollState.offset = newOffset;
        }
    }
    
    private boolean isSubEntry(VCEntry targetEntry) {
        return targetEntry.parent != null;
    }    

    // --- Input Handling ---

    private boolean isMouseOverEntry(VCEntry entry, double mouseX, double mouseY) {
        if (!entry.hasControls()) return false;
        
        for (UIControl c : entry.controls) {
            if (c.isHovered(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnyActiveOverlay() {
        return getActiveOverlayControl() != null;
    }
    
    private UIControl getActiveOverlayControl() {
        if (cache.overlaySearched && cache.activeOverlay != null) {
            return cache.activeOverlay;
        }
        
        cache.overlaySearched = true;
        cache.activeOverlay = null;

        iterate(getTargetEntries(), c -> {
            if (c.hasActiveOverlay()) {
                cache.activeOverlay = c;
                return true;
            }
            return false;
        });
        
        return cache.activeOverlay;
    }    
    
    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        var adjustedClick = adjustClick(click);
        
        if (hasAnyActiveOverlay()) {
            return handleOverlayClick(adjustedClick, doubled);
        }

        double mouseX = adjustedClick.x();
        double mouseY = adjustedClick.y();        

        if (resetBounds != null &&
            resetBounds.contains(mouseX, mouseY) &&
            !searchField.getText().isEmpty()) {
            searchField.setText("");
            return true;
        }

        if (super.mouseClicked(click, doubled)) {
            return true;
        }        
        
        if (searchField.isFocused() && !searchField.isMouseOver(mouseX, mouseY)) {
            searchField.setFocused(false);
            setFocused(null);
        }

        if (tabManager.handleClick(mouseX, mouseY)) {
            return true;
        }
        
        var filtered = getAllFiltered();
        int maxVisible = layout.calcAllowedEntries(filtered, this::isSubEntry);
        
        if (filtered.size() > maxVisible &&
            scroller.handleClick(mouseX, mouseY, filtered, layout)) {
            return true;
        }

        if (modIcon.handleClick(mouseX, mouseY)) {
            return true;
        }

        return handleEntryClicks(adjustedClick, doubled, filtered);
    }

    private boolean handleOverlayClick(Click click, boolean doubled) {
        var cCtx = ClickContext.fromClick(click, uiScale, doubled);
        UIControl active = getActiveOverlayControl();
        
        if (active != null) {
            active.handleClick(cCtx, 0, 0);
            return true;
        }
        return false;
    }    

    private boolean handleEntryClicks(Click click, boolean doubled, List<VCEntry> filtered) {
        var renderCtx = new EntryRenderContext(
            layout.getEntryX(),
            layout.getListStartY(),
            layout.getEndY(),
            scrollState.offset,
            filtered
        );
        
        boolean[] handled = {false};
        new EntryBoundsIterator(renderCtx).forEach(bounds -> {
            if (!bounds.entry().isHeader() && bounds.contains(click.y()) &&
                isMouseOverEntry(bounds.entry(), click.x(), click.y()) &&
                entryClick(click, doubled, bounds.entry(), bounds.x(), bounds.y())) {
                handled[0] = true;
                return true;
            }
            return false;
        });
        
        return handled[0];
    }
    
    private boolean entryClick(Click click, boolean doubled, VCEntry entry, int entryX, int currentY) {
        if (!entry.hasControls()) return false;
        
        boolean isSubEntry = isSubEntry(entry);
        int controlAreaWidth = Dimensions.CONTROL_W;
        int controlX = entryX + layout.getEntryW() - controlAreaWidth;
        int controlY = Dimensions.getMetadataY(currentY, isSubEntry);
        
        int adjustedX = isSubEntry ? controlX + Dimensions.SUB_CONTROL_OUTDENT : controlX;
        var ctx = ClickContext.fromClick(click, uiScale, doubled);
        int gap = Dimensions.CONTROL_GAP;

        int currentX = adjustedX;

        for (int i = 0; i < entry.controls.size(); i++) {
            UIControl c = entry.controls.get(i);
            if (c.handleClick(ctx, currentX, controlY)) {
                return true;
            }
            
            currentX += c.getPreferredWidth();
            if (i < entry.controls.size() - 1) {
                currentX += gap;
            }
        }
        
        return false;
    }

    @Override
    public boolean mouseReleased(Click click) {
        Click adjustedClick = adjustClick(click);
        
        if (super.mouseReleased(click)) {
            return true;
        }

        scrollState.reset();

        var cCtx = ClickContext.fromClick(adjustedClick, uiScale, false);

        iterate(getRenderedEntries(), c -> {
            c.handleMouseRelease(cCtx);
            return false;
        });
        
        return false;
    }
    
    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        Click adjustedClick = adjustClick(click);
        double guiScale = client.getWindow().getScaleFactor();
        double adjustedOffsetX = offsetX * guiScale / uiScale;
        double adjustedOffsetY = offsetY * guiScale / uiScale;
        
        if (super.mouseDragged(click, offsetX, offsetY)) {
            return true;
        }

        var dragCtx = DragContext.fromDrag(adjustedClick, adjustedOffsetX, adjustedOffsetY, uiScale);
        var filtered = getAllFiltered();
        
        var renderCtx = new EntryRenderContext(
            layout.getEntryX(),
            layout.getListStartY(),
            layout.getEndY(),
            scrollState.offset,
            filtered
        );
        
        boolean[] handled = {false};
        new EntryBoundsIterator(renderCtx).forEach(bounds -> {
            if (!bounds.entry().isHeader() && bounds.entry().hasControls() && 
                isMouseOverEntry(bounds.entry(), adjustedClick.x(), adjustedClick.y())) {
                for (UIControl c : bounds.entry().controls) {
                    if (c.handleDrag(dragCtx, bounds.x(), bounds.y())) {
                        handled[0] = true;
                        return true;
                    }
                }
            }
            return false;
        });
        
        if (handled[0]) return true;
        if (scroller.handleDrag(adjustedClick, filtered, layout)) return true;
        return super.mouseDragged(click, offsetX, offsetY);
    }


    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (iterate(getTargetEntries(), c -> c.hasActiveOverlay() && c.handleOverlayScroll(verticalAmount))) {
            return true;
        }

        var filtered = getAllFiltered();
        int maxVisible = layout.calcAllowedEntries(filtered, this::isSubEntry);
        if (filtered.size() > maxVisible) {
            scrollState.offset -= verticalAmount;
            scrollState.offset = layout.clampScrollOffset(scrollState.offset, filtered.size(), maxVisible);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    
    @Override
    public boolean keyPressed(KeyInput input) {

        if (input.key() == GLFW.GLFW_KEY_F && Keyboard.hasCtrlModifier(input)) {
            searchField.setFocused(true);
            setFocused(searchField);
            return true;
        }
        
        if (searchField.isFocused() && searchField.keyPressed(input)) {
            return true;
        }

        int keyCode = input.key();
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            handleEscapeKey();
            return true;
        }

        if (super.keyPressed(input)) return true;        

        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            var filtered = getAllFiltered();
            int maxVisible = layout.calcAllowedEntries(filtered, this::isSubEntry);
            scrollState.offset = layout.clampScrollOffset(scrollState.offset + 1, filtered.size(), maxVisible);
            return true;

        } else if (keyCode == GLFW.GLFW_KEY_UP) {
            scrollState.offset = Math.max(scrollState.offset - 1, 0);
            return true;
        }

        return iterate(getRenderedEntries(), c -> c.handleKeyPress(input));
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (searchField.isFocused() && searchField.charTyped(input)) {
            return true;
        }
        if (super.charTyped(input)) return true;
        return iterate(getRenderedEntries(), c -> 
            c instanceof SearchControl fc && fc.handleCharInput(input)
        );
    }
    
    private void handleEscapeKey() {
        if (tabManager.anyActive()) {
            tabManager.clearActive();
            filteredEntries = new ArrayList<>(configEntries);
            invalidateCache();            
            return;
        }
        var expanded = getRenderedEntries().stream().map(e -> e.name)
            .filter(this::isExpanded)
            .findFirst()
            .orElse(null);

        if (expanded != null) {
            toggleExpanded(expanded);
        } else this.close();
        
        clearPerFrameCache();
        cache.overlayControls = null;
    }

    private void openColorWheel(ColorControl entry) {
        ScreenManager.navigateConfigScreen(new ColorWheel(this, entry.getColorBinding().getKey()));
    }

    // --- DI helpers ---

    public Consumer<ColorControl> getColorWheelOpener() {
        return colorWheelOpener;
    }

    private UIFactory.ExpandableStateProvider createStateProvider() {
        return new UIFactory.ExpandableStateProvider() {
            @Override
            public boolean isExpanded(String entryName) {
                return VCScreen.this.isExpanded(entryName);
            }
            
            @Override
            public void toggleExpanded(String entryName) {
                VCScreen.this.toggleExpanded(entryName);
            }
        };
    }

    private TabManager.NavigationCallback getNavigationCallback() {
            return new TabManager.NavigationCallback() {
                @Override
                public void navigateToEntry(VCEntry entry) {
                    VCScreen.this.navigateToEntry(entry);
                }
                
                @Override
                public boolean isSubEntry(VCEntry entry) {
                    return VCScreen.this.isSubEntry(entry);
                }
                
                @Override
                public void clearSearch() {
                    if (!lastSearch.isEmpty()) {
                        searchField.setText("");
                        lastSearch = "";
                        if (activeCategory != null) {
                            VCScreen.this.filterByCategory(activeCategory);
                        } else {
                            filteredEntries = new ArrayList<>(configEntries);
                        }
                    }
                }
                
                @Override
                public void filterByCategory(UICategory category) {
                    VCScreen.this.filterByCategory(category);
                }

                @Override
                public List<VCEntry> findEntriesBy(String navKey) {
                    return VCScreen.this.getEntriesBy(navKey);
                }                
                
                @Override
                public void showOnlyEntries(List<VCEntry> entries) {
                    VCScreen.this.showOnlyEntries(entries);
                }
            };
    }    

    // --- Utils ---

    /**
     * Build list of controls that need to render overlays.
     */
    private List<UIControl> buildOverlayControlList(int mouseX, int mouseY) {
        return allControls(getTargetEntries())
            .filter(c -> c.hasActiveOverlay() || c.isHovered(mouseX, mouseY))
            .toList();
    }
    
    /**
     * Create a render context for control rendering
     */
    private VCRenderContext createRenderContext(DrawContext context, int mouseX, int mouseY) {
        return new VCRenderContext(
            context,
            textRenderer,
            mouseX,
            mouseY,
            0, // delta
            getThemeColor(),
            layout.getEntryW()
        );
    }    

    /** Get all filtered entries on the screen */
    private List<VCEntry> getAllFiltered() {
        if (cache.filtered != null) {
            return cache.filtered;
        }

        List<VCEntry> filtered = new ArrayList<>();
        for (VCEntry e : filteredEntries) {
            filtered.add(e);
            
            if (isEntryExpanded(e)) {
                filtered.addAll(e.getSubEntries());
            }
        }
        cache.filtered = filtered;
        return filtered;
    }

    /** Get currently rendered entries on the screen */
    private List<VCEntry> getRenderedEntries() {
        int maxVisible = layout.calcAllowedEntries(getAllFiltered(), this::isSubEntry);
        return getAllFiltered().stream().limit(scrollState.offset + (long)maxVisible).toList();
    }

    //** Get all accessible entries with controls */
    private List<VCEntry> getTargetEntries() {
        if (cache.targets != null) {
            return cache.targets;
        }

        List<VCEntry> targets = new ArrayList<>();
        for (VCEntry e : filteredEntries) {
            if (e.hasControls()) targets.add(e);
        
            if (e.hasSubEntries()) {
                targets.addAll(e.getSubEntries());
            }
        }
        cache.targets = targets;
        return targets;
    }

    /** Find entries by name content */
    public List<VCEntry> getEntriesBy(String navigationKey) {
        return Navigation.findByNavigationKey(navigationKey, configEntries);
    }
    
    /**
     * Invalidate all control value caches on screen init/refresh.
     * Forces all controls to read fresh values from their bindings on first render.
     */
    private void invalidateAllControlCaches() {
        allControls(configEntries).forEach(c -> {
            if (c instanceof AbstractUIControl ac) {
                ac.invalidateCachedValue();
            }
        });
    }

    private void markExpandedDirty() {
        iterate(configEntries, c -> {
            if (c instanceof ExpandableControl ec) {
                ec.invalidateCachedValue();
            }
            return false;
        });
    }
    
    /** Stream all controls from all generated entries and sub-entries */
    private Stream<UIControl> allControls(List<VCEntry> entries) {
        return entries.stream()
            .flatMap(e -> {
                var stream = e.hasControls() 
                    ? e.controls.stream() 
                    : Stream.<UIControl>empty();
                
                if (e.hasSubEntries()) {
                    var subStream = e.getSubEntries().stream()
                        .filter(VCEntry::hasControls)
                        .flatMap(sub -> sub.controls.stream());
                    stream = Stream.concat(stream, subStream);
                }
                
                return stream;
            });
    }

    private boolean iterate(List<VCEntry> entries, ControlHandler handler) {
        for (VCEntry e : entries) {
            if (handledBy(e, handler)) return true;
            
            if (e.hasSubEntries()) {
                for (VCEntry sub : e.getSubEntries()) {
                    if (handledBy(sub, handler)) return true;
                }
            }
        }
        return false;
    }

    private boolean handledBy(VCEntry e, ControlHandler handler) {
        if (e.hasControls()) {
            for (UIControl c : e.controls) {
                if (handler.handle(c)) return true;
            }
        }
        return false;
    }
    
    @FunctionalInterface
    private interface ControlHandler {
        boolean handle(UIControl control);
    }

    @Override
    public void close() {
        preserveState();
        ScreenManager.invalidateCache();
        super.close();
    }
}
