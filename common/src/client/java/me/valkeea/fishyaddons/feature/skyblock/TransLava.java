package me.valkeea.fishyaddons.feature.skyblock;

import me.valkeea.fishyaddons.api.skyblock.GameMode;
import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas;
import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas.Island;
import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.vconfig.annotation.VCInit;
import me.valkeea.fishyaddons.vconfig.annotation.VCListener;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.api.IntKey;
import net.minecraft.client.Minecraft;

@VCModule
public class TransLava {
    private static boolean enabled;
    private static int color;

    @VCInit
    public static void init() {
        FaEvents.ENVIRONMENT_CHANGE.register(env -> update(env.newIsland(), env.isSkyblock()));
        update();
    }

    @VCListener(value = BooleanKey.TRANS_LAVA, ints = IntKey.TRANS_LAVA_COLOR)
    public static void update() {
        update(SkyblockAreas.getIsland(), GameMode.skyblock());
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static int getColor() {
        return color;
    }

    public static void update(Island island, boolean isSkyblock) {
        boolean wasEnabled = enabled;
        int prevColor = color;

        enabled = isSkyblock && island == Island.CI && Config.get(BooleanKey.TRANS_LAVA);
        color = Config.get(IntKey.TRANS_LAVA_COLOR);

        if (wasEnabled != enabled || (enabled && prevColor != color)) {
            refreshChunks();
        }
    }

    private static void refreshChunks() {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.getConnection() == null) return;

        int viewDist = mc.options.getEffectiveRenderDistance();
        int cx = mc.player.blockPosition().getX() >> 4;
        int cz = mc.player.blockPosition().getZ() >> 4;

        mc.level.setSectionRangeDirty(
            cx - viewDist, mc.level.getMinSectionY(), cz - viewDist,
            cx + viewDist, mc.level.getMaxSectionY(), cz + viewDist
        );
    }

    private TransLava() {}
}
