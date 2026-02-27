package org.runetale.skills.system;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.junit.jupiter.api.Test;
import org.runetale.skills.api.SkillsRuntimeApi;
import org.runetale.skills.config.CombatConfig;
import org.runetale.skills.domain.CombatStyleType;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.service.CombatStyleService;
import org.runetale.testing.junit.ContractTest;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.doubleThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ContractTest
class CombatDamageXpSystemContractTest {

	@Test
	void accurateMeleeDamageRoutesAllXpToAttack() {
		SkillsRuntimeApi runtimeApi = mock(SkillsRuntimeApi.class);
		CombatStyleService styleService = mock(CombatStyleService.class);
		CombatDamageXpSystem system = new CombatDamageXpSystem(runtimeApi, styleService, createConfig());

		CommandBuffer<EntityStore> commandBuffer = mock(CommandBuffer.class);
		Ref<EntityStore> attackerRef = mock(Ref.class);
		UUID attackerId = UUID.randomUUID();
		when(styleService.getCombatStyle(attackerId)).thenReturn(CombatStyleType.ACCURATE);

		Damage event = mock(Damage.class);
		when(event.getCause()).thenReturn(null);
		Damage.Source source = mock(Damage.EntitySource.class);

		invokeGrantAttackerDamageXp(system, commandBuffer, attackerRef, attackerId, event, source, 2.5D);

		verify(runtimeApi).grantSkillXp(
				eq(commandBuffer),
				eq(attackerRef),
				eq(SkillType.CONSTITUTION),
				closeTo(8.25D),
				eq("combat:constitution:damage"),
				eq(true));
		verify(runtimeApi).grantSkillXp(
				eq(commandBuffer),
				eq(attackerRef),
				eq(SkillType.ATTACK),
				eq(10.0D),
				eq("combat:melee:accurate"),
				eq(true));
		verifyNoMoreInteractions(runtimeApi);
	}

	@Test
	void controlledMeleeDamageSplitsXpAcrossAttackStrengthAndDefence() {
		SkillsRuntimeApi runtimeApi = mock(SkillsRuntimeApi.class);
		CombatStyleService styleService = mock(CombatStyleService.class);
		CombatDamageXpSystem system = new CombatDamageXpSystem(runtimeApi, styleService, createConfig());

		CommandBuffer<EntityStore> commandBuffer = mock(CommandBuffer.class);
		Ref<EntityStore> attackerRef = mock(Ref.class);
		UUID attackerId = UUID.randomUUID();
		when(styleService.getCombatStyle(attackerId)).thenReturn(CombatStyleType.CONTROLLED);

		Damage event = mock(Damage.class);
		when(event.getCause()).thenReturn(null);
		Damage.Source source = mock(Damage.EntitySource.class);

		invokeGrantAttackerDamageXp(system, commandBuffer, attackerRef, attackerId, event, source, 2.0D);

		verify(runtimeApi).grantSkillXp(
				eq(commandBuffer),
				eq(attackerRef),
				eq(SkillType.CONSTITUTION),
				closeTo(6.6D),
				eq("combat:constitution:damage"),
				eq(true));
		verify(runtimeApi).grantSkillXp(
				eq(commandBuffer),
				eq(attackerRef),
				eq(SkillType.ATTACK),
				eq(3.0D),
				eq("combat:melee:controlled:attack"),
				eq(true));
		verify(runtimeApi).grantSkillXp(
				eq(commandBuffer),
				eq(attackerRef),
				eq(SkillType.STRENGTH),
				eq(3.0D),
				eq("combat:melee:controlled:strength"),
				eq(true));
		verify(runtimeApi).grantSkillXp(
				eq(commandBuffer),
				eq(attackerRef),
				eq(SkillType.DEFENCE),
				eq(2.0D),
				eq("combat:melee:controlled:defence"),
				eq(true));
		verifyNoMoreInteractions(runtimeApi);
	}

