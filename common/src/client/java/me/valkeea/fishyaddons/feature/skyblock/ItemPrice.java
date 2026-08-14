package me.valkeea.fishyaddons.feature.skyblock;

import java.util.HashMap;
import java.util.Map;

import me.valkeea.fishyaddons.api.skyblock.GameMode;
import me.valkeea.fishyaddons.tracker.PriceUtil;
import me.valkeea.fishyaddons.tracker.profit.InventoryTracker;
import me.valkeea.fishyaddons.tracker.profit.TrackedItemData;
import me.valkeea.fishyaddons.util.Keyboard;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ItemPrice {
    private static final Map<String, Double> seen = new HashMap<>();
    private static final int MAX_SIZE = 50;

    /**
     * Get the price of an item stack, formatted for tooltip display
     */
    public static Component getTooltipLine(ItemStack stack) {
        if (!(GameMode.skyblock() && Config.get(BooleanKey.ITEM_PRICE_TT))) return null;

        var name = stack.getHoverName().getString();
        if (name == null || name.isEmpty()) return null;

        var count = stack.getCount();
        name = handleSpecialCases(name, stack, count);

        double price = seen.computeIfAbsent(name, TrackedItemData::getPrice);
        if (seen.size() > MAX_SIZE) seen.clear();

        if (invalidPrice(count, price)) return null;

        boolean mul = stack.getCount() > 1 && Keyboard.isShiftDown();
        var value = PriceUtil.formatPrice(mul ? price * count : price);

        return Component.literal("§6" + value + (mul ? " §8(" + count + "x)" : ""));
    }

    private static boolean invalidPrice(int count, double price) {
        int relevantCutoff = 100;
        double highestValid = 2.1;
        return (count == 1 && price < relevantCutoff) || price < highestValid;
    }

    /**
     * Workaround for now to manually add rarity to the name string when necessary,
     * or extract the enchantment from lore.
     */
    private static String handleSpecialCases(String name, ItemStack stack, int count) {

        if (stack.getItem() == Items.ENCHANTED_BOOK) {

            var lore = stack.get(DataComponents.LORE);
            if (lore == null) return name;

            var bookInfo = PriceUtil.getBookInfo(lore);
            if (bookInfo != null) {
                name = bookInfo.name();
            }
        }

        if (name.startsWith("[Lvl") || name.contains("Exp Boost")) {
            var rarity = InventoryTracker.getRarityTier(stack.getHoverName());
            name = rarity + name;
        }
        
        if (name.endsWith(" x" + count)) {
            name = name.substring(0, name.lastIndexOf(" x" + count)).trim();
        }

        return name;
    }


    private ItemPrice() {}
}
