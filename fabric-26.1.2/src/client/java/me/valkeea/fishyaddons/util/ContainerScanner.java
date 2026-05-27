package me.valkeea.fishyaddons.util;

import me.valkeea.fishyaddons.api.skyblock.Profile;
import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.event.impl.GuiChangeEvent;
import me.valkeea.fishyaddons.event.impl.ScreenOpenEvent;
import me.valkeea.fishyaddons.tool.RunDelayed;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

@SuppressWarnings("squid:S6548")
public class ContainerScanner {
    private static final ContainerScanner INSTANCE = new ContainerScanner();
    private static final int USERNAME_SLOT = 13;

    private boolean inGui = false;
    private String current = "";
    private ContainerScanner() {}

    public static ContainerScanner getInstance() {
        return INSTANCE;
    }

    public void init() {
        FaEvents.GUI_CHANGE.register(this::onInventory);
        FaEvents.SCREEN_OPEN.register(this::onScreen);        
        FaEvents.SCREEN_CLOSE.register(e -> onScreenClose());
    }

    public void onScreen(ScreenOpenEvent event) {
        setUser(event);
        updateGui(event.titleString);
    }

    public void onInventory(GuiChangeEvent event) {
        updateGui(event.titleString);
    }

    public void onScreenClose() {
        if (inGui) reset();
    }    

    /** Username check - in Skyblock Menu the player head at slot 13 */
    private void setUser(ScreenOpenEvent event) {
        if (event.titleContains("SkyBlock Menu")) {
            var task = "username_init_" + System.currentTimeMillis();
            RunDelayed.run(() -> Profile.initUsername(event.getStackAt(USERNAME_SLOT)), 200L, task);
        }
    }

    /** Verify up-to date gui status */
    public void updateGui(String gui) {
        if (!current.equals(gui)) {
            current = gui;
        }

        inGui = true;
    }

    /** Get the current gui title */
    public static String current() {
        return INSTANCE.current;
    }

    /** Check if the player is currently in any gui */
    public static boolean inGui() {
        return INSTANCE.inGui;
    }

    private void reset() {
        current = "";
        inGui = false;
    }

    /** Check if the current screen is a player inventory or server gui */
    public static boolean isGuiOrInv() {
        var mc = Minecraft.getInstance();
        return mc.screen != null &&
               (mc.screen instanceof InventoryScreen ||
                mc.screen instanceof ContainerScreen);
    }    
}
