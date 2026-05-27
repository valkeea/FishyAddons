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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class GoalButton  {
    private static IndexedButton btn = null;
    private GoalButton() {}
    
    private static final String TITLE = "Recipe";
    private static final List<Component> tooltip = List.of(
        Component.literal("[Add as a collection goal]").withStyle(s -> s.withColor(FishyMode.getThemeColor())),
        Component.literal("Please open any nested crafts").withStyle(s -> s.withColor(0xFFAAAAAA)),
        Component.literal("to fully calculate the recipe!").withStyle(s -> s.withColor(0xFFAAAAAA))
    );

    private static void init() {
        FaEvents.MOUSE_CLICK.register(GoalButton::setup, EventPriority.LOW, EventPhase.POST);
    }

    private static void create(ContainerScreen gcs) {
        init();
        btn = new IndexedButton(
            gcs, (short) 35, RecipeScanner::addAsGoal,
            Identifier.fromNamespaceAndPath("fishyaddons", "icon.png"), TITLE
        );
    }

    private static void setup(MouseClickEvent e) {

        var s = Minecraft.getInstance().screen;
        if (btn != null && s instanceof ContainerScreen && btn.mouseClicked(e.click.x(), e.click.y())) {

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

    public static void render(GuiGraphicsExtractor context, ContainerScreen gcs, int mouseX, int mouseY) {

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
                gcs.getFont(),
                tooltip,
                mouseX,
                mouseY,
                FishyMode.getThemeColor(),
                1.0F
            );
        }
    }
}
