package org.runetale.skills.service;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks players with actively open timed crafting pages.
 */
public class CraftingPageTrackerService {

	private final Map<UUID, UUID> trackedWorldByPlayerId = new ConcurrentHashMap<>();

	public void trackOpenPage(@Nonnull UUID playerId, @Nonnull UUID worldId) {
		this.trackedWorldByPlayerId.put(playerId, worldId);
	}

	public void untrackOpenPage(@Nonnull UUID playerId) {
		this.trackedWorldByPlayerId.remove(playerId);
	}

	@Nonnull
	public Map<UUID, UUID> snapshotTrackedPages() {
		return new HashMap<>(this.trackedWorldByPlayerId);
	}
}
