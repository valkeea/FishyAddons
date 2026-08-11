package me.valkeea.fishyaddons.hud.ui;

import java.util.List;

import it.unimi.dsi.fastutil.Pair;
import me.valkeea.fishyaddons.api.skyblock.GameMode;
import me.valkeea.fishyaddons.compat.McApi;
import me.valkeea.fishyaddons.event.EventPhase;
import me.valkeea.fishyaddons.event.EventPriority;
import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.feature.skyblock.EqTextures;
import me.valkeea.fishyaddons.hud.base.InteractiveHudElement;
import me.valkeea.fishyaddons.hud.core.ClickableRegionManager;
import me.valkeea.fishyaddons.hud.core.HudDrawer;
import me.valkeea.fishyaddons.hud.core.HudElementState;
import me.valkeea.fishyaddons.hud.core.HudUtils;
import me.valkeea.fishyaddons.mixin.HandledScreenAccessor;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.api.IntKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class EqDisplay extends InteractiveHudElement {

    private static final Identifier SLOT_TEXTURE = Identifier.fromNamespaceAndPath(
        "minecraft", "textures/gui/container/inventory.png"
    );
    
    private static final int SLOT_SIZE = 18;
    private static final int SLOTS = 4;    

    private static EqDisplay instance = null;
    private static Pair<Integer, Integer> anchoredPos = null;    
    private static boolean useAnchoredPos = false;
    private static double anchoredPct = 0.0;

    private EqDisplay() {
        super(
            BooleanKey.EQ_DISPLAY,
            "Equipment Display",
            -1, 150,
            12,
            0xFFFFFFFF,
            false,
            false
        );
        registerEvents();
    }

    public static EqDisplay getInstance() {
        if (instance == null) {
            instance = new EqDisplay();
        }
        return instance;
    }

    public static void reset() {
        if (instance != null) {
            instance.invalidateCache();
        }
    }

    private void registerEvents() {
        FaEvents.MOUSE_CLICK.register(e -> {
            if (instance.handleMouseClick(e.click)) {
                e.setConsumed(true);
            }
        }, EventPriority.NORMAL, EventPhase.PRE);
    }

    @Override
    protected boolean shouldRender() {
        return Config.get(BooleanKey.EQ_DISPLAY) && GameMode.skyblock() &&
        (EqTextures.hasAnyData() || isEditingMode()) &&
        (HudUtils.isInventoryOpen() || Config.get(BooleanKey.EQ_DISPLAY_ALWAYS));
    }

    @Override
    protected IntKey getMaxLinesConfigKey() {
        return IntKey.NONE;
    }

    @Override
    protected List<Component> getDisplayLines(HudElementState state) {
        return List.of();
    }

    @Override
    protected void setupClickableRegions(ClickableRegionManager manager, HudElementState state) {
        float scale = state.size / 12.0F;
        int size = (int) (SLOT_SIZE * scale);

        for (int i = 0; i < SLOTS; i++) {
            int y = state.y + (int) (i * SLOT_SIZE * scale);
            var region = manager.addRegion(state.x, y, size, size, i, idx -> openEquipment());

            var tooltip = EqTextures.getSlotTooltip(i);
            if (!tooltip.isEmpty()) {
                region.withTooltip(tooltip);
            }
        }
    }

    @Override
    protected void drawCustomContent(HudDrawer drawer, Minecraft mc, HudElementState state) {
        if (useAnchoredPos && anchoredPos == null) {
            findAnchoredPos();
        }

        for (int i = 0; i < SLOTS; i++) {
            int y = i * SLOT_SIZE;
            int texSize = 256;
            drawer.drawIcon(SLOT_TEXTURE, 0, y, 7, 83, SLOT_SIZE, SLOT_SIZE, texSize, texSize);

            if (EqTextures.hasSlotData(i) && !EqTextures.isEmptySlot(i)) {
                var stack = EqTextures.getSlotItemStack(i);
                if (stack != null && !stack.isEmpty()) {
                    drawer.drawItem(stack, 1, y + 1);
                }
            }
        }
    }

    @Override
    public boolean isHovered(double mouseX, double mouseY) {
        return true; // Always render via inventory hook
    }

    @Override
    protected int calculateContentWidth(Minecraft mc) {
        return SLOT_SIZE;
    }

    @Override
    protected int calculateContentHeight(Minecraft mc) {
        return SLOT_SIZE * SLOTS;
    }

    @Override
    public boolean hasCosmetics() {
        return false;
    }

    @Override
    public void onCacheRefresh() {
        initPositions(useAnchoredPos, anchoredPct);
    }

    private void openEquipment() {
        if (!GameMode.skyblock()) return;

        var mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.connection.sendChat(getCmd());
        }
    }    

    private String getCmd() {
        int cmdIndex = Config.get(IntKey.EQ_DISPLAY_CMD);
        return switch (cmdIndex) {
            case 1 -> "/loadouts";
            case 2 -> "/stats";
            default -> "/equipment";
        };
    }

    public static void initPositions(boolean anchored, double anchorPct) {
        useAnchoredPos = anchored;
        anchoredPct = anchorPct;
        anchoredPos = null;
    }

    private static void findAnchoredPos() {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;

        var screen = McApi.screen();
        if (screen instanceof InventoryScreen inv) {

            var handler = inv.getMenu();
            var hsa = (HandledScreenAccessor) inv;
            int guiX = hsa.getX();
            int guiY = hsa.getY();

            int x = guiX - SLOT_SIZE + getAnchoredOffset();
            int y = guiY + handler.getSlot(5).y -1;
            
            anchoredPos = Pair.of(x, y);

            if (anchoredPos != null) {
                instance.setHudPosition(anchoredPos.left(), anchoredPos.right());
                instance.invalidateCache();
            }
        }
    }

    private static int getAnchoredOffset() {
        return (int) (anchoredPct * SLOT_SIZE);
    }
}
