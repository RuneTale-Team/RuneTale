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
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.lootprotection.config.LootProtectionConfig;
import org.runetale.lootprotection.service.BlockOwnershipClaimService;
import org.runetale.lootprotection.service.LootProtectionNotificationService;

import javax.annotation.Nonnull;
import java.util.Set;

public class LootProtectionDamageBlockClaimSystem extends EntityEventSystem<EntityStore, DamageBlockEvent> {

    private final LootProtectionConfig config;
    private final BlockOwnershipClaimService blockOwnershipClaimService;
    private final LootProtectionNotificationService notificationService;
    private final Query<EntityStore> query;

    public LootProtectionDamageBlockClaimSystem(
            @Nonnull LootProtectionConfig config,
            @Nonnull BlockOwnershipClaimService blockOwnershipClaimService,
            @Nonnull LootProtectionNotificationService notificationService) {
        super(DamageBlockEvent.class);
        this.config = config;
        this.blockOwnershipClaimService = blockOwnershipClaimService;
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
        return RootDependency.firstSet();
    }

    @Override
    public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull DamageBlockEvent event) {
        if (event.isCancelled() || !this.config.enabled() || !this.config.blockOwnership().enabled()) {
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
        if (player != null && player.getGameMode() == GameMode.Creative) {
            return;
        }

        Vector3i target = event.getTargetBlock();
        String worldName = store.getExternalData().getWorld().getName();
        long now = System.currentTimeMillis();
        BlockOwnershipClaimService.ClaimResult claimResult = this.blockOwnershipClaimService.claimOrRefresh(
                worldName,
                target.x,
                target.y,
                target.z,
                playerRef.getUuid(),
                now,
                this.config.blockOwnership().inactivityResetMillis());

        if (!claimResult.isBlocked()) {
            return;
        }

        event.setCancelled(true);
        this.notificationService.sendContestedBlockNotice(playerRef);
    }
}
