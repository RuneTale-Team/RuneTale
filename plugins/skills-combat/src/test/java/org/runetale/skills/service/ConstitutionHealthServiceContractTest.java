package org.runetale.skills.service;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.runetale.skills.api.SkillsRuntimeApi;
import org.runetale.skills.config.CombatConfig;
import org.runetale.testing.junit.ContractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ContractTest
class ConstitutionHealthServiceContractTest {

	@Test
	void syncForLevelAppliesHealthModifierAndHealsByMaxHealthDelta() {
		SkillsRuntimeApi runtimeApi = mock(SkillsRuntimeApi.class);
		CombatConfig config = mock(CombatConfig.class);
		ConstitutionHealthService service = new ConstitutionHealthService(runtimeApi, config);

		ComponentAccessor<EntityStore> accessor = mock(ComponentAccessor.class);
		Ref<EntityStore> playerRef = mock(Ref.class);
		EntityStatMap statMap = mock(EntityStatMap.class);
		EntityStatValue before = mock(EntityStatValue.class);
		EntityStatValue after = mock(EntityStatValue.class);

		int healthStatIndex = DefaultEntityStatTypes.getHealth();
		when(config.constitutionBaseLevel()).thenReturn(10);
		when(config.constitutionHealthPerLevel()).thenReturn(10.0D);
		when(accessor.getComponent(playerRef, EntityStatMap.getComponentType())).thenReturn(statMap);
		when(statMap.get(healthStatIndex)).thenReturn(before, after);
		when(before.getMax()).thenReturn(100.0F);
		when(after.getMax()).thenReturn(110.0F);

		service.syncForLevel(accessor, playerRef, 11, true);

		ArgumentCaptor<Modifier> modifierCaptor = ArgumentCaptor.forClass(Modifier.class);
		verify(statMap).putModifier(eq(healthStatIndex), eq("skills:constitution:max_health"), modifierCaptor.capture());
		verify(statMap).addStatValue(eq(healthStatIndex), eq(10.0F));

		assertThat(modifierCaptor.getValue()).isInstanceOf(StaticModifier.class);
		StaticModifier staticModifier = (StaticModifier) modifierCaptor.getValue();
		assertThat(staticModifier.getTarget()).isEqualTo(Modifier.ModifierTarget.MAX);
		assertThat(staticModifier.getCalculationType()).isEqualTo(StaticModifier.CalculationType.ADDITIVE);
		assertThat(staticModifier.getAmount()).isEqualTo(10.0F);
	}

	@Test
	void syncForLevelRemovesModifierWhenLevelIsAtOrBelowBaseline() {
		SkillsRuntimeApi runtimeApi = mock(SkillsRuntimeApi.class);
		CombatConfig config = mock(CombatConfig.class);
		ConstitutionHealthService service = new ConstitutionHealthService(runtimeApi, config);

		ComponentAccessor<EntityStore> accessor = mock(ComponentAccessor.class);
		Ref<EntityStore> playerRef = mock(Ref.class);
		EntityStatMap statMap = mock(EntityStatMap.class);
		EntityStatValue health = mock(EntityStatValue.class);

		int healthStatIndex = DefaultEntityStatTypes.getHealth();
		when(config.constitutionBaseLevel()).thenReturn(10);
		when(config.constitutionHealthPerLevel()).thenReturn(10.0D);
		when(accessor.getComponent(playerRef, EntityStatMap.getComponentType())).thenReturn(statMap);
		when(statMap.get(healthStatIndex)).thenReturn(health, health);
		when(health.getMax()).thenReturn(100.0F);

		service.syncForLevel(accessor, playerRef, 10, true);

		verify(statMap).removeModifier(eq(healthStatIndex), eq("skills:constitution:max_health"));
		verify(statMap, never()).putModifier(eq(healthStatIndex), eq("skills:constitution:max_health"), any());
		verify(statMap, never()).addStatValue(eq(healthStatIndex), anyFloat());
	}
}
