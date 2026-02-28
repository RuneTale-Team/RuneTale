package org.runetale.lootprotection.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.lootprotection.config.LootProtectionConfig;
import org.runetale.lootprotection.service.BlockOwnershipClaimService;
import org.runetale.lootprotection.service.DropClaimWindowService;
import org.runetale.lootprotection.service.LootProtectionNotificationService;

import javax.annotation.Nonnull;
import java.util.Set;

public class LootProtectionBreakBlockClaimSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    private final LootProtectionConfig config;
    private final BlockOwnershipClaimService blockOwnershipClaimService;
    private final DropClaimWindowService dropClaimWindowService;
    private final LootProtectionNotificationService notificationService;
    private final Query<EntityStore> query;

    public LootProtectionBreakBlockClaimSystem(
            @Nonnull LootProtectionConfig config,
            @Nonnull BlockOwnershipClaimService blockOwnershipClaimService,
            @Nonnull DropClaimWindowService dropClaimWindowService,
            @Nonnull LootProtectionNotificationService notificationService) {
        super(BreakBlockEvent.class);
        this.config = config;
        this.blockOwnershipClaimService = blockOwnershipClaimService;
        this.dropClaimWindowService = dropClaimWindowService;
        this.notificationService = notificationService;
        this.query = Query.and(PlayerRef.getComponentType());
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return this.query;
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return RootDependency.lastSet();
    }

    @Override
    public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull BreakBlockEvent event) {
        if (event.isCancelled() || !this.config.enabled()) {
            return;
        }

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        }
        if (playerRef == null) {
            return;
        }

        Player player = commandBuffer.getComponent(ref, Player.getComponentType());
        if (player == null) {
            player = store.getComponent(ref, Player.getComponentType());
        }

        Vector3i target = event.getTargetBlock();
        String worldName = store.getExternalData().getWorld().getName();
        if (player != null && player.getGameMode() == GameMode.Creative) {
            this.blockOwnershipClaimService.clear(worldName, target.x, target.y, target.z);
            return;
        }

        if (this.config.blockOwnership().enabled()) {
            BlockOwnershipClaimService.ClaimResult claimResult = this.blockOwnershipClaimService.claimOrRefresh(
                    worldName,
                    target.x,
                    target.y,
                    target.z,
                    playerRef.getUuid(),
                    System.currentTimeMillis(),
                    this.config.blockOwnership().inactivityResetMillis());
            if (claimResult.isBlocked()) {
                event.setCancelled(true);
                this.notificationService.sendContestedBlockNotice(playerRef);
                return;
            }
        }

        if (this.config.protectBlockBreakDrops() && this.config.ownerLock().enabled()) {
            long now = System.currentTimeMillis();
            this.dropClaimWindowService.openWindow(
                    worldName,
                    target.x + 0.5D,
                    target.y + 0.5D,
                    target.z + 0.5D,
                    playerRef.getUuid(),
                    now,
                    this.config.dropClaim().windowMillis());
        }

        this.blockOwnershipClaimService.clear(worldName, target.x, target.y, target.z);
    }
}
