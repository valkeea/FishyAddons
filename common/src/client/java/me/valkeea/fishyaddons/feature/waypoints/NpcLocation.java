package me.valkeea.fishyaddons.feature.waypoints;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas;
import me.valkeea.fishyaddons.tool.FishyMode;
import me.valkeea.fishyaddons.util.FishyNotis;
import me.valkeea.fishyaddons.util.text.ChatButton;
import me.valkeea.fishyaddons.util.text.StringUtils;
import me.valkeea.fishyaddons.vconfig.annotation.VCListener;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.api.IntKey;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

@VCModule
public class NpcLocation {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(BlockPos.class, new BlockPosDeserializer())
            .registerTypeAdapter(BlockPos.class, new BlockPosSerializer())
            .create();

    private static final String NPC_LOCATIONS_RESOURCE = "/assets/fishyaddons/data/npc_locations.json";  
    private static Map<String, BlockPos> loaded = null;
    private static long duration = TimeUnit.MINUTES.toMillis(5);

    private static class BlockPosDeserializer implements JsonDeserializer<BlockPos> {
        @Override
        public BlockPos deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) 
                throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            int x = obj.get("x").getAsInt();
            int y = obj.get("y").getAsInt();
            int z = obj.get("z").getAsInt();
            return new BlockPos(x, y, z);
        }
    }

    private static class BlockPosSerializer implements JsonSerializer<BlockPos> {
        @Override
        public JsonElement serialize(BlockPos src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("x", src.getX());
            obj.addProperty("y", src.getY());
            obj.addProperty("z", src.getZ());
            return obj;
        }
    }

    public static void clearCache() {
        if (loaded != null) {
            loaded = null;
        }
    }
    
    private static Map<String, BlockPos> loadAreaData(String area) {
        try (InputStream is = NpcLocation.class.getResourceAsStream(NPC_LOCATIONS_RESOURCE)) {
            if (is == null) return null;
            
            try (var reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                Type mapType = new TypeToken<Map<String, Map<String, BlockPos>>>(){}.getType();
                Map<String, Map<String, BlockPos>> allData = GSON.fromJson(reader, mapType);
                return allData != null ? allData.get(area) : null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }    

    @VCListener(ints = IntKey.NPC_MIN)
    private static void refreshDuration(int min) {
        duration = TimeUnit.MINUTES.toMillis(min);
    }

    public static void drawFor(String npcName) {
        var area = SkyblockAreas.getIsland();
        loaded = loadAreaData(area.key());

        if (loaded == null || loaded.isEmpty()) {
            invalidArea();
            return;
        }

        BlockPos pos = null;
        String name = "";
        for (var entry : loaded.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(npcName)) {
                pos = entry.getValue();
                name = entry.getKey();
                break;
            }
        }

        if (pos != null) {
            TempWaypoint.setBeacon(pos, FishyMode.getThemeColor(), name, duration);
            FishyNotis.send("Set waypoint for NPC: " + name + " at " + pos.toShortString());
        } else {
            List<String> similar = new ArrayList<>();
            loaded.keySet().stream()
                .filter(key -> StringUtils.closeMatch(npcName, key))
                .forEach(similar::add);
            
            if (!similar.isEmpty()) {
                FishyNotis.themed("Unknown NPC. Did you mean;");
                similar.forEach(key -> FishyNotis.alert(Text.literal("§7- §8" + key)
                .styled(style -> style.withClickEvent(new net.minecraft.text.ClickEvent.RunCommand("fa npc " + key)))));
            } else {
                FishyNotis.send("NPC not found: " + npcName);
            }
        }   
    }    

    public static void drawAll() {
        var area = SkyblockAreas.getIsland();
        Map<String, BlockPos> areaNpcs = loaded == null ? loadAreaData(area.key()) : loaded;

        if (areaNpcs == null || areaNpcs.isEmpty()) {
            invalidArea();
            return;
        }

        FishyNotis.send(Text.literal("§7Drawing waypoints for all NPCs in " + area.displayName())
            .append(ChatButton.create("fa npc clear", "Clear")));

        areaNpcs.forEach((name, pos) -> 
            TempWaypoint.setBeacon(pos, FishyMode.getThemeColor(), name, duration));
    }

    private static void invalidArea() {
        FishyNotis.send("No NPC data found for this area.");
    }

    // --- Dev ---

    private static final File OUTPUT_FILE = new File(MinecraftClient.getInstance().runDirectory, "npc_locations.json");
    private static final Map<String, Map<String, BlockPos>> scanned = new HashMap<>();
    private static Map<String, Map<String, BlockPos>> knownNpcs = null;      
    
    private static void ensureFullDatasetLoaded() {
        if (knownNpcs != null) return;
        
        knownNpcs = new HashMap<>();
        try (InputStream is = NpcLocation.class.getResourceAsStream(NPC_LOCATIONS_RESOURCE)) {
            if (is == null) return;
            
            try (var reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                Type mapType = new TypeToken<Map<String, Map<String, BlockPos>>>(){}.getType();
                Map<String, Map<String, BlockPos>> npcData = GSON.fromJson(reader, mapType);
                if (npcData != null) {
                    knownNpcs = npcData;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveNpc() {
        var mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        
        ensureFullDatasetLoaded();
        
        var p = mc.player;
        var w = mc.world;
        int r = 3;

        for (var as : w.getEntitiesByClass(
                ArmorStandEntity.class,
                p.getBoundingBox().expand(r),
                e -> true)) {
    
            var name = as.getCustomName() != null ? as.getCustomName().getString() : null;
            if (name == null || name.isEmpty() || !name.equals(name.toUpperCase())) continue;

            var asPos = as.getBlockPos();
            if (p.getBlockPos().isWithinDistance(asPos, r)) {

                var npc = w.getEntitiesByClass(
                    PlayerEntity.class,
                    new Box(asPos).expand(2),
                    e -> true
                ).stream().findFirst();
                
                if (npc.isPresent()) {

                    var area = SkyblockAreas.getIsland().key();
                    var vec = new Vec3d((int) npc.get().getX(), (int) npc.get().getY(), (int) npc.get().getZ());
                    var pos = BlockPos.ofFloored(vec);

                    if (!knownNpcs.containsKey(area) || !knownNpcs.get(area).containsKey(name) || 
                        !knownNpcs.get(area).get(name).equals(pos)) {
                        scanned.computeIfAbsent(area, k -> new HashMap<>()).put(name, pos);
                        FishyNotis.send("Saved NPC: " + name + " at " + pos.toShortString());
                        TempWaypoint.setBeacon(pos, FishyMode.getThemeColor(), name, duration);
                        saveToFile();
                    }
                }
            }
        }
    } 

    private static void saveToFile() {
        try (var writer = new FileWriter(OUTPUT_FILE)) {
            GSON.toJson(scanned, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }    
}
