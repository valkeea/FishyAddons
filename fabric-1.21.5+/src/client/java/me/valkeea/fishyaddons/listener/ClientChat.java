package me.valkeea.fishyaddons.listener;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.handler.ChatAlert;
import me.valkeea.fishyaddons.handler.ChatTimers;
import me.valkeea.fishyaddons.handler.PetInfo;
import me.valkeea.fishyaddons.render.BeaconRenderer;
import me.valkeea.fishyaddons.util.AreaUtils;
import me.valkeea.fishyaddons.util.HelpUtil;
import me.valkeea.fishyaddons.util.SkyblockCheck;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class ClientChat {
    private static final ClientChat INSTANCE = new ClientChat();
    private ClientChat() {}
    public static ClientChat getInstance() { return INSTANCE; }

    private static BlockPos beaconPos = null;

    public static BlockPos getBeaconPos() {
        return beaconPos;
    }

    public static void setBeaconPos(BlockPos pos) {
        beaconPos = pos;
    }

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            String text = message.getString();
            INSTANCE.onClientChat(text);
            ChatAlert.handleMatch(text);
            PetInfo.handleChat(text);

            Pattern coordPattern = Pattern.compile(
                "\\bx\\s*:\\s*(-?\\d{1,7})\\s*,\\s*y\\s*:\\s*(-?\\d{1,7})\\s*,\\s*z\\s*:\\s*(-?\\d{1,7})\\b",
                Pattern.CASE_INSENSITIVE
            );
            Matcher matcher = coordPattern.matcher(text);
            if (matcher.find()) {
                int x = Integer.parseInt(matcher.group(1));
                int y = Integer.parseInt(matcher.group(2));
                int z = Integer.parseInt(matcher.group(3));
                BlockPos newPos = new BlockPos(x, y, z);
                String label = HelpUtil.stripColor(text.substring(0, matcher.start()).trim());
                if (label.isEmpty()) {
                    label = text.substring(matcher.end()).trim();
                }

                if (!newPos.equals(BeaconRenderer.getActualPos(new Vec3d(newPos))) && 
                    FishyConfig.getState("renderCoords", false)) {
                    BeaconRenderer.setBeacon(BeaconRenderer.getActualPos(new Vec3d(newPos)),
                    FishyConfig.getInt("renderCoordsColor"), label.isEmpty() ? null : label);
                }
            }
        });
    }

    public void onClientChat(String message) {
        Pattern pattern = Pattern.compile("entered (MM )?The Catacombs");
        Matcher matcher = pattern.matcher(message);

        if (matcher.find()) {
            WorldEvent.getInstance().bypass();
            AreaUtils.setIsland("dungeon");
        }

        if (message.contains("Glacite Mineshafts")) {
            WorldEvent.getInstance().bypass();
            AreaUtils.setIsland("mineshaft");
        }

        if (message.contains("You are playing on profile:")) {
            SkyblockCheck.getInstance().setInSkyblock(true);
        }

        if (message.contains("You adjusted the frequency of the Beacon!")) {
            ChatTimers.getInstance().beaconStart();
        }
    }
}