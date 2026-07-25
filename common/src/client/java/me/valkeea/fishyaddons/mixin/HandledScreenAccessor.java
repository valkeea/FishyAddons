package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

@Mixin(AbstractContainerScreen.class)
public interface HandledScreenAccessor {
    @Invoker("slotClicked")
    void callOnMouseClick(Slot slot, int slotId, int button, ContainerInput containerInput);

    @Accessor("hoveredSlot")
    Slot getHoveredSlot();

    @Accessor("imageWidth")
    int getImageWidth();

    @Accessor("imageHeight")
    int getImageHeight();
    
    @Accessor("leftPos")
    int getX();
    
    @Accessor("topPos")
    int getY();
}
