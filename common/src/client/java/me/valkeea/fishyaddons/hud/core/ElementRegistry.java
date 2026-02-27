package me.valkeea.fishyaddons.hud.core;

import java.util.ArrayList;
import java.util.List;

import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.feature.qol.ItemSearchOverlay;
import me.valkeea.fishyaddons.hud.base.InteractiveHudElement;
import me.valkeea.fishyaddons.hud.elements.custom.HealthDisplay;
import me.valkeea.fishyaddons.hud.elements.custom.InfoDisplay;
import me.valkeea.fishyaddons.hud.elements.custom.ScDisplay;
import me.valkeea.fishyaddons.hud.elements.custom.SkillXpDisplay;
import me.valkeea.fishyaddons.hud.elements.interactive.CollectionDisplay;
import me.valkeea.fishyaddons.hud.elements.interactive.ProfitDisplay;
import me.valkeea.fishyaddons.hud.elements.segmented.EffectDisplay;
import me.valkeea.fishyaddons.hud.elements.segmented.NetworkDisplay;
import me.valkeea.fishyaddons.hud.elements.segmented.TimerDisplay;
import me.valkeea.fishyaddons.hud.elements.simple.CakeDisplay;
import me.valkeea.fishyaddons.hud.elements.simple.PetDisplay;
import me.valkeea.fishyaddons.hud.elements.simple.TitleDisplay;
import me.valkeea.fishyaddons.hud.ui.SearchHudElement;
import me.valkeea.fishyaddons.hud.ui.UIFeedback;

public class ElementRegistry {
    private ElementRegistry() {}
    private static final List<HudElement> ELEMENTS = new ArrayList<>();
    private static final List<InteractiveHudElement> INTERACTIVE_ELEMENTS = new ArrayList<>();

    public static void register(HudElement element) {
        if (element != null) {
            ELEMENTS.add(element);
            if (element instanceof InteractiveHudElement interactive) {
                INTERACTIVE_ELEMENTS.add(interactive);
            }
        }
    }

    public static List<HudElement> getElements() {
        return ELEMENTS;
    }
    
    public static List<InteractiveHudElement> getInteractiveElements() {
        return INTERACTIVE_ELEMENTS;
    }

    public static List<HudElement> getConfigurable() {
        List<HudElement> configurable = new ArrayList<>();
        for (HudElement element : ELEMENTS) {
            if (element != null && element.isConfigurable()) {
                configurable.add(element);
            }
        }
        return configurable;
    }

    public static void init() {

        var searchHudElement = SearchHudElement.getInstance();
        var searchOverlay = ItemSearchOverlay.getInstance();

        register(searchHudElement);
        searchOverlay.setSearchField(searchHudElement);

        register(new NetworkDisplay());
        register(new TimerDisplay());
        register(new TitleDisplay());
        register(new PetDisplay());
        register(new CakeDisplay());
        register(new EffectDisplay());
        register(new SkillXpDisplay());
        register(new HealthDisplay());

        register(CollectionDisplay.getInstance());
        register(ProfitDisplay.getInstance());
        register(ScDisplay.getInstance());
        register(InfoDisplay.getInstance());
        register(UIFeedback.getInstance());        

        FaEvents.HUD_RENDER.register(event -> {

            if (ScreenRenderContext.isInEditMode()) return;
            
            for (HudElement element : ELEMENTS) {
                if (element != null) {
                    if (element instanceof InteractiveHudElement interactive 
                        && ScreenRenderContext.shouldSkipInHudRender(interactive)) {
                        continue;
                    }
                    element.render(event.getContext(), event.getClient(), 0, 0);
                }
            }
        });
    }
}
