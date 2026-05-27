package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.entity.player.Inventory;

@Mixin(Inventory.class)
public interface PlayerInventoryAccessor {
    @Accessor("selected")
    int getSelectedSlot();
}
