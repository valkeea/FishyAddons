package me.wait.fishyaddons.config;

import me.wait.fishyaddons.handlers.RetexHandler;
import me.wait.fishyaddons.event.ModelBakeHandler;
import me.wait.fishyaddons.handlers.TextureStitchHandler;
import me.wait.fishyaddons.util.FishyNotis;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.MalformedJsonException;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class TextureConfig {
    private static final File CONFIG_FILE = new File("config/fishyaddons/fishyzones.json");
    private static final File BACKUP_DIR = new File("config/fishyaddons/backup");
    private static final File BACKUP_FILE = new File(BACKUP_DIR, "fishyzones.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type CONFIG_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

    private static final Set<String> enabledIslands = new HashSet<>();
    private static boolean retexStatus = true;
    private static boolean allToggled = true;
    private static boolean recreatedConfig = false;
    private static boolean restoredConfig = false;

    public static boolean isRecreated() { return recreatedConfig; }
    public static boolean isRestored() { return restoredConfig; }
    public static void resetFlags() {
        recreatedConfig = false;
        restoredConfig = false;
    }

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            System.err.println("[TextureConfig] Config file does not exist. Creating a new one...");
            loadOrRestore();
            return;
        }

        try (Reader reader = new InputStreamReader(new FileInputStream(CONFIG_FILE), StandardCharsets.UTF_8)) {
            Map<String, Object> config = GSON.fromJson(reader, CONFIG_TYPE);

            if (config == null || !validate(config)) {
                System.err.println("[TextureConfig] Invalid config detected. Attempting to restore from backup...");
                loadOrRestore();
                return;
            }

            List<String> islands = (List<String>) config.get("enabledIslands");
            enabledIslands.clear();
            if (islands != null) {
                enabledIslands.addAll(islands);
                enabledIslands.remove("default");
            }

            retexStatus = (boolean) config.getOrDefault("retexStatus", true);
        } catch (JsonSyntaxException | MalformedJsonException e) {
            System.err.println("[TextureConfig] Malformed JSON detected: " + e.getMessage());
            loadOrRestore();
        } catch (IOException e) {
            System.err.println("[TextureConfig] Failed to load configuration: " + e.getMessage());
            loadOrRestore();
        }
    }

    public static void save() {
        Map<String, Object> config = new HashMap<>();
        List<String> islandsToSave = new ArrayList<>(enabledIslands);
        islandsToSave.remove("default");
        config.put("enabledIslands", islandsToSave);
        config.put("retexStatus", retexStatus);

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(CONFIG_FILE), StandardCharsets.UTF_8)) {
            GSON.toJson(config, CONFIG_TYPE, writer);
        } catch (IOException e) {
            System.err.println("[FishyAddons] Failed to save texture config:");
            e.printStackTrace();
        }
    }

    public static void saveBackup() {
        try {
            if (CONFIG_FILE.exists()) {
                Files.copy(CONFIG_FILE.toPath(), BACKUP_FILE.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println("[ConfigHandler] Failed to save backup: " + e.getMessage());
        }
    }

    private static boolean validate(Map<String, Object> config) {
        return config.containsKey("enabledIslands") && config.containsKey("retexStatus");
    }

    private static void loadOrRestore() {
        if (BACKUP_FILE.exists()) {
            System.err.println("[TextureConfig] Restoring from backup...");
            try {
                Files.copy(BACKUP_FILE.toPath(), CONFIG_FILE.toPath(), StandardCopyOption.REPLACE_EXISTING);
                load();
                restoredConfig = true;
                return;
            } catch (IOException e) {
                System.err.println("[TextureConfig] Failed to restore from backup: " + e.getMessage());
            }
        }

        System.err.println("[TextureConfig] No valid backup found. Creating a new default configuration...");
        enabledIslands.clear();
        enabledIslands.addAll(RetexHandler.getKnownIslands());
        retexStatus = true;
        save();
        recreatedConfig = true;
    }

    public static void toggleIslandTexture(String island, boolean enabled) {
        if (!RetexHandler.getKnownIslands().contains(island)) {
            System.err.println("[FishyAddons] Attempted to toggle unknown island: " + island);
            return;
        }

        if (enabled) {
            enabledIslands.add(island);
            FishyNotis.send(EnumChatFormatting.GRAY + "Enabled retexturing for " + island + ". Use f3 + t or swap servers for instant effects.");
        } else {
            enabledIslands.remove(island);
            FishyNotis.send(EnumChatFormatting.GRAY + "Disabled retexturing for " + island + ". Use f3 + t or swap servers for instant effects.");
        }
        save();
        RetexHandler.reloadOverrides();
    }

    public static boolean isIslandTextureEnabled(String island) {
        return enabledIslands.contains(island);
    }

    public static Set<String> getEnabledIslands() {
        return Collections.unmodifiableSet(enabledIslands);
    }

    public static void setAllToggled(boolean enabled) {
        allToggled = enabled;
        if (enabled) {
            enabledIslands.addAll(RetexHandler.getKnownIslands());
        } else {
            enabledIslands.clear();
        }
        save();
    }

    public static void setRetexStatus(boolean enabled) {
        retexStatus = enabled;
        save();
        updateRegistration();
    }

    public static void updateRegistration() {
        if (retexStatus) {
            MinecraftForge.EVENT_BUS.register(ModelBakeHandler.INSTANCE);
            MinecraftForge.EVENT_BUS.register(TextureStitchHandler.INSTANCE);
        } else {
            MinecraftForge.EVENT_BUS.unregister(ModelBakeHandler.INSTANCE);
            MinecraftForge.EVENT_BUS.unregister(TextureStitchHandler.INSTANCE);
        }
    }

    public static boolean isRetexStatus() {
        return retexStatus;
    }

    public static boolean isAllToggled() {
        return allToggled;
    }
}
