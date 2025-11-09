package me.valkeea.fishyaddons.render;

import net.minecraft.util.math.BlockPos;

public interface IBeaconData {
    BlockPos getPos();
    int getColor();
    String getLabel();
    long getSetTime();

    default boolean fillBlock() {
        return true;
    }

    default boolean noDepth() {
        return true;
    }
}
