package me.valkeea.fishyaddons.command;

import java.util.Set;
import java.util.stream.Collectors;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;

import me.valkeea.fishyaddons.command.handler.CommandHandler;
import me.valkeea.fishyaddons.command.handler.FaRoot;
import me.valkeea.fishyaddons.command.handler.FgRoot;
import me.valkeea.fishyaddons.command.handler.FpRoot;
import me.valkeea.fishyaddons.command.handler.FwpRoot;
import me.valkeea.fishyaddons.vconfig.ui.manager.ScreenManager;
import me.valkeea.fishyaddons.vconfig.ui.screen.VCState;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public class CmdManager {
    private CmdManager() {}
    
    private static final CommandHandler MAIN_HANDLER = new FaRoot();
    private static final CommandHandler PROFIT_HANDLER = new FpRoot();
    private static final CommandHandler WAYPOINT_HANDLER = new FwpRoot();
    private static final CommandHandler GUARD_HANDLER = new FgRoot();
    
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            registerFaCommand(dispatcher, MAIN_HANDLER.getRootNames()[0]);
            registerFaCommand(dispatcher, MAIN_HANDLER.getRootNames()[1]);
            registerHandlerCommand(dispatcher, PROFIT_HANDLER);
            registerHandlerCommand(dispatcher, GUARD_HANDLER);
            registerHandlerCommand(dispatcher, WAYPOINT_HANDLER);
        });
    }
    
    private static void registerFaCommand(CommandDispatcher<FabricClientCommandSource> dispatcher, String root) {
        var builder = ClientCommands.literal(root);
        MAIN_HANDLER.register(builder);

        builder
            .then(buildSubcommandRoot("profit", PROFIT_HANDLER))
            .then(buildSubcommandRoot("guard", GUARD_HANDLER))
            .then(buildSubcommandRoot("waypoint", WAYPOINT_HANDLER));

        Set<String> reservedNames = builder.getArguments().stream()
            .map(CommandNode::getName)
            .collect(Collectors.toSet());

        builder
            .then(
                ClientCommands.argument(
                    "query",
                    GatedGreedyString.excluding(reservedNames)
                ).executes(context -> {
                    String query = StringArgumentType.getString(context, "query");
                    VCState.setSearchTo(query);
                    ScreenManager.openConfigScreen();
                    return 1;
                })
            )
            .executes(context -> {
                if (CmdHelper.checkGUI() == 1) return 1;
                ScreenManager.openConfigScreen();
                return 1;
            });
        
        dispatcher.register(builder);
    }

    /** Registers a command handler's commands under its root names.*/
    private static void registerHandlerCommand(
        CommandDispatcher<FabricClientCommandSource> dispatcher,
        CommandHandler handler
    ) {
        String[] rootNames = handler.getRootNames();
        for (String rootName : rootNames) {
            var builder = ClientCommands.literal(rootName);
            handler.register(builder);
            dispatcher.register(builder);
        }
    }

    /** Builds a subcommand root for a given command handler.*/
    private static LiteralArgumentBuilder<FabricClientCommandSource> buildSubcommandRoot(
        String rootLiteral,
        CommandHandler handler
    ) {
        var builder = ClientCommands.literal(rootLiteral);
        handler.register(builder);
        return builder;
    }    
}
