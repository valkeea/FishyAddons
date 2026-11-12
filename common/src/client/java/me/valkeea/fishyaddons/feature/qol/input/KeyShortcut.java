package me.valkeea.fishyaddons.feature.qol.input;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.blaze3d.platform.InputConstants;

import me.valkeea.fishyaddons.event.EventGroup;
import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.event.impl.KeyInputEvent;
import me.valkeea.fishyaddons.event.impl.MouseInputEvent;
import me.valkeea.fishyaddons.event.impl.ScreenChangeEvent;
import me.valkeea.fishyaddons.util.Keyboard;
import me.valkeea.fishyaddons.vconfig.annotation.VCListener;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.config.impl.ShortcutsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;

@VCModule
public final class KeyShortcut {
    private static final Logger LOGGER = LoggerFactory.getLogger(KeyShortcut.class);
    private static final KeyPressTracker<String> keys = new KeyPressTracker<>();
    private static final ShortcutContext context = new ShortcutContext();
    private static final EventGroup events = new EventGroup();
    private static Map<String, KeyBind> config = Map.of();
    private static boolean enabled;
    private static boolean resync;

    record KeyBind(int code, boolean mouse, String command) {}

    private KeyShortcut() {}

    private static void register(boolean v) {
        events.subscribe(v, () -> events.addAll(
            () -> FaEvents.KEYBOARD_OBSERVE.register(KeyShortcut::onKeyboardInput),
            () -> FaEvents.MOUSE_OBSERVE.register(KeyShortcut::onMouseInput),
            () -> FaEvents.SCREEN_CHANGE.register(KeyShortcut::onScreenChange)
        ));
    }

    @VCListener(BooleanKey.KEY_SHORTCUTS)
    public static void refresh() {
        enabled = Config.get(BooleanKey.KEY_SHORTCUTS);
        if (!enabled) {
            config = Map.of();
            keys.retainKeys(config.keySet());
            return;
        }

        Map<String, KeyBind> parsed = new HashMap<>();
        for (var e : ShortcutsConfig.getKeybinds().entrySet()) {
            var binding = parseBinding(e.getKey(), e.getValue());
            if (binding != null) parsed.put(e.getKey(), binding);
        }

        config = Map.copyOf(parsed);
        keys.retainKeys(config.keySet());
        if (events.isEnabled()) {
            var mc = Minecraft.getInstance();
            if (mc != null && mc.isWindowActive()) sync();
            else resync = true;
        }

        register(enabled);
    }

    static void notifyChatClosed() {
        context.chatClosed(System.nanoTime());
    }

    static void suppressHeldKeys() {
        var mc = Minecraft.getInstance();
        if (mc.isWindowActive()) {
            sync();
        }
    }

    /** Called when the window focus changes. */
    static void updateFocus(boolean focused) {
        if (!focused) {
            resync = true;
            return;
        }
        if (!resync) return;
        resync = false;
        sync();
    }

    static void onScreenChange(ScreenChangeEvent e) {
        if (e.previous instanceof ChatScreen && !(e.next instanceof ChatScreen)) {
            notifyChatClosed();
        }
        suppressHeldKeys();
    }

    static void onKeyboardInput(KeyInputEvent e) {
        handleInput(e.input.key(), false, e.action, e.mc, e.screen != null);
    }

    static void onMouseInput(MouseInputEvent e) {
        handleInput(e.input.button(), true, e.action, e.mc, e.screen != null);
    }

    private static void handleInput(int code, boolean mouse, int action, Minecraft mc, boolean screenOpen) {
        if (action != InputConstants.PRESS && action != InputConstants.RELEASE) return;

        var pressed = action == InputConstants.PRESS;
        var allowed = !resync && mc.isWindowActive() && context.allowsShortcut(
            enabled, mc.player != null, screenOpen, System.nanoTime());

        for (var e : config.entrySet()) {
            var binding = e.getValue();
            if (binding.mouse() != mouse || binding.code() != code) continue;
            boolean active = allowed && ShortcutsConfig.isKeybindToggled(e.getKey());
            if (keys.update(e.getKey(), pressed, active)) {
                execute(binding.command());
            }
        }
    }

    private static void sync() {
        for (var e : config.entrySet()) {

            var binding = e.getValue();
            boolean pressed = binding.mouse()
                ? Keyboard.isMouseButtonDown(binding.code())
                : Keyboard.isKeyDown(binding.code());

            boolean active = ShortcutsConfig.isKeybindToggled(e.getKey());
            keys.update(e.getKey(), pressed, active);
        }
    }
    /**
     * Parses a key binding string into a {@link KeyBind}.
     * Returns null if the binding is invalid.
     */
    static @Nullable KeyBind parseBinding(String key, String command) {
        try {
            if (key.startsWith("MOUSE")) {
                int button = Keyboard.mouseBindingToButton(key);
                if (button != -1) return new KeyBind(button, true, command);

            } else {
                int code;
                if (key.startsWith("GLFW_KEY_")) {
                    code = Keyboard.getKeyCodeFromString(key);

                } else if (key.length() == 1) {
                    code = InputConstants.getKey(
                        "key.keyboard." + key.toLowerCase(Locale.ROOT)).getValue();

                } else if (key.startsWith("KEY_")) {
                    code = InputConstants.getKey(
                        "key.keyboard." + key.substring(4)
                        .toLowerCase(Locale.ROOT)).getValue();

                } else code = InputConstants.getKey(key).getValue();
                if (code != -1)  return new KeyBind(code, false, command);
            }

        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid shortcut binding '{}': {}", key, e.getMessage());
            return null;
        }

        LOGGER.warn("Invalid shortcut binding '{}'", key);
        return null;
    }

    private static void execute(String command) {
        if (command.isEmpty()) return;
        if (command.startsWith("/")) command = command.substring(1);

        var player = Minecraft.getInstance().player;
        
        if (player != null && player.connection != null) {
            player.connection.sendCommand(command);
            context.shortcutSent(System.nanoTime());
        }
    }

    static class ShortcutContext {
        static final long CHAT_BUFFER_NS = 250_000_000;
        static final long SHORTCUT_BUFFER_NS = 2 * CHAT_BUFFER_NS;
        long chatClosedAt;
        long cmdSentAt;

        void chatClosed(long now) {
            chatClosedAt = now;
        }

        void shortcutSent(long now) {
            cmdSentAt = now;
        }

        boolean allowsShortcut(boolean enabled, boolean hasPlayer, boolean screenOpen, long now) {
            return enabled && hasPlayer && !screenOpen && now - chatClosedAt >= CHAT_BUFFER_NS 
            && now - cmdSentAt >= SHORTCUT_BUFFER_NS;
        }
    }
}
