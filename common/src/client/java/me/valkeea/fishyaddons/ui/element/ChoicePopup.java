package me.valkeea.fishyaddons.ui.element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import me.valkeea.fishyaddons.tool.FishyMode;
import me.valkeea.fishyaddons.ui.GuiUtil;
import me.valkeea.fishyaddons.ui.list.DynamicListScreen;
import me.valkeea.fishyaddons.util.text.Color;
import me.valkeea.fishyaddons.vconfig.ui.widget.FaButton;
import me.valkeea.fishyaddons.vconfig.ui.widget.FaTextField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class ChoicePopup extends AbstractContainerWidget {
    private static final int PAD = 10;
    private static final int HPAD = 20;
    private static final int BTNH = 20;

    private final List<AbstractWidget> widgets = new ArrayList<>();
    
    private final Runnable onNo;
    private final Runnable onYes;
    private final List<String> msg;
    private final Component yesBtnText;
    private final Component noBtnText;
    private final int centerX;
    private final int centerY;

    private boolean focused;
    private Font tr;

    private int theme;
    private int fill;

    public ChoicePopup(String msg, Component yesBtnText, Component noBtnText, Runnable onYes, Runnable onNo, FaTextField tf, Screen screen) {
        super(0, 0, 0, 0, Component.empty(), new ScrollbarSettings(null, null, null, 3, 32, 24 / 2, true));
        this.centerX = screen.width / 2;
        this.centerY = screen.height / 2;
        this.msg = Arrays.asList(msg.split("\n"));
        this.yesBtnText = yesBtnText;
        this.noBtnText = noBtnText;        
        this.onYes = onYes == null ? () -> {} : onYes;
        this.onNo = onNo == null ? () -> {} : onNo;
        this.init(tf);
    }

    private void init(@Nullable FaTextField tf) {
        this.setPosition(centerX, centerY);
        tr = Minecraft.getInstance().font;
        var textWidth = Math.max(
            tr.width(yesBtnText) + (noBtnText == null ? 0 : tr.width(noBtnText)),
            msg.stream().mapToInt(tr::width).max().orElse(0)
        );

        boolean hasField = tf != null;

        int btnW = Math.max(tr.width(yesBtnText), tr.width(noBtnText)) + PAD;
        int tH = msg.size() * tr.lineHeight;

        width = Math.max(textWidth, btnW * 2) + HPAD * 2;
        height = tH + BTNH + PAD * 3;
        theme = FishyMode.getThemeColor();
        fill = Color.desaturateAndDarken(theme, 0.5f);

        if (hasField) height += BTNH + PAD;

        int y = centerY - height / 2 + tH + PAD + 15;
        if (hasField) {

            tf.setPosition(centerX - width / 2 + HPAD, y);
            tf.setSize(width - HPAD * 2, BTNH);
            y += BTNH + PAD;
            widgets.add(tf);
            this.setFocused(tf);
        }
        
        var yesBtn = new FaButton(centerX - btnW, y, btnW, BTNH,
            yesBtnText, b -> onYes.run());
        var noBtn = new FaButton(centerX, y, btnW, BTNH,
            noBtnText, b -> onNo.run());

        widgets.add(yesBtn);
        widgets.add(noBtn);
    }

    @Override 
    public void extractWidgetRenderState(GuiGraphicsExtractor gge, int mX, int mY, float delta) {
        int fillW = width / 2;
        int fillH = height / 2 + 1;

        gge.fill(centerX - fillW, centerY - fillH, centerX + fillW, centerY + fillH, fill);
        GuiUtil.wireBox(gge, centerX - fillW, centerY - fillH, width, height, theme);

        int tH = centerY - fillH + 15;
        for (int i = 0; i < msg.size(); i++) gge.centeredText(tr, msg.get(i), centerX, tH + i * tr.lineHeight, theme);
        for (var w : widgets) w.extractRenderState(gge, mX, mY, delta);
    }

    @Override
    public List<AbstractWidget> children() {
        return widgets;
    }

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {/** Access */}

    @Override
    public int contentHeight() {
        return height;
    }

    @Override
    public boolean isFocused() {
        return focused;
    }
    
    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        for (var w : widgets) {
            if (w.isMouseOver(click.x(), click.y())) {
                if (w.isFocused()) return w.mouseClicked(click, doubled);
                else {
                    w.setFocused(true);
                    return w.mouseClicked(click, doubled);
                }
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        return widgets.stream().anyMatch(w -> w.keyPressed(input));
    }

    @Override
    public boolean charTyped(CharacterEvent chr) {
        return widgets.stream().anyMatch(w -> w.charTyped(chr));
    }

    @Override public int getX() { return centerX - width / 2;}
    @Override public int getY() { return centerY - height / 2;}
    @Override public int getWidth() { return width;}
    @Override public int getHeight() { return height;}

    public static void set(String msg, Component yesText, Runnable onYes, Component noText, Runnable onNo, @Nullable FaTextField field, DynamicListScreen instance) {
        var popup = new ChoicePopup(msg, yesText, noText, onYes, onNo, field, instance);
        instance.setOverlay(popup);
    }
}
