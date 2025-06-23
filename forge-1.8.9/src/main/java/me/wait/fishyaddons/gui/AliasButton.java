package me.wait.fishyaddons.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

// Better handling of aliases in the main list GUI.
public class AliasButton extends GuiButton {
    public final String alias;

    public AliasButton(int id, int x, int y, int width, int height, String displayText, String alias) {
        super(id, x, y, width, height, displayText);
        this.alias = alias;
    }
}
