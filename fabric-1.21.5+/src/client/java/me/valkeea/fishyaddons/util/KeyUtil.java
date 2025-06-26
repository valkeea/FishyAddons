package me.valkeea.fishyaddons.util;

import org.lwjgl.glfw.GLFW;
import java.util.HashMap;
import java.util.Map;

public class KeyUtil {
    private KeyUtil() {}
    private static final Map<Integer, String> GLFW_KEY_NAMES = new HashMap<>();
    static {
        GLFW_KEY_NAMES.put(GLFW.GLFW_KEY_A, "GLFW_KEY_A");
        GLFW_KEY_NAMES.put(GLFW.GLFW_KEY_B, "GLFW_KEY_B");
        // Use reflection once to fill the map
        for (java.lang.reflect.Field field : GLFW.class.getFields()) {
            if (field.getType() == int.class && field.getName().startsWith("GLFW_KEY_")) {
                try {
                    GLFW_KEY_NAMES.put(field.getInt(null), field.getName());
                } catch (Exception ignored) {}
            }
        }
    }

    // Returns a user-friendly display name for a key
    public static String getDisplayNameFor(String key) {
        if (key == null) return "";
        if (key.startsWith("GLFW_KEY_KP_")) {
            // Numpad keys
            return key.replace("GLFW_KEY_KP_", "Numpad ");
        }
        if (key.startsWith("GLFW_KEY_")) {
            // Other GLFW keys
            String name = key.substring("GLFW_KEY_".length()).replace('_', ' ');
            // Special cases for international keys
            if (name.startsWith("WORLD_")) {
                return "Intl " + name.substring(6);
            }
            // Capitalize first letter
            return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
        }
        if (key.startsWith("MOUSE")) {
            try {
                int btn = Integer.parseInt(key.substring(5));
                return "Mouse " + (btn + 1);
            } catch (Exception ignored) {}
            return key;
        }
        // fallback: just uppercase
        return key.substring(0, 1).toUpperCase() + key.substring(1).toLowerCase();
    }

    public static int getKeyCodeFromString(String key) {
        try {
            return (int) org.lwjgl.glfw.GLFW.class.getField(key).get(null);
        } catch (Exception e) {
            return -1;
        }
    }    

    // Returns the GLFW key name for a given key code
    public static String getGlfwKeyName(int keyCode) {
        return GLFW_KEY_NAMES.getOrDefault(keyCode, null);
    }        
}
