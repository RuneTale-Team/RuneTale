package org.runetale.blockregeneration;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import org.runetale.blockregeneration.config.BlockRegenExternalConfigBootstrap;
import org.runetale.blockregeneration.config.BlockRegenPathLayout;

import javax.annotation.Nonnull;

public class BlockRegenerationPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public BlockRegenerationPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up block regeneration plugin...");
        BlockRegenPathLayout pathLayout = BlockRegenPathLayout.fromDataDirectory(this.getDataDirectory());
        BlockRegenExternalConfigBootstrap.seedMissingDefaults(pathLayout);
        LOGGER.atInfo().log("Block regeneration setup complete.");
    }

    @Override
    protected void start() {
        LOGGER.atInfo().log("Started block regeneration plugin.");
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("Shutting down block regeneration plugin...");
    }
}
