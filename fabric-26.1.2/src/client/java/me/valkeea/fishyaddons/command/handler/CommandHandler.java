package me.valkeea.fishyaddons.command.handler;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

/**
 * Each handler is responsible for registering its own command tree.
 */
public interface CommandHandler {
    
    /**
     * Register this handler's subcommands into the provided builder.
     * 
     * @param builder the root argument builder to add subcommands to
     */
    void register(LiteralArgumentBuilder<FabricClientCommandSource> builder);
    
    /**
     * Get the root command name(s) this handler manages.
     * 
     * @return array of root command names (primary first)
     */
    String[] getRootNames();

    default void validate() {
        String[] roots = getRootNames();
        if (roots == null || roots.length == 0) {
            throw new IllegalStateException(
                "CommandHandler must provide at least one root name"
            );
        }
    }    
}
