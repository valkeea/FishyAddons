package me.valkeea.fishyaddons.util;

import me.valkeea.fishyaddons.config.ItemConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class FishyNotis {
    private FishyNotis() {}
    private static final String FORMAT1 = "   [ ";
    private static final String FORMAT2 = " ] ";
    private static final String SPACE = "     ";

    private static Text formatMessage(String message) {
        return Text.literal("[FA] ").formatted(Formatting.AQUA)
            .append(Text.literal(message).formatted(Formatting.GRAY));
    }

    public static void send(String message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(formatMessage(Formatting.GRAY + message), false);
        }
    }

    public static void send(Text message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            Text prefix = Text.literal("[FA] ").formatted(Formatting.AQUA);
            mc.player.sendMessage(prefix.copy().append(message), false);
        }
    }

    public static void alert(Text message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(message, false);
        }
    }

    public static void action(Text message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(message, true);
        }
    }        

    public static void protectNoti() {
        if (!ItemConfig.isProtectNotiEnabled()) return;
        send(Text.literal("Item protected").formatted(Formatting.GRAY).getString());
    }

    public static void helpNoti() {
        alert(Text.literal(" [FA] Available Commands:").formatted(Formatting.AQUA, Formatting.BOLD));
        alert(Text.literal("/fishyaddons = /fa, /faguard = /fg").formatted(Formatting.GRAY));
        alert(Text.literal("/fg clear | add | remove | list").formatted(Formatting.GRAY)
                .append(Text.literal(" - Clear, add or remove guarded items.").formatted(Formatting.RESET)));
        alert(Text.literal("/fa key | /fa cmd | /fg").formatted(Formatting.GRAY)
                .append(Text.literal(" - Commands for guis.").formatted(Formatting.RESET)));
        alert(Text.literal("/fa key | cmd + on | off").formatted(Formatting.GRAY)
                .append(Text.literal(" - Toggle all custom keybinds / commands.").formatted(Formatting.RESET)));
        alert(Text.literal("/fa key | cmd | fg + add").formatted(Formatting.GRAY)
                .append(Text.literal(" - Add a new keybind / command / guarded item.").formatted(Formatting.RESET)));
        alert(Text.literal("/fa lava on | off").formatted(Formatting.GRAY)
                .append(Text.literal(" - Toggle clear lava.").formatted(Formatting.RESET)));
        alert(Text.literal("/fa ping | on | off").formatted(Formatting.GRAY)
                .append(Text.literal(" - Check ping / enable hud display").formatted(Formatting.RESET)));
        alert(Text.literal("/fa guide").formatted(Formatting.GRAY)
                .append(Text.literal(" - Send introduction message").formatted(Formatting.RESET)));
    }

    public static void guideNoti() {
        alert(Text.literal("α Welcome to FishyAddons! α").formatted(Formatting.AQUA, Formatting.BOLD));
        alert(Text.literal("Current features include:").formatted(Formatting.GRAY));

        alert(Text.literal("FishyAddons Safeguard:").formatted(Formatting.GOLD));
        alert(Text.literal(" - ").formatted(Formatting.DARK_AQUA)
                .append(Text.literal("Protects your items from being dropped or interacted with" + SPACE + "in certain GUIs.").formatted(Formatting.GRAY)));
        alert(Text.literal(FORMAT1).formatted(Formatting.DARK_AQUA)
                .append(Text.literal("/faguard = fg").formatted(Formatting.AQUA))
                .append(Text.literal(FORMAT2).formatted(Formatting.DARK_AQUA))
                .append(Text.literal("Open GUI").formatted(Formatting.WHITE)));
        alert(Text.literal(FORMAT1).formatted(Formatting.DARK_AQUA)
                .append(Text.literal("/fg add | remove").formatted(Formatting.AQUA))
                .append(Text.literal(FORMAT2).formatted(Formatting.DARK_AQUA))
                .append(Text.literal("Add or remove held item").formatted(Formatting.WHITE)));
        alert(Text.literal(FORMAT1).formatted(Formatting.DARK_AQUA)
                .append(Text.literal("/fg list").formatted(Formatting.AQUA))
                .append(Text.literal(FORMAT2).formatted(Formatting.DARK_AQUA))
                .append(Text.literal("List added UUIDs").formatted(Formatting.WHITE)));
        alert(Text.literal(FORMAT1).formatted(Formatting.DARK_AQUA)
                .append(Text.literal("/fg clear").formatted(Formatting.AQUA))
                .append(Text.literal(FORMAT2).formatted(Formatting.DARK_AQUA))
                .append(Text.literal("Clear all UUIDs").formatted(Formatting.WHITE)));

        alert(Text.literal("Visual Features:").formatted(Formatting.GOLD));
        alert(Text.literal(" - ").formatted(Formatting.DARK_AQUA)
                .append(Text.literal("Clear lava (under), custom redstone particles for Jawbus" + SPACE + "laser and Flaming Flay.").formatted(Formatting.GRAY)));                
        alert(Text.literal(FORMAT1).formatted(Formatting.DARK_AQUA)
                .append(Text.literal("/fa lava on | off").formatted(Formatting.AQUA))
                .append(Text.literal(FORMAT2).formatted(Formatting.DARK_AQUA))
                .append(Text.literal("Toggle lava visibility").formatted(Formatting.WHITE)));
        alert(Text.literal("General QoL:").formatted(Formatting.GOLD));
        alert(Text.literal(" - ").formatted(Formatting.DARK_AQUA)        
                .append(Text.literal(" Custom keybinds, command aliases and chat text replacement.").formatted(Formatting.GRAY)));        
        alert(Text.literal(FORMAT1).formatted(Formatting.DARK_AQUA)
                .append(Text.literal("/fa key | add | on | off |").formatted(Formatting.AQUA))
                .append(Text.literal(FORMAT2).formatted(Formatting.DARK_AQUA))
                .append(Text.literal("Open GUI, add keybinds, toggle all").formatted(Formatting.WHITE)));
        alert(Text.literal(FORMAT1).formatted(Formatting.DARK_AQUA)
                .append(Text.literal("/fa cmd | add | on | off").formatted(Formatting.AQUA))
                .append(Text.literal(FORMAT2).formatted(Formatting.DARK_AQUA))
                .append(Text.literal("Open GUI, add aliases, toggle all").formatted(Formatting.WHITE)));
        alert(Text.literal(FORMAT1).formatted(Formatting.DARK_AQUA)   
                .append(Text.literal("/fa chat | add | on | off").formatted(Formatting.AQUA)) 
                .append(Text.literal(FORMAT2).formatted(Formatting.DARK_AQUA))
                .append(Text.literal("Open GUI, add chat replacements, toggle all").formatted(Formatting.WHITE)));
        alert(Text.literal(" - ").formatted(Formatting.DARK_AQUA)
                .append(Text.literal(" Ping display, shows your current ping in the HUD.").formatted(Formatting.GRAY)));
        alert(Text.literal(FORMAT1).formatted(Formatting.DARK_AQUA)
                .append(Text.literal("/fa ping | on | off").formatted(Formatting.AQUA))
                .append(Text.literal(FORMAT2).formatted(Formatting.DARK_AQUA))
                .append(Text.literal("Toggle ping display in the HUD").formatted(Formatting.WHITE)));
        }
}