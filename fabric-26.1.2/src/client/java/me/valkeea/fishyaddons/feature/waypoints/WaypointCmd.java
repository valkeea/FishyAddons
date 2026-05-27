package me.valkeea.fishyaddons.feature.waypoints;

import static me.valkeea.fishyaddons.feature.waypoints.ChainConfig.USER_CHAINS;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas;
import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas.Island;
import me.valkeea.fishyaddons.tool.GuiScheduler;
import me.valkeea.fishyaddons.util.FishyNotis;
import me.valkeea.fishyaddons.util.ZoneUtils;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.ui.screen.ColorWheel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/**
 * Command access for waypoint management.
 */
public class WaypointCmd {
    private WaypointCmd() {}

    private static String lastModified = "";

    // --- Waypoint management ---

    /** Add a waypoint to the next available position in the last modified chain */
    public static int addNext() {

        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return 0;
        }

        var currentArea = getCurrentArea();
        if (currentArea == null) {
            areaNotFound();
            return 0;
        }

        if (lastModified.isEmpty()) {
            FishyNotis.warn("No active chain found! §8Use '§3/fwp set <chain>§8' to identify which chain you wish to modify.");
            return 0;
        }

        int nextOrder = 1;
        WaypointChain targetChain = null;
        for (var chain : USER_CHAINS) {
            if (chain.name().equals(lastModified) && chain.area.equals(currentArea.key())) {
                targetChain = chain;
                break;
            }
        }

        if (targetChain != null) {
            List<Integer> existingOrders = new ArrayList<>();
            for (var wp : targetChain.waypoints) {
                existingOrders.add(extractOrderNumber(wp.label()));
            }

            while (existingOrders.contains(nextOrder)) {
                nextOrder++;
            }

            return addUserWaypoint(lastModified, nextOrder, currentArea, blockUnderPlayer());
        }

