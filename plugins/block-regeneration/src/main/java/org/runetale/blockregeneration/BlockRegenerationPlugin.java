package org.runetale.blockregeneration;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import org.runetale.blockregeneration.command.BlockRegenCommand;
import org.runetale.blockregeneration.config.BlockRegenExternalConfigBootstrap;
import org.runetale.blockregeneration.config.BlockRegenPathLayout;
import org.runetale.blockregeneration.service.BlockRegenConfigService;
import org.runetale.blockregeneration.service.BlockRegenCoordinatorService;
import org.runetale.blockregeneration.service.BlockRegenDefinitionService;
import org.runetale.blockregeneration.service.BlockRegenNotificationService;
import org.runetale.blockregeneration.service.BlockRegenPlacementQueueService;
import org.runetale.blockregeneration.service.BlockRegenRuntimeService;
import org.runetale.blockregeneration.system.BlockRegenBreakSystem;
import org.runetale.blockregeneration.system.BlockRegenDamageGateSystem;
import org.runetale.blockregeneration.system.BlockRegenPendingPlacementSystem;
import org.runetale.blockregeneration.system.BlockRegenRespawnSystem;

import javax.annotation.Nonnull;

public class BlockRegenerationPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private BlockRegenCoordinatorService coordinatorService;
    private BlockRegenNotificationService notificationService;

    public BlockRegenerationPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up block regeneration plugin...");
        BlockRegenPathLayout pathLayout = BlockRegenPathLayout.fromDataDirectory(this.getDataDirectory());
        BlockRegenExternalConfigBootstrap.seedMissingDefaults(pathLayout);

        BlockRegenConfigService configService = new BlockRegenConfigService(pathLayout.pluginConfigRoot());
        BlockRegenDefinitionService definitionService = new BlockRegenDefinitionService();
        BlockRegenRuntimeService runtimeService = new BlockRegenRuntimeService();
        BlockRegenPlacementQueueService placementQueueService = new BlockRegenPlacementQueueService();
        this.coordinatorService = new BlockRegenCoordinatorService(configService, definitionService, runtimeService,
                placementQueueService);
        this.notificationService = new BlockRegenNotificationService(() -> this.coordinatorService.notifyCooldownMillis());

        this.coordinatorService.initialize();
        this.getEntityStoreRegistry().registerSystem(new BlockRegenBreakSystem(this.coordinatorService, this.notificationService));
        this.getEntityStoreRegistry().registerSystem(new BlockRegenDamageGateSystem(this.coordinatorService, this.notificationService));
        this.getEntityStoreRegistry().registerSystem(new BlockRegenPendingPlacementSystem(this.coordinatorService));
        this.getEntityStoreRegistry().registerSystem(new BlockRegenRespawnSystem(this.coordinatorService));
        this.getCommandRegistry().registerCommand(new BlockRegenCommand(this.coordinatorService));

        LOGGER.atInfo().log("Block regeneration setup complete.");
    }

    @Override
    protected void start() {
        LOGGER.atInfo().log("Started block regeneration plugin.");
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("Shutting down block regeneration plugin...");
        if (this.notificationService != null) {
            this.notificationService.clear();
        }
        if (this.coordinatorService != null) {
            this.coordinatorService.clearRuntimeState();
        }
        this.coordinatorService = null;
        this.notificationService = null;
    }
}
