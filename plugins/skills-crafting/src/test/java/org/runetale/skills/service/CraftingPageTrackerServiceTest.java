package org.runetale.skills.service;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CraftingPageTrackerServiceTest {

	@Test
	void trackAndUntrackManageSnapshotContents() {
		CraftingPageTrackerService service = new CraftingPageTrackerService();
		UUID firstId = UUID.randomUUID();
		UUID secondId = UUID.randomUUID();

		service.trackOpenPage(firstId);
		service.trackOpenPage(secondId);

		assertThat(service.snapshotTrackedPlayerIds()).containsExactlyInAnyOrder(firstId, secondId);

		service.untrackOpenPage(firstId);

		assertThat(service.snapshotTrackedPlayerIds()).containsExactly(secondId);
	}

	@Test
	void snapshotIsDetachedFromInternalState() {
		CraftingPageTrackerService service = new CraftingPageTrackerService();
		UUID playerId = UUID.randomUUID();

		service.trackOpenPage(playerId);
		Collection<UUID> snapshot = service.snapshotTrackedPlayerIds();
		service.untrackOpenPage(playerId);

		assertThat(snapshot).containsExactly(playerId);
		assertThat(service.snapshotTrackedPlayerIds()).isEmpty();
	}
}
