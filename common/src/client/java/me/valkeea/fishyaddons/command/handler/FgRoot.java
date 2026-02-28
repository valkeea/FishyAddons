package me.valkeea.fishyaddons.command.handler;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import me.valkeea.fishyaddons.command.CmdHelper;
import me.valkeea.fishyaddons.config.ItemConfig;
import me.valkeea.fishyaddons.feature.item.safeguard.ItemHandler;
import me.valkeea.fishyaddons.tool.GuiScheduler;
import me.valkeea.fishyaddons.tool.ItemData;
import me.valkeea.fishyaddons.ui.VCScreen;
import me.valkeea.fishyaddons.ui.VCState;
import me.valkeea.fishyaddons.util.FishyNotis;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class FgRoot implements CommandHandler {
    
    @Override
    public String[] getRootNames() {
        return new String[]{"guard", "fg"};
    }
    
    @Override
    public void register(LiteralArgumentBuilder<FabricClientCommandSource> builder) {
        builder
            .then(addCmd())
            .then(removeCmd())
            .then(listCmd())
            .then(clearCmd())
            .then(confirmClearCmd())
            .then(cancelClearCmd())
            .then(helpCmd())
            .executes(context -> {
                VCState.setLastSearchText("safeguard");
                GuiScheduler.scheduleGui(new VCScreen());
                return 1;
            });
    }

    private static class CmdChat {
        static boolean pendingClear = false;
    }    
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> addCmd() {
        return ClientCommandManager.literal("add")
        .executes(ctx -> {

            var mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.world == null) return 1;

            var held = mc.player.getMainHandStack();
            if (held == null || held.isEmpty()) {
                FishyNotis.notice("You must be holding an item to use this command.");
                return 1;
            }

            var uuid = ItemData.extractUUID(held);
            if (uuid.isEmpty()) {
                FishyNotis.warn("Held item doesn't have a UUID.");
                return 1;
            }

            var name = held.getName();
            ItemConfig.addUUID(uuid, name);

            FishyNotis.format(Text.literal("Your ").formatted(Formatting.GRAY)
                .append(name)
                .append(Text.literal(" is now protected.").formatted(Formatting.GRAY)));

            return 1;
        });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> removeCmd() {
        return ClientCommandManager.literal("remove")
        .executes(ctx -> {

            var mc = MinecraftClient.getInstance();
            if (mc.player == null) return 1;

            var held = mc.player.getMainHandStack();
            if (held == null || held.isEmpty()) {
                FishyNotis.notice("You must be holding an item to use this command.");
                return 1;
            }

            var uuid = ItemData.extractUUID(held);
            if (!uuid.isEmpty() && ItemHandler.isProtected(held)) {

                ItemConfig.removeUUID(uuid);
                FishyNotis.format(Text.literal("Your ").formatted(Formatting.GRAY)
                    .append(held.getName())
                    .append(Text.literal(" is no longer protected.").formatted(Formatting.GRAY)));

            } else FishyNotis.notice("Held item isn't protected or doesn't have a UUID.");

            return 1;
        });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> listCmd() {
        return ClientCommandManager.literal("list")
        .executes(ctx -> {
            CmdHelper.sendSortedProtectedList();
            return 1;
        });
    }


    private static LiteralArgumentBuilder<FabricClientCommandSource> clearCmd() {
        return ClientCommandManager.literal("clear")
        .executes(context -> {
            if (!CmdChat.pendingClear) {
                CmdChat.pendingClear = true;
                FishyNotis.send("Are you SURE you want to clear all protected items?");
                CmdHelper.sendClickable("/fa guard confirmclear", "/fa guard cancelclear");
            } else {
                FishyNotis.warn("Please respond to the confirmation prompt for: /fa guard clear.");
            }
            return 1;
        });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> confirmClearCmd() {
        return ClientCommandManager.literal("confirmclear")
        .executes(context -> {
            ItemConfig.clearAll();
            FishyNotis.send("All protected items have been cleared.");
            CmdChat.pendingClear = false;
            return 1;
        });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> cancelClearCmd() {
        return ClientCommandManager.literal("cancelclear")
        .executes(context -> {
            FishyNotis.notice("/fa guard clear was canceled.");
            CmdChat.pendingClear = false;
            return 1;
        });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> helpCmd() {
        return ClientCommandManager.literal("help")
        .executes(context -> {
            FishyNotis.themed("Usage: §bfa/fa guard §8< §7add §8| §7remove §8| §7list §8| §7clear §8>");
            return 1;
        });
    }
}
