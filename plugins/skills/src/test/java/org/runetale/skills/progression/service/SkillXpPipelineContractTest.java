package org.runetale.skills.progression.service;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.junit.jupiter.api.Test;
import org.runetale.skills.component.PlayerSkillProfileComponent;
import org.runetale.skills.config.XpConfig;
import org.runetale.skills.config.XpRoundingMode;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.progression.domain.SkillXpGrantResult;
import org.runetale.skills.progression.event.SkillXpGrantEvent;
import org.runetale.skills.service.DebugModeService;
import org.runetale.skills.service.XpService;
import org.runetale.testing.core.TestConstructors;
import org.runetale.testing.ecs.InMemoryComponentAccessor;
import org.runetale.testing.junit.ContractTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ContractTest
class SkillXpPipelineContractTest {

	@Test
	void dispatchAndProgressionApplyConsistentXpGain() {
		SkillXpDispatchService dispatchService = new SkillXpDispatchService(new DebugModeService(List.of("skills")));
		ComponentType<EntityStore, PlayerSkillProfileComponent> profileType = new ComponentType<>();
		InMemoryComponentAccessor<EntityStore> accessor = new InMemoryComponentAccessor<>(null);
		accessor.registerFactory(profileType,
				() -> TestConstructors.instantiateNoArgs(PlayerSkillProfileComponent.class));
		Ref<EntityStore> playerRef = mock(Ref.class);

		boolean dispatched = dispatchService.grantSkillXp(accessor, playerRef, SkillType.MINING, 10.4D,
				"node:mining_copper", true);

		assertThat(dispatched).isTrue();
		assertThat(accessor.getEntityInvocations()).hasSize(1);
		SkillXpGrantEvent event = (SkillXpGrantEvent) accessor.getEntityInvocations().getFirst().event();

		SkillProgressionService progressionService = new SkillProgressionService(profileType, createXpService());
		SkillXpGrantResult result = progressionService.grantExperience(accessor, playerRef, event.getSkillType(),
				event.getExperience());

		assertThat(result.getSkillType()).isEqualTo(SkillType.MINING);
		assertThat(result.getGainedExperience()).isEqualTo(10L);
		assertThat(result.getUpdatedExperience()).isEqualTo(10L);
		assertThat(result.getUpdatedLevel()).isEqualTo(1);

		PlayerSkillProfileComponent profile = accessor.getComponent(playerRef, profileType);
		assertThat(profile.getExperience(SkillType.MINING)).isEqualTo(10L);
	}

	private static XpService createXpService() {
		return new XpService(new XpConfig(
				99,
				1.0D,
				300.0D,
				2.0D,
				7.0D,
				4,
				XpRoundingMode.NEAREST));
	}
}
