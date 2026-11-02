package me.valkeea.fishyaddons.feature.qol;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.mojang.blaze3d.platform.InputConstants;

import me.valkeea.fishyaddons.util.Keyboard;
import me.valkeea.fishyaddons.util.ServerCommand;
import me.valkeea.fishyaddons.vconfig.annotation.VCListener;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.config.impl.ShortcutsConfig;

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
        
        long now = System.currentTimeMillis();
        if (now - lastChatClose < GRACE_PERIOD_MS) return;
        
        Map<String, String> keybinds = cachedKeybinds;
        for (var e : keybinds.entrySet()) {

            var key = e.getKey();
            var command = e.getValue();
            if (!ShortcutsConfig.isKeybindToggled(key)) continue;
            
            boolean isPressed = false;
            if (key.startsWith("MOUSE")) {
                isPressed = Keyboard.isMouseButtonDown(Keyboard.mouseBindingToSdlButton(key));
            } else {
                int keyCode = parseKeyCode(key);
                if (keyCode != -1) {
                    isPressed = InputConstants.isKeyDown(keyCode);
                }
            }

            handleKeyInput(key, command, isPressed);
        }
    }

    private static int parseKeyCode(String key) {
        try {
            
            if (key.startsWith("GLFW_KEY_")) {
                return Keyboard.getKeyCodeFromString(key);

            } else if (key.length() == 1) {
                return InputConstants.getKey("key.keyboard." + key.toLowerCase()).getValue();

            } else if (key.startsWith("KEY_")) {
                return InputConstants.getKey("key.keyboard." + key.substring(4).toLowerCase()).getValue();

            } else {
                return InputConstants.getKey(key).getValue();
            }

        } catch (Exception _) {
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
