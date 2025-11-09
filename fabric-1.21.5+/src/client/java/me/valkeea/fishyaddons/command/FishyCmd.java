package me.valkeea.fishyaddons.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.ItemConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.feature.item.safeguard.ItemHandler;
import me.valkeea.fishyaddons.feature.qol.ChatAlert;
import me.valkeea.fishyaddons.feature.qol.ChatReplacement;
import me.valkeea.fishyaddons.feature.qol.CommandAlias;
import me.valkeea.fishyaddons.feature.qol.KeyShortcut;
import me.valkeea.fishyaddons.feature.qol.NetworkMetrics;
import me.valkeea.fishyaddons.feature.visual.RenderTweaks;
import me.valkeea.fishyaddons.feature.waypoints.TempWaypoint;
import me.valkeea.fishyaddons.feature.waypoints.WaypointCmd;
import me.valkeea.fishyaddons.tool.GuiScheduler;
import me.valkeea.fishyaddons.tool.PlayerPosition;
import me.valkeea.fishyaddons.ui.HudEditScreen;
import me.valkeea.fishyaddons.ui.list.ChatAlerts;
import me.valkeea.fishyaddons.ui.list.TabbedListScreen;
import me.valkeea.fishyaddons.util.FishyNotis;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class FishyCmd {
    private static final String PROFILE = "profile";
    private static final String TOGGLE = "toggle";
    private static final String CLEAR = "clear";
    private static final String NEW_NAME = "newName";

    protected static LiteralArgumentBuilder<FabricClientCommandSource> registerCmd() {
        return ClientCommandManager.literal("cmd")
                .then(ClientCommandManager.literal("on")
                    .executes(context -> {
                        FishyConfig.enable(Key.ALIASES_ENABLED, true);
                        FishyNotis.on("Custom commands");
                        CommandAlias.refresh();
                        return 1;
                    }))
                .then(ClientCommandManager.literal("off")
                    .executes(context -> {
                        FishyConfig.disable(Key.ALIASES_ENABLED);
                        FishyNotis.off("Custom commands");
                        CommandAlias.refresh();
                        return 1;
                    }))
            .executes(context -> {
                if (checkGUI() == 1) return 1;
                MinecraftClient.getInstance().execute(() ->
                    GuiScheduler.scheduleGui(new TabbedListScreen(MinecraftClient.getInstance().currentScreen, TabbedListScreen.Tab.COMMANDS))
                );
                return 1;
            });     
    }    

    protected static LiteralArgumentBuilder<FabricClientCommandSource> registerChat() {
        return ClientCommandManager.literal("chat")
                .then(ClientCommandManager.literal("on")
                    .executes(context -> {
                        FishyConfig.enable(Key.CHAT_REPLACEMENTS_ENABLED, true);
                        FishyNotis.on("Chat replacements");
                        ChatReplacement.refresh();
                        return 1;
                    }))
                .then(ClientCommandManager.literal("off")
                    .executes(context -> {
                        FishyConfig.disable(Key.CHAT_REPLACEMENTS_ENABLED);
                        FishyNotis.off("Chat replacements");
                        ChatReplacement.refresh();
                        return 1;
                    }))
                .executes(context -> {
                    if (checkGUI() == 1) return 1;
                    MinecraftClient.getInstance().execute(() ->
                        GuiScheduler.scheduleGui(new TabbedListScreen(MinecraftClient.getInstance().currentScreen, TabbedListScreen.Tab.CHAT))
                    );
                    return 1;
                });
    }
    
    protected static LiteralArgumentBuilder<FabricClientCommandSource> registerKey() {
        return ClientCommandManager.literal("key")
                .then(ClientCommandManager.literal("on")
                    .executes(context -> {
                        FishyConfig.enable(Key.KEY_SHORTCUTS_ENABLED, true);
                        FishyNotis.on("Keybinds");
                        KeyShortcut.refresh();
                        return 1;
                    }))
                .then(ClientCommandManager.literal("off")
                    .executes(context -> {
                        FishyConfig.disable(Key.KEY_SHORTCUTS_ENABLED);
                        FishyNotis.off("Keybinds");
                        KeyShortcut.refresh();
                        return 1;
                    }))
                .executes(context -> {
                    if (checkGUI() == 1) return 1;
                    MinecraftClient.getInstance().execute(() ->
                        GuiScheduler.scheduleGui(new TabbedListScreen(MinecraftClient.getInstance().currentScreen, TabbedListScreen.Tab.KEYBINDS))
                    );
                    return 1;
                });
    }
    
    protected static LiteralArgumentBuilder<FabricClientCommandSource> registerAlert() {
        return ClientCommandManager.literal("alert")
                .then(ClientCommandManager.literal("on")
                    .executes(context -> {
                        FishyConfig.enable(Key.CHAT_ALERTS_ENABLED, true);
                        FishyNotis.on("Chat alerts");
                        ChatAlert.refresh();
                        return 1;
                    }))
                .then(ClientCommandManager.literal("off")
                    .executes(context -> {
                        FishyConfig.disable(Key.CHAT_ALERTS_ENABLED);
                        FishyNotis.off("Chat alerts");
                        ChatAlert.refresh();
                        return 1;
                    }))
                .executes(context -> {
                    if (checkGUI() == 1) return 1;
                    MinecraftClient.getInstance().execute(() ->
                        GuiScheduler.scheduleGui(new ChatAlerts(null))
                    );
                    return 1;
                });
    }

    protected static LiteralArgumentBuilder<FabricClientCommandSource> registerGuide() {
        return ClientCommandManager.literal("guide")
            .executes(context -> {
                if (checkGUI() == 1) return 1;
                FishyNotis.guideNoti2();
                return 1;
            });
    }

    protected static LiteralArgumentBuilder<FabricClientCommandSource> registerHelp() {
        return ClientCommandManager.literal("help")
            .executes(context -> {
                FishyNotis.helpNoti();
                return 1;
            });
    }
    
    public static void addGuardSubcommands(LiteralArgumentBuilder<FabricClientCommandSource> root) {
        root.then(ClientCommandManager.literal("add")
                .executes(context -> {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc.player == null || mc.world == null) return 1;

                    ItemStack held = mc.player.getMainHandStack();
                    if (held == null || held.isEmpty()) {
                        FishyNotis.notice("You must be holding an item to use this command.");
                        return 1;
                    }

                    String uuid = ItemHandler.extractUUID(held);
                    if (uuid.isEmpty()) {
                        FishyNotis.warn("Held item doesn't have a UUID.");
                        return 1;
                    }
                    Text name = held.getName();
                    ItemConfig.addUUID(uuid, name);

                    FishyNotis.format(Text.literal("Your ").formatted(Formatting.GRAY)
                        .append(name)
                        .append(Text.literal(" is now protected.").formatted(Formatting.GRAY)));

                    return 1;
                }))
            .then(ClientCommandManager.literal("remove")
                .executes(context -> {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc.player == null) return 1;
                    ItemStack held = mc.player.getMainHandStack();
                    if (held == null || held.isEmpty()) {
                        FishyNotis.notice("You must be holding an item to use this command.");
                        return 1;
                    }
                    String uuid = ItemHandler.extractUUID(held);
                    if (!uuid.isEmpty() && ItemHandler.isProtected(held)) {
                        ItemConfig.removeUUID(uuid);
                        FishyNotis.format(Text.literal("Your ").formatted(Formatting.GRAY)
                            .append(held.getName())
                            .append(Text.literal(" is no longer protected.").formatted(Formatting.GRAY)));
                    } else {
                        FishyNotis.notice("Held item isn't protected or doesn't have a UUID.");
                    }
                    return 1;
                }))
            .then(ClientCommandManager.literal("list")
                .executes(context -> {
                    CmdHelper.sendSortedProtectedList();
                    return 1;
                }))
            .then(ClientCommandManager.literal(CLEAR)
                .executes(context -> {
                    if (!CmdChat.pendingClear) {
                        CmdChat.pendingClear = true;
                        FishyNotis.send("Are you SURE you want to clear all protected items?");
                        sendClickable("/fa guard confirmclear", "/fa guard cancelclear");
                    } else {
                        FishyNotis.warn("Please respond to the confirmation prompt for: /fa guard clear.");
                    }
                    return 1;
                }))
            .then(ClientCommandManager.literal("confirmclear")
                .executes(context -> {
                    ItemConfig.clearAll();
                    FishyNotis.send("All protected items have been cleared.");
                    CmdChat.pendingClear = false;
                    return 1;
                }))
            .then(ClientCommandManager.literal("cancelclear")
                .executes(context -> {
                    FishyNotis.notice("/fa guard clear was canceled.");
                    CmdChat.pendingClear = false;
                    return 1;
                }))
            .then(ClientCommandManager.literal("help")
                .executes(context -> {
                    FishyNotis.themed("Usage: §bfa/fa guard §8< §7add §8| §7remove §8| §7list §8| §7clear §8>");
                    return 1;
                }));
    }   



    protected static void addProfitSubcommands(LiteralArgumentBuilder<FabricClientCommandSource> root) {
        String reName = "rename";
        String restore = "restore";
        String price = "price";

        root.then(ClientCommandManager.literal(TOGGLE)
                .executes(context -> {
                    TrackerCmd.handle(new String[]{TOGGLE});
                    return 1;
                }))
            .then(ClientCommandManager.literal("stats")
                .executes(context -> {
                    TrackerCmd.handle(new String[]{"stats"});
                    return 1;
                }))
            .then(ClientCommandManager.literal("init")
                .executes(context -> {
                    TrackerCmd.handle(new String[]{"init"});
                    return 1;
                }))
            .then(ClientCommandManager.literal("refresh")
                .executes(context -> {
                    TrackerCmd.handle(new String[]{"refresh"});
                    return 1;
                }))
            .then(ClientCommandManager.literal("status")
                .executes(context -> {
                    TrackerCmd.handle(new String[]{"status"});
                    return 1;
                }))
            .then(ClientCommandManager.literal(CLEAR)
                .executes(context -> {
                    TrackerCmd.handle(new String[]{CLEAR});
                    return 1;
                }))
            .then(ClientCommandManager.literal("ignored")
                .executes(context -> {
                    TrackerCmd.handle(new String[]{"ignored"});
                    return 1;
                }))
            .then(ClientCommandManager.literal(restore)
                .then(ClientCommandManager.literal("all")
                    .executes(context -> {
                        TrackerCmd.handle(new String[]{restore, "all"});
                        return 1;
                    }))
                .then(ClientCommandManager.argument("item", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                    .executes(context -> {
                        String itemName = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "item");
                        TrackerCmd.handle(new String[]{restore, itemName});
                        return 1;
                    }))
                .executes(context -> {
                    TrackerCmd.handle(new String[]{restore});
                    return 1;
                }))
            .then(ClientCommandManager.literal("type")
                .then(ClientCommandManager.literal("insta_sell")
                    .executes(context -> {
                        TrackerCmd.handle(new String[]{"type", "insta_sell"});
                        return 1;
                    }))
                .then(ClientCommandManager.literal("sell_offer")
                    .executes(context -> {
                        TrackerCmd.handle(new String[]{"type", "sell_offer"});
                        return 1;
                    }))
                .executes(context -> {
                    TrackerCmd.handle(new String[]{"type"});
                    return 1;
                }))
            .then(ClientCommandManager.literal(price)
                .then(ClientCommandManager.argument("item", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                    .executes(context -> {
                        String itemName = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "item");
                        TrackerCmd.handle(new String[]{price, itemName});
                        return 1;
                    }))
                .executes(context -> {
                    TrackerCmd.handle(new String[]{price});
                    return 1;
                }))
            .then(ClientCommandManager.literal(PROFILE)
                .then(ClientCommandManager.argument("name", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                    .executes(context -> {
                        String profileName = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "name");
                        TrackerCmd.handle(new String[]{PROFILE, profileName});
                        return 1;
                    }))
                .then(ClientCommandManager.literal("delete")
                    .then(ClientCommandManager.argument(PROFILE, com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                        .executes(context -> {
                            String profileName = com.mojang.brigadier.arguments.StringArgumentType.getString(context, PROFILE);
                            TrackerCmd.handle(new String[]{PROFILE, "delete", profileName});
                            return 1;
                        })))
                .then(ClientCommandManager.literal(reName)
                    .then(ClientCommandManager.argument("oldName", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .then(ClientCommandManager.argument(NEW_NAME, com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                            .executes(context -> {
                                String oldName = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "oldName");
                                String newName = com.mojang.brigadier.arguments.StringArgumentType.getString(context, NEW_NAME);
                                TrackerCmd.handle(new String[]{PROFILE, reName, oldName, newName});
                                return 1;
                            }))))
                .executes(context -> {
                    TrackerCmd.handle(new String[]{PROFILE});
                    return 1;
                }))
            .executes(context -> {
                TrackerCmd.handle(new String[]{});
                return 1;
            });
    }
    
    protected static LiteralArgumentBuilder<FabricClientCommandSource> registerLava() {
        return ClientCommandManager.literal("lava")
                .then(ClientCommandManager.literal("on")
                    .executes(context -> {
                        FishyConfig.settings.set(Key.FISHY_LAVA, true);
                        FishyNotis.on("Clear Lava");
                        RenderTweaks.refresh();
                        return 1;
                    }))
                .then(ClientCommandManager.literal("off")
                    .executes(context -> {
                        FishyConfig.settings.set(Key.FISHY_LAVA, false);
                        FishyNotis.off("Clear Lava");
                        RenderTweaks.refresh();
                        return 1;
                    }))
                .executes(context -> {
                    FishyNotis.alert(Text.literal("Usage: /fa lava <on | off>"));
                    return 1;
                });
    }

    protected static LiteralArgumentBuilder<FabricClientCommandSource> registerHud() {
        return ClientCommandManager.literal("hud")
                .executes(context -> {
                    if (checkGUI() == 1) return 1;
                    MinecraftClient.getInstance().execute(() ->
                        GuiScheduler.scheduleGui(new HudEditScreen())
                    );
                    return 1;
                });
    }   
    
    protected static LiteralArgumentBuilder<FabricClientCommandSource> registerPing() {
        return ClientCommandManager.literal("ping")
                .then(ClientCommandManager.literal("on")
                    .executes(context -> {
                        FishyConfig.enable(Key.HUD_PING_ENABLED, true);
                        FishyNotis.on("Network Display");
                        NetworkMetrics.refresh();
                        return 1;
                    }))
                .then(ClientCommandManager.literal("off")
                    .executes(context -> {
                        FishyConfig.disable(Key.HUD_PING_ENABLED);
                        FishyNotis.off("Network Display");
                        NetworkMetrics.refresh();
                        return 1;
                    }))
                .executes(context -> {
                    NetworkMetrics.send();
                    var msg = Text.literal(NetworkMetrics.getPing() + " §8ms");
                    if (NetworkMetrics.shouldDisplay(Key.HUD_PING_SHOW_TPS)) {
                        msg = msg.copy().append(Text.literal("§8, §7" + NetworkMetrics.getTpsString() + " §8TPS"));
                    }
                    FishyNotis.send(msg);
                    return 1;
                });
    }   
    
    private static final String F5 = "Custom F5";

    protected static LiteralArgumentBuilder<FabricClientCommandSource> registerCam() {
        return ClientCommandManager.literal("camera")
                .then(ClientCommandManager.literal("on")
                    .executes(context -> {
                        FishyConfig.enable(Key.SKIP_F5, true);
                        FishyNotis.on(F5);
                        return 1;
                    }))
                .then(ClientCommandManager.literal("off")
                    .executes(context -> {
                        FishyConfig.disable(Key.SKIP_F5);
                        FishyNotis.off(F5);
                        return 1;
                    }))
                .then(ClientCommandManager.literal(TOGGLE)
                    .executes(context -> {
                        boolean current = FishyConfig.getState(Key.SKIP_F5, false);
                        FishyConfig.enable(Key.SKIP_F5, !current);
                        if (!current) {
                            FishyNotis.on(F5);
                        } else {
                            FishyNotis.off(F5);
                        }
                        return 1;
                    }))                        
                .executes(context -> {
                    FishyNotis.themed("Usage: §b/fa cam §8<§7 on §8| §7off §8| §7toggle §8>");
                    return 1;
                });
    }

    private static final String LABEL = "label";
    protected static LiteralArgumentBuilder<FabricClientCommandSource> registerPos() {
        return ClientCommandManager.literal("coords")
                .then(ClientCommandManager.argument(LABEL, com.mojang.brigadier.arguments.StringArgumentType.greedyString())
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
                                    net.minecraft.util.math.BlockPos pos = new net.minecraft.util.math.BlockPos(x, y, z);
                                    TempWaypoint.removeBeaconAt(pos);
                                    return 1;
                                })))))
                .then(ClientCommandManager.literal("redraw")
                    .then(ClientCommandManager.argument("x", IntegerArgumentType.integer())
                        .then(ClientCommandManager.argument("y", IntegerArgumentType.integer())
                            .then(ClientCommandManager.argument("z", IntegerArgumentType.integer())
                                .executes(context -> {
                                    // Redraw without label
                                    int x = context.getArgument("x", Integer.class);
                                    int y = context.getArgument("y", Integer.class);
                                    int z = context.getArgument("z", Integer.class);
                                    net.minecraft.util.math.BlockPos pos = new net.minecraft.util.math.BlockPos(x, y, z);
                                    TempWaypoint.redraw(pos, "");
                                    return 1;
                                })
                                .then(ClientCommandManager.argument(LABEL, StringArgumentType.greedyString())
                                    .executes(context -> {
                                        // Redraw with label
                                        int x = context.getArgument("x", Integer.class);
                                        int y = context.getArgument("y", Integer.class);
                                        int z = context.getArgument("z", Integer.class);
                                        String label = context.getArgument(LABEL, String.class);
                                        net.minecraft.util.math.BlockPos pos = new net.minecraft.util.math.BlockPos(x, y, z);
                                        TempWaypoint.redraw(pos, label);
                                        return 1;
                                    }))))))
                .executes(context -> {
                    PlayerPosition.giveAwayCoords();
                    return 1;
                });
    }

    protected static LiteralArgumentBuilder<FabricClientCommandSource> registerRain() {
            return ClientCommandManager.literal("rain")
                    .then(ClientCommandManager.literal("track")
                        .executes(context -> {
                            boolean isRaining = me.valkeea.fishyaddons.feature.skyblock.WeatherTracker.isRaining();
                            me.valkeea.fishyaddons.feature.skyblock.WeatherTracker.track();
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
                        FishyNotis.themed("§lRain Tracker:");
                        FishyNotis.alert(Text.literal("§3/fa rain track §8- §7Track the current rain state."));
                        FishyNotis.alert(Text.literal("§3/fa rain on | off §8- §7Enable/disable rain notifications"));
                        return 1;
                    });
    }    

    protected static LiteralArgumentBuilder<FabricClientCommandSource> registerFishing() {
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
                        me.valkeea.fishyaddons.tracker.fishing.ScData.getInstance().sendCatchPercentages();
                        return 1;
                    }))
                .then(ClientCommandManager.argument("name", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                    .executes(context -> {
                        String name = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "name");
                        me.valkeea.fishyaddons.tracker.fishing.ScData.getInstance().sendHistogramSummary(name);
                        return 1;
                    }))
                .executes(context -> {
                    FishyNotis.themed("Usage:");
                    FishyNotis.alert(Text.literal("§3/fa sc sounds §8- §7Instructions for resource pack sounds"));
                    FishyNotis.alert(Text.literal("§3/fa sc since §8- §7Stats for 'sc since' in the current island"));
                    FishyNotis.alert(Text.literal("§3/fa sc rng §8- §7Catch % for rare scs"));
                    FishyNotis.alert(Text.literal("§3/fa sc <name> §8- §7Data summary for a specific sc"));
                    return 1;
                });
    }

    protected static LiteralArgumentBuilder<FabricClientCommandSource> registerDiana() {
        return ClientCommandManager.literal("diana")
                .then(ClientCommandManager.literal("reset")
                .executes(context -> {
                    if (me.valkeea.fishyaddons.tracker.DianaStats.loaded()) {
                        me.valkeea.fishyaddons.tracker.DianaStats.getInstance().resetAll();
                        FishyNotis.send("Diana stats have been reset.");
                    } else {
                        FishyNotis.warn("Diana stats are not loaded.");
                    }
                    return 1;
                }))
                .executes(context -> {
                    me.valkeea.fishyaddons.tracker.DianaStats.getInstance().sendDianaStats();
                    return 1;
                });
    }

    protected static LiteralArgumentBuilder<FabricClientCommandSource> registerSkill() {
        return ClientCommandManager.literal("skilltracker")
                .then(ClientCommandManager.literal("dt")
                .executes(context -> {
                    me.valkeea.fishyaddons.tracker.SkillTracker.getInstance().toggleDownTime();
                    return 1;
                }))
                .executes(context -> {
                    FishyNotis.themed("Usage:");
                    FishyNotis.alert(Text.literal("§3/fa skilltracker downtime §8- §7Toggle downtime mode. Otherwise, skill XP tracking is paused after 1.5min and wiped after 15min."));
                    return 1;
                });
    }    

    private static final String CHAIN_NAME = "chainName";
    private static final String ORDER_NUMBER = "orderNumber";

    protected static LiteralArgumentBuilder<FabricClientCommandSource> registerWaypoint(String root) {
        return ClientCommandManager.literal(root)
                .then(ClientCommandManager.argument(CHAIN_NAME, StringArgumentType.word())
                    .then(ClientCommandManager.argument(ORDER_NUMBER, IntegerArgumentType.integer(1))
                        .executes(context -> {
                            String chainName = StringArgumentType.getString(context, CHAIN_NAME);
                            int orderNumber = IntegerArgumentType.getInteger(context, ORDER_NUMBER);
                            return WaypointCmd.addWaypoint(chainName, orderNumber);
                        })))
                .then(ClientCommandManager.literal("list")
                    .executes(context ->  WaypointCmd.listWaypoints()
                    ))
                .then(ClientCommandManager.literal("info")
                    .then(ClientCommandManager.argument(CHAIN_NAME, StringArgumentType.word())
                        .executes(context -> {
                            String chainName = StringArgumentType.getString(context, CHAIN_NAME);
                            return WaypointCmd.showChainInfo(chainName);
                        })))
                .then(ClientCommandManager.literal(CLEAR)
                    .then(ClientCommandManager.argument(CHAIN_NAME, StringArgumentType.word())
                        .executes(context -> {
                            String chainName = StringArgumentType.getString(context, CHAIN_NAME);
                            return WaypointCmd.clearChain(chainName);
                        })))
                .then(ClientCommandManager.literal("remove")
                    .then(ClientCommandManager.argument(CHAIN_NAME, StringArgumentType.word())
                        .then(ClientCommandManager.argument(ORDER_NUMBER, IntegerArgumentType.integer(1))
                            .executes(context -> {
                                String chainName = StringArgumentType.getString(context, CHAIN_NAME);
                                int orderNumber = IntegerArgumentType.getInteger(context, ORDER_NUMBER);
                                return WaypointCmd.removeWaypoint(chainName, orderNumber);
                            }))))
                .then(ClientCommandManager.literal("reset")
                    .then(ClientCommandManager.argument(CHAIN_NAME, StringArgumentType.word())
                        .executes(context -> {
                            String chainName = StringArgumentType.getString(context, CHAIN_NAME);
                            return WaypointCmd.resetChain(chainName);
                        })))
                .then(ClientCommandManager.literal("color")
                    .then(ClientCommandManager.argument(CHAIN_NAME, StringArgumentType.word())
                        .executes(context -> {
                            String chainName = StringArgumentType.getString(context, CHAIN_NAME);
                            return WaypointCmd.openColorPicker(chainName);
                        })))
                .then(ClientCommandManager.literal(TOGGLE)
                    .then(ClientCommandManager.argument(CHAIN_NAME, StringArgumentType.word())
                        .executes(context -> {
                            String chainName = StringArgumentType.getString(context, CHAIN_NAME);
                            return WaypointCmd.toggleChainVisibility(chainName);
                        })))
                .then(ClientCommandManager.literal("next")
                    .executes(context -> WaypointCmd.addNext()
                    ))
                .then(ClientCommandManager.literal("set")
                    .then(ClientCommandManager.argument(CHAIN_NAME, StringArgumentType.word())
                        .executes(context -> {
                            String chainName = StringArgumentType.getString(context, CHAIN_NAME);
                            return WaypointCmd.setLastModifiedChain(chainName);
                        })))  
                .then(ClientCommandManager.literal("rename")
                    .then(ClientCommandManager.argument(CHAIN_NAME, StringArgumentType.word())
                        .then(ClientCommandManager.argument(NEW_NAME, StringArgumentType.word())
                            .executes(context -> {
                                String chainName = StringArgumentType.getString(context, CHAIN_NAME);
                                String newName = StringArgumentType.getString(context, NEW_NAME);
                                return WaypointCmd.renameChain(chainName, newName);
                            }))))
                .executes(context -> {
                    FishyNotis.fwp();
                    return 1;
                });
    }

    protected static int checkGUI() {
        if (MinecraftClient.getInstance().currentScreen != null
            && !(MinecraftClient.getInstance().currentScreen instanceof ChatScreen)) {
            return 1;
        }
        return 0;
    }

    private static class CmdChat {
        static boolean pendingClear = false;
    }

    private static void sendClickable(String onAccept, String onDecline) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        Text yes = Text.literal("[Yes]")
            .styled(style -> style.withClickEvent(new net.minecraft.text.ClickEvent.RunCommand(onAccept)).withColor(0xCCFFCC));
        Text no = Text.literal("[No]")
            .styled(style -> style.withClickEvent(new net.minecraft.text.ClickEvent.RunCommand(onDecline)).withColor(0xFF8080));
        mc.player.sendMessage(Text.literal(" ").append(yes).append(Text.literal(" ")).append(no), false);
    }

    private FishyCmd() {
        throw new UnsupportedOperationException("Utility class");
    }
}
