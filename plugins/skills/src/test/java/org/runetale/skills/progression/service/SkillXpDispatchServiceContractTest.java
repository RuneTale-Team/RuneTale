package org.runetale.skills.progression.service;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.junit.jupiter.api.Test;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.progression.event.SkillXpGrantEvent;
import org.runetale.testing.ecs.RecordingComponentAccessor;
import org.runetale.testing.junit.WithHytaleLogger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@WithHytaleLogger
class SkillXpDispatchServiceContractTest {

	@Test
	void grantSkillXpDispatchesEventWithNormalizedSource() {
		SkillXpDispatchService service = new SkillXpDispatchService();
		RecordingComponentAccessor<EntityStore> accessor = new RecordingComponentAccessor<>(null);
		Ref<EntityStore> playerRef = mock(Ref.class);

		boolean granted = service.grantSkillXp(accessor, playerRef, SkillType.MINING, 7.5D, "   ", true);

		assertThat(granted).isTrue();
		assertThat(accessor.getEntityInvocations()).hasSize(1);
		assertThat(accessor.getEntityInvocations().getFirst().ref()).isSameAs(playerRef);
		assertThat(accessor.getEntityInvocations().getFirst().event()).isInstanceOf(SkillXpGrantEvent.class);

		SkillXpGrantEvent event = (SkillXpGrantEvent) accessor.getEntityInvocations().getFirst().event();
		assertThat(event.getSkillType()).isEqualTo(SkillType.MINING);
		assertThat(event.getExperience()).isEqualTo(7.5D);
		assertThat(event.getSource()).isEqualTo("unspecified");
		assertThat(event.shouldNotifyPlayer()).isTrue();
	}

	@Test
	void grantSkillXpRejectsZeroExperienceWithoutDispatchingEvent() {
		SkillXpDispatchService service = new SkillXpDispatchService();
		RecordingComponentAccessor<EntityStore> accessor = new RecordingComponentAccessor<>(null);
		Ref<EntityStore> playerRef = mock(Ref.class);

		boolean granted = service.grantSkillXp(accessor, playerRef, SkillType.MINING, 0.0D, "node:ore", false);

		assertThat(granted).isFalse();
		assertThat(accessor.getEntityInvocations()).isEmpty();
	}

	@Test
	void grantSkillXpBySkillIdRejectsUnknownSkillWithoutDispatchingEvent() {
		SkillXpDispatchService service = new SkillXpDispatchService();
		RecordingComponentAccessor<EntityStore> accessor = new RecordingComponentAccessor<>(null);
		Ref<EntityStore> playerRef = mock(Ref.class);

		boolean granted = service.grantSkillXp(accessor, playerRef, "fishing", 10.0D, "command:test", false);

		assertThat(granted).isFalse();
		assertThat(accessor.getEntityInvocations()).isEmpty();
	}
}
