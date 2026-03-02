package me.valkeea.fishyaddons.tool;

import org.jetbrains.annotations.Nullable;

public class RunDelayed {

    private static String lastRunKey = null;

	/**
	 * Runs the given action after the specified delay in milliseconds using a virtual thread.
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
		
		Thread.startVirtualThread(() -> {
			try {
				Thread.sleep(delayMillis);
				action.run();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
	}

	private RunDelayed() {}	
}
