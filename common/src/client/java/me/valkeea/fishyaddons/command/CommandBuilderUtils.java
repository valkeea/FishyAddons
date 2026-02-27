package me.valkeea.fishyaddons.command;

import java.util.Optional;
import java.util.function.IntSupplier;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.tool.GuiScheduler;
import me.valkeea.fishyaddons.ui.list.TabbedListScreen;
import me.valkeea.fishyaddons.util.FishyNotis;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

public class CommandBuilderUtils {
    private CommandBuilderUtils() {}
    
    public static class ToggleCommandBuilder {
        private final String literalName;
        private final String configKey;
        private final String label;
        private boolean includeToggle = false;
        private Runnable onEnableCallback;
        private Runnable onDisableCallback;
        private IntSupplier defaultAction;
        
        public ToggleCommandBuilder(String literalName, String configKey, String label) {
            this.literalName = literalName;
            this.configKey = configKey;
            this.label = label;
        }
        
        /** Include a "toggle" subcommand that switches between on/off. */
        public ToggleCommandBuilder withToggle() {
            this.includeToggle = true;
            return this;
        }
        
        /** Callback to run when the feature is enabled. */
        public ToggleCommandBuilder onEnable(Runnable callback) {
            this.onEnableCallback = callback;
            return this;
        }
        
        /** Callback to run when the feature is disabled. */
        public ToggleCommandBuilder onDisable(Runnable callback) {
            this.onDisableCallback = callback;
            return this;
        }
        
        /** Callback to run on both enable and disable. */
        public ToggleCommandBuilder onToggle(Runnable callback) {
            this.onEnableCallback = callback;
            this.onDisableCallback = callback;
            return this;
        }
        
        /** Default action when command is ran without arguments. */
        public ToggleCommandBuilder withDefaultAction(IntSupplier action) {
            this.defaultAction = action;
            return this;
        }
        
        /** Show a help message as the default action. */
        public ToggleCommandBuilder withHelpMessage(String message) {
            this.defaultAction = helpAction(message);
            return this;
        }
        
        /** Open a tabbed GUI when command is ran without arguments. */
        public ToggleCommandBuilder withGuiTab(TabbedListScreen.Tab tab) {
            this.defaultAction = openTabbedGuiAction(tab);
            return this;
        }
        
        /** Open a screen when command is ran without arguments. */
        public ToggleCommandBuilder withGuiScreen(Screen screen) {
            this.defaultAction = openGuiAction(screen);
            return this;
        }
        
        /** Build the toggle command with all configured options. */
        public LiteralArgumentBuilder<FabricClientCommandSource> build() {

            if (defaultAction == null) {
                defaultAction = helpAction("Usage: /" + literalName + " <on|off" + 
                    (includeToggle ? "|toggle" : "") + ">");
            }
            
            return createToggleCommand(
                literalName,
                configKey,
                label,
                includeToggle,
                Optional.ofNullable(onEnableCallback),
                Optional.ofNullable(onDisableCallback),
                defaultAction
            );
        }
    }
    
    protected static LiteralArgumentBuilder<FabricClientCommandSource> createToggleCommand(
            String literalName,
            String key,
            String label,
            boolean includeToggle,
            Optional<Runnable> onEnableCallback,
            Optional<Runnable> onDisableCallback,
            IntSupplier defaultAction) {
        
        LiteralArgumentBuilder<FabricClientCommandSource> builder = ClientCommandManager.literal(literalName);
        
        builder.then(ClientCommandManager.literal("on")
            .executes(context -> {
                FishyConfig.setState(key, true);
                FishyNotis.on(label);
                onEnableCallback.ifPresent(Runnable::run);
                return 1;
            }));
        
        builder.then(ClientCommandManager.literal("off")
            .executes(context -> {
                FishyConfig.disable(key);
                FishyNotis.off(label);
                onDisableCallback.ifPresent(Runnable::run);
                return 1;
            }));
        
        if (includeToggle) {
            builder.then(ClientCommandManager.literal("toggle")
                .executes(context -> {
                    boolean current = FishyConfig.getState(key, false);
                    FishyConfig.setState(key, !current);
                    if (!current) {
                        FishyNotis.on(label);
                        onEnableCallback.ifPresent(Runnable::run);
                    } else {
                        FishyNotis.off(label);
                        onDisableCallback.ifPresent(Runnable::run);
                    }
                    return 1;
                }));
        }
        
        builder.executes(context -> defaultAction.getAsInt());
        
        return builder;
    }

