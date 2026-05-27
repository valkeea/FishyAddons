package me.valkeea.fishyaddons.util.text;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class ChatButton {
    private ChatButton() {}

    public static MutableComponent create(String command, String buttonText) {
        return Component.literal(" [")
            .withStyle(style -> style.withColor(0xFF808080))
            .append((Component.literal(buttonText))
            .withStyle(style -> style.withClickEvent(
                new net.minecraft.network.chat.ClickEvent.RunCommand(command)
            ).withColor(me.valkeea.fishyaddons.tool.FishyMode.getCmdColor())))
            .append(Component.literal("]").withStyle(style -> style.withColor(0xFF808080)));
    }
}
