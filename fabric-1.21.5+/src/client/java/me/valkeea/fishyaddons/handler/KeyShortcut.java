package me.valkeea.fishyaddons.handler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.lwjgl.glfw.GLFW;

import me.valkeea.fishyaddons.config.FishyConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

public class KeyShortcut {
    private KeyShortcut() {}

    private static final long COOLDOWN_MS = 1000;
    private static final Set<String> keysHeld = new HashSet<>();
    private static final Map<String, Long> lastExecutionTime = new HashMap<>();  

    private static Map<String, String> cachedKeybinds = Map.of();
    private static boolean enabled = false;
    
    public static void refresh() {
        enabled = FishyConfig.getState(me.valkeea.fishyaddons.config.Key.KEY_SHORTCUTS_ENABLED, true);
        cachedKeybinds = Map.copyOf(FishyConfig.getKeybinds());
    }    

    public static void handleShortcuts() {
        if (!enabled) return;
        Map<String, String> keybinds = cachedKeybinds;
        for (Map.Entry<String, String> entry : keybinds.entrySet()) {
            String key = entry.getKey();
            String command = entry.getValue();
            
            if (!FishyConfig.isKeybindToggled(key)) continue;
            
            boolean isPressed = false;
            if (key.startsWith("MOUSE")) {
                int mouseButton = parseMouseButton(key);
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
        try {
            if (key.startsWith("GLFW_KEY_")) {
                java.lang.reflect.Field field = org.lwjgl.glfw.GLFW.class.getField(key);
                return field.getInt(null);
            }
            if (key.length() == 1) {
                return InputUtil.fromTranslationKey("key.keyboard." + key.toLowerCase()).getCode();
            }
            if (key.startsWith("KEY_")) {
                return InputUtil.fromTranslationKey("key.keyboard." + key.substring(4).toLowerCase()).getCode();
            }
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
        if ((command != null && !command.isEmpty()) && (!lastExecutionTime.containsKey(key) || currentTime - lastExecutionTime.get(key) >= COOLDOWN_MS)) {
            if (command.startsWith("/")) {
                command = command.substring(1);
            }
            MinecraftClient.getInstance().player.networkHandler.sendChatCommand(command);
            lastExecutionTime.put(key, currentTime);
        }
    }
}