package org.runetale.skills.service;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks players with actively open timed crafting pages.
 */
public class CraftingPageTrackerService {

	private final Set<UUID> trackedPlayerIds = ConcurrentHashMap.newKeySet();

	public void trackOpenPage(@Nonnull UUID playerId) {
		this.trackedPlayerIds.add(playerId);
	}

	public void untrackOpenPage(@Nonnull UUID playerId) {
		this.trackedPlayerIds.remove(playerId);
	}

	@Nonnull
	public Collection<UUID> snapshotTrackedPlayerIds() {
		return new ArrayList<>(this.trackedPlayerIds);
	}
}
