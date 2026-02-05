package org.runetale.skills;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import java.util.logging.Level;

/**
 * SkillsPlugin - A Hytale server plugin.
 */
public class SkillsPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static SkillsPlugin instance;

    public SkillsPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static SkillsPlugin getInstance() {
        return instance;
    }

    @Override
    protected void setup() {
        LOGGER.at(Level.INFO).log("Setting up...");

        // TODO: Register commands and listeners here
        // Use this.getEventRegistry() to register event listeners
        // Use this.getCommandRegistry() to register commands

        LOGGER.at(Level.INFO).log("Setup complete!");
    }

    @Override
    protected void start() {
        LOGGER.at(Level.INFO).log("Started!");
    }

    @Override
    protected void shutdown() {
        LOGGER.at(Level.INFO).log("Shutting down...");

        // TODO: Clean up resources for hot reload support
        // - Cancel scheduled tasks
        // - Close connections
        // - Registries are cleaned up automatically

        instance = null;
    }
}
