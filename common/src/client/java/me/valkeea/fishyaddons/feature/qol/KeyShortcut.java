package me.valkeea.fishyaddons.feature.qol;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.lwjgl.glfw.GLFW;

import me.valkeea.fishyaddons.util.ServerCommand;
import me.valkeea.fishyaddons.vconfig.annotation.VCListener;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.config.impl.ShortcutsConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

@VCModule
public class KeyShortcut {
    private KeyShortcut() {}

    private static final long GRACE_PERIOD_MS = 300;
    private static final Set<String> keysHeld = new HashSet<>();

    private static Map<String, String> cachedKeybinds = Map.of();
    private static boolean enabled = false;
    private static long lastChatClose = 0;

    @VCListener(BooleanKey.KEY_SHORTCUTS) 
    public static void refresh() {
        enabled = Config.get(BooleanKey.KEY_SHORTCUTS);
        cachedKeybinds = Map.copyOf(ShortcutsConfig.getKeybinds());
    }    

    public static void notifyChatClosed() {
        lastChatClose = System.currentTimeMillis();
        keysHeld.clear();
    }

    public static void handleShortcuts() {
        if (!enabled) return;
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastChatClose < GRACE_PERIOD_MS) return;
        
        Map<String, String> keybinds = cachedKeybinds;
        for (Map.Entry<String, String> entry : keybinds.entrySet()) {
            String key = entry.getKey();
            String command = entry.getValue();
            
            if (!ShortcutsConfig.isKeybindToggled(key)) continue;
            
            boolean isPressed = false;
            if (key.startsWith("MOUSE")) {
                int mouseButton = parseMouseButton(key);
                boolean pressed = GLFW.glfwGetMouseButton(MinecraftClient.getInstance().getWindow().getHandle(), mouseButton) == GLFW.GLFW_PRESS;
                isPressed = pressed;
                
            } else {
                int keyCode = parseKeyCode(key);
                if (keyCode != -1) {
                    isPressed = InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow(), keyCode);
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
                execute(command);
                keysHeld.add(key);
            }
        } else {
            keysHeld.remove(key);
        }
    }

    private static void execute(String command) {
        if (command != null && !command.isEmpty()) {
            if (command.startsWith("/")) command = command.substring(1);
            ServerCommand.send(command);
        }
    }
}
