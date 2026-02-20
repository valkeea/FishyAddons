package me.valkeea.fishyaddons.hud.elements.simple;

import me.valkeea.fishyaddons.hud.base.SimpleTextElement;
import me.valkeea.fishyaddons.tool.FishyMode;
import me.valkeea.fishyaddons.util.text.Enhancer;
import net.minecraft.text.Text;

public class TitleDisplay extends SimpleTextElement {
    private static String title = null;
    private static int titlecolor = FishyMode.getThemeColor();
    private static long alertStartTime = 0L;
    private static final long DURATION = 2500L;

        public TitleDisplay() {
            super(
                "titleHud",
                "Title Display",
                "Alert Title",
                300, 120,
                40,
                titlecolor,
                false,
                false
            );
        }

    public static void setTitle(String t, int color) {
        title = t;
        titlecolor = color;
        alertStartTime = System.currentTimeMillis();
    }

    @Override
    public int getTextAlignment() {
        return 1;
    }    

    @Override
    protected boolean shouldRender() {
        return title != null && !title.isEmpty()
            && (System.currentTimeMillis() - alertStartTime < DURATION);
    }

    @Override
    protected int getTextColor() {
        return titlecolor;
    }

    @Override
    protected Text getText() {
        return title == null ? Text.empty() : Enhancer.parseFormattedText(title);
    }
}
