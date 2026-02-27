package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.Mouse;

@Mixin(Mouse.class)
public interface MouseAccessor {

    @Accessor("x")
    double getX();

    @Accessor("y")
    double getY();
}
