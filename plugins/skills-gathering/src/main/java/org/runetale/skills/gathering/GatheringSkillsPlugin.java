package org.runetale.skills.gathering;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;

public class GatheringSkillsPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public GatheringSkillsPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up skills gathering plugin scaffold...");
    }

    @Override
    protected void start() {
        LOGGER.atInfo().log("Started skills gathering plugin scaffold.");
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("Shutting down skills gathering plugin scaffold...");
    }
}
