package me.valkeea.fishyaddons.command.handler;

import java.util.Set;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import me.valkeea.fishyaddons.api.skyblock.SlayerTables.SlayerType;
import me.valkeea.fishyaddons.command.CmdHelper;
import me.valkeea.fishyaddons.command.CommandBuilderUtils;
import me.valkeea.fishyaddons.command.EnumArgumentType;
import me.valkeea.fishyaddons.command.GatedGreedyString;
import me.valkeea.fishyaddons.feature.qol.NetworkMetrics;
import me.valkeea.fishyaddons.feature.skyblock.WeatherTracker;
import me.valkeea.fishyaddons.feature.waypoints.NpcLocation;
import me.valkeea.fishyaddons.feature.waypoints.NpcWaypoints;
import me.valkeea.fishyaddons.feature.waypoints.TempWaypoint;
import me.valkeea.fishyaddons.tool.PlayerPosition;
import me.valkeea.fishyaddons.tracker.DianaStats;
import me.valkeea.fishyaddons.tracker.SkillTracker;
import me.valkeea.fishyaddons.tracker.SlayerStats;
import me.valkeea.fishyaddons.ui.list.TabbedListScreen.Tab;
import me.valkeea.fishyaddons.util.FishyNotis;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.ui.screen.HudEditScreen;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class FaRoot implements CommandHandler {
    
    @Override
    public String[] getRootNames() {
        return new String[]{"fishyaddons", "fa"};
    }
    
    @Override
    public void register(LiteralArgumentBuilder<FabricClientCommandSource> builder) {
        builder
            .then(cmdCommand())
            .then(cameraCommand())
            .then(chatCommand())
            .then(keyCommand())
            .then(alertCommand())
            .then(lavaCommand())
            .then(hudCommand())
            .then(pingCommand())
            .then(guideCommand())
            .then(helpCommand())
            .then(rainCommand())
            .then(fishCommand())
            .then(coordCommand())
            .then(dianaCommand())
            .then(skillCommand())
            .then(slayerCommand())
            .then(npcCommand());
    }


    private static final String F5 = "Custom F5";
    
    // --- Toggle commands with gui init ---
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> cmdCommand() {
        return CommandBuilderUtils.toggleCommand("cmd", BooleanKey.ALIASES, "Custom commands")
            .withGuiTab(Tab.COMMANDS)
            .build();
    }
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> chatCommand() {
        return CommandBuilderUtils.toggleCommand("chat", BooleanKey.CHAT_REPLACEMENTS, "Chat replacements")
            .withGuiTab(Tab.CHAT)
            .build();
    }
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> keyCommand() {
        return CommandBuilderUtils.toggleCommand("key", BooleanKey.KEY_SHORTCUTS, "Keybinds")
            .withGuiTab(Tab.KEYBINDS)
            .build();
    }
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> alertCommand() {
        return CommandBuilderUtils.toggleCommand("alert", BooleanKey.CHAT_ALERTS, "Chat alerts")
            .withGuiScreen(new me.valkeea.fishyaddons.ui.list.ChatAlerts())
            .build();
    }
    
    // --- Toggle commands ---
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> cameraCommand() {
        return CommandBuilderUtils.toggleCommand("camera", BooleanKey.SKIP_F5, F5)
            .withToggle()
            .withHelpMessage("Usage: §b/fa cam §8<§7 on §8| §7off §8| §7toggle §8>")
            .build();
    }
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> lavaCommand() {
        return CommandBuilderUtils.toggleCommand("lava", BooleanKey.FISHY_LAVA, "Clear Lava")
            .withHelpMessage("Usage: §b/fa lava §8<§7on §8| §7off§8>")
            .build();
    }
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> pingCommand() {
        return CommandBuilderUtils.toggleCommand("ping", BooleanKey.HUD_METRICS_ENABLED, "Network Display")
            .withDefaultAction(() -> {
                NetworkMetrics.send();
                var msg = Component.literal(NetworkMetrics.getPing() + " §8ms");
                if (NetworkMetrics.shouldDisplay(BooleanKey.METRICS_SHOW_TPS)) {
                    msg = msg.copy().append(Component.literal("§8, §7" + NetworkMetrics.getTpsString() + " §8TPS"));
                }
                FishyNotis.send(msg);
                return 1;
            })
            .build();
    }
    
    // --- Simple ---
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> hudCommand() {
        return CommandBuilderUtils.createGuiCommand("hud", new HudEditScreen());
    }
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> guideCommand() {
        return CommandBuilderUtils.createSimpleCommand("guide", () -> {
            if (CmdHelper.checkGUI() == 1) return 1;
            FishyNotis.guideNoti2();
            return 1;
        });
    }
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> helpCommand() {
        return CommandBuilderUtils.createSimpleCommand("help", () -> {
            FishyNotis.helpNoti();
            return 1;
        });
    }

    // --- Complex ---

    private static LiteralArgumentBuilder<FabricClientCommandSource> rainCommand() {
        return literal("rain").then(literal("track")
        .executes(ctx -> {
            boolean isRaining = WeatherTracker.isRaining();
            WeatherTracker.track();
            var status = isRaining ? "It is currently raining. You will be notified when the rain stops." : "It is not currently raining.";
            FishyNotis.format(Component.literal(status).withStyle(isRaining ? ChatFormatting.DARK_AQUA : ChatFormatting.RED));
            return 1;
        }))
        .then(literal("on")
        .executes(ctx -> {
            Config.toggle(BooleanKey.RAIN_NOTI);
            FishyNotis.on("Rain notifications");
            return 1;
        }))
        .then(literal("off")
        .executes(ctx -> {
            Config.toggle(BooleanKey.RAIN_NOTI);
            WeatherTracker.reset();
            FishyNotis.off("Rain notifications");
            return 1;
        }))
        .executes(ctx -> {
            FishyNotis.themed("""
                §lWeather Tracker:
                §3/fa rain track §8- §7Check the current rain state.
                §3/fa rain on | off §8- §7Enable/disable rain notifications""");
            return 1;
        });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> fishCommand() {
        return literal("sc").then(literal("sounds")
        .executes(ctx -> {
            FishyNotis.themed("""
                §3To replace the 3 placeholder sounds:
                §81 §7In any resource pack, create a folder structure: §bfishyaddons/sounds/custom/
                §82 §7Place your .ogg files inside 'custom'
                §83 §7Then name them as: §bfishyaddons_1§7, §bfishyaddons_2§7, §bfishyaddons_3""");
            return 1;
        }))
        .then(literal("since")
        .executes(ctx -> {
            me.valkeea.fishyaddons.tracker.fishing.ScStats.getInstance().sendStats();
            return 1;
        }))
        .then(literal("rng")
        .executes(ctx -> {
            me.valkeea.fishyaddons.tracker.fishing.ScData.getInstance().sendCatchRates();
            return 1;
        }))
        .then(ClientCommands.argument("name", GatedGreedyString.excluding(Set.of("sounds", "since", "rng")))
        .executes(ctx -> {
            var name = StringArgumentType.getString(ctx, "name");
            me.valkeea.fishyaddons.tracker.fishing.ScData.getInstance().sendHistogramSummary(name);
            return 1;
        }))
        .executes(ctx -> {
            FishyNotis.themed("""
                §lUsage:"
                §3/fa fishyaddons sc sounds §8- §7Instructions to replace placeholder sounds
                §3/fa fishyaddons sc since §8- §7Stats for 'sc since x' in the current island
                §3/fa fishyaddons sc rng §8- §7Catch % for all rare scs
                §3/fa fishyaddons sc <name> §8- §7Data summary for a specific sc""");
            return 1;
        });
    }

    private static final String TOGGLE = "toggle";
    private static final String RESET = "reset";
    private static final String LABEL = "label";


    protected static LiteralArgumentBuilder<FabricClientCommandSource> coordCommand() {
        return literal("coords").then(ClientCommands.argument(LABEL, GatedGreedyString.excluding(Set.of("last", "hide", "redraw")))
        .executes(ctx -> {
            var label = ctx.getArgument(LABEL, String.class);
            PlayerPosition.giveAwayCoordsWithLabel(label);
            return 1;
        }))
        .then(literal("last")
        .executes(ctx -> {
            TempWaypoint.redrawLast();
            return 1;
        }))
        .then(literal("hide")
        .then(ClientCommands.argument("x", IntegerArgumentType.integer())
        .then(ClientCommands.argument("y", IntegerArgumentType.integer())
        .then(ClientCommands.argument("z", IntegerArgumentType.integer())
        .executes(ctx -> {
            int x = ctx.getArgument("x", Integer.class);
            int y = ctx.getArgument("y", Integer.class);
            int z = ctx.getArgument("z", Integer.class);
            var pos = new net.minecraft.core.BlockPos(x, y, z);
            TempWaypoint.removeBeaconAt(pos);
            return 1;
        })))))
        .then(literal("redraw")
        .then(ClientCommands.argument("x", IntegerArgumentType.integer())
        .then(ClientCommands.argument("y", IntegerArgumentType.integer())
        .then(ClientCommands.argument("z", IntegerArgumentType.integer())
        .executes(ctx -> {
            int x = ctx.getArgument("x", Integer.class);
            int y = ctx.getArgument("y", Integer.class);
            int z = ctx.getArgument("z", Integer.class);
            var pos = new net.minecraft.core.BlockPos(x, y, z);
            TempWaypoint.redraw(pos, "");
            return 1;
        })
        .then(ClientCommands.argument(LABEL, StringArgumentType.greedyString())
        .executes(ctx -> {
            int x = ctx.getArgument("x", Integer.class);
            int y = ctx.getArgument("y", Integer.class);
            int z = ctx.getArgument("z", Integer.class);
            var label = ctx.getArgument(LABEL, String.class);
            var pos = new net.minecraft.core.BlockPos(x, y, z);
            TempWaypoint.redraw(pos, label);
            return 1;
        }))))))
        .executes(ctx -> {
            PlayerPosition.giveAwayCoords();
            return 1;
        });
    }    

    private static LiteralArgumentBuilder<FabricClientCommandSource> dianaCommand() {
        return literal("diana").then(literal(RESET)
        .executes(ctx -> {
            if (DianaStats.loaded()) {
                DianaStats.getInstance().resetAll();
                FishyNotis.send("Diana stats have been reset.");
            } else  FishyNotis.warn("Diana stats are not loaded.");
            return 1;
        }))
        .executes(ctx -> {
            DianaStats.getInstance().sendDianaStats();
            return 1;
        });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> skillCommand() {
        return literal("skill").then(literal("dt")
        .executes(ctx -> {
            SkillTracker.getInstance().toggleDownTime();
            return 1;
        }))
        .then(literal(RESET)
        .executes(ctx -> {
            SkillTracker.getInstance().resetAll();
            FishyNotis.notice("Skill Tracker has been reset.");
            return 1;
        }))
        .then(literal(TOGGLE)
        .executes(ctx -> {
            boolean current = Config.get(BooleanKey.HUD_SKILL_XP);
            Config.toggle(BooleanKey.HUD_SKILL_XP);
            SkillTracker.refresh();

            if (!current) {
                FishyNotis.on("Skill Tracker");
            } else FishyNotis.off("Skill Tracker");

            return 1;
        }))
        .executes(ctx -> {
            FishyNotis.themed("""
                "Usage:"
                §3/fa skill dt §8- §7Toggle downtime mode. Otherwise, skill XP tracking is paused after 1.5min and wiped after 15min.
                §3/fa skill reset §8- §7Reset all tracked XP for the session.""");
            return 1;
        });
    }    

    private static LiteralArgumentBuilder<FabricClientCommandSource> slayerCommand() {
        return literal("slayer").then(literal(RESET).then(literal("all").then(literal("confirm")
        .executes(ctx -> {
            if (SlayerStats.loaded()) {
                SlayerStats.getInstance().resetAll();
                FishyNotis.send("All slayer stats have been reset.");
            } else  FishyNotis.warn("Slayer stats are not loaded.");
            return 1;
        }))
        .then(literal("cancel")
        .executes(ctx -> {
            FishyNotis.notice("Slayer stats reset was canceled.");
            return 1;
        }))
        .executes(ctx -> {
            FishyNotis.themed("Are you SURE you want to reset ALL slayer stats?");
            CmdHelper.sendClickable("/fa slayer reset all confirm", "/fa slayer reset all cancel");
            return 1;
        }))
        .then(ClientCommands.argument("type", EnumArgumentType.of(SlayerType.class))
        .executes(ctx -> {
            var type = ctx.getArgument("type", SlayerType.class);
            if (SlayerStats.loaded()) {
                SlayerStats.getInstance().resetType(type);
                FishyNotis.send(type.getCmdName() + " slayer stats have been reset.");
            } else FishyNotis.warn("Slayer stats are not loaded.");
            return 1;
        })))
        .then(ClientCommands.argument("type", EnumArgumentType.of(SlayerType.class))
        .executes(ctx -> {
            var type = ctx.getArgument("type", SlayerType.class);
            SlayerStats.getInstance().sendSlayerStats(type);
            return 1;
        }))
        .executes(ctx -> {
            SlayerStats.getInstance().sendSlayerStats();
            return 1;
        });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> npcCommand() {
        return literal("npc")
        .then(ClientCommands.argument("name", GatedGreedyString.excluding(Set.of("all", "clear", "save", "scan")))
        .executes(ctx -> {
            var npcName = StringArgumentType.getString(ctx, "name");
            NpcLocation.drawFor(npcName);
            return 1;
        }))
        .then(literal("all")
        .executes(ctx -> {
            NpcLocation.drawAll();
            return 1;
        }))
        .then(literal("clear")
        .executes(ctx -> {
            NpcWaypoints.clearBeacons();
            return 1;
        }))
        .executes(ctx -> {
            FishyNotis.themed("Usage: §b/fa npc §8<§7name §8| §7all §8| §7clear§8>");
            return 1;
        });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> literal(String name) {
        return ClientCommands.literal(name);
    }
}
