package org.runetale.lootprotection.system;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.lootprotection.service.OnlinePlayerLookupService;

import javax.annotation.Nonnull;

public class LootProtectionPlayerRefIndexSystem extends RefSystem<EntityStore> {

    private final OnlinePlayerLookupService onlinePlayerLookupService;
    private final Query<EntityStore> query;

    public LootProtectionPlayerRefIndexSystem(@Nonnull OnlinePlayerLookupService onlinePlayerLookupService) {
        this.onlinePlayerLookupService = onlinePlayerLookupService;
        this.query = Query.and(PlayerRef.getComponentType());
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return this.query;
    }

    @Override
    public void onEntityAdded(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull AddReason reason,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        }
        if (playerRef == null) {
            return;
        }
        this.onlinePlayerLookupService.put(playerRef.getUuid(), ref);
    }

    @Override
    public void onEntityRemove(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull RemoveReason reason,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        }
        if (playerRef == null) {
            return;
        }
        this.onlinePlayerLookupService.remove(playerRef.getUuid(), ref);
    }
}
