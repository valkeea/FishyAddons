package me.valkeea.fishyaddons.hud.elements.interactive;

import java.util.ArrayList;
import java.util.List;

import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas;
import me.valkeea.fishyaddons.compat.McApi;
import me.valkeea.fishyaddons.event.EventPhase;
import me.valkeea.fishyaddons.event.EventPriority;
import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.hud.base.InteractiveHudElement;
import me.valkeea.fishyaddons.hud.core.HudButtonManager;
import me.valkeea.fishyaddons.hud.core.HudElementState;
import me.valkeea.fishyaddons.tracker.SafariStats;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.IntKey;
import me.valkeea.fishyaddons.vconfig.ui.widget.dropdown.VCToggleMenu;
import me.valkeea.fishyaddons.vconfig.ui.widget.dropdown.item.SwitchItem;
import me.valkeea.fishyaddons.vconfig.ui.widget.dropdown.item.ToggleMenuItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class CritterDisplay extends InteractiveHudElement {
    private static CritterDisplay instance = null;

    private VCToggleMenu modeMenu;

    private CritterDisplay() {
        super(
            BooleanKey.TRACK_CRITTERS,
            "Critter Display",
            20, 320, 12, 0xFF9BF3FF,
            false, true
        );
        createModeMenu();
        registerEvents();
    }

    public static CritterDisplay getInstance() {
        if (instance == null) {
            instance = new CritterDisplay();
        }
        return instance;
    }

    private void createModeMenu() {
        int y = - 20 * getHudSize() / 12;

        modeMenu = new VCToggleMenu(
            this::getMenuItems,
            0, y, 150, 12, null
        );

        this.modeMenu.setVisible(false);
        registerToggleMenu(modeMenu);
    }

    private void registerEvents() {
        FaEvents.MOUSE_CLICK.register(e -> {
            if (instance.handleMouseClick(e.click)) {
                e.setConsumed(true);
            }
        }, EventPriority.NORMAL, EventPhase.POST);
    }

    @Override
    protected IntKey getMaxLinesConfigKey() {
        return IntKey.NONE;
    }

    private List<ToggleMenuItem> getMenuItems() {
        return SwitchItem.createList(List.of("Completion", "Capture Count", "Shard Count"), IntKey.CRITTER_MODE, 0);
    }

    // --- Display Data ---

    @Override
    protected boolean shouldRender() {
        return SafariStats.isEnabled();
    }

    @Override
    public List<Component> getDisplayLines(HudElementState state) {
        if (!shouldRender()) return List.of();
        
        List<Component> lines = new ArrayList<>();

        if (isEditingMode() && SafariStats.getData().isEmpty()) {
            lines.add(Component.literal("§dCritter Tracker"));
            return lines;
        }

        if (!SkyblockAreas.isSafari()) return List.of();

        lines.add(SafariStats.timerComponent());
        lines.addAll(SafariStats.getData());
        return lines;
    }

    @Override
    protected void postRenderCustom(GuiGraphicsExtractor gge, Minecraft mc, 
                                    HudElementState state, int mouseX, int mouseY) {

        var window = mc.getWindow();                                
        var screen = McApi.screen();

        if (!isEditingMode() && isInventoryOpen(mc)) {
        
            float scale = state.size / 12.0F;
            int hudX = state.x;
            int hudY = state.y;
            int h = window.getGuiScaledHeight();
            
            if (modeMenu.isVisible() && screen != null) {
                int menuY = hudY + 2;
                modeMenu.setPosition(hudX, menuY, h);
                modeMenu.render(gge, screen, mouseX, mouseY, scale);
            }

        } else if (screen != null && modeMenu.isVisible()) {
            modeMenu.setVisible(false);
        }
    }

    // --- Interaction ---

    @Override
    protected void setupButtons(HudButtonManager manager, HudElementState state) {
        manager.addButton("Mode",
        btn -> {
            boolean wasVisible = modeMenu.isVisible();
            modeMenu.setVisible(!wasVisible);
        });
    }

    @Override
    protected List<Component> getTooltipForLine(int lineIndex, Component line) {
        return SafariStats.getFullStats();
    }

    @Override
    protected boolean isLineClickable(int lineIndex, Component line) {
        return false;
    }
}
