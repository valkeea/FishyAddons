package me.valkeea.fishyaddons.util;

import me.valkeea.fishyaddons.tool.FishyMode;
import me.valkeea.fishyaddons.util.text.Color;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.ui.render.VCText;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public class FishyNotis {
    private FishyNotis() {}
    private static final String FORMAT1 = "   [ ";
    private static final String FORMAT2 = " ] ";
    private static final String SPACE = "     ";
    private static final String GUIDE_CMD = "/fa guide";

    private static void chat(Component message) {
        var mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.execute(() -> 
            mc.player.sendSystemMessage(message));
        }
    }

    private static Component prefix() {
        int theme = FishyMode.getCmdColor();
        int theme2 = Color.mulRGB(theme, 0.7f);
        return Component.literal("[").withStyle(style -> style.withColor(theme2))
                .append(Component.literal("α").withStyle(style -> style.withColor(theme)))
                .append(Component.literal("]").withStyle(style -> style.withColor(theme2)))
            .append(Component.literal(" ▸ ").withStyle(style -> style.withColor(ChatFormatting.DARK_GRAY)));
    }

    /** Send text with prefix and custom formatting */
    public static void format(Component message) {
        chat(prefix().copy().append(message));
    }       

    /** Send text with prefix and default styling if none present */
    public static void send(Component message) {
        Component styledMsg = message.copy().withStyle(style -> style.withColor(ChatFormatting.GRAY));
        chat(prefix().copy().append(styledMsg));
    }

    /** Send plain string with prefix and default styling */
    public static void send(String message) {
        Component styledMsg = Component.literal(message).withStyle(style -> style.withColor(ChatFormatting.GRAY));
        chat(prefix().copy().append(styledMsg));
    }

    /** Send styled text as is */
    public static void alert(Component message) {
        chat(message);
    }    

    public static void off(String message) {
        Component styledMsg = Component.literal(message).withStyle(style -> style.withColor(ChatFormatting.GRAY));
        Component off = Component.literal(" OFF").withStyle(style -> style.withColor(0xFF8080));
        chat(prefix().copy().append(styledMsg).append(off));
    }

    public static void on(String message) {
        Component styledMsg = Component.literal(message).withStyle(style -> style.withColor(ChatFormatting.GRAY));
        Component on = Component.literal(" ON").withStyle(style -> style.withColor(0xCCFFCC));
        chat(prefix().copy().append(styledMsg).append(on));
    }

    /** warning / notice for issues */
    public static void warn(String message) {
        chat(Component.literal("|FA] " + message).withStyle(style -> style.withColor(0xFF8080).withItalic(true)));
    }

    /** warning used for alert features */
    public static void warn2(String message) {
        Component styledMsg = Component.literal(message).withStyle(style -> style.withColor(0xA10303).withBold(true));
        chat(prefix().copy().append(styledMsg));
    }

    /** non-intrusive message for minor info */
    public static void notice(String message) {
        chat(Component.literal(message).withStyle(style -> style.withColor(ChatFormatting.DARK_GRAY).withItalic(true)));
    }

    /** Send text with current theme color */
    public static void themed(String message) {
        Component text = Component.literal(message).withStyle(style -> style.withColor(FishyMode.getCmdColor()));
        chat(text);
    }

    /** Send text with current theme color for start and end */
    public static void themed2(String before, Component middle, String after) {
        int theme = FishyMode.getCmdColor();
        var combined = Component.literal(before).withStyle(style -> style.withColor(theme))
            .append(middle)
            .append(Component.literal(after).withStyle(style -> style.withColor(theme)));
        chat(combined);
    }

    /** Send text as an overlay message */
    public static void action(Component message) {
        var mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendSystemMessage(message);
        }
    }        

    public static void ccNoti() {
        if (!me.valkeea.fishyaddons.feature.qol.CopyChat.isNotiOn()) return;
        send("Copied to clipboard");
    }

    public static void bookNoti(Component styledItemName) {
        if (Config.get(BooleanKey.TRACKER_NOTIS)) {
            var prefix = VCText.header("BOOK DROP! ", Style.EMPTY.withBold(true));
            var message = prefix.copy().append(styledItemName);
            FishyNotis.alert(message);
        }
    }
    
    public static void trackerNoti(Component styledItemName, int quantity) {
        if (Config.get(BooleanKey.TRACKER_NOTIS)) {
            var prefix = VCText.header("TRACKED DROP! ", Style.EMPTY.withBold(true));
            var message = prefix.copy().append(styledItemName).append(Component.literal(quantity > 1 ? " §8x" + quantity : ""));
            FishyNotis.alert(message);
        }
    }    

    public static void helpNoti() {
        int theme = FishyMode.getCmdColor();
        chat(Component.literal("α Available Commands α").withStyle(style -> style.withColor(theme).withBold(true)));
        chat(Component.literal("/fishyaddons opens the main config!").withStyle(ChatFormatting.AQUA));
        chat(Component.literal("/fishyaddons = /fa, /fa guard = /fg").withStyle(ChatFormatting.AQUA));
        chat(Component.literal("/fg clear | add | remove | list").withStyle(ChatFormatting.DARK_AQUA));
        chat(Component.literal("/fa key | /fa cmd |/fa chat | /fg").withStyle(ChatFormatting.DARK_AQUA));
        chat(Component.literal("/fa key | cmd | chat + on | off").withStyle(ChatFormatting.DARK_AQUA));
        chat(Component.literal("/fa key | cmd | fg + add").withStyle(ChatFormatting.DARK_AQUA));
        chat(Component.literal("/fa lava | camera + on | off").withStyle(ChatFormatting.DARK_AQUA));
        chat(Component.literal("/fa ping | on | off").withStyle(ChatFormatting.DARK_AQUA));
        chat(Component.literal("/fa coords <title>").withStyle(ChatFormatting.DARK_AQUA));
        chat(Component.literal("/fa sc since | rng | <scname>").withStyle(ChatFormatting.DARK_AQUA));
        chat(Component.literal("/fa diana | reset").withStyle(ChatFormatting.DARK_AQUA));
        chat(Component.literal("/fa slayer | <type> | reset").withStyle(ChatFormatting.DARK_AQUA));
        chat(Component.literal("/fa skill dt | reset").withStyle(ChatFormatting.DARK_AQUA));
        chat(Component.literal("/fa hud").withStyle(ChatFormatting.DARK_AQUA));
        chat(Component.literal(GUIDE_CMD).withStyle(ChatFormatting.DARK_AQUA));
        chat(Component.literal("/fa help").withStyle(ChatFormatting.DARK_AQUA));
        chat(Component.literal("/fp - Profit Tracker commands").withStyle(ChatFormatting.AQUA));
        chat(Component.literal("/fwp - Waypoint Chain commands").withStyle(ChatFormatting.AQUA));        
    }

    public static void guideNoti2() {
        int theme = FishyMode.getCmdColor();
        chat(Component.literal("α FishyAddons Commands α").withStyle(style -> style.withColor(theme).withBold(true)));
        chat(Component.literal(FORMAT1).withStyle(ChatFormatting.DARK_AQUA).append(Component.literal("/fishyaddons | /fa").withStyle(ChatFormatting.AQUA))
            .append(Component.literal(FORMAT2).withStyle(ChatFormatting.DARK_AQUA))
            .append(Component.literal("Open Config Screen").withStyle(ChatFormatting.WHITE)));
        chat(Component.literal("FishyAddons Safeguard:").withStyle(style -> style.withColor(0x8AE2B6)));
        chat(Component.literal(" - ").withStyle(ChatFormatting.DARK_AQUA)
            .append(Component.literal("Protects your items from being dropped or interacted with" + SPACE + "in certain GUIs.").withStyle(ChatFormatting.GRAY)));
        chat(Component.literal(FORMAT1).withStyle(ChatFormatting.DARK_AQUA)
            .append(Component.literal("/fg add | remove").withStyle(ChatFormatting.AQUA))
            .append(Component.literal(FORMAT2).withStyle(ChatFormatting.DARK_AQUA))
            .append(Component.literal("Add or remove held item").withStyle(ChatFormatting.WHITE)));
        chat(Component.literal(FORMAT1).withStyle(ChatFormatting.DARK_AQUA)
            .append(Component.literal("/fg list").withStyle(ChatFormatting.AQUA))
            .append(Component.literal(FORMAT2).withStyle(ChatFormatting.DARK_AQUA))
            .append(Component.literal("List added UUIDs").withStyle(ChatFormatting.WHITE)));
        chat(Component.literal(FORMAT1).withStyle(ChatFormatting.DARK_AQUA)
            .append(Component.literal("/fg clear").withStyle(ChatFormatting.AQUA))
            .append(Component.literal(FORMAT2).withStyle(ChatFormatting.DARK_AQUA))
            .append(Component.literal("Clear all UUIDs").withStyle(ChatFormatting.WHITE)));

        chat(Component.literal("Visual Features:").withStyle(style -> style.withColor(0x8AE2B6)));
        chat(Component.literal(FORMAT1).withStyle(ChatFormatting.DARK_AQUA)
            .append(Component.literal("/fa lava on | off").withStyle(ChatFormatting.AQUA))
            .append(Component.literal(FORMAT2).withStyle(ChatFormatting.DARK_AQUA))
            .append(Component.literal("Toggle lava visibility").withStyle(ChatFormatting.WHITE)));

        chat(Component.literal("General QoL:").withStyle(style -> style.withColor(0x8AE2B6)));
        chat(Component.literal(" - ").withStyle(ChatFormatting.DARK_AQUA)
            .append(Component.literal(" Custom keybinds, command aliases and chat text replacement.").withStyle(ChatFormatting.GRAY)));
        chat(Component.literal(FORMAT1).withStyle(ChatFormatting.DARK_AQUA)
            .append(Component.literal("/fa key | add | on | off |").withStyle(ChatFormatting.AQUA))
            .append(Component.literal(FORMAT2).withStyle(ChatFormatting.DARK_AQUA))
            .append(Component.literal("Open GUI, add keybinds, toggle").withStyle(ChatFormatting.WHITE)));
        chat(Component.literal(FORMAT1).withStyle(ChatFormatting.DARK_AQUA)
            .append(Component.literal("/fa cmd | add | on | off").withStyle(ChatFormatting.AQUA))
            .append(Component.literal(FORMAT2).withStyle(ChatFormatting.DARK_AQUA))
            .append(Component.literal("Open GUI, add aliases, toggle all").withStyle(ChatFormatting.WHITE)));
        chat(Component.literal(FORMAT1).withStyle(ChatFormatting.DARK_AQUA)
            .append(Component.literal("/fa chat | add | on | off").withStyle(ChatFormatting.AQUA))
            .append(Component.literal(FORMAT2).withStyle(ChatFormatting.DARK_AQUA))
            .append(Component.literal("Add chat replacements, toggle all").withStyle(ChatFormatting.WHITE)));
        chat(Component.literal(FORMAT1).withStyle(ChatFormatting.DARK_AQUA)
            .append(Component.literal("/fa alert | add | on | off").withStyle(ChatFormatting.AQUA))
            .append(Component.literal(FORMAT2).withStyle(ChatFormatting.DARK_AQUA))    
            .append(Component.literal("Add alerts, toggle all").withStyle(ChatFormatting.WHITE)));                            
        chat(Component.literal(" - ").withStyle(ChatFormatting.DARK_AQUA)
            .append(Component.literal(" Ping display, Render coords, Custom f5").withStyle(ChatFormatting.GRAY)));
        chat(Component.literal(" - ").withStyle(ChatFormatting.DARK_AQUA)
            .append(Component.literal(" Profit Tracker").withStyle(ChatFormatting.GRAY)));
        chat(Component.literal(FORMAT1).withStyle(ChatFormatting.DARK_AQUA)
            .append(Component.literal("/fa ping | on | off").withStyle(ChatFormatting.AQUA))
            .append(Component.literal(FORMAT2).withStyle(ChatFormatting.DARK_AQUA))
            .append(Component.literal("Check ping, toggle hud display").withStyle(ChatFormatting.WHITE)));
        chat(Component.literal(FORMAT1).withStyle(ChatFormatting.DARK_AQUA)
            .append(Component.literal("/fa coords <title>").withStyle(ChatFormatting.AQUA))
            .append(Component.literal(FORMAT2).withStyle(ChatFormatting.DARK_AQUA))
            .append(Component.literal("Send current coordinates").withStyle(ChatFormatting.WHITE)));
        chat(Component.literal(FORMAT1).withStyle(ChatFormatting.DARK_AQUA)
            .append(Component.literal("/fa camera | on | off").withStyle(ChatFormatting.AQUA))
            .append(Component.literal(FORMAT2).withStyle(ChatFormatting.DARK_AQUA))
            .append(Component.literal("Toggle custom f5 perspective").withStyle(ChatFormatting.WHITE)));
        chat(Component.literal(FORMAT1).withStyle(ChatFormatting.DARK_AQUA)
            .append(Component.literal("/fa hud").withStyle(ChatFormatting.AQUA))
            .append(Component.literal(FORMAT2).withStyle(ChatFormatting.DARK_AQUA))
            .append(Component.literal("Edit all hud elements").withStyle(ChatFormatting.WHITE)));
        chat(Component.literal(FORMAT1).withStyle(ChatFormatting.DARK_AQUA)
            .append(Component.literal("/fp").withStyle(ChatFormatting.AQUA))
            .append(Component.literal(FORMAT2).withStyle(ChatFormatting.DARK_AQUA))
            .append(Component.literal("Guide to Profit Tracker commands").withStyle(ChatFormatting.WHITE)));                
    }

    public static void guideNoti() {
        int theme = FishyMode.getCmdColor();
        chat(Component.literal("α Welcome to FishyAddons! α").withStyle(style -> style.withColor(theme).withBold(true)));
        chat(Component.literal(" - ").withStyle(ChatFormatting.DARK_AQUA)
                .append(Component.literal(" Main config opens with /fa (Keybind in vanilla settings)").withStyle(ChatFormatting.GRAY)));
        Component guide = Component.literal(FORMAT1).withStyle(ChatFormatting.DARK_AQUA)
                .append(Component.literal(GUIDE_CMD).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(FORMAT2).withStyle(ChatFormatting.DARK_AQUA))
                .append(Component.literal("Show a detailed guide to commands.").withStyle(ChatFormatting.WHITE))
                .withStyle(style -> style.withClickEvent(new net.minecraft.network.chat.ClickEvent.RunCommand(GUIDE_CMD)));
        chat(guide);
        chat(Component.literal(" - ").withStyle(ChatFormatting.DARK_AQUA)
                .append(Component.literal(" You can also use /fa help for a simple list of available commands").withStyle(ChatFormatting.GRAY)));
        chat(Component.literal(" I hope you enjoy using FishyAddons! ").withStyle(ChatFormatting.DARK_AQUA));
    }

    public static void fp() {
        int theme = FishyMode.getCmdColor();
        chat(Component.literal("α Profit Tracker Commands α").withStyle(style -> style.withColor(theme).withBold(true)));
        chat(Component.literal("§3/fa profit = /fp"));
        chat(Component.literal("§3/fp toggle §8- §7Enable/disable tracking"));
        chat(Component.literal("§3/fp clear §8- §7Clear current data"));
        chat(Component.literal("§3/fp refresh §8- §7Manually refresh cached prices"));
        chat(Component.literal("§3/fp stats §8- §7Show session/profile stats"));
        chat(Component.literal("§3/fp price <amount> <item> §8- §7Check and update price"));
        chat(Component.literal("§3/fp profile §8- §7Show current/all profiles"));
        chat(Component.literal("§3/fp profile <name> §8- §7Create or switch to a profile"));
        chat(Component.literal("§3/fp profile rename <oldName> <newName> §8- §7Rename a profile"));
        chat(Component.literal("§3/fp ignored §8- §7Show ignored items (clickable to restore)"));
        chat(Component.literal("§3/fp restore <item | all> §8- §7Restore ignored item(s)"));
        themed("While active, the tracker will have HUD buttons in inventory screens.");
        themed("Profile data will save automatically on swap/disconnect!");
    }

    public static void fwp() {
        int theme = FishyMode.getCmdColor();
        chat(Component.literal("α Waypoint Chain Commands α").withStyle(style -> style.withColor(theme).withBold(true)));
        FishyNotis.alert(Component.literal("§3/fwp <chain> <order> §8- §7Add waypoint at current location"));
        FishyNotis.alert(Component.literal("§3/fwp list §8- §7List all waypoint chains"));
        FishyNotis.alert(Component.literal("§3/fwp info <chain> §8- §7Show detailed chain information"));
        FishyNotis.alert(Component.literal("§3/fwp color <chain> §8- §7Customize chain color"));
        FishyNotis.alert(Component.literal("§3/fwp rename <chain> <newName> §8- §7Rename a waypoint chain"));
        FishyNotis.alert(Component.literal("§3/fwp next §8- §7Add a waypoint to the previously modified chain"));
        FishyNotis.alert(Component.literal("§3/fwp set <chain> §8- §7Manually set the last modified chain"));
        FishyNotis.alert(Component.literal("§3/fwp toggle <chain> §8- §7Toggle chain visibility"));
        FishyNotis.alert(Component.literal("§3/fwp clear <chain> §8- §7Remove entire chain"));
        FishyNotis.alert(Component.literal("§3/fwp remove <chain> <order> §8- §7Remove specific waypoint"));
        FishyNotis.alert(Component.literal("§3/fwp reset <chain> §8- §7Reset completion status"));
    }
}
