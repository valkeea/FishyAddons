package me.valkeea.fishyaddons.ui.widget;

import java.util.function.Consumer;
import java.util.function.Function;

import me.valkeea.fishyaddons.util.Keyboard;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

public class ListeningWidget extends FaButton {
    private boolean listening = false;
    private final Consumer<String> onKeySet;
    private String keyName;
    private final Function<String, Text> labelProvider;

    public ListeningWidget(int x, int y, int width, int height, String initialKey, Consumer<String> onKeySet, Function<String, Text> labelProvider) {
        super(x, y, width, height, labelProvider.apply(initialKey), btn -> {
            ListeningWidget widget = (ListeningWidget) btn;
            widget.listening = true;
            widget.setMessage(Text.literal("Press any key...").styled(s -> s.withColor(0xFFFFFF80)));
        });
        this.keyName = initialKey;
        this.onKeySet = onKeySet;
        this.labelProvider = labelProvider;
        this.setMessage(labelProvider.apply(keyName));
    }
    
    @Override
    public boolean keyPressed(KeyInput input) {
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
    public boolean mouseClicked(Click click, boolean doubled) {
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