        return 0;
    }

    public static int addWaypoint(String chainName, int orderNumber) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            FishyNotis.warn("You must be in a world to add waypoints!");
            return 0;
        }

        Island currentArea = getCurrentArea();
        if (currentArea == null) {
            areaNotFound();
            return 0;
        }

        return addUserWaypoint(chainName, orderNumber, currentArea, blockUnderPlayer());
    }

    public static int addUserWaypoint(String chainName, int orderNumber, Island area, BlockPos position) {
        WaypointChain existingChain = null;
        for (var chain : USER_CHAINS) {
            if (chain.name().equals(chainName) && chain.area.equals(area.key())) {
                existingChain = chain;
                break;
            }
        }

        if (existingChain != null) {
            for (var waypoint : existingChain.waypoints) {
                if (waypoint.label().endsWith(" " + orderNumber)) {
                    FishyNotis.alert(Component.literal("§cWaypoint §3" + orderNumber + " §calready exists in chain '§3" + chainName + "§c'!"));
                    FishyNotis.alert(Component.literal("§8Use '§3/fwp remove " + chainName + " " + orderNumber + "§8' first to replace it."));
                    return 0;
                }
            }
            
            existingChain.waypoints.add(new Waypoint(position, chainName + " " + orderNumber, false));

            existingChain.waypoints.sort((a, b) -> {
                int orderA = extractOrderNumber(a.label());
                int orderB = extractOrderNumber(b.label());
                return Integer.compare(orderA, orderB);
            });

        } else {
            List<Waypoint> waypoints = new ArrayList<>();
            waypoints.add(new Waypoint(position, chainName + " " + orderNumber, false));
            WaypointChain newChain = new WaypointChain(area.key(), chainName, waypoints, ChainType.USER_DEFINED);
            USER_CHAINS.add(newChain);
        }

        setLastModified(chainName);
        ChainConfig.save();
        WaypointChains.clearUserChainCache(); // Clear cache after adding waypoint
        FishyNotis.alert(Component.literal("§7Added waypoint §3" + orderNumber + " §7to chain '§3" + chainName + "§7' in " + area.displayName() + "!"));
        return 1;
    }

    /** Removes a waypoint from a user chain */
    public static int removeWaypoint(String chainName, int orderNumber) {

        Island area = getCurrentArea();
        if (area == null) {
            areaNotFound();
            return 0;
        }

        for (var chain : USER_CHAINS) {
            if (chain.name().equals(chainName) && chain.area.equals(area.key()) && chain.type == ChainType.USER_DEFINED) {

                setLastModified(chainName);

                boolean removed = chain.waypoints.removeIf(waypoint -> 
                    waypoint.label().endsWith(" " + orderNumber));    
                
                if (removed) {
                    if (chain.waypoints.isEmpty()) USER_CHAINS.remove(chain);
                    ChainConfig.save();
                    WaypointChains.clearUserChainCache();
                    FishyNotis.alert(Component.literal("§aRemoved waypoint " + orderNumber + " from chain '§3" + chainName + "§7'!"));
                    return 1;

                } else {
                    FishyNotis.alert(Component.literal("§cWaypoint " + orderNumber + " not found in chain '§3" + chainName + "§7'!"));
                    return 0;
                }
            }
        }

        chainNotFound(chainName, area.displayName());
        return 0;
    }

    // --- List and statistics ---

    public static int listWaypoints() {
        var area = getCurrentArea();
        if (area == null) {
            areaNotFound();
            return 0;
        }

        List<WaypointChain> areaChains = new ArrayList<>();
        for (var chain : USER_CHAINS) {
            if (chain.area.equals(area.key())) {
                areaChains.add(chain);
            }
        }

        if (areaChains.isEmpty()) {
            FishyNotis.alert(Component.literal("§7No waypoint chains found in " + area + "."));
            return 0;
        }

        FishyNotis.themed("Waypoint Chains in " + area + ": ");
        for (var chain : areaChains) {

            boolean isVisible = ChainConfig.isChainVisible(chain.name(), area.key());
            String visibilityStatus = isVisible ? "§aVisible" : "§7Hidden";
            
            var toggleButton = me.valkeea.fishyaddons.util.text.ChatButton.create(
                "/fwp toggle " + chain.name(), 
                "Toggle"
            );

            var chainInfo = Component.literal("§3" + chain.name() + " §8- §7" + chain.waypoints.size() + " waypoints " + " §8(" + visibilityStatus + "§8)");
            Minecraft.getInstance().player.sendSystemMessage(chainInfo.copy().append(toggleButton));
        }
        return 1;
    }

    public static int showChainInfo(String chainName) {
        var currentArea = getCurrentArea();
        if (currentArea == null) {
            areaNotFound();
            return 0;
        }

        return showChainInfo(chainName, currentArea);
    }    

    private static int showChainInfo(String chainName, Island area) {
        for (var chain : USER_CHAINS) {
            String areaKey = area.key();

            if (chain.name().equals(chainName) && chain.area.equals(areaKey) && chain.type == ChainType.USER_DEFINED) {

                setLastModified(chainName);
                FishyNotis.themed("Chain Info: " + chainName + " (" + area + ")");
                
                if (chain.waypoints.isEmpty()) {
                    FishyNotis.alert(Component.literal("§7This chain is empty."));
                    return 1;
                }
                
                int nextWaypoint = -1;
                var mc = Minecraft.getInstance();
                Vec3 playerPos = mc.player != null ? mc.player.position() : new Vec3(0, 0, 0);
                
                nextWaypoint = IntStream.range(0, chain.waypoints.size())
                    .filter(i -> !chain.waypoints.get(i).visited())
                    .findFirst()
                    .orElse(-1);

                String avgRunTime = WaypointChains.getAvgRunTime(chainName, areaKey);
                boolean isActive = WaypointChains.isChainActive(chainName, areaKey);

                if (avgRunTime != null) {
                    FishyNotis.alert(Component.literal("§7Session AVG: §b" + avgRunTime));
                }
                
                if (isActive) {
                    long elapsedTime = System.currentTimeMillis() - WaypointChains.getChainStartTime(chainName, areaKey);
                    FishyNotis.alert(Component.literal("§7Current run: §3" + formatTime(elapsedTime) + " elapsed"));
                }
                
                int color = ChainConfig.getChainColor(chainName, areaKey);
                String colorHex = String.format("#%06X", color & 0xFFFFFFFF);
                var editBtn = me.valkeea.fishyaddons.util.text.ChatButton.create(
                    "/fwp color " + chainName,
                    "Edit"
                );
                FishyNotis.alert(Component.literal("§7Custom color: ").append(Component.literal(colorHex + " ").withStyle(style ->
                    style.withColor(color & 0xFFFFFFFF))).append(editBtn));

                if (nextWaypoint != -1) {
                    Waypoint next = chain.waypoints.get(nextWaypoint);
                    double nextDistance = mc.player != null ? playerPos.distanceTo(new Vec3(
                        next.position.getX() + 0.5,
                        next.position.getY() + 0.5,
                        next.position.getZ() + 0.5
                    )) : 0;

                    FishyNotis.alert(Component.literal("§7Next: §3" + next.label() + " §8(§a" + String.format("%.1f", nextDistance) + "m§8)"));
                }
                
                FishyNotis.alert(Component.literal("§7Waypoints:"));
                for (int i = 0; i < chain.waypoints.size(); i++) {
                    var waypoint = chain.waypoints.get(i);
                    boolean wasVisited = waypoint.visited();
                    
                    String status = wasVisited ? "§a✓" : (i == nextWaypoint ? "§p→" : "§8○");
                    String pos = waypoint.position.getX() + "," + waypoint.position.getY() + "," + waypoint.position.getZ();

                    FishyNotis.alert(Component.literal("  " + status + " §7" + (i + 1) + ". §f" + waypoint.label() + " §8(" + pos + ")"));
                }
                
                return 1;
            }
        }

        chainNotFound(chainName, area.displayName());
        return 0;
    }

    // --- Chain management ---

    public static int renameChain(String oldName, String newName) {
        var area = getCurrentArea();
        if (area == null) {
            areaNotFound();
            return 0;
        }

        for (WaypointChain chain : USER_CHAINS) {

            if (chain.name().equals(newName) && chain.area.equals(area.key())) {
                FishyNotis.warn("A chain with the name '" + newName + "' already exists in " + area.displayName() + "!");
                return 0;
            }

            if (chain.name().equals(oldName) && chain.area.equals(area.key())) {
                ChainConfig.renameUserChain(chain, newName);
                setLastModified(newName);
                break;
            }
        }

        ChainConfig.save();
        WaypointChains.clearUserChainCache();
        FishyNotis.alert(Component.literal("§7Renamed chain '§3" + oldName + "§7' to '§3" + newName + "§7' in §b" + area.displayName() + "§7!"));
        return 1;
    }
    
    public static int clearChain(String chainName) {
        var area = getCurrentArea();
        if (area == null) {
            areaNotFound();
            return 0;
        }

        boolean removed = USER_CHAINS.removeIf(chain -> 
            chain.name().equals(chainName) && chain.area.equals(area.key()) && chain.type == ChainType.USER_DEFINED);
        
        if (removed) {
            ChainConfig.save();
            WaypointChains.clearUserChainCache(); // Clear cache after clearing chain
            FishyNotis.alert(Component.literal("§aCleared chain '§3" + chainName + "§7' from " + area.displayName() + "!"));
            return 1;
        } else {
            chainNotFound(chainName, area.displayName());
            return 0;
        }
    }

    public static int resetChain(String chainName) {
        var area = getCurrentArea();
        if (area == null) {
            areaNotFound();
            return 0;
        }

        for (var chain : USER_CHAINS) {
            if (chain.name().equals(chainName) && chain.area.equals(area.key()) && chain.type == ChainType.USER_DEFINED) {

                setLastModified(chainName);

                for (var waypoint : chain.waypoints) {
                    waypoint.setVisited(false);
                    ChainConfig.saveUserProgress();
                }
                FishyNotis.alert(Component.literal("§aReset completion status for chain '§3" + chainName + "§7'!"));
                return 1;
            }
        }

        chainNotFound(chainName, area.displayName());
        return 0;
    }   

    public static int openColorPicker(String chainName) {
        var area = getCurrentArea();
        if (area == null) {
            areaNotFound();
            return 0;
        }

        boolean chainExists = ChainConfig.getUserChains().stream()
            .anyMatch(chain -> chain.name().equals(chainName) && chain.area.equals(area.key()));
        
        if (!chainExists) {
            chainNotFound(chainName, area.displayName());
            return 0;
        }

        setLastModified(chainName);

        var mc = Minecraft.getInstance();
        if (mc.screen != null && !(mc.screen instanceof ChatScreen)) {
            FishyNotis.warn("Error opening screen.");
            return 0;
        }

        int currentColor = ChainConfig.getChainColor(chainName, area.key());

        GuiScheduler.scheduleGui(new ColorWheel(null, currentColor, selected -> {
            ChainConfig.setChainColor(chainName, area.key(), selected);

            String colorHex = String.format("#%06X", selected & 0xFFFFFFFF);
            FishyNotis.alert(Component.literal("§7Set color for chain '§3" + chainName + "§7' to ").append(Component.literal(colorHex + "§7!").withStyle(style ->
                style.withColor(selected & 0xFFFFFFFF))));
        }));

        return 1;
    }

    public static int toggle() {
        
        boolean newState = !Config.get(BooleanKey.FWP_ENABLED);
        Config.set(BooleanKey.FWP_ENABLED, newState);
        WaypointChains.refresh();

        if (newState) FishyNotis.on("Waypoint Chains");
        else FishyNotis.off("Waypoint Chains");
        return 1;
    }

    public static int toggleChainVisibility(String chainName) {
        var area = getCurrentArea();
        if (area == null) {
            areaNotFound();
            return 0;
        }

        boolean chainExists = ChainConfig.getUserChains().stream()
            .anyMatch(chain -> chain.name().equals(chainName) && chain.area.equals(area.key()));
        
        if (!chainExists) {
            chainNotFound(chainName, area.displayName());
            return 0;
        }

        setLastModified(chainName);

        ChainConfig.toggleChainVisibility(chainName, area.key());
        WaypointChains.clearUserChainCache(); // Clear cache after toggling visibility
        boolean isVisible = ChainConfig.isChainVisible(chainName, area.key());

        String status = isVisible ? "§avisible" : "§7hidden";
        FishyNotis.alert(Component.literal("§7Chain '§3" + chainName + "§7' is now " + status + "!"));
        
        return 1;
    }

    /** Manually mark this chain as last modified */
    public static int setLastModifiedChain(String chainName) {
        setLastModified(chainName);
        return 1;
    }

    // --- Utility methods ---

    private static Island getCurrentArea() {
        if (ZoneUtils.isInDungeon()) return Island.DUNGEON;

        var area = SkyblockAreas.getIsland();
        if (area == Island.NA || area == Island.DEF) {
            return null;

        } else return area;
    }    

    private static String formatTime(long timeMs) {
        long seconds = timeMs / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        
        if (minutes > 0) {
            return String.format(Locale.US, "%dm %02ds", minutes, seconds);
        } else {
            return String.format(Locale.US, "%.1fs", timeMs / 1000.0);
        }
    }    

    private static int extractOrderNumber(String label) {
        String[] parts = label.split(" ");
        if (parts.length > 0) {
            try {
                return Integer.parseInt(parts[parts.length - 1]);
            } catch (NumberFormatException _) {
                return 0;
            }
        }
        return 0;
    }

    private static BlockPos blockUnderPlayer() {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return null;
        }

        return mc.player.blockPosition().below();
    }

    private static void setLastModified(String chainName) {
        if (lastModified.isEmpty() || !lastModified.equals(chainName)) {
            lastModified = chainName;
        }
    }

    private static void chainNotFound(String chainName, String area) {
        FishyNotis.alert(Component.literal("§cChain '§3" + chainName + "§c' not found in " + area + "§c!"));
    }

    private static void areaNotFound() {
        FishyNotis.warn("Could not detect current area!");
    }
}
