package me.valkeea.fishyaddons.vconfig.config.impl;

import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import me.valkeea.fishyaddons.feature.qol.ChatAlert;
import me.valkeea.fishyaddons.vconfig.config.BaseConfig;
import me.valkeea.fishyaddons.vconfig.config.ConfigSection;

@SuppressWarnings("squid:S6548")
public class AlertConfig extends BaseConfig {

    private static final AlertConfig INSTANCE = new AlertConfig();

    private final ConfigSection<AlertData> chatAlerts =
        new ConfigSection<>("chatAlerts",
            new TypeToken<Map<String, AlertData>>(){}.getType(),
            v -> { requestSave(); ChatAlert.refresh(); });

    private AlertConfig() {
        super("alerts.json");
    }

    public static AlertConfig getInstance() {
        return INSTANCE;
    }

    @Override
    protected void loadFromJson(JsonObject json) {
        chatAlerts.loadFromJson(json);
    }

    @Override
    protected void saveToJson(JsonObject json) {
        chatAlerts.saveToJson(json);
    }
    
    // --- Chat Alerts API ---

    public static void setAlerts(Map<String, AlertData> alerts) {
        INSTANCE.chatAlerts.getValues().putAll(alerts);
        INSTANCE.requestSave();
    }
    
    public static Map<String, AlertData> getChatAlerts() {
        return INSTANCE.chatAlerts.getValues();
    }

    public static void setChatAlert(String key, AlertData data) {
        INSTANCE.chatAlerts.set(key, data);
    }

    public static void removeChatAlert(String key) {
        INSTANCE.chatAlerts.remove(key);
    }

    public static boolean isChatAlertToggled(String key) {
        AlertData data = INSTANCE.chatAlerts.getValues().get(key);
        if (data != null) return data.isToggled();
        return INSTANCE.chatAlerts.isToggled(key);
    }

    public static boolean isTitleActive(String key) {
        AlertData data = INSTANCE.chatAlerts.getValues().get(key);
        return data != null && data.getOnscreen() != null && !data.getOnscreen().isBlank();
    }

    public static void toggleChatAlert(String key, boolean enabled) {
        AlertData data = INSTANCE.chatAlerts.getValues().get(key);
        if (data != null) {
            data.setToggled(enabled);
            INSTANCE.chatAlerts.set(key, data);
        } else {
            INSTANCE.chatAlerts.toggle(key, enabled);
        }
        INSTANCE.requestSave();
    }

    // --- Alert Data ---
    public static class AlertData {
        private String msg;
        private String onscreen;
        private int color;
        private String soundId;
        private float volume;
        private boolean toggled;
        private boolean startsWith;

        public AlertData() {
            this("", "", 0xFF6DE6B5, "", 1.0F, true, false);
        }

        public AlertData(String msg, String onscreen, int color, String soundId, float volume, boolean toggled, boolean startsWith) {
            this.msg = msg;
            this.onscreen = onscreen;
            this.color = color;
            this.soundId = soundId;
            this.volume = Math.clamp(volume, 0.0f, 10.0f);
            this.toggled = toggled;
            this.startsWith = startsWith;
        }

        public void setMsg(String msg) { this.msg = msg; }        
        public String getMsg() { return msg; }
        public String getOnscreen() { return onscreen; }

        public int getColor() { 
            if ((color & 0xFF000000) == 0) {
                color |= 0xFF000000;
            }
            return color; 
        }
        
        public String getSoundId() { return soundId; }
        public float getVolume() { return volume; }
        public boolean isToggled() { return toggled; }  
        public void setToggled(boolean toggled) { this.toggled = toggled; }
        public boolean isStartsWith() { return startsWith; }
        public void setStartsWith(boolean startsWith) { this.startsWith = startsWith; }
        public void setOnscreen(String onscreen) { this.onscreen = onscreen; }
        public void setColor(int color) { this.color = color; }
        public void setSoundId(String soundId) { this.soundId = soundId; }
        public void setVolume(float volume) { this.volume = Math.clamp(volume, 0.0f, 10.0f); }
    }    
}
