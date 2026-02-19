package org.runetale.skills.system;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.junit.jupiter.api.Test;
import org.runetale.skills.config.CombatConfig;
import org.runetale.skills.domain.CombatStyleType;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.progression.service.SkillXpDispatchService;
import org.runetale.skills.service.CombatStyleService;
import org.runetale.testing.junit.ContractTest;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ContractTest
class CombatDamageXpSystemContractTest {

	@Test
	void accurateMeleeDamageRoutesAllXpToAttack() {
		SkillXpDispatchService dispatchService = mock(SkillXpDispatchService.class);
		CombatStyleService styleService = mock(CombatStyleService.class);
		CombatDamageXpSystem system = new CombatDamageXpSystem(dispatchService, styleService, createConfig());

		CommandBuffer<EntityStore> commandBuffer = mock(CommandBuffer.class);
		Ref<EntityStore> attackerRef = mock(Ref.class);
		UUID attackerId = UUID.randomUUID();
		when(styleService.getCombatStyle(attackerId)).thenReturn(CombatStyleType.ACCURATE);

		Damage event = mock(Damage.class);
		when(event.getCause()).thenReturn(null);
		Damage.Source source = mock(Damage.EntitySource.class);

		invokeGrantAttackerDamageXp(system, commandBuffer, attackerRef, attackerId, event, source, 2.5D);

		verify(dispatchService).grantSkillXp(
				eq(commandBuffer),
				eq(attackerRef),
				eq(SkillType.ATTACK),
				eq(10.0D),
				eq("combat:melee:accurate"),
				eq(true));
		verifyNoMoreInteractions(dispatchService);
	}

	@Test
	void controlledMeleeDamageSplitsXpAcrossAttackStrengthAndDefense() {
		SkillXpDispatchService dispatchService = mock(SkillXpDispatchService.class);
		CombatStyleService styleService = mock(CombatStyleService.class);
		CombatDamageXpSystem system = new CombatDamageXpSystem(dispatchService, styleService, createConfig());

		CommandBuffer<EntityStore> commandBuffer = mock(CommandBuffer.class);
		Ref<EntityStore> attackerRef = mock(Ref.class);
		UUID attackerId = UUID.randomUUID();
		when(styleService.getCombatStyle(attackerId)).thenReturn(CombatStyleType.CONTROLLED);

		Damage event = mock(Damage.class);
		when(event.getCause()).thenReturn(null);
		Damage.Source source = mock(Damage.EntitySource.class);

		invokeGrantAttackerDamageXp(system, commandBuffer, attackerRef, attackerId, event, source, 2.0D);

		verify(dispatchService).grantSkillXp(
				eq(commandBuffer),
				eq(attackerRef),
				eq(SkillType.ATTACK),
				eq(3.0D),
				eq("combat:melee:controlled:attack"),
				eq(true));
		verify(dispatchService).grantSkillXp(
				eq(commandBuffer),
				eq(attackerRef),
				eq(SkillType.STRENGTH),
				eq(3.0D),
				eq("combat:melee:controlled:strength"),
				eq(true));
		verify(dispatchService).grantSkillXp(
				eq(commandBuffer),
				eq(attackerRef),
				eq(SkillType.DEFENSE),
				eq(2.0D),
				eq("combat:melee:controlled:defense"),
				eq(true));
		verifyNoMoreInteractions(dispatchService);
	}

	@Test
	void aggressiveMeleeDamageRoutesAllXpToStrength() {
		SkillXpDispatchService dispatchService = mock(SkillXpDispatchService.class);
		CombatStyleService styleService = mock(CombatStyleService.class);
		CombatDamageXpSystem system = new CombatDamageXpSystem(dispatchService, styleService, createConfig());

		CommandBuffer<EntityStore> commandBuffer = mock(CommandBuffer.class);
		Ref<EntityStore> attackerRef = mock(Ref.class);
		UUID attackerId = UUID.randomUUID();
		when(styleService.getCombatStyle(attackerId)).thenReturn(CombatStyleType.AGGRESSIVE);

		Damage event = mock(Damage.class);
		when(event.getCause()).thenReturn(null);
		Damage.Source source = mock(Damage.EntitySource.class);

		invokeGrantAttackerDamageXp(system, commandBuffer, attackerRef, attackerId, event, source, 3.0D);

		verify(dispatchService).grantSkillXp(
				eq(commandBuffer),
				eq(attackerRef),
				eq(SkillType.STRENGTH),
				eq(12.0D),
				eq("combat:melee:aggressive"),
				eq(true));
		verifyNoMoreInteractions(dispatchService);
	}

