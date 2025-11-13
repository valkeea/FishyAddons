package me.valkeea.fishyaddons.util.text;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class ChatButton {
    private ChatButton() {}

    public static MutableText create(String command, String buttonText) {
        return Text.literal(" [")
            .styled(style -> style.withColor(0xFF808080))
            .append((Text.literal(buttonText))
            .styled(style -> style.withClickEvent(
                new net.minecraft.text.ClickEvent.RunCommand(command)
            ).withColor(me.valkeea.fishyaddons.tool.FishyMode.getCmdColor())))
            .append(Text.literal("]").styled(style -> style.withColor(0xFF808080)));
    }
}
