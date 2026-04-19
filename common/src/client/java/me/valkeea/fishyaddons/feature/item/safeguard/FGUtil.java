package me.valkeea.fishyaddons.feature.item.safeguard;

import java.util.Map;
import java.util.Set;

import me.valkeea.fishyaddons.tool.ItemData;
import me.valkeea.fishyaddons.tool.PlaySound;
import me.valkeea.fishyaddons.util.FishyNotis;
import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.api.StringKey;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.config.impl.ItemConfig;
import net.minecraft.item.ItemStack;

public class FGUtil {
    private FGUtil() {}

    public static boolean isKeyBound() {
        return !Config.get(StringKey.KEY_LOCK_SLOT).equals("NONE");
    }   

    public static boolean isSlotLocked(int idx) {
        return ItemConfig.isSlotLocked(idx);
    }

    public static boolean isSlotBound(int idx) {
        return ItemConfig.isSlotBound(idx);
    }

    public static boolean preventSlotClick(int idx) {
        return isKeyBound() && (isSlotLocked(idx) || isSlotBound(idx));
    }

    public static boolean isProtected(ItemStack stack) {
        String uuid = ItemData.extractUUID(stack);
        return !uuid.isEmpty() && ItemConfig.isProtected(uuid);
    }

    public static Set<String> getProtectedUUIDs() {
        return ItemConfig.getProtectedUUIDs().keySet();
    }

    public static Map<String, String> getProtectedUUIDsWithNames() {
        return ItemConfig.getProtectedUUIDs();
    }

    public static void removeFromFg(String uuid) {
        ItemConfig.removeUUID(uuid);
        GuiHandler.clearCache();
    }

    public static void addToFg(String uuid, net.minecraft.text.Text name) {
        ItemConfig.addUUID(uuid, name);
        GuiHandler.clearCache();
    }

    public static boolean audioFeedback() {
        return Config.get(BooleanKey.FG_AUDIO_FEEDBACK);
    }

    public static boolean chatFeedback() {
        return Config.get(BooleanKey.FG_CHAT_FEEDBACK);
    }

    public static boolean tooltipEnabled() {
        return Config.get(BooleanKey.FG_TOOLTIP);
    }

    public static void triggerProtection() {
        if (audioFeedback()) PlaySound.protectTrigger();
        if (chatFeedback()) FishyNotis.send("Item protected");
    }
}
