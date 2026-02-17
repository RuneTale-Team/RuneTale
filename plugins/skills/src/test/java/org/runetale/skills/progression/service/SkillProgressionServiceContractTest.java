package org.runetale.skills.progression.service;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.junit.jupiter.api.Test;
import org.runetale.skills.component.PlayerSkillProfileComponent;
import org.runetale.skills.config.XpConfig;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.progression.domain.SkillXpGrantResult;
import org.runetale.skills.service.XpService;
import org.runetale.testing.ecs.InMemoryComponentAccessor;
import org.runetale.testing.junit.ContractTest;
import org.runetale.testing.core.TestConstructors;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ContractTest
class SkillProgressionServiceContractTest {

	@Test
	void grantExperienceCreatesProfileAndAppliesRoundedGain() {
		ComponentType<EntityStore, PlayerSkillProfileComponent> profileType = new ComponentType<>();
		InMemoryComponentAccessor<EntityStore> accessor = new InMemoryComponentAccessor<>(null);
		accessor.registerFactory(profileType,
				() -> TestConstructors.instantiateNoArgs(PlayerSkillProfileComponent.class));
		Ref<EntityStore> playerRef = mock(Ref.class);
		XpService xpService = new XpService(XpConfig.load(Path.of(".missing-test-config")));
		SkillProgressionService service = new SkillProgressionService(profileType, xpService);

		SkillXpGrantResult result = service.grantExperience(accessor, playerRef, SkillType.MINING, 7.5D);

		assertThat(result.getPreviousExperience()).isZero();
		assertThat(result.getUpdatedExperience()).isEqualTo(8L);
		assertThat(result.getGainedExperience()).isEqualTo(8L);
		assertThat(result.getPreviousLevel()).isEqualTo(1);
		assertThat(result.getUpdatedLevel()).isEqualTo(1);

		PlayerSkillProfileComponent profile = accessor.getComponent(playerRef, profileType);
		assertThat(profile).isNotNull();
		assertThat(profile.getExperience(SkillType.MINING)).isEqualTo(8L);
		assertThat(profile.getLevel(SkillType.MINING)).isEqualTo(1);
	}

	@Test
	void grantExperienceCanTriggerLevelUpFromThresholdGain() {
		ComponentType<EntityStore, PlayerSkillProfileComponent> profileType = new ComponentType<>();
		InMemoryComponentAccessor<EntityStore> accessor = new InMemoryComponentAccessor<>(null);
		accessor.registerFactory(profileType,
				() -> TestConstructors.instantiateNoArgs(PlayerSkillProfileComponent.class));
		Ref<EntityStore> playerRef = mock(Ref.class);
		XpService xpService = new XpService(XpConfig.load(Path.of(".missing-test-config")));
		SkillProgressionService service = new SkillProgressionService(profileType, xpService);

		long levelTwoThreshold = xpService.xpForLevel(2);
		SkillXpGrantResult result = service.grantExperience(accessor, playerRef, SkillType.WOODCUTTING, levelTwoThreshold);

		assertThat(result.getPreviousLevel()).isEqualTo(1);
		assertThat(result.getUpdatedLevel()).isGreaterThanOrEqualTo(2);
		assertThat(result.isLevelUp()).isTrue();

		PlayerSkillProfileComponent profile = accessor.getComponent(playerRef, profileType);
		assertThat(profile.getLevel(SkillType.WOODCUTTING)).isEqualTo(result.getUpdatedLevel());
		assertThat(profile.getExperience(SkillType.WOODCUTTING)).isEqualTo(result.getUpdatedExperience());
	}

	@Test
	void grantExperienceWithZeroGainLeavesExistingProgressUnchanged() {
		ComponentType<EntityStore, PlayerSkillProfileComponent> profileType = new ComponentType<>();
		InMemoryComponentAccessor<EntityStore> accessor = new InMemoryComponentAccessor<>(null);
		accessor.registerFactory(profileType,
				() -> TestConstructors.instantiateNoArgs(PlayerSkillProfileComponent.class));
		Ref<EntityStore> playerRef = mock(Ref.class);
		XpService xpService = new XpService(XpConfig.load(Path.of(".missing-test-config")));
		SkillProgressionService service = new SkillProgressionService(profileType, xpService);

		SkillXpGrantResult first = service.grantExperience(accessor, playerRef, SkillType.SMITHING, 15.0D);
		SkillXpGrantResult second = service.grantExperience(accessor, playerRef, SkillType.SMITHING, 0.0D);

		assertThat(second.getPreviousExperience()).isEqualTo(first.getUpdatedExperience());
		assertThat(second.getUpdatedExperience()).isEqualTo(first.getUpdatedExperience());
		assertThat(second.getGainedExperience()).isZero();
		assertThat(second.getUpdatedLevel()).isEqualTo(first.getUpdatedLevel());

		PlayerSkillProfileComponent profile = accessor.getComponent(playerRef, profileType);
		assertThat(profile.getExperience(SkillType.SMITHING)).isEqualTo(first.getUpdatedExperience());
		assertThat(profile.getLevel(SkillType.SMITHING)).isEqualTo(first.getUpdatedLevel());
	}

}
