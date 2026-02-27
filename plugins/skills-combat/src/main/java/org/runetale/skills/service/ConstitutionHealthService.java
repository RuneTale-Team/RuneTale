package org.runetale.skills.service;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.api.SkillsRuntimeApi;
import org.runetale.skills.config.CombatConfig;
import org.runetale.skills.domain.SkillType;

import javax.annotation.Nonnull;

public class ConstitutionHealthService {

	private static final String HEALTH_MODIFIER_KEY = "skills:constitution:max_health";

	private final SkillsRuntimeApi runtimeApi;
	private final CombatConfig combatConfig;

	public ConstitutionHealthService(@Nonnull SkillsRuntimeApi runtimeApi, @Nonnull CombatConfig combatConfig) {
		this.runtimeApi = runtimeApi;
		this.combatConfig = combatConfig;
	}

	public void syncForPlayer(
			@Nonnull ComponentAccessor<EntityStore> accessor,
			@Nonnull Ref<EntityStore> playerRef,
			boolean healOnIncrease) {
		int constitutionLevel = this.runtimeApi.getSkillLevel(accessor, playerRef, SkillType.CONSTITUTION);
		syncForLevel(accessor, playerRef, constitutionLevel, healOnIncrease);
	}

	public void syncForLevel(
			@Nonnull ComponentAccessor<EntityStore> accessor,
			@Nonnull Ref<EntityStore> playerRef,
			int constitutionLevel,
			boolean healOnIncrease) {
		EntityStatMap statMap = accessor.getComponent(playerRef, EntityStatMap.getComponentType());
		if (statMap == null) {
			return;
		}

		int healthStatIndex = DefaultEntityStatTypes.getHealth();
		EntityStatValue healthBefore = statMap.get(healthStatIndex);
		if (healthBefore == null) {
			return;
		}

		float previousMax = healthBefore.getMax();
		float healthBonus = (float) constitutionHealthBonus(constitutionLevel);
		if (healthBonus > 0.0F) {
			statMap.putModifier(
					healthStatIndex,
					HEALTH_MODIFIER_KEY,
					new StaticModifier(Modifier.ModifierTarget.MAX, StaticModifier.CalculationType.ADDITIVE, healthBonus));
		} else {
			statMap.removeModifier(healthStatIndex, HEALTH_MODIFIER_KEY);
		}

		if (!healOnIncrease) {
			return;
		}

		EntityStatValue healthAfter = statMap.get(healthStatIndex);
		if (healthAfter == null) {
			return;
		}

		float maxDelta = healthAfter.getMax() - previousMax;
		if (maxDelta > 0.0F) {
			statMap.addStatValue(healthStatIndex, maxDelta);
		}
	}

	public double constitutionHealthBonus(int constitutionLevel) {
		int baseLevel = this.combatConfig.constitutionBaseLevel();
		if (constitutionLevel <= baseLevel) {
			return 0.0D;
		}

		return (constitutionLevel - baseLevel) * this.combatConfig.constitutionHealthPerLevel();
	}
}
