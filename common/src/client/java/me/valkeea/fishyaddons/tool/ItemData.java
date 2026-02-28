package me.valkeea.fishyaddons.tool;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtString;

public class ItemData {
    private ItemData() {}

     /**
      * Extracts the UUID from an ItemStack's custom data, if present.
      * Returns an empty string if no UUID is found or if the custom data is missing.
      */
    public static String extractUUID(ItemStack stack) {
        var component = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (component == null)  return "";
        var uuidElement = component.copyNbt().get("uuid");
        return uuidElement instanceof NbtString uuid ? uuid.asString().orElse("") : "";
    }    

}
