package org.runetale.skills.service;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CraftingPageTrackerServiceTest {

	@Test
	void trackAndUntrackManageSnapshotContents() {
		CraftingPageTrackerService service = new CraftingPageTrackerService();
		UUID firstId = UUID.randomUUID();
		UUID secondId = UUID.randomUUID();
		Ref<EntityStore> firstRef = mock(Ref.class);
		Ref<EntityStore> secondRef = mock(Ref.class);

		service.trackOpenPage(firstId, firstRef);
		service.trackOpenPage(secondId, secondRef);

		assertThat(service.snapshotTrackedRefs()).containsExactlyInAnyOrder(firstRef, secondRef);

		service.untrackOpenPage(firstId);

		assertThat(service.snapshotTrackedRefs()).containsExactly(secondRef);
	}

	@Test
	void snapshotIsDetachedFromInternalState() {
		CraftingPageTrackerService service = new CraftingPageTrackerService();
		UUID playerId = UUID.randomUUID();
		Ref<EntityStore> ref = mock(Ref.class);

		service.trackOpenPage(playerId, ref);
		Collection<Ref<EntityStore>> snapshot = service.snapshotTrackedRefs();
		service.untrackOpenPage(playerId);

		assertThat(snapshot).containsExactly(ref);
		assertThat(service.snapshotTrackedRefs()).isEmpty();
	}
}
