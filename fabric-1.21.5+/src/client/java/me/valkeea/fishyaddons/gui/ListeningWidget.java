package me.valkeea.fishyaddons.gui;

import me.valkeea.fishyaddons.util.KeyUtil;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import java.util.function.Consumer;
import java.util.function.Function;

public class ListeningWidget extends ButtonWidget {
    private boolean listening = false;
    private final Consumer<String> onKeySet;
    private String keyName;
    private final Function<String, Text> labelProvider;

    public ListeningWidget(int x, int y, int width, int height, String initialKey, Consumer<String> onKeySet, Function<String, Text> labelProvider) {
        super(x, y, width, height, labelProvider.apply(initialKey), btn -> {
            ListeningWidget widget = (ListeningWidget) btn;
            widget.listening = true;
            widget.setMessage(Text.literal("Press any key...").styled(s -> s.withColor(0xFFFFFF80)));
        }, DEFAULT_NARRATION_SUPPLIER);
        this.keyName = initialKey;
        this.onKeySet = onKeySet;
        this.labelProvider = labelProvider;
        this.setMessage(labelProvider.apply(keyName));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listening) {
            String newKey = KeyUtil.getGlfwKeyName(keyCode);
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
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}