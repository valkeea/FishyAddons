package me.valkeea.fishyaddons.feature.skyblock.timer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.hud.core.ElementRegistry;
import me.valkeea.fishyaddons.hud.elements.segmented.EffectDisplay;
import me.valkeea.fishyaddons.tool.RunDelayed;
import me.valkeea.fishyaddons.util.FishyNotis;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

@SuppressWarnings("squid:S6548")
public class EffectTimers {
    private static final Logger LOGGER = LoggerFactory.getLogger("FishyAddons/Effects");
    private static final EffectTimers INSTANCE = new EffectTimers();
    public static EffectTimers getInstance() { return INSTANCE; }
    private EffectTimers() {}

    private static final File CONFIG = new File(MinecraftClient.getInstance().runDirectory, "config/fishyaddons/display.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String EFFECTS_KEY = "effects";
    private static final long WARN_EARLY = (5 * 60 * 1000);

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Set<String> notifiedItems = new HashSet<>();
    private boolean scheduledCheck = false;

    public static class SerializedEntry {
        private String displayName;
        private String textureId;
        private long expiresAt;
        private boolean pauseOffline;
        private String state = "RUNNING";
        private long pausedMs = -1L;

        public SerializedEntry() {}
        public SerializedEntry(String displayName, String textureId, long expiresAt, boolean pauseOffline, String state, long pausedMs) {
            this.displayName = displayName;
            this.textureId = textureId;
            this.expiresAt = expiresAt;
            this.pauseOffline = pauseOffline;
            this.state = state;
            this.pausedMs = pausedMs;
        }
    }

    public static class Entry {
        public enum State { RUNNING, PAUSED }

        public final String id;
        public final String displayName;
        public final Identifier textureId;
        public final boolean pauseOffline;

        private State state;
        private long expiresAt;
        private long pausedMs;

        private Entry(String id, String displayName, Identifier textureId, long expiresAt, boolean pauseOffline) {
            this.id = id;
            this.displayName = displayName;
            this.textureId = textureId;
            this.expiresAt = expiresAt;
            this.pauseOffline = pauseOffline;
            this.state = State.RUNNING;
            this.pausedMs = 0L;
        }

        public long remainingMs() {
            return state == State.PAUSED 
                ? pausedMs 
                : Math.max(0L, expiresAt - System.currentTimeMillis());
        }

        public boolean expired() { 
            return remainingMs() <= 0L; 
        }

        public void pause() {
            if (state == State.RUNNING && pauseOffline) {
                pausedMs = remainingMs();
                state = State.PAUSED;
            }
        }

        public void resume() {
            if (state == State.PAUSED) {
                expiresAt = System.currentTimeMillis() + pausedMs;
                state = State.RUNNING;
            }
        }

        public State getState() { return state; }
        public void setState(State state) { this.state = state; }
        public void setPausedMs(long pausedMs) { this.pausedMs = pausedMs; }
        public long getPausedMs() { return pausedMs; }
    }

    private final Map<String, Entry> active = new HashMap<>();

    public synchronized void init() {
        load();
        scheduleNotifications();

        FaEvents.ENVIRONMENT_CHANGE.register(event -> {
            if (!event.gameModeChanged()) return;
            if (event.isSkyblock()) {
                resumeAll();
                cleanupExpired();
            } else {
                pauseAll();
            }
        });
    }

    /** Add a cooldown, paused if offline. */
    public synchronized void addCooldown(String name, long durationMs, Identifier textureId) {
        addCooldown(name, durationMs, textureId, true);
    }

    /** Add a cooldown. */
    public synchronized void addCooldown(String name, long durationMs, Identifier textureId, boolean pauseOffline) {
        if (durationMs <= 0) return;

        cleanupExpired();

        String id = normalize(name);
        long expiresAt = System.currentTimeMillis() + durationMs;
        active.put(id, new Entry(id, name, textureId, expiresAt, pauseOffline));

        ElementRegistry.getElements().stream()
            .filter(EffectDisplay.class::isInstance)
            .map(e -> (EffectDisplay)e)
            .findFirst()
            .ifPresent(EffectDisplay::updateSpace);

        save();
    }

    /** Stack or add a cooldown. */
    public synchronized void stackCooldown(String name, long durationMs, Identifier textureId, boolean pauseOffline) {

        if (durationMs <= 0) return;
        cleanupExpired();

        String id = normalize(name);
        Entry existing = active.get(id);

        if (existing != null) {
            long remainingMs = existing.remainingMs();
            long newExpiresAt = System.currentTimeMillis() + remainingMs + durationMs;
            active.put(id, new Entry(id, name, textureId, newExpiresAt, pauseOffline));

        } else {
            long expiresAt = System.currentTimeMillis() + durationMs;
            active.put(id, new Entry(id, name, textureId, expiresAt, pauseOffline));
        }

        save();
    }

    public synchronized void remove(String name) {
        active.remove(normalize(name));
    }

    /** List of active, non-expired entries (copy). */
    public synchronized List<Entry> listActive() {
        cleanupExpired();
        if (active.isEmpty()) return Collections.emptyList();
        return new ArrayList<>(active.values());
    }

    public synchronized void clear() {
        active.clear();
    }

    private void cleanupExpired() {
        active.values().removeIf(Entry::expired);
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    public static String formatTime(long ms) {
        long totalSec = Math.max(0L, ms / 1000L);
        long d = totalSec / 86400;
        long h = (totalSec % 86400) / 3600;
        long m = (totalSec % 3600) / 60;
        long s = totalSec % 60;

        if (d > 0) return d + "d " + h + "h";
        if (h > 0) return h + "h " + m + "min";
        if (m > 0) return m + "min";
        return s + "s";
    } 

    private synchronized void load() {
        if (!CONFIG.exists()) {
            return;
        }

        try (var reader = new FileReader(CONFIG)) {
            Map<String, Object> data = GSON.fromJson(reader, new TypeToken<Map<String, Object>>(){}.getType());

            if (data == null || !data.containsKey(EFFECTS_KEY)) return;

            Object o = data.get(EFFECTS_KEY);
            Map<String, SerializedEntry> effects = GSON.fromJson(
                GSON.toJson(o),
                new TypeToken<Map<String, SerializedEntry>>(){}.getType()
            );

            if (effects != null) {

                active.clear();
                long now = System.currentTimeMillis();

                for (Map.Entry<String, SerializedEntry> entry : effects.entrySet()) {
                    SerializedEntry se = entry.getValue();
                    Identifier texture = Identifier.tryParse(se.textureId);
                    Entry loadedEntry = new Entry(entry.getKey(), se.displayName, texture, se.expiresAt, se.pauseOffline);

                    if ("PAUSED".equals(se.state) && se.pausedMs > 0) {
                        loadedEntry.setState(Entry.State.PAUSED);
                        loadedEntry.setPausedMs(se.pausedMs);
                        active.put(entry.getKey(), loadedEntry);

                    } else if (se.expiresAt > now) {
                        loadedEntry.setState(Entry.State.RUNNING);
                        active.put(entry.getKey(), loadedEntry);
                        
                    } else {
                        FishyNotis.send("§8" + se.displayName + " §7expired while you were away!");
                    }
                }
            }

        } catch (IOException e) {
            LOGGER.warn("Failed to load effect cooldowns: {}", e.getMessage());
        }
    }

    private synchronized void save() {
        try {
            CONFIG.getParentFile().mkdirs();

            Map<String, Object> data = new HashMap<>();
            if (CONFIG.exists()) {
                try (var reader = new FileReader(CONFIG)) {
                    Map<String, Object> existing = GSON.fromJson(reader, new TypeToken<Map<String, Object>>(){}.getType());
                    if (existing != null) data.putAll(existing);
                }
            }

            Map<String, SerializedEntry> effects = new HashMap<>();

            for (Map.Entry<String, Entry> entry : active.entrySet()) {
                Entry e = entry.getValue();

                if (!e.expired()) {
                    String stateStr = e.getState() == Entry.State.PAUSED ? "PAUSED" : "RUNNING";
                    long pausedMs = e.getState() == Entry.State.PAUSED ? e.getPausedMs() : -1L;
                    
                    effects.put(entry.getKey(), new SerializedEntry(
                        e.displayName,
                        e.textureId.toString(),
                        e.expiresAt,
                        e.pauseOffline,
                        stateStr,
                        pausedMs
                    ));
                }
            }

            data.put(EFFECTS_KEY, effects);

            try (FileWriter writer = new FileWriter(CONFIG)) {
                GSON.toJson(data, writer);
            }

        } catch (IOException e) {
            LOGGER.warn("Failed to save effect cooldowns: {}", e.getMessage());
        }
    }

    // Schedule periodic checks for expiring items
    private void scheduleNotifications() {
        if (scheduledCheck) return;
        scheduledCheck = true;

        scheduler.scheduleAtFixedRate(() -> {
            var mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.world == null) return;

            checkExpiring();
        }, 1, 1, TimeUnit.MINUTES);
    }

    private synchronized void checkExpiring() {

        for (Entry e : active.values()) {
            long timeLeft = e.remainingMs();
            String key = e.id + ":expiring";

            if (timeLeft > 0 && timeLeft <= WARN_EARLY && !notifiedItems.contains(key)) {
                warnUser(e, timeLeft);
                notifiedItems.add(key);
                if (hasNoHypixelAlert(key)) warnOnExpiration(e, timeLeft, key);
            }
        }
    }

    private boolean hasNoHypixelAlert(String key) {
        String lowerKey = key.toLowerCase(Locale.ROOT);
        return lowerKey.contains("flask") || lowerKey.contains("gummy");
    }

    private void warnOnExpiration(Entry entry, long timeLeft, String runKey) {
        RunDelayed.run(() -> {
            synchronized (EffectTimers.this) {
                warnUser(entry, 0);
                notifiedItems.remove(runKey);
            }
        }, timeLeft + 1000, runKey);
    }

    private static void warnUser(Entry entry, long timeLeft) {

        String info = notCooldown(entry.displayName) ? "expires in" : "cooldown ends in";
        String endInfo = notCooldown(entry.displayName) ? "has expired!" : "cooldown has ended!";

        if (timeLeft > 0) {
            FishyNotis.send("§8" + entry.displayName + " §7" + info + " §e" + formatTime(timeLeft) + "§7!");
        } else {
            FishyNotis.send("§8" + entry.displayName + " §7" + endInfo);
        }
    }

    private static boolean notCooldown(String id) {
        String lower = id.toLowerCase(Locale.ROOT);
        return !lower.contains("flask");
    }

    public synchronized void shutdown() {
        pauseAll();
        save();
        scheduler.shutdown();
    }

    private synchronized void pauseAll() {
        for (Entry e : active.values()) {
            e.pause();
        }
    }

    private synchronized void resumeAll() {
        for (Entry e : active.values()) {
            e.resume();
        }
    }
}
