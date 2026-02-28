package org.runetale.lootprotection.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.entity.item.PreventPickup;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.lootprotection.component.OwnedLootComponent;
import org.runetale.lootprotection.config.LootProtectionConfig;
import org.runetale.lootprotection.service.OwnedLootDeliveryService;

import javax.annotation.Nonnull;
import java.util.UUID;

public class LootProtectionOwnedItemTickSystem extends EntityTickingSystem<EntityStore> {

    private final LootProtectionConfig config;
    private final ComponentType<EntityStore, OwnedLootComponent> ownedLootComponentType;
    private final OwnedLootDeliveryService ownedLootDeliveryService;
    private final Query<EntityStore> query;

    public LootProtectionOwnedItemTickSystem(
            @Nonnull LootProtectionConfig config,
            @Nonnull ComponentType<EntityStore, OwnedLootComponent> ownedLootComponentType,
            @Nonnull OwnedLootDeliveryService ownedLootDeliveryService) {
        this.config = config;
        this.ownedLootComponentType = ownedLootComponentType;
        this.ownedLootDeliveryService = ownedLootDeliveryService;
        this.query = Query.and(this.ownedLootComponentType, ItemComponent.getComponentType());
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return this.query;
    }

    @Override
    public void tick(
            float dt,
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        OwnedLootComponent ownedLootComponent = archetypeChunk.getComponent(index, this.ownedLootComponentType);
        ItemComponent itemComponent = archetypeChunk.getComponent(index, ItemComponent.getComponentType());
        if (ownedLootComponent == null || itemComponent == null) {
            return;
        }

        if (!this.config.enabled() || !this.config.ownerLock().enabled()) {
            makePublic(ref, commandBuffer);
            return;
        }

        long now = System.currentTimeMillis();
        if (now >= ownedLootComponent.getPublicUnlockAtEpochMillis()) {
            makePublic(ref, commandBuffer);
            return;
        }

        UUID ownerId = ownedLootComponent.tryGetOwnerUuid();
        if (ownerId == null) {
            makePublic(ref, commandBuffer);
            return;
        }

        long retryIntervalMillis = this.config.ownerLock().retryIntervalMillis();
        if (now - ownedLootComponent.getLastTransferAttemptAtEpochMillis() < retryIntervalMillis) {
            return;
        }

        ownedLootComponent.setLastTransferAttemptAtEpochMillis(now);
        this.ownedLootDeliveryService.attemptDeliver(
                ref,
                itemComponent,
                ownerId,
                store,
                commandBuffer,
                true);
    }

    private void makePublic(@Nonnull Ref<EntityStore> itemRef, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        commandBuffer.tryRemoveComponent(itemRef, PreventPickup.getComponentType());
        commandBuffer.tryRemoveComponent(itemRef, this.ownedLootComponentType);
    }
}
