package org.runetale.skills.system;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.runetale.skills.api.SkillsRuntimeApi;
import org.runetale.skills.config.CombatConfig;
import org.runetale.skills.domain.SkillType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Disabled("Requires runtime ECS component registration")
class ConstitutionBaselineSystemTest {

	@Test
	void onEntityAddedQueuesBaselineConstitutionXpWhenMissing() {
		SkillsRuntimeApi runtimeApi = mock(SkillsRuntimeApi.class);
		CombatConfig combatConfig = mock(CombatConfig.class);
		ConstitutionBaselineSystem system = new ConstitutionBaselineSystem(runtimeApi, combatConfig);

		Ref<EntityStore> playerRef = mock(Ref.class);
		Store<EntityStore> store = mock(Store.class);
		CommandBuffer<EntityStore> commandBuffer = mock(CommandBuffer.class);

		when(combatConfig.constitutionBaseLevel()).thenReturn(10);
		when(combatConfig.sourceConstitutionBaseline()).thenReturn("combat:constitution:baseline");
		when(runtimeApi.xpForLevel(10)).thenReturn(1358L);
		when(runtimeApi.getSkillExperience(commandBuffer, playerRef, SkillType.CONSTITUTION)).thenReturn(58L);

		system.onEntityAdded(playerRef, AddReason.SPAWN, store, commandBuffer);

		verify(runtimeApi).grantSkillXp(
				eq(commandBuffer),
				eq(playerRef),
				eq(SkillType.CONSTITUTION),
				eq(1300.0D),
				eq("combat:constitution:baseline"),
				eq(false));
	}

	@Test
	void onEntityAddedSkipsWhenPlayerAlreadyMeetsBaseline() {
		SkillsRuntimeApi runtimeApi = mock(SkillsRuntimeApi.class);
		CombatConfig combatConfig = mock(CombatConfig.class);
		ConstitutionBaselineSystem system = new ConstitutionBaselineSystem(runtimeApi, combatConfig);

		Ref<EntityStore> playerRef = mock(Ref.class);
		Store<EntityStore> store = mock(Store.class);
		CommandBuffer<EntityStore> commandBuffer = mock(CommandBuffer.class);

		when(combatConfig.constitutionBaseLevel()).thenReturn(10);
		when(runtimeApi.xpForLevel(10)).thenReturn(1358L);
		when(runtimeApi.getSkillExperience(commandBuffer, playerRef, SkillType.CONSTITUTION)).thenReturn(1500L);

		system.onEntityAdded(playerRef, AddReason.SPAWN, store, commandBuffer);

		verify(runtimeApi, never()).grantSkillXp(
				any(),
				any(),
				any(),
				anyDouble(),
				anyString(),
				anyBoolean());
	}
}
