package me.valkeea.fishyaddons.handler;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.util.FishyNotis;
import me.valkeea.fishyaddons.util.HelpUtil;
import net.minecraft.client.MinecraftClient;

public class CakeTimer {
    private static final CakeTimer INSTANCE = new CakeTimer();
    private static final File TIMER_FILE = new File(MinecraftClient.getInstance().runDirectory, "config/fishyaddons/display.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Pattern CAKE_PATTERN = Pattern.compile("(?:Big Yum! You refresh|Yum! You gain) (.+) for 48 hours!");

    // 48 hours in milliseconds
    private static final long CAKE_DURATION_MS = 48L * 60L * 60L * 1000L;
    
    private final Map<String, Long> activeCakes = new HashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private boolean hasScheduledExpiryCheck = false;
    
    private CakeTimer() {}
    
    public static CakeTimer getInstance() {
        return INSTANCE;
    }
    
    public void init() {
        loadTimers();
        scheduleNoti();
    }

    public boolean handleChat(String message) {
        String cleanMessage = HelpUtil.stripColor(message);
        Matcher matcher = CAKE_PATTERN.matcher(cleanMessage);

        if (matcher.find()) {
            String buffName = matcher.group(1);
            if (buffName != null) {
                buffName = buffName.trim();
            }

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
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) return;
            
            long currentTime = System.currentTimeMillis();
            boolean hasExpiredCakes = false;
            
            for (Map.Entry<String, Long> entry : activeCakes.entrySet()) {
                if (entry.getValue() <= currentTime) {
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
        if (hasScheduledExpiryCheck) return;
        hasScheduledExpiryCheck = true;
        
        // Check every minute for expired cakes
        scheduler.scheduleAtFixedRate(() -> {
            if (!FishyConfig.getState(Key.CAKE_NOTI, false)) {
                return;
            }
            
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.world == null) return;
            
            long currentTime = System.currentTimeMillis();
            
            for (Map.Entry<String, Long> entry : activeCakes.entrySet()) {
                long timeLeft = entry.getValue() - currentTime;
                
                // Notify 5 minutes before expiry
                if (timeLeft > 0 && timeLeft <= 5 * 60 * 1000) {
                    FishyNotis.send("§d" + entry.getKey() + " §7expires in §c" + formatTimeLeft(timeLeft) + "§7!");
                    activeCakes.remove(entry.getKey());
                    saveTimers();
                    break;
                }
            }
        }, 1, 1, TimeUnit.MINUTES);
    }
    
    public Map<String, Long> getActiveCakes() {
        // Clean up expired cakes
        long currentTime = System.currentTimeMillis();
        activeCakes.entrySet().removeIf(entry -> entry.getValue() <= currentTime);
        return new HashMap<>(activeCakes);
    }
    
    public String getNextExpiringCake() {
        long currentTime = System.currentTimeMillis();
        String nextCake = null;
        long earliestExpiry = Long.MAX_VALUE;
        
        for (Map.Entry<String, Long> entry : activeCakes.entrySet()) {
            long expiryTime = entry.getValue();
            if (expiryTime > currentTime && expiryTime < earliestExpiry) {
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
    
    private void loadTimers() {
        if (!TIMER_FILE.exists()) {
            return;
        }
        
        try (FileReader reader = new FileReader(TIMER_FILE)) {
            Map<String, Long> loadedTimers = GSON.fromJson(reader, new TypeToken<Map<String, Long>>(){}.getType());
            if (loadedTimers != null) {
                activeCakes.clear();
                activeCakes.putAll(loadedTimers);
                
                // Adjust timers based on system time
                long currentTime = System.currentTimeMillis();
                activeCakes.entrySet().removeIf(entry -> entry.getValue() <= currentTime);
            }
        } catch (IOException e) {
            System.err.println("Failed to load century cake timers: " + e.getMessage());
        }
    }
    
    private void saveTimers() {
        try {
            TIMER_FILE.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(TIMER_FILE)) {
                GSON.toJson(activeCakes, writer);
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
