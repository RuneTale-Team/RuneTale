package org.runetale.blockregeneration.system;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.DelayedSystem;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.blockregeneration.service.BlockRegenCoordinatorService;
import org.runetale.blockregeneration.service.BlockRegenRuntimeService;

import javax.annotation.Nonnull;
import java.util.List;

public class BlockRegenRespawnSystem extends DelayedSystem<EntityStore> {

    private static final float BASE_POLL_INTERVAL_SECONDS = 0.1F;

    private final BlockRegenCoordinatorService coordinatorService;
    private long nextPollAtMillis;

    public BlockRegenRespawnSystem(@Nonnull BlockRegenCoordinatorService coordinatorService) {
        super(BASE_POLL_INTERVAL_SECONDS);
        this.coordinatorService = coordinatorService;
        this.nextPollAtMillis = 0L;
    }

    @Override
    public void delayedTick(float deltaTime, int systemIndex, @Nonnull Store<EntityStore> store) {
        if (!this.coordinatorService.isEnabled()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now < this.nextPollAtMillis) {
            return;
        }
        this.nextPollAtMillis = now + this.coordinatorService.respawnTickMillis();

        World world = store.getExternalData().getWorld();
        List<BlockRegenRuntimeService.RespawnAction> actions = this.coordinatorService.pollDueRespawns(world.getName(), now);
        for (BlockRegenRuntimeService.RespawnAction action : actions) {
            world.execute(() -> world.setBlock(action.x(), action.y(), action.z(), action.sourceBlockId()));
        }
    }
}
