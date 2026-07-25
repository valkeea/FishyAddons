package me.valkeea.fishyaddons.tool;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.item.ItemStack;

public class ItemData {
    private ItemData() {}

     /**
      * Extracts the UUID from an ItemStack's custom data, if present.
      * Returns an empty string if no UUID is found or if the custom data is missing.
      */
    public static String extractUUID(ItemStack stack) {
        var component = stack.get(DataComponents.CUSTOM_DATA);
        if (component == null)  return "";
        var uuidElement = component.copyTag().get("uuid");
        return uuidElement instanceof StringTag uuid ? uuid.asString().orElse("") : "";
    }    

}
