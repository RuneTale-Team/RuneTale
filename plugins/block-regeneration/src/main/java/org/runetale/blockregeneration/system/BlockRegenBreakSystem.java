package org.runetale.blockregeneration.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.blockregeneration.service.BlockRegenCoordinatorService;
import org.runetale.blockregeneration.service.BlockRegenNotificationService;
import org.runetale.blockregeneration.service.BlockRegenRuntimeService;

import javax.annotation.Nonnull;

public class BlockRegenBreakSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    private final BlockRegenCoordinatorService coordinatorService;
    private final BlockRegenNotificationService notificationService;
    private final Query<EntityStore> query;

    public BlockRegenBreakSystem(
            @Nonnull BlockRegenCoordinatorService coordinatorService,
            @Nonnull BlockRegenNotificationService notificationService) {
        super(BreakBlockEvent.class);
        this.coordinatorService = coordinatorService;
        this.notificationService = notificationService;
        this.query = Query.and(PlayerRef.getComponentType());
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull BreakBlockEvent event) {
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

        BlockRegenCoordinatorService.HandleOutcome outcome = this.coordinatorService.handleSuccessfulInteraction(
                "break",
                world.getName(),
                target.x,
                target.y,
                target.z,
                event.getBlockType().getId(),
                System.currentTimeMillis());

        if (!outcome.matched() || outcome.result() == null) {
            return;
        }

        BlockRegenRuntimeService.GatherResult result = outcome.result();
        if (result.action() == BlockRegenRuntimeService.Action.BLOCKED_WAITING) {
            event.setCancelled(true);
            this.notificationService.sendDepletedNotice(playerRef);
            return;
        }

        world.setBlock(target.x, target.y, target.z, result.blockToSet());
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return this.query;
    }
}