	@Test
	void aggressiveMeleeDamageRoutesAllXpToStrength() {
		SkillsRuntimeApi runtimeApi = mock(SkillsRuntimeApi.class);
		CombatStyleService styleService = mock(CombatStyleService.class);
		CombatDamageXpSystem system = new CombatDamageXpSystem(runtimeApi, styleService, createConfig());

		CommandBuffer<EntityStore> commandBuffer = mock(CommandBuffer.class);
		Ref<EntityStore> attackerRef = mock(Ref.class);
		UUID attackerId = UUID.randomUUID();
		when(styleService.getCombatStyle(attackerId)).thenReturn(CombatStyleType.AGGRESSIVE);

		Damage event = mock(Damage.class);
		when(event.getCause()).thenReturn(null);
		Damage.Source source = mock(Damage.EntitySource.class);

		invokeGrantAttackerDamageXp(system, commandBuffer, attackerRef, attackerId, event, source, 3.0D);

		verify(runtimeApi).grantSkillXp(
				eq(commandBuffer),
				eq(attackerRef),
				eq(SkillType.CONSTITUTION),
				closeTo(9.9D),
				eq("combat:constitution:damage"),
				eq(true));
		verify(runtimeApi).grantSkillXp(
				eq(commandBuffer),
				eq(attackerRef),
				eq(SkillType.STRENGTH),
				eq(12.0D),
				eq("combat:melee:aggressive"),
				eq(true));
		verifyNoMoreInteractions(runtimeApi);
	}

	@Test
	void defensiveMeleeDamageRoutesAllXpToDefence() {
		SkillsRuntimeApi runtimeApi = mock(SkillsRuntimeApi.class);
		CombatStyleService styleService = mock(CombatStyleService.class);
		CombatDamageXpSystem system = new CombatDamageXpSystem(runtimeApi, styleService, createConfig());

		CommandBuffer<EntityStore> commandBuffer = mock(CommandBuffer.class);
		Ref<EntityStore> attackerRef = mock(Ref.class);
		UUID attackerId = UUID.randomUUID();
		when(styleService.getCombatStyle(attackerId)).thenReturn(CombatStyleType.DEFENSIVE);

		Damage event = mock(Damage.class);
		when(event.getCause()).thenReturn(null);
		Damage.Source source = mock(Damage.EntitySource.class);

		invokeGrantAttackerDamageXp(system, commandBuffer, attackerRef, attackerId, event, source, 3.0D);

		verify(runtimeApi).grantSkillXp(
				eq(commandBuffer),
				eq(attackerRef),
				eq(SkillType.CONSTITUTION),
				closeTo(9.9D),
				eq("combat:constitution:damage"),
				eq(true));
		verify(runtimeApi).grantSkillXp(
				eq(commandBuffer),
				eq(attackerRef),
				eq(SkillType.DEFENCE),
				eq(12.0D),
				eq("combat:melee:defensive"),
				eq(true));
		verifyNoMoreInteractions(runtimeApi);
	}

	@Test
	void controlledMeleeDamageDistributesSingleRemainderToAttackFirst() {
		SkillsRuntimeApi runtimeApi = mock(SkillsRuntimeApi.class);
		CombatStyleService styleService = mock(CombatStyleService.class);
		CombatDamageXpSystem system = new CombatDamageXpSystem(runtimeApi, styleService, createConfig());

		CommandBuffer<EntityStore> commandBuffer = mock(CommandBuffer.class);
		Ref<EntityStore> attackerRef = mock(Ref.class);
		UUID attackerId = UUID.randomUUID();
		when(styleService.getCombatStyle(attackerId)).thenReturn(CombatStyleType.CONTROLLED);

		Damage event = mock(Damage.class);
		when(event.getCause()).thenReturn(null);
		Damage.Source source = mock(Damage.EntitySource.class);

		invokeGrantAttackerDamageXp(system, commandBuffer, attackerRef, attackerId, event, source, 1.75D);

		verify(runtimeApi).grantSkillXp(
				eq(commandBuffer),
				eq(attackerRef),
				eq(SkillType.CONSTITUTION),
				closeTo(5.775D),
				eq("combat:constitution:damage"),
				eq(true));
		verify(runtimeApi).grantSkillXp(
				eq(commandBuffer),
				eq(attackerRef),
				eq(SkillType.ATTACK),
				eq(3.0D),
				eq("combat:melee:controlled:attack"),
				eq(true));
		verify(runtimeApi).grantSkillXp(
				eq(commandBuffer),
				eq(attackerRef),
				eq(SkillType.STRENGTH),
				eq(2.0D),
				eq("combat:melee:controlled:strength"),
				eq(true));
		verify(runtimeApi).grantSkillXp(
				eq(commandBuffer),
				eq(attackerRef),
				eq(SkillType.DEFENCE),
				eq(2.0D),
				eq("combat:melee:controlled:defence"),
				eq(true));
		verifyNoMoreInteractions(runtimeApi);
	}

