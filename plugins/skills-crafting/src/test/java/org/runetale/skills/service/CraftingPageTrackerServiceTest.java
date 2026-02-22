package org.runetale.skills.service;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CraftingPageTrackerServiceTest {

	@Test
	void trackAndUntrackManageSnapshotContents() {
		CraftingPageTrackerService service = new CraftingPageTrackerService();
		UUID firstId = UUID.randomUUID();
		UUID secondId = UUID.randomUUID();
		UUID worldId = UUID.randomUUID();

		service.trackOpenPage(firstId, worldId);
		service.trackOpenPage(secondId, worldId);

		assertThat(service.snapshotTrackedPages().keySet()).containsExactlyInAnyOrder(firstId, secondId);

		service.untrackOpenPage(firstId);

		assertThat(service.snapshotTrackedPages().keySet()).containsExactly(secondId);
	}

	@Test
	void snapshotIsDetachedFromInternalState() {
		CraftingPageTrackerService service = new CraftingPageTrackerService();
		UUID playerId = UUID.randomUUID();
		UUID worldId = UUID.randomUUID();

		service.trackOpenPage(playerId, worldId);
		Map<UUID, UUID> snapshot = service.snapshotTrackedPages();
		service.untrackOpenPage(playerId);

		assertThat(snapshot).containsEntry(playerId, worldId);
		assertThat(service.snapshotTrackedPages()).isEmpty();
	}

	@Test
	void trackingSamePlayerRebindsToLatestWorld() {
		CraftingPageTrackerService service = new CraftingPageTrackerService();
		UUID playerId = UUID.randomUUID();
		UUID firstWorldId = UUID.randomUUID();
		UUID secondWorldId = UUID.randomUUID();

		service.trackOpenPage(playerId, firstWorldId);
		service.trackOpenPage(playerId, secondWorldId);

		assertThat(service.snapshotTrackedPages()).containsEntry(playerId, secondWorldId);
	}
}
