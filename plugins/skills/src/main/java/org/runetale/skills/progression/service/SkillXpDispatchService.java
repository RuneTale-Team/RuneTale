package org.runetale.skills.progression.service;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.progression.event.SkillXpGrantEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dispatches XP grant requests through ECS events.
 */
public class SkillXpDispatchService {

	private static final Logger LOGGER = Logger.getLogger(SkillXpDispatchService.class.getName());

	public boolean grantSkillXp(
			@Nonnull ComponentAccessor<EntityStore> accessor,
			@Nonnull Ref<EntityStore> playerRef,
			@Nonnull SkillType skillType,
			double experience,
			@Nullable String source,
			boolean notifyPlayer) {
		double normalizedExperience = Math.max(0.0D, experience);
		if (normalizedExperience <= 0.0D) {
			return false;
		}

		String normalizedSource = (source == null || source.isBlank()) ? "unspecified" : source.trim();
		accessor.invoke(playerRef, new SkillXpGrantEvent(skillType, normalizedExperience, normalizedSource, notifyPlayer));
		return true;
	}

	public boolean grantSkillXp(
			@Nonnull ComponentAccessor<EntityStore> accessor,
			@Nonnull Ref<EntityStore> playerRef,
			@Nonnull String skillId,
			double experience,
			@Nullable String source,
			boolean notifyPlayer) {
		SkillType skillType = SkillType.tryParseStrict(skillId);
		if (skillType == null) {
			LOGGER.log(Level.WARNING, String.format("Rejected XP grant for unknown skillId=%s", skillId));
			return false;
		}

		return grantSkillXp(accessor, playerRef, skillType, experience, source, notifyPlayer);
	}
}
