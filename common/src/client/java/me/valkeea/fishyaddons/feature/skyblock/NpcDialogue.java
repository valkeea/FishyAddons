package me.valkeea.fishyaddons.feature.skyblock;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import me.valkeea.fishyaddons.tool.RunDelayed;
import me.valkeea.fishyaddons.util.text.FromText;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;

public class NpcDialogue {
    private static final String TRAPPER = "\naccept the trapper's task to hunt the animal?";
    private static final String SELECT = "select an option: [";

    private static final long BASE_DELAY = 650;
    private static final int RANDOM_DELAY_RANGE = 151;
    private static final Random RANDOM = new Random();

    private static volatile String pendingCommand;
    private static final AtomicReference<ClickEvent.Custom> pendingCustom = new AtomicReference<>();
    private static final AtomicBoolean CANCELLED = new AtomicBoolean(false);

    private NpcDialogue() {}
    
    public static void checkForCommands(Component message, String clean) {
        if (!(clean.startsWith(SELECT) || clean.startsWith(TRAPPER))) return;

        var option = findAcceptButton(message);
        if (option != null) {

            var runnable = FromText.findCommand(option);
            var custom = FromText.findCustomClickEvent(option);
            var timeStamp = String.valueOf(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));

            if ((runnable != null)) {

                pendingCommand = runnable;
                pendingCustom.set(null);
                CANCELLED.set(false);

                long delay = BASE_DELAY + RANDOM.nextInt(RANDOM_DELAY_RANGE);

                RunDelayed.run(() -> {
                    if (CANCELLED.get()) return;
                    Minecraft.getInstance().player.connection.sendCommand(runnable.replace("/", ""));
                }, delay, runnable + "_" + timeStamp);

            } else if (custom != null) {
                
                pendingCommand = null;
                pendingCustom.set(custom);
                CANCELLED.set(false);

                long delay = BASE_DELAY + RANDOM.nextInt(RANDOM_DELAY_RANGE);

                RunDelayed.run(() -> {
                    if (CANCELLED.get()) return;
                    Minecraft.getInstance().player.connection.getConnection().send(
                        new ServerboundCustomClickActionPacket(custom.id(), custom.payload()));
                }, delay, custom.id() + "_" + timeStamp);
            }
        }
    }

    public static void onManualClick(ClickEvent event) {
        if ((event instanceof ClickEvent.RunCommand rc && rc.command().equals(pendingCommand))
            || (event instanceof ClickEvent.Custom c && c.equals(pendingCustom.get()))) {
            CANCELLED.set(true);
        }
    }

    private static Component findAcceptButton(Component text) {
        if (text.getString().contains("[") &&
            (text.getContents().toString().contains("§a") || FromText.findNodeWithColor(text, ChatFormatting.GREEN) != null)) {
            return text;
        }

        for (var sibling : text.getSiblings()) {
            var found = findAcceptButton(sibling);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    public static boolean enabled() {
        return Config.get(BooleanKey.ACCEPT_NPC);
    }    
}
