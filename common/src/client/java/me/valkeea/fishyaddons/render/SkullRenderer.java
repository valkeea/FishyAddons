package me.valkeea.fishyaddons.render;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.SkullBlock;

public final class SkullRenderer {

    public static boolean isSkull(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof AbstractSkullBlock;
    }

    public static void applyWornHead(LivingEntityRenderState state, ItemStack stack) {
        var type = skullType(stack);
        state.wornHeadType = type;
        state.wornHeadProfile = stack.get(DataComponents.PROFILE);
        state.headItem.clear();
    }

    private static SkullBlock.Type skullType(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem item && item.getBlock() instanceof AbstractSkullBlock skull) {
            return skull.getType();
        }
        throw new IllegalArgumentException("Expected a skull item");
    }
}
