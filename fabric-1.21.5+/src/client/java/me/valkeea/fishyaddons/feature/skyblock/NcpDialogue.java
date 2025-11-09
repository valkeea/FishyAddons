package me.valkeea.fishyaddons.feature.skyblock;

import java.util.Random;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.tool.RunDelayed;
import me.valkeea.fishyaddons.util.text.FromText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class NcpDialogue {
    private static final Random RANDOM = new Random();
    private NcpDialogue() {}
    
    public static boolean checkForCommands(Text message) {

        Text option = findAcceptButton(message);

        if (option != null) {
            String runnable = FromText.findCommand(option);
            
            if ((runnable != null)) {
                long delay = (long)850 + RANDOM.nextInt(151);
                RunDelayed.run(() -> MinecraftClient.getInstance().player.networkHandler.sendChatCommand(
                    runnable.replace("/", "")),
                    delay, runnable);
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