	@Test
	void defensiveMeleeDamageRoutesAllXpToDefense() {
		SkillXpDispatchService dispatchService = mock(SkillXpDispatchService.class);
		CombatStyleService styleService = mock(CombatStyleService.class);
		CombatDamageXpSystem system = new CombatDamageXpSystem(dispatchService, styleService, createConfig());

		CommandBuffer<EntityStore> commandBuffer = mock(CommandBuffer.class);
		Ref<EntityStore> attackerRef = mock(Ref.class);
		UUID attackerId = UUID.randomUUID();
		when(styleService.getCombatStyle(attackerId)).thenReturn(CombatStyleType.DEFENSIVE);

		Damage event = mock(Damage.class);
		when(event.getCause()).thenReturn(null);
		Damage.Source source = mock(Damage.EntitySource.class);

		invokeGrantAttackerDamageXp(system, commandBuffer, attackerRef, attackerId, event, source, 3.0D);

		verify(dispatchService).grantSkillXp(
				eq(commandBuffer),
				eq(attackerRef),
				eq(SkillType.DEFENSE),
				eq(12.0D),
				eq("combat:melee:defensive"),
				eq(true));
		verifyNoMoreInteractions(dispatchService);
	}

	@Test
	void controlledMeleeDamageDistributesSingleRemainderToAttackFirst() {
		SkillXpDispatchService dispatchService = mock(SkillXpDispatchService.class);
		CombatStyleService styleService = mock(CombatStyleService.class);
		CombatDamageXpSystem system = new CombatDamageXpSystem(dispatchService, styleService, createConfig());

		CommandBuffer<EntityStore> commandBuffer = mock(CommandBuffer.class);
		Ref<EntityStore> attackerRef = mock(Ref.class);
		UUID attackerId = UUID.randomUUID();
		when(styleService.getCombatStyle(attackerId)).thenReturn(CombatStyleType.CONTROLLED);

		Damage event = mock(Damage.class);
		when(event.getCause()).thenReturn(null);
		Damage.Source source = mock(Damage.EntitySource.class);

		invokeGrantAttackerDamageXp(system, commandBuffer, attackerRef, attackerId, event, source, 1.75D);

		verify(dispatchService).grantSkillXp(
				eq(commandBuffer),
				eq(attackerRef),
				eq(SkillType.ATTACK),
				eq(3.0D),
				eq("combat:melee:controlled:attack"),
				eq(true));
		verify(dispatchService).grantSkillXp(
				eq(commandBuffer),
				eq(attackerRef),
				eq(SkillType.STRENGTH),
				eq(2.0D),
				eq("combat:melee:controlled:strength"),
				eq(true));
		verify(dispatchService).grantSkillXp(
				eq(commandBuffer),
				eq(attackerRef),
				eq(SkillType.DEFENSE),
				eq(2.0D),
				eq("combat:melee:controlled:defense"),
				eq(true));
		verifyNoMoreInteractions(dispatchService);
	}

	@Test
	void projectileDamageRoutesAllXpToRanged() {
		SkillXpDispatchService dispatchService = mock(SkillXpDispatchService.class);
		CombatStyleService styleService = mock(CombatStyleService.class);
		CombatConfig config = new CombatConfig(
				4.0D,
				"Combat:Ranged",
				"Combat:Melee:",
				"Accurate",
				"Aggressive",
				"Defensive",
				"Controlled:Attack",
				"Controlled:Strength",
				"Controlled:Defense",
				"Combat:Block:Defense",
				List.of("projectile"));
		CombatDamageXpSystem system = new CombatDamageXpSystem(dispatchService, styleService, config);

		CommandBuffer<EntityStore> commandBuffer = mock(CommandBuffer.class);
		Ref<EntityStore> attackerRef = mock(Ref.class);
		UUID attackerId = UUID.randomUUID();

		Damage event = mock(Damage.class);
		Damage.Source source = mock(Damage.ProjectileSource.class);

		invokeGrantAttackerDamageXp(system, commandBuffer, attackerRef, attackerId, event, source, 2.0D);

		verify(dispatchService).grantSkillXp(
				eq(commandBuffer),
				eq(attackerRef),
				eq(SkillType.RANGED),
				eq(8.0D),
				eq("combat:ranged"),
				eq(true));
		verifyNoMoreInteractions(dispatchService);
	}

	@Test
	void zeroOrNegativeEffectiveDamageDoesNotDispatchXp() {
		SkillXpDispatchService dispatchService = mock(SkillXpDispatchService.class);
		CombatStyleService styleService = mock(CombatStyleService.class);
		CombatDamageXpSystem system = new CombatDamageXpSystem(dispatchService, styleService, createConfig());

		CommandBuffer<EntityStore> commandBuffer = mock(CommandBuffer.class);
		Ref<EntityStore> attackerRef = mock(Ref.class);
		UUID attackerId = UUID.randomUUID();
		when(styleService.getCombatStyle(attackerId)).thenReturn(CombatStyleType.ACCURATE);

		Damage event = mock(Damage.class);
		Damage.Source source = mock(Damage.EntitySource.class);

		invokeGrantAttackerDamageXp(system, commandBuffer, attackerRef, attackerId, event, source, 0.0D);
		invokeGrantAttackerDamageXp(system, commandBuffer, attackerRef, attackerId, event, source, -4.0D);

		verifyNoMoreInteractions(dispatchService);
	}

	private static CombatConfig createConfig() {
		return new CombatConfig(
				4.0D,
				"combat:ranged",
				"combat:melee:",
				"accurate",
				"aggressive",
				"defensive",
				"controlled:attack",
				"controlled:strength",
				"controlled:defense",
				"combat:block:defense",
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
}
