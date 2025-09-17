package me.valkeea.fishyaddons.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import me.valkeea.fishyaddons.tool.GuiScheduler;
import me.valkeea.fishyaddons.ui.VCScreen;
import me.valkeea.fishyaddons.ui.VCState;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;

public class CmdManager {
    private CmdManager() {}
    
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            registerFaCommand(dispatcher, "fa");
            registerFaCommand(dispatcher, "fishyaddons");
            registerFgCommand(dispatcher, "fg");
            registerFpCommand(dispatcher, "fp");
        });
    }
    
    private static void registerFaCommand(CommandDispatcher<FabricClientCommandSource> dispatcher, String root) {
        dispatcher.register(
            ClientCommandManager.literal(root)
                .then(FishyCmd.registerCmd())
                .then(FishyCmd.registerChat())
                .then(FishyCmd.registerAlert())
                .then(FishyCmd.registerKey())
                .then(FishyCmd.registerGuide())
                .then(FishyCmd.registerHelp())
                .then(FishyCmd.registerLava())
                .then(FishyCmd.registerHud())
                .then(FishyCmd.registerPing())
                .then(FishyCmd.registerCam())
                .then(FishyCmd.registerPos())
                .then(FishyCmd.registerRain())
                .then(FishyCmd.registerFishing())
                .then(buildProfitRoot("profit"))
                .executes(context -> {
                    me.valkeea.fishyaddons.util.FishyNotis.fp();
                    return 1;
                })
                .then(buildGuardRoot("guard"))
                .executes(context -> {
                    if (FishyCmd.checkGUI() == 1) return 1;
                    MinecraftClient.getInstance().execute(() ->
                        GuiScheduler.scheduleGui(new VCScreen()));
                    return 1;
                })
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
        );
    }

    private static void registerFgCommand(CommandDispatcher<FabricClientCommandSource> dispatcher, String root) {
        dispatcher.register(buildGuardRoot(root));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildGuardRoot(String rootLiteral) {
        LiteralArgumentBuilder<FabricClientCommandSource> builder = ClientCommandManager.literal(rootLiteral);
        FishyCmd.addGuardSubcommands(builder);
        builder.executes(context -> {
            VCState.setLastSearchText("safeguard");
            GuiScheduler.scheduleGui(new VCScreen());
            return 1;
        });
        return builder;
    }

    private static void registerFpCommand(CommandDispatcher<FabricClientCommandSource> dispatcher, String root) {
        dispatcher.register(buildProfitRoot(root));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildProfitRoot(String rootLiteral) {
        LiteralArgumentBuilder<FabricClientCommandSource> builder = ClientCommandManager.literal(rootLiteral);
        FishyCmd.addProfitSubcommands(builder);
        builder.executes(context -> {
            me.valkeea.fishyaddons.util.FishyNotis.fp();
            return 1;
        });
        return builder;
    }
}