package org.runetale.skills.service;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks players with actively open timed crafting pages.
 */
public class CraftingPageTrackerService {

	private final Map<UUID, Ref<EntityStore>> craftingRefByPlayer = new ConcurrentHashMap<>();

	public void trackOpenPage(@Nonnull UUID playerId, @Nonnull Ref<EntityStore> playerRef) {
		this.craftingRefByPlayer.put(playerId, playerRef);
	}

	public void untrackOpenPage(@Nonnull UUID playerId) {
		this.craftingRefByPlayer.remove(playerId);
	}

	@Nonnull
	public Collection<Ref<EntityStore>> snapshotTrackedRefs() {
		return new ArrayList<>(this.craftingRefByPlayer.values());
	}
}
