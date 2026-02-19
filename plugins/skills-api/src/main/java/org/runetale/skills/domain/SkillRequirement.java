package org.runetale.skills.domain;

import javax.annotation.Nonnull;

/**
 * Immutable pair of a skill type and its required level, used by multi-skill
 * crafting recipe requirements.
 */
public record SkillRequirement(@Nonnull SkillType skillType, int requiredLevel) {

	public SkillRequirement {
		requiredLevel = Math.max(1, requiredLevel);
	}
}
