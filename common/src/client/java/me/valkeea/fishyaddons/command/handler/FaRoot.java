package me.valkeea.fishyaddons.command.handler;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import me.valkeea.fishyaddons.api.skyblock.SlayerTables.SlayerType;
import me.valkeea.fishyaddons.command.CmdHelper;
import me.valkeea.fishyaddons.command.CommandBuilderUtils;
import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.feature.qol.ChatAlert;
import me.valkeea.fishyaddons.feature.qol.ChatReplacement;
import me.valkeea.fishyaddons.feature.qol.CommandAlias;
import me.valkeea.fishyaddons.feature.qol.KeyShortcut;
import me.valkeea.fishyaddons.feature.qol.NetworkMetrics;
import me.valkeea.fishyaddons.feature.skyblock.WeatherTracker;
import me.valkeea.fishyaddons.feature.visual.RenderTweaks;
import me.valkeea.fishyaddons.feature.waypoints.TempWaypoint;
import me.valkeea.fishyaddons.tool.PlayerPosition;
import me.valkeea.fishyaddons.tracker.SkillTracker;
import me.valkeea.fishyaddons.tracker.SlayerStats;
import me.valkeea.fishyaddons.ui.HudEditScreen;
import me.valkeea.fishyaddons.ui.list.TabbedListScreen;
import me.valkeea.fishyaddons.util.FishyNotis;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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
            .then(slayerCommand());
    }


    private static final String F5 = "Custom F5";
    
    // --- Toggle commands with gui init ---
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> cmdCommand() {
        return CommandBuilderUtils.toggleCommand("cmd", Key.ALIASES_ENABLED, "Custom commands")
            .onToggle(CommandAlias::refresh)
            .withGuiTab(TabbedListScreen.Tab.COMMANDS)
            .build();
    }
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> chatCommand() {
        return CommandBuilderUtils.toggleCommand("chat", Key.CHAT_REPLACEMENTS_ENABLED, "Chat replacements")
            .onToggle(ChatReplacement::refresh)
            .withGuiTab(TabbedListScreen.Tab.CHAT)
            .build();
    }
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> keyCommand() {
        return CommandBuilderUtils.toggleCommand("key", Key.KEY_SHORTCUTS_ENABLED, "Keybinds")
            .onToggle(KeyShortcut::refresh)
            .withGuiTab(TabbedListScreen.Tab.KEYBINDS)
            .build();
    }
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> alertCommand() {
        return CommandBuilderUtils.toggleCommand("alert", Key.CHAT_ALERTS_ENABLED, "Chat alerts")
            .onToggle(ChatAlert::refresh)
            .withGuiScreen(new me.valkeea.fishyaddons.ui.list.ChatAlerts(null))
            .build();
    }
    
    // --- Toggle commands ---
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> cameraCommand() {
        return CommandBuilderUtils.toggleCommand("camera", Key.SKIP_F5, F5)
            .withToggle()
            .withHelpMessage("Usage: §b/fa cam §8<§7 on §8| §7off §8| §7toggle §8>")
            .build();
    }
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> lavaCommand() {
        return CommandBuilderUtils.toggleCommand("lava", Key.FISHY_LAVA, "Clear Lava")
            .onToggle(RenderTweaks::refresh)
            .withHelpMessage("Usage: §b/fa lava §8<§7on §8| §7off§8>")
            .build();
    }
    
    private static LiteralArgumentBuilder<FabricClientCommandSource> pingCommand() {
        return CommandBuilderUtils.toggleCommand("ping", Key.HUD_PING_ENABLED, "Network Display")
            .onToggle(NetworkMetrics::refresh)
            .withDefaultAction(() -> {
                NetworkMetrics.send();
                var msg = Text.literal(NetworkMetrics.getPing() + " §8ms");
                if (NetworkMetrics.shouldDisplay(Key.HUD_PING_SHOW_TPS)) {
                    msg = msg.copy().append(Text.literal("§8, §7" + NetworkMetrics.getTpsString() + " §8TPS"));
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
        return ClientCommandManager.literal("rain")
        .then(ClientCommandManager.literal("track")
        .executes(context -> {
            boolean isRaining = WeatherTracker.isRaining();
            WeatherTracker.track();
            String status = isRaining ? "It is currently raining. You will be notified when the rain stops." : "It is not currently raining.";
            FishyNotis.format(Text.literal(status).formatted(isRaining ? Formatting.DARK_AQUA : Formatting.RED));
            return 1;
        }))
        .then(ClientCommandManager.literal("on")
        .executes(context -> {
            FishyConfig.toggle(Key.RAIN_NOTI, true);
            FishyNotis.on("Rain notifications");
            return 1;
        }))
        .then(ClientCommandManager.literal("off")
        .executes(context -> {
            FishyConfig.toggle(Key.RAIN_NOTI, false);
            me.valkeea.fishyaddons.feature.skyblock.WeatherTracker.reset();
            FishyNotis.off("Rain notifications");
            return 1;
        }))
        .executes(context -> {
            FishyNotis.themed("§lWeather Tracker:");
            FishyNotis.alert(Text.literal("§3/fa rain track §8- §7Check the current rain state."));
            FishyNotis.alert(Text.literal("§3/fa rain on | off §8- §7Enable/disable rain notifications"));
            return 1;
        });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> fishCommand() {
        return ClientCommandManager.literal("sc")
        .then(ClientCommandManager.literal("sounds")
        .executes(context -> {
            FishyNotis.send("§aTo create a resource pack:");
            FishyNotis.alert(Text.literal("§7- In any resource pack, create a folder named: §bfishyaddons"));
            FishyNotis.alert(Text.literal("§7- Inside that folder, create another folder named: §bsounds"));
            FishyNotis.alert(Text.literal("§7- Inside the sounds folder, create a folder named: §bcustom"));
            FishyNotis.alert(Text.literal("§7- Place your custom .ogg files inside the custom folder"));
            FishyNotis.alert(Text.literal("§7- Then use sound IDs: §bfishyaddons:fishyaddons_1§7, §bfishyaddons:fishyaddons_2§7, §bfishyaddons:fishyaddons_3"));
            return 1;
        }))
        .then(ClientCommandManager.literal("since")
        .executes(context -> {
            me.valkeea.fishyaddons.tracker.fishing.ScStats.getInstance().sendStats();
            return 1;
        }))
        .then(ClientCommandManager.literal("rng")
        .executes(context -> {
            me.valkeea.fishyaddons.tracker.fishing.ScData.getInstance().sendCatchRates();
            return 1;
        }))
        .then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
        .executes(context -> {
            var name = StringArgumentType.getString(context, "name");
            me.valkeea.fishyaddons.tracker.fishing.ScData.getInstance().sendHistogramSummary(name);
            return 1;
        }))
        .executes(context -> {
            FishyNotis.themed("Usage:");
            FishyNotis.alert(Text.literal("§3/fa fishyaddons sc sounds §8- §7Instructions for resource pack sounds"));
            FishyNotis.alert(Text.literal("§3/fa fishyaddons sc since §8- §7Stats for 'sc since' in the current island"));          
            FishyNotis.alert(Text.literal("§3/fa fishyaddons sc rng §8- §7Catch % for all rare scs"));
            FishyNotis.alert(Text.literal("§3/fa fishyaddons sc <name> §8- §7Data summary for a specific sc"));
            return 1;
        });
    }

    private static final String TOGGLE = "toggle";
    private static final String RESET = "reset";
    private static final String LABEL = "label";


    protected static LiteralArgumentBuilder<FabricClientCommandSource> coordCommand() {
        return ClientCommandManager.literal("coords")
        .then(ClientCommandManager.argument(LABEL, StringArgumentType.greedyString())
        .executes(context -> {
            String label = context.getArgument(LABEL, String.class);
            PlayerPosition.giveAwayCoordsWithLabel(label);
            return 1;
        }))
        .then(ClientCommandManager.literal("last")
        .executes(context -> {
            TempWaypoint.redrawLast();
            return 1;
        }))
        .then(ClientCommandManager.literal("hide")
        .then(ClientCommandManager.argument("x", IntegerArgumentType.integer())
        .then(ClientCommandManager.argument("y", IntegerArgumentType.integer())
        .then(ClientCommandManager.argument("z", IntegerArgumentType.integer())
        .executes(context -> {
            int x = context.getArgument("x", Integer.class);
            int y = context.getArgument("y", Integer.class);
            int z = context.getArgument("z", Integer.class);
            var pos = new net.minecraft.util.math.BlockPos(x, y, z);
            TempWaypoint.removeBeaconAt(pos);
            return 1;
        })))))
        .then(ClientCommandManager.literal("redraw")
        .then(ClientCommandManager.argument("x", IntegerArgumentType.integer())
        .then(ClientCommandManager.argument("y", IntegerArgumentType.integer())
        .then(ClientCommandManager.argument("z", IntegerArgumentType.integer())
        .executes(context -> {
            int x = context.getArgument("x", Integer.class);
            int y = context.getArgument("y", Integer.class);
            int z = context.getArgument("z", Integer.class);
            var pos = new net.minecraft.util.math.BlockPos(x, y, z);
            TempWaypoint.redraw(pos, "");
            return 1;
        })
        .then(ClientCommandManager.argument(LABEL, StringArgumentType.greedyString())
        .executes(context -> {
            int x = context.getArgument("x", Integer.class);
            int y = context.getArgument("y", Integer.class);
            int z = context.getArgument("z", Integer.class);
            String label = context.getArgument(LABEL, String.class);
            var pos = new net.minecraft.util.math.BlockPos(x, y, z);
            TempWaypoint.redraw(pos, label);
            return 1;
        }))))))
        .executes(context -> {
            PlayerPosition.giveAwayCoords();
            return 1;
        });
    }    

    private static LiteralArgumentBuilder<FabricClientCommandSource> dianaCommand() {
        return ClientCommandManager.literal("diana")
        .then(ClientCommandManager.literal(RESET)
        .executes(context -> {
            if (me.valkeea.fishyaddons.tracker.DianaStats.loaded()) {
                me.valkeea.fishyaddons.tracker.DianaStats.getInstance().resetAll();
                FishyNotis.send("Diana stats have been reset.");
            } else  FishyNotis.warn("Diana stats are not loaded.");
            return 1;
        }))
        .executes(context -> {
            me.valkeea.fishyaddons.tracker.DianaStats.getInstance().sendDianaStats();
            return 1;
        });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> skillCommand() {
        return ClientCommandManager.literal("skill")
        .then(ClientCommandManager.literal("dt")
        .executes(context -> {
            SkillTracker.getInstance().toggleDownTime();
            return 1;
        }))
        .then(ClientCommandManager.literal(RESET)
        .executes(context -> {
            SkillTracker.getInstance().resetAll();
            FishyNotis.notice("Skill Tracker has been reset.");
            return 1;
        }))
        .then(ClientCommandManager.literal(TOGGLE)
        .executes(context -> {
            boolean current = FishyConfig.getState(Key.HUD_SKILL_XP_ENABLED, false);
            FishyConfig.toggle(Key.HUD_SKILL_XP_ENABLED, !current);
            SkillTracker.refresh();

            if (!current) {
                FishyNotis.on("Skill Tracker");
            } else FishyNotis.off("Skill Tracker");

            return 1;
        }))
        .executes(context -> {
            FishyNotis.themed("Usage:");
            FishyNotis.alert(Text.literal("§3/fa skill dt §8- §7Toggle downtime mode. Otherwise, skill XP tracking is paused after 1.5min and wiped after 15min."));
            FishyNotis.alert(Text.literal("§3/fa skill reset §8- §7Reset all tracked XP for the session."));
            return 1;
        });
    }    

    private static LiteralArgumentBuilder<FabricClientCommandSource> slayerCommand() {
        return ClientCommandManager.literal("slayer")
            .then(ClientCommandManager.literal(RESET)
                .then(ClientCommandManager.literal("all")
                    .then(ClientCommandManager.literal("confirm")
                        .executes(context -> {
                            if (SlayerStats.loaded()) {
                                SlayerStats.getInstance().resetAll();
                                FishyNotis.send("All slayer stats have been reset.");
                            } else  FishyNotis.warn("Slayer stats are not loaded.");
                            return 1;
                        }))
                    .then(ClientCommandManager.literal("cancel")
                        .executes(context -> {
                            FishyNotis.notice("Slayer stats reset was canceled.");
                            return 1;
                        }))
                    .executes(context -> {
                        FishyNotis.themed("Are you SURE you want to reset ALL slayer stats?");
                        CmdHelper.sendClickable("/fa slayer reset all confirm", "/fa slayer reset all cancel");
                        return 1;
                    }))
                .then(ClientCommandManager.argument("type", StringArgumentType.word())
                    .executes(context -> {
                        var typeName = StringArgumentType.getString(context, "type");
                        var type = parseSlayerType(typeName);
                        if (type != null && SlayerStats.loaded()) {
                            SlayerStats.getInstance().resetType(type);
                            FishyNotis.send(type.getCmdName() + " slayer stats have been reset.");
                        } else if (type == null) {
                            FishyNotis.warn("Invalid slayer type. Use: wolf, zombie, spider, enderman, blaze, or vampire");
                        } else FishyNotis.warn("Slayer stats are not loaded.");
                        return 1;
                    })))
            .then(ClientCommandManager.argument("type", StringArgumentType.word())
                .executes(context -> {
                    var typeName = StringArgumentType.getString(context, "type");
                    var type = parseSlayerType(typeName);
                    if (type != null) {
                        SlayerStats.getInstance().sendSlayerStats(type);
                    } else FishyNotis.warn("Invalid slayer type. Use: wolf, zombie, spider, enderman, blaze, or vampire");
                    return 1;
                }))
            .executes(context -> {
                SlayerStats.getInstance().sendSlayerStats();
                return 1;
            });
    }
    
    private static SlayerType parseSlayerType(String name) {
        try {
            return SlayerType.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
