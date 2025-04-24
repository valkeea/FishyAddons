package me.wait.fishyaddons.handlers;

import me.wait.fishyaddons.FishyAddons;
import me.wait.fishyaddons.config.ConfigHandler;
import me.wait.fishyaddons.gui.KeybindListGUI;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SideOnly(Side.CLIENT)
public class KeybindHandler {
    public static final KeybindHandler INSTANCE = new KeybindHandler();
    private static boolean isRegistered = false;

    private static final Map<String, Long> lastExecutionTime = new HashMap<>();
    private static final long COOLDOWN_MS = 1000;
    private static Map<String, String> cachedKeybinds = new HashMap<>();
    private final Set<String> keysHeld = new HashSet<>();

    private KeybindHandler() {
        // Private constructor to enforce singleton
    }

    private static Minecraft getMc() {
        return Minecraft.getMinecraft();
    }
    
    public static void updateRegistration() {
        if (!ConfigHandler.getKeybinds().isEmpty()) {
            if (!isRegistered) {
                MinecraftForge.EVENT_BUS.register(INSTANCE);
                isRegistered = true;
            }
        } else {
            if (isRegistered) {
                MinecraftForge.EVENT_BUS.unregister(INSTANCE);
                isRegistered = false;
            }
        }
    }

    public static void refreshKeybindCache() {
        cachedKeybinds.clear();
        cachedKeybinds.putAll(ConfigHandler.getKeybinds());
        updateRegistration();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || getMc().thePlayer == null || getMc().currentScreen != null || cachedKeybinds.isEmpty()) return;

        // Iterate through cached keybinds
        for (Map.Entry<String, String> entry : cachedKeybinds.entrySet()) {
            String key = entry.getKey();
            String command = entry.getValue();

            if (!ConfigHandler.getKeybinds().containsKey(key)) {
                continue;
            }

            if (!ConfigHandler.isKeybindToggled(key)) continue;

            // Keyboard keybinds
            int keyCode = Keyboard.getKeyIndex(key);
            if (keyCode != -1 && Keyboard.isKeyDown(keyCode)) {
                handleKeyInput(key, true);
            } else if (keyCode != -1) {
                handleKeyInput(key, false);
            }

            // Mouse button keybinds
            if (key.startsWith("MOUSE") && isMouseButtonPressed(key)) {
                handleKeyInput(key, true);
            } else if (key.startsWith("MOUSE")) {
                handleKeyInput(key, false);
            }
        }

        if (FishyAddons.openGUI.isPressed()) {
            getMc().displayGuiScreen(new KeybindListGUI());
        }
    }

    private boolean isMouseButtonPressed(String key) {
        try {
            int mouseButton = Integer.parseInt(key.replace("MOUSE", "").trim());
            return mouseButton >= 0 && mouseButton < Mouse.getButtonCount() && Mouse.isButtonDown(mouseButton);
        } catch (NumberFormatException e) {
            System.err.println("Invalid mouse button format in keybind: " + key);
            return false;
        }
    }

    private void handleKeybindExecution(String key) {
        long currentTime = System.currentTimeMillis();

        // Check if the keybind is on cooldown
        if (!lastExecutionTime.containsKey(key) || currentTime - lastExecutionTime.get(key) >= COOLDOWN_MS) {
            String command = ConfigHandler.getKeybindCommand(key);
            if (command != null && !command.isEmpty()) {
                getMc().thePlayer.sendChatMessage(command);
                lastExecutionTime.put(key, currentTime);
            }
        }
    }

    private void handleKeyInput(String key, boolean isPressed) {
        if (isPressed) {
            if (!keysHeld.contains(key)) {
                handleKeybindExecution(key);
                keysHeld.add(key);
            }
        } else {
            keysHeld.remove(key);
        }
    }
}