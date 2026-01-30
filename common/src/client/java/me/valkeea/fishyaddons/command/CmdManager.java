package me.valkeea.fishyaddons.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import me.valkeea.fishyaddons.command.handler.CommandHandler;
import me.valkeea.fishyaddons.command.handler.FgRoot;
import me.valkeea.fishyaddons.command.handler.FaRoot;
import me.valkeea.fishyaddons.command.handler.FpRoot;
import me.valkeea.fishyaddons.command.handler.FwpRoot;
import me.valkeea.fishyaddons.tool.GuiScheduler;
import me.valkeea.fishyaddons.ui.VCScreen;
import me.valkeea.fishyaddons.ui.VCState;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;

public class CmdManager {
    private CmdManager() {}
    
    private static final CommandHandler MAIN_HANDLER = new FaRoot();
    private static final CommandHandler PROFIT_HANDLER = new FpRoot();
    private static final CommandHandler WAYPOINT_HANDLER = new FwpRoot();
    private static final CommandHandler GUARD_HANDLER = new FgRoot();
    
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            registerFaCommand(dispatcher, "fa");
            registerFaCommand(dispatcher, "fishyaddons");
            registerHandlerCommand(dispatcher, PROFIT_HANDLER);
            registerHandlerCommand(dispatcher, GUARD_HANDLER);
            registerHandlerCommand(dispatcher, WAYPOINT_HANDLER);
        });
    }
    
    private static void registerFaCommand(CommandDispatcher<FabricClientCommandSource> dispatcher, String root) {
        LiteralArgumentBuilder<FabricClientCommandSource> builder = ClientCommandManager.literal(root);
        MAIN_HANDLER.register(builder);

        builder
            .then(buildSubcommandRoot("profit", PROFIT_HANDLER))
            .then(buildSubcommandRoot("guard", GUARD_HANDLER))
            .then(buildSubcommandRoot("waypoint", WAYPOINT_HANDLER))

            .then(
                ClientCommandManager.argument(
                    "query",
                    StringArgumentType.greedyString()
                ).executes(context -> {
                    String query = StringArgumentType.getString(context, "query");
                    VCState.setLastSearchText(query);
                    GuiScheduler.scheduleGui(new VCScreen());
                    return 1;
                })
            )
            .executes(context -> {
                if (CmdHelper.checkGUI() == 1) return 1;
                MinecraftClient.getInstance().execute(() ->
                    GuiScheduler.scheduleGui(new VCScreen()));
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
            LiteralArgumentBuilder<FabricClientCommandSource> builder = 
                ClientCommandManager.literal(rootName);
            handler.register(builder);
            dispatcher.register(builder);
        }
    }

    /** Builds a subcommand root for a given command handler.*/
    private static LiteralArgumentBuilder<FabricClientCommandSource> buildSubcommandRoot(
        String rootLiteral,
        CommandHandler handler
    ) {
        LiteralArgumentBuilder<FabricClientCommandSource> builder = 
            ClientCommandManager.literal(rootLiteral);
        handler.register(builder);
        return builder;
    }    
}
