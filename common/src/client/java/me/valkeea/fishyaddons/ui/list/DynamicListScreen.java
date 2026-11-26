package me.valkeea.fishyaddons.ui.list;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import me.valkeea.fishyaddons.feature.qol.FishyPresets;
import me.valkeea.fishyaddons.tool.FishyMode;
import me.valkeea.fishyaddons.ui.element.ChoicePopup;
import me.valkeea.fishyaddons.ui.element.DropdownMenu;
import me.valkeea.fishyaddons.util.text.Color;
import me.valkeea.fishyaddons.vconfig.ui.screen.AdjustedScreen;
import me.valkeea.fishyaddons.vconfig.ui.widget.FaButton;
import me.valkeea.fishyaddons.vconfig.ui.widget.FaTextField;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public abstract class DynamicListScreen extends AdjustedScreen {
    private ChoicePopup activePopup;
    private DropdownMenu presets;

    protected DynamicListScreen(Component title) {
        super(title);
    }

    /**
     * Whether the screen is currently in the process of adding a new entry.
     */
    public abstract boolean isAdding();

    /**
     * Sets the adding state of the screen.
     */
    public abstract void setAdding(boolean state);

    /**
     * Sets an overlay that routes input to the specified widget
     * until called with a different overlay or null.
     */
    public void setOverlay(AbstractWidget overlay) {
        if (activePopup != null) {
            removeWidget(activePopup);
            activePopup = null;
        }
        if (overlay instanceof ChoicePopup popup) {
            addRenderableWidget(popup);
            setFocused(popup);
            activePopup = popup;
        }
    }

    @Override
    protected void init() {
        super.init();
        activePopup = null;
        presets = null;
    }

    protected void renderPresets(GuiGraphicsExtractor gge, int mX, int mY) {
        if (presets != null && presets.isVisible()) presets.render(gge, this, mX, mY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        var adjusted = adjustClick(click);
        if (activePopup != null) return activePopup.mouseClicked(adjusted, doubled);
        if (presets != null && presets.isVisible()) {
            if (presets.mouseClicked(adjusted)) {
                setFocused(null);
                return true;
            }
            if (!presets.isMouseOver(adjusted.x(), adjusted.y())) return true;
        }

        boolean handled = super.mouseClicked(click, doubled);
        if (!handled) setFocused(null);
        return handled;
    }

    protected void addPresetButtons(int x, int y, int btnW, int h, Supplier<FishyPresets.PresetType> type,
                                    BiConsumer<String, Boolean> load, Consumer<String> save) {

        var loadTt = Tooltip.create(text("§8Add saved settings from a preset config. Existing settings persist."));
        var saveTt = Tooltip.create(text("§8Save the current config as a preset."));
        var swapTt = Tooltip.create(text("§8Swap the current config with a preset. Existing settings are wiped."));

        int color = FishyMode.getThemeColor();
        int loadX = x;
        var loadBtn = new FaButton(
            x, y, btnW, h,
            styled("Load", Color.createGradient(color, 0.3f)),
            b -> showPresets(type.get(), false, loadX, y, load));
        loadBtn.setTooltip(loadTt);
        addRenderableWidget(loadBtn);

        x += btnW;
        var saveBtn = new FaButton.Trigger(
            x, y, btnW, h,
            styled("Save", Color.createGradient(color, 0.5f)), b -> showSavePreset(save)) {};

        saveBtn.setTooltip(saveTt);
        addRenderableWidget(saveBtn);

        x += btnW;
        int swapX = x;
        var swapBtn = new FaButton(x, y, btnW, h,
            styled("Swap", Color.createGradient(color, 1.0f)),
            b -> showPresets(type.get(), true, swapX, y, load));
        swapBtn.setTooltip(swapTt);
        addRenderableWidget(swapBtn);
    }

    private void showSavePreset(Consumer<String> save) {
        var tf = new FaTextField(font, width / 2 - 60, height / 2, 120, 20, text("Preset Name"));
        tf.setMaxLength(32);
        tf.set("");
        ChoicePopup.set("Enter preset name", text("Save"), () -> {
            String suffix = tf.get().trim();
            if (!suffix.isEmpty()) save.accept(suffix);
            setOverlay(null);
        }, text("Cancel"), () -> setOverlay(null), tf, this);
    }

    private void showPresets(FishyPresets.PresetType type, boolean override, int x, int y,
            BiConsumer<String, Boolean> load) {
        var suffixes = FishyPresets.listPresetSuffixes(type);

        if (suffixes.isEmpty()) {
            ChoicePopup.set("No presets found for this tab.", text("OK"), () -> setOverlay(null),
                text(""), () -> {}, null, this);
            return;
        }

        int menuW = 0;
        for (String suffix : suffixes) {
            int w = font.width(suffix) + 10;
            if (menuW < w) menuW = w;
        }

        int entryH = 14;
        presets = new DropdownMenu(suffixes, x, y - suffixes.size() * entryH, menuW, entryH,
            suffix -> load.accept(suffix, override));
    }

    protected Component text(String s) {
        return Component.literal(s);
    }

    protected Component styled(String s, int color) {
        return Component.literal(s).withStyle(style -> style.withColor(color));
    }    
}
