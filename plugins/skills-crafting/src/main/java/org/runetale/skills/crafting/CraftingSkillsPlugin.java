package org.runetale.skills.crafting;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;

public class CraftingSkillsPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public CraftingSkillsPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up skills crafting plugin scaffold...");
    }

    @Override
    protected void start() {
        LOGGER.atInfo().log("Started skills crafting plugin scaffold.");
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("Shutting down skills crafting plugin scaffold...");
    }
}
