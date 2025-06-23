package me.valkeea.fishyaddons.handler;

import me.valkeea.fishyaddons.config.FishyConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class KeyShortcut {
    private KeyShortcut() {}
    private static final Map<String, Long> lastExecutionTime = new HashMap<>();
    private static final long COOLDOWN_MS = 1000;
    private static final Set<String> keysHeld = new HashSet<>();
    private static Map<String, String> cachedKeybinds = Map.of();


    public static void handleShortcuts() {
        Map<String, String> keybinds = cachedKeybinds;
        for (Map.Entry<String, String> entry : keybinds.entrySet()) {
            String key = entry.getKey();
            String command = entry.getValue();
            
            if (!FishyConfig.isKeybindToggled(key)) continue;
            
            boolean isPressed = false;
            if (key.startsWith("MOUSE")) {
                int mouseButton = parseMouseButton(key); // returns zero-based
                boolean pressed = GLFW.glfwGetMouseButton(MinecraftClient.getInstance().getWindow().getHandle(), mouseButton) == GLFW.GLFW_PRESS;
                isPressed = pressed;
            } else {
                int keyCode = parseKeyCode(key);
                if (keyCode != -1) {
                    isPressed = InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), keyCode);
                }
            }
            handleKeyInput(key, command, isPressed);
        }
    }

    private static int parseKeyCode(String key) {
        // Accepts "G", "KEY_G", "GLFW_KEY_G", etc.
        try {
            if (key.startsWith("GLFW_KEY_")) {
                // Direct GLFW key name (e.g., numpad, international)
                java.lang.reflect.Field field = org.lwjgl.glfw.GLFW.class.getField(key);
                return field.getInt(null);
            }
            if (key.length() == 1) {
                // Single char, e.g. "G"
                return InputUtil.fromTranslationKey("key.keyboard." + key.toLowerCase()).getCode();
            }
            if (key.startsWith("KEY_")) {
                // e.g. "KEY_G"
                return InputUtil.fromTranslationKey("key.keyboard." + key.substring(4).toLowerCase()).getCode();
            }
            // Try as translation key
            return InputUtil.fromTranslationKey(key).getCode();
        } catch (Exception e) {
            return -1;
        }
    }

    private static int parseMouseButton(String key) {
        try {
            return Integer.parseInt(key.replace("MOUSE", "").trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static void handleKeyInput(String key, String command, boolean isPressed) {
        if (isPressed) {
            if (!keysHeld.contains(key)) {
                execute(key, command);
                keysHeld.add(key);
            }
        } else {
            keysHeld.remove(key);
        }
    }

    private static void execute(String key, String command) {
        long currentTime = System.currentTimeMillis();
        if (!lastExecutionTime.containsKey(key) || currentTime - lastExecutionTime.get(key) >= COOLDOWN_MS) {
            if (command != null && !command.isEmpty()) {
                if (command.startsWith("/")) {
                    // Remove the leading slash for sendChatCommand
                    command = command.substring(1);
                }
                MinecraftClient.getInstance().player.networkHandler.sendChatCommand(command);
                lastExecutionTime.put(key, currentTime);
            }
        }
    }

    public static void refreshCache() {
        cachedKeybinds = Map.copyOf(FishyConfig.getKeybinds());
    }
}