    // --- Builders ---

    /** Create a new ToggleCommandBuilder for command construction. */
    public static ToggleCommandBuilder toggleCommand(String literalName, String configKey, String label) {
        return new ToggleCommandBuilder(literalName, configKey, label);
    }    
    
    /** Screen initialization */
    public static LiteralArgumentBuilder<FabricClientCommandSource> createGuiCommand(
            String literalName,
            Screen screen) {
        return ClientCommandManager.literal(literalName)
            .executes(ctx -> openGuiAction(screen).getAsInt());
    }
    
    /** Simple command with specified action. */
    public static LiteralArgumentBuilder<FabricClientCommandSource> createSimpleCommand(
            String literalName,
            IntSupplier action) {
        return ClientCommandManager.literal(literalName)
            .executes(ctx -> action.getAsInt());
    }

    // --- Actions ---
    
    /** Opens a GUI screen as the default action, checking if already in GUI. */
    public static IntSupplier openGuiAction(Screen screen) {
        return () -> {
            if (CmdHelper.checkGUI() == 1) return 1;
            MinecraftClient.getInstance().execute(() -> 
                GuiScheduler.scheduleGui(screen)
            );
            return 1;
        };
    }
    
    /** Opens a tabbed GUI screen. */
    public static IntSupplier openTabbedGuiAction(TabbedListScreen.Tab tab) {
        return () -> {
            if (CmdHelper.checkGUI() == 1) return 1;
            MinecraftClient.getInstance().execute(() -> 
                GuiScheduler.scheduleGui(new TabbedListScreen(
                    MinecraftClient.getInstance().currentScreen, tab))
            );
            return 1;
        };
    }
    
    /** Shows a help message as the default action. */
    public static IntSupplier helpAction(String helpMessage) {
        return () -> {
            FishyNotis.themed(helpMessage);
            return 1;
        };
    }

    public static class CommandArgs {
        private final String[] args;
        
        public CommandArgs(String[] args) {
            this.args = args != null ? args : new String[0];
        }
        
        /** Get argument at index, or return default value if not present. */
        public String get(int index, String defaultValue) {
            return has(index) ? args[index] : defaultValue;
        }
        
        /** Get argument at index, or return null if not present. */
        public String get(int index) {
            return get(index, null);
        }
        
        public boolean has(int index) { return index >= 0 && index < args.length; }
        public int length() { return args.length; }
        
        /** Check if argument at index matches any of the provided values (case-insensitive). */
        public boolean matches(int index, String... values) {
            if (!has(index)) return false;
            String arg = args[index].toLowerCase();
            for (String value : values) {
                if (arg.equals(value.toLowerCase())) {
                    return true;
                }
            }
            return false;
        }
        
        /** Get a slice of arguments from start index to end (exclusive). */
        public String[] slice(int start, int end) {
            if (start < 0 || start >= args.length) return new String[0];
            int actualEnd = Math.min(end, args.length);
            String[] result = new String[actualEnd - start];
            System.arraycopy(args, start, result, 0, result.length);
            return result;
        }
        
        /** Get all arguments from start index onwards. */
        public String[] sliceFrom(int start) {
            return slice(start, args.length);
        }
        
        /** Join arguments from start index with a delimiter. */
        public String join(int start, String delimiter) {
            if (!has(start)) return "";
            StringBuilder sb = new StringBuilder();
            for (int i = start; i < args.length; i++) {
                if (i > start) sb.append(delimiter);
                sb.append(args[i]);
            }
            return sb.toString();
        }
        
        /** Join all arguments from start index with spaces. */
        public String joinFrom(int start) {
            return join(start, " ");
        }
    }        
}
