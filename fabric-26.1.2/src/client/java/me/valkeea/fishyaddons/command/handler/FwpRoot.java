package me.valkeea.fishyaddons.command.handler;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import me.valkeea.fishyaddons.feature.waypoints.WaypointCmd;
import me.valkeea.fishyaddons.util.FishyNotis;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public class FwpRoot implements CommandHandler {
    
    @Override
    public String[] getRootNames() {
        return new String[]{"waypoint", "fwp"};
    }
    
    @Override
    public void register(LiteralArgumentBuilder<FabricClientCommandSource> builder) {
        builder
            .then(addCmd())
            .then(addNextCmd())
            .then(removeCmd())
            .then(listCmd())
            .then(infoCmd())
            .then(renameCmd())
            .then(clearCmd())
            .then(resetCmd())
            .then(colorCmd())
            .then(toggleCmd())
            .then(setCmd())
            .executes(context -> {
                FishyNotis.fwp();
                return 1;
            });            
    }
    
    private static final String NAME = "chainName";
    private static final String ORDER = "orderNumber";

    private static LiteralArgumentBuilder<FabricClientCommandSource> addCmd() {
        return ClientCommands.literal("add")
        .then(ClientCommands.argument(NAME, StringArgumentType.word())
        .then(ClientCommands.argument(ORDER, IntegerArgumentType.integer())
        .executes(ctx -> {
            String chainName = StringArgumentType.getString(ctx, NAME);
            int orderNumber = IntegerArgumentType.getInteger(ctx, ORDER);
            return WaypointCmd.addWaypoint(chainName, orderNumber);
        })));
    }
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> addNextCmd() {
        return ClientCommands.literal("addnext")
        .executes(ctx -> WaypointCmd.addNext());
    }
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> removeCmd() {
        return ClientCommands.literal("remove")
        .then(ClientCommands.argument(NAME, StringArgumentType.word())
        .then(ClientCommands.argument(ORDER, IntegerArgumentType.integer())
        .executes(ctx -> {
            String chainName = StringArgumentType.getString(ctx, NAME);
            int orderNumber = IntegerArgumentType.getInteger(ctx, ORDER);
            return WaypointCmd.removeWaypoint(chainName, orderNumber);
        })));
    }
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> listCmd() {
        return ClientCommands.literal("list")
        .executes(ctx -> WaypointCmd.listWaypoints());
    }
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> infoCmd() {
        return ClientCommands.literal("info")
        .then(ClientCommands.argument(NAME, StringArgumentType.word())
        .executes(ctx -> {
            String chainName = StringArgumentType.getString(ctx, NAME);
            return WaypointCmd.showChainInfo(chainName);
        }));
    }
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> renameCmd() {
        return ClientCommands.literal("rename")
        .then(ClientCommands.argument("oldName", StringArgumentType.word())
        .then(ClientCommands.argument("newName", StringArgumentType.word())
        .executes(ctx -> {
            String oldName = StringArgumentType.getString(ctx, "oldName");
            String newName = StringArgumentType.getString(ctx, "newName");
            return WaypointCmd.renameChain(oldName, newName);
        })));
    }
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> clearCmd() {
        return ClientCommands.literal("clear")
        .then(ClientCommands.argument(NAME, StringArgumentType.word())
        .executes(ctx -> {
            String chainName = StringArgumentType.getString(ctx, NAME);
            return WaypointCmd.clearChain(chainName);
        }));
    }
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> resetCmd() {
        return ClientCommands.literal("reset")
        .then(ClientCommands.argument(NAME, StringArgumentType.word())
        .executes(ctx -> {
            String chainName = StringArgumentType.getString(ctx, NAME);
            return WaypointCmd.resetChain(chainName);
        }));
    }
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> colorCmd() {
        return ClientCommands.literal("color")
        .then(ClientCommands.argument(NAME, StringArgumentType.word())
        .executes(ctx -> {
            String chainName = StringArgumentType.getString(ctx, NAME);
            return WaypointCmd.openColorPicker(chainName);
        }));
    }
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> toggleCmd() {
        return ClientCommands.literal("toggle")
        .then(ClientCommands.argument(NAME, StringArgumentType.word())
        .executes(ctx -> {
            String chainName = StringArgumentType.getString(ctx, NAME);
            return WaypointCmd.toggleChainVisibility(chainName);
        }))
        .executes(ctx -> WaypointCmd.toggle());
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> setCmd() {
        return ClientCommands.literal("set")
        .then(ClientCommands.argument(NAME, StringArgumentType.word())
        .executes(ctx -> {
            String chainName = StringArgumentType.getString(ctx, NAME);
            return WaypointCmd.setLastModifiedChain(chainName);
        }));
    }
}
