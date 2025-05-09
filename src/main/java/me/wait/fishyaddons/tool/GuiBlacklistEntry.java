package me.wait.fishyaddons.tool;

import java.util.List;

public class GuiBlacklistEntry {
    public final List<String> identifiers;
    public boolean enabled;
    public final boolean checkTitle;

    public GuiBlacklistEntry(List<String> identifiers, boolean enabled, boolean checkTitle) {
        this.identifiers = identifiers;
        this.enabled = enabled;
        this.checkTitle = checkTitle;
    }
}
