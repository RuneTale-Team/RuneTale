package org.runetale.starterkit;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.starterkit.component.ReceivedStarterKitComponent;
import org.runetale.starterkit.config.StarterKitConfigService;
import org.runetale.starterkit.config.StarterKitExternalConfigBootstrap;
import org.runetale.starterkit.config.StarterKitPathLayout;
import org.runetale.starterkit.domain.StarterKitConfig;
import org.runetale.starterkit.system.GrantStarterKitSystem;

import javax.annotation.Nonnull;

public class StarterKitPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private StarterKitConfig config;
    private ComponentType<EntityStore, ReceivedStarterKitComponent> receivedKitComponentType;

    public StarterKitPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up starter kit plugin...");

        StarterKitPathLayout pathLayout = StarterKitPathLayout.fromDataDirectory(this.getDataDirectory());
        StarterKitExternalConfigBootstrap.seedMissingDefaults(pathLayout);

        StarterKitConfigService configService = new StarterKitConfigService(pathLayout.pluginConfigRoot());
        this.config = configService.load();

        this.receivedKitComponentType = this.getEntityStoreRegistry()
                .registerComponent(ReceivedStarterKitComponent.class, "ReceivedStarterKit",
                        ReceivedStarterKitComponent.CODEC);

        this.getEntityStoreRegistry()
                .registerSystem(new GrantStarterKitSystem(this.receivedKitComponentType, this.config));

        LOGGER.atInfo().log("Starter kit setup complete enabled=%s items=%d",
                this.config.enabled(), this.config.items().size());
    }

    @Override
    protected void start() {
        LOGGER.atInfo().log("Started starter kit plugin.");
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("Shutting down starter kit plugin...");
        this.config = null;
        this.receivedKitComponentType = null;
    }
}
