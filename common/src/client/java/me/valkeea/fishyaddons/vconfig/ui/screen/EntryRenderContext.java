package me.valkeea.fishyaddons.vconfig.ui.screen;

import java.util.List;
import me.valkeea.fishyaddons.vconfig.ui.model.VCEntry;

public record EntryRenderContext(
    int entryX,
    int startY,
    int endY,
    int scrollOffset,
    List<VCEntry> filteredEntries
) {
}
