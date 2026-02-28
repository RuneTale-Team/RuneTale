package org.runetale.lootprotection;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.lootprotection.component.OwnedLootComponent;
import org.runetale.lootprotection.config.LootProtectionConfig;
import org.runetale.lootprotection.config.LootProtectionExternalConfigBootstrap;
import org.runetale.lootprotection.config.LootProtectionPathLayout;
import org.runetale.lootprotection.service.BlockOwnershipClaimService;
import org.runetale.lootprotection.service.DropClaimWindowService;
import org.runetale.lootprotection.service.LootProtectionConfigService;
import org.runetale.lootprotection.service.LootProtectionNotificationService;
import org.runetale.lootprotection.service.OnlinePlayerLookupService;
import org.runetale.lootprotection.service.OwnedLootDeliveryService;
import org.runetale.lootprotection.system.LootProtectionBreakBlockClaimSystem;
import org.runetale.lootprotection.system.LootProtectionDamageBlockClaimSystem;
import org.runetale.lootprotection.system.LootProtectionDeathDropClaimSystem;
import org.runetale.lootprotection.system.LootProtectionItemSpawnOwnershipSystem;
import org.runetale.lootprotection.system.LootProtectionOwnedItemTickSystem;
import org.runetale.lootprotection.system.LootProtectionPlayerRefIndexSystem;

import javax.annotation.Nonnull;

public class LootProtectionPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private LootProtectionConfig config;
    private BlockOwnershipClaimService blockOwnershipClaimService;
    private DropClaimWindowService dropClaimWindowService;
    private OnlinePlayerLookupService onlinePlayerLookupService;
    private LootProtectionNotificationService notificationService;
    private OwnedLootDeliveryService ownedLootDeliveryService;
    private ComponentType<EntityStore, OwnedLootComponent> ownedLootComponentType;

    public LootProtectionPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up loot protection plugin...");

        LootProtectionPathLayout pathLayout = LootProtectionPathLayout.fromDataDirectory(this.getDataDirectory());
        LootProtectionExternalConfigBootstrap.seedMissingDefaults(pathLayout);

        LootProtectionConfigService configService = new LootProtectionConfigService(pathLayout.pluginConfigRoot());
        this.config = configService.load().normalized();

        this.blockOwnershipClaimService = new BlockOwnershipClaimService();
        this.dropClaimWindowService = new DropClaimWindowService();
        this.onlinePlayerLookupService = new OnlinePlayerLookupService();
        this.notificationService = new LootProtectionNotificationService(
                () -> this.config.blockOwnership().notifyCooldownMillis(),
                () -> this.config.ownerLock().inventoryFullNotifyCooldownMillis());
        this.ownedLootDeliveryService = new OwnedLootDeliveryService(
                this.onlinePlayerLookupService,
                this.notificationService);

        this.ownedLootComponentType = this.getEntityStoreRegistry()
                .registerComponent(OwnedLootComponent.class, "OwnedLoot", OwnedLootComponent.CODEC);

        this.getEntityStoreRegistry().registerSystem(new LootProtectionPlayerRefIndexSystem(this.onlinePlayerLookupService));
        this.getEntityStoreRegistry().registerSystem(new LootProtectionDamageBlockClaimSystem(
                this.config,
                this.blockOwnershipClaimService,
                this.notificationService));
        this.getEntityStoreRegistry().registerSystem(new LootProtectionBreakBlockClaimSystem(
                this.config,
                this.blockOwnershipClaimService,
                this.dropClaimWindowService,
                this.notificationService));
        this.getEntityStoreRegistry().registerSystem(new LootProtectionDeathDropClaimSystem(
                this.config,
                this.dropClaimWindowService));
        this.getEntityStoreRegistry().registerSystem(new LootProtectionItemSpawnOwnershipSystem(
                this.config,
                this.dropClaimWindowService,
                this.ownedLootDeliveryService,
                this.ownedLootComponentType));
        this.getEntityStoreRegistry().registerSystem(new LootProtectionOwnedItemTickSystem(
                this.config,
                this.ownedLootComponentType,
                this.ownedLootDeliveryService));

        LOGGER.atInfo().log("Loot protection setup complete.");
    }

    @Override
    protected void start() {
        LOGGER.atInfo().log("Started loot protection plugin.");
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("Shutting down loot protection plugin...");
        if (this.notificationService != null) {
            this.notificationService.clear();
        }
        if (this.onlinePlayerLookupService != null) {
            this.onlinePlayerLookupService.clear();
        }
        if (this.blockOwnershipClaimService != null) {
            this.blockOwnershipClaimService.clear();
        }
        if (this.dropClaimWindowService != null) {
            this.dropClaimWindowService.clear();
        }
        this.config = null;
        this.blockOwnershipClaimService = null;
        this.dropClaimWindowService = null;
        this.onlinePlayerLookupService = null;
        this.notificationService = null;
        this.ownedLootDeliveryService = null;
        this.ownedLootComponentType = null;
    }
}
