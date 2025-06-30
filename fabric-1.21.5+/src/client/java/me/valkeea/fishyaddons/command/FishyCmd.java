package me.valkeea.fishyaddons.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.ItemConfig;
import me.valkeea.fishyaddons.gui.AliasAddScreen;
import me.valkeea.fishyaddons.gui.ChatAddScreen;
import me.valkeea.fishyaddons.gui.HudEditScreen;
import me.valkeea.fishyaddons.gui.QolScreen;
import me.valkeea.fishyaddons.gui.TabbedListScreen;
import me.valkeea.fishyaddons.gui.VisualSettingsScreen;
import me.valkeea.fishyaddons.handler.ClientPing;
import me.valkeea.fishyaddons.safeguard.ItemHandler;
import me.valkeea.fishyaddons.tool.GuiScheduler;
import me.valkeea.fishyaddons.util.FishyNotis;
import me.valkeea.fishyaddons.util.PlayerPosition;
import me.valkeea.fishyaddons.util.TextFormatUtil;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;


public class FishyCmd {
    private FishyCmd() {}

        protected static LiteralArgumentBuilder<FabricClientCommandSource> registerCmd() {
            return ClientCommandManager.literal("cmd")
                    .then(ClientCommandManager.literal("add")
                        .executes(context -> {
                            if (checkGUI() == 1) return 1;
                            MinecraftClient.getInstance().execute(() ->
                                GuiScheduler.scheduleGui(new AliasAddScreen(MinecraftClient.getInstance().currentScreen))
                            );
                            return 1;
                        }))
                    .then(ClientCommandManager.literal("on")
                        .executes(context -> {
                            FishyConfig.getCommandAliases().keySet().forEach(alias -> FishyConfig.toggleCommand(alias, true));
                            FishyNotis.send("Command aliases " + Formatting.GREEN + "ON.");
                            return 1;
                        }))
                    .then(ClientCommandManager.literal("off")
                        .executes(context -> {
                            FishyConfig.getCommandAliases().keySet().forEach(alias -> FishyConfig.toggleCommand(alias, false));
                            FishyNotis.send("Command aliases " + Formatting.RED + "OFF.");
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
                    .then(ClientCommandManager.literal("add")
                        .executes(context -> {
                            if (checkGUI() == 1) return 1;
                            MinecraftClient.getInstance().execute(() ->
                                GuiScheduler.scheduleGui(new ChatAddScreen(MinecraftClient.getInstance().currentScreen))
                            );
                            return 1;
                        }))
                    .then(ClientCommandManager.literal("on")
                        .executes(context -> {
                            FishyConfig.getChatReplacements().keySet().forEach(key -> FishyConfig.toggleChatReplacement(key, true));
                            FishyNotis.send("Chat replacements " + Formatting.GREEN + "ON.");
                            return 1;
                        }))
                    .then(ClientCommandManager.literal("off")
                        .executes(context -> {
                            FishyConfig.getChatReplacements().keySet().forEach(key -> FishyConfig.toggleChatReplacement(key, false));
                            FishyNotis.send("Chat replacements " + Formatting.RED + "OFF.");
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
                            FishyConfig.getKeybinds().keySet().forEach(key -> FishyConfig.toggleKeybind(key, true));
                            FishyNotis.send("Keybinds " + Formatting.GREEN + "ON.");
                            return 1;
                        }))
                    .then(ClientCommandManager.literal("off")
                        .executes(context -> {
                            FishyConfig.getKeybinds().keySet().forEach(key -> FishyConfig.toggleKeybind(key, false));
                            FishyNotis.send("Keybinds " + Formatting.RED + "OFF.");
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
                            FishyConfig.getChatAlerts().keySet().forEach(key -> FishyConfig.toggleChatAlert(key, true));
                            FishyNotis.send("Chat alerts " + Formatting.GREEN + "ON.");
                            return 1;
                        }))
                    .then(ClientCommandManager.literal("off")
                        .executes(context -> {
                            FishyConfig.getChatAlerts().keySet().forEach(key -> FishyConfig.toggleChatAlert(key, false));
                            FishyNotis.send("Chat alerts " + Formatting.RED + "OFF.");
                            return 1;
                        }))
                    .executes(context -> {
                        if (checkGUI() == 1) return 1;
                        MinecraftClient.getInstance().execute(() ->
                            GuiScheduler.scheduleGui(new TabbedListScreen(MinecraftClient.getInstance().currentScreen, TabbedListScreen.Tab.ALERT))
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
        
        public static void addGuardSubcommands(LiteralArgumentBuilder<FabricClientCommandSource> root, String rootNameForMessages) {
            root.then(ClientCommandManager.literal("add")
                    .executes(context -> {
                        MinecraftClient mc = MinecraftClient.getInstance();
                        if (mc.player == null || mc.world == null) return 1;

                        ItemStack held = mc.player.getMainHandStack();
                        if (held == null || held.isEmpty()) {
                            FishyNotis.send(Text.literal("You must be holding an item.").formatted(Formatting.GRAY));
                            return 1;
                        }

                        RegistryWrapper.WrapperLookup registries = mc.world.getRegistryManager();
                        String uuid = ItemHandler.extractUUID(held, registries);
                        if (uuid == null || uuid.isEmpty()) {
                            FishyNotis.send(Text.literal("Held item doesn't have a UUID.").formatted(Formatting.GRAY));
                            return 1;
                        }
                        String displayNameJson = TextFormatUtil.serialize(held.getName());
                        ItemConfig.addUUID(uuid, held.getName());

                        Text displayNameText = TextFormatUtil.deserialize(displayNameJson);
                        FishyNotis.send(Text.literal("Your ").formatted(Formatting.GRAY)
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
                            FishyNotis.send(Text.literal("You must be holding an item.").formatted(Formatting.GRAY));
                            return 1;
                        }
                        RegistryWrapper.WrapperLookup registries = mc.world.getRegistryManager();
                        String uuid = ItemHandler.extractUUID(held, registries);
                        if (uuid != null && ItemHandler.isProtected(held, registries)) {
                            ItemConfig.removeUUID(uuid);
                            FishyNotis.send(Text.literal("Your ").formatted(Formatting.GRAY)
                                .append(Text.literal(held.getName().getString()).formatted(Formatting.RESET))
                                .append(Text.literal(" is no longer protected.").formatted(Formatting.GRAY)));
                        } else {
                            FishyNotis.send(Text.literal("Held item isn't protected or doesn't have a UUID.").formatted(Formatting.GRAY));
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
                            FishyNotis.send(Text.literal("Are you SURE you want to clear all protected items?").formatted(Formatting.GRAY));
                            sendClickable();
                        } else {
                            FishyNotis.send(Text.literal("Please respond to the confirmation prompt for: /fa guard clear.").formatted(Formatting.GRAY));
                        }
                        return 1;
                    }))
                .then(ClientCommandManager.literal("confirmclear")
                    .executes(context -> {
                        ItemConfig.clearAll();
                        FishyNotis.send(Text.literal("All protected items have been cleared.").formatted(Formatting.GRAY));
                        CmdChat.pendingClear = false;
                        return 1;
                    }))
                .then(ClientCommandManager.literal("cancelclear")
                    .executes(context -> {
                        FishyNotis.send(Text.literal("/fa guard clear was canceled.").formatted(Formatting.GRAY));
                        CmdChat.pendingClear = false;
                        return 1;
                    }))
                .then(ClientCommandManager.literal("help")
                    .executes(context -> {
                        FishyNotis.send(Text.literal("Usage: /" + rootNameForMessages + " <add|remove|list|clear>").formatted(Formatting.GRAY));
                        return 1;
                    }));
        }   
        
        protected static LiteralArgumentBuilder<FabricClientCommandSource> registerLava() {
            return ClientCommandManager.literal("lava")
                    .then(ClientCommandManager.literal("on")
                        .executes(context -> {
                            FishyConfig.settings.set("fishyLava", true);
                            FishyNotis.send(Text.literal("Clear Lava " + Formatting.GREEN + "ON.").formatted(Formatting.GRAY));
                            return 1;
                        }))
                    .then(ClientCommandManager.literal("off")
                        .executes(context -> {
                            FishyConfig.settings.set("fishyLava", false);
                            FishyNotis.send("Clear Lava " + Formatting.RED + "OFF.");
                            return 1;
                        }))
                    .executes(context -> {
                        if (checkGUI() == 1) return 1;
                        MinecraftClient.getInstance().execute(() ->
                            GuiScheduler.scheduleGui(new VisualSettingsScreen())
                        );
                        return 1;
                    });
        }

        protected static LiteralArgumentBuilder<FabricClientCommandSource> registerHud() {
            return ClientCommandManager.literal("hud")
                    .then(ClientCommandManager.literal("edit")
                        .executes(context -> {
                            if (checkGUI() == 1) return 1;
                            MinecraftClient.getInstance().execute(() ->
                                GuiScheduler.scheduleGui(new HudEditScreen())
                            );
                            return 1;
                        }))
                    .then(ClientCommandManager.literal("on")
                        .executes(context -> {
                            FishyConfig.enable("pingHud", true);
                            FishyConfig.enable("timerHud", true);
                            FishyNotis.send("Ping HUD " + Formatting.GREEN + "ON.");
                            return 1;
                        }))
                    .then(ClientCommandManager.literal("off")
                        .executes(context -> {
                            FishyConfig.disable("pingHud");
                            FishyConfig.disable("timerHud");
                            FishyNotis.send("Ping HUD " + Formatting.RED + "OFF.");
                            return 1;
                        }))
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
                            FishyConfig.enable("pingHud", true);
                            FishyNotis.send("Ping display " + Formatting.GREEN + "ON.");
                            return 1;
                        }))
                    .then(ClientCommandManager.literal("off")
                        .executes(context -> {
                            FishyConfig.disable("pingHud");
                            FishyNotis.send("Ping display " + Formatting.RED + "OFF.");
                            return 1;
                        }))
                    .executes(context -> {
                        ClientPing.send();
                        FishyNotis.send(
                            Text.literal(" » ").formatted(Formatting.DARK_AQUA)
                                .append(Text.literal(ClientPing.get() + " ms").formatted(Formatting.WHITE))
                        );
                        return 1;
                    });
        } 
        
        protected static LiteralArgumentBuilder<FabricClientCommandSource> registerCam() {
            return ClientCommandManager.literal("camera")
                    .then(ClientCommandManager.literal("on")
                        .executes(context -> {
                            FishyConfig.enable("skipPerspective", true);
                            FishyNotis.send("Custom f5 " + Formatting.GREEN + "ON.");
                            return 1;
                        }))
                    .then(ClientCommandManager.literal("off")
                        .executes(context -> {
                            FishyConfig.disable("skipPerspective");
                            FishyNotis.send("Custom f5 " + Formatting.RED + "OFF.");
                            return 1;
                        }))
                    .executes(context -> {
                        if (checkGUI() == 1) return 1;
                        MinecraftClient.getInstance().execute(() ->
                            GuiScheduler.scheduleGui(new QolScreen())
                        );
                        return 1;
                    });
        }

    protected static LiteralArgumentBuilder<FabricClientCommandSource> registerPos() {
        return ClientCommandManager.literal("coords")
                .executes(context -> {
                    PlayerPosition.giveAwayCoords();
                    return 1;
                });
    }

    protected static LiteralArgumentBuilder<FabricClientCommandSource> registerQol() {
        return ClientCommandManager.literal("qol")
                .executes(context -> {
                    if (checkGUI() == 1) return 1;
                    MinecraftClient.getInstance().execute(() ->
                        GuiScheduler.scheduleGui(new QolScreen())
                    );
                    return 1;
                });
    }

    protected static LiteralArgumentBuilder<FabricClientCommandSource> registerVisual() {
        return ClientCommandManager.literal("visual")
                .executes(context -> {
                    if (checkGUI() == 1) return 1;
                    MinecraftClient.getInstance().execute(() ->
                        GuiScheduler.scheduleGui(new VisualSettingsScreen())
                    );
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
        Text yes = Text.literal("[Yes]").formatted(Formatting.GREEN)
            .styled(style -> style.withClickEvent(new net.minecraft.text.ClickEvent.RunCommand("/fa guard confirmclear")));
        Text no = Text.literal("[No]").formatted(Formatting.RED)
            .styled(style -> style.withClickEvent(new net.minecraft.text.ClickEvent.RunCommand("/fa guard cancelclear")));
        mc.player.sendMessage(Text.literal(" ").append(yes).append(Text.literal(" ")).append(no), false);
    }
}
