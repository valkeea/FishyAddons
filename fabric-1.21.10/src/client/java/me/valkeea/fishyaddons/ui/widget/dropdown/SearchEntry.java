package me.valkeea.fishyaddons.ui.widget.dropdown;

public class SearchEntry {
    public final String name;
    public final String description;
    public final Runnable onSelect;
    public final String displayName;

    public SearchEntry(String name, String description, String displayName,Runnable onSelect) {
        this.name = name;
        this.description = description;
        this.displayName = displayName;
        this.onSelect = onSelect;
    }

    @Override
    public String toString() {
        return name;
    }
}
