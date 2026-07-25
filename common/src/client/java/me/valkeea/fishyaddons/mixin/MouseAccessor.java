package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.MouseHandler;

@Mixin(MouseHandler.class)
public interface MouseAccessor {

    @Accessor("xpos")
    double getX();

    @Accessor("ypos")
    double getY();
}
