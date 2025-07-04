package me.valkeea.fishyaddons.config;

import me.valkeea.fishyaddons.gui.TabbedListScreen;
import net.minecraft.text.Text;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class FishyPresets {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public enum PresetType { COMMANDS, KEYBINDS, CHAT, ALERT }

    public static Path getPresetPath(PresetType type) {
        String name = switch (type) {
            case COMMANDS -> "commands";
            case KEYBINDS -> "keybinds";
            case CHAT -> "chat";
            case ALERT -> "alert";
        };
        return Paths.get("config", "fishyaddons", "preset", name + ".json");
    }

    public static Path getPresetDir() {
        return Paths.get("config", "fishyaddons", "preset");
    }

    public static java.util.List<String> listPresetSuffixes(PresetType type) {
        java.util.List<String> result = new ArrayList<>();
        Path dir = getPresetDir();
        if (!Files.exists(dir)) return result;
        try (var stream = Files.list(dir)) {
            Pattern pattern = Pattern.compile("preset\\." + Pattern.quote(getTypeName(type)) + "\\.(.+)\\.json");
            stream.forEach(path -> {
                String fileName = path.getFileName().toString();
                Matcher matcher = pattern.matcher(fileName);
                if (matcher.matches()) {
                    result.add(matcher.group(1));
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private static String getTypeName(PresetType type) {
        return switch (type) {
            case COMMANDS -> "commands";
            case KEYBINDS -> "keybinds";
            case CHAT -> "chat";
            case ALERT -> "alert";
        };
    }

    public static final Map<String, String> EXAMPLE_COMMANDS_PRESET = Map.of(
        "/m7", "/joininstance MASTER_CATACOMBS_FLOOR_SEVEN",
        "/m6", "/joininstance MASTER_CATACOMBS_FLOOR_SIX",
        "/m5", "/joininstance MASTER_CATACOMBS_FLOOR_FIVE",
        "/m4", "/joininstance MASTER_CATACOMBS_FLOOR_FOUR",
        "/m3", "/joininstance MASTER_CATACOMBS_FLOOR_THREE",
        "/m2", "/joininstance MASTER_CATACOMBS_FLOOR_TWO",
        "/m1", "/joininstance MASTER_CATACOMBS_FLOOR_ONE"
    );

    public static final Map<String, String> EXAMPLE_KEYBINDS_PRESET = Map.of(
        "MOUSE3", "/pets",
        "MOUSE4", "/wardrobe",
        "GLFW_KEY_B", "/wardrobe"
    );

    public static final Map<String, String> EXAMPLE_CHAT_PRESET = Map.of(
        ":cat:", "ᗢᘏᓗ",
        ":hi:", "ඞ",
        "\u003c3", "❤",
        ":star", "✮",
        ":yes:", "✔",
        ":no:", "✖",
        ":java:", "☕",
        ":arrow:", "➜", 
        ":shrug:", "¯\\_(ツ)_/¯",
        ":tableflip:", "(╯°□°）╯︵ ┻━┻"
    );

    public static final Map<String, FishyConfig.AlertData> EXAMPLE_ALERT_PRESET = Map.of(
        "This is what will be detected from chat.", new FishyConfig.AlertData(
            "this is what you will send in chat!",
            "Title Screen Alert",
            0xFF00FF,
            "minecraft:entity.player.levelup",
            1.0F,
            true
        )
    );

    public static void ensureDefaultPresets() {
        ensureDefaultCommandsPreset();
    }

    private static void ensureDefaultCommandsPreset() {
        Path path = getPresetDir().resolve("preset.commands.default.json");
        if (!Files.exists(path)) {
            saveStringPreset(PresetType.COMMANDS, "mastermode", EXAMPLE_COMMANDS_PRESET);
            saveStringPreset(PresetType.KEYBINDS, "example", EXAMPLE_KEYBINDS_PRESET);
            saveStringPreset(PresetType.CHAT, "hypixel", EXAMPLE_CHAT_PRESET);
            saveAlertPreset("example", EXAMPLE_ALERT_PRESET);
            System.out.println("Default presets created in " + path.getParent());
        }
    }

    public static Map<String, String> loadStringPreset(PresetType type, String suffix) {
        Path path = getPresetDir().resolve("preset." + getTypeName(type) + "." + suffix + ".json");
        if (!Files.exists(path)) return null;
        try {
            String json = Files.readString(path);
            return GSON.fromJson(json, new TypeToken<Map<String, String>>(){}.getType());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Map<String, FishyConfig.AlertData> loadAlertPreset(String suffix) {
        Path path = getPresetDir().resolve("preset.alert." + suffix + ".json");
        if (!Files.exists(path)) return null;
        try {
            String json = Files.readString(path);
            return GSON.fromJson(json, new TypeToken<Map<String, FishyConfig.AlertData>>(){}.getType());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void saveStringPreset(PresetType type, String suffix, Map<String, String> data) {
        Path path = getPresetDir().resolve("preset." + getTypeName(type) + "." + suffix + ".json");
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(data));
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    public static void saveAlertPreset(String suffix, Map<String, FishyConfig.AlertData> data) {
        Path path = getPresetDir().resolve("preset.alert." + suffix + ".json");
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(data));
        } catch (Exception e) {          
            e.printStackTrace();
        }
    }
}