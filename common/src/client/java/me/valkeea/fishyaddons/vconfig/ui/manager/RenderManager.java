package me.valkeea.fishyaddons.vconfig.ui.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import me.valkeea.fishyaddons.util.text.Color;
import me.valkeea.fishyaddons.vconfig.ui.control.UIControl;
import me.valkeea.fishyaddons.vconfig.ui.layout.Colors;
import me.valkeea.fishyaddons.vconfig.ui.layout.Dimensions;
import me.valkeea.fishyaddons.vconfig.ui.model.BaseContext;
import me.valkeea.fishyaddons.vconfig.ui.model.Bounds;
import me.valkeea.fishyaddons.vconfig.ui.model.Tab;
import me.valkeea.fishyaddons.vconfig.ui.model.Tab.TabItem;
import me.valkeea.fishyaddons.vconfig.ui.model.VCEntry;
import me.valkeea.fishyaddons.vconfig.ui.render.RenderUtils;
import me.valkeea.fishyaddons.vconfig.ui.render.VCRenderContext;
import me.valkeea.fishyaddons.vconfig.ui.render.VCText;
import me.valkeea.fishyaddons.vconfig.ui.screen.VCScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class RenderManager {
    private final VCScreen screen;
    private final Supplier<Integer> themeColorSupplier;
    private final LayoutManager layout;
    
    public RenderManager(VCScreen screen, 
                 Supplier<Integer> themeColorSupplier,
                 LayoutManager layout) {
        this.screen = screen;
        this.themeColorSupplier = themeColorSupplier;
        this.layout = layout;
    }
    
    public void renderConfigEntry(BaseContext renderCtx, VCEntry e, int x, int y, boolean sub) {
        int headerHeight = layout.getHeaderH(sub);
        
        if (e.isHeader()) {
            renderHeaderEntry(renderCtx.context, e, x, y, renderCtx.entryWidth, headerHeight, sub);
            return;
        }
        
        int entryH = Dimensions.getEntryH(sub);
        
        if (sub) {
            Bounds b = Dimensions.getSubEntryBgBounds(x, y, renderCtx.entryWidth, entryH);
            renderCtx.context.fill(b.x, b.y, b.x + b.width, b.y + b.height, 0x30000000);
        }
        
        // Entry metadata (name + description)
        int contentX = Dimensions.getMetadataX(layout.getEntryX(), sub);
        int contentY = Dimensions.getMetadataY(y, sub);
        renderEntryMetadata(renderCtx, e, contentX, contentY, sub);
        
        // Control area
        int controlAreaWidth = Dimensions.CONTROL_W;
        int controlX = x + renderCtx.entryWidth - controlAreaWidth - Dimensions.CONTROL_OUTDENT;
        int controlY = Dimensions.getControlY(y, sub);
        
        if (e.hasControls()) {
            int adjustedX = sub ? controlX + Dimensions.SUB_CONTROL_OUTDENT : controlX;
            renderControls(e, adjustedX, controlY, controlAreaWidth, renderCtx);
        }
        
        if (!sub) { // Separator line for main entries
            int separatorY = y + entryH - Dimensions.getSmallSeparatorH();
            renderCtx.context.fill(x, separatorY, x + renderCtx.entryWidth, separatorY + 1, Colors.SEPARATOR_LINE);
        }
    }
    
    private void renderEntryMetadata(BaseContext renderCtx, VCEntry e, int contentX, int contentY, boolean sub) {

        int nameColor = sub ? Color.mulRGB(themeColorSupplier.get(), 0.8f) : themeColorSupplier.get();
        VCText.flatText(renderCtx.context, screen.getTextRenderer(), e.cleanName, contentX, contentY, nameColor);
    
        if (e.description != null) {
            int descOffset = Dimensions.getDescriptionOffset();
            int descStartY = contentY + descOffset;
            
            if (sub) {
                if (e.description.length > 0) {
                    VCText.flatText(renderCtx.context, screen.getTextRenderer(), e.description[0], contentX, descStartY, Colors.SUB_DESCRIPTION_TEXT);
                }
            } else {
                int lineSpacing = Dimensions.DESC_LINE_SPACING;
                for (int i = 0; i < Math.min(e.description.length, 2); i++) {
                    VCText.flatText(renderCtx.context, screen.getTextRenderer(), e.description[i], contentX, descStartY + i * lineSpacing, Colors.DESCRIPTION_TEXT);
                }
            }
        }
    }
    
    private void renderControls(VCEntry e, int x, int y, int availableW, BaseContext vcContext) {
        if (!e.hasControls()) return;
        
        List<UIControl> controls = e.controls;
        int gap = Dimensions.CONTROL_GAP;
        
        var rCtx = new VCRenderContext(
            vcContext.context,
            screen.getTextRenderer(),
            vcContext.mouseX,
            vcContext.mouseY,
            1.0f,
            themeColorSupplier.get(),
            vcContext.entryWidth
        );
        
        // Render controls left-to-right with gaps
        int currentX = x;
        int rightBoundary = x + availableW;

        for (int i = 0; i < controls.size(); i++) {
            UIControl c = controls.get(i);
            
            c.render(rCtx, currentX, y);
            c.updateHover(vcContext.mouseX, vcContext.mouseY, currentX, y);
            
            // Move X position for next control
            int controlW = c.getPreferredWidth();
            if (i != 1 && currentX + controlW > rightBoundary) {
                int avSpace = rightBoundary - currentX;
                controlW = avSpace;
            }

            currentX += controlW;
            
            if (i < controls.size() - 1) {
                currentX += gap;
            }
        }
    }
    
    public void renderHeaderEntry(DrawContext context, VCEntry e, int x, int y, int entryWidth, int headerHeight, boolean sub) {
        
        if (sub) {
            int textX = x + Dimensions.SUB_HEADER_HORIZONTAL_OFFSET;
            int textY = y + Dimensions.SUB_ENTRY_VERTICAL_OFFSET;
            VCText.flatText(context, screen.getTextRenderer(), e.name, textX, textY, 0xFF888888);
            Bounds b = Dimensions.getSubEntryBgBounds(x, y, entryWidth, headerHeight);
            context.fill(b.x, b.y, b.x + b.width, b.y + b.height, 0x30000000);
            return;
        }
        
        Text title = VCText.header(e.name, null); 
        var tr = screen.getTextRenderer();      
        int textW = tr.getWidth(e.name);
        
        int textCenterX = layout.getCenterX();
        int textX = textCenterX - textW / 2;
        int textY = y + Dimensions.getHeaderTextY(sub);

        int lineHeight = Dimensions.getTallSeparatorH();
        int lineY = textY + tr.fontHeight / 2 - lineHeight / 2;
        
        int gapPadding = Dimensions.HEADER_GAP_PADDING;
        
        int gapStart = textCenterX - textW / 2 - gapPadding;
        int gapEnd = textCenterX + textW / 2 + gapPadding;

        int g1 = 0xFF40E0D0;
        int g2 = Colors.AQUA;
        int g3 = 0xFFE0FFFF;
        
        if (gapStart > x) {
            RenderUtils.horizontalGradient(context, x, lineY, gapStart - x, lineHeight, g1, g2);
        }
        
        if (gapEnd < x + entryWidth) {
            RenderUtils.horizontalGradient(context, gapEnd, lineY, x + entryWidth - gapEnd, lineHeight, g2, g3);
        }
        
        VCText.flatText(context, screen.getTextRenderer(), title, textX, textY, Colors.AQUA);
    }
    
    public void renderTabsWithCoordinates(BaseContext renderCtx, Screen screen, List<Tab> tabs, int activeTabIndex, Integer theme) {
        for (int i = 0; i < tabs.size(); i++) {
            Tab tab = tabs.get(i);
            boolean isActive = i == activeTabIndex;
            boolean isHovered = tab.isPointInTab(renderCtx.mouseX, renderCtx.mouseY);

            renderTabWithCoordinates(renderCtx, screen, tab, isActive, isHovered, theme);

            if (tab.isDropdownVisible()) {
                renderTabDropdownWithCoordinates(renderCtx, screen, tab, theme);
            }
        }
    }

    private void renderTabWithCoordinates(BaseContext renderCtx, Screen screen, Tab tab, boolean isActive,
                                            boolean isHovered, Integer theme) {
        int x = tab.getX();
        int y = tab.getY();
        int width = tab.getWidth();
        int height = tab.getHeight();
        
        int bgColor;
        if (isActive) {
            bgColor = 0x60555555;
        } else if (isHovered) {
            bgColor = 0x40333333;
        } else {
            bgColor = 0x30222222;
        }

        renderCtx.context.fill(x, y, x + width, y + height, bgColor);
        
        if (isActive) {
            renderCtx.context.fill(x, y, x + Dimensions.TAB_BORDER_OFFSET, y + height, theme);
        }
        
        int textColor;

        if (isActive) {
            textColor = theme;
        } else if (isHovered) {
            textColor = 0xFFBBBBBB;
        } else {
            textColor = 0xFF888888;
        }

        int textX = Dimensions.getTabTextX(x);
        int textY = y + (height - screen.getTextRenderer().fontHeight) / 2;
        
        VCText.flatText(renderCtx.context, screen.getTextRenderer(), tab.displayName, textX, textY, textColor);
        
        int arrowX = Dimensions.getTabIndicatorX(x, width);
        int arrowY = y + height / 2;
        int arrowSize = 3;
        renderDropdownArrow(renderCtx.context, arrowX, arrowY, arrowSize, textColor, !tab.isDropdownVisible());
    }

    private void renderTabDropdownWithCoordinates(BaseContext renderCtx, Screen screen, Tab tab, Integer theme) {
        int x = tab.getDropdownX();
        int y = tab.getDropdownY();
        int width = tab.getDropdownWidth();
        int height = tab.getDropdownHeight();
        
        renderCtx.context.fill(x, y, x + width, y + height, 0x90000000);
        RenderUtils.border(renderCtx.context, x, y, width, height, Color.mulRGB(theme, 0.6f));

        List<TabItem> items = tab.getDropdownItems();

        int itemHeight = Dimensions.TAB_ITEM_H;
        int padding = Dimensions.TAB_ITEM_PADDING;

        int listY = y + padding;

        for (int i = 0; i < items.size() && listY < y + height - padding; i++) {
            TabItem item = items.get(i);
            boolean isHovered = renderCtx.mouseX >= x + padding && 
                               renderCtx.mouseX <= x + width - padding &&
                               renderCtx.mouseY >= listY && renderCtx.mouseY <= listY + itemHeight;
            
            int textColor = isHovered ? theme : Colors.DESCRIPTION_TEXT;
            if (isHovered) {
                renderCtx.context.fill(x + padding, listY, x + width - padding, 
                                     listY + itemHeight, 0x30FFFFFF);
            }
            
            VCText.flatText(renderCtx.context, screen.getTextRenderer(), "• " + item.displayName(), 
                                 Dimensions.getTabTextX(x), listY + Dimensions.TAB_BORDER_OFFSET, textColor);
            listY += itemHeight;
        }
    }
    
    private void renderDropdownArrow(DrawContext context, int x, int y, int size, int color, boolean up) {
        if (up) {
            for (int i = 0; i < size; i++) {
                context.fill(x - i, y - i, x + i + 1, y - i + 1, color);
            }
        } else {
            for (int i = 0; i < size; i++) {
                context.fill(x - i, y + i, x + i + 1, y + i + 1, color);
            }
        }
    }

    public void formatAndRenderTooltip(VCRenderContext ctx, String[] tooltipLines, int x, int y, int width, int maxWidth) {
        
        List<Text> lines = new ArrayList<>();
        for (int i = 0; i < tooltipLines.length; i++) {
            String part = tooltipLines[i].trim();
            if (i > 0 && part.charAt(0) != '§') part = "§7• " + part;
            if (part.contains("\n")) {
                String[] split = part.split("\n");
                for (String s : split) {
                    lines.add(Text.literal(s.trim()));
                }
            } else {
                lines.add(Text.literal(part));
            }
        }

        entryInfo(ctx, lines, x, y, width, maxWidth);
    }

    private void entryInfo(VCRenderContext ctx, List<Text> lines, int x, int y, int width, int maxHeight) {
        if (lines == null || lines.isEmpty()) return;

        var context = ctx.context;
        float textScale = Dimensions.REDUCTION_MUL;
        
        int scaledWidth = width;
        int scaledMaxHeight = maxHeight;
        
        int lineHeight = (int)Math.ceil(ctx.textRenderer.fontHeight + 2 / textScale);
        int padding = 4;
        int tooltipHeight = Math.min(lines.size() * lineHeight + padding * 2, scaledMaxHeight);
        int displayLines = Math.min(lines.size(), (scaledMaxHeight - padding * 2) / lineHeight);
        
        context.fill(x, y, x + scaledWidth, y + tooltipHeight, 0xE0000000);
        RenderUtils.border(context, x, y, scaledWidth, tooltipHeight, ctx.themeColor & 0x60FFFFFF);

        var matrices = context.getMatrices();
        
        matrices.pushMatrix();
        matrices.scale(textScale, textScale);

        x = (int)(x / textScale);
        y = (int)(y / textScale);

        for (int i = 0; i < displayLines; i++) {
            context.drawText(ctx.textRenderer, lines.get(i), 
                           x + padding, y + padding + i * lineHeight, 
                           ctx.themeColor, false);               
        }
        
        if (displayLines < lines.size()) {
            context.drawText(ctx.textRenderer, Text.literal("..."), 
                           x + padding, y + tooltipHeight - lineHeight - padding / 2, 
                           0xFF888888, false);
        }
        matrices.popMatrix();
    }
    
    public void renderSearchReset(DrawContext context, Bounds bounds, boolean showClear, boolean isHovered, net.minecraft.client.font.TextRenderer textRenderer, int themeColor) {
        int x = bounds.x;
        int y = bounds.y;
        int w = bounds.width;
        int h = bounds.height;
        
        int bgColor = isHovered ? 0x50FFFFFF : 0x30FFFFFF;
        context.fill(x + 1, y + 1, x + w - 1, y + h - 1, bgColor);
        
        String icon = showClear ? "❌" : "⌕";
        int textColor = isHovered ? themeColor : 0xFFAAAAAA;
        
        int textW = textRenderer.getWidth(icon);
        int textX = x + (w - textW) / 2;
        int textY = y + (h - textRenderer.fontHeight) / 2;
        
        context.drawText(textRenderer, Text.literal(icon), textX, textY, textColor, false);
    }
}
