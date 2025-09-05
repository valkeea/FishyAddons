package me.valkeea.fishyaddons.hud;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.util.EqTextures;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class EqDisplay {
    private static EqDisplay instance = null;    
    private static final Logger LOGGER = LoggerFactory.getLogger("FishyAddons/ArmorDisplay");
    private static final Identifier SLOT_TEXTURE = Identifier.of("minecraft", "textures/gui/container/inventory.png");

    private static final Map<Integer, ItemStack> cachedSkullItems = new HashMap<>();
    private static final Map<Integer, Integer[]> cachedArmorPositions = new HashMap<>();
    private static final Map<Integer, Integer[]> renderedPositions = new HashMap<>();

    private static final long CACHE_REFRESH_INTERVAL = 5000;
    private static final int SLOT_SIZE = 18;   

    private static boolean positionsComputed = false;
    private static boolean renderedPositionsComputed = false;
    private static boolean shouldRender = false;
    private static long lastEquipmentCheck = 0;

    public static EqDisplay getInstance() {
        if (instance == null) {
            instance = new EqDisplay();
        }
        return instance;
    }    
    
    /**
     * Refresh cached equipment data
     */
    private static void refresh() {

        cachedSkullItems.clear();
        for (int i = 0; i < 4; i++) {
            ItemStack item = EqTextures.getSlotItemStack(i);
            if (item != null) {
                cachedSkullItems.put(i, item);
            }
        }
    }

    public static void reset() {
        shouldRender = false;
        lastEquipmentCheck = 0;
    }

    /**
     * Calculate the actual rendered positions
     */
    private static void computePos(InventoryScreen screen) {
        if (renderedPositionsComputed) return;
        
        int[][] armorSlotPositions;
        if (!positionsComputed) {
            armorSlotPositions = findVanillaPos(screen);
            if (armorSlotPositions.length >= 4) {
                for (int i = 0; i < 4; i++) {
                    cachedArmorPositions.put(i, new Integer[]{armorSlotPositions[i][0], armorSlotPositions[i][1]});
                }
                positionsComputed = true;
            }
        } else {
            armorSlotPositions = new int[4][2];
            for (int i = 0; i < 4; i++) {
                Integer[] cached = cachedArmorPositions.get(i);
                if (cached != null) {
                    armorSlotPositions[i][0] = cached[0];
                    armorSlotPositions[i][1] = cached[1];
                }
            }
        }
        
        if (armorSlotPositions.length >= 4) {
            for (int i = 0; i < 4; i++) {
                int vanillaSlotX = armorSlotPositions[i][0];
                int vanillaSlotY = armorSlotPositions[i][1];
                int slotX = vanillaSlotX - 24;
                int slotY = vanillaSlotY - 1;
                renderedPositions.put(i, new Integer[]{slotX, slotY});
            }
            renderedPositionsComputed = true;
        } else {
            // Fallback
            LOGGER.warn("Using fallback positions");
            for (int i = 0; i < 4; i++) {
                int slotX = -50;
                int slotY = 10 + (i * 20);
                renderedPositions.put(i, new Integer[]{slotX, slotY});
            }
            renderedPositionsComputed = true;
        }
    }
    
    /**
     * Render a secondary armor display on the inventory screen
     */
    public static void render(DrawContext context, InventoryScreen screen) {
        if (!FishyConfig.getState(Key.EQ_DISPLAY, false) || !shouldRender()) {
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        // Compute positions only when needed
        computePos(screen);

        long currentTime = System.currentTimeMillis();
        boolean shouldRefreshCache = (currentTime - lastEquipmentCheck) > CACHE_REFRESH_INTERVAL;
        if (shouldRefreshCache) {
            refresh();
            lastEquipmentCheck = currentTime;
        }
        
        for (int i = 0; i < 4; i++) {
            Integer[] pos = renderedPositions.get(i);
            if (pos != null) {
                int slotX = pos[0];
                int slotY = pos[1];
                
                renderSlotBg(context, slotX, slotY);
                if (EqTextures.hasSlotData(i)) {
                    if (EqTextures.isEmptySlot(i)) {
                    } else if (cachedSkullItems.containsKey(i) || EqTextures.hasSkullTexture(i)) {
                        renderSkull(context, slotX + 1, slotY + 1, i);
                    }
                }
            }
        }
    }

    private static int[][] findVanillaPos(InventoryScreen screen) {
        try {
            var handler = screen.getScreenHandler();
            if (handler == null) {
                return new int[0][0];
            } else {
                int[][] positions = new int[4][2];

                for(int i = 0; i < 4; ++i) {
                positions[i][0] = 8;
                positions[i][1] = 8 + i * 18;
                }

                return positions;
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to find vanilla armor slot positions: {}", e.getMessage());
            return new int[0][0];
        }
    }    
    
    private static boolean shouldRender() {
        if (!shouldRender) {
            if (!FishyConfig.getState(Key.EQ_DISPLAY, false)) {
                shouldRender = false;
            }
            for (int i = 0; i < 4; i++) {
                if (EqTextures.hasSlotData(i)) {
                    shouldRender = true;
                }
            }
        }
        return shouldRender;
    }
    
    private static void renderSlotBg(DrawContext context, int x, int y) {
        context.drawTexture(net.minecraft.client.render.RenderLayer::getGuiTextured, 
                           SLOT_TEXTURE, x, y, 7, 83, SLOT_SIZE, SLOT_SIZE, 256, 256);
    }
    
    private static void renderSkull(DrawContext context, int x, int y, int slotIndex) {
        try {
            ItemStack skullStack = EqTextures.getSlotItemStack(slotIndex);
            if (skullStack != null && !skullStack.isEmpty()) {
                context.drawItem(skullStack, x, y);
            } else {
                context.fill(x, y, x + 16, y + 16, 0xFF404040);
            }
            
        } catch (Exception e) {
            context.fill(x, y, x + 16, y + 16, 0xFF8B4513);
        }
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!shouldRender()) { return false; }
        for (int i = 0; i < 4; i++) {
            Integer[] pos = renderedPositions.get(i);
            if (pos != null) {
                int slotX = pos[0];
                int slotY = pos[1];
                if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE &&
                    mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean handleMouseClick(int button) {
        if (!me.valkeea.fishyaddons.util.SkyblockCheck.getInstance().isInSkyblock()) {
            return false;
        }

        if (button == 0) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.networkHandler.sendChatMessage("/equipment");
                return true;
            }
        }
        return false;
    }
}