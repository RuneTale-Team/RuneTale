package org.runetale.skills.progression.service;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.component.PlayerSkillProfileComponent;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.progression.domain.SkillXpGrantResult;
import org.runetale.skills.service.OsrsXpService;

import javax.annotation.Nonnull;

/**
 * Central mutation service for player skill XP and level progression.
 */
public class SkillProgressionService {

	private final ComponentType<EntityStore, PlayerSkillProfileComponent> profileComponentType;
	private final OsrsXpService xpService;

	public SkillProgressionService(
			@Nonnull ComponentType<EntityStore, PlayerSkillProfileComponent> profileComponentType,
			@Nonnull OsrsXpService xpService) {
		this.profileComponentType = profileComponentType;
		this.xpService = xpService;
	}

	@Nonnull
	public SkillXpGrantResult grantExperience(
			@Nonnull ComponentAccessor<EntityStore> accessor,
			@Nonnull Ref<EntityStore> playerRef,
			@Nonnull SkillType skillType,
			double experience) {

		PlayerSkillProfileComponent profile = accessor.ensureAndGetComponent(playerRef, this.profileComponentType);

		long previousXp = profile.getExperience(skillType);
		int previousLevel = profile.getLevel(skillType);

		long updatedXp = this.xpService.addXp(previousXp, experience);
		long gainedXp = Math.max(0L, updatedXp - previousXp);
		int updatedLevel = this.xpService.levelForXp(updatedXp);

		if (gainedXp > 0L || updatedLevel != previousLevel) {
			profile.set(skillType, updatedXp, updatedLevel);
			accessor.putComponent(playerRef, this.profileComponentType, profile);
		}

		return new SkillXpGrantResult(skillType, previousXp, updatedXp, gainedXp, previousLevel, updatedLevel);
	}
}
