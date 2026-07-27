package me.valkeea.fishyaddons.util.text;

public enum Glyphs {
    TFISH("\uE02A"),
    TREASURE("\uE025"),
    MFIND("\uE01A");

    private final String character;

    Glyphs(String character) {
        this.character = character;
    }

    public String toString() {
        return character;
    }

    public boolean isKnownGlyph(String s) {
        for (Glyphs glyph : Glyphs.values()) {
            if (glyph.character.equals(s)) {
                return true;
            }
        }
        return false;
    }
}
