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
        PingDisplay pingDisplay = new PingDisplay();
        TimerDisplay timerDisplay = new TimerDisplay();
        TitleDisplay titleDisplay = new TitleDisplay();
        PetDisplay petDisplay = new PetDisplay();
        TrackerDisplay trackerDisplay = new TrackerDisplay();
        SearchHudElement searchHudElement = new SearchHudElement();
        CakeDisplay centuryCakeDisplay = new CakeDisplay();
        InfoDisplay infoDisplay = InfoDisplay.getInstance();
        ScDisplay scDisplay = ScDisplay.getInstance();

        register(pingDisplay);
        register(timerDisplay);
        register(titleDisplay);
        register(petDisplay);
        register(trackerDisplay);
        register(searchHudElement);
        register(centuryCakeDisplay);
        register(infoDisplay);
        register(scDisplay);

        pingDisplay.register();
        timerDisplay.register();
        titleDisplay.register();
        petDisplay.register();
        trackerDisplay.register();
        searchHudElement.register();
        centuryCakeDisplay.register();
        infoDisplay.register();
        scDisplay.register();

        me.valkeea.fishyaddons.handler.ItemSearchOverlay searchOverlay = me.valkeea.fishyaddons.handler.ItemSearchOverlay.getInstance();
        searchOverlay.setSearchHudElement(searchHudElement);        
    }
}