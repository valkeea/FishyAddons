package me.valkeea.fishyaddons.feature.skyblock;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import me.valkeea.fishyaddons.tool.RunDelayed;
import me.valkeea.fishyaddons.util.text.FromText;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class NcpDialogue {
    private static final String TRAPPER = "\naccept the trapper's task to hunt the animal?";
    private static final String SELECT = "select an option: [";

    private static final long BASE_DELAY = 650;
    private static final int RANDOM_DELAY_RANGE = 151;
    private static final Random RANDOM = new Random();
    private NcpDialogue() {}
    
    public static boolean checkForCommands(Component message, String clean) {
        if (!(clean.startsWith(SELECT) || clean.startsWith(TRAPPER))) return false;

        var option = findAcceptButton(message);
        if (option != null) {

            var runnable = FromText.findCommand(option);
            var timeStamp = String.valueOf(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
            
            if ((runnable != null)) {
                long delay = BASE_DELAY + RANDOM.nextInt(RANDOM_DELAY_RANGE);
                RunDelayed.run(() -> Minecraft.getInstance().player.connection.sendCommand(
                    runnable.replace("/", "")),
                    delay, runnable + "_" + timeStamp
                );
            }
        }

        return false;
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
