package org.runetale.skills.actions.system;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.system.DelayedSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockBreakingDropType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockGathering;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.HarvestingDropType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.PhysicsDropType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.SoftBlockDropType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.interaction.BlockHarvestUtils;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.actions.service.ItemActionPlacementQueueService;
import org.runetale.skills.config.ItemActionsConfig;

import javax.annotation.Nonnull;
import java.util.List;

public class ItemActionPendingPlacementSystem extends DelayedSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final float POLL_INTERVAL_SECONDS = 0.05F;

    private final ItemActionPlacementQueueService placementQueueService;

    public ItemActionPendingPlacementSystem(@Nonnull ItemActionPlacementQueueService placementQueueService) {
        super(POLL_INTERVAL_SECONDS);
        this.placementQueueService = placementQueueService;
    }

    @Override
    public void delayedTick(float deltaTime, int systemIndex, @Nonnull Store<EntityStore> store) {
        long nowMillis = System.currentTimeMillis();
        World world = store.getExternalData().getWorld();
        List<ItemActionPlacementQueueService.PendingPlacement> due = this.placementQueueService.pollDueForWorld(
                world.getName(),
                nowMillis);
        if (due.isEmpty()) {
            return;
        }

        for (ItemActionPlacementQueueService.PendingPlacement placement : due) {
            try {
                if (!targetStillMatches(world, placement)) {
                    continue;
                }

                applyPlacement(store, world, placement);
            } catch (Exception exception) {
                LOGGER.atWarning().withCause(exception).log(
                        "[Skills Actions] Failed pending placement world=%s pos=%d,%d,%d mode=%s block=%s",
                        placement.position().worldName(),
                        placement.position().x(),
                        placement.position().y(),
                        placement.position().z(),
                        placement.applyMode(),
                        placement.replacementBlockId());
            }
        }
    }

    private void applyPlacement(
            @Nonnull Store<EntityStore> store,
            @Nonnull World world,
            @Nonnull ItemActionPlacementQueueService.PendingPlacement placement) {
        if (placement.applyMode() == ItemActionsConfig.BlockApplyMode.NATURAL_REMOVE) {
            applyNaturalRemoval(store, world, placement);
            return;
        }

        String replacementBlockId = placement.replacementBlockId();
        if (replacementBlockId == null || replacementBlockId.isBlank()) {
            LOGGER.atWarning().log(
                    "[Skills Actions] Skipped replacement due to blank set block id pos=%d,%d,%d",
                    placement.position().x(),
                    placement.position().y(),
                    placement.position().z());
            return;
        }

        world.setBlock(
                placement.position().x(),
                placement.position().y(),
                placement.position().z(),
                replacementBlockId);
    }

    private void applyNaturalRemoval(
            @Nonnull Store<EntityStore> entityStore,
            @Nonnull World world,
            @Nonnull ItemActionPlacementQueueService.PendingPlacement placement) {
        int x = placement.position().x();
        int y = placement.position().y();
        int z = placement.position().z();
        BlockType currentBlockType = world.getBlockType(x, y, z);
        if (currentBlockType == null) {
            return;
        }

        Ref<ChunkStore> chunkReference = world.getChunkStore().getChunkReference(ChunkUtil.indexChunkFromBlock(x, z));
        if (chunkReference == null || !chunkReference.isValid()) {
            return;
        }

        Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
        if (chunkStore == null) {
            return;
        }

        int filler = 0;
        BlockChunk blockChunk = chunkStore.getComponent(chunkReference, BlockChunk.getComponentType());
        if (blockChunk != null) {
            BlockSection blockSection = blockChunk.getSectionAtBlockY(y);
            filler = blockSection.getFiller(x, y, z);
        }

        int quantity = 1;
        String itemId = null;
        String dropListId = null;
        BlockGathering blockGathering = currentBlockType.getGathering();
        if (blockGathering != null) {
            PhysicsDropType physics = blockGathering.getPhysics();
            BlockBreakingDropType breaking = blockGathering.getBreaking();
            SoftBlockDropType soft = blockGathering.getSoft();
            HarvestingDropType harvest = blockGathering.getHarvest();
            if (physics != null) {
                itemId = physics.getItemId();
                dropListId = physics.getDropListId();
            } else if (breaking != null) {
                quantity = breaking.getQuantity();
                itemId = breaking.getItemId();
                dropListId = breaking.getDropListId();
            } else if (soft != null) {
                itemId = soft.getItemId();
                dropListId = soft.getDropListId();
            } else if (harvest != null) {
                itemId = harvest.getItemId();
                dropListId = harvest.getDropListId();
            }
        }

        int setBlockSettings = 256 | 32;
        BlockHarvestUtils.naturallyRemoveBlock(
                new Vector3i(x, y, z),
                currentBlockType,
                filler,
                quantity,
                itemId,
                dropListId,
                setBlockSettings,
                chunkReference,
                entityStore,
                chunkStore);
    }

    private boolean targetStillMatches(
            @Nonnull World world,
            @Nonnull ItemActionPlacementQueueService.PendingPlacement placement) {
        String expectedCurrentBlockId = placement.expectedCurrentBlockId();
        if (expectedCurrentBlockId == null || expectedCurrentBlockId.isBlank()) {
            return true;
        }

        BlockType currentBlockType = world.getBlockType(
                placement.position().x(),
                placement.position().y(),
                placement.position().z());
        if (currentBlockType == null) {
            return false;
        }

        String actualBlockId = currentBlockType.getId();
        boolean matches = ItemActionsConfig.ItemXpActionDefinition.idsMatch(expectedCurrentBlockId, actualBlockId);
        if (!matches) {
            LOGGER.atFine().log(
                    "[Skills Actions] Skipped replacement because target block changed expected=%s actual=%s pos=%d,%d,%d",
                    expectedCurrentBlockId,
                    actualBlockId,
                    placement.position().x(),
                    placement.position().y(),
                    placement.position().z());
        }
        return matches;
    }
}
