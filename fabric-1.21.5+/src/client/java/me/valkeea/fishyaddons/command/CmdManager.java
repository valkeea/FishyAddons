package me.valkeea.fishyaddons.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import me.valkeea.fishyaddons.gui.FishyAddonsScreen;
import me.valkeea.fishyaddons.gui.SafeguardScreen;
import me.valkeea.fishyaddons.tool.GuiScheduler;
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
        });
    }
    
    private static void registerFaCommand(CommandDispatcher<FabricClientCommandSource> dispatcher, String root) {
        dispatcher.register(ClientCommandManager.literal(root)
            .then(FishyCmd.registerCmd())
            .then(FishyCmd.registerChat())
            .then(FishyCmd.registerAlert())
            .then(FishyCmd.registerKey())
            .then(FishyCmd.registerGuide())
            .then(FishyCmd.registerHelp())
            .then(FishyCmd.registerLava())
            .then(FishyCmd.registerQol())
            .then(FishyCmd.registerVisual())
            .then(FishyCmd.registerHud())
            .then(FishyCmd.registerPing())
            .then(FishyCmd.registerCam())
            .then(FishyCmd.registerPos())
            .then(buildGuardRoot("guard", root + " guard"))
            .executes(context -> {
                if (FishyCmd.checkGUI() == 1) return 1;
                MinecraftClient.getInstance().execute(() ->
                    GuiScheduler.scheduleGui(new FishyAddonsScreen()));
                return 1;
            })               
        );
    }

    private static void registerFgCommand(CommandDispatcher<FabricClientCommandSource> dispatcher, String root) {
        dispatcher.register(buildGuardRoot(root, root));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildGuardRoot(String rootLiteral, String rootNameForMessages) {
        LiteralArgumentBuilder<FabricClientCommandSource> builder = ClientCommandManager.literal(rootLiteral);
        FishyCmd.addGuardSubcommands(builder, rootNameForMessages);
        builder.executes(context -> {
            if (FishyCmd.checkGUI() == 1) return 1;
            MinecraftClient.getInstance().execute(() ->
                GuiScheduler.scheduleGui(new SafeguardScreen()));
            return 1;
        });
        return builder;
    }
}
