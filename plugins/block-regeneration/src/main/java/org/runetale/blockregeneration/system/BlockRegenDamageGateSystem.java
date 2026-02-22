package org.runetale.blockregeneration.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.blockregeneration.service.BlockRegenCoordinatorService;
import org.runetale.blockregeneration.service.BlockRegenNotificationService;

import javax.annotation.Nonnull;

public class BlockRegenDamageGateSystem extends EntityEventSystem<EntityStore, DamageBlockEvent> {

    private final BlockRegenCoordinatorService coordinatorService;
    private final BlockRegenNotificationService notificationService;
    private final Query<EntityStore> query;

    public BlockRegenDamageGateSystem(
            @Nonnull BlockRegenCoordinatorService coordinatorService,
            @Nonnull BlockRegenNotificationService notificationService) {
        super(DamageBlockEvent.class);
        this.coordinatorService = coordinatorService;
        this.notificationService = notificationService;
        this.query = Query.and(PlayerRef.getComponentType());
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull DamageBlockEvent event) {
        if (event.isCancelled() || !this.coordinatorService.isEnabled()) {
            return;
        }

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        }

        Vector3i target = event.getTargetBlock();
        World world = store.getExternalData().getWorld();
        if (!this.coordinatorService.shouldBlockWaiting(world.getName(), target.x, target.y, target.z)) {
            BlockType hitBlockType = event.getBlockType();
            if (hitBlockType == null || this.coordinatorService.findInteractedDefinition(hitBlockType.getId()) == null) {
                return;
            }
        }

        event.setCancelled(true);
        this.notificationService.sendDepletedNotice(playerRef);
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return this.query;
    }
}
