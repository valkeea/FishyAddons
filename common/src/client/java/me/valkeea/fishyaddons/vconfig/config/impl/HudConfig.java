package me.valkeea.fishyaddons.vconfig.config.impl;

import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import me.valkeea.fishyaddons.vconfig.config.BaseConfig;
import me.valkeea.fishyaddons.vconfig.config.ConfigSection;
import net.minecraft.client.Minecraft;

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
        double screenW = getScreenWidth();
        int intValue = resolveCoord(hudKey, "X", defaultX, screenW);
        int maxX = (int) screenW - getHudSize(hudKey, 12);
        return Math.clamp(intValue, 0, maxX);
    }

    public static int getHudY(String hudKey, int defaultY) {
        double screenH = getScreenHeight();
        int intValue = resolveCoord(hudKey, "Y", defaultY, screenH);
        int maxY = (int) screenH - getHudSize(hudKey, 12);
        return Math.clamp(intValue, 0, maxY);
    }
    
    // Positions are stored as fractions (0.0-1.0) of the current GUI-scaled screen size
    private static int resolveCoord(String hudKey, String axis, int defaultValue, double screenDim) {
        var pctValue = INSTANCE.hud.getValues().get(hudKey + axis + "Pct");
        if (pctValue instanceof Number pct) {
            return (int) Math.round(pct.doubleValue() * screenDim);
        }

        var legacy = INSTANCE.hud.getValues().get(hudKey + axis);
        if (legacy instanceof Number n) {
            int legacyPixels = n.intValue();
            setPct(hudKey, axis, legacyPixels, screenDim);
            return legacyPixels;
        }

        return defaultValue;
    }

    private static void setPct(String hudKey, String axis, int pixels, double screenDim) {
        double pct = screenDim > 0 ? pixels / screenDim : 0.0;
        INSTANCE.hud.set(hudKey + axis + "Pct", pct);
        INSTANCE.hud.remove(hudKey + axis);
    }

    public static void setHudX(String hudKey, int x) {
        setPct(hudKey, "X", x, getScreenWidth());
    }

    public static void setHudY(String hudKey, int y) {
        setPct(hudKey, "Y", y, getScreenHeight());
    }

    private static double getScreenWidth() {
        return Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    private static double getScreenHeight() {
        return Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }

    @Override
    protected void loadFromJson(JsonObject json) {
        hud.loadFromJson(json);
    }
     
    @Override
    protected void saveToJson(JsonObject json) {
        hud.saveToJson(json);
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
