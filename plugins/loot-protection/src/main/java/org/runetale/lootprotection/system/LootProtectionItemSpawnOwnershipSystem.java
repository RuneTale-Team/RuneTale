package org.runetale.lootprotection.system;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.entity.item.PreventPickup;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerItemEntityPickupSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.lootprotection.component.OwnedLootComponent;
import org.runetale.lootprotection.config.LootProtectionConfig;
import org.runetale.lootprotection.service.DropClaimWindowService;
import org.runetale.lootprotection.service.OwnedLootDeliveryService;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.UUID;

public class LootProtectionItemSpawnOwnershipSystem extends RefSystem<EntityStore> {

    private static final Set<Dependency<EntityStore>> DEPENDENCIES =
            Set.of(new SystemDependency<>(Order.BEFORE, PlayerItemEntityPickupSystem.class));

    private final LootProtectionConfig config;
    private final DropClaimWindowService dropClaimWindowService;
    private final OwnedLootDeliveryService ownedLootDeliveryService;
    private final ComponentType<EntityStore, OwnedLootComponent> ownedLootComponentType;
    private final Query<EntityStore> query;

    public LootProtectionItemSpawnOwnershipSystem(
            @Nonnull LootProtectionConfig config,
            @Nonnull DropClaimWindowService dropClaimWindowService,
            @Nonnull OwnedLootDeliveryService ownedLootDeliveryService,
            @Nonnull ComponentType<EntityStore, OwnedLootComponent> ownedLootComponentType) {
        this.config = config;
        this.dropClaimWindowService = dropClaimWindowService;
        this.ownedLootDeliveryService = ownedLootDeliveryService;
        this.ownedLootComponentType = ownedLootComponentType;
        this.query = Query.and(ItemComponent.getComponentType(), TransformComponent.getComponentType());
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return this.query;
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return DEPENDENCIES;
    }

    @Override
    public void onEntityAdded(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull AddReason reason,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        if (reason != AddReason.SPAWN || !this.config.enabled() || !this.config.ownerLock().enabled()) {
            return;
        }

        if (commandBuffer.getComponent(ref, this.ownedLootComponentType) != null) {
            return;
        }

        ItemComponent itemComponent = commandBuffer.getComponent(ref, ItemComponent.getComponentType());
        if (itemComponent == null) {
            itemComponent = store.getComponent(ref, ItemComponent.getComponentType());
        }
        TransformComponent transformComponent = commandBuffer.getComponent(ref, TransformComponent.getComponentType());
        if (transformComponent == null) {
            transformComponent = store.getComponent(ref, TransformComponent.getComponentType());
        }
        if (itemComponent == null || transformComponent == null) {
            return;
        }

        long now = System.currentTimeMillis();
        String worldName = store.getExternalData().getWorld().getName();
        UUID ownerId = this.dropClaimWindowService.findOwnerForDrop(
                worldName,
                transformComponent.getPosition().getX(),
                transformComponent.getPosition().getY(),
                transformComponent.getPosition().getZ(),
                now,
                this.config.dropClaim().matchRadius());
        if (ownerId == null) {
            return;
        }

        commandBuffer.putComponent(ref, this.ownedLootComponentType, new OwnedLootComponent(
                ownerId,
                now,
                now + this.config.ownerLock().timeoutMillis(),
                0L));

        if (commandBuffer.getComponent(ref, PreventPickup.getComponentType()) == null) {
            commandBuffer.addComponent(ref, PreventPickup.getComponentType());
        }

        this.ownedLootDeliveryService.attemptDeliver(
                ref,
                itemComponent,
                ownerId,
                store,
                commandBuffer,
                true);
    }

    @Override
    public void onEntityRemove(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull RemoveReason reason,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {
    }
}
