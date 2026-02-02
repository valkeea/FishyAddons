package me.valkeea.fishyaddons.feature.skyblock.timer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.util.FishyNotis;
import me.valkeea.fishyaddons.util.text.TextUtils;
import net.minecraft.client.MinecraftClient;

@SuppressWarnings("squid:S6548")
public class CakeTimer {
    private static final CakeTimer INSTANCE = new CakeTimer();    
    private static final File TIMER_FILE = new File(MinecraftClient.getInstance().runDirectory, "config/fishyaddons/display.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Pattern CAKE_PATTERN = Pattern.compile("(?:Big Yum! You refresh|Yum! You gain) (.+) for 48 hours!");
    private static final long CAKE_DURATION_MS = 48L * 60L * 60L * 1000L;

    private static boolean enabled = false;
    
    private final Map<String, Long> activeCakes = new HashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private boolean scheduledCheck = false;
    
    private CakeTimer() {}
    
    public static CakeTimer getInstance() {
        return INSTANCE;
    }

    public static void refresh() {
        enabled = FishyConfig.getState(Key.HUD_CENTURY_CAKE_ENABLED, false);
    }

    public static boolean isEnabled() {
        return enabled;
    }
    
    public void init() {
        refresh();
        loadTimers();
        scheduleNoti();
    }

    public boolean handleChat(String message) {

        var cleanMessage = TextUtils.stripColor(message);
        var matcher = CAKE_PATTERN.matcher(cleanMessage);

        if (matcher.find()) {

            var buffName = matcher.group(1);
            if (buffName != null) buffName = buffName.trim();
            
            long currentTime = System.currentTimeMillis();
            long expiryTime = currentTime + CAKE_DURATION_MS;

            activeCakes.put(buffName, expiryTime);
            saveTimers();
            return true;
        }

        return false;
    }
    
    public void onLoad() {
        if (!FishyConfig.getState(Key.CAKE_NOTI, false)) {
            return;
        }
        
        scheduler.schedule(() -> {
            var mc = MinecraftClient.getInstance();
            if (mc.player == null) return;
            
            long now = System.currentTimeMillis();
            boolean hasExpiredCakes = false;
            
            for (Map.Entry<String, Long> entry : activeCakes.entrySet()) {
                if (entry.getValue() <= now) {
                    hasExpiredCakes = true;
                    break;
                }
            }
            
            if (hasExpiredCakes) {
                me.valkeea.fishyaddons.util.FishyNotis.send("§cSome of your century cake buffs have expired!");
            }
        }, 5, TimeUnit.SECONDS);
    }
    
    private void scheduleNoti() {
        if (scheduledCheck) return;
        scheduledCheck = true;

        if (!FishyConfig.getState(Key.CAKE_NOTI, false)) return;

        scheduler.scheduleAtFixedRate(() -> {

            var mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.world == null) return;
            
            long now = System.currentTimeMillis();
            
            for (Map.Entry<String, Long> entry : activeCakes.entrySet()) {
                long timeLeft = entry.getValue() - now;

                if (timeLeft > 0 && timeLeft <= 5 * 60 * 1000) {
                    FishyNotis.send("§8" + entry.getKey() + " §7expires in §e" + formatTimeLeft(timeLeft) + "§7!");
                    activeCakes.remove(entry.getKey());
                    saveTimers();
                    break;
                }
            }
        }, 1, 1, TimeUnit.MINUTES);
    }
    
    public Map<String, Long> getActiveCakes() {
        long now = System.currentTimeMillis();
        activeCakes.entrySet().removeIf(entry -> entry.getValue() <= now);
        return new HashMap<>(activeCakes);
    }
    
    public String getNextExpiringCake() {

        long now = System.currentTimeMillis();
        String nextCake = null;
        long earliestExpiry = Long.MAX_VALUE;
        
        for (Map.Entry<String, Long> entry : activeCakes.entrySet()) {
            long expiryTime = entry.getValue();
            if (expiryTime > now && expiryTime < earliestExpiry) {
                earliestExpiry = expiryTime;
                nextCake = entry.getKey();
            }
        }
        
        return nextCake;
    }
    
    public long getTimeUntilNextExpiry() {

        String nextCake = getNextExpiringCake();
        if (nextCake == null) return -1;
        
        Long expiryTime = activeCakes.get(nextCake);
        if (expiryTime == null) return -1;
        
        return expiryTime - System.currentTimeMillis();
    }

    public String symbol(String cakeName) {
        String symbol = cakeName.replaceAll("[^\\p{So}]", "");
        return symbol.isEmpty() ? "?" : symbol;
    }

    public boolean hasActiveCakes() {
        long currentTime = System.currentTimeMillis();
        return activeCakes.values().stream().anyMatch(expiry -> expiry > currentTime);
    }
    
    public static String formatTimeLeft(long timeMs) {
        if (timeMs <= 0) return "Expired";
        
        long totalSeconds = timeMs / 1000;
        long days = totalSeconds / (24 * 3600);
        long hours = (totalSeconds % (24 * 3600)) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        
        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours, minutes);
        } else if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    private static final String KEY = "cakes";
    
    private void loadTimers() {

        if (!TIMER_FILE.exists()) {
            return;
        }
        
        try (FileReader reader = new FileReader(TIMER_FILE)) {
            Map<String, Object> data = GSON.fromJson(reader, new TypeToken<Map<String, Object>>(){}.getType());

            if (data != null && data.containsKey(KEY)) {
                activeCakes.clear();
                Object cakesObj = data.get(KEY);
                Map<String, Long> loadedTimers = GSON.fromJson(
                    GSON.toJson(cakesObj),
                    new TypeToken<Map<String, Long>>(){}.getType()
                );

                if (loadedTimers != null) {
                    activeCakes.putAll(loadedTimers);
                    long now = System.currentTimeMillis();
                    activeCakes.entrySet().removeIf(entry -> entry.getValue() <= now);
                }
            }

        } catch (IOException e) {
            System.err.println("Failed to load century cake timers: " + e.getMessage());
        }
    }
    
    private void saveTimers() {

        try {
            TIMER_FILE.getParentFile().mkdirs();
            Map<String, Object> data = new HashMap<>();

            if (TIMER_FILE.exists()) {
                try (FileReader reader = new FileReader(TIMER_FILE)) {
                    Map<String, Object> existing = GSON.fromJson(reader, new TypeToken<Map<String, Object>>(){}.getType());
                    if (existing != null) {
                        data.putAll(existing);
                    }
                }
            }
            
            data.put(KEY, activeCakes);
            
            try (FileWriter writer = new FileWriter(TIMER_FILE)) {
                GSON.toJson(data, writer);
            }

        } catch (IOException e) {
            System.err.println("Failed to save century cake timers: " + e.getMessage());
        }
    }
    
    public void shutdown() {
        saveTimers();
        scheduler.shutdown();
    }
}
