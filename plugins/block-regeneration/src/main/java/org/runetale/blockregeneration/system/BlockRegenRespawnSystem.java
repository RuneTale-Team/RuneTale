package org.runetale.blockregeneration.system;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.DelayedSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.blockregeneration.service.BlockRegenCoordinatorService;
import org.runetale.blockregeneration.service.BlockRegenRuntimeService;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BlockRegenRespawnSystem extends DelayedSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final float BASE_POLL_INTERVAL_SECONDS = 0.1F;

    private final BlockRegenCoordinatorService coordinatorService;
    private final Map<String, Long> nextPollAtMillisByWorld;

    public BlockRegenRespawnSystem(@Nonnull BlockRegenCoordinatorService coordinatorService) {
        super(BASE_POLL_INTERVAL_SECONDS);
        this.coordinatorService = coordinatorService;
        this.nextPollAtMillisByWorld = new ConcurrentHashMap<>();
    }

    @Override
    public void delayedTick(float deltaTime, int systemIndex, @Nonnull Store<EntityStore> store) {
        if (!this.coordinatorService.isEnabled()) {
            return;
        }

        World world = store.getExternalData().getWorld();
        String worldName = world.getName();
        long now = System.currentTimeMillis();
        Long nextPollAtMillis = this.nextPollAtMillisByWorld.get(worldName);
        if (nextPollAtMillis != null && now < nextPollAtMillis) {
            return;
        }
        this.nextPollAtMillisByWorld.put(worldName, now + this.coordinatorService.respawnTickMillis());

        List<BlockRegenRuntimeService.RespawnAction> actions = this.coordinatorService.pollDueRespawns(worldName, now);
        for (BlockRegenRuntimeService.RespawnAction action : actions) {
            try {
                world.setBlock(action.x(), action.y(), action.z(), action.sourceBlockId());
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log(
                        "[BlockRegen] Failed respawn apply world=%s pos=%d,%d,%d block=%s definition=%s",
                        action.worldName(),
                        action.x(),
                        action.y(),
                        action.z(),
                        action.sourceBlockId(),
                        action.definitionId());
            }
        }
    }
}
