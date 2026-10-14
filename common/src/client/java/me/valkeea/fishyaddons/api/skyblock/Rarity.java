package me.valkeea.fishyaddons.api.skyblock;

import org.jetbrains.annotations.Nullable;

public enum Rarity {
    COMMON("§f", "white"),
    UNCOMMON("§a", "green"),
    RARE("§9", "blue"),
    EPIC("§5", "dark_purple"),
    LEGENDARY("§6", "gold"),
    MYTHIC("§d", "light_purple"),
    DIVINE("§b", "aqua"),
    SPECIAL("§c", "red"),
    ULTIMATE("§9", "dark_red");   

    private final String code;
    private final String color;

    Rarity(String code, String color) {
        this.code = code;
        this.color = color;
    }

    public String getColorCode() {
        return code;
    }

    public String getTextColor() {
        return color;
    }

    public static @Nullable Rarity fromTextColor(String color) {
        for (var rarity : Rarity.values()) {
            if (rarity.getTextColor().equals(color)) {
                return rarity;
            }
        }
        return null;
    }
}
