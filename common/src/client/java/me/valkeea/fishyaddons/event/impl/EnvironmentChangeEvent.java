package me.valkeea.fishyaddons.event.impl;

import me.valkeea.fishyaddons.api.skyblock.GameMode;
import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas;
import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas.Island;
import me.valkeea.fishyaddons.event.BaseEvent;

public class EnvironmentChangeEvent extends BaseEvent {
    public final Island newIsland;
    public final boolean isInSkyblock;

    public EnvironmentChangeEvent(Island newIsland) {
        this.newIsland = newIsland;
        this.isInSkyblock = GameMode.skyblock();
    }

    public EnvironmentChangeEvent(boolean isInSkyblock) {
        this.newIsland = SkyblockAreas.getIsland();
        this.isInSkyblock = isInSkyblock;
    }

    public boolean isSkyblock() {
        return isInSkyblock;
    }

    public Island newIsland() {
        return newIsland;
    }
}
