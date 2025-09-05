package me.valkeea.fishyaddons.gui;

import java.util.Map;
import java.util.function.Supplier;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Handles all rendering logic for configuration entries
 */
public class VCGui {
    private final VCScreen screen;
    private final Supplier<Integer> themeColorSupplier;
    private final Supplier<Float> uiScaleSupplier;
    private final Supplier<Map<String, VCSlider>> sliderRegistrySupplier;
    
    public VCGui(VCScreen screen, 
                              Supplier<Integer> themeColorSupplier,
                              Supplier<Float> uiScaleSupplier,
                              Supplier<Map<String, VCSlider>> sliderRegistrySupplier) {
        this.screen = screen;
        this.themeColorSupplier = themeColorSupplier;
        this.uiScaleSupplier = uiScaleSupplier;
        this.sliderRegistrySupplier = sliderRegistrySupplier;
    }
    
    public int renderConfigEntry(VCContext renderCtx, VCEntry entry, int x, int y, boolean isSubEntry) {

        // Calculate dimensions based on UI scale using UIConstants
        float uiScale = uiScaleSupplier.get();
        int entryHeight = VCConstants.getEntryHeight(uiScale);
        int subEntryHeight = VCConstants.getSubEntryHeight(uiScale);
        int headerHeight = VCConstants.getHeaderHeight(uiScale);
        
        if (entry.type == VCEntry.EntryType.HEADER) {
            return renderHeaderEntry(renderCtx.context, entry, x, y, renderCtx.entryWidth, headerHeight, isSubEntry);
        }
        
        int baseCurrentHeight = isSubEntry ? subEntryHeight : entryHeight;
        int currentEntryHeight = calculateCurrentEntryHeight(baseCurrentHeight, uiScale);
        
        // Separate bg: 95% width of entry, offset start x to the right
        if (isSubEntry) {
            int subBgWidth = (int)(renderCtx.entryWidth * 0.95f);
            renderCtx.context.fill(x + (int)(5 * uiScale), y, x + subBgWidth, y + currentEntryHeight, 0x30000000);
        }
        
        // Entry content
        int contentX = x + (int)(15 * uiScale);
        int indentOffset = isSubEntry ? (int)(30 * uiScale) : 0;
        contentX += indentOffset;
        int contentY = y + (int)(12 * uiScale);

        // Name
        int nameColor = isSubEntry ? darkenColor(themeColorSupplier.get()) : themeColorSupplier.get();
        String displayName = entry.name;
        VCText.drawScaledText(renderCtx.context, screen.getTextRenderer(), displayName, contentX, contentY, nameColor, uiScale);

        // Desc if available
        desc(renderCtx, screen, entry, contentX, contentY, uiScale, isSubEntry);

        // Control area
        int controlAreaWidth = getCompactControlAreaWidth(uiScale);
        int controlX = x + renderCtx.entryWidth - controlAreaWidth;
        int controlY = y + (int)(14 * uiScale);
        
        // Controls
        if (entry.type == VCEntry.EntryType.EXPANDABLE) {
            renderExpandButton(renderCtx, entry, controlX, controlY);
        } else if (isSubEntry) {
            renderEntryControl(renderCtx.context, entry, controlX + (int)(20 * uiScale), controlY, renderCtx.mouseX, renderCtx.mouseY);
        } else {
            renderEntryControl(renderCtx.context, entry, controlX, controlY, renderCtx.mouseX, renderCtx.mouseY);
        }

        // Sub-entries use an end separator
        if (!isSubEntry) {
            int separatorY = y + currentEntryHeight - (int)(2 * uiScale);
            renderCtx.context.fill(x, separatorY, x + renderCtx.entryWidth, separatorY + 1, 0xFF444444);
        }
        
        return currentEntryHeight;
    }

