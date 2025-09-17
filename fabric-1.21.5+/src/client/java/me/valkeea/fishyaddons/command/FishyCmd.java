package me.valkeea.fishyaddons.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.ItemConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.handler.ActiveBeacons;
import me.valkeea.fishyaddons.handler.ChatAlert;
import me.valkeea.fishyaddons.handler.ChatReplacement;
import me.valkeea.fishyaddons.handler.ClientPing;
import me.valkeea.fishyaddons.handler.CommandAlias;
import me.valkeea.fishyaddons.handler.KeyShortcut;
import me.valkeea.fishyaddons.handler.RenderTweaks;
import me.valkeea.fishyaddons.safeguard.ItemHandler;
import me.valkeea.fishyaddons.tool.GuiScheduler;
import me.valkeea.fishyaddons.ui.HudEditScreen;
import me.valkeea.fishyaddons.ui.list.ChatAlerts;
import me.valkeea.fishyaddons.ui.list.TabbedListScreen;
import me.valkeea.fishyaddons.util.FishyNotis;
import me.valkeea.fishyaddons.util.PlayerPosition;
import me.valkeea.fishyaddons.util.text.TextFormatUtil;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class FishyCmd {
    private static final String PROFILE = "profile";

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

                        RegistryWrapper.WrapperLookup registries = mc.world.getRegistryManager();
                        String uuid = ItemHandler.extractUUID(held, registries);
                        if (uuid == null || uuid.isEmpty()) {
                            FishyNotis.warn("Held item doesn't have a UUID.");
                            return 1;
                        }
                        String displayNameJson = TextFormatUtil.serialize(held.getName());
                        ItemConfig.addUUID(uuid, held.getName());

                        Text displayNameText = TextFormatUtil.deserialize(displayNameJson);
                        FishyNotis.format(Text.literal("Your ").formatted(Formatting.GRAY)
                            .append(displayNameText)
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
                        RegistryWrapper.WrapperLookup registries = mc.world.getRegistryManager();
                        String uuid = ItemHandler.extractUUID(held, registries);
                        if (uuid != null && ItemHandler.isProtected(held, registries)) {
                            ItemConfig.removeUUID(uuid);
                            FishyNotis.format(Text.literal("Your ").formatted(Formatting.GRAY)
                                .append(Text.literal(held.getName().getString()).formatted(Formatting.RESET))
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
                .then(ClientCommandManager.literal("clear")
                    .executes(context -> {
                        if (!CmdChat.pendingClear) {
                            CmdChat.pendingClear = true;
                            FishyNotis.send("Are you SURE you want to clear all protected items?");
                            sendClickable();
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
                        FishyNotis.themed("Usage: /fa profit <add | remove | list | clear>");
                        return 1;
                    }));
        }   

        protected static void addProfitSubcommands(LiteralArgumentBuilder<FabricClientCommandSource> root) {
            root.then(ClientCommandManager.literal("toggle")
                    .executes(context -> {
                        ProfitTrackerCommand.handle(new String[]{"toggle"});
                        return 1;
                    }))
                .then(ClientCommandManager.literal("stats")
                    .executes(context -> {
                        ProfitTrackerCommand.handle(new String[]{"stats"});
                        return 1;
                    }))
                .then(ClientCommandManager.literal("init")
                    .executes(context -> {
                        ProfitTrackerCommand.handle(new String[]{"init"});
                        return 1;
                    }))
                .then(ClientCommandManager.literal("refresh")
                    .executes(context -> {
                        ProfitTrackerCommand.handle(new String[]{"refresh"});
                        return 1;
                    }))
                .then(ClientCommandManager.literal("status")
                    .executes(context -> {
                        ProfitTrackerCommand.handle(new String[]{"status"});
                        return 1;
                    }))
                .then(ClientCommandManager.literal("clear")
                    .executes(context -> {
                        ProfitTrackerCommand.handle(new String[]{"clear"});
                        return 1;
                    }))
                .then(ClientCommandManager.literal("ignored")
                    .executes(context -> {
                        ProfitTrackerCommand.handle(new String[]{"ignored"});
                        return 1;
                    }))
                .then(ClientCommandManager.literal("restore")
                    .then(ClientCommandManager.literal("all")
                        .executes(context -> {
                            ProfitTrackerCommand.handle(new String[]{"restore", "all"});
                            return 1;
                        }))
                    .then(ClientCommandManager.argument("item", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                        .executes(context -> {
                            String itemName = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "item");
                            ProfitTrackerCommand.handle(new String[]{"restore", itemName});
                            return 1;
                        }))
                    .executes(context -> {
                        ProfitTrackerCommand.handle(new String[]{"restore"});
                        return 1;
                    }))
                .then(ClientCommandManager.literal("type")
                    .then(ClientCommandManager.literal("insta_sell")
                        .executes(context -> {
                            ProfitTrackerCommand.handle(new String[]{"type", "insta_sell"});
                            return 1;
                        }))
                    .then(ClientCommandManager.literal("sell_offer")
                        .executes(context -> {
                            ProfitTrackerCommand.handle(new String[]{"type", "sell_offer"});
                            return 1;
                        }))
                    .executes(context -> {
                        ProfitTrackerCommand.handle(new String[]{"type"});
                        return 1;
                    }))
                .then(ClientCommandManager.literal("price")
                    .then(ClientCommandManager.argument("item", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                        .executes(context -> {
                            String itemName = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "item");
                            ProfitTrackerCommand.handle(new String[]{"price", itemName});
                            return 1;
                        }))
                    .executes(context -> {
                        ProfitTrackerCommand.handle(new String[]{"price"});
                        return 1;
                    }))
                .then(ClientCommandManager.literal(PROFILE)
                    .then(ClientCommandManager.argument("name", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                        .executes(context -> {
                            String profileName = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "name");
                            ProfitTrackerCommand.handle(new String[]{PROFILE, profileName});
                            return 1;
                        }))
                    .then(ClientCommandManager.literal("delete")
                        .then(ClientCommandManager.argument(PROFILE, com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                            .executes(context -> {
                                String profileName = com.mojang.brigadier.arguments.StringArgumentType.getString(context, PROFILE);
                                ProfitTrackerCommand.handle(new String[]{PROFILE, "delete", profileName});
                                return 1;
                            })))
                    .then(ClientCommandManager.literal("rename")
                        .then(ClientCommandManager.argument("oldName", com.mojang.brigadier.arguments.StringArgumentType.word())
                            .then(ClientCommandManager.argument("newName", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                                .executes(context -> {
                                    String oldName = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "oldName");
                                    String newName = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "newName");
                                    ProfitTrackerCommand.handle(new String[]{PROFILE, "rename", oldName, newName});
                                    return 1;
                                }))))
                    .executes(context -> {
                        ProfitTrackerCommand.handle(new String[]{PROFILE});
                        return 1;
                    }))
                .executes(context -> {
                    ProfitTrackerCommand.handle(new String[]{});
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
                            FishyNotis.on("Ping display");
                            ClientPing.refresh();
                            return 1;
                        }))
                    .then(ClientCommandManager.literal("off")
                        .executes(context -> {
                            FishyConfig.disable(Key.HUD_PING_ENABLED);
                            FishyNotis.off("Ping display");
                            ClientPing.refresh();
                            return 1;
                        }))
                    .executes(context -> {
                        ClientPing.send();
                        FishyNotis.send(
                            Text.literal(ClientPing.get() + " §8ms").formatted(Formatting.WHITE)
                        );
                        return 1;
                    });
        }   
        
        protected static LiteralArgumentBuilder<FabricClientCommandSource> registerCam() {
            return ClientCommandManager.literal("cam")
                    .then(ClientCommandManager.literal("on")
                        .executes(context -> {
                            FishyConfig.enable(Key.SKIP_F5, true);
                            FishyNotis.on("Custom F5");
                            return 1;
                        }))
                    .then(ClientCommandManager.literal("off")
                        .executes(context -> {
                            FishyConfig.disable(Key.SKIP_F5);
                            FishyNotis.off("Custom F5");
                            return 1;
                        }))
                    .then(ClientCommandManager.literal("toggle")
                        .executes(context -> {
                            boolean current = FishyConfig.getState(Key.SKIP_F5, false);
                            FishyConfig.enable(Key.SKIP_F5, !current);
                            if (!current) {
                                FishyNotis.on("Custom F5");
                            } else {
                                FishyNotis.off("Custom F5");
                            }
                            return 1;
                        }))                        
                    .executes(context -> {
                        FishyNotis.alert(Text.literal("Usage: /fa cam <on | off> | <toggle>"));
                        return 1;
                    });
        }

    protected static LiteralArgumentBuilder<FabricClientCommandSource> registerPos() {
        return ClientCommandManager.literal("coords")
                .then(ClientCommandManager.argument("label", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                    .executes(context -> {
                        String label = context.getArgument("label", String.class);
                        PlayerPosition.giveAwayCoordsWithLabel(label);
                        return 1;
                    }))
                .then(ClientCommandManager.literal("last")
                    .executes(context -> {
                        ActiveBeacons.redrawLast();
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
                                    ActiveBeacons.removeBeaconAt(pos);
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
                                    ActiveBeacons.redraw(pos, "");
                                    return 1;
                                })
                                .then(ClientCommandManager.argument("label", StringArgumentType.greedyString())
                                    .executes(context -> {
                                        // Redraw with label
                                        int x = context.getArgument("x", Integer.class);
                                        int y = context.getArgument("y", Integer.class);
                                        int z = context.getArgument("z", Integer.class);
                                        String label = context.getArgument("label", String.class);
                                        net.minecraft.util.math.BlockPos pos = new net.minecraft.util.math.BlockPos(x, y, z);
                                        ActiveBeacons.redraw(pos, label);
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
                            boolean isRaining = me.valkeea.fishyaddons.handler.WeatherTracker.isRaining();
                            me.valkeea.fishyaddons.handler.WeatherTracker.track();
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
                            me.valkeea.fishyaddons.handler.WeatherTracker.reset();
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
        return ClientCommandManager.literal("fishing")
                .then(ClientCommandManager.literal("sounds")
                    .executes(context -> {
                            // Give instructions on resource pack creation
                            FishyNotis.send("§aTo create a resource pack:");
                            FishyNotis.alert(Text.literal("§7- Create a folder named: §bfishyaddons"));
                            FishyNotis.alert(Text.literal("§7- Inside that folder, create another folder named: §bsounds"));
                            FishyNotis.alert(Text.literal("§7- Inside the sounds folder, create a folder named: §bcustom"));
                            FishyNotis.alert(Text.literal("§7- Place your custom .ogg files inside the custom folder"));
                            FishyNotis.alert(Text.literal("§7- Then use sound IDs: §bfishyaddons:fishyaddons_1§7, §bfishyaddons:fishyaddons_2§7, §bfishyaddons:fishyaddons_3"));
                        return 1;
                    }))
                .executes(context -> {
                    FishyNotis.send("§3Usage: /fa fishing sounds - open directory to add your custom .ogg files");
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

    // Helper for confirmation state
    private static class CmdChat {
        static boolean pendingClear = false;
    }

    // Helper for clickable confirmation
    private static void sendClickable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        Text yes = Text.literal("[Yes]")
            .styled(style -> style.withClickEvent(new net.minecraft.text.ClickEvent.RunCommand("/fa guard confirmclear")).withColor(0xCCFFCC));
        Text no = Text.literal("[No]")
            .styled(style -> style.withClickEvent(new net.minecraft.text.ClickEvent.RunCommand("/fa guard cancelclear")).withColor(0xFF8080));
        mc.player.sendMessage(Text.literal(" ").append(yes).append(Text.literal(" ")).append(no), false);
    }

    private FishyCmd() {
        throw new UnsupportedOperationException("Utility class");
    }
}
