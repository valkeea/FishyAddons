package me.valkeea.fishyaddons.util;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.sdl.SDLMouse;
import org.lwjgl.sdl.SDLScancode;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;

public class Keyboard {
    private Keyboard() {}

    private static final Map<Integer, String> SCANCODE_TO_NAME = new HashMap<>();
    private static final Map<String, Integer> NAME_TO_SCANCODE = new HashMap<>();
    private static final Map<String, String> SDL_TO_GLFW = Map.ofEntries(
        Map.entry("GRAVE", "GRAVE_ACCENT"),
        Map.entry("LSHIFT", "LEFT_SHIFT"), Map.entry("RSHIFT", "RIGHT_SHIFT"),
        Map.entry("LCTRL", "LEFT_CONTROL"), Map.entry("RCTRL", "RIGHT_CONTROL"),
        Map.entry("LALT", "LEFT_ALT"), Map.entry("RALT", "RIGHT_ALT"),
        Map.entry("LGUI", "LEFT_SUPER"), Map.entry("RGUI", "RIGHT_SUPER"),
        Map.entry("CAPSLOCK", "CAPS_LOCK"), Map.entry("RETURN", "ENTER"),
        Map.entry("PAGEUP", "PAGE_UP"), Map.entry("PAGEDOWN", "PAGE_DOWN"),
        Map.entry("LEFTBRACKET", "LEFT_BRACKET"), Map.entry("RIGHTBRACKET", "RIGHT_BRACKET"),
        Map.entry("EQUALS", "EQUAL"), Map.entry("PRINTSCREEN", "PRINT_SCREEN"),
        Map.entry("SCROLLLOCK", "SCROLL_LOCK"), Map.entry("NUMLOCKCLEAR", "NUM_LOCK"),
        Map.entry("KP_PLUS", "KP_ADD"), Map.entry("KP_MINUS", "KP_SUBTRACT"),
        Map.entry("KP_PERIOD", "KP_DECIMAL"), Map.entry("KP_EQUALS", "KP_EQUAL"),
        Map.entry("NONUSBACKSLASH", "WORLD_1"));

    // Config stores GLFW mouse button indices; SDL uses 1 = left, 2 = middle, 3 = right, 4/5 = side.
    private static final int[] GLFW_TO_SDL_BUTTON = {1, 3, 2, 4, 5};

    static {
        String prefix = "SDL_SCANCODE_";
        for (var field : SDLScancode.class.getFields()) {
            String fieldName = field.getName();
            if (field.getType() != int.class || !fieldName.startsWith(prefix)) continue;
            String sdlName = fieldName.substring(prefix.length());
            if (sdlName.equals("UNKNOWN") || sdlName.equals("COUNT")) continue;
            try {
                int scancode = field.getInt(null);
                String name = "GLFW_KEY_" + SDL_TO_GLFW.getOrDefault(sdlName, sdlName);
                SCANCODE_TO_NAME.putIfAbsent(scancode, name);
                NAME_TO_SCANCODE.putIfAbsent(name, scancode);
            } catch (Exception _) {
                // Reflection failed, skip
            }
        }
    }

    public static boolean hasCtrlModifier(KeyEvent input) {
        return input.hasControlDownWithQuirk();
    }

    /** @return the config name for an SDL scancode, or null if unknown */
    public static String getGlfwKeyName(int scancode) {
        return SCANCODE_TO_NAME.get(scancode);
    }

    /** @return the SDL scancode for a config key name, or -1 if unknown */
    public static int getKeyCodeFromString(String key) {
        if (key == null) return -1;
        return NAME_TO_SCANCODE.getOrDefault(key, -1);
    }

    /** Converts a stored (GLFW-indexed) mouse binding like "MOUSE3" to an SDL button, or -1. */
    public static int mouseBindingToSdlButton(String key) {
        try {
            int glfw = Integer.parseInt(key.replace("MOUSE", "").trim());
            return glfw >= 0 && glfw < GLFW_TO_SDL_BUTTON.length ? GLFW_TO_SDL_BUTTON[glfw] : -1;
        } catch (NumberFormatException _) {
            return -1;
        }
    }

    /** Converts an SDL button (from a click event) to the stored binding string, or null. */
    public static String sdlButtonToMouseBinding(int sdlButton) {
        for (int i = 0; i < GLFW_TO_SDL_BUTTON.length; i++) {
            if (GLFW_TO_SDL_BUTTON[i] == sdlButton) return "MOUSE" + i;
        }
        return null;
    }

    public static boolean isMouseButtonDown(int sdlButton) {
        if (sdlButton < 1) return false;
        return (SDLMouse.SDL_GetMouseState((FloatBuffer) null, (FloatBuffer) null) & (1 << (sdlButton - 1))) != 0;
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
            
            var inputKey = InputConstants.Type.KEYBOARD.getOrCreate(keyCode);
            var displayText = inputKey.getDisplayName();
            String displayName = displayText.getString();

            if (!displayName.isEmpty()) {
                return displayName;
            }

        } catch (Exception _) {
            // Not able to get name via Minecraft's InputConstants
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

    public static boolean isShiftDown() {
        var mc = Minecraft.getInstance();
        if (mc.options == null) return false;
        int keyCode = mc.options.keyShift.getDefaultKey().getValue();
        return InputConstants.isKeyDown(keyCode);
    }
}
