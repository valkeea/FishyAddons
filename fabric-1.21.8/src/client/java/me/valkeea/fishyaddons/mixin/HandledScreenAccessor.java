package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {
    @Invoker("onMouseClick")
    void callOnMouseClick(Slot slot, int slotId, int button, SlotActionType actionType);

    @Accessor("focusedSlot")
    Slot getFocusedSlot();

    @Accessor("backgroundWidth")
    int getBackgroundWidth();

    @Accessor("backgroundHeight")
    int getBackgroundHeight();
    
    @Accessor("x")
    int getX();
    
    @Accessor("y")
    int getY();
}