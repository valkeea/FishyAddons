package me.valkeea.fishyaddons.hud;

import java.util.ArrayList;
import java.util.List;

public class ElementRegistry {
    private ElementRegistry() {}
    private static final List<HudElement> ELEMENTS = new ArrayList<>();

    public static void register(HudElement element) {
        ELEMENTS.add(element);
    }

    public static List<HudElement> getElements() {
        return ELEMENTS;
    }

    public static void init() {
        NetworkDisplay networkDisplay = new NetworkDisplay();
        TimerDisplay timerDisplay = new TimerDisplay();
        TitleDisplay titleDisplay = new TitleDisplay();
        PetDisplay petDisplay = new PetDisplay();
        SearchHudElement searchHudElement = new SearchHudElement();
        CakeDisplay centuryCakeDisplay = new CakeDisplay();
        SkillXpDisplay skillXpDisplay = new SkillXpDisplay();
        HealthDisplay healthDisplay = new HealthDisplay();        
        TrackerDisplay trackerDisplay = TrackerDisplay.getInstance();
        InfoDisplay infoDisplay = InfoDisplay.getInstance();
        ScDisplay catchHistogramDisplay = ScDisplay.getInstance();

        register(networkDisplay);
        register(timerDisplay);
        register(titleDisplay);
        register(petDisplay);
        register(trackerDisplay);
        register(searchHudElement);
        register(centuryCakeDisplay);
        register(infoDisplay);
        register(catchHistogramDisplay);
        register(skillXpDisplay);
        register(healthDisplay);

        networkDisplay.register();
        timerDisplay.register();
        titleDisplay.register();
        petDisplay.register();
        trackerDisplay.register();
        searchHudElement.register();
        centuryCakeDisplay.register();
        infoDisplay.register();
        catchHistogramDisplay.register();
        skillXpDisplay.register();
        healthDisplay.register();

        me.valkeea.fishyaddons.handler.ItemSearchOverlay searchOverlay = me.valkeea.fishyaddons.handler.ItemSearchOverlay.getInstance();
        searchOverlay.setSearchHudElement(searchHudElement);        
    }
}