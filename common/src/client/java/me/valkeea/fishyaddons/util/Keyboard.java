package me.valkeea.fishyaddons.util;

import java.util.HashMap;
import java.util.Map;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Util;
import net.minecraft.util.Util.OperatingSystem;

public class Keyboard {
    private Keyboard() {}
    private static final Map<Integer, String> GLFW_KEY_NAMES = new HashMap<>();
	private static final boolean ON_MAC_OS = Util.getOperatingSystem() == OperatingSystem.OSX;
	private static final int CTRL_MODIFIER = ON_MAC_OS ? 8 : 2;
    
	public static boolean hasCtrlModifier(KeyInput input) {
		return (input.modifiers() & CTRL_MODIFIER) != 0;
	}

    static {
        GLFW_KEY_NAMES.put(GLFW.GLFW_KEY_A, "GLFW_KEY_A");
        GLFW_KEY_NAMES.put(GLFW.GLFW_KEY_B, "GLFW_KEY_B");

        for (var field : GLFW.class.getFields()) {
            if (field.getType() == int.class && field.getName().startsWith("GLFW_KEY_")) {
                try {
                    GLFW_KEY_NAMES.put(field.getInt(null), field.getName());
                } catch (Exception ignored) {
                    // Reflection failed, skip
                }
            }
        }
    }

    public static String getGlfwKeyName(int keyCode) {
        return GLFW_KEY_NAMES.getOrDefault(keyCode, null);
    }

    public static int getKeyCodeFromString(String key) {
        try {
            return (int) org.lwjgl.glfw.GLFW.class.getField(key).get(null);
        } catch (Exception e) {
            return -1;
        }
    }        

    public static String getDisplayNameFor(String key) {
        if (key == null) return "";

        if (key.startsWith("GLFW_KEY_KP_")) {
            return key.replace("GLFW_KEY_KP_", "Num ");
        }

        var uiName = uiName(key);
        if (uiName != null) {
            return uiName;
        }

        var mcName = getMcName(key);
        if (mcName != null && !mcName.isEmpty()) {
            return mcName;
        }

        return key.substring(0, 1).toUpperCase() + key.substring(1).toLowerCase();
    }

    private static String getMcName(String key) {

        try {
            int keyCode = getKeyCodeFromString(key);
            if (keyCode == -1) return null;
            
            var inputKey = InputUtil.fromKeyCode(new KeyInput(keyCode, 0, 0));
            if (inputKey != null) {

                var displayText = inputKey.getLocalizedText();
                if (displayText != null) {
                    String displayName = displayText.getString();
                    if (displayName != null && !displayName.isEmpty()) {
                        return displayName;
                    }
                }
            }
        } catch (Exception e) {
            // Not able to get name via Minecraft's InputUtil
        }
        return null;
    }

    private static String uiName(String key) {
        switch (key) {
            case "GLFW_KEY_GRAVE_ACCENT": return "Section";
            case "GLFW_KEY_LEFT_SHIFT": return "LShift";
            case "GLFW_KEY_RIGHT_SHIFT": return "RShift";
            case "GLFW_KEY_LEFT_CONTROL": return "LCtrl";
            case "GLFW_KEY_RIGHT_CONTROL": return "RCtrl";
            case "GLFW_KEY_LEFT_ALT": return "LAlt";
            case "GLFW_KEY_RIGHT_ALT": return "RAlt";
            case "GLFW_KEY_CAPS_LOCK": return "Caps";
            case "GLFW_KEY_ESCAPE": return "Esc";
            case "GLFW_KEY_DELETE": return "Del";
            case "GLFW_KEY_INSERT": return "Ins";
            case "GLFW_KEY_HOME": return "Home";
            case "GLFW_KEY_END": return "End";
            case "GLFW_KEY_PAGE_UP": return "Pg ↑";
            case "GLFW_KEY_PAGE_DOWN": return "Pg ↓";
            case "GLFW_KEY_UP": return "↑";
            case "GLFW_KEY_DOWN": return "↓";
            case "GLFW_KEY_LEFT": return "←";
            case "GLFW_KEY_RIGHT": return "→";
            default: return null;
        }
    }
}
