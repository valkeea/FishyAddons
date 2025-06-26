package me.valkeea.fishyaddons.gui;

public class SearchEntry {
    public final String name;
    public final String description;
    public final Runnable onSelect;

    public SearchEntry(String name, String description, Runnable onSelect) {
        this.name = name;
        this.description = description;
        this.onSelect = onSelect;
    }

    @Override
    public String toString() {
        return name;
    }
}