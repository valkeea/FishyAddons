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

    public static void ccNoti() {
        if (!me.valkeea.fishyaddons.handler.CopyChat.isNotiOn()) return;
        send("Copied to clipboard");
    }

    public static void helpNoti() {
        alert(Text.literal(" [FA] Available Commands:").formatted(Formatting.AQUA, Formatting.BOLD));
        alert(Text.literal("/fishyaddons opens the main config!").formatted(Formatting.AQUA));        
        alert(Text.literal("/fishyaddons = /fa, /fa guard = /fg").formatted(Formatting.AQUA));
        alert(Text.literal("/fg clear | add | remove | list | alert").formatted(Formatting.DARK_AQUA));
        alert(Text.literal("/fa key | /fa cmd |/fa chat | /fg").formatted(Formatting.DARK_AQUA));
        alert(Text.literal("/fa key | cmd | chat + on | off").formatted(Formatting.DARK_AQUA));
        alert(Text.literal("/fa key | cmd | fg + add").formatted(Formatting.DARK_AQUA));
        alert(Text.literal("/fa lava | camera + on | off").formatted(Formatting.DARK_AQUA));
        alert(Text.literal("/fa ping | on | off").formatted(Formatting.DARK_AQUA));
        alert(Text.literal("/fa coords").formatted(Formatting.DARK_AQUA));
        alert(Text.literal("/fa hud").formatted(Formatting.DARK_AQUA));
        alert(Text.literal("/fa guide").formatted(Formatting.DARK_AQUA));
        alert(Text.literal("/fa help").formatted(Formatting.DARK_AQUA));
    }

    public static void guideNoti2() {
        alert(Text.literal("α Welcome to FishyAddons! α").formatted(Formatting.AQUA, Formatting.BOLD));
        alert(Text.literal("Current commands are:").formatted(Formatting.GRAY));

        alert(Text.literal("FishyAddons Safeguard:").formatted(Formatting.GOLD));
        alert(Text.literal(" - ").formatted(Formatting.DARK_AQUA)
                .append(Text.literal("Protects your items from being dropped or interacted with" + SPACE + "in certain GUIs.").formatted(Formatting.GRAY)));
        alert(Text.literal(FORMAT1).formatted(Formatting.DARK_AQUA)
                .append(Text.literal("/faguard (/fg), /fa visual, /fa qol, /fa alert").formatted(Formatting.AQUA))
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
                .append(Text.literal("Open GUI, add keybinds, toggle").formatted(Formatting.WHITE)));
        alert(Text.literal(FORMAT1).formatted(Formatting.DARK_AQUA)
                .append(Text.literal("/fa cmd | add | on | off").formatted(Formatting.AQUA))
                .append(Text.literal(FORMAT2).formatted(Formatting.DARK_AQUA))
                .append(Text.literal("Open GUI, add aliases, toggle all").formatted(Formatting.WHITE)));
        alert(Text.literal(FORMAT1).formatted(Formatting.DARK_AQUA)   
                .append(Text.literal("/fa chat | add | on | off").formatted(Formatting.AQUA)) 
                .append(Text.literal(FORMAT2).formatted(Formatting.DARK_AQUA))
                .append(Text.literal("Add chat replacements, toggle all").formatted(Formatting.WHITE)));
        alert(Text.literal(" - ").formatted(Formatting.DARK_AQUA)
                .append(Text.literal(" Ping display, Render coords, Custom f5").formatted(Formatting.GRAY)));
        alert(Text.literal(" - ").formatted(Formatting.DARK_AQUA)
                .append(Text.literal(" Clean Wither Impact, Mute Phantoms").formatted(Formatting.GRAY)));
        alert(Text.literal(FORMAT1).formatted(Formatting.DARK_AQUA)
                .append(Text.literal("/fa ping | on | off").formatted(Formatting.AQUA))
                .append(Text.literal(FORMAT2).formatted(Formatting.DARK_AQUA))
                .append(Text.literal("Check ping, toggle hud display").formatted(Formatting.WHITE)));
        alert(Text.literal(FORMAT1).formatted(Formatting.DARK_AQUA)
                .append(Text.literal("/fa coords").formatted(Formatting.AQUA))
                .append(Text.literal(FORMAT2).formatted(Formatting.DARK_AQUA))
                .append(Text.literal("Send current coordinates").formatted(Formatting.WHITE)));
        alert(Text.literal(FORMAT1).formatted(Formatting.DARK_AQUA)
                .append(Text.literal("/fa camera | on | off").formatted(Formatting.AQUA))
                .append(Text.literal(FORMAT2).formatted(Formatting.DARK_AQUA))
                .append(Text.literal("Toggle custom f5 perspective").formatted(Formatting.WHITE)));
        alert(Text.literal(FORMAT1).formatted(Formatting.DARK_AQUA)
                .append(Text.literal("/fa hud").formatted(Formatting.AQUA))
                .append(Text.literal(FORMAT2).formatted(Formatting.DARK_AQUA))
                .append(Text.literal("Edit all hud elements").formatted(Formatting.WHITE)));
        }

        public static void guideNoti() {
                alert(Text.literal("α Welcome to FishyAddons! α").formatted(Formatting.AQUA, Formatting.BOLD));
                alert(Text.literal("Current features include:").formatted(Formatting.GRAY));
                alert(Text.literal(" - ").formatted(Formatting.DARK_AQUA)
                        .append(Text.literal(" Item safeguard, Slotlocking and -binding").formatted(Formatting.GRAY)));
                alert(Text.literal(" - ").formatted(Formatting.DARK_AQUA)     
                        .append(Text.literal(" Pet display, Mob death/fire animation skip").formatted(Formatting.GRAY)));                                   
                alert(Text.literal(" - ").formatted(Formatting.DARK_AQUA)
                        .append(Text.literal(" Keybinds, Command Aliases, Mute hyperion/phantoms").formatted(Formatting.GRAY)));
                alert(Text.literal(" - ").formatted(Formatting.DARK_AQUA)
                        .append(Text.literal(" Custom F5, Ping display, Coordinate beacons").formatted(Formatting.GRAY)));
                alert(Text.literal(" - ").formatted(Formatting.DARK_AQUA)
                        .append(Text.literal(" Custom HUD, Clear lava, Redstone particle colors").formatted(Formatting.GRAY)));                                                
                alert(Text.literal(" - ").formatted(Formatting.DARK_AQUA)
                        .append(Text.literal(" Chat Event-based alerts, Timer for Moonglade Minigame").formatted(Formatting.GRAY)));
                alert(Text.literal(FORMAT1).formatted(Formatting.DARK_AQUA)
                        .append(Text.literal("/fa guide").formatted(Formatting.AQUA))
                        .append(Text.literal(FORMAT2).formatted(Formatting.DARK_AQUA))
                        .append(Text.literal("Show a more detailed guide.").formatted(Formatting.WHITE)));
                alert(Text.literal(" - ").formatted(Formatting.DARK_AQUA)
                        .append(Text.literal(" You can also use /fa help to see available commands").formatted(Formatting.GRAY))
                        .append(Text.literal(SPACE + "     or use the search function in /fa.").formatted(Formatting.GRAY)));
                alert(Text.literal(" Enjoy using FishyAddons! ").formatted(Formatting.DARK_AQUA));
        }
}