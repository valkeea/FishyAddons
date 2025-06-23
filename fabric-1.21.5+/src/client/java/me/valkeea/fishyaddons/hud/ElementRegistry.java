package me.valkeea.fishyaddons.hud;

import java.util.List;
import java.util.ArrayList;

public class ElementRegistry {
    private ElementRegistry() {}
    private static final List<HudElement> ELEMENTS = new ArrayList<>();

    public static void register(HudElement element) {
        ELEMENTS.add(element);
    }

    public static List<HudElement> getElements() {
        return ELEMENTS;
    }
}