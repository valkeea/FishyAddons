package me.valkeea.fishyaddons.vconfig.config.impl;

import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import me.valkeea.fishyaddons.vconfig.config.BaseConfig;
import me.valkeea.fishyaddons.vconfig.config.ConfigSection;
import net.minecraft.client.MinecraftClient;

@SuppressWarnings("squid:S6548")
public class HudConfig extends BaseConfig {
    private static final HudConfig INSTANCE = new HudConfig();
    
    private final ConfigSection<Object> hud =
        new ConfigSection<>("hud",
            new TypeToken<Map<String, Object>>(){}.getType(),
            v -> requestSave());

    public static HudConfig getInstance() {
        return INSTANCE;
    }

    private HudConfig() {
        super("hud.json");
    }    
    
    // --- HUD Getters/Setters ---
    
    public static int getHudX(String hudKey, int defaultX) {
        Object value = INSTANCE.hud.getValues().getOrDefault(hudKey + "X", defaultX);
        int intValue = value instanceof Number n ? n.intValue() : defaultX;
        return Math.clamp(intValue, 0, MinecraftClient.getInstance().getWindow().getScaledWidth());
    }

    public static int getHudY(String hudKey, int defaultY) {
        Object value = INSTANCE.hud.getValues().getOrDefault(hudKey + "Y", defaultY);
        int intValue = value instanceof Number n ? n.intValue() : defaultY;
        return Math.clamp(intValue, 0, MinecraftClient.getInstance().getWindow().getScaledHeight());
    }

    public static void setHudX(String hudKey, int x) {
        if (!INSTANCE.hud.getValues().containsKey(hudKey + "X")) {
            INSTANCE.hud.set(hudKey + "X", 5);
        }
        INSTANCE.hud.set(hudKey + "X", x);
    }

    @Override
    protected void loadFromJson(JsonObject json) {
        hud.loadFromJson(json);
    }
     
    @Override
    protected void saveToJson(JsonObject json) {
        hud.saveToJson(json);
    }

    public static void setHudY(String hudKey, int y) {
        if (!INSTANCE.hud.getValues().containsKey(hudKey + "Y")) {
            INSTANCE.hud.set(hudKey + "Y", 5);
        }
        INSTANCE.hud.set(hudKey + "Y", y);
    }

    public static int getHudSize(String hudKey, int defaultSize) {
        Object value = INSTANCE.hud.getValues().getOrDefault(hudKey + "Size", defaultSize);
        return value instanceof Number n ? n.intValue() : defaultSize;
    }

    public static void setHudSize(String hudKey, int size) {
        INSTANCE.hud.set(hudKey + "Size", size);
    }

    public static int getHudColor(String hudKey, int defaultColor) {
        Object value = INSTANCE.hud.getValues().getOrDefault(hudKey + "Color", defaultColor);
        return value instanceof Number n ? n.intValue() | 0xFF000000 : defaultColor;
    }

    public static void setHudColor(String hudKey, int color) {
        INSTANCE.hud.set(hudKey + "Color", color);
    }

    public static boolean getHudOutline(String hudKey, boolean outline) {
        Object value = INSTANCE.hud.getValues().getOrDefault(hudKey + "Outline", outline);
        return value instanceof Boolean b ? b : outline;
    }

    public static void setHudOutline(String hudKey, boolean outline) {
        INSTANCE.hud.set(hudKey + "Outline", outline);
    }

    public static boolean getHudBg(String hudKey, boolean bg) {
        Object value = INSTANCE.hud.getValues().getOrDefault(hudKey + "Bg", bg);
        return value instanceof Boolean b ? b : bg;
    }

    public static void setHudBg(String hudKey, boolean bg) {
        INSTANCE.hud.set(hudKey + "Bg", bg);
    }
}
