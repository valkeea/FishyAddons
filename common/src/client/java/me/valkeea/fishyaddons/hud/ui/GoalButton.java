package me.valkeea.fishyaddons.hud.ui;

import java.util.List;

import me.valkeea.fishyaddons.event.EventPhase;
import me.valkeea.fishyaddons.event.EventPriority;
import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.event.impl.MouseClickEvent;
import me.valkeea.fishyaddons.tool.FishyMode;
import me.valkeea.fishyaddons.tracker.collection.CollectionTracker;
import me.valkeea.fishyaddons.tracker.collection.RecipeScanner;
import me.valkeea.fishyaddons.util.ContainerScanner;
import me.valkeea.fishyaddons.vconfig.ui.render.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class GoalButton  {
    private static IndexedButton btn = null;
    private GoalButton() {}
    
    private static final String TITLE = "Recipe";
    private static final List<Text> tooltip = List.of(
        Text.literal("[Add as a collection goal]").styled(s -> s.withColor(FishyMode.getThemeColor())),
        Text.literal("Please open any nested crafts").styled(s -> s.withColor(0xFFAAAAAA)),
        Text.literal("to fully calculate the recipe!").styled(s -> s.withColor(0xFFAAAAAA))
    );

    private static void init() {
        FaEvents.MOUSE_CLICK.register(GoalButton::setup, EventPriority.LOW, EventPhase.POST);
    }

    private static void create(GenericContainerScreen gcs) {
        init();
        btn = new IndexedButton(
            gcs, (short) 35, RecipeScanner::addAsGoal,
            Identifier.of("fishyaddons", "icon.png"), TITLE
        );
    }

    private static void setup(MouseClickEvent e) {

        var s = MinecraftClient.getInstance().currentScreen;
        if (btn != null && s instanceof GenericContainerScreen && btn.mouseClicked(e.click.x(), e.click.y())) {

            e.setConsumed(true);
            
            UIFeedback.getInstance().set(
                "Added!",
                100,
                (int)e.click.x() + 20,
                (int)e.click.y() - 20,
                null
            );
        }
    }

    public static void render(DrawContext context, GenericContainerScreen gcs, int mouseX, int mouseY) {

        if (!ContainerScanner.current().endsWith(TITLE) ||
            !CollectionTracker.isEnabled()) {
            if (btn != null) btn = null;
            return;
        }

        if (btn == null) create(gcs);
        if (!btn.isVisible(gcs.getTitle().getString())) return;

        btn.render(context, mouseX, mouseY);

        if (btn.isMouseOver(mouseX, mouseY)) {
            RenderUtils.preview(
                context,
                gcs.getTextRenderer(),
                tooltip,
                mouseX,
                mouseY,
                FishyMode.getThemeColor(),
                1.0F
            );
        }
    }
}
