package me.valkeea.fishyaddons.util;

import net.minecraft.client.MinecraftClient;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Queues commands if sent too quickly and retries once after cooldown.
 * Commands are discarded after 2 seconds if not sent.
 */
public class ServerCommand {
    private ServerCommand() {}
    private static final long COMMAND_COOLDOWN_MS = 200;
    private static final long MAX_QUEUE_TIME_MS = 2000;
    
    private static long lastCommandTime = 0;
    private static final Queue<QueuedCommand> commandQueue = new LinkedList<>();
    
    /**
     * Send a command to the server with spam protection.
     * If cooldown is active, command will be queued for one retry attempt.
     * 
     * @param command The command to send (without the leading slash)
     */
    public static void send(String command) {
        var client = MinecraftClient.getInstance();
        
        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        if (client.player == null || client.player.networkHandler == null) {
            queueCommand(command);
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCommandTime < COMMAND_COOLDOWN_MS) {
            queueCommand(command);
            return;
        }

        sendImmediate(command);
    }
    
    /**
     * Send a command immediately without queuing.
     */
    private static void sendImmediate(String command) {
        var client = MinecraftClient.getInstance();
        
        if (client.player != null && client.player.networkHandler != null) {
            client.player.networkHandler.sendChatCommand(command);
            lastCommandTime = System.currentTimeMillis();
        }
    }
    
    /**
     * Queue a command.
     */
    private static void queueCommand(String command) {
        long currentTime = System.currentTimeMillis();
        commandQueue.offer(new QueuedCommand(command, currentTime));
    }
    
    /**
     * Process the command queue.
     */
    public static void tick() {
        if (commandQueue.isEmpty()) {
            return;
        }
        
        var client = MinecraftClient.getInstance();
        if (client.player == null || client.player.networkHandler == null) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastCommandTime >= COMMAND_COOLDOWN_MS) {
            QueuedCommand queued = commandQueue.peek();
            
            if (queued != null) {
                if (currentTime - queued.queueTime > MAX_QUEUE_TIME_MS) {
                    commandQueue.poll();
                    return;
                }

                commandQueue.poll();
                sendImmediate(queued.command);
            }
        }

        while (!commandQueue.isEmpty()) {
            QueuedCommand queued = commandQueue.peek();
            if (queued != null && currentTime - queued.queueTime > MAX_QUEUE_TIME_MS) {
                commandQueue.poll();
            } else {
                break;
            }
        }
    }
    
    /**
     * Get the number of commands currently in queue.
     */
    public static int getQueueSize() {
        return commandQueue.size();
    }
    
    /**
     * Clear all queued commands.
     */
    public static void clearQueue() {
        commandQueue.clear();
    }
    
    /**
     * Check if a command can be sent immediately without queuing.
     */
    public static boolean canSendImmediate() {
        return System.currentTimeMillis() - lastCommandTime >= COMMAND_COOLDOWN_MS;
    }
    
    /**
     * Queued commands with their queue time.
     */
    private static class QueuedCommand {
        final String command;
        final long queueTime;
        
        QueuedCommand(String command, long queueTime) {
            this.command = command;
            this.queueTime = queueTime;
        }
    }
}
