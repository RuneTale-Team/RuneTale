package org.runetale.skills.actions.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.actions.service.ItemActionPlacementQueueService;
import org.runetale.skills.config.ItemActionsConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ItemActionPlacementDespawnSystem extends EntityEventSystem<EntityStore, PlaceBlockEvent> {

    private final ItemActionPlacementQueueService placementQueueService;
    private final ItemActionsConfig itemActionsConfig;
    private final Query<EntityStore> query;

    public ItemActionPlacementDespawnSystem(
            @Nonnull ItemActionPlacementQueueService placementQueueService,
            @Nonnull ItemActionsConfig itemActionsConfig) {
        super(PlaceBlockEvent.class);
        this.placementQueueService = placementQueueService;
        this.itemActionsConfig = itemActionsConfig;
        this.query = Query.and(PlayerRef.getComponentType());
    }

    @Override
    public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull PlaceBlockEvent event) {
        if (event.isCancelled() || this.itemActionsConfig.placementDespawnRules().isEmpty()) {
            return;
        }

        Vector3i targetBlock = event.getTargetBlock();
        String placedBlockId = resolvePlacedBlockId(event, store, targetBlock);
        if (placedBlockId == null || placedBlockId.isBlank()) {
            return;
        }

        ItemActionsConfig.PlacementDespawnRule matchedRule = matchRule(placedBlockId);
        if (matchedRule == null || !matchedRule.hasDespawnAction()) {
            return;
        }

        World world = store.getExternalData().getWorld();
        String expectedBlockId = matchedRule.requireTargetBlockMatch()
                ? placedBlockId
                : null;
        this.placementQueueService.queue(
                world.getName(),
                targetBlock.x,
                targetBlock.y,
                targetBlock.z,
                expectedBlockId,
                matchedRule.setBlockId(),
                matchedRule.applyMode(),
                System.currentTimeMillis() + matchedRule.delayMillis());
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return this.query;
    }

    @Nullable
    private ItemActionsConfig.PlacementDespawnRule matchRule(@Nonnull String blockId) {
        for (ItemActionsConfig.PlacementDespawnRule rule : this.itemActionsConfig.placementDespawnRules()) {
            if (!rule.enabled()) {
                continue;
            }
            if (rule.matchesTargetBlockId(blockId)) {
                return rule;
            }
        }
        return null;
    }

    @Nullable
    private static String resolvePlacedBlockId(
            @Nonnull PlaceBlockEvent event,
            @Nonnull Store<EntityStore> store,
            @Nonnull Vector3i targetBlock) {
        ItemStack itemInHand = event.getItemInHand();
        if (itemInHand != null) {
            String blockKey = itemInHand.getBlockKey();
            if (blockKey != null && !blockKey.isBlank()) {
                return blockKey;
            }
        }

        World world = store.getExternalData().getWorld();
        BlockType currentBlockType = world.getBlockType(targetBlock.x, targetBlock.y, targetBlock.z);
        if (currentBlockType == null) {
            return null;
        }
        return currentBlockType.getId();
    }
}
