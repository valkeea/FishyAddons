package me.valkeea.fishyaddons.ui.widget;

import java.util.function.Consumer;
import java.util.function.Function;

import me.valkeea.fishyaddons.util.Keyboard;
import me.valkeea.fishyaddons.vconfig.ui.widget.FaButton;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

public class ListeningWidget extends FaButton {
    private boolean listening = false;
    private final Consumer<String> onKeySet;
    private String keyName;
    private final Function<String, net.minecraft.network.chat.Component> labelProvider;

    public ListeningWidget(int x, int y, int width, int height, String initialKey, Consumer<String> onKeySet,
        Function<String, net.minecraft.network.chat.Component> labelProvider
    ) {
        super(x, y, width, height, labelProvider.apply(initialKey), btn -> {
            ListeningWidget widget = (ListeningWidget) btn;
            widget.listening = true;
            widget.setMessage(net.minecraft.network.chat.Component.literal("Press any key...").withStyle(s -> s.withColor(0xFFFFFF80)));
        });
        this.keyName = initialKey;
        this.onKeySet = onKeySet;
        this.labelProvider = labelProvider;
        this.setMessage(labelProvider.apply(keyName));
    }
    
    @Override
    public boolean keyPressed(KeyEvent input) {
        if (listening) {
            String newKey = Keyboard.getGlfwKeyName(input.key());
            if (newKey != null) {
                keyName = newKey;
                onKeySet.accept(keyName);
                this.setMessage(labelProvider.apply(keyName));
            } else {
                this.setMessage(labelProvider.apply(keyName));
            }
            listening = false;
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (listening && click.button() == 0 && this.isMouseOver(click.x(), click.y())) {
            keyName = "NONE";
            onKeySet.accept(keyName);
            this.setMessage(labelProvider.apply(keyName));
            listening = false;
            return true;
        }
        return super.mouseClicked(click, doubled);
    }
}
