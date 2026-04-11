package me.valkeea.fishyaddons.vconfig.config.impl;

import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import me.valkeea.fishyaddons.vconfig.config.BaseConfig;
import me.valkeea.fishyaddons.vconfig.config.ConfigSection;

@SuppressWarnings("squid:S6548")
public class ColorConfig extends BaseConfig {
    private static final ColorConfig INSTANCE = new ColorConfig();
    
    private final ConfigSection<Integer> faColors =
        new ConfigSection<>("faColors",
            new TypeToken<Map<String, Integer>>(){}.getType(),
            v -> requestSave());
    
    private ColorConfig() {
        super("facolors.json");
    }
    
    public static ColorConfig getInstance() {
        return INSTANCE;
    }
    
    @Override
    protected void loadFromJson(JsonObject json) {
        faColors.loadFromJson(json);
    }
    
    @Override
    protected void saveToJson(JsonObject json) {
        faColors.saveToJson(json);
    }
    
    public static Map<String, Integer> getFaC() {
        return INSTANCE.faColors.getValues();
    }
    
    public static void setFaC(String key, int color) {
        INSTANCE.faColors.set(key, color);
    }
    
    public static void removeFaC(String key) {
        INSTANCE.faColors.remove(key);
    }
}