    private void desc(VCContext renderCtx, VCScreen screen, VCEntry entry, int contentX, int contentY, float uiScale, boolean isSubEntry) {
        // Description only for main entries, or for sub-entries if not null and parent is expanded
        if (!isSubEntry && entry.description != null) {
            String[] descLines = entry.description.split("\n");
            int lineSpacing = (int)(12 * uiScale);
            int descStartY = contentY + (int)(16 * uiScale);
                
            for (int i = 0; i < Math.min(descLines.length, 2); i++) { // Max 2 lines to prevent overlap
                VCText.drawScaledText(renderCtx.context, screen.getTextRenderer(), descLines[i], contentX, descStartY + i * lineSpacing, 0xFFCCCCCC, uiScale);
            }
        } else if (isSubEntry && entry.description != null) {
            // For sub-entries, show only one line
            String[] descLines = entry.description.split("\n");
            if (descLines.length > 0) {
                int descY = contentY + (int)(16 * uiScale);
                VCText.drawScaledText(renderCtx.context, screen.getTextRenderer(), descLines[0], contentX, descY, 0xFFC4C4C4, uiScale);
            }
        }
    }


    private int calculateCurrentEntryHeight(int baseCurrentHeight, float uiScale) {
        // Progressively aggressive scaling
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
    
    private int getCompactControlAreaWidth(float uiScale) {
        int baseControlAreaWidth = 140;
        if (uiScale < 0.5f) {
            return Math.max(60, (int)(baseControlAreaWidth * uiScale * 0.5f));
        } else if (uiScale < 0.7f) {
            return Math.max(70, (int)(baseControlAreaWidth * uiScale * 0.6f));
        } else {
            return (int)(baseControlAreaWidth * uiScale);
        }
    }
    
    public int renderHeaderEntry(DrawContext context, VCEntry entry, int x, int y, int entryWidth, int headerHeight, boolean isSubHeader) {
        float uiScale = uiScaleSupplier.get();
        
        if (isSubHeader) {
            // Header without separator lines, adjusted position
            int textX = x + (int)(20 * uiScale);
            int textY = y + (int)(15 * uiScale);

            VCText.drawScaledText(context, screen.getTextRenderer(), entry.name, textX, textY, 0xFF888888, uiScale);
            int subBgWidth = (int)(entryWidth * 0.95f); // 95% width of entry, offset start x to the right
            context.fill(x + (int)(5 * uiScale), y, x + subBgWidth, y + headerHeight, 0x30000000);

            return headerHeight;
        }
        
        // Main header with separator lines
        int unscaledTextWidth = screen.getTextRenderer().getWidth(entry.name);
        int effectiveTextWidth;

        if (uiScale < 1.0f) {
            effectiveTextWidth = (int)(unscaledTextWidth * uiScale);
        } else {
            effectiveTextWidth = unscaledTextWidth;
        }
        
        // For scales 0.7-0.9, effective width calculation is adjusted to prevent offset
        if (uiScale >= 0.7f && uiScale < 1.0f) {
            effectiveTextWidth = Math.round(unscaledTextWidth + 4 * uiScale);
        }
        
        int textCenterX = x + entryWidth / 2;
        int textX = textCenterX - effectiveTextWidth / 2;
        int textY = y + (int)(12 * uiScale);
        
        // Calculate line dimensions, positioning and determine gap width
        int scaledTextHeight = (int)(screen.getTextRenderer().fontHeight * Math.min(uiScale, 1.0f));
        int lineHeight = Math.max(1, (int)(1 * uiScale));
        int lineY = textY + scaledTextHeight / 2 - lineHeight / 2;
        
        int gapPadding;
        if (uiScale >= 0.7f && uiScale < 1.0f) {
            gapPadding = Math.max(8, (int)(12 * uiScale));
        } else {
            gapPadding = Math.max(6, (int)(8 * uiScale));
        }
        
        int gapStart = textCenterX - effectiveTextWidth / 2 - gapPadding;
        int gapEnd = textCenterX + effectiveTextWidth / 2 + gapPadding;
        
        if (gapStart > x) {
            context.fill(x, lineY, gapStart, lineY + lineHeight, 0xFF55FFFF);
        }
        
        if (gapEnd < x + entryWidth) {
            context.fill(gapEnd, lineY, x + entryWidth, lineY + lineHeight, 0xFF55FFFF);
        }
        
        VCText.drawScaledText(context, screen.getTextRenderer(), entry.name, textX, textY, 0xFF55FFFF, uiScale);
        
        return headerHeight;
    }
    
    public void renderExpandButton(VCContext renderCtx, VCEntry entry, int x, int y) {
        boolean isExpanded = screen.isExpanded(entry.name);
        float uiScale = uiScaleSupplier.get();
        
        int buttonHeight = (int)(18 * uiScale);
        int buttonGap = uiScale < 0.7f ? Math.max(1, (int)(2 * uiScale)) : (int)(3 * uiScale);
        int expandButtonWidth = (int)(60 * uiScale);
        int expandButtonX = x + (int)(40 * uiScale) + buttonGap;
        int expandButtonY = y + (buttonHeight - (int)(12 * uiScale)) / 2;

        if (entry.hasToggle()) {
            boolean enabled = entry.getToggleState();
            int toggleWidth = (int)(30 * uiScale);
            boolean toggleHovered = VCButton.isHovered(x, y, toggleWidth, buttonHeight, renderCtx.mouseX, renderCtx.mouseY);
            
            VCButton.render(renderCtx.context, screen.getTextRenderer(),
                VCButton.toggle(x, y, toggleWidth, buttonHeight, enabled)
                    .withHovered(toggleHovered)
                    .withScale(uiScale)
            );            
        }

        boolean expandButtonHovered = VCButton.isHovered(expandButtonX, y, expandButtonWidth, buttonHeight, renderCtx.mouseX, renderCtx.mouseY);
        int triangleColor = expandButtonHovered ? 0xFFA3FFFF : 0xFF55FFFF;
        VCRenderUtils.gradientTriangle(renderCtx.context, expandButtonX, y + buttonHeight / 2, expandButtonWidth, buttonHeight / 2, triangleColor, isExpanded);

        String text = isExpanded ? "Collapse" : "Expand";
        int textWidth = screen.getTextRenderer().getWidth(text);
        int textX = expandButtonX + (expandButtonWidth / 2) - (int)(textWidth * Math.min(uiScale, 1.0f) / 2);
        VCText.drawScaledText(renderCtx.context, screen.getTextRenderer(), text, textX, expandButtonY, themeColorSupplier.get(), uiScale);

        // Tooltip to preview subentries
        if (expandButtonHovered && entry.tooltipText != null) {
            int width = MinecraftClient.getInstance().getWindow().getWidth();
            int tooltipX = Math.min(expandButtonX + expandButtonWidth / 3, width - 100);
            int tooltipY = expandButtonY;
            VCRenderUtils.preview(renderCtx.context, screen.getTextRenderer(), entry.tooltipText, tooltipX, tooltipY, themeColorSupplier.get(), uiScale);
        }
    }
    
    private void renderEntryControl(DrawContext context, VCEntry entry, int x, int y, int mouseX, int mouseY) {
        switch (entry.type) {
            case TOGGLE, ITEM_CONFIG_TOGGLE, BLACKLIST_TOGGLE:
                renderToggleControl(context, entry, x, y, mouseX, mouseY);
                break;
            case TOGGLE_WITH_SLIDER:
                renderToggleWithSliderControl(context, entry, x, y, mouseX, mouseY);
                break;
            case SIMPLE_BUTTON:
                renderSimpleButtonControl(context, entry, x, y, mouseX, mouseY);
                break;
            case BUTTON:
                renderButtonControl(context, x, y, mouseX, mouseY);
                break;
            case KEYBIND:
                renderKeybindControl(context, entry, x, y, mouseX, mouseY);
                break;
            case SLIDER:
                renderSliderControl(context, entry, x, y, mouseX, mouseY);
                break;
            case HEADER:
                // Rendered separately
                break;
            case EXPANDABLE:
                // Same for expandable
                break;
        }
    }

    private void renderToggleControl(DrawContext context, VCEntry entry, int x, int y, int mouseX, int mouseY) {
        float uiScale = uiScaleSupplier.get();
        int currentX = x;
        boolean enabled = entry.getToggleState();
        int toggleWidth = (int)(30 * uiScale);
        int buttonHeight = (int)(18 * uiScale);

        boolean toggleHovered = VCButton.isHovered(currentX, y, toggleWidth, buttonHeight, mouseX, mouseY);
        
        VCButton.render(context, screen.getTextRenderer(),
            VCButton.toggle(currentX, y, toggleWidth, buttonHeight, enabled)
                .withHovered(toggleHovered)
                .withScale(uiScale)
        );
        

        int buttonGap = uiScale < 0.7f ? Math.max(1, (int)(2 * uiScale)) : (int)(3 * uiScale);
        currentX += toggleWidth + buttonGap;
        
        if (entry.hasHudControls()) {
            int hudWidth = uiScale < 0.7f ? Math.max(20, (int)(40 * uiScale)) : (int)(30 * uiScale);
            boolean hudHovered = VCButton.isHovered(currentX, y, hudWidth, buttonHeight, mouseX, mouseY);
            VCButton.render(context, screen.getTextRenderer(),
                VCButton.standard(currentX, y, hudWidth, buttonHeight, "HUD")
                    .withHovered(hudHovered)
                    .withScale(uiScale)
            );                                    
            
            currentX += hudWidth + buttonGap;
        }

        // 3. Color button (if entry has color control)
        if (entry.hasColorControl()) {
            renderColorButton(context, entry, currentX, y);
        }

        if (entry.hasAdd()) {
            int addWidth = uiScale < 0.7f ? Math.max(20, (int)(40 * uiScale)) : (int)(30 * uiScale);
            boolean addHovered = VCButton.isHovered(currentX, y, addWidth, buttonHeight, mouseX, mouseY);
            VCButton.render(context, screen.getTextRenderer(),
                VCButton.standard(currentX, y, addWidth, buttonHeight, "ADD")
                    .withHovered(addHovered)
                    .withScale(uiScale)
            );                                    
        }
    }

    private void renderColorButton(DrawContext context, VCEntry entry, int x, int y) {
        float uiScale = uiScaleSupplier.get();
        int colorSize = (int)(18 * uiScale);
        int currentColor = getCurrentColor(entry);
        context.fill(x, y, x + colorSize, y + colorSize, currentColor);
        context.drawBorder(x, y, colorSize, colorSize, 0x80FFFFFF);
    }
    
    private int getCurrentColor(VCEntry entry) {
        // Determine which color setting this entry controls
        if (Key.RENDER_COORDS.equals(entry.configKey)) {
            return FishyConfig.getInt(Key.RENDER_COORD_COLOR);
        } else if (Key.XP_OUTLINE.equals(entry.configKey)) {
            return FishyConfig.getInt(Key.XP_COLOR);
        } else if (Key.CUSTOM_PARTICLE_COLOR_INDEX.equals(entry.configKey)) {
            if ("custom".equals(FishyConfig.getParticleColorMode())) {
                float[] rgb = FishyConfig.getCustomParticleRGB();
                if (rgb != null && rgb.length == 3) {
                    int r = (int)(rgb[0] * 255);
                    int g = (int)(rgb[1] * 255);
                    int b = (int)(rgb[2] * 255);
                    return (0xFF << 24) | (r << 16) | (g << 8) | b;
                }
            } else {
                int index = FishyConfig.getCustomParticleColorIndex();
                return switch (index) {
                    case 1 -> 0xFF66FFFF;
                    case 2 -> 0xFF66FF99;
                    case 3 -> 0xFFFFCCFF;
                    case 4 -> 0xFFE5E5FF;
                    default -> 0xFF808080;
                };
            }
        }
        return 0xFFFF0000;
    }

    public void renderSimpleButtonControl(DrawContext context, VCEntry entry, int x, int y, int mouseX, int mouseY) {
        int currentX = x;
        int buttonHeight = (int)(18 * uiScaleSupplier.get());
        float uiScale = uiScaleSupplier.get();
        int buttonGap = uiScale < 0.7f ? Math.max(1, (int)(2 * uiScale)) : (int)(3 * uiScale);
    
        int toggleWidth = (int)(30 * uiScale);
        boolean toggleHovered = VCButton.isHovered(currentX, y, toggleWidth, buttonHeight, mouseX, mouseY);
        boolean enabled = entry.getToggleState();
        
        VCButton.render(context, screen.getTextRenderer(),
            VCButton.toggle(currentX, y, toggleWidth, buttonHeight, enabled)
                .withHovered(toggleHovered)
                .withScale(uiScale)
        );                                                
        
        currentX += toggleWidth + buttonGap;
        
        // Simple toggle
        String buttonText = entry.buttonText != null ? entry.buttonText : "CONF";
        int textWidth = screen.getTextRenderer().getWidth(buttonText);
        int minWidth = (int)(30 * uiScale);
        int textBasedWidth = (int)(textWidth * Math.min(uiScale, 1.0f)) + (int)(10 * uiScale);
        
        int simpleButtonWidth;
        if (uiScale >= 0.7f && uiScale <= 0.8f) {
            simpleButtonWidth = Math.max(minWidth, textBasedWidth + (int)(4 * uiScale));
        } else {
            simpleButtonWidth = Math.max(minWidth, textBasedWidth);
        }
        
        boolean simpleButtonHovered = VCButton.isHovered(currentX, y, simpleButtonWidth, buttonHeight, mouseX, mouseY);
        boolean simpleButtonEnabled = entry.getSimpleButtonState();
        
        VCButton.render(context, screen.getTextRenderer(),
            VCButton.toggleWithText(currentX, y, simpleButtonWidth, buttonHeight, buttonText, simpleButtonEnabled)
                .withHovered(simpleButtonHovered)
                .withScale(uiScale)
        );
    }

    public void renderButtonControl(DrawContext context, int x, int y, int mouseX, int mouseY) {
        float uiScale = uiScaleSupplier.get();
        int buttonWidth = uiScale < 0.7f ? Math.max(40, (int)(90 * uiScale)) : (int)(90 * uiScale);
        int buttonHeight = (int)(18 * uiScale);
        
        boolean buttonHovered = VCButton.isHovered(x, y, buttonWidth, buttonHeight, mouseX, mouseY);
        
        VCButton.render(context, screen.getTextRenderer(),
            VCButton.standard(x, y, buttonWidth, buttonHeight, "Configure")
                .withHovered(buttonHovered)
                .withScale(uiScale)
        );                           
    }

    public void renderKeybindControl(DrawContext context, VCEntry entry, int x, int y, int mouseX, int mouseY) {
        float uiScale = uiScaleSupplier.get();
        String currentKey = entry.getKeybindValue();
        String displayText;
        boolean listening = entry.isListening();
        boolean hasKey = !("NONE".equals(currentKey) || currentKey == null || currentKey.isEmpty());

        if (listening || !hasKey) {
            displayText = "> <";
        } else {
            displayText = me.valkeea.fishyaddons.util.KeyUtil.getDisplayNameFor(currentKey);
        }
        
        int buttonWidth = (int)(30 * uiScale);
        int buttonHeight = (int)(18 * uiScale);

        boolean keybindHovered = VCButton.isHovered(x, y, buttonWidth, buttonHeight, mouseX, mouseY);
        
        VCButton.render(context, screen.getTextRenderer(), 
            VCButton.keybind(x, y, buttonWidth, buttonHeight, displayText)
                .withListening(listening)
                .withHasKey(hasKey)
                .withHovered(keybindHovered)
                .withScale(uiScale)
        );                                        
    }

    public void renderSliderControl(DrawContext context, VCEntry entry, int x, int y, int mouseX, int mouseY) {
        // Get or create persistent slider for this entry
        VCSlider slider = getOrCreateSlider(entry);
        if (slider == null) return;

        // Update slider position and value pre render if changed externally
        slider.setPosition(x, y + 3);
        slider.setValue(entry.getSliderValue());
        slider.render(context, mouseX, mouseY);
        
        float uiScale = uiScaleSupplier.get();
        int valueTextX = x + slider.getWidth() + (int)(8 * uiScale);
        int valueTextY = y + (int)(7 * uiScale);
        String valueText = formatSliderValue(entry);
        int textColor = getSliderTextColor(entry);

        VCText.drawScaledText(context, screen.getTextRenderer(), valueText, valueTextX, valueTextY, textColor, uiScale);

        if (entry.hasColorControl()) {
            int buttonGap = uiScale < 0.7f ? Math.max(1, (int)(2 * uiScale)) : (int)(3 * uiScale);
            int colorButtonX = valueTextX + (int)(screen.getTextRenderer().getWidth(valueText) * Math.min(uiScale, 1.0f)) + buttonGap;
            renderColorButton(context, entry, colorButtonX, y);
        }
    }

    public void renderToggleWithSliderControl(DrawContext context, VCEntry entry, int x, int y, int mouseX, int mouseY) {
        int currentX = x;
        float uiScale = uiScaleSupplier.get();
        int buttonHeight = (int)(18 * uiScale);
        boolean enabled = entry.getToggleState();
        int toggleWidth = (int)(30 * uiScale);
        boolean toggleHovered = VCButton.isHovered(currentX, y, toggleWidth, buttonHeight, mouseX, mouseY);
        
        VCButton.render(context, screen.getTextRenderer(),
            VCButton.toggle(x, y, toggleWidth, buttonHeight, enabled)
                .withHovered(toggleHovered)
                .withScale(uiScale)
        );

        int toggleSliderGap = uiScale < 0.7f ? Math.max(2, (int)(4 * uiScale)) : (int)(6 * uiScale);
        currentX += toggleWidth + toggleSliderGap;

        VCSlider slider = getOrCreateSlider(entry);
        if (slider != null) {
            slider.setPosition(currentX, y + (buttonHeight - slider.getHeight()) / 2);
            slider.setValue(entry.getSliderValue());
            slider.render(context, mouseX, mouseY);
            
            currentX += slider.getWidth() + (int)(8 * uiScale);
            
            float value = entry.getSliderValue();
            String valueText;
            if (value <= 0.0f) {
                valueText = "Always";
            } else {
                valueText = String.format(entry.getFormatString(), value);
            }
            int textColor = getSliderTextColor(entry);
            VCText.drawScaledText(context, screen.getTextRenderer(), valueText, currentX, y + (int)(7 * uiScale), textColor, uiScale);
        }
    }

    private VCSlider getOrCreateSlider(VCEntry entry) {
        Map<String, VCSlider> sliderRegistry = sliderRegistrySupplier.get();
        if (entry.configKey == null) return null;
        
        return sliderRegistry.computeIfAbsent(entry.configKey, key -> {
            float uiScale = uiScaleSupplier.get();
            
            VCSlider slider = new VCSlider(0, 0, entry.getSliderValue(), entry.getMinValue(), 
                entry.getMaxValue(), entry.getFormatString(), value -> {
                entry.setSliderValue(value);
                if (entry.valueChangeAction != null) {
                    entry.valueChangeAction.accept(value);
                }
            });
            
            slider.setUIScale(uiScale);
            
            return slider;
        });
    }

    public String formatSliderValue(VCEntry entry) {
        float value = entry.getSliderValue();
        
        if (entry.getSliderType() == VCEntry.SliderType.STRING) {
            String[] themes = {"Default", "purple", "Blue", "White", "Green"};
            int index = Math.clamp((int)value, 0, themes.length - 1);
            return themes[index];
        } else if (entry.getSliderType() == VCEntry.SliderType.PRESET) {
            return switch ((int)value) {
                case 0 -> "Off";
                case 1 -> "Aqua";
                case 2 -> "Mint";
                case 3 -> "Pink";
                case 4 -> "Prism";
                default -> "Off";
            };
        } else {
            if (entry.getFormatString().contains("%%")) {
                return String.format(entry.getFormatString(), value * 100);
            } else {
                return String.format(entry.getFormatString(), value);
            }
        }
    }

    private int getSliderTextColor(VCEntry entry) {
        if (Key.CUSTOM_PARTICLE_COLOR_INDEX.equals(entry.configKey)) {
            return getCurrentColor(entry);
        }
        return themeColorSupplier.get();
    }

    private int darkenColor(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        r = (int)(r * 0.8);
        g = (int)(g * 0.8);
        b = (int)(b * 0.8);

        return (color & 0xFF000000) | (r << 16) | (g << 8) | b;
    }
}
