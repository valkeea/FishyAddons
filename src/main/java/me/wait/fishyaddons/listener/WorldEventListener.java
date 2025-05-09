package me.wait.fishyaddons.listener;

import me.wait.fishyaddons.util.ZoneUtils;
import me.wait.fishyaddons.util.AreaUtils;
import me.wait.fishyaddons.util.SkyblockCheck;
import me.wait.fishyaddons.util.ScoreboardUtils;
import me.wait.fishyaddons.event.ClientConnectedToServer;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


@SideOnly(Side.CLIENT)
public class WorldEventListener {
    private boolean updateRulesNextTick = false;
    private int scoreboardDelay = 0;
    private boolean checkedInitialIsland = false;
    private boolean checkBypass = false;

    private static final WorldEventListener INSTANCE = new WorldEventListener();
    private WorldEventListener() {}
    public static WorldEventListener getInstance() {
        return INSTANCE;
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new WorldEventListener());
    }

    private static Minecraft getMc() {
        return Minecraft.getMinecraft();
    }

    public void bypass() {
        checkBypass = true;
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        updateRulesNextTick = true;
        scoreboardDelay = 20;
        checkedInitialIsland = checkBypass;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END || event.type != TickEvent.Type.CLIENT) return;
        if (getMc().theWorld == null || getMc().thePlayer == null) return;

        if (!checkedInitialIsland) {
            ScoreboardUtils.logSidebar();
            AreaUtils.updateIsland();
            checkedInitialIsland = true;
        }

        if (updateRulesNextTick) {
            if (scoreboardDelay > 0) {
                scoreboardDelay--;
            }

            if (scoreboardDelay == 0) {
                SkyblockCheck.getInstance().updateSkyblockCache();
                ZoneUtils.update();
                updateRulesNextTick = false;
                checkBypass = false;
                ClientConnectedToServer.triggerAction();
            }
        }
    }
}