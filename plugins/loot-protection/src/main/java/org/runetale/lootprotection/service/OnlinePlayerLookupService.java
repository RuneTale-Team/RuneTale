package org.runetale.lootprotection.service;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class OnlinePlayerLookupService {

    private final Map<UUID, Ref<EntityStore>> playerRefById = new ConcurrentHashMap<>();

    public void put(@Nonnull UUID playerId, @Nonnull Ref<EntityStore> playerRef) {
        this.playerRefById.put(playerId, playerRef);
    }

    @Nullable
    public Ref<EntityStore> getIfOnline(@Nonnull UUID playerId) {
        Ref<EntityStore> ref = this.playerRefById.get(playerId);
        if (ref == null) {
            return null;
        }
        if (!ref.isValid()) {
            this.playerRefById.remove(playerId, ref);
            return null;
        }
        return ref;
    }

    public void remove(@Nonnull UUID playerId, @Nonnull Ref<EntityStore> playerRef) {
        this.playerRefById.remove(playerId, playerRef);
    }

    public void clear() {
        this.playerRefById.clear();
    }
}
