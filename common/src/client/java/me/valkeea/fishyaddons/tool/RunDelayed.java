package me.valkeea.fishyaddons.tool;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.Nullable;

public class RunDelayed {

	private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static String lastRunKey = null;

	/**
	 * Runs the given action after the specified delay in milliseconds.
	 * @param action The Runnable to execute
	 * @param delayMillis Delay in milliseconds
     * @param runKey Optional key to prevent duplicate runs; if the same key is used consecutively, the action will not run again
	 */
	public static void run(Runnable action, long delayMillis, @Nullable String runKey) {

        if (runKey != null) {
            synchronized (RunDelayed.class) {
                if (runKey.equals(lastRunKey)) {
                    return;
                }
                lastRunKey = runKey;
            }
        }
		
		scheduler.schedule(action, delayMillis, TimeUnit.MILLISECONDS);
	}

	public static void shutdown() {
		scheduler.shutdown();
	}

	private RunDelayed() {}	
}
