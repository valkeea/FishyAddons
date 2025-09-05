package me.valkeea.fishyaddons.util;

import me.valkeea.fishyaddons.config.ItemConfig;
import me.valkeea.fishyaddons.tool.FishyMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class FishyNotis {
    private FishyNotis() {}
    private static final String FORMAT1 = "   [ ";
    private static final String FORMAT2 = " ] ";
    private static final String SPACE = "     ";
    private static final String GUIDE_CMD = "/fa guide";

    private static void chat(Text message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(message, false);
        }
    }

    public static void alert(Text message) {
        chat(message);
    }
    
    private static Text prefix() {
        int theme = FishyMode.getCmdColor();
        return Text.literal("[α]").styled(style -> style.withColor(theme))
            .append(Text.literal(" » ").styled(style -> style.withColor(Formatting.DARK_GRAY)));
    }

    public static void send(Text message) {
        Text styledMsg = message.copy().styled(style -> style.withColor(Formatting.GRAY));
        chat(prefix().copy().append(styledMsg));
    }

    public static void send(String message) {
        Text styledMsg = Text.literal(message).styled(style -> style.withColor(Formatting.GRAY));
        chat(prefix().copy().append(styledMsg));
    }

    public static void off(String message) {
        Text styledMsg = Text.literal(message).styled(style -> style.withColor(Formatting.GRAY));
        Text off = Text.literal(" OFF").styled(style -> style.withColor(0xFF8080));
        chat(prefix().copy().append(styledMsg).append(off));
    }

    public static void on(String message) {
        Text styledMsg = Text.literal(message).styled(style -> style.withColor(Formatting.GRAY));
        Text on = Text.literal(" ON").styled(style -> style.withColor(0xCCFFCC));
        chat(prefix().copy().append(styledMsg).append(on));
    }

    public static void warn(String message) {
        chat(Text.literal(message).styled(style -> style.withColor(0xFF8080).withItalic(true)));
    }

    public static void notice(String message) {
        chat(Text.literal(message).styled(style -> style.withColor(Formatting.DARK_GRAY).withItalic(true)));
    }

    public static void format(Text message) {
        int theme = FishyMode.getCmdColor();
        Text prefix = Text.literal("[α]").styled(style -> style.withColor(theme))
            .append(Text.literal(" » ").formatted(Formatting.DARK_GRAY));
        chat(prefix.copy().append(message));
    }

    public static void themed(String message) {
        Text text = Text.literal(message).styled(style -> style.withColor(FishyMode.getCmdColor()));
        chat(text);
    }

    public static void action(Text message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(message, true);
        }
    }        

    public static void protectNoti() {
        if (!ItemConfig.isProtectNotiEnabled()) return;
        send("Item protected");
    }

    public static void ccNoti() {
        if (!me.valkeea.fishyaddons.handler.CopyChat.isNotiOn()) return;
        send("Copied to clipboard");
    }

    public static void helpNoti() {
        int theme = FishyMode.getCmdColor();
        chat(Text.literal("α Available Commands α").styled(style -> style.withColor(theme).withBold(true)));
        chat(Text.literal("/fishyaddons opens the main config! /fp to see tracker commands.").formatted(Formatting.AQUA));
        chat(Text.literal("/fishyaddons = /fa, /fa guard = /fg").formatted(Formatting.AQUA));
        chat(Text.literal("/fg clear | add | remove | list | alert").formatted(Formatting.DARK_AQUA));
        chat(Text.literal("/fa key | /fa cmd |/fa chat | /fg").formatted(Formatting.DARK_AQUA));
        chat(Text.literal("/fa key | cmd | chat + on | off").formatted(Formatting.DARK_AQUA));
        chat(Text.literal("/fa key | cmd | fg + add").formatted(Formatting.DARK_AQUA));
        chat(Text.literal("/fa lava | camera + on | off").formatted(Formatting.DARK_AQUA));
        chat(Text.literal("/fa ping | on | off").formatted(Formatting.DARK_AQUA));
        chat(Text.literal("/fa coords <title>").formatted(Formatting.DARK_AQUA));
        chat(Text.literal("/fa hud").formatted(Formatting.DARK_AQUA));
        chat(Text.literal(GUIDE_CMD).formatted(Formatting.DARK_AQUA));
        chat(Text.literal("/fa help").formatted(Formatting.DARK_AQUA));
    }

    public static void guideNoti2() {
        int theme = FishyMode.getCmdColor();
        chat(Text.literal("α FishyAddons Commands α").styled(style -> style.withColor(theme).withBold(true)));
        chat(Text.literal(FORMAT1).formatted(Formatting.DARK_AQUA)
                .append(Text.literal("/fishyaddons | /fa").formatted(Formatting.AQUA))
                .append(Text.literal(FORMAT2).formatted(Formatting.DARK_AQUA))
                .append(Text.literal("Open Config Screen").formatted(Formatting.WHITE)));
        chat(Text.literal("FishyAddons Safeguard:").styled(style -> style.withColor(0x8AE2B6)));
        chat(Text.literal(" - ").formatted(Formatting.DARK_AQUA)
                .append(Text.literal("Protects your items from being dropped or interacted with" + SPACE + "in certain GUIs.").formatted(Formatting.GRAY)));
        chat(Text.literal(FORMAT1).formatted(Formatting.DARK_AQUA)
                .append(Text.literal("/fg add | remove").formatted(Formatting.AQUA))
                .append(Text.literal(FORMAT2).formatted(Formatting.DARK_AQUA))
                .append(Text.literal("Add or remove held item").formatted(Formatting.WHITE)));
        chat(Text.literal(FORMAT1).formatted(Formatting.DARK_AQUA)
                .append(Text.literal("/fg list").formatted(Formatting.AQUA))
                .append(Text.literal(FORMAT2).formatted(Formatting.DARK_AQUA))
                .append(Text.literal("List added UUIDs").formatted(Formatting.WHITE)));
        chat(Text.literal(FORMAT1).formatted(Formatting.DARK_AQUA)
                .append(Text.literal("/fg clear").formatted(Formatting.AQUA))
                .append(Text.literal(FORMAT2).formatted(Formatting.DARK_AQUA))
                .append(Text.literal("Clear all UUIDs").formatted(Formatting.WHITE)));

        chat(Text.literal("Visual Features:").styled(style -> style.withColor(0x8AE2B6)));
        chat(Text.literal(FORMAT1).formatted(Formatting.DARK_AQUA)
                .append(Text.literal("/fa lava on | off").formatted(Formatting.AQUA))
                .append(Text.literal(FORMAT2).formatted(Formatting.DARK_AQUA))
                .append(Text.literal("Toggle lava visibility").formatted(Formatting.WHITE)));

        chat(Text.literal("General QoL:").styled(style -> style.withColor(0x8AE2B6)));
        chat(Text.literal(" - ").formatted(Formatting.DARK_AQUA)
                .append(Text.literal(" Custom keybinds, command aliases and chat text replacement.").formatted(Formatting.GRAY)));
        chat(Text.literal(FORMAT1).formatted(Formatting.DARK_AQUA)
                .append(Text.literal("/fa key | add | on | off |").formatted(Formatting.AQUA))
                .append(Text.literal(FORMAT2).formatted(Formatting.DARK_AQUA))
                .append(Text.literal("Open GUI, add keybinds, toggle").formatted(Formatting.WHITE)));
        chat(Text.literal(FORMAT1).formatted(Formatting.DARK_AQUA)
                .append(Text.literal("/fa cmd | add | on | off").formatted(Formatting.AQUA))
                .append(Text.literal(FORMAT2).formatted(Formatting.DARK_AQUA))
                .append(Text.literal("Open GUI, add aliases, toggle all").formatted(Formatting.WHITE)));
        chat(Text.literal(FORMAT1).formatted(Formatting.DARK_AQUA)
                .append(Text.literal("/fa chat | add | on | off").formatted(Formatting.AQUA))
                .append(Text.literal(FORMAT2).formatted(Formatting.DARK_AQUA))
                .append(Text.literal("Add chat replacements, toggle all").formatted(Formatting.WHITE)));
        chat(Text.literal(FORMAT1).formatted(Formatting.DARK_AQUA)
                .append(Text.literal("/fa alert | add | on | off").formatted(Formatting.AQUA))
                .append(Text.literal(FORMAT2).formatted(Formatting.DARK_AQUA))    
                .append(Text.literal("Add alerts, toggle all").formatted(Formatting.WHITE)));                            
        chat(Text.literal(" - ").formatted(Formatting.DARK_AQUA)
                .append(Text.literal(" Ping display, Render coords, Custom f5").formatted(Formatting.GRAY)));
        chat(Text.literal(" - ").formatted(Formatting.DARK_AQUA)
                .append(Text.literal(" Profit Tracker").formatted(Formatting.GRAY)));
        chat(Text.literal(FORMAT1).formatted(Formatting.DARK_AQUA)
                .append(Text.literal("/fa ping | on | off").formatted(Formatting.AQUA))
                .append(Text.literal(FORMAT2).formatted(Formatting.DARK_AQUA))
                .append(Text.literal("Check ping, toggle hud display").formatted(Formatting.WHITE)));
        chat(Text.literal(FORMAT1).formatted(Formatting.DARK_AQUA)
                .append(Text.literal("/fa coords <title>").formatted(Formatting.AQUA))
                .append(Text.literal(FORMAT2).formatted(Formatting.DARK_AQUA))
                .append(Text.literal("Send current coordinates").formatted(Formatting.WHITE)));
        chat(Text.literal(FORMAT1).formatted(Formatting.DARK_AQUA)
                .append(Text.literal("/fa camera | on | off").formatted(Formatting.AQUA))
                .append(Text.literal(FORMAT2).formatted(Formatting.DARK_AQUA))
                .append(Text.literal("Toggle custom f5 perspective").formatted(Formatting.WHITE)));
        chat(Text.literal(FORMAT1).formatted(Formatting.DARK_AQUA)
                .append(Text.literal("/fa hud").formatted(Formatting.AQUA))
                .append(Text.literal(FORMAT2).formatted(Formatting.DARK_AQUA))
                .append(Text.literal("Edit all hud elements").formatted(Formatting.WHITE)));
        chat(Text.literal(FORMAT1).formatted(Formatting.DARK_AQUA)
                .append(Text.literal("/fp").formatted(Formatting.AQUA))
                .append(Text.literal(FORMAT2).formatted(Formatting.DARK_AQUA))
                .append(Text.literal("Guide to Profit Tracker commands").formatted(Formatting.WHITE)));                
        }

        public static void guideNoti() {
                int theme = FishyMode.getCmdColor();
                chat(Text.literal("α Welcome to FishyAddons! α").styled(style -> style.withColor(theme).withBold(true)));
                chat(Text.literal(" - ").formatted(Formatting.DARK_AQUA)
                        .append(Text.literal(" Main config opens with /fa (Keybind in vanilla settings)").formatted(Formatting.GRAY)));
                Text guide = Text.literal(FORMAT1).formatted(Formatting.DARK_AQUA)
                        .append(Text.literal(GUIDE_CMD).formatted(Formatting.AQUA))
                        .append(Text.literal(FORMAT2).formatted(Formatting.DARK_AQUA))
                        .append(Text.literal("Show a detailed guide to commands.").formatted(Formatting.WHITE))
                        .styled(style -> style.withClickEvent(new net.minecraft.text.ClickEvent.RunCommand(GUIDE_CMD)));
                chat(guide);
                chat(Text.literal(" - ").formatted(Formatting.DARK_AQUA)
                        .append(Text.literal(" You can also use /fa help for a simple list of available commands").formatted(Formatting.GRAY)));
                chat(Text.literal(" I hope you enjoy using FishyAddons! ").formatted(Formatting.DARK_AQUA));
        }

        public static void fp() {
                int theme = FishyMode.getCmdColor();
                chat(Text.literal("α Profit Tracker Commands α").styled(style -> style.withColor(theme).withBold(true)));
                chat(Text.literal("§3/fa profit = /fp"));
                chat(Text.literal("§3/fp toggle §8- §7Enable/disable tracking"));
                chat(Text.literal("§3/fp clear §8- §7Clear current data"));
                chat(Text.literal("§3/fp refresh §8- §7Manually refresh cached prices"));
                chat(Text.literal("§3/fp stats §8- §7Show session/profile stats"));
                chat(Text.literal("§3/fp init §8- §7Manually initialize APIs"));
                chat(Text.literal("§3/fp status §8- §7Check API status"));
                chat(Text.literal("§3/fp price <amount> <item> §8- §7Check and update price"));
                chat(Text.literal("§3/fp profile §8- §7Show current/all profiles"));
                chat(Text.literal("§3/fp profile <name> §8- §7Create or switch to a profile"));
                chat(Text.literal("§3/fp profile rename <oldName> <newName> §8- §7Rename a profile"));
                chat(Text.literal("§3/fp ignored §8- §7Show ignored items (clickable to restore)"));
                chat(Text.literal("§3/fp restore <item | all> §8- §7Restore ignored item(s)"));
                themed("While active, the tracker will have HUD buttons in inventory screens.");
                themed("Profile data will save automatically on swap/disconnect!");
        }
}