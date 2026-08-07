package me.valkeea.fishyaddons.feature.skyblock;

import java.util.HashMap;
import java.util.Map;

import me.valkeea.fishyaddons.api.skyblock.GameMode;
import me.valkeea.fishyaddons.tracker.PriceUtil;
import me.valkeea.fishyaddons.tracker.profit.TrackedItemData;
import me.valkeea.fishyaddons.util.Keyboard;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class ItemPrice {

    private static final Map<String, Double> seen = new HashMap<>(); // Render path
    private static final int MAX_SIZE = 50;

    public static Component getTooltipLine(ItemStack stack) {
        if (!(GameMode.skyblock() && Config.get(BooleanKey.ITEM_PRICE_TT))) return null;

        var name = stack.getHoverName().getString();
        if (name == null || name.isEmpty()) return null;

        double price = seen.computeIfAbsent(name, TrackedItemData::getPrice);
        if (seen.size() > MAX_SIZE) seen.clear();

        int count = stack.getCount();
        int relevantCutoff = 100;
        double highestValid = 2.1;

        if ((count == 1 && price < relevantCutoff) || price < highestValid) return null;
        boolean mul = stack.getCount() > 1 && Keyboard.isShiftDown();
        var value = PriceUtil.formatPrice(mul ? price * count : price);

        return Component.literal("§6" + value + (mul ? " §8(" + count + "x)" : ""));
    }

    private ItemPrice() {}
}
