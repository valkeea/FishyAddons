package me.valkeea.fishyaddons.command;

import java.util.function.IntSupplier;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import me.valkeea.fishyaddons.tool.GuiScheduler;
import me.valkeea.fishyaddons.ui.list.TabbedListScreen;
import me.valkeea.fishyaddons.util.FishyNotis;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class CommandBuilderUtils {
    private CommandBuilderUtils() {}
    
    public static class ToggleCommandBuilder {
        private final String literalName;
        private final BooleanKey configKey;
        private final String label;
        private boolean includeToggle = false;
        private IntSupplier defaultAction;
        
        public ToggleCommandBuilder(String literalName, BooleanKey configKey, String label) {
            this.literalName = literalName;
            this.configKey = configKey;
            this.label = label;
        }
        
        /** Include a "toggle" subcommand that switches between on/off. */
        public ToggleCommandBuilder withToggle() {
            this.includeToggle = true;
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
                defaultAction
            );
        }
    }
    
    protected static LiteralArgumentBuilder<FabricClientCommandSource> createToggleCommand(
            String literalName,
            BooleanKey key,
            String label,
            boolean includeToggle,
            IntSupplier defaultAction) {
        
        var builder = ClientCommands.literal(literalName);
        
        builder.then(ClientCommands.literal("on")
            .executes(context -> {
                Config.set(key, true);
                FishyNotis.on(label);
                return 1;
            }));
        
        builder.then(ClientCommands.literal("off")
            .executes(context -> {
                Config.set(key, false);
                FishyNotis.off(label);
                return 1;
            }));
        
        if (includeToggle) {
            builder.then(ClientCommands.literal("toggle")
                .executes(context -> {
                    boolean current = Config.get(key);
                    Config.set(key, !current);
                    if (!current) {
                        FishyNotis.on(label);
                    } else {
                        FishyNotis.off(label);
                    }
                    return 1;
                }));
        }
        
        builder.executes(context -> defaultAction.getAsInt());
        
        return builder;
    }

    // --- Builders ---

    /** Create a new ToggleCommandBuilder for command construction. */
    public static ToggleCommandBuilder toggleCommand(String literalName, BooleanKey configKey, String label) {
        return new ToggleCommandBuilder(literalName, configKey, label);
    }    
    
    /** Screen initialization */
    public static LiteralArgumentBuilder<FabricClientCommandSource> createGuiCommand(
            String literalName,
            Screen screen) {
        return ClientCommands.literal(literalName)
            .executes(ctx -> openGuiAction(screen).getAsInt());
    }
    
    /** Simple command with specified action. */
    public static LiteralArgumentBuilder<FabricClientCommandSource> createSimpleCommand(
            String literalName,
            IntSupplier action) {
        return ClientCommands.literal(literalName)
            .executes(ctx -> action.getAsInt());
    }

    // --- Actions ---
    
    /** Open a UI screen as the default action, checking if already in GUI. */
    public static IntSupplier openGuiAction(Screen screen) {
        return () -> {
            if (CmdHelper.checkGUI() == 1) return 1;
            Minecraft.getInstance().execute(() -> GuiScheduler.scheduleGui(screen));
            return 1;
        };
    }
    
    /** Open a tab */
    public static IntSupplier openTabbedGuiAction(TabbedListScreen.Tab tab) {
        return () -> {
            if (CmdHelper.checkGUI() == 1) return 1;
            Minecraft.getInstance().execute(() -> GuiScheduler.scheduleGui(new TabbedListScreen(tab)));
            return 1;
        };
    }
    
    /** Show a help message as the default action. */
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
    }        
}