	@Test
	void projectileDamageRoutesAllXpToRanged() {
		SkillsRuntimeApi runtimeApi = mock(SkillsRuntimeApi.class);
		CombatStyleService styleService = mock(CombatStyleService.class);
		CombatConfig config = new CombatConfig(
				4.0D,
				3.3D,
				10,
				10.0D,
				"Combat:Ranged",
				"Combat:Melee:",
				"Accurate",
				"Aggressive",
				"Defensive",
				"Controlled:Attack",
				"Controlled:Strength",
				"Controlled:Defence",
				"Combat:Block:Defence",
				"Combat:Constitution:Damage",
				"Combat:Constitution:Baseline",
				List.of("projectile"));
		CombatDamageXpSystem system = new CombatDamageXpSystem(runtimeApi, styleService, config);

		CommandBuffer<EntityStore> commandBuffer = mock(CommandBuffer.class);
		Ref<EntityStore> attackerRef = mock(Ref.class);
		UUID attackerId = UUID.randomUUID();

		Damage event = mock(Damage.class);
		Damage.Source source = mock(Damage.ProjectileSource.class);

		invokeGrantAttackerDamageXp(system, commandBuffer, attackerRef, attackerId, event, source, 2.0D);

		verify(runtimeApi).grantSkillXp(
				eq(commandBuffer),
				eq(attackerRef),
				eq(SkillType.CONSTITUTION),
				closeTo(6.6D),
				eq("combat:constitution:damage"),
				eq(true));
		verify(runtimeApi).grantSkillXp(
				eq(commandBuffer),
				eq(attackerRef),
				eq(SkillType.RANGED),
				eq(8.0D),
				eq("combat:ranged"),
				eq(true));
		verifyNoMoreInteractions(runtimeApi);
	}

	@Test
	void zeroOrNegativeEffectiveDamageDoesNotDispatchXp() {
		SkillsRuntimeApi runtimeApi = mock(SkillsRuntimeApi.class);
		CombatStyleService styleService = mock(CombatStyleService.class);
		CombatDamageXpSystem system = new CombatDamageXpSystem(runtimeApi, styleService, createConfig());

		CommandBuffer<EntityStore> commandBuffer = mock(CommandBuffer.class);
		Ref<EntityStore> attackerRef = mock(Ref.class);
		UUID attackerId = UUID.randomUUID();
		when(styleService.getCombatStyle(attackerId)).thenReturn(CombatStyleType.ACCURATE);

		Damage event = mock(Damage.class);
		Damage.Source source = mock(Damage.EntitySource.class);

		invokeGrantAttackerDamageXp(system, commandBuffer, attackerRef, attackerId, event, source, 0.0D);
		invokeGrantAttackerDamageXp(system, commandBuffer, attackerRef, attackerId, event, source, -4.0D);

		verifyNoMoreInteractions(runtimeApi);
	}

	private static CombatConfig createConfig() {
		return new CombatConfig(
				4.0D,
				3.3D,
				10,
				10.0D,
				"combat:ranged",
				"combat:melee:",
				"accurate",
				"aggressive",
				"defensive",
				"controlled:attack",
				"controlled:strength",
				"controlled:defence",
				"combat:block:defence",
				"combat:constitution:damage",
				"combat:constitution:baseline",
				List.of("projectile"));
	}

	private static void invokeGrantAttackerDamageXp(
			CombatDamageXpSystem system,
			CommandBuffer<EntityStore> commandBuffer,
			Ref<EntityStore> attackerRef,
			UUID attackerId,
			Damage event,
			Damage.Source source,
			double finalDamage) {
		try {
			Method method = CombatDamageXpSystem.class.getDeclaredMethod(
					"grantAttackerDamageXp",
					CommandBuffer.class,
					Ref.class,
					UUID.class,
					Damage.class,
					Damage.Source.class,
					double.class);
			method.setAccessible(true);
			method.invoke(system, commandBuffer, attackerRef, attackerId, event, source, finalDamage);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Unable to invoke grantAttackerDamageXp for contract testing", e);
		}
	}

	private static double closeTo(double expected) {
		return doubleThat(actual -> Math.abs(actual - expected) < 0.000001D);
	}
}
