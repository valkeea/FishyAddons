package me.valkeea.fishyaddons.tracker.collection;

import java.util.concurrent.atomic.AtomicLong;

import me.valkeea.fishyaddons.event.EventPhase;
import me.valkeea.fishyaddons.event.EventPriority;
import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.event.impl.ScreenOpenEvent;
import me.valkeea.fishyaddons.tool.RunDelayed;
import me.valkeea.fishyaddons.tracker.collection.RecipeScanner.SlotStackProvider;

/**
 * Main entry point for collection tracking.
 */
public class CollectionTracker {
    private CollectionTracker() {}

    private static final long SCAN_DELAY_MS = 200L;
    private static final long AUTO_PAUSE_THRESHOLD_MS = 90_000L;
    private static final long AUTO_RESET_THRESHOLD_MS = 900_000L;

    private static boolean initialized = false;
    private static boolean enabled = false;
    private static boolean downTiming = false;
    
    private static final AtomicLong startTime = new AtomicLong(-1);
    private static final AtomicLong pausedTime = new AtomicLong(0);
    private static final AtomicLong lastGainTime = new AtomicLong(0);
    private static final AtomicLong pausedFor = new AtomicLong(0);

    private static SlotStackProvider provider = null;        

    public static void initIfNeeded(boolean newState) {
        if (!newState || initialized) return;

        enabled = newState;
        
        CollectionData.init();
        CraftingRecipes.load();
        
        FaEvents.SCREEN_OPEN.register(
            event -> onScreenOpen(event), EventPriority.NORMAL, EventPhase.NORMAL
        );
        
        initialized = true;
    }

    /**
     * Handle screen open events.
     */
    private static void onScreenOpen(ScreenOpenEvent event) {
        if (!enabled) return;

        var title = event.titleString;
        if (title == null || title.isEmpty()) return;
        
        var lowerTitle = title.toLowerCase();
        var collection = lowerTitle.contains("collection");
        
        RunDelayed.run(() ->
        scan(event, title, collection), SCAN_DELAY_MS,
        "guiscan_" + System.currentTimeMillis());
    }

    private static void scan(ScreenOpenEvent event, String title, boolean collection) {

        var handler = event.screen.getScreenHandler();
        if (handler == null) return;

        var recipeSlot = handler.slots.get(32);
        var recipe = recipeSlot.hasStack()
                    && recipeSlot.getStack().getName().getString().toLowerCase().contains("supercraft");

        if (!recipe && !collection) return;

        provider = createSlotProvider(handler);
        
        if (recipe)  RecipeScanner.scanRecipeGui(provider);
        else ProgressScanner.scanCollectionGui(title, provider);
    }

    private static SlotStackProvider createSlotProvider(net.minecraft.screen.ScreenHandler handler) {
        return index -> {
            if (index >= 0 && index < handler.slots.size()) {
                return handler.slots.get(index).getStack();
            }
            return net.minecraft.item.ItemStack.EMPTY;
        };
    }

    /**
     * Add a drop to collection tracking.
     * Known collection: tracked immediately.
     * Known crafted: converted and tracked.
     * Unknown: added to pending drops for later verification.
     * 
     * @param itemName Name of the item
     * @param quantity Quantity gained
     */
    public static void addDrop(String itemName, int quantity) {
        if (!enabled || quantity <= 0) return;
        
        long now = System.currentTimeMillis();
        startTime.compareAndSet(-1, now);
        lastGainTime.set(now);
        
        var normalized = CollectionData.normalize(itemName);
        
        checkDt();
        
        if (CraftingRecipes.known(normalized)) {
            CollectionData.addGain(normalized, quantity);
            return;
        }
        
        if (CollectionData.knownBaseDrop(normalized)) {
            CollectionData.addGain(normalized, quantity);
            return;
        }
        
        if (CollectionData.isPotentialDrop(normalized)) {
            CollectionData.addPendingDrop(normalized, quantity);
        }
    }

    private static void checkDt() {
        if (downTiming) {
            toggleDownTime();
        } else {
            long paused = pausedTime.getAndSet(0);
            if (paused > 0) {
                pausedFor.addAndGet(System.currentTimeMillis() - paused);
            }
        }
    }    

    /**
     * Notify that a recipe was discovered.
     * Processes any pending drops for that item.
     */
    protected static void onRecipeDiscovered(String enchantedItemName) {
        if (!enabled) return;
        
        CollectionData.processPendingDrops(enchantedItemName);
    }

    /**
     * Session duration in milliseconds
     */
    public static long getSessionDuration() {
        return getTimeElapsedMs();
    }

    public static String getFormattedSessionDuration() {
        long durationMs = getTimeElapsedMs();
        if (durationMs == 0) {
            return "No session";
        }
        
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        minutes = minutes % 60;
        
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }

    public static boolean isPaused() {
        return pausedTime.get() > 0;
    }

    public static boolean isDownTiming() {
        return downTiming;
    }

    public static void toggleDownTime() {
        downTiming = !downTiming;
        long now = System.currentTimeMillis();
        
        if (downTiming) {
            pausedTime.compareAndSet(0, now);
        } else {
            long paused = pausedTime.getAndSet(0);
            if (paused > 0) {
                pausedFor.addAndGet(now - paused);
            }
            lastGainTime.set(0);
        }
    }

    public static void pauseTracking() {
        if (!downTiming) {
            long now = System.currentTimeMillis();
            pausedTime.compareAndSet(0, now);
        }
    }

    public static void tick() {
        if (!enabled) return;
        
        long now = System.currentTimeMillis();
        long lastGain = lastGainTime.get();
        long paused = pausedTime.get();
        
        if (lastGain > 0 && (now - lastGain) > AUTO_PAUSE_THRESHOLD_MS) {
            pauseTracking();
        }
        
        if (paused > 0 && (now - paused) > AUTO_RESET_THRESHOLD_MS && !downTiming) {
            resetSession();
        }
    }

    public static void resetSession() {
        startTime.set(-1);
        pausedTime.set(0);
        pausedFor.set(0);
        lastGainTime.set(0);
        downTiming = false;
        CollectionData.resetSession();
    }

    public static void shutdown() {
        CraftingRecipes.save();
        CollectionData.markAllBaselinesStale();
        CollectionData.resetSession();
    }

    public static void setEnabled(boolean state) {
        enabled = state;
    }

    public static boolean isEnabled() {
        return enabled;
    }    

    public static long getTimeElapsedMs() {
        long start = startTime.get();
        if (start == -1) return 0;
        
        long now = System.currentTimeMillis();
        long effectiveDuration = now - start - pausedFor.get();
        return Math.max(effectiveDuration, 0);
    }

    public static long getCurrentPauseDurationMs() {
        long paused = pausedTime.get();
        if (paused > 0) {
            return System.currentTimeMillis() - paused;
        }
        return 0;
    }

    /**
     * Get the SlotStackProvider for accessing recipe GUI stacks.
     * @return the current SlotStackProvider, or null if not in a recipe GUI context
     */
    protected static SlotStackProvider getProvider() {
        return provider;
    }
}
