package me.valkeea.fishyaddons.feature.skyblock;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.tool.RunDelayed;
import me.valkeea.fishyaddons.util.text.FromText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class NcpDialogue {
    private static final long BASE_DELAY = 650;
    private static final int RANDOM_DELAY_RANGE = 151;
    private static final Random RANDOM = new Random();
    private NcpDialogue() {}
    
    public static boolean checkForCommands(Text message) {

        var option = findAcceptButton(message);
        if (option != null) {

            var runnable = FromText.findCommand(option);
            var timeStamp = String.valueOf(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
            
            if ((runnable != null)) {
                long delay = BASE_DELAY + RANDOM.nextInt(RANDOM_DELAY_RANGE);
                RunDelayed.run(() -> MinecraftClient.getInstance().player.networkHandler.sendChatCommand(
                    runnable.replace("/", "")),
                    delay, runnable + "_" + timeStamp
                );
            }
        }

        return false;
    }

    private static Text findAcceptButton(Text text) {

        if (text.getString().contains("[") &&
            (text.getContent().toString().contains("§a") || FromText.findNodeWithColor(text, Formatting.GREEN) != null)) {
            return text;
        }

        for (Text sibling : text.getSiblings()) {
            Text found = findAcceptButton(sibling);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    public static boolean enabled() {
        return FishyConfig.getState(me.valkeea.fishyaddons.config.Key.ACCEPT_NPC, false);
    }    
}
