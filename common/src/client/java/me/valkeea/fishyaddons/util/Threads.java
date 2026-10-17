package me.valkeea.fishyaddons.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

public final class Threads {

    private Threads() {}

    public static ThreadFactory factory(String name) {
        return r -> {
            Thread t = new Thread(r, name);
            t.setDaemon(true);
            return t;
        };
    }

    public static ScheduledExecutorService scheduler(String name) {
        return Executors.newSingleThreadScheduledExecutor(factory(name));
    }

    public static void startWorker(String name, Runnable task) {
        factory(name).newThread(task).start();
    }
}